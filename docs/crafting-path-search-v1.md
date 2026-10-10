# 제작 경로 검색 계약 v1

기준 코드: `15f02631df176aa191e95d1f19bb62110be7796e`. 이 문서는 FE/BE 병렬 구현을 위한 **미구현 API 계약**이다. 기존 endpoint의 동작 변경이나 새 엔진의 실행 검증을 뜻하지 않는다. 공유 파일은 [contracts/crafting-paths-v1](../contracts/crafting-paths-v1/README.md)에 있다. JSON Schema의 명명된 `definitions`가 endpoint별 wire shape이며, 아래 의미 검증도 필수다.

## 첫 수직 슬라이스

시작 아이템과 숫자 목표만 제출하면 서버가 고정 후보군을 만들고, 위에서 아래로 분기·합류하는 경로와 관측점별 추천을 반환한다. 제품에 action 편집기, policy JSON, 내부 ID, 개발 조작부, 진단 덤프를 노출하지 않는다. UI는 기존 6개 언어를 유지한다. source와 모델 해석은 사용자에게 필요한 간결한 문구로 제공한다.

첫 지원은 Solar Amulet, Rare, nonfractured explicit 정확히 1개, catalystQuality=null, conditions=[], activeOmens=[]이다. 전체 ItemState·실제 implicit/source roll·socket의 알려짐/모름은 보존하고 기존 validator로 검증한다. 새 생성 후보는 전부 single-stat source 정수 분포여야 한다. 후보 일부가 joint/미지원이어도 제거하거나 분모를 바꾸지 않는다.

목표는 현재 catalogVersion의 기존 GoalFilter AST다. 검토된 14 source/6 pseudo 중 선택 base에 적용되는 항목과 AND/COUNT를 지원한다. OR는 기존 COUNT `{min:1,max:null}` 표현이다. NOT/IF/weighted AST는 저장·입력 형태를 보존하지만 v1 검색에서는 미지원으로 보고한다. disabled 내용의 기존 구조 검증은 유지한다. 추가 화폐 12종 전체, Normal/Magic/복수 explicit 시작, 임의 상태 의존 정책, 새로운 base/roll 효과는 후속 capability다. 현재 선택 가능한 134 bases를 모두 검색 지원한다고 표시하지 않는다.

production `candidateSetVersion = solar-one-explicit-candidates-v1`은 아래 **6개** fixed policy를 모두 비교한다. 생성 순서는 아래 순서이고, 노출 ID는 서버가 안정적으로 부여한다. ID를 사용자 문구로 쓰지 않는다.

| actions | mode |
|---|---|
| CHAOS | REPEAT_CYCLE |
| GREATER_CHAOS | REPEAT_CYCLE |
| PERFECT_CHAOS | REPEAT_CYCLE |
| ANNULMENT, EXALTED | REPEAT_CYCLE |
| ANNULMENT, GREATER_EXALTED | REPEAT_CYCLE |
| ANNULMENT, PERFECT_EXALTED | REPEAT_CYCLE |

각 action은 기존 Workbench 계획과 availability를 따른다. 적용 불가능하면 해당 정책에서 종료하며 다른 action으로 자동 우회하지 않는다. 모든 화폐 적용 1번을 attempts 1로 센다. Annul→Exalted 한 주기는 2회이며 중간 상태도 목표를 평가한다. 성공하면 다음 action 전에 흡수한다. 100/300/500은 관측점이며 총 사용 횟수 제한이 아니다. 최대 횟수·게임 예산 필드는 없다.

현재 일반 Chaos renewal은 modifier-ID 목표만 지원한다. 위 숫자 목표와 다른 후보에 대한 renewal 증명은 BE 구현 과제다. 후보별 전체 지원 preflight, 동일 empty-state 복귀, 모든 목표 판정과 phase를 증명한 경우에만 shortcut을 사용한다. 증명이 안 되면 정확한 일반 전파 또는 정직한 unresolved 결과를 제공한다. 기존 전이 1,000개 한도를 단순히 높인 것만으로 완료하지 않는다.

첫 출시 완료에는 실제 Solar source fixture의 숫자 목표를 **완전 계산하는 경로**와 자동 비교가 필요하다. 모든 입력이 항상 완료된다는 약속은 아니다. Alchemy/Divine/Fracturing/Artificer 및 보류 화폐·특수 복구 화폐는 이 계약의 실행 범위가 아니다.

