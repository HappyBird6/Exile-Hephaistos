# 백엔드 코드 안내

제작작업대와 제작지원은 **하나의 Spring Boot 서버 안에서 기능별로 분리**한다. HTTP 주소를 서비스 내부 디렉토리와 같게 바꾸지 않는다. 기존 프론트 요청, JSON 필드, 오류 코드, 규칙 식별자와 저장 파일의 읽기 호환을 유지한다.

## 어디서 코드를 찾나

`src/main/java/com/poe2craft/` 기준이다.

```text
workbench/
  api/                 수동 제작·심연 제작·카오스 반복 HTTP 요청
  repeat/              카오스 반복 실행과 요청/결과 모델
  display/             반복 판정에 필요한 옵션 문자열·숫자 표시
craftingsupport/
  goal/                목표 조건 카탈로그·검증·평가
  recommendation/      제작 가능성 평가·추천·addition pool 캐시
  search/              자동·method·certified·v1 경로 탐색과 작업 저장
  path/                호환 그래프·first-hit 계산
shared/
  item/                아이템 상태·검증·베이스 카탈로그
    text/              복사 텍스트 분석과 표시용 모델
    infrastructure/    출처가 검증된 아이템 데이터 로딩
  crafting/            양쪽에서 사용하는 화폐·품질·심연 규칙과 시뮬레이터
    api/               공용 Solar 시작 아이템 API
    application/       카탈로그 조회·제작 실행 인터페이스와 구현
    domain/            게임 규칙과 계산용 상태
    infrastructure/    제작 재료·규칙 정의 로딩
  probability/         정확한 유리수 확률 연산
  ruleset/             봉인 리소스·규칙 식별자와 HTTP 경계
  json/                검토 데이터의 엄격한 JSON 읽기 정책
configuration/         운영 설정의 타입·검증·Spring 등록
bootstrap/             공용·제작지원·호환 API 구현 연결과 HTTP 응답 정책
```

각 기능 안에서 `api`는 HTTP 입력·응답, `application`은 실행 흐름과 외부 의존성의 인터페이스, `domain`은 계산·규칙, `infrastructure`는 파일·DB·번들 데이터 접근을 맡는다. 작은 기능은 역할 없는 빈 하위 폴더를 만들지 않는다. 테스트도 기능별로 모으고, 여러 테스트가 사용하는 데이터는 공용 fixture로 분리한다. 공용 규칙의 결합 테스트와 실행 설정의 시나리오 테스트는 각각 공용·bootstrap 테스트에 둔다. `currencyrule`과 주석으로 남겨둔 준비 파일은 이후 작업을 위해 보존한다.

## 프론트 요청과 담당 기능

프론트 경로는 `frontend/src/features/crafting/` 기준이다. 화면 이름뿐 아니라 실제 요청과 연결된 백엔드 실행 코드를 따라 분류했다.

