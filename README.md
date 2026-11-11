# Exile-Hephaistos

PoE2 아이템 텍스트 파싱, 검토된 카탈로그 기반 Workbench와 최대 6옵션 자동 탐색을 제공하는 로컬 작업대입니다. 실제 지원 범위와 계산 한계는 [현행 개요](docs/current/overview.md), 개발 문서는 [문서 지도](docs/README.md)를 확인하세요.

등록 베이스와 지원 UI 언어는 [자동 생성된 현행 요약](docs/current/support.md)을 따릅니다. 등록 수가 모든 화폐·목표의 계산 완료를 보장하지는 않습니다. 확률은 출처 스냅샷과 명시된 모델 가정에 따릅니다.

백엔드 코드 위치·API 담당 기능·설정 변경 방법은 [백엔드 안내](backend/README.md)를 확인하세요.

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
Compose의 호스트 포트 기본값은 백엔드 9000, 프런트 9001입니다. 필요하면 `.env`의 `EXILE_BACKEND_PORT`와 `EXILE_FRONTEND_PORT`로 변경합니다. 컨테이너 내부 포트와 프록시는 8080을 유지합니다. 작업 저장 파일은 외부 `exile-hephaistos_app-data` 볼륨에 보존합니다. 기존 컨테이너 내부 저장 파일이 있다면 최초 전환 전에 백업·복원이 필요합니다.

Frontend: http://localhost:9001 · Backend liveness: http://localhost:9000/actuator/health/liveness
readiness는 앱·PostgreSQL·Redis 연결 상태를 확인합니다. 제작 계산의 완결이나 게임 규칙 검증을 뜻하지 않습니다.

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
사용자 로그인·역할 기반 접근 통제는 없습니다. 계산 job은 소유권 cookie로 구분합니다. Compose 포트는 localhost에만 노출합니다.

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

운영 보안·요청 제한·비용 보호 설정은 [보안 개발 문서](docs/security.md)를 참고하세요.