## API와 최소 payload

namespace는 `/api/v1/crafting/path-searches`. 이 계약은 기존 `/basic-paths`, `/support/goal-filters`를 제거하거나 변경하지 않는다. 아래 schema 정의는 `schema.json#/definitions/<이름>`을 뜻한다.

| Method | 경로 | 요청 정의 | 응답 정의 |
|---|---|---|---|
| POST | `/` | createRequest | 202 jobSnapshot |
| GET | `/{jobId}` | 없음 | 200 jobSnapshot |
| GET | `/{jobId}/graph?revision=N&cursor=...` | opaque cursor, 최초 cursor 생략 | 200 graphPage |
| POST | `/{jobId}/cancel` | mutationRequest, operation=CANCEL | 200 jobSnapshot |
| POST | `/{jobId}/resume` | mutationRequest, operation=RESUME | 202 jobSnapshot |
| POST | `/{jobId}/recoveries` | recoveryRequest | 202 jobSnapshot |

모든 POST는 기존 `X-Crafting-Ruleset`이 필수다. start.provenance는 현재 `/basic-paths/provenance`에서 얻는 실제 provenance이며 body/header/root identity가 같아야 한다. goal.catalogVersion은 같은 ruleset/context에서 읽은 목표 catalog 응답 값이다. 서버는 claimed metadata만 믿지 않고 현재 내용과 비교한다. 초기 queued 응답에도 job identity와 현재 provenance, capability를 제공한다. 정상 구조지만 capability가 미지원이면 202의 `UNSUPPORTED` terminal job으로 반환하고 recommendations/rankings는 빈 배열이다. 성공률 0 또는 임의 성공 예제를 만들지 않는다.

createRequest의 observations는 canonical nonnegative decimal **문자열만** 받는다. 1~32개의 중복 없는 값이고 Long 범위 이하여야 한다. 응답은 오름차순이다. 기본 FE 요청은 `["100","300","500"]`, 기본 선택은 100이다. source-unit item 값은 기존 JSON 정수 shape를 쓰되 v1 transport는 JavaScript 안전 정수 범위를 벗어나는 입력을 거부한다. 문자열·float를 정수로 보정하지 않는다.

clientRequestId는 요청 재시도용 opaque key다. 같은 key+같은 요청은 TTL 내 같은 job을 반환한다. 같은 key+다른 요청은 409 `REQUEST_ID_REUSED`다. 다른 사용자의 요청과 임의로 공유하는 전역 key로 구현하지 않는다. requestFingerprint는 서버가 계산하며 UI 신원/인증 토큰이 아니다. goal AST의 의미 있는 필드와 원본 start/provenance, 관측점, 후보군·predicate·search 버전을 포함한다. FE가 보낸 정책은 받지 않는다.

`capabilities`는 이 요청의 평가/검색 지원, 이유 code, 이 후보군이 쓰는 action 및 goal group 종류, conditionalRecovery/resume 지원 여부다. 새 기능을 필드만 추가하여 활성화하지 않는다. v1의 지원 여부를 먼저 변경하고, 새 wire shape가 필요하면 계약 버전을 변경한다. 향후 기능용 거대한 nullable DTO나 임의 확장 map은 없다.

## 목표·숫자 분포·질량

GoalValidator, ItemStatProjection, GoalEvaluator를 공통 predicate로 연결한다. full item마다 `MATCH → hit`, `NO_MATCH → 진행`, `UNKNOWN/UNSUPPORTED → unresolved`다. 기존 range, unit, 부재와 0, source layer 및 pseudo 집계 의미를 그대로 사용한다. 전체 요청의 정적 미지원과 전파 중 발견한 미확정 질량은 구분한다.

숫자 평가 지원은 roll 분포 지원을 뜻하지 않는다. weight는 PoE2DB published 값/전체 eligible weight다. 누락 weight에 일괄 1/n fallback을 적용하지 않는다. removal의 uniform 1/N, single-stat의 uniform integer roll은 현재 ledger에 명시된 모델이며 실제 게임 확률 검증 주장과 다르다. multi-stat의 독립 roll 분해나 다른 opt-in 모델의 자동 적용은 금지한다.

