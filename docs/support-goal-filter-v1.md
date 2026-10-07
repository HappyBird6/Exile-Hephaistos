# Support 목표 필터 공통 계약 v1

이 문서는 FE mock과 BE 구현이 공유하는 1차 계약이다. 기준은 master `7d27d6ce0a9fe35ab8f7d5dad15366416c10c725`이며, 별도 검증 branch `codex/support-goal-verification`의 `ebde5eaa`는 이 계약의 기반에 합치지 않았다. 현재 실행 코드의 완성이나 전체 base 지원을 의미하지 않는다. 계약 데이터는 `contracts/support-goal-filter-v1/fixtures.json`에 있다.

## 화면과 지원 범위

왼쪽 일반 필터는 선택 base, item level 범위, rarity를 제공한다. 오른쪽은 능력치 검색·추가, 행별 min/max, AND/NOT/IF/COUNT/WEIGHTED_V1/WEIGHTED_V2 그룹이다. 그룹 종류 변경, 활성화, 접기, 삭제를 제공한다. 제품 문구는 영어로 작성한다. 가격, 판매자, 거래 상태, 엔드게임 전용 항목, legacy 시련 패시브 및 용병 그룹은 제공하지 않는다. 거래소 URL import/export는 2차 범위다.

능력치는 번들 제작 catalog와 선택 base에 적격인 정의에서만 얻는다. 거래소 전체 목록이나 미푸시 134 bases의 존재를 가정하지 않는다. 선택 base 미지원은 빈 목록과 이유를 표시하며 Solar로 바꾸지 않는다. 검색 결과는 level, layer, 제작 후보 적격성, 실제 현재 아이템 존재를 구분한다. 현재 존재하는 속성을 새 생성 level 조건 때문에 삭제하지 않는다. base 변경 시 기존 목표를 보존하고 부적격 행을 진단한다.

## AST와 판정

`GoalFilter`는 `{version:1,catalogVersion,general,groups}`다. `general`은 `{baseItemId,itemLevel:{min,max},rarities:[...]}`다. 실제 `ItemState`의 base/level/rarity와 비교한다. `catalogVersion`은 제공 catalog의 불변 버전이며 불일치는 409다. `Group`은 `{id,type,disabled,range,entries}`이며, `Entry`는 `{id,statId,unit,range,weight,disabled}`다. COUNT 그룹 range의 unit은 count, weighted는 score이며 AND/NOT/IF의 range는 null이다. 행 unit은 catalog unit과 일치해야 한다. range는 `{min:number|null,max:number|null}`이며 양끝 포함, null은 제한 없음이다. 양끝 null은 존재 조건이다. weight는 weighted에서 필수 유한 수이며 음수와 0도 허용한다. 그 외 그룹에서는 null이다. 접기는 FE 전용 `collapsedByGroupId`이며 판정 AST에 넣지 않는다. 중첩 그룹은 v1에서 허용하지 않는다.

최상위는 일반 필터 AND 모든 활성 그룹이다. 활성 행은 stat이 존재하고 값이 range 안에 있으면 충족한다. 값 0은 존재이며 부재는 0과 다르다. 같은 stat의 여러 modifier 기여는 catalog 규칙에 따라 먼저 집계한다. AND는 모든 행 충족, NOT은 어느 행도 충족하지 않음, IF는 각 행이 부재이거나 범위 충족, COUNT는 충족 행 수가 그룹 range 안에 있음이다. COUNT는 family 수가 아니라 행 수를 센다. 동일 그룹에서 동일 statId 중복은 422이며 그룹 간 중복은 허용한다.

WEIGHTED_V1은 먼저 각 행에 IF를 적용하고, 존재하는 행의 `값 × weight` 합이 그룹 range 안이면 충족한다. 모든 행이 부재이면 합 0을 range와 비교한다. WEIGHTED_V2는 활성 행 중 하나 이상의 stat이 존재해야 하며, 존재하고 행 range에 맞는 행의 `값 × weight`만 합산한다. 부재나 범위 이탈은 기여 0이다. 하나가 존재하지만 모두 범위 밖이면 합 0도 검사한다. 정규화·clamp·임의 반올림을 하지 않는다. 서로 다른 단위를 합하는 것은 사용자가 지정한 score이며 게임 내 합산 단위가 아니다.