| 프론트 호출 위치 | 기존 API | 담당 코드와 용도 |
|---|---|---|
| `CraftingPage.tsx` → `craftingApi.ts` | `GET /api/v1/crafting/initial` | 공용 `CraftingCatalogController`: Solar 시작 상태. 제작작업대·제작지원·호환 Explorer가 공유 |
| `workbenchApi.ts`, `CraftingPage.tsx` | `/api/v1/crafting/workbench/initial`, `/actions`, `/apply`, `/quality-display`, `/map-text` | 제작작업대 `WorkbenchController`: 시작 상태·한 번 제작·텍스트를 검증된 상태로 연결 |
| `AbyssPanel.tsx`, `desecrationCurrency.ts`, `abyss-workbench/applyAbyssCurrency.ts` | `/api/v1/crafting/workbench/abyss/*` | 제작작업대 `WorkbenchController`: 수동 심연 제작·선택·적용 |
| `chaos-repeat/api/applyChaosRepeat.ts` | `POST /api/v1/crafting/workbench/chaos-repeat` | 제작작업대 `ChaosRepeatController` → `ChaosRepeatService`: 동기 반복 제작 |
| `useItemTextImport.ts` → `itemTextApi.ts` | `POST /api/v1/items/parse` | 공용 `ItemTextController` → `ItemTextService`: 원문과 미해석 행을 보존하는 표시용 분석 |
| `goal-filter/CraftStart.tsx`, `ConnectedGoalFilter.tsx` | `GET /api/v1/crafting/workbench/registry` | 제작작업대에 남긴 기존 조회 주소. 실제 재료 정의 로더·카탈로그는 공용이며 제작지원도 조회 |
| `goal-filter/api.ts`, `numericApi.ts` | `/api/v1/crafting/support/goal-filters/{catalog,validate,evaluate,recommend}` | 제작지원 `goal`: 목표 조건 조회·검증·평가·추천 |
| `method-search/automaticApi.ts`, `AutomaticAbyssRoutes.tsx` | `/api/v2/crafting/method-searches/automatic/**` | 제작지원 `search`: 현행 자동 탐색 작업·저장·재개·심연 경로 |
| `method-search/api.ts`, `certifiedApi.ts` | `/api/v2/crafting/method-searches/**` | 제작지원 `search`: 기존 method·indexed·certified 조회와 제어 계약 |
| `path-tree/api.ts` | `/api/v1/crafting/path-searches/**` | 제작지원 `search`: 기존 v1 경로 탐색 작업 |
| `basic-paths/api.ts` | `/api/v1/crafting/basic-paths/**`, `/api/v2/crafting/method-searches/provenance` | 제작지원 `path`·`search`: first-hit·회복 계산과 규칙 출처 조회 |
| `supportApi.ts` | `/api/v1/crafting/support/{families,assess,recommend}` | 제작지원 `recommendation`: 기존 family 평가·추천 계약 |
| `CraftingExplorer.tsx` → `craftingApi.ts` | `/api/v1/crafting/{actions,transitions,explore}` | 제작지원 `path/LegacyGraphController`: 기존 Explorer 호환 계산 |

HTTP 주소에 `workbench`가 있다고 해서 카탈로그 구현을 제작지원에 복사하지 않는다. 구형 API도 호출 코드와 계약이 남아 있으므로 이번 구조 변경에서 삭제하지 않는다.

## 인터페이스와 의존 방향

제작작업대와 제작지원은 서로의 내부 클래스에 의존하지 않는다. 둘 다 공용 기능과 운영 설정을 사용하고, `bootstrap`이 실제 구현을 연결한다.

- `CraftingCatalogs`: 검토 카탈로그를 읽는 공용 인터페이스.
- `CraftingOperations`: 동일한 제작 규칙으로 시작 상태·단일 제작·심연 제작·품질 표시를 실행하는 공용 인터페이스. 실제 구현은 `CatalogCraftingService`이며 `bootstrap`에서만 선택한다.
- `AutomaticRuntimeFactory`: 자동 작업이 현행·과거 목표 조건의 실행 환경을 복원하는 경계. 번들 데이터 선택은 `BundledSearchRuntimeFactory`가 담당한다.
- `AutomaticJobRepository`, `CertifiedJobRepository`, `MethodGraphStore`, `MethodCursorStore`, `AdditionPoolStore`: 파일·SQLite·PostgreSQL 접근과 계산 흐름을 분리하는 기존 인터페이스.
- `MethodSearchEngine`: 기존 탐색 엔진들의 공통 실행 계약. 알고리즘 의미나 지원 범위를 이번 작업에서 변경하지 않는다.

`scripts/wire-*`, `scripts/prepare-*`, `scripts/qa` 등의 기존 일회성 패치 스크립트는 당시 체크아웃·로컬 QA 자료를 전제로 한다. 이들은 과거 패치의 원본으로 보존하며 새 디렉토리에 적용하는 일반 개발 도구로 사용하지 않는다. 현재 코드 탐색과 이후 변경의 이동 기준은 이 문서를 따른다.

`ArchitectureTest`가 서비스 간 직접 의존, 공용 코드에서 서비스로 향하는 의존, 순환 참조, domain의 저장소·HTTP 의존, 리소스 봉인 우회를 검사한다. 클래스마다 형식적인 `Impl` 파일을 만들지는 않는다.

## 설정을 바꾸려면