fraction은 기약분수 `{numerator:"...",denominator:"..."}`이며 denominator>0, 0<=numerator<=denominator다. 표시용 반올림을 비교/계산에 쓰지 않는다. 각 point에 다음이 성립한다.

- lower = 해당 횟수까지 처음 성공한 질량.
- active = 해당 횟수까지 계산했지만 아직 성공하지 않은 진행 질량.
- dead = 그 정책에서 더 진행하지 않는 질량. 전역 복구 불가능을 뜻하지 않는다.
- unresolved = 미지원 목표/전이 또는 계산 중단으로 해당 횟수 결과가 확정되지 않은 질량.
- `lower + active + dead + unresolved = 1`, `upper = lower + unresolved`.
- unresolved=0이면 COMPLETE, 양수이고 lower>0이면 PARTIAL, 양수이고 lower=0이면 UNKNOWN.

전이 확률, 한 번의 유한 경로를 따라갈 확률, CDF(n), eventual success를 혼동하지 않는다. recommendations.points는 CDF다. eventual은 증명된 경우만 probability와 proofVersion을 함께 제공하며 그 외 둘 다 null이다. 주어진 관측점에서 COMPLETE라도 eventual은 UNKNOWN일 수 있다. 독립시행 공식은 renewal 증명 없이 적용하지 않는다. 완전히 닫힌 chain이나 증명 없는 거대 관측점으로 최종 확률 1을 추정하지 않는다.

## 그래프와 상태 재사용

nodes는 lossless full ItemState다. node ID는 job의 고정 provenance와 canonical full state로 정하며 digest 충돌 시 실제 동등성도 확인한다. modifier/map/set 순서의 의미 없는 차이만 정규화한다. 목표에 없는 stat도 pool/family/슬롯/제거/복구에 영향을 주면 버리지 않는다.

executions는 `(stateId, policyId, phase)`의 고유 identity다. 서로 다른 phase의 질량을 합하지 않는다. 같은 full state는 여러 execution에서 공유한다. phase는 해당 policy의 다음 action index이며 loop는 phase를 되돌린다. edges는 execution 사이의 **조건부** 전이 확률이고 정책 선택 자체에 확률을 붙이지 않는다. 서로 다른 정책의 edge를 더해 1을 만들지 않는다. 동일 source/action/destination execution으로 가는 elementary 결과는 합산하여 한 edge로 만든다.

expansions는 해당 execution의 열거 상태다. COMPLETE에서는 outgoing 합=1, PARTIAL에서는 outgoing 합+unresolved=1, UNSUPPORTED에서는 outgoing 없음/unresolved=1이다. UNAVAILABLE은 outgoing 없음/unresolved=0이며 정책 종료다. terminal hit/정책 종료 노드는 expansion이 필요 없다. expansion이 없다는 사실은 0%나 완료를 뜻하지 않는다. renewal shortcut으로 CDF를 증명했더라도 화면용 미열거 분기를 fabricated node로 채우지 않는다.

graphPage는 고정 revision에 대한 누적 그래프의 분할이다. 첫 snapshot에는 root와 가능한 상위 결과를 우선 제공한다. 이후 페이지는 이전 페이지에 나온 ID를 참조할 수 있다. 같은 ID 재등장은 내용이 같아야 하고 FE는 중복 반영하지 않는다. expansion completion은 관련 outgoing edge가 모두 전달된 페이지에서만 발행한다. nextCursor=null은 그 revision에서 보관된 표시 그래프의 끝이며 전체 가능한 상태의 열거 완료가 아니다.

화면은 state 공유를 유지하며 REPEAT edge를 접힌 반복 표시로 그린다. 화면상 상태 묶음은 계산상의 상태 합류 증명이 아니다. 계산 bucket 압축에는 모든 허용 action에 대한 bucket별 전이 확률·availability와 목표 판정, 정확한 checkpoint 구분의 동등성 증명이 필요하다.

## 추천 범위와 순위