GGG 직원 Novynn의 2025-02-04 설명을 기준으로 두 버전의 차이를 정했다: [Weighted Sum V1 vs Weighted Sum V2](https://www.pathofexile.com/forum/view-thread/3665545). 게시글의 일반 사용자 추측을 공식 정의로 사용하지 않는다. 존재 여부 옵션, boolean stat, multi-stat 효과, 조건부 효과 등 이 계약에서 근거가 확정되지 않은 항목은 `UNSUPPORTED`와 필요한 증거를 반환하며 숫자로 바꾸지 않는다. 이는 위 명시 범위의 자체 구현 계약이며 현재 거래소와 모든 edge case가 일치한다는 주장은 아니다.

disabled 그룹 및 행은 판정에서 제외한다. 전부 disabled면 `EMPTY_GOAL` 422이며 활성 그룹에 활성 행이 없으면 `EMPTY_GROUP` 422다. disabled 내용은 보존하고 알려지지 않은 stat의 지원 경고는 반환하되 계산을 차단하지 않는다. 구조·버전·ID 중복·유한 수·range 역전 검사는 disabled에도 적용한다. COUNT range는 0 이상 정수이고 max는 활성 행 수 이하여야 한다. 그룹 종류 변경 시 FE는 range/weight를 새 종류에 맞게 편집하고 변경 전 값은 로컬에서 보존할 수 있다.

판정은 `MATCH | NO_MATCH | UNKNOWN | UNSUPPORTED`다. 알려지지 않은 값은 부재로 취급하지 않는다. 완전 검증된 아이템에서만 부재를 확정한다. AND에 확정 false가 있으면 NO_MATCH, 그렇지 않으면 UNSUPPORTED, UNKNOWN, MATCH 순으로 결정한다. NOT은 확정 true가 있으면 NO_MATCH이며 나머지도 같은 우선순위다. COUNT는 unknown 행의 가능한 count 구간으로 확정 여부를 판단하고, weighted는 증명된 score 경계가 없으면 UNKNOWN/UNSUPPORTED다. 설명에는 그룹·행별 상태, 실제값 또는 null, 존재 `PRESENT | ABSENT | UNKNOWN`, score/count, 원인 code와 JSON pointer를 남긴다.

## Catalog와 pseudo

catalog 응답은 `{version:1,catalogVersion,context,groupTypes,stats,issues}`다. context는 snapshot/base/itemLevel, groupTypes는 각 type의 evaluation/probability 지원 상태와 reasonCode를 가진다. stat은 `{statId,label,unit,kind,support,eligible,eligibilityReason,sourceStatIds,contributions,sourceUrls}`다. kind는 `EXPLICIT | IMPLICIT | PSEUDO`, support는 `{evaluation:"SUPPORTED"|"UNKNOWN"|"UNSUPPORTED",probability:동일 enum,reasonCode:string|null}`이다. statId는 자체 버전형 식별자이며 GGG ID로 가장하지 않는다. catalog 원본 stat 단위와 표시 단위의 변환은 명시적인 유리수 scale로 등록한다. 알 수 없는 변환은 UNSUPPORTED다.

회귀용 `fixture:explicit.cold_resistance` 및 `fixture:explicit.all_elemental_resistance`는 percent 단위다. `fixture:pseudo.total_cold_resistance`는 두 원천 stat에 coefficient 1을 적용해 합한다. 냉기 10 + 모든 원소 12 = 22다. 원천 modifier/stat/layer별로 한 번만 기여하고 pseudo 결과를 다시 원천으로 더하지 않는다. 모든 원천이 부재이면 pseudo도 부재, 하나라도 존재하면 0 합이어도 존재, 하나라도 미확정이면 값 UNKNOWN이다. fixture ID는 합성 테스트 전용이다. BE가 실제 번들 stat ID·단위·layer 매핑을 증명하고 production statId를 catalog에 노출해야 한다. hybrid/joint-stat 분포를 독립 roll로 분해하지 않는다.

## API

새 namespace는 `/api/v1/crafting/support/goal-filters`다. 기존 `/families`, `/assess`, `/recommend`와 `required + candidates + candidateCount`, minimumTier 및 N-of-M 동작은 그대로 유지한다. 기존 목표를 숫자 AST로 자동 변환하지 않는다.

| Method와 경로 | 요청 | 응답 |
|---|---|---|
| GET `/catalog?snapshotId=...&baseItemId=...&itemLevel=82` | 선택 context | 위 Catalog |
| POST `/validate` | `{context,goal}` | `{version:1,valid,issues,capabilities:{evaluation,probability}}` |
| POST `/evaluate` | `{item,goal}` | `{version:1,catalogVersion,status,generalStatus,groups,issues}` |
| POST `/recommend` | `{item,goal,activeOmens,limits:{maxStates,maxEdges,maxMillis}}` | 아래 Recommendation |

`item`은 기존 전체 ItemState JSON이며 explicits는 modifierId와 실제 values map을 보존한다. Bucket은 이 API의 수치 판정 입력이 아니다. `context`는 `{snapshotId,baseItemId,itemLevel}`이며 요청 item과 goal/general/base가 다르면 validation issue를 반환한다. stat unit과 rule mapping은 서버가 검증하며 FE가 계산한 합을 신뢰하지 않는다. fixture의 `observedStats`는 evaluator unit test 전용이며 HTTP 요청으로 받지 않는다. mock은 fixture의 `apiExamples`와 동일한 wire shape를 사용하고 실제 ItemState fixture는 BE가 번들 catalog로 추가한다.

issue는 `{code,path,message,severity:"ERROR"|"WARNING"}`이고 message는 영어다. malformed JSON/enum은 400, 구조·range·unit·item validation 오류는 422 Problem Details, catalogVersion 불일치는 409, 서비스 실패는 503이다. 정상 구조에서 미지원 capability는 200과 지원 상태를 반환한다. unknown statId는 `UNKNOWN_STAT` 422, 알려졌지만 근거가 부족한 stat은 capability UNSUPPORTED다. 오류는 원문·SQL·stack trace를 노출하지 않는다. FE는 AbortSignal과 요청 identity로 늦은 응답을 폐기하고 입력·focus를 보존한다.

Recommendation은 `{version:1,catalogVersion,evaluation,probability,comparisons,rankingCertified,comparedSequences,totalSequences}`다. probability는 `{status:"COMPLETE"|"PARTIAL"|"UNKNOWN"|"UNSUPPORTED",reasonCode,modelVersion,ledgerVersion}`다. COMPLETE/PARTIAL의 comparison은 `{sequence,successLower,successUpper,failureProbability,unresolvedProbability,complete}`이고 기존 WorkbenchAction 문자열을 사용한다. 질량은 lower + failure + unresolved = 1, upper = lower + unresolved다. PARTIAL은 검증된 모델에서 budget 때문에 남은 질량이며 rankingCertified=false다. UNKNOWN은 분포 근거/자료 부족, UNSUPPORTED는 알고리즘/효과 미구현이다. 이 둘은 comparisons=[], rankingCertified=false, comparedSequences=0, totalSequences=null이며 0%나 추정 확률을 제공하지 않는다. 가능한 bounds가 없어도 임의 [0,1] comparison을 만들지 않는다.

현재 occupied/qualified DP 및 StateBucket은 explicit roll을 주변화한다. 수치 판정이 정확해도 수치 추천 확률은 `UNSUPPORTED / NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED`다. BE는 기존 addition modifier 선택 분포에 실제 roll 분포 및 pseudo 기여 상관을 연결하는 별도 projection을 검증한 뒤만 COMPLETE/PARTIAL을 노출한다. cache key는 snapshot/base/rules/ledger/AST/catalog 및 roll projection 버전을 포함해야 한다. uniform integer ledger의 단일 stat 적용 범위를 넘겨 다중 stat·derived·조건부 값에 확대하지 않는다. 기존 family/minimumTier 엔진과 예산 질량·first-hit 흡수 회귀는 유지한다.

## 파일 소유권과 통합

계약 담당은 이 문서와 `contracts/support-goal-filter-v1/**`만 소유한다. FE와 BE는 계약 commit에서 시작하고 변경 제안은 계약 담당에게 반환한다.

| 담당 | 독립 소유 파일 |
|---|---|
| FE | `frontend/src/features/crafting/goal-filter/**`의 types, API client, mock, Zustand editor, Query hook, 컴포넌트, 전용 CSS 및 tests |
| BE | `backend/src/main/java/com/poe2craft/crafting/domain/goalfilter/**`, `application/goalfilter/**`, `presentation/goalfilter/**`, `infrastructure/goalfilter/**`; 대응 test 경로 및 `backend/src/main/resources/crafting/goalfilter/**` |
| 통합 담당 | 기존 `CraftSupport.tsx` 연결, bootstrap 등록 및 필요한 ArchUnit 경계 조정; 변경 전 파일별 부모 통지 |

공용 위험 파일은 `StateBucket.java`, `item/ItemState.java`, `item/ItemStateValidator.java`, `bootstrap/SupportConfiguration.java`, `crafting/domain/SupportGoals.java`, `crafting/application/SupportRecommendations.java`, `frontend/src/features/crafting/supportApi.ts`, `CraftSupport.tsx`, `craft-support.css`, `frontend/src/app/App.tsx`, `frontend/src/shared/i18n/**`, `backend/src/main/resources/crafting/registry-v*.json`, base catalog, 공용 CSS 및 lockfile이다. 수정이 필요하면 정확한 경로와 이유를 먼저 부모에게 알리고 단일 담당을 배정한다. 고장 PC의 미푸시 base/registry/i18n/Workbench UI 변경과 병합 전에 대조한다. 새 기능 문구는 FE 전용 영어 파일에 둔다. 공용 번역사전과 registry를 이 작업에서 확장하지 않는다.

이번 단계는 문서·fixture·fixture 자체 검증과 자체 리뷰만 한다. 서버·Docker·DB·Redis를 교체하지 않으며 live8081/API8080/Redis6381 및 volume을 보존한다. FE는 mock 기반으로 위 화면과 접근성·취소 회귀를, BE는 실판정·catalog 적격성·숫자 분포 지원 차단 및 기존 family 회귀를 검증한다. 통합 후에 연결된 영역의 정식 검사를 실행한다.


## Numeric addition model (2026-10-07)

무품질 Solar 단일 stat catalog는 `solar-numeric-addition-v1` / `uniform-integer-roll-v1` 모델로 수치 추천을 지원한다. Transmutation, Augmentation, Regal, Exalted 및 각 Greater/Perfect variant의 기존 AdditionRules/AdditionTransitions 후보 조건과 catalog weight를 그대로 사용한다. 후보 modifier의 선택 질량은 weight / 전체 적격 weight이고, 선택된 단일 stat의 min..max 정수 roll은 모델 내 균등이다. 목표와 무관한 후보도 분모에 포함한다. 이는 실제 게임 roll 분포 검증 결과가 아니며 API interpretation과 UI에서 분리 표시한다. 공통 ratio/HALF_UP 10001ticks multi-stat 정책은 사용하지 않는다.

NumericAdditionKernel은 StateBucket을 후보 선택에만 사용하며 implicits, explicits의 전체 values, rarity, snapshot/base/level, conditions, sockets를 보존한다. NumericAdditionSearch는 전체 ItemState 동일성으로 질량을 합산하고 GoalEvaluator와 ItemStatProjection을 사용한다. 시작 상태와 각 단계에서 최초 MATCH 질량을 흡수한다. 각 rarity에서 허용되는 추가 행동의 모든 유한 prefix(빈 Stop 포함)를 길이 순서로 비교하며 최대 explicit 6개에서 종료한다. 후보가 없는 실제 단계는 illegalActionFailure, 마지막 단계의 미성공은 terminalFailure로 분류한다. 제거/복구/반복정책과 Annul/Chaos는 후속 범위다.

maxStates는 평가할 전체 수치 상태 수, maxEdges는 생성할 modifier/roll edge 수, maxMillis는 계산 시간의 요청 전체 예산이다. 도중 중단된 정규화 kernel의 미발행 질량과 미평가 상태 질량은 unresolved다. 모든 비교에서 exact success + failure + unresolved = 1, upper = success + unresolved가 성립한다. 미탐색 순서는 comparedSequences에 포함하지 않으며 totalSequences로 공개한다. PARTIAL의 rankingCertified는 false이고 정렬은 확인된 lower 질량 순서일 뿐 확정 추천 순위가 아니다. exactMass는 exact rational 분자/분모 및 terminal/illegal 실패 분류를 제공한다. 숫자 probability 필드는 화면 표시용 근삿값이다.

품질, 특수 상태/효과, fractured, omens, multi-stat, 다른 base 또는 미검토 unit/effect 목표는 이유 code와 함께 UNSUPPORTED다. 이는 각각 명시된 구현 범위이며 게임 분포 증거 부족과 구현 미완료를 혼동하지 않는다. 별도 model ledger는 `backend/src/main/resources/crafting/goalfilter/numeric-addition-ledger-v1.json`에 보존한다. cache는 현재 사용하지 않는다.


## Frontend Docker build

`goal-filter/mock.ts`와 관련 TypeScript 테스트는 `../../../../../contracts/support-goal-filter-v1/fixtures.json`을 참조한다. Docker build의 frontend 루트는 `/app`이므로 해당 경로는 `/contracts/support-goal-filter-v1/fixtures.json`으로 해석된다. `infra/docker/frontend.Dockerfile`은 빌드 단계에 이 JSON 파일 하나만 복사한다. 다른 contracts 파일이나 비밀값을 추가하지 않으며 최종 nginx 이미지에는 기존과 같이 dist만 전달한다. 원본 Dockerfile로 직접 빌드할 수 있어 임시 Dockerfile 우회는 더 이상 필요하지 않다. 이 보완은 제품 동작이나 실행 중인 서비스 교체를 포함하지 않는다.
