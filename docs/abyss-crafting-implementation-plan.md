# 심연 훼손·드러내기 제작 구현 계획

2026-10-29 · 기준 소스 `ed6446c7adf152795e9fff2a15d9c74597ac4d9d` · 상태: 계획, 미구현.
확정 요구사항의 단일 기준은 [사용자 제작 규칙](current/user-crafting-rules.md)의 심연 절이다. 이 문서의 타입명·API명은 제안이며 현재 존재하는 기능으로 표시하지 않는다. 이번 승인 범위는 문서 작성과 코드 조사다.

## 1. 현재 코드에서 확인한 경계

| 실제 코드/자료 | 현재 동작과 누락 |
|---|---|
| [ItemState](../backend/src/main/java/com/poe2craft/item/ItemState.java), [ModifierInstance](../backend/src/main/java/com/poe2craft/item/ModifierInstance.java) | explicits는 modifierId·values·fractured의 확정 인스턴스 목록이다. 미공개 훼손 슬롯, 관측된 3후보, 선택 상태가 없다. |
| [ItemStateValidator](../backend/src/main/java/com/poe2craft/item/ItemStateValidator.java), [StateBucket](../backend/src/main/java/com/poe2craft/crafting/domain/StateBucket.java) | 카탈로그 ID와 family 중복을 검증하고 bucket은 modifierIds를 정렬하며 최대 6개를 검사한다. ID 없는 슬롯을 기존 목록 밖에 숨기면 슬롯·제거·동등성 계산이 틀린다. |
| [WorkbenchSimulator](../backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java) | 수동 ESSENCE_ABYSS는 표식 modifier의 제거/교체를 다룬다. automaticPlan은 ESSENCE_ABYSS를 명시적으로 UNSUPPORTED 처리한다. 드러내기 후보 관측/선택 전이는 없다. |
| [AbyssEssenceTest](../backend/src/test/java/com/poe2craft/crafting/AbyssEssenceTest.java), [StockyAbyssTest](../backend/src/test/java/com/poe2craft/crafting/StockyAbyssTest.java) | 표식 두 후보, 균등 제거/선택 가정, fracture 보존 등을 검사한다. 이것을 3후보 드러내기나 실제 게임 분포의 검증으로 재사용할 수 없다. |
| [WorkbenchCurrency](../backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchCurrency.java), [WorkbenchOmen](../backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchOmen.java), [정의 JSON](../backend/src/main/resources/crafting/workbench-definitions-v1.json) | 화폐 및 기존 징조 정의를 재사용할 수 있지만 메아리/훼손 재작업 전용 프로토콜이 없다. BLESSED 등을 이름 유사성만으로 대입하지 않는다. |
| [WorkbenchController](../backend/src/main/java/com/poe2craft/crafting/presentation/WorkbenchController.java) | apply는 WorkbenchCurrency를 받으며 상태·modifier 필드를 엄격히 허용 목록으로 검증한다. 새 상태/API는 이 검증도 함께 확장해야 한다. |
| [ItemModels](../backend/src/main/java/com/poe2craft/item/ItemModels.java), [ItemTextService](../backend/src/main/java/com/poe2craft/item/testparser/ItemTextService.java) | DESECRATED 표시/텍스트 분류가 존재하나 실행 가능한 훼손 슬롯 모델과 같지 않다. |
| [AutomaticCraftingKernel](../backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingKernel.java), [AdditionRules](../backend/src/main/java/com/poe2craft/crafting/domain/AdditionRules.java) | 실제 후보 weight의 합으로 분모를 계산하고 슬롯·family·징조 필터를 적용한다. 새 선택 이후 이 풀을 다시 구성해야 한다. |
| [GoalDirectedPlanner](../backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/GoalDirectedPlanner.java), [AutomaticMethodGraph](../backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodGraph.java) | initialStage와 카오스 제한, 상태 병합·순환·저장 버전이 존재한다. 관측 후 결정을 무작위 화폐 가지로 합치면 의미가 달라진다. |

현재의 심연 표식(`essence_abyss_guaranteed_pick`)을 미공개 훼손 슬롯 또는 선택 완료 심연 옵션으로 자동 변환하지 않는다. 기존 자료에 그런 대응 근거가 없다.

## 2. 구현 전에 확정할 데이터 계약

사용자 확정 사항인 슬롯 점유·3후보 관측·선택·메아리 재추첨·6옵션 재작업은 바로 테스트 계약으로 만든다. 다음 세부는 출처를 확보한 뒤 봉인 카탈로그에 기록한다.