candidateScope의 total은 이 버전에서 비교할 유한 정책 수다. generated는 실제 생성한 정책 수이고 enumerationComplete는 후보 생성의 완료다. recommendations는 실제 평가한 후보이며 미평가 후보를 0%로 채우지 않는다. v1 최대 6개 후보 결과를 보관하고 FE는 선택 관측점의 상위 최대 5개를 표시한다. 유효한 후보가 3개 미만이면 실제 개수만 표시한다. 이미 root가 MATCH이면 즉시 달성 표시를 우선하고 불필요한 제작을 권하지 않는다.

rankings는 관측점마다 독립적이다. v1은 보수적으로 **후보군 전부 생성·평가되고 해당 관측점이 모두 COMPLETE일 때만** CERTIFIED_WITHIN_CANDIDATES다. 부분 계산은 lower 내림차순 PROVISIONAL이며 순위 확정을 주장하지 않는다. exact lower 동률은 공동 순위(1,1,3), 동일 순위 내 policy ID 순서다. 부분 lower 동률은 동등한 실제 확률의 증명이 아니다. 비교 불가하면 UNAVAILABLE과 빈 entries다. UI가 표시하지 않는 추가 정책 때문에 확정 조건을 완화하지 않는다.

표현: “현재 비교한 경로 중 추천”, “100회 이내 달성 확률”, “부분 계산”. 전체 상태 의존 정책을 탐색한 전역 최적·최저 비용이라고 하지 않는다. 가격 모델이나 횟수 상한은 추가하지 않는다.

## 별도 조건부 복구

처음에는 **정확한 이전 상태 선택**만 지원한다. recoveryRequest는 parentRevision에 존재하는 failureExecutionId와 checkpointStateId를 참조한다. 서버는 failure가 NO_MATCH인 실제 도달 execution이고 checkpoint가 선택 정책의 이전 경로에 있는 실제 상태인지 검증한다. 단순히 같은 job에 존재한다는 것만으로 승인하지 않는다. loop의 동일 상태가 이미 checkpoint라면 0회부터 1로 판정할 수 있으며 별도의 경로를 만들어 확률을 키우지 않는다.

서버가 failure의 full state/provenance를 복구 root로 고정하고 checkpoint의 모든 필드와 일치하는 목표를 만든다. caller가 새 item/임의 목표/policy를 주입하지 않는다. 복구에도 v1 후보군 및 지원 검사를 적용한다. 지원 밖 실패 상태는 UNSUPPORTED이며 특수 화폐를 추가하지 않는다.

복구 job.recovery에는 parentJobId/revision·failureExecutionId·checkpointStateId, conditional=true, includedInMain=false를 기록한다. points는 **그 실패 상태에 있다는 조건 아래** checkpoint로 돌아올 확률이다. 실패 상태에 도달할 확률이나 이후 원래 목표 성공률을 곱하거나 더하지 않는다. 본경로 결과는 불변이며 복구 job 완료로 갱신하지 않는다. 미도달/미계산을 영구 복구 불가능으로 표시하지 않는다.

## Job, 취소·재개·pagination

메모리 bounded executor/job 저장소와 TTL부터 구현한다. DB/Redis 영구 저장이나 전역 상태 사전계산은 필요 없다. status는 QUEUED/RUNNING/PAUSED/CANCELLED/COMPLETED/UNSUPPORTED/FAILED/EXPIRED다. COMPLETED는 요청된 관측점·후보 평가의 완료이며 eventual 증명을 뜻하지 않는다. 계산 한도 중단은 PAUSED, 사용자 취소는 CANCELLED다. resumable=true는 정확한 continuation을 보관한 PAUSED/CANCELLED에만 가능하다. 불가역 미지원 질량만 남으면 false다.

revision은 snapshot commit마다 단조 증가한다. 그래프·확률·순위는 같은 revision으로 원자 발행한다. FE는 현재 입력 generation/jobId가 같고 최신 revision 이상인 응답만 적용한다. 새 입력은 이전 job을 취소하며 늦은 응답이 편집 상태나 트리를 덮어쓰지 않는다. 브라우저 AbortSignal은 서버 취소의 대체물이 아니다.

