# Exile-Hephaistos

PoE2 제작 의사결정 지원 프로젝트입니다. 크롤링 관리자 기능을 제공하며, 게임 계산·일반 사용자 로그인·가격·AI API는 구현 전입니다.

## 빠른 시작 — Docker만 사용

Docker Desktop의 Linux container 모드를 실행합니다. Windows 기본 **Windows PowerShell 5.1**에서 프로젝트 루트로 이동한 뒤 실행합니다:

```powershell
.\scripts\dev.ps1 stack
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 stack
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

Vite: http://localhost:5173. `/api` 요청은 localhost:8080으로 proxy합니다. 제작 endpoint는 아직 없으며 관리자 API는 별도 로그인과 CSRF로 보호합니다.
`APP_CORS_ALLOWED_ORIGINS` 등은 후속 계약이며 현재 개발은 same-origin proxy를 사용합니다. session cookie는 기본 Secure/HttpOnly/SameSite=Lax입니다. 관리자 로그인 개발 시 HTTPS 또는 운영 안내의 로컬 전용 설정을 사용하세요.

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
# 상태 조회는 외부 요청 없이 raw 수집기의 준비 상태만 출력
 docker compose --env-file .env.example -f infra/compose.yaml --profile batch run --rm etl --status
```

CI는 backend/Frontend/Python 검사 후 Docker 이미지 build를 수행하며 push·배포하지 않습니다.

Python의 명시적 `crawl` 명령으로 poe2db 공개 목록 원본을 저장할 수 있습니다.
실행 조건과 오프라인 검증은 [data-pipeline/README.md](data-pipeline/README.md)를 참고하세요.
게임 데이터 정규화·검증·발행은 아직 지원하지 않습니다.

관리자 페이지 `/admin`에서는 DB에 저장한 수집 대상을 수정하고 원본 수집을 요청할 수 있습니다.
관리자 계정과 수집 실행기는 기본 비활성화이며, 환경변수·실행 환경·보관 위치는
[관리자 크롤링 운영 안내](docs/admin-crawling.md)에 설명합니다.
변경 비교와 데이터 최신화는 정제 단계 이후에 구현합니다.

## 버전 및 범위

- Java 21, Spring Boot **3.5.16**, Gradle **8.14.3**(배포 SHA-256 검증).
- Boot BOM의 jOOQ OSS **3.19.35**, Flyway **11.7.2**, Testcontainers **1.21.4**. Java 21에서 사용합니다.
- PostgreSQL **17.6**, Redis **7.4.5**. 런타임 build 이미지는 digest로 고정했습니다.
- Frontend는 `package-lock.json`, Python은 `uv.lock`으로 고정합니다.
- 모듈은 실제 구현이 필요할 때 추가하며 빈 패키지를 일괄 생성하지 않습니다.
- OpenAI 설정/키 없이 실행합니다. AI 기능은 disabled이고 실제 유료 API 호출은 없습니다.

호환성 근거: [Spring Boot 3.5 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [BOM](https://docs.spring.io/spring-boot/3.5/appendix/dependency-versions/coordinates.html), [jOOQ Java 지원](https://www.jooq.org/download/support-matrix-jdk). 버전 고정은 장기 보안 지원 보장이 아니며 배포 전 업데이트 검토가 필요합니다.

기술 명세: [docs/TECHNICAL_SPEC.md](docs/TECHNICAL_SPEC.md). 운영 기준: [docs/solo-workflow/MULTI_SESSION_WORKFLOW.md](docs/solo-workflow/MULTI_SESSION_WORKFLOW.md).

## 제작 작업대 프리뷰

`/`는 PoE2 화폐 창고를 참고한 제작 작업대입니다. 기본 태양의 목걸이를 배치하거나 게임 복사 텍스트를 입력할 수 있습니다. 현재 32종 화폐(조각 4종과 감정 주문서 제외)를 우클릭/클릭/키보드로 선택하고 중앙 아이템으로 사용 요청을 확인합니다. Esc 또는 선택 해제로 취소합니다. 복사 텍스트는 해석 없이 원문으로 유지하며 제작 효과·옵션 변경·화폐 소모·확률 계산은 수행하지 않습니다. `/admin`, `/admin/crawling`은 기존 관리자 기능입니다.

이미지 출처와 미확보 자산은 [제작 화면 자산 기록](docs/crafting-ui.md)을 참고하세요.