- 훼손을 발생시키는 정확한 행동/화폐와 재작업 지정 징조 ID, 적용 장비·희귀도·레벨·접두접미 조건, 소모 시점·횟수·조합·실패 부작용.
- 기존 옵션/심연 옵션 후보 풀, 충돌군, tier·수치 분포와 weight 출처. ‘기존 옵션’이 일반 옵션 풀인지 이전 옵션 보존을 포함하는지 구체적 데이터와 대조한다.
- 3후보의 중복 허용, 복원/비복원 추출, 순서 의존, 슬롯/충돌군 필터 적용 시점, 실제 합동 분포. 독립 추첨으로 가정하지 않는다.
- 메아리 후 이전 3후보의 유지/폐기/복구 가능성, 현재 후보를 보류한 채 다른 제작 가능 여부, 여러 훼손 슬롯의 허용 개수.
- 미공개 상태의 알려진 side·충돌군·제거 자격, 재작업 중 기존 선택 옵션의 효과/충돌군이 언제 제거되는지.

자료가 없는 세부만 미확정으로 남긴다. 기존 1/n 사용자 방침은 후보 집합과 추출 계약이 확인된 뒤 명시적인 대체 모델로 적용할 수 있지만, 그것만으로 3후보 합동 분포를 결정하지 않는다. 출처 미확정 결과를 정확 확률로 노출하지 않는다. 별도 외부 자료 조사는 구현 시작 단계에서 수행한다.

## 3. 상태 모델: 실제 슬롯과 관측 상태 분리

권장안은 explicit 슬롯을 식별 가능한 값으로 만들고 `NORMAL / DESECRATED_UNREVEALED / DESECRATED_SELECTED` 유형을 표현하는 것이다. 기존 확정 modifier와 미공개 슬롯을 구분하는 typed payload를 검토한다. 임의 가짜 modifierId나 값 0으로 미공개 상태를 위장하지 않는다.

- 미공개 슬롯에도 안정적인 slotId, 확인된 접두/접미, 훼손 출처가 필요하다. 기존 explicits와 함께 총 6개·접두 3개·접미 3개 제한과 제거 후보 계산에 포함한다. 같은 슬롯을 placeholder와 선택 modifier로 이중 집계하지 않는다.
- 미공개 슬롯은 아직 수치 목표를 달성한 것으로 평가하지 않는다. 목표에 관계없는 미공개 슬롯 때문에 이미 확정된 수치까지 사라져서는 안 된다. 불확실/미확정과 실제 NO_MATCH를 구분하는 평가 계약을 정한다.
- 관측 중인 offer는 별도 제작 세션 상태로 둔다: targetSlotId, offerId/revision, 현재 후보 3개 및 provenance, 메아리 사용/단계, 확인된 이전 후보 가용성. 화면에서 아직 보지 않은 미래 후보를 planner가 아는 상태로 만들지 않는다.
- ItemStateValidator와 bucket/projection, canonical key, parser→실행 상태 매핑, 프런트 runtime validator를 함께 수정한다. 관측/선택 의미를 잃는 기존 StateBucket 투영은 거절하거나 명시적으로 확장한다.
- 구형 저장은 새 슬롯 정보가 없는 확정 modifier로 읽는다. 근거 없는 훼손 이력 추정은 금지한다. 기존 표식은 legacy 의미를 유지한다.

## 4. 행동·전이와 API

`훼손 실행 → 미공개 슬롯 → 드러내기 관측 → 선택 완료`를 기본 상태 머신으로 만든다. 메아리는 `관측된 offer → 새 offer` 전이, 지정 징조 재작업은 `선택 완료 슬롯 → 재작업 단계` 전이다. 재작업의 정확한 중간 상태는 위 데이터 계약으로 확정한다.

