# Exile-Hephaistos

PoE2 아이템 텍스트 파싱, 검토된 카탈로그 기반 Workbench와 최대 6옵션 자동 탐색을 제공하는 로컬 작업대입니다. 게임 데이터와 규칙은 [v0.5.5](docs/v0.5.5/), 개발·관리 원칙은 [AGENTS.md](AGENTS.md)를 확인하세요.

등록 베이스와 지원 UI 언어는 [자동 생성된 현행 요약](#현행-데이터-요약)을 따릅니다. 등록 수가 모든 화폐·목표의 계산 완료를 보장하지는 않습니다. 확률은 출처 스냅샷과 명시된 모델 가정에 따릅니다.

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
임시 계정 로그인은 Redis 세션을 사용하며 관리자 인증과 별개입니다. 계산 job은 기존 소유권 cookie로 구분합니다. Compose 포트는 localhost에만 노출합니다.

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

운영 설정은 `backend-settings.yaml`과 `infra/docker/nginx.conf`에서 관리합니다. 관리자 인증은 HTTPS와 별도 비밀번호를 사용하며, HTTPS 환경의 계정 쿠키에는 `ACCOUNT_COOKIE_SECURE=true`를 설정합니다. 요청 제한·로그 보관·디스크 사용량을 함께 확인하세요. 과거 보안 검토는 [보관 자료](docs/legacy/security.md)에 남아 있습니다.


## 문서와 데이터 관리

현재 편집 위치는 아래와 같습니다. 과거 문서·원본 HTML·검증 보고서·라이선스는 삭제하지 않고 `docs/legacy/`에 기존 하위 구조로 보관했습니다. 관리 방법은 이 README, 에이전트 지침은 AGENTS.md만 갱신하며 작업별 계획·완료·인계 문서는 새로 만들지 않습니다.

```text
docs/
  contracts/              # 버전 공통 API·형식 schema와 FE/BE 공유 검증 사례
  v0.5.5/
    i18n/                 # UI·게임 번역 JSON
    game-data/
      catalog/            # 베이스·옵션·가중치·증강물·출처 JSON
      crafting/           # 화폐·징조·목표·지원 범위·ruleset JSON
      display/            # 프런트 전용 게임 표시 데이터 JSON
    game-rule/            # 화폐·아이템·증강물·확률·표시·사용자 정책 MD
    mappings/             # 종류별 짧은 ID ↔ 기존 식별자 JSON
    manifest.json         # 자동 생성 파일 목록·실행 경로·SHA-256·매핑 목록
  legacy/                 # 재평가 전 보관; 별도 삭제 승인 없이 제거하지 않음
```

**현재 단계:** v0.5.5가 게임 JSON·번역의 편집 원본이고 기존 backend/frontend 경로는 호환용 복사본입니다. 이번에는 짧은 ID를 부여했으며 API·저장 형식·서버 시작 로더는 기존 식별자를 계속 사용합니다. 서버의 새 매핑 로드와 송수신 ID 전환은 다음 코드 경량화 단계입니다. 표시 이름·설명·출처 문자열도 이 단계에서 제거하지 않았습니다. `0.5.5`는 저장소의 버전 구분이며 검증된 GGG 패치 번호를 뜻하지 않습니다.

[화폐](docs/v0.5.5/game-rule/currency.md), [상태·저장](docs/v0.5.5/game-rule/item-state.md), [증강물](docs/v0.5.5/game-rule/sockets-and-augments.md), [확률·목표](docs/v0.5.5/game-rule/probability-and-goals.md), [번역](docs/v0.5.5/game-rule/i18n-and-display.md), [사용자 정책](docs/v0.5.5/game-rule/user-policy.md)을 변경 대상에 맞게 수정합니다. 과거 세부 명세는 legacy에 남지만 중복 유지보수하지 않습니다. 기존 runtime JSON에 적힌 `docs/...` 출처 경로는 수집 당시 기록이며 현재 실제 파일은 `docs/legacy/...`에 있습니다. 봉인된 출처 문자열을 이동 경로에 맞춰 다시 직렬화하지 않습니다.

### ID 규칙

`b0001`처럼 종류 접두사 + 소문자 base36 일련번호(최소 네 자리)를 사용합니다. 이름의 번역·파일 정렬·데이터 추가로 기존 ID를 다시 부여하지 않습니다. 삭제된 항목은 `retired: true`로 남기고 번호를 재사용하지 않습니다. 동일한 원래 키를 복구하면 원래 ID를 다시 활성화합니다. 다음 데이터 버전을 만들 때도 이전 매핑을 승계해 기존 ID와 retired 항목을 보존합니다. 서로 다른 종류의 원래 키가 같아도 합치지 않습니다.

| 접두사 | 대상 | 기존 식별 기준 |
|---|---|---|
| b | 베이스 | top-bases / base-policies.legacy의 key |
| m / s | 옵션 / stat | modifier ID / source stat ID |
| r / c / o | 재료 / 화폐 실행 / 징조 실행 | registry entry ID / currency ID / omen key |
| a | 증강물 | augments의 ID |
| t / f / p | 태그 / 옵션 family / pseudo | 각 원본의 식별자 |
| k | 장비 분류 | itemClass |
| n / x / u | 게임 번역 항목 / 옵션 템플릿 / UI 문구 | 번역 객체의 key |

각 매핑은 `domain`, `prefix`, `entries[{id, sourceKey, retired}]` 형식입니다. `manifest.mappings`가 파일 목록과 digest를 제공합니다. 향후 로더는 각 종류에서 `id → sourceKey`, `sourceKey → id` 인덱스를 만들 수 있습니다. 베이스 key와 게임 Metadata ID, 화폐 action과 표시용 재료 ID는 별개이며 원본 JSON의 명시적 연결을 사용합니다. 이름 유사성으로 자동 병합하지 않습니다. 역사 자료·테스트 fixture·파일 경로·provenance 버전은 새 게임 ID의 대상이 아닙니다.

### 어디를 수정하면 되는가

| 수정할 내용 | 사람이 편집할 파일 |
|---|---|
| 베이스·옵션·가중치·화폐·징조·증강물 | `docs/v0.5.5/game-data/catalog/`, `crafting/`의 해당 JSON |
| 게임 이름·툴팁·옵션 표시·UI 문구 | `docs/v0.5.5/i18n/`의 해당 JSON |
| 프런트 전용 게임 표시 데이터 | `docs/v0.5.5/game-data/display/`의 해당 JSON |
| 게임 규칙 설명 | `docs/v0.5.5/game-rule/`의 해당 MD |
| API 형식·공유 검증 사례 | `docs/contracts/`의 해당 schema·fixture |
| API 제한·지원 언어 정책 | `backend/src/main/resources/contracts/` |
| 서버 운영 값 | `backend/src/main/resources/backend-settings.yaml` |

경량화의 우선 목표는 직접 이해하고 수정하기 쉬운 구조입니다. 수정 지점·중간 계층·중복 관리를 줄입니다. 데이터 전송 크기와 코드 줄 수만으로 판단하지 않습니다.

### 갱신과 검증

JSON을 수정한 뒤 저장소 루트에서 **한 명령**으로 갱신합니다. engine/ledger/ruleset의 의미가 바뀌면 버전과 호환 정책을 먼저 검토합니다.

```sh
node scripts/versioned-game-data.mjs --write
```

이 명령은 ruleset digest → ID·실행 복사본·manifest → 브라우저 복사본 → 공개 계약·README 요약 순으로 갱신합니다. 순서를 외우거나 내부 스크립트를 각각 실행할 필요가 없습니다. `--check`는 같은 정합성을 검사하고 파일을 수정하지 않습니다. 프런트 test/build에서도 자동 실행합니다.

```sh
node scripts/versioned-game-data.mjs --check
```

**규칙 MD만 수정하면 생성 명령이 필요 없습니다.** 문서 문구를 실행 데이터의 해시에 포함하지 않습니다. 새 JSON은 해당 폴더에 추가하면 자동으로 파일 목록과 실행 경로를 생성합니다. `manifest.json`의 파일 목록·해시·경로는 직접 편집하지 않습니다.

| 원본 폴더 | 자동 생성되는 실행 위치 |
|---|---|
| `game-data/catalog/`, `game-data/crafting/` | backend resources의 같은 하위 경로 |
| `game-data/display/` | `frontend/src/features/crafting/`의 같은 하위 경로 |
| `i18n/` | `frontend/src/shared/i18n/`의 같은 하위 경로 |

기존 실행 경로의 복사본은 서버·프런트 코드의 호환을 위해 유지합니다. 직접 편집하면 검사에서 불일치를 알립니다. 원본 파일을 제거할 때는 기존 실행 복사본도 검토해 함께 보관해야 하며 도구가 자동 삭제하지 않습니다. 발급한 ID 매핑을 지우고 재생성하지 않습니다.

게임 수치·weight·단위·출처를 추측하지 않고 원본 bytes와 배열 순서를 유지합니다. 수집 raw JSON도 출처 검증에 필요하므로 보존합니다. 번역 템플릿을 변경하면 Java `ModifierTextResources.TEMPLATE_SHA256`을 함께 검토·갱신하고 `node scripts/check-display-coverage.mjs`로 검증합니다. 새 메커니즘은 단일·반복·일반/훼손 제작, 저장 복원, 확률·표시의 직접 영향을 검사합니다. 과거 compatibility digest를 현재 값으로 덮어쓰지 않습니다.

번역 수집은 `collect-i18n-terms.mjs` / `collect-i18n-modifiers.mjs`의 `--output-dir`로 저장소 밖 검토 결과를 만들고 검토한 값만 i18n 원본에 반영합니다. legacy의 증거 숫자·결정을 현재 정책으로 덮어쓰지 않습니다.

### 공유 계약

기존 루트 `contracts/`는 [docs/contracts](docs/contracts/)로 이동했습니다. API 버전·양쪽 구현 검증 자료이므로 게임 버전 폴더와 구분합니다. Java·TypeScript import, 생성 도구와 Docker 빌드는 새 경로를 사용합니다. 서버가 직접 읽는 `backend/src/main/resources/contracts/`와 생성된 `frontend/src/shared/contracts/`는 기존 실행 위치를 유지합니다.

- `*.schema.json`: 데이터 형식을 정의합니다. `workbench-definitions-v1.schema.json`의 화폐·징조 목록과 개수는 원본 JSON에서 자동 갱신합니다. 경로 검색 타입은 `frontend/src/features/crafting/path-tree/generate-types.mjs`로 생성합니다.
- `crafting-paths-v1/`, `crafting-methods-v2/`, `support-goal-filter-v1/`: API 형식·확률 질량·그래프·목표 판정의 공유 사례입니다. 합성 fixture는 실제 게임 확률이나 전체 기능 지원의 증거가 아닙니다. 확률의 하한·상한·미해결 질량, revision과 cursor 경계를 유지합니다.
- `chaos-repeat/`: 요청의 UTF-8 bytes와 SHA-256을 양쪽에서 대조합니다. 반복 응답의 최종 상태·횟수·마지막 witness·부분 실패를 구분하고 네트워크 실패 시 자동 재시도하거나 완료 횟수를 추측하지 않습니다.
- `sanctification/`: Java와 브라우저가 같은 축성 표시 계산을 하는지 검증합니다.

계약 설명의 기존 README는 [legacy/contracts](docs/legacy/contracts/)에 보관했습니다. 새로운 관리 안내는 이 README에 통합합니다. 검증 스크립트는 다음 위치에서 실행할 수 있습니다.

```sh
node docs/contracts/crafting-paths-v1/verify.mjs
node docs/contracts/chaos-repeat/check-body-hash.mjs
python docs/contracts/support-goal-filter-v1/verify_fixtures.py
```

## 현행 데이터 요약

<!-- generated:public-contracts:start -->
| 항목 | 현행 값 |
|---|---|
| 등록 베이스 | 179 |
| UI 언어 | en, ko, zh-CN, zh-TW, ja, es, ru, th (기본 ko) |
| 증강물 | 등록 289 / 선택 244 |
| 화폐·징조 실행 정의 | 171 / 20 (보관함 표시 정책과 별개) |
| 카오스 반복 | 기본 1000, 범위 1–1000 |
| 반복 요청 본문 | 65536 UTF-8 bytes |
| 패턴 | 길이 512, 깊이 16, 문법 노드 512, 프로그램 4096 |
| 제작지원 정책 | approved-187-2026-10-30-strict-exalted-v8 |
<!-- generated:public-contracts:end -->
