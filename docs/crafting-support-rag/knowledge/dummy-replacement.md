# 제거할 임시 옵션 준비

문서 ID: `support.dummy-replacement` · 적용: 제왕/엑잘 준비 후 실제 후속 교체 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 목적과 적용 조건

에센스·합금 등의 실제 후속 작업에서 제거할 옵션을 마련하는 전략이다. 비목표 옵션을 아무 이유 없이 채우거나, 일반 엑잘로 좋은 옵션을 계속 추가하는 전략이 아니다.

준비 행동은 현재 inventory의 `TEMPORARY_AFFIX_FOR_REMOVAL`에서 가져온다. 준비와 후속 kernel plan이 실제로 가능하고, 후속 결과에 목표를 진전시키는 분기가 있어야 후보로 남긴다. 준비용 화폐가 현재 아이템에 적용되는지는 게임 엔진이 검증한다.

## 엑잘 더미의 엄격 조건

1. 시작 카오스용 징조 없는 엑잘은 [별도 문서](first-affix-chaos.md)를 따른다.
2. 그 외 엑잘 더미는 좌측 찬미 또는 우측 찬미 **한 개**를 붙인 후보로 구분한다. 두 징조를 동시에 붙이거나 징조 없는 일반 마무리 후보로 바꾸지 않는다.
3. 후속 화폐는 `replacementModifiers`가 실제로 존재하는 제거·추가 작업이어야 한다.
4. 준비 결과에서 원래 상태에 없던 명시 옵션이 정확히 하나 추가되고, 그것이 분열 옵션이 아니어야 한다.
5. 후속 제거 분기 중 추가한 그 옵션을 제거하며, 원래 explicits를 모두 남기고 원래 훼손 슬롯 상태도 유지하는 분기가 존재해야 한다.
6. 해당 분기에 검증된 후속 옵션을 붙인 대표 결과 중 목표를 진전시키는 결과가 있어야 한다.

`consumesPreparedAffix`는 이러한 **유효한 분기가 존재하는지** 검사한다. 원하는 더미가 확정 제거되는 뜻은 아니다. 다른 옵션이 제거되는 실패 위험과 화폐 소모는 기존 전이 분포에 남긴다.

## 제왕과 공통 준비

현재 코드에서는 제왕도 임시 추가와 직접 목표 획득 purpose를 가진다. 제왕과 엑잘의 모든 조건을 동일하게 만들지 않는다. 엑잘의 추가된 옵션 식별/소비 조건은 코드에서 엑잘에 적용하는 조건이다.

일반 준비 연결은 후속이 목표 획득 purpose이거나 교체 옵션을 가진 화폐인지 검사한다. 같은 준비 화폐의 직접 목표 획득 후보가 이미 있는 경우 일부 준비 경로가 생략되며, 시작 카오스 준비 등 예외는 [목표 진전](goal-progress.md)의 순서를 따른다.

제왕의 JSON 설명에는 임시 용도만 적혀 있지만 실행 코드는 직접 획득도 허용한다. 이 문서는 실행 추출이며 설명 불일치를 해소한 정책 변경이 아니다. [검토 기록](../review-notes.md)을 참고한다.

## 준비 중 목표를 달성했을 때

- `initial`에 EXALTED가 있고 첫 `cycle`이 CHAOS가 아니면 graph는 `REQUIRE_REPLACEMENT`를 선택한다.
- 그러므로 엑잘 더미 추가 중 우연히 전체 목표가 맞아도 예정된 후속 제거·추가를 생략하지 않는다.
- 그 외 기본은 `STOP_ON_FIRST_HIT`이다. 제왕 준비나 카오스 준비까지 무조건 후속을 강제하지 않는다.
- 실행 Template는 임시 추가 뒤에 실제 교체 또는 카오스 행동이 있어야 한다고 검사한다. 임시 추가만 있는 독립 Template를 만들지 않는다.

이 종료 정책은 후보 이름만으로 복원되지 않는다. 기존과 비교할 때 `initial/cycle/purpose` 및 실행기의 종료 의미를 함께 비교한다.

## 비교 사례

- 긍정: 좌/우 찬미로 더미를 추가한 후, 실제로 그 더미를 제거해 목표 옵션을 붙일 수 있는 에센스/합금 경로.
- 부정: 교체 화폐가 있다는 이유만으로 원래 유효 옵션만 제거되는 준비 경로도 더미 소비 성공으로 처리.
- 부정: 더미 준비 중 목표 달성을 감지하고 예정된 엑잘 후속 교체를 생략.
- 부정: 자리가 없거나 후속 작업이 적용 불가인데 엑잘 더미 후보를 생성.

## 근거

- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): `consumesPreparedAffix`, `choices` 준비 연결.
- [AutomaticCraftingKernel](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticCraftingKernel.java): `strictExaltedAllowed`를 plan/expand 양쪽에서 적용.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `Method`의 `PreparationSuccess` 선택.
- [AutomaticMethodExecution](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodExecution.java): `Template`, `classifyState`.
- [GoalDirectedPlannerScenariosTest](../../../backend/src/test/java/com/poe2craft/bootstrap/GoalDirectedPlannerScenariosTest.java): 엄격 엑잘·준비 후 목표 획득 시나리오.
