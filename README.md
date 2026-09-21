# Exile-Hephaistos

PoE2 제작 의사결정 지원 프로젝트의 개발 환경입니다. 현재는 기반 scaffold이며 게임 계산·로그인·가격·AI API는 구현 전입니다.

## 빠른 시작 — Docker만 사용

Docker Desktop의 Linux container 모드를 실행합니다. PowerShell 7에서:

```powershell
./scripts/dev.ps1 stack
```

스크립트는 `.env`가 없을 때만 무작위 로컬 비밀번호로 생성합니다. 기존 `.env`는 보존합니다.
Frontend: http://localhost:8081 · Backend liveness: http://localhost:8080/actuator/health/liveness

readiness는 검증된 published snapshot이 없어 **503**입니다. 프로세스 실행 실패가 아니며, 실제 데이터 기반 readiness 구현 전까지 정상 서비스로 광고하지 않습니다.
`./scripts/dev.ps1 stop`은 컨테이너를 정지하고 DB volume을 보존합니다. `down -v`는 데이터 삭제이므로 일반 종료에 사용하지 않습니다.
Compose는 **로컬 개발 전용**입니다. DB 계정은 로컬 migration·app 공용이며 운영 계정 분리·TLS·별도 migration job·배포는 후속 작업입니다.

## 호스트에서 개발

Java 21 (`JAVA_HOME`), Node 24.18.1 / npm 11.16.0, Python 3.12, uv 0.9.7, Docker가 필요합니다. 전역 Java 8 설정을 바꾸기보다 터미널의 `JAVA_HOME`을 Java 21 경로로 설정하세요.

```powershell
./scripts/dev.ps1 infra
# 별도 터미널
./scripts/dev.ps1 backend
# 별도 터미널
./scripts/dev.ps1 frontend
```

Vite: http://localhost:5173. `/api` 요청은 localhost:8080으로 proxy합니다. 아직 제품 endpoint는 없으며 기본 Security는 probe 외 요청을 거부하고 CSRF를 유지합니다.
`APP_CORS_ALLOWED_ORIGINS` 등은 후속 계약이며 현재 개발은 same-origin proxy를 사용합니다. session cookie는 Secure/HttpOnly/SameSite=Lax입니다. 로그인 구현 시 로컬 HTTPS 개발을 구성해야 합니다.

Linux/macOS에서는 `.env.example`을 `.env`로 복사하고 비밀번호를 변경한 뒤 `docker compose --env-file .env -f infra/compose.yaml --profile stack up -d --build`를 사용합니다. 호스트 Backend 실행에는 `.env`의 환경변수를 shell에 export하고 `cd backend && ./gradlew bootRun`을 실행합니다. Spring Boot가 `.env`를 자동 로딩한다고 가정하지 않습니다.

## 검증

```powershell
cd backend
./gradlew.bat check generateJooq bootJar
cd ../frontend
npm ci
npm run lint
npm run typecheck
npm run format:check
npm run test -- --run
npm run build
cd ../data-pipeline
uv sync --frozen
uv run --frozen ruff check .
uv run --frozen ruff format --check .
uv run --frozen python -m pytest
```

Backend `check`는 Spotless·JUnit·ArchUnit·`integrationTest`를 포함합니다. Docker 미실행 시 통합 테스트를 skip하지 않고 실패합니다. 테스트마다 임시 PostgreSQL/Redis를 사용하며 개발 DB를 변경하지 않습니다. JPA flush → jOOQ 조회 → 공유 transaction rollback도 검사합니다.
`generateJooq`는 임시 PostgreSQL에 Flyway를 실행하고 모듈 schema별 infrastructure 패키지로 생성합니다. 현재 domain table이 없으므로 생성할 record가 없습니다. 운영 URL 입력은 받지 않습니다. 생성물을 사용할 때 해당 모듈 query와 함께 source set 연결을 추가합니다.

```powershell
# 프로젝트 루트: 비밀 출력 없는 Compose 검증
 docker compose --env-file .env.example -f infra/compose.yaml --profile stack --profile batch config --quiet
# ETL은 상태 출력만 제공하며 외부 수집/발행은 하지 않음
 docker compose --env-file .env.example -f infra/compose.yaml --profile batch run --rm etl --status
```

CI는 backend/Frontend/Python 검사 후 Docker 이미지 build를 수행하며 push·배포하지 않습니다.

## 버전 및 범위

- Java 21, Spring Boot **3.5.16**, Gradle **8.14.3**(배포 SHA-256 검증).
- Boot BOM의 jOOQ OSS **3.19.35**, Flyway **11.7.2**, Testcontainers **1.21.4**. Java 21에서 사용합니다.
- PostgreSQL **17.6**, Redis **7.4.5**. 런타임 build 이미지는 digest로 고정했습니다.
- Frontend는 `package-lock.json`, Python은 `uv.lock`으로 고정합니다.
- 모듈은 실제 구현이 필요할 때 추가하며 빈 패키지를 일괄 생성하지 않습니다.
- OpenAI 설정/키 없이 실행합니다. AI 기능은 disabled이고 실제 유료 API 호출은 없습니다.

호환성 근거: [Spring Boot 3.5 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [BOM](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html), [jOOQ Java 지원](https://www.jooq.org/download/support-matrix-jdk). 버전 고정은 장기 보안 지원 보장이 아니며 배포 전 업데이트 검토가 필요합니다.

기술 명세는 기존 작업 공간의 `docs/TECHNICAL_SPEC.md`를 기준으로 했습니다. 운영 문서 이동 등 기존 미커밋 변경은 이 환경 구성 commit에 포함하지 않습니다.