- 드러내기는 WorkbenchCurrency enum에 넣지 않는다. 제안 API는 별도 reveal/choose/reroll 명령 또는 공용 discriminated command이며 화폐 소모 이벤트와 사용자 선택 이벤트를 분리한다.
- offer 생성은 서버가 책임지고, choose는 관측된 offer의 후보 ID만 받는다. 상태 revision·offer 일치 검사, 재전송 멱등성, 만료·중복 선택 거절로 새로고침/요청 재시도가 무료 재추첨을 만들지 않게 한다. 영속화 방식은 기존 저장 경계를 검토해 정한다.
- 메아리는 후보 확인 후 명시적 사용자 실행이며 징조 소모를 기록한다. 과거 후보를 다시 선택할 수 있는지는 확정 계약에 따르고, 기본적으로 ‘6개 중 고르기’ UI를 만들지 않는다.
- 6옵션 재작업은 동일 slotId의 원자적 교체로 검증한다. 일반 추가의 ‘빈 슬롯 없음’ 검사를 그대로 사용하지 않으며, 다른 유효 옵션/implicit/품질/소켓은 보존한다. 요청 실패 시 반쪽 제거 상태를 남기지 않는다.
- 제거형 에센스/합금은 미공개 슬롯도 확인된 자격에 따라 후보에 넣는다. 기존 fracture·side 지정·충돌 제약을 재사용하되, 새 규칙이 기존의 모든 징조 조합까지 허용한다고 확대하지 않는다.

## 5. 세 전략을 탐색기에 넣는 방법

### 한쪽 2유효 + 훼손 1칸

GoalDirectedPlanner에 ‘한쪽 3칸 확보 → 반대쪽 작업 → 훼손 재작업’ 후보 방법을 추가한다. 접두/접미 대칭을 테스트한다. 슬롯 포화는 추가 풀을 제한하지만 제거형 작업은 해당 쪽 유효 옵션도 제거할 수 있으므로 별도 위험을 보여준다. 사용자 정의 수용 조건으로 ‘원하는 옵션까지’와 ‘적당한 후보에서 종료’를 구분한다.

### 고가중치 비목표 옵션으로 충돌군 차단

선택 완료된 비목표 옵션의 실제 familyIds가 이후 후보 풀에서 제거하는 항목을 계산한다. 한 번의 목표 추첨 확률은 확인된 weighted 모델에서 `목표 후보 weight 합 / 전체 eligible weight 합`이다. 비목표 family만 제거되고 목표 분자가 유지될 때 개선되며, 목표도 막으면 개선이 아니다. 미공개 슬롯에 임의 family를 붙여 blocking 이익을 계산하지 않는다.

작은 합성 예: 목표 weight 10, 비목표 family A 70, 기타 20이면 원래 10/100. A만 충돌로 빠지고 목표가 남으면 10/30이다. 목표까지 A에 속하면 목표 분자는 0이다. 이 숫자는 게임 자료가 아닌 분모 로직 테스트용이다. 선택 후보의 자체 획득 비용과 마지막 재작업 위험까지 포함해 직행 경로와 비교한다. 비목표 상태를 일괄 가지치기하지 않는다.

### 미공개 훼손 더미로 제거 위험 분산

제거 후보 중 보호되지 않은 유효 슬롯 수를 g, 전체 제거 후보를 n이라 할 때, 균등 단일 제거 모델에서 위험은 g/n이다. 제거 가능한 더미 1개가 추가되고 다른 조건이 같으면 g/(n+1)이다. side 지정·fracture·두 개 제거·제거 후 합법성 필터가 있으면 해당 실제 분포로 다시 계산한다. 이 식을 게임의 보편 규칙으로 선언하지 않는다. 더미가 제거 풀에서 제외되면 개선이 없으며, 6슬롯 제한·추가 후 충돌·더미 획득 비용도 비교한다.

## 6. 확률·선택·반복·종료

3후보 집합을 얻는 확률 가지와 집합을 본 사용자의 선택 가지를 분리한다. 후보 선택에 임의 1/3을 부여하지 않는다. 사용자 수동 선택, 사전에 명시한 수용 정책, 목표 달성률을 최적화하는 정책을 구분해 결과에 표시한다. 최적화 예측이 실제 사용자가 그렇게 선택했다는 뜻은 아니다.

후보 집합 합동 분포의 질량 합과 중복 처리부터 검증한다. 같은 후보 분포·독립 반복·정지 조건이 확인된 경우에만 `1-(1-p)^N`을 쓰고, 메아리 이력/후보 보존/선택에 따라 바뀌면 상태 전이로 계산한다. 사용자 화폐 최대 횟수 제한 없음 정책을 유지하면서 계산 예산·취소·중단/재개와 미완료 확률 경계를 별도로 유지한다. 반복 SCC에서 종료 가능성·흡수 확률·기대 비용 발산을 구분하고 실패 질량을 버리지 않는다.

카오스는 기존 initialStage 및 정확히 희귀 1옵션 제한을 그대로 유지한다. 새 훼손 슬롯도 실제 affix 수에 포함하고, 재작업·제거 후 첫 단계가 다시 열리지 않게 한다. 심연 반복에 카오스 전용 기본 100회 UI를 무조건 복사하지 않고 별도 반복 안내와 수용 정책을 설계한다.

