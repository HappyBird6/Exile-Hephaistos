# Exile-Hephaistos

PoE2 아이템 텍스트 파싱, 검토된 카탈로그 기반 Workbench와 최대 6옵션 자동 탐색을 제공하는 로컬 작업대입니다. 게임 데이터와 규칙은 [v0.5.5](docs/v0.5.5/), 개발·관리 원칙은 [AGENTS.md](AGENTS.md)를 확인하세요.

등록 베이스와 지원 UI 언어는 [자동 생성된 현행 요약](#현행-데이터-요약)을 따릅니다. 등록 수가 모든 화폐·목표의 계산 완료를 보장하지는 않습니다. 확률은 출처 스냅샷과 명시된 모델 가정에 따릅니다.

백엔드 코드 위치·API 담당 기능·설정 변경 방법은 [백엔드 안내](backend/README.md)를 확인하세요.

제작지원에서 베이스와 목표 옵션을 선택하면 계산한 제작 세션이 성공 확률순으로 표시됩니다. 각 세션에서 최종 아이템 예시, 경로별 화폐 사용 순서와 세션 오버뷰를 볼 수 있습니다. ‘작업대에서 시작’을 누르면 같은 시작 아이템과 선택한 경로로 직접 제작할 수 있습니다.

제작지원과 테스트 제작작업대에서는 PoE2 거래소의 일반 능력치 필터와 같은 방식으로 목표를 설정합니다. 생명력이 85 이상이고 화염·냉기 저항 중 하나가 30% 이상인 장비를 찾으려면, 기본 `And` 그룹에 생명력을 넣고 최소를 85로 설정합니다. `Count` 그룹에 두 저항을 추가하고 각 행의 최소를 30, 그룹 최소를 1로 설정합니다. 가중합의 빈 가중치는 1입니다. `Weighted Sum`에서는 아이템에 있는 능력치가 행 범위를 벗어나면 탈락하고, `Weighted Sum v2`에서는 그 행의 점수만 제외합니다. v2는 선택한 능력치가 하나 이상 있는 아이템을 대상으로 합니다. [PoE2 Trade](https://www.pathofexile.com/trade2/search/poe2/Standard) · [GGG의 가중합 판정 설명](https://www.pathofexile.com/forum/view-thread/3665545)

`Count`는 같은 능력치를 여러 번 추가해도 각 행의 범위를 따로 판정합니다. 소수 경계는 반올림합니다. 최소값이 음수이면 한 행 이상이 맞아야 하며, 음수의 크기만큼 불일치 행을 허용합니다. 예를 들어 활성 행이 세 개이고 최소가 `-1`이면 두 행 이상이 맞아야 합니다. 최대값 `0`은 상한을 두지 않으며, 최대값 `-1`은 일치 행이 없는 경우입니다. [PoE2 Trade](https://www.pathofexile.com/trade2/search/poe2/Standard)

**테스트 제작작업대**(`/?workbench=preview`)는 제작지원과 같은 제작설정을 사용합니다. 베이스·레벨과 목표를 입력하면 충족 조건과 옵션 구성별 완성 아이템 카드가 표시됩니다. 카드 본문은 실제로 목표를 충족한 한 아이템의 수치입니다. 각 옵션 왼쪽에는 해당 구성에서 가능한 티어를 표시합니다. 예를 들어 `T4, T2~T1`이면 T4·T2·T1이 가능하며, 옵션을 펼치면 티어별 예시 수치를 볼 수 있습니다. 획득처는 영문으로 표시하며 Perfect Essence와 Alloy는 개별 화폐 이름까지 표시합니다. [PoE2DB Modifiers](https://poe2db.tw/us/Bows#ModifiersCalc)

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
      catalog/
        classes.json      # 장비 분류 사실
        amulets/          # 장비 종류별 폴더 (rings, gloves, bows 등)
          modifier-pools.json # 함께 사용하는 옵션 ID 목록
          solar-amulet/   # base.json 정체성 + catalog.json 옵션 참조
      modifiers/          # 장비 종류별 공통 옵션 정의·기존 문맥 차이값
      crafting/           # essences, alloys, liquids, augments, reveal, materials, policies
      assets/             # 베이스 이미지 매핑
      sources.json        # 출처 참조 → URL
    game-rule/            # 화폐·아이템·증강물·확률·표시·사용자 정책 MD
    mappings/             # 종류별 짧은 ID ↔ 기존 식별자 JSON
    manifest.json         # 자동 생성 파일 목록·실행 경로·SHA-256·매핑 목록
  legacy/                 # 재평가 전 보관; 별도 삭제 승인 없이 제거하지 않음
```

v0.5.5가 게임 JSON·번역의 편집 원본이고 backend/frontend 경로는 호환용 생성 결과입니다. 백엔드는 검증된 불변 카탈로그·옵션 정의를 공유하고 제작 정책을 JSON에서 읽습니다. 제작 API는 같은 ID 사전을 확인한 클라이언트와 짧은 ID를 주고받으며, 내부 계산과 저장 형식은 기존 식별자를 유지합니다. 이름·설명은 i18n, 출처 URL은 sources.json에서 관리하며 기존 실행 결과에는 다시 합칩니다. `0.5.5`는 저장소의 버전 구분이며 검증된 GGG 패치 번호를 뜻하지 않습니다.

[화폐](docs/v0.5.5/game-rule/currency.md), [상태·저장](docs/v0.5.5/game-rule/item-state.md), [증강물](docs/v0.5.5/game-rule/sockets-and-augments.md), [확률·목표](docs/v0.5.5/game-rule/probability-and-goals.md), [번역](docs/v0.5.5/game-rule/i18n-and-display.md), [사용자 정책](docs/v0.5.5/game-rule/user-policy.md)을 변경 대상에 맞게 수정합니다. 과거 세부 명세는 legacy에 남지만 중복 유지보수하지 않습니다. 기존 runtime JSON에 적힌 `docs/...` 출처 경로는 수집 당시 기록이며 현재 실제 파일은 `docs/legacy/...`에 있습니다. 봉인된 출처 문자열을 이동 경로에 맞춰 다시 직렬화하지 않습니다.

### ID 규칙

`b0001`처럼 종류 접두사 + 소문자 base36 일련번호(최소 네 자리)를 사용합니다. 이름의 번역·파일 정렬·데이터 추가로 기존 ID를 다시 부여하지 않습니다. 삭제된 항목은 `retired: true`로 남기고 번호를 재사용하지 않습니다. 동일한 원래 키를 복구하면 원래 ID를 다시 활성화합니다. 다음 데이터 버전을 만들 때도 이전 매핑을 승계해 기존 ID와 retired 항목을 보존합니다. 서로 다른 종류의 원래 키가 같아도 합치지 않습니다.

| 접두사 | 대상 | 기존 식별 기준 |
|---|---|---|
| b | 베이스 | 선택용 key와 실제 게임 Metadata ID를 각각 발급 |
| m / s | 옵션 / stat | modifier ID / source stat ID |
| r / c / o | 재료 / 화폐 실행 / 징조 실행 | registry entry ID / currency ID / omen key |
| a | 증강물 | augments의 ID |
| t / f / p | 태그 / 옵션 family / pseudo | 각 원본의 식별자 |
| k | 장비 분류 | itemClass |
| n / x / u | 게임 번역 항목 / 옵션 템플릿 / UI 문구 | 번역 객체의 key |

각 매핑은 `domain`, `prefix`, `entries[{id, sourceKey, retired}]` 형식입니다. `manifest.mappings`가 파일 목록과 digest를 제공합니다. 서버는 생성된 `contracts/game-ids-v1.json`을 시작 시 검증하고 종류별 양방향 색인을 공유합니다. 프런트의 `shared/contracts/gameIds.json`도 같은 명령으로 생성하며 직접 편집하지 않습니다. 베이스 key와 게임 Metadata ID, 화폐 action과 표시용 재료 ID는 별개이며 원본 JSON의 명시적 연결을 사용합니다. 이름 유사성으로 자동 병합하지 않습니다. 역사 자료·테스트 fixture·파일 경로·provenance 버전은 새 게임 ID의 대상이 아닙니다.

제작 API의 짧은 ID 전송은 `X-Crafting-Ids`에 같은 사전 식별자를 보낼 때 사용합니다. 클라이언트는 서버 확인 후 요청 본문도 변환하며 `X-Crafting-Ids-Body: compact`로 표시합니다. 서버는 입력에서 원래 ID로 복원하고 응답의 식별자 필드·키만 줄입니다. 자유 텍스트·미등록 값·기존 저장 문서는 보존합니다. 원문 바이트에 중복 실행 방지 서명이 묶이는 카오스 반복 API는 이 변환에서 제외합니다. 구형 클라이언트는 헤더 없이 기존 형식을 사용합니다.

### 어디를 수정하면 되는가

| 수정할 내용 | 사람이 편집할 파일 |
|---|---|
| 베이스 정체성·옵션 목록 | `game-data/catalog/<종류>/<베이스>/base.json`, `catalog.json`, 종류별 `modifier-pools.json` |
| 옵션 수치·가중치 | `game-data/modifiers/<종류>.json` (공유 옵션은 shared) |
| 화폐·증강물·대상·지원 정책 | `game-data/crafting/`의 해당 분류 JSON |
| 게임 이름·툴팁·옵션 표시·UI 문구 | `docs/v0.5.5/i18n/`의 해당 JSON |
| 베이스 이미지·출처 | `game-data/assets/base-images.json`, `game-data/sources.json` |
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

**규칙 MD만 수정하면 생성 명령이 필요 없습니다.** 문서 문구를 실행 데이터의 해시에 포함하지 않습니다. 새 베이스·옵션은 위 구조와 참조 규칙에 맞게 추가합니다. 생성기는 원본 목록과 실행 결과 목록을 별도로 기록합니다. 새로운 종류의 원본을 추가할 때는 생성 규칙도 함께 정의합니다. `manifest.json`의 파일 목록·해시·경로는 직접 편집하지 않습니다.

| 원본 폴더 | 자동 생성되는 실행 위치 |
|---|---|
| `game-data/catalog/<장비종류>/<베이스>/` | 공통 modifier를 펼쳐 `backend/src/main/resources/catalog/<베이스>/`에 생성 |
| 베이스·modifiers·crafting·i18n의 조합 | 기존 backend catalog 인덱스·재료·정책 JSON |
| 같은 원본·assets | frontend 주얼·공개 옵션·툴팁·대상·이미지 JSON |
| i18n 루트의 UI·번역 JSON | `frontend/src/shared/i18n/`의 같은 하위 경로 |

기존 실행 경로의 복사본은 서버·프런트 코드의 호환을 위해 유지합니다. 직접 편집하면 검사에서 불일치를 알립니다. 원본 파일을 제거할 때는 기존 실행 복사본도 검토해 함께 보관해야 하며 도구가 자동 삭제하지 않습니다. 발급한 ID 매핑을 지우고 재생성하지 않습니다.

게임 수치·weight·단위·출처를 추측하지 않고 값과 배열 순서를 유지합니다. 수집 raw JSON은 `docs/legacy/raw-data/catalog/<베이스>/`에 당시 bytes 그대로 보관합니다. 서버 시작과 배포 JAR에서는 읽지 않으며, 과거 원자료 대조 테스트와 수집·감사 도구에서만 사용합니다. 서버는 ruleset에 봉인된 실행 JSON의 해시를 검증합니다. 번역 템플릿을 변경하면 Java `ModifierTextResources.TEMPLATE_SHA256`을 함께 검토·갱신하고 `node scripts/check-display-coverage.mjs`로 검증합니다. 새 메커니즘은 단일·반복·일반/훼손 제작, 저장 복원, 확률·표시의 직접 영향을 검사합니다. 과거 compatibility digest를 현재 값으로 덮어쓰지 않습니다.

번역 수집은 `collect-i18n-terms.mjs` / `collect-i18n-modifiers.mjs`의 `--output-dir`로 저장소 밖 검토 결과를 만들고 검토한 값만 i18n 원본에 반영합니다. legacy의 증거 숫자·결정을 현재 정책으로 덮어쓰지 않습니다.

공통 옵션 수정은 `game-data/modifiers/<종류>.json`에서 합니다. 파일 안에서 `implicit`(고정), `normal`(일반), `essence`(에센스 전용), `desecrated`(훼손 공개), `augment`(증강물), `alloy`(합금), `liquid`(감정액)를 구분합니다. 해당 데이터가 없는 분류는 생략합니다. 장비·증강물·공개 옵션·추가 화폐 옵션이 이 정의를 함께 참조합니다. 표시 이름·문장은 `i18n/catalog/modifiers.en.json`에서 수정합니다. 각 분류의 `variants`에는 같은 원래 ID에 남아 있던 가중치·티어·태그·이름·출처 차이만 저장합니다. variant는 원래 정의와 같은 분류에 둡니다. `id@번호`는 편집 원본의 참조 키이며 API ID나 `mappings/`의 짧은 ID가 아닙니다. 실행 파일을 만들 때 원래 modifier ID로 복원합니다.

각 분류 안의 `groups`는 티어별 공통 정보를 한 번만 기록하고, `definitions`는 단일·미확정 옵션을 보관합니다. 예를 들어 목걸이 `normal.groups["amulet:prefix:SpellDamage"]`에서 접두 여부·family·태그·`statIds`를 공통 관리합니다. `tiers`는 기존 modifier ID를 키로 사용하고 각 행에 `tier`, `requiredItemLevel`, `weight`, `ranges`, `sourceRef`를 기록합니다. 그룹 이름은 편집용 이름이며 게임 ID나 새 매핑 ID가 아닙니다.

`statIds: ["spell_damage_+%"]`와 `ranges: [[3, 7]]`은 주문 피해 3~7%를 뜻합니다. 복합 옵션의 ranges도 statIds와 같은 개수·순서를 유지합니다. 다른 family·태그·능력치 구성이나 획득 종류를 하나의 그룹으로 섞지 않습니다. 티어 번호는 그룹 내부 순위이며 전역 ID가 아닙니다. 티어 미확정인 훼손 옵션에 새 티어를 추정하지 않습니다. 기존 단일 옵션은 `definitions`의 ID 키 아래에 기존 필드를 유지하며 id를 값에 반복하지 않습니다.

에센스가 일반 옵션을 지정해서 붙이는 경우, 그 정의는 `normal`에 두고 `crafting/essences/targets.json`에서 같은 ID를 참조합니다. 에센스 전용 옵션만 `essence`에 둡니다. 표시 문구·ID 매핑·베이스별 풀은 기존 ID를 계속 참조하며, 생성기가 티어 묶음을 기존 실행 형식으로 펼칩니다. 분류나 그룹 이름으로 화폐의 실제 자격·확률을 새로 추론하지 않습니다. 정리 전 modifier 원본은 `docs/legacy/modifiers-before-tier-groups/`에 보관합니다.

장비 종류별 `modifier-pools.json`은 공통 옵션 목록입니다. 각 베이스의 `modifierPool`이 이 목록을 참조하고, 고정 옵션은 `implicitModifiers`로 위치와 ID만 지정합니다. 추가 제작 옵션 파일도 `modifierRefs`만 보관합니다. 옵션 개수와 가중치 합계는 자동 계산합니다. `*-snapshot` 목록은 기존 수집 시점·능력치 계열·지원 범위 차이를 보존한 것이며, 베이스마다 별도 게임 규칙이 있다는 뜻은 아닙니다. 차이를 합칠 때는 출처를 확인하고 게임 동작 변경으로 검토합니다. 기존 `snapshotId`와 출처 해시는 이력이며 실제 옵션/지원 범위를 바꾸면 해당 snapshot·ruleset 버전도 검토합니다.

`modifiers/`와 `modifier-pools.json`은 편집용 원본으로만 유지합니다. 서버의 펼친 JSON은 자동 생성 결과이며, 서버는 검증된 카탈로그 객체와 동일한 옵션 정의를 재사용합니다. 현재 ruleset은 v15입니다. 기존 아이템 snapshot은 보존하지만, 재료 실행 정책과 기본 제작 한도가 포함된 호환성 해시는 `item-rules-v2`로 구분합니다. 이전 호환 ID를 현재 ID로 덮어쓰지 않습니다. 과거 ruleset으로 저장한 탐색 작업은 기존 버전 검증을 그대로 적용하며 자동 재지정하지 않습니다.

베이스의 이름·요구치 표시 문구는 `i18n/catalog/bases.json`, 정체성·등록 순서·능력치는 각 `base.json`에서 관리합니다. 전체 목록 `top-bases.json`을 별도로 편집하지 않습니다. `catalog/classes.json`은 장비 분류, `crafting/policies/base-support.json`은 앱 지원 여부, `catalog-compatibility.json`은 기존 snapshot·규칙 버전 연결입니다. 등록 `order`와 옵션 배열 순서는 기존 표시·동작 순서를 보존합니다.

`crafting` 하위 폴더의 역할은 다음과 같습니다. 옵션 수치 자체는 modifiers, 표시 이름·설명은 i18n에서 관리합니다.

| 폴더 | 편집 내용 |
|---|---|
| `essences` / `alloys` / `liquids` | 재료별 대상 베이스와 결과 옵션 ID 연결 |
| `augments` | 증강물 정체성·분류와 소켓·추가 풀·배율 규칙 |
| `reveal` | 훼손 공개 후보·대상 베이스·수동 선택 정책 |
| `materials` | 재료 출처·표시 메타데이터·적용 장비 분류 |
| `policies` | 앱 지원 범위와 기존 snapshot·규칙 버전 연결 |
| `goalfilter` | 목표 판정 정의·베이스 목록·과거 결과 읽기 자료 |

루트의 `registry-v2.json`은 현행 재료 등록·지원 정보, `workbench-definitions-v1.json`은 화폐 행동·징조 정의, `automatic-currency-policy-v1.json`은 자동 제작 정책, `supported-base-sets-v1.json`은 지원 베이스 집합입니다. `ruleset-v1.json`은 실행 버전과 파일 해시를 기록합니다. registry-v1과 readonly 파일은 과거 자료입니다.

`crafting/essences`, `alloys`, `liquids`의 targets는 `groups`(행동 → 옵션 참조 목록)와 `bases`(베이스 → 그룹) 형식입니다. 완전히 같은 목록만 공유하며 그룹 이름은 게임 규칙이나 확률을 뜻하지 않습니다. `augments/items.json`은 정체성·분류, `rules.json`은 실행 규칙이고 이름·효과 문구·템플릿은 `i18n/augments/`에 있습니다. 재료 이름·툴팁은 `i18n/materials/en.json`, 출처·표시 메타데이터는 `crafting/materials/items.json`, 대상 분류는 `target-classes.json`에서 수정합니다. 툴팁 번역을 바꿔도 재료 적용 대상은 바뀌지 않습니다.

`crafting/materials/execution.json`은 화폐 ID를 실행 프로필에 연결합니다. 프로필의 `kind`는 재료 분류, `mode`는 지원하는 추가/교체 메커니즘, `inputRarities`는 적용 희귀도, `resultRarity`는 추가 후 희귀도입니다. 교체형은 기존 희귀도를 유지하므로 `resultRarity: null`을 사용합니다. 같은 프로필을 공유하는 화폐는 함께 변경되므로 개별 변경에는 새 프로필을 만들어 연결합니다. 새로운 알고리즘은 Java 구현이 필요하며 JSON에 표현식이나 코드를 넣지 않습니다.

에센스 후보는 기존 베이스까지 `crafting/essences/targets.json`에서 관리합니다. 일반 추가 후보 수가 예전 Java 목록과 다르다는 이유로 거절하지 않으며, 실제 옵션 존재·layer·가중치·아이템 레벨·슬롯/가족 충돌 검증은 유지합니다. 합금·감정액 후보는 각각 `alloys/targets.json`, `liquids/targets.json`, 증강물 설치 제한은 `augments/items.json`, 효과·기본 제작 옵션 한도(`baseCraftedLimit`)는 `augments/rules.json`에서 수정합니다. 원본 수정 후 위 `--write` 명령으로 생성하고 서버를 재시작합니다. 실행 중 active 규칙을 다른 값으로 덮어쓰지 않습니다.

`crafting/reveal/options.json`의 `probability: null`은 확률 미확정이며 수동 공개만 허용한다는 뜻입니다. 기존 실행 형식의 weight 0을 실제 출현 확률 0으로 해석하지 않습니다. 주얼·수동 공개 옵션의 프런트 정의는 공통 modifier에서 생성하므로 별도 원본을 유지하지 않습니다. 사용하지 않는 sapphire 표시 데이터와 기존 catalog 공통 파일·display 원본은 `docs/legacy/game-data-before-normalization/`에 보관합니다. 검증 proof/review는 `docs/legacy/raw-data/`에서 테스트만 읽습니다.

출처 URL은 `sources.json`에 한 번 등록하고 `sourceRef`로 연결합니다. `src` 참조는 내부 출처 키이며 송수신용 짧은 ID가 아닙니다. 기존 참조 키를 재할당하지 않습니다. 과거 readonly 정의·registry-v1·저장 예제는 당시 원문과 bytes를 유지합니다. 원래 수집 시점·snapshot·근거 해시는 해당 메타데이터에 보존하고 새 수집 기록으로 덮어쓰지 않습니다.

## 백엔드 유지보수와 메모리

베이스 등록은 `CatalogCraftingService`의 공통 등록 경로에서 처리합니다. 카탈로그 로딩은 `ItemCatalogLoader`, JSON 제작 정책은 `WorkbenchDefinitions`/`WorkbenchDefinitionsLoader`, 실행은 `WorkbenchSimulator`에서 찾습니다. 이번에 수정한 타입 상단에는 역할·필드·생성자·메서드·중첩 타입의 한국어 설명을 두었습니다.

번들 카탈로그는 풀과 확장 순서별로 한 번 읽고, 전체 값이 같은 불변 옵션 정의만 공유합니다. 같은 ID라도 문맥별 값이 다르면 합치지 않습니다. 179개 베이스의 등록 결과에서 옵션 참조 31,179개를 유지하면서 실제 옵션 객체는 29,225개에서 3,798개로 줄었습니다(`CatalogMemoryProfileTest`). 이는 옵션 객체 수 측정이며 서버 전체 힙 감소율은 아닙니다. 증강물 ID 조회는 인덱스를 사용하고 내부 표시 계산에서 전체 JSON을 반복 복사하지 않습니다. 외부 응답은 방어 복사를 유지합니다. 증강물 조합 실행기 캐시는 베이스별 최대 32개이며 사용자 아이템 상태를 저장하지 않습니다.

하드코딩 점검에서 확인했으나 이번 변경에서 값이나 동작을 바꾸지 않은 항목입니다.

| 위치 | 확인한 데이터/규칙 | 처리 |
|---|---|---|
| `CatalogCraftingService.initial` | Solar 초기 implicit 15, Iron Ring 1–4, 시간 잃은 주얼 반경 1000 | 초기 아이템 데이터 점검만 |
| `WorkbenchSimulator` | 분열 최소 명시 옵션 4개, 기존 연금 옵션 수와 일반 화폐 희귀도 전이 | 일반 화폐 규칙 점검만 |
| `QualityCurrencyRules`, `QualityInfuserRules` | 기본 장비 품질 20, 주입 증가 10, 장비 경로로 대상 분류 | 품질 정책 점검만 |
| `ModifierDefinition` | 옵션 요구 레벨의 1–100 범위 검증 | 공통 데이터 제약 점검만 |
| `BodyEssenceTargets`, `HelmetEssenceTargets`, `RingEssenceTargets`, `WorkbenchCurrency.catalystType` | 기존 베이스 ID·분류·촉매 ID 규약 | 정체성/호환 어댑터 유지 |

제작 옵션 후보와 적용 정책은 위 JSON 경로로 이동했으며, 징조 조합·옵션 제거/선택 순서·미공개 슬롯 처리는 구현된 알고리즘 경계로 유지합니다. 과거 snapshot·게임 수치에 고정된 테스트를 이번 구조에 맞추려고 일괄 변경하지 않습니다. 전체 테스트 개편과 짧은 ID 전송 전환은 별도 작업입니다.

### 공유 계약

기존 루트 `contracts/`는 [docs/contracts](docs/contracts/)로 이동했습니다. API 버전·양쪽 구현 검증 자료이므로 게임 버전 폴더와 구분합니다. Java·TypeScript import, 생성 도구와 Docker 빌드는 새 경로를 사용합니다. 서버가 직접 읽는 `backend/src/main/resources/contracts/`와 생성된 `frontend/src/shared/contracts/`는 기존 실행 위치를 유지합니다.

- 루트의 `goal-definitions-v1.schema.json`, `workbench-definitions-v1.schema.json`: 현재 생성·검증 도구가 사용하는 데이터 형식입니다. 사용처가 없는 goal-bases, reviewed-essences, ruleset, supported-base-sets 스키마는 `docs/legacy/contracts/`에 보관합니다. `workbench-definitions-v1.schema.json`의 화폐·징조 목록과 개수는 원본 JSON에서 자동 갱신합니다. 경로 검색 타입은 `frontend/src/features/crafting/path-tree/generate-types.mjs`로 생성합니다.
- `crafting-paths-v1/`, `crafting-methods-v2/`, `support-goal-filter-v1/`: API 형식·확률 질량·그래프·목표 판정의 공유 사례입니다. 합성 fixture는 실제 게임 확률이나 전체 기능 지원의 증거가 아닙니다. 확률의 하한·상한·미해결 질량, revision과 cursor 경계를 유지합니다.
- `chaos-repeat/`: 요청의 UTF-8 bytes와 SHA-256을 양쪽에서 대조합니다. 반복 응답의 최종 상태·횟수·마지막 witness·부분 실패를 구분하고 네트워크 실패 시 자동 재시도하거나 완료 횟수를 추측하지 않습니다.
- `sanctification/`: Java와 브라우저가 같은 축성 표시 계산을 하는지 검증합니다.

`crafting-paths-v1/solar-source-fixture.json`은 현행 실행 데이터에 묶인 검증 사례입니다. 과거 v11·v13·v14 사례는 `docs/legacy/contracts/crafting-paths-v1/solar-source-fixture-v*.json`에 보존합니다. 데이터 변경 시 먼저 목표·옵션·확률 oracle이 여전히 타당한지 검증한 다음 `node scripts/refresh-source-fixture-identities.mjs`로 현행 사례의 해시를 갱신하고 계약 테스트를 실행합니다. 과거 사례를 최신 값으로 덮어쓰지 않습니다.

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
| 제작지원 정책 | approved-187-2026-11-19-lossless-chaos-v12 |
<!-- generated:public-contracts:end -->