mutationRequest.commandId는 TTL 내 멱등이다. 이미 처리한 동일 payload는 이전 결과를 반환하며, 같은 commandId의 다른 payload는 409 COMMAND_ID_REUSED다. 멱등 조회를 expectedRevision 검사보다 먼저 한다. 새 command의 expectedRevision이 다르면 409 REVISION_CONFLICT다. FE는 현재 job을 조회하고 여전히 사용자 의도가 같을 때 새 commandId로 재시도한다. 완료가 취소보다 먼저 commit되면 cancel은 완료 snapshot을 그대로 반환한다. 취소가 먼저 commit되면 이후 worker 결과는 폐기한다. 취소 acknowledgement 이후 같은 실행 generation의 결과를 발행하지 않는다.

resume는 frozen 요청과 provenance를 바꾸지 않고 새로운 실행 generation으로 이어간다. 이미 RUNNING인 job에 새 resume는 409 JOB_NOT_RESUMABLE다. 정확한 frontier 질량, 정책 phase, 흡수/종료 질량, 완료 layer, 미완료 kernel의 deterministic 위치, 미평가 후보와 관측점 cursor를 보관한다. 계산 중단으로 unresolved에 포함했던 질량을 이어 처리하되 기존 흡수 질량/edge를 중복 더하지 않는다. 지원 근거가 없는 unresolved를 재개만으로 계산된 것으로 바꾸지 않는다. 재개 결과는 동일 계산량의 중단 없는 실행과 같아야 한다.

graph cursor는 jobId/revision/offset에 묶인 opaque 값이다. 새 revision으로 조용히 바꾸지 않는다. 고정 revision은 TTL/메모리 정책 내에서 유지하며 eviction 시 410 REVISION_EXPIRED다. job 만료/서버 재시작 후 continuation이 없으면 410 JOB_EXPIRED로 새 실행을 안내한다. cursor를 요청자가 조작하면 422 INVALID_CURSOR다. expiresAt은 RFC3339 시각이며 만료 후 자동 재계산하지 않는다. 서버 부하 거부는 503 SEARCH_CAPACITY_REACHED다.

## 버전·삭제·유지보수

기존 strict loader, GoalDefinitions, BaseRegistry, SealedResources, ruleset manifest를 재사용한다. 새 정적 출처 체계나 source 이름별 분기를 만들지 않는다. schemaVersion은 형식이고 rulesetIdentity는 내용 identity다. 실제 게임 시즌/패치가 UNVERIFIED인 현행 자료에 임의 이름을 붙이지 않는다.

단일 전이는 full state+transition provenance+action으로 cache한다. predicate는 여기에 AST/catalogVersion/predicateVersion, 검색 결과는 후보군/searchVersion/observations를 추가한다. partial 전이를 complete cache로 저장하지 않는다. revision/checkpoint는 job 내부 값이며 complete 계산 캐시와 혼합하지 않는다.

속성 추가/삭제/단위/weight/기계적 효과 변경 시 ruleset 또는 관련 catalog/definition version을 갱신한다. 변경된 job·cache·경로·목표·checkpoint의 실행은 무효화하고 원본은 보존한다. 현재 값을 과거 identity에 다시 붙이지 않는다. ruleset header mismatch는 기존 422 RULESET_IDENTITY_MISMATCH, 목표 catalog mismatch는 기존 409 CATALOG_VERSION_MISMATCH를 유지한다. 목표 stat 삭제 후 unknown 참조는 422 UNKNOWN_STAT이고 의미가 유사한 다른 stat으로 치환하지 않는다. 저장 checkpoint의 삭제된 modifier는 422 INVALID_ITEM으로 재실행을 거부한다. 자동 migration은 v1에 없다.

새 기존형 stat은 검토된 JSON+fixture 추가로 공통 evaluator·전이·검색 회귀가 작동해야 한다. source/stat/pseudo 참조 무결성, 중복/unknown field, unit, source digest를 검증한다. JSON 삭제 후 dangling 참조는 로드 단계에서 실패해야 한다. 새 효과/상관 roll/quality 변환은 typed 엔진과 ledger의 정당한 확장 대상이며 JSON만으로 모든 기능이 가능하다고 약속하지 않는다.

## 완료·검증 기준

계약 검증기는 schema/의미/독립 oracle만 검증한다. 제품 완료는 FE/BE가 같은 fixture를 읽는 소비자 테스트와 실제 Solar 새 endpoint의 일치 확인을 추가로 요구한다.

