# Exile-Hephaistos

영어 PoE2 아이템 복사 텍스트를 분석하고 Solar Amulet의 제작 확률을 탐색하는 작업대입니다. PoE2DB Base 속성 가중치로 일반 화폐 6종의 다음 상태와 제한된 화폐 순서를 계산합니다. 화폐 소모나 무작위 수치 roll은 하지 않으며, 붙여넣은 아이템은 표시용입니다. [ItemState·확률 탐색 명세](docs/item-state.md).

- `/`: 재료 탭·검색·툴팁·즐겨찾기·아이템 입력과 카드.
- `/admin`: 로그인 없이 접근하는 최소 관리 페이지. 현재 관리 도구는 없습니다.
- Backend: `POST /api/v1/items/parse`로 영어 아이템 텍스트를 분석합니다.
- PostgreSQL·Redis와 저장 기반은 다음 작업을 위해 유지합니다.

## Docker 실행

Docker Desktop의 Linux container 모드를 켜고 Windows PowerShell 5.1에서 실행합니다.

```powershell
.\scripts\dev.ps1 stack
.\scripts\dev.ps1 status
.\scripts\dev.ps1 stop
```

실행 정책으로 차단될 때만 다음처럼 해당 프로세스에서 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 stack
```

Compose 프로젝트 이름은 `exile-hephaistos`입니다. 운영 실행은 원본 checkout에서 하며 검증용 worktree에서 기존 프로젝트를 실행하지 않습니다.
Frontend: http://localhost:8081 · Backend liveness: http://localhost:8080/actuator/health/liveness
readiness는 앱·PostgreSQL·Redis 연결 상태를 확인합니다. 미구현 제작 계산의 준비 완료를 뜻하지 않습니다.

최초 실행 시 스크립트가 DB·Redis 설정과 무작위 로컬 DB 비밀번호를 `.env`에 생성합니다. 기존 `.env`는 덮어쓰지 않습니다. `.env.example`은 필요하지 않습니다.
`stop`은 컨테이너만 정지합니다. PostgreSQL은 외부 volume을 사용하며 기존 데이터를 삭제하지 않습니다. 코드 반영 시 `stack`을 다시 실행합니다.

## 기존 데이터 보존

기존 `.env`의 `POSTGRES_VOLUME_NAME`을 유지하세요. 새 환경의 기본 이름은 `exile-hephaistos_postgres-data`입니다. DB 이름·계정·비밀번호는 기존 volume의 값과 일치해야 하며 `.env`를 수정해도 DB 비밀번호가 자동 변경되지는 않습니다.
이번 기능 정리는 과거 DB table·schema, Docker volume, 로컬 원본 파일을 삭제하지 않습니다. 이미 적용된 migration SQL은 이력 호환을 위해 유지합니다. 제거된 기능은 앱에서 접근하거나 실행하지 않습니다.

## 호스트 개발

Java 21 (`JAVA_HOME`), Node 24.18.1 / npm 11.16.0, Docker를 사용합니다.

```powershell
.\scripts\dev.ps1 infra
# 별도 터미널
.\scripts\dev.ps1 backend
# 별도 터미널
.\scripts\dev.ps1 frontend
```

Vite는 http://localhost:5173 에서 실행하고 `/api`를 Backend 8080 포트로 전달합니다. Python은 앱 실행에 필요하지 않습니다.
로그인·세션·역할 검사는 없습니다. Compose 포트는 localhost에만 노출합니다.

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
```

Backend `check`는 Spotless·JUnit·ArchUnit·Testcontainers 통합 검사를 포함합니다. Docker가 필요하며 임시 PostgreSQL·Redis를 사용하고 기존 개발 DB는 변경하지 않습니다.
`generateJooq`도 임시 PostgreSQL에서만 실행합니다. JPA flush → jOOQ 조회 → 공유 transaction rollback 검증을 유지합니다.

Windows 실행 스크립트 변경은 Python 3.12로 아래 검사를 수행합니다. 개발 스크립트 테스트용이며 앱 런타임 의존성이 아닙니다.

```powershell
python -m unittest discover -s scripts/tests -v
docker compose --env-file .env -f infra/compose.yaml --profile stack config --quiet
```

GitHub Actions는 사용하지 않습니다. `.gitattributes`는 Windows/Linux 줄바꿈을, `.editorconfig`는 편집기 인코딩·들여쓰기를 통일합니다.

## 구현과 출처

- Java 21, Spring Boot 3.5.16, Gradle 8.14.3, PostgreSQL 17.6, Redis 7.4.5.
- Frontend 의존성은 `package-lock.json`으로 고정합니다.
- 주석만 있는 제작 관련 준비 파일은 다음 작업을 위해 유지합니다.
- [기술 명세](docs/TECHNICAL_SPEC.md), [아이템 분석](docs/item-text-parsing.md), [제작 자산 출처](docs/crafting-ui.md), [지원 범위](docs/supported-mechanics.md).
- [Worker 운영 명세](docs/solo-workflow/MULTI_SESSION_WORKFLOW.md).
