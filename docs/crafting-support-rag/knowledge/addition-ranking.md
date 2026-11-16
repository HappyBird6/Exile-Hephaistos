# 추가 화폐와 징조 우선순위

문서 ID: `support.addition-ranking` · 적용: 직접 엑잘/제왕 후보 비교 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 적용 조건

`initial`이 비어 있고 `cycle`이 하나인 같은 화폐 후보를 비교한다. 준비 엑잘→카오스나 더미→에센스를 이 비교의 직접 후보로 바꾸지 않는다. 해당 화폐의 징조 없는 직접 후보가 없으면 비교를 수행하지 않는다.

현재 호출 순서는 EXALTED 다음 REGAL이다. 엑잘은 별도의 엄격 제한을 통과한 후보만 여기 들어온다. RAG가 먼저 징조를 추천한 뒤 실제 허용 후보를 건너뛰어서는 안 된다.

## 제외와 정렬 순서

1. 징조 후보와 일반 후보의 plan 분기 및 `uniformCandidates`가 같으면 그 징조 후보를 제외한다.
2. 각각 결과 상한 5,000으로 분포를 요청한다. 이것은 현재 구현의 비교 예산이며 게임 규칙이 아니다. 둘 다 COMPLETE가 아니면 그 비교로 확률 우위를 확정하지 않는다.
3. 둘 다 모든 결과에서 전체 목표를 확실히 달성하면 징조 후보를 제외한다. 비목표 옵션 분포가 달라도 이 조건에 해당할 수 있다.
4. 일반 후보의 `advances` 결과마다 **동일 결과 아이템**이 징조 분포에 존재하고 그 확률이 모두 일반 이상이면 일반 후보를 제거한다. 단순히 전체 성공률 숫자 하나만 비교하는 조건으로 바꾸지 않는다.
5. 남은 직접 후보가 모두 COMPLETE이면 `advances` 결과의 확률 합을 내림차순 정렬한다. 동률은 기존 순서를 보존하고 다른 종류 후보가 차지하던 위치는 유지한다.
6. 정렬 단계에서 미완료 분포를 만나면 그 정렬을 종료한다. 앞서 적용한 후보 제외를 되돌린다고 추정하지 않는다.

확률 비교와 분수 계산은 엔진이 수행한다. LLM이 옵션 설명을 보고 유리한 쪽의 수치를 만들어서는 안 된다.

## 비용과 관련된 한계

이 정책은 현재 화폐 시세를 조회하지 않는다. 비싼 징조가 성공률을 높인다는 이유로 저렴한 일반 후보를 제외할 수 있다. 이번 목표는 기존 판단의 재현이므로 이를 비용 최적화로 바꾸거나 “최저 비용”이라고 설명하지 않는다.

## 비교 사례

- 긍정: 목표 접미어 획득에 실제 도움이 되는 우측 대관식 제왕을 우선하는 경우.
- 긍정: 두 방법 모두 확정 목표 달성이거나 분포 자체가 같으면 일반 제왕을 남기는 경우.
- 부정: 준비가 포함된 경로까지 같은 직접 추가로 묶어 제거.
- 부정: 분포가 일부만 계산됐는데 성공 확률을 확정하거나, 징조의 현재 가격을 새 기준으로 섞음.

## 근거

- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): `preferAddition`.
- [AutomaticPreparationRulesTest](../../../backend/src/test/java/com/poe2craft/shared/crafting/domain/AutomaticPreparationRulesTest.java): `goalRegalUsesTheHelpfulSuffixOmen`, `certainCompletedRegalGoalDoesNotPayForAnOmenWithDifferentUnconstrainedJunk`, `identicalRegalLawKeepsOnlyPlainRegal`.
