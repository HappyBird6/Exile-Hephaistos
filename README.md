# Exile-Hephaistos

PoE2 제작 의사결정 지원 프로젝트의 개발 환경입니다. 현재는 기반 scaffold이며 게임 계산·로그인·가격·AI API는 구현 전입니다.

## 빠른 시작 — Docker만 사용

Docker Desktop의 Linux container 모드를 실행합니다. Windows 기본 **Windows PowerShell 5.1**에서 프로젝트 루트로 이동한 뒤 실행합니다:

```powershell
.\scripts\dev.ps1 stack
```

Compose 프로젝트 이름은 **`exile-hephaistos`**로 고정됩니다. 스크립트는 실행한 터미널의 현재 폴더와 무관하게 스크립트가 있는 프로젝트를 사용합니다. 현재 운영 기준 폴더는 원래 Git checkout이며 검증용 worktree에서 실행하지 않습니다. Docker Desktop에서는 `exile-hephaistos` 그룹을 사용하세요.

` .\scripts\dev.ps1 status`로 상태를 확인합니다. 코드 변경을 이미지에 반영할 때는 ` .\scripts\dev.ps1 stack`을 다시 실행합니다. `stop`은 이 프로젝트만 정지합니다. PowerShell 실행 정책으로 차단될 때만 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 stack`으로 해당 프로세스에서 실행하세요. 전역 실행 정책을 변경할 필요는 없습니다.

스크립트는 `.env`가 없을 때만 무작위 로컬 비밀번호로 생성합니다. 기존 `.env`는 보존합니다.
Frontend: http://localhost:8081 · Backend liveness: http://localhost:8080/actuator/health/liveness

readiness는 검증된 published snapshot이 없어 **503**입니다. 프로세스 실행 실패가 아니며, 실제 데이터 기반 readiness 구현 전까지 정상 서비스로 광고하지 않습니다.
`.\scripts\dev.ps1 stop`은 컨테이너를 정지하고 DB volume을 보존합니다. PostgreSQL은 외부 volume을 사용하므로 Compose가 수명을 관리하지 않습니다. 일반 종료에는 `stop`을 사용하고 DB volume을 직접 삭제하지 마세요.
Compose는 **로컬 개발 전용**입니다. DB 계정은 로컬 migration·app 공용이며 운영 계정 분리·TLS·별도 migration job·배포는 후속 작업입니다.

## 기존 데이터와 실행 환경 전환

`.env`의 `POSTGRES_VOLUME_NAME`으로 사용할 DB volume을 지정합니다. 현재 PC는 기존 데이터 보존을 위해 `exile-bootstrap-01a0c22b_postgres-data`를 재사용하며, 새 설치의 기본값은 `exile-hephaistos_postgres-data`입니다. 기존 volume 사용 시 DB 이름·사용자·비밀번호도 해당 DB와 일치해야 합니다. `.env` 변경은 기존 DB 비밀번호를 자동 변경하지 않습니다.

기존 `infra` 또는 `exile-bootstrap-*` 그룹이 실행 중이면 먼저 그 그룹만 정지해야 같은 포트를 사용할 수 있습니다. 이전 worktree에서 Compose를 다시 실행하지 마세요. 환경 전환 시 원본 `.env`의 백업은 `.env.before-unify-*`에 로컬로만 보관합니다.

## 호스트에서 개발

Java 21 (`JAVA_HOME`), Node 24.18.1 / npm 11.16.0, Python 3.12, uv 0.9.7, Docker가 필요합니다. 전역 Java 8 설정을 바꾸기보다 터미널의 `JAVA_HOME`을 Java 21 경로로 설정하세요.

```powershell
.\scripts\dev.ps1 infra
# 별도 터미널
.\scripts\dev.ps1 backend
# 별도 터미널
.\scripts\dev.ps1 frontend
```

Vite: http://localhost:5173. `/api` 요청은 localhost:8080으로 proxy합니다. 아직 제품 endpoint는 없으며 기본 Security는 probe 외 요청을 거부하고 CSRF를 유지합니다.
`APP_CORS_ALLOWED_ORIGINS` 등은 후속 계약이며 현재 개발은 same-origin proxy를 사용합니다. session cookie는 Secure/HttpOnly/SameSite=Lax입니다. 로그인 구현 시 로컬 HTTPS 개발을 구성해야 합니다.

Linux/macOS에서는 `.env.example`을 `.env`로 복사하고 비밀번호를 변경합니다. 최초 설치는 `docker volume create exile-hephaistos_postgres-data`로 volume을 준비한 뒤 `docker compose --env-file .env -f infra/compose.yaml --profile stack up -d --build`를 사용합니다. 호스트 Backend 실행에는 `.env`의 환경변수를 shell에 export하고 `cd backend && ./gradlew bootRun`을 실행합니다. Spring Boot가 `.env`를 자동 로딩한다고 가정하지 않습니다.

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

기술 명세: [docs/TECHNICAL_SPEC.md](docs/TECHNICAL_SPEC.md). 운영 기준: [docs/solo-workflow/MULTI_SESSION_WORKFLOW.md](docs/solo-workflow/MULTI_SESSION_WORKFLOW.md).