[src/main/resources/backend-settings.yaml](src/main/resources/backend-settings.yaml)을 수정한다. **각 값 바로 위에 의미와 사용하는 Java 파일을 적었다.** 작업 수·보관 시간·계산 실행 예산·캐시 크기·작업 저장 위치·카오스 반복 동시 실행 수를 기능별로 찾을 수 있다.

`application.yaml`이 이 파일을 가져오고 `configuration/BackendSettings.java`가 타입과 유효 범위를 검사한다. 잘못된 0 용량이나 음수 시간은 시작할 때 거부한다. 코드에서 직접 생성하는 테스트도 같은 YAML 기본값을 읽으므로 별도의 기본값 목록을 유지하지 않는다.

설정 변경은 서버 재시작 후 적용된다. Spring의 외부 설정으로 덮어쓸 수도 있다. 예를 들어 `backend.path-search.max-jobs`의 환경변수 이름은 `BACKEND_PATHSEARCH_MAXJOBS`다. 기존 `crafting.method-job-directory` / `CRAFTING_METHOD_JOB_DIRECTORY`도 계속 지원하며, 새 `backend.storage.method-job-directory`를 지정하면 새 설정이 우선한다. 저장 경로를 바꾼다고 기존 파일을 자동으로 옮기지 않는다.

DB·Redis 연결과 비밀값은 기존 `application.yaml`·환경변수에서 관리한다. 게임 확률·weight·옵션 수는 `src/main/resources/catalog`·`crafting`의 봉인된 게임 데이터다. API의 허용 범위, 버전 문자열, 저장 스키마, 수학적 알고리즘 내부 상수도 운영 설정과 구분한다.

## 이후 변경을 합칠 때

이번 작업 기준은 `9751cd7c`다. 다른 작업의 변경은 이 브랜치에 자동으로 포함하지 않았다. 이후 변경은 파일의 이름보다 역할을 먼저 확인하고 해당 기능으로 옮긴다.

| 기존 위치·이름 | 현재 위치·이름 |
|---|---|
| `item`, `item.testparser` | `shared.item`, `shared.item.text` |
| `crafting.application.WorkbenchService` | `shared.crafting.application.CatalogCraftingService`와 `CraftingOperations` |
| `WorkbenchService.automaticKernel` | `craftingsupport.search.application.AutomaticKernelFactory.create` |
| `crafting.domain`의 공용 제작 규칙 | `shared.crafting.domain` |
| `AutomaticCraftingKernel`, `AbyssStrategyPlanner`, `domain.pathsearch` | `craftingsupport.search.domain` |
| `application.pathsearch`, 작업 저장 구현 | `craftingsupport.search.application`·`infrastructure` |
| `*.goalfilter` | `craftingsupport.goal` 아래 각 역할 |
| `application.chaosrepeat`, `application.chaosdisplay` | `workbench.repeat`, `workbench.display` |
| `presentation.WorkbenchController`, `ChaosRepeatController` | `workbench.api` |
| `presentation.CraftingController` | 공용 `CraftingCatalogController`와 제작지원 `LegacyGraphController` |
| `SupportController`, `SupportRecommendations` | `SupportAssessmentController`, `SupportRecommendationService` |
| `bootstrap.CraftingConfiguration`, `SupportConfiguration` | `CatalogConfiguration`, `CraftingSupportConfiguration`, `LegacyGraphConfiguration` |
| `ExactNumericDistribution.Fraction` | `shared.probability.Fraction` |

규칙 JSON·계약 fixture·DB migration·프론트 코드는 구조 변경을 위해 재작성하지 않는다. 새로운 운영 설정을 추가하면 YAML의 의미·소비 파일 주석, 타입 검증, 실제 소비 코드도 함께 추가한다.

## 검증

`backend`에서 `./gradlew check generateJooq bootJar`를 실행한다. Docker가 필요한 통합 검사는 임시 PostgreSQL·Redis를 사용하며 기존 개발 DB를 초기화하지 않는다. 기존 실패가 있으면 기준 커밋과 비교해 구분하고, 기대값을 완화하거나 테스트를 제외해 성공으로 표시하지 않는다.
