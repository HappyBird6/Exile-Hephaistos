# 첫 옵션 카오스와 준비

문서 ID: `support.first-affix-chaos` · 적용: 시작 단계의 첫 변경 가능 목표 획득 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 적용 조건

현재 자동 정책은 `FIRST_AFFIX_ACQUISITION`이다. 아래 조건을 동시에 만족하는 경우 카오스 획득 후보를 검토한다.

1. 현재 탐색의 시작 단계다. 같은 아이템이 후속 노드로 나타난 경우에는 적용하지 않는다.
2. 변경 가능한 옵션으로 이미 획득한 목표가 없다. 이 판정은 현재 상태와 분열 옵션만 남긴 비교 상태의 목표 판정을 대조하는 기존 `acquired` 계산을 사용한다. 분열로 이미 만족한 목표만으로 카오스를 차단하지 않는다.
3. 카오스를 실제로 적용할 상태가 Rare이고, `mutableAffixCount`가 정확히 1이다.
4. 현재 정책과 실제 kernel이 해당 화폐/상태를 허용하고, 일반 후보의 목표 진전 검사를 통과한다.

`mutableAffixCount`는 **분열되지 않은 explicits 수 + modifierId가 없는 미공개 훼손 슬롯 수**다. 공개된 훼손 옵션을 다시 한 번 세지 않는다. 개수가 1이라는 것만으로 미공개 훼손에 대한 확률 계산까지 지원한다고 판단하지 않는다.

## 변경 가능한 옵션이 없을 때

시작 상태에서 변경 가능 옵션이 0개라면 기본 엑잘로 한 옵션을 준비한 후 카오스로 이어지는 후보를 검토한다. 준비 엑잘에는 징조가 없어야 한다. 실제 준비/후속 plan이 유효하고 목표 진전 결과가 있어야 한다.

기존 후보 표현은 다음 의미를 보존한다. 코드 실행 지시가 아닌 기존 행동 식별 설명이다.

| 후보 | `initial` | `cycle` |
|---|---|---|
| 바로 반복 | 없음 | 기본 CHAOS, 징조 없음, `GOAL_ACQUISITION` |
| 엑잘 준비 후 반복 | 기본 EXALTED, 징조 없음, `TEMPORARY_AFFIX_FOR_REMOVAL` | 기본 CHAOS, 징조 없음, `GOAL_ACQUISITION` |

첫 엑잘만으로 목표를 얻는 직접 후보가 나올 수 있으며, 그 후보가 있다고 엑잘→카오스 준비 경로를 지우지 않는다. 준비에서 목표가 달성되면 `STOP_ON_FIRST_HIT`으로 카오스를 사용하기 전에 멈출 수 있다. 이후 반복 실패마다 준비 엑잘을 다시 소모하는 것으로 계산하지 않는다.

일반 준비 규칙은 제왕→카오스도 검토할 수 있지만, **마법 1옵션에 제왕을 써서 변경 가능 옵션이 2개가 되면 현재 카오스 제한을 통과하지 못한다.** “제왕 준비도 항상 가능”으로 문서화하지 않는다.

## 금지 조건

- 변경 가능한 옵션 2개 이상, 시작 단계가 아닌 노드, 변경 가능한 기존 목표를 이미 획득한 상태.
- 옵션을 줄였거나 훼손 작업을 했다는 이유로 시작 단계로 돌아가는 처리.
- 준비 엑잘/제왕에 징조를 붙인 카오스 준비. 이것은 세 화폐 조합의 확률이 같다는 가정이 아니라 현재 서비스 정책이다.
- 상위/완벽 카오스, 현재 보류된 절사·좌/우 말소를 검색 결과만으로 추가하는 처리.
- 실제 게임의 모든 카오스 사용을 이 자동 탐색 조건으로 제한한다고 설명하는 것.

## 반복과 종료

현재 새 자동 탐색의 관측 기준은 100회다. 실패 후 같은 자격을 유지하면 실제 후속 상태에서 반복한다. 목표 획득/기존 목표 상실/미지원 분류는 기존 실행기를 따른다. 100회 후 남은 비성공 반복 질량을 또 다른 100회 경로로 연결해 무제한 성공률로 바꾸지 않는다.

300/500 관측은 과거 저장/표시 계약에도 존재한다. 이번 추출로 신규 UI의 반복 횟수를 늘리지 않는다. 확률식·같은 분포인 실패 상태를 묶는 증명은 엔진에 남긴다. 단순 독립 반복식을 RAG가 임의 적용하지 않는다.

## 비교 사례

- 긍정: 분열된 A와 일반 비목표 B 한 개를 가진 Rare 시작 상태에서 B를 첫 일반 목표로 바꾸는 카오스 후보.
- 긍정: 분열 A만 가진 시작 상태에서 징조 없는 엑잘→카오스 준비 후보.
- 부정: 동일 아이템을 `initialStage=false`로 평가했을 때 카오스 재진입.
- 부정: 분열 A + 일반 B/C, 또는 일반 목표 B를 이미 확보한 상태에서 다른 일반 목표를 카오스로 계속 획득.

## 근거

- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): `choices`의 `acquired`, `allowChaos`, `firstChaos`와 준비 징조 제외.
- [AutomaticCraftingKernel](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticCraftingKernel.java): `mutableAffixCount`, `invalidInitialChaos`, `strictExaltedAllowed`.
- [AutomaticMethodExecution](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodExecution.java): `boundedChaos`, 준비 성공·재시도 처리.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `step`, `strategy`의 관측 한도.
- [AutomaticAbyssSearchTest](../../../backend/src/test/java/com/poe2craft/bootstrap/AutomaticAbyssSearchTest.java): `fracturedGoalDoesNotBlockFirstMutableChaosButTwoMutableAffixesStillDo`, `fracturedChaosHundredRetriesMatchFiniteSeries`.