## 7. 캐시·저장·UI 연결

- [TransitionCache](../backend/src/main/java/com/poe2craft/crafting/application/TransitionCache.java), [BasicTransitionCache](../backend/src/main/java/com/poe2craft/crafting/application/BasicTransitionCache.java), [AutomaticMethodGraph](../backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodGraph.java)의 키에 슬롯 유형·side·선택 modifier·관측 offer·가용 징조·확정된 이력 의존값을 빠짐없이 반영한다. 확률 또는 선택지가 다른 상태를 합치지 않는다.
- [SqliteMethodGraphStore](../backend/src/main/java/com/poe2craft/crafting/infrastructure/SqliteMethodGraphStore.java), [MethodCursorStore](../backend/src/main/java/com/poe2craft/crafting/application/pathsearch/MethodCursorStore.java), [SavedAutomaticResultReader](../backend/src/main/java/com/poe2craft/crafting/application/pathsearch/SavedAutomaticResultReader.java), [scenarioCode](../frontend/src/features/crafting/method-search/scenarioCode.ts)와 계약 스키마 버전을 함께 올린다. 옛 결과는 의미를 유지해 읽고 새 규칙으로의 재개는 명시적으로 거절하거나 검증된 마이그레이션만 허용한다. 운영 저장 자료를 조용히 재작성하지 않는다.
- [workbenchApi](../frontend/src/features/crafting/workbenchApi.ts), [workbenchStateShape](../frontend/src/features/crafting/workbenchStateShape.ts), [itemCardData](../frontend/src/features/crafting/itemCardData.ts), 목표 필터와 method-search 그래프에 미공개 슬롯·3후보·선택/메아리 버튼·소모·재작업 위험을 표시한다. 드러내기 버튼은 화폐 목록 밖에 둔다. 후보 하나를 선택한 뒤 확정 상태가 갱신되어야 한다.
- 접두/접미 슬롯 수와 더미 제거 위험을 사용자가 이해할 수 있게 표시하되 진단용 내부 ID·테스트 요소를 서비스 화면에 추가하지 않는다. 6개 언어, 키보드 focus, 기존 hover 후속 강조·클릭·패닝을 회귀 검사한다.

## 8. 단계별 실행과 완료 기준

1. **데이터 계약:** 위 미확정 항목의 최소 출처/사용자 확인을 확보하고 후보·징조 표를 작성한다. 알려진 요구사항은 지연시키지 않되 미확정 분포를 정확 확률로 출시하지 않는다.
2. **상태 모델 먼저:** 작은 카탈로그로 미공개 슬롯 1개가 총수·side·제거에 포함됨, 5→6 허용/7 거절, 3접두/3접미, 미보유 목표 불일치, 잘못된 payload 거절을 red→green 검증한다.
3. **수동 상태 머신:** reveal은 화폐 소모 없음, 후보 3개 관측 후 선택, 위조·이전 offer·중복 요청 거절, 메아리 소모와 새 후보, 저장/복원, 6옵션 동일 슬롯 재작업과 다른 옵션 보존을 검사한다. 미확정 후보 보존 규칙은 확정된 계약별 fixture로 추가한다.
4. **세 전략의 제한된 탐색:** 2유효+1훼손의 접두/접미 대칭, 위 10/100→10/30 및 목표 차단 반례, 유효2+더미1의 2/3 위험과 side 제외 반례를 테스트한다. 소형 전수열거와 계산 결과를 대조한 뒤 실제 카탈로그로 넓힌다. 무제한 전탐색이나 예산 상향으로 우회하지 않는다.
5. **회귀·호환:** 카오스 첫1옵션/0옵션 준비/후속 차단, 6옵션·에센스·합금·fracture·품질·수치 단위·AND/OR, 기존 표식 저장, 중단/재개·순환·캐시 분리, API schema와 위조된 선택 입력을 검사한다. 기존 AbyssEssenceTest 기대값은 구표식 모델과 새 훼손 모델을 구분해 보존/대체 근거를 남긴다.
6. **UI와 격리 통합:** 실제 브라우저에서 미공개 상태 입력→드러내기→3후보 선택→메아리→복원 및 6옵션 재작업을 검증한다. 운영 데이터가 없는 임시 환경을 사용한다. Docker backend check/generateJooq/bootJar, frontend lint/typecheck/format/test/build 및 필요한 스크립트 검사를 수행한다. 실패·미실행을 구분한다.
7. **적용은 후속 구현 승인 범위에서:** 검증 후 사용자의 서버 반영 지시를 재확인해 실행하고 데이터 백업·복원 해시·런타임 산출물 일치를 확인한다. 이번 문서 작업에서는 기동·배포하지 않는다.