- exact oracle, 질량 보존, self-loop/2-state loop, root hit/첫 성공 흡수, 관측점별 순위 역전·동률.
- 동일 상태 합류와 phase 분리, unrelated modifier의 pool 영향, 품질/socket/원래 roll 보존.
- UNKNOWN과 UNSUPPORTED를 성공이나 실패로 바꾸지 않음, 미탐색 후보의 순위 미확정.
- 별도 실패 상태 복구·정확한 이전 checkpoint·본경로 미합산.
- 취소/완료 경합, 멱등 재시도, 오래된 revision/페이지, 중복 페이지, resume 질량 중복 금지, 만료·ruleset 변경.
- 새 stat의 data-only fixture 추가 및 삭제 무효화, 공통 schema/참조 무결성 회귀.
- 실제 Solar fixture의 독립 oracle와 numeric predicate/renewal 결과 일치. 합성 예제를 production 지원 증거로 쓰지 않음.
- FE의 상위 노드 우선 표시, 위→아래 분기·합류·반복, 관측 선택·추천 강조, 6언어/키보드/focus/좁은 화면, 입력 변경 후 늦은 응답 폐기.
- 운영 조작부와 내부 ID 미노출. 서버/DB/볼륨을 변경하지 않는 격리 검증.

스케일업은 실제 Solar 완전 사례 → 일반 16종 전파/재개 → 증명된 bucket 압축 → 추가 기본 화폐/base 순서다. 각 단계에서 상태/edge 수·메모리·fraction 크기·첫 결과 시간·취소 응답을 측정한다. 수억 상태 전계산, 영구 저장, 임의 분포 가정으로 시작하지 않는다.
# Method-state presentation extension

`Recommendation.method` is an optional additive projection of a completed, proven
single-explicit renewal calculation. It does not replace elementary `GraphPage.edges`
or change observation counts, goal evaluation, candidate ranking, or recovery semantics.
Older strict-schema clients must be updated together with the server; omission/null
means that a method distribution is not available, not that its success chance is zero.

The supported methods remain the six existing Chaos and Annulment/Exalted policies.
There is no search over successive different methods. Method depth is one for this
projection even when hundreds of currency uses occur. The starting item is depth zero.
The goal absorbs immediately, including after the removal action of a two-action cycle.
At an observation limit an active path is retained at its actual full state; for an odd
two-action observation this is the empty intermediate state, not a completed cycle.

For each success state with one-cycle mass q, success total p, unresolved total u, and
r = 1 - p - u, first-hit mass through k complete cycles is q times the geometric sum
of r^j for j from zero to k-1. This is unconditional from the method's starting item,
not conditional on eventual success. The implementation apportions the existing
bounded first-hit mass by q/p, retaining its arithmetic budget and unresolved mass.
Active mass at a completed cycle is apportioned among nonmatching states by q/r.
No division is performed for a zero total. Unavailable or incomplete proofs do not
publish a method projection.

Success and active state details each have an independent six-state display budget.
Full-state identities are retained; distinct rolls are not merged into a representative
item. `omitted.hit` and `omitted.active` contain calculated probability whose individual
states are not shown. They are not unresolved mass or synthetic item nodes.
For every observation, displayed HIT plus omitted HIT equals `Point.lower`; displayed
ACTIVE plus omitted ACTIVE equals `Point.active`. Adding dead and unresolved yields one.
Zero probability exits are not drawn. A partial lower bound is marked as such.

Every retained method outcome also retains its actually enumerated elementary edge and
policy execution/phase, even beyond the legacy first-24 display budget. The union of
these selections contributes to displayed edge mass only once; its complement remains
the expansion's unshown mass. These connections establish real recovery ancestry.
When method outcomes are shown, the client loads the remaining fixed-revision graph
pages through the existing cancellation/stale-response guards before resolving the
remaining recovery choices. No execution or ancestry is inferred from item equality alone.

The presentation draws root-to-outcome method arrows, action icons from the existing
currency mapping, repetition semantics and state-specific observation probabilities.
All arrows of a certified selected recommendation are emphasized. A recovery job keeps
its distinct dashed styling and conditional scope and never changes main-path mass.
The viewport contains horizontal overflow and supports keyboard focus and edge-to-item
navigation. Existing six locales are preserved.