## 이번 계획 검증

관련 소스·기존 테스트를 읽고 경로/상대 링크와 문서 diff를 자체 검토했다. 이번에는 코드·테스트 실행 결과를 새로 생성하지 않았으며 구현 완료나 게임 확률 확인으로 주장하지 않는다. 서버·DB·Redis·볼륨·환경 파일은 작업 대상이 아니다.


## 2026-10-29 구현 진행 기록

최신 사용자 지시가 위 초기 계획의 후보 가중치 조사·징조 상세 계산 요구보다 우선한다. 현재 기준 HEAD는 241302d006e67a813153e17c6cf348cffbe3fc86이며, 기존 작업 branch에서 사용자 변경이 없는 상태로 시작했다.

추가한 구현: AbyssCraftingState(실제 슬롯·관측 후보·선택), AbyssProbabilityComparison(다음 추가의 가중치 차단 비교), WorkbenchSimulator의 abyssPlan/applyAbyss, Workbench의 /abyss API, 6언어 AbyssPanel과 별도 저장/복원·응답 검증. 기존 자동 탐색 ItemState와 확률 법칙을 미공개 후보 분포로 확장하지 않았다.

현재까지 관련 슬롯·확률·화폐·API·기존 심연 에센스·ArchUnit 검사 및 프런트 심연 관련 7개 테스트가 통과했다. 전체 테스트·최종 빌드·격리 실제 API·실제 브라우저 검증은 완료 보고 전 별도 확인이 필요하다.

남은 필수 작업: 자동 탐색 주 그래프의 세 전략 및 수동 선택 구간 연결, 전체 영향 범위 검증, 실제 브라우저 검증, 규칙 Library 동일 identity 갱신과 바이트 검증, 사용자 저장 백업·복원·전후 해시 확인, 서버 적용. 서버 9000/9001과 해당 앱 DB/Redis 컨테이너는 시작 시 이미 중단되어 있었다. 이 작업에서 운영 컨테이너와 8080 서버는 변경하지 않았다.


## 2026-10-29 자동 탐색 연결 검증

- AutomaticMethodGraph의 구체적인 선택 노드에 AbyssStrategyPlanner를 연결했다. 소유자 확인을 유지한 노드별 GET API로 세 전략을 제공하고, 선택하면 원래 옵션을 보존한 미공개 슬롯을 AbyssPanel에서 연다.
- 목표를 이미 만족하는 옵션의 제거 전후 판정과 현재 허용 화폐를 사용한다. 분포 노드의 대표값을 실제 아이템으로 취급하지 않는다. 후보 추첨 확률이 없으므로 수동 전략을 자동 성공 확률에 곱하지 않는다.
- 브라우저에서 생명력 19·마나 14 접두를 가진 목걸이와 힘 5 목표로 세 전략을 확인했다. 고가중치 차단은 5.45%(~5.60%), 더미 제거 위험 분산의 기존 유효 옵션 보존은 0%→33.33%였다. 한쪽 채우기 선택 시 두 기존 접두와 녹색 미공개 접두가 유지됐다.
- Frontend 전체 120개 파일·2,366개 테스트, lint/typecheck/format/build 통과. 후속 변경은 전략 카드에 접두/접미·징조·차단 옵션명을 표시하는 내용이다.
- 격리 HTTP 검증 7개 통과: 미공개 6번째 슬롯, 후보 3개 관측, 6옵션 선택 및 재작업 시 나머지 5개 보존, 조건부 가중치, 더미 제거 위험, 에센스 실행.
- Backend 전체 검사는 실행 결과와 기존 HEAD 비교를 별도 완료 기록에 남긴다. GoalCatalogParityTest와 CertifiedSearchJobsTest의 실패는 기준 241302d에서도 재현됐다. 실패를 skip하거나 기대값을 완화하지 않았다.
- 운영 반영 전 별도 백업 복원 검증을 수행했다. Exile Redis의 호스트 6379 충돌 때문에 내부 연결만 남기는 별도 Compose 설정을 검증했으며 실제 적용은 승인 대기다. 다른 프로젝트의 Redis와 STS 8080, 기존 환경 파일은 변경하지 않았다.
