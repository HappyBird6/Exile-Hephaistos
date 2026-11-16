# 공통 훼손 전이와 관측

문서 ID: `support.desecration` · 적용: 주 자동 탐색의 공통 ItemState 전이 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 적용 조건

`desecrationActions`는 아래를 만족할 때 남는 접두/접미 각각에 훼손 작업을 제안한다.

- 이미 훼손 슬롯이 없음.
- Rare이며 전체 명시 슬롯 사용량이 6 미만.
- `conditions`가 비어 있음.
- 검토된 기본 주얼 제작 대상이 아님.
- `catalystQuality`가 없음.
- 실제 슬롯 검증 결과 해당 쪽에 빈자리가 있음.

이는 현재 자동 경로의 구현 경계다. 게임 전체 훼손 자격으로 일반화하지 않는다. 후보는 일반 획득의 MATCH 증가 조건과 별도로 추가하되, 목표 판정 미지원/품질 우선 반환 등 앞선 공통 흐름은 따른다.

## 생성되는 행동과 결과

행동은 `currency=null`, `operation=DESECRATE`, `side=PREFIX 또는 SUFFIX`, `purpose=ORDINARY`, 징조 없음이다. 훼손 에센스 등 특정 화폐로 임의 치환하지 않는다.

검증된 작업은 해당 쪽에 미공개 훼손 슬롯을 하나 만든다. 이 슬롯 변화는 확정 전이지만 **드러낼 옵션 종류나 수치가 확정되는 것은 아니다.** 공개 전에는 가짜 modifier ID·수치·배타 계열·선택 가중치를 부여하지 않는다.

주 자동 탐색은 이를 기존 화폐와 같은 `Choice/Method/Node`로 다룬다. 별도 추천 API에 남은 [세 훼손 전략](../reference/abyss-strategies.md)을 다시 자동 경로로 삽입하지 않는다.

## 후속 제작과 조건부 표시

- 미공개 슬롯도 해당 쪽 슬롯 용량과 제거 후보에 포함한다. 분열은 제거 후보에 포함하지 않는다.
- 훼손이 있는 아이템은 kernel에서 `slotPlan` 경로로 계산한다. 훼손을 지운 임시 일반 아이템으로 대체하지 않는다.
- 이 준비로 시작 단계 카오스 제한을 우회하거나 엄격 엑잘 제한을 해제하지 않는다. 예: 미공개 훼손 슬롯 하나를 가진 상태에서 일반 마무리 엑잘이 자동 허용되는 것은 아니다.
- 이미 훼손 슬롯이 있고 준비 없는 단일 행동인 경우 `withSelectionComparison`을 거칠 수 있다. 현재 실제 비교 부착 대상은 **징조 없는 EXALTED**이며, 미공개 슬롯·유효 목표 후보·비교 결과가 있어야 한다.
- 비교 목표 후보는 엔진이 후보의 최대 수치로 판정했을 때 MATCH 행 수가 늘어나는 ID들이다. 이것은 수치 목표를 실제로 굴려 맞출 확률이 아니다.
- `p%(~N%)`는 해당 후속 옵션 종류 선택의 기본 확률과 특정 비목표 계열 차단 후의 조건부 확률이다. 드러내기 후보 등장 확률·전체 목표 성공률·신뢰구간으로 해석하지 않는다.

## 관측과 종료

사용자가 실제로 본 후보/선택한 결과를 검증해 같은 슬롯을 갱신한다. 이미 슬롯이 있는 6옵션 상태에서 공개/교체하는 것과 새 슬롯을 추가하는 것은 다르다.

현재 자동 전체 경로에서는 미관측 훼손 선택 분포를 만들지 않는다. 훼손 상태를 열린 계산 경계로 남기며 결과를 0%로 단정하지 않는다. 관측 후 확정 상태에서 새 탐색을 시작할 수 있지만, 임의로 대표 수치를 골라 실제 관측이라고 저장하지 않는다.

## 비교 사례

- 긍정: Rare의 남은 접두/접미에 훼손 후보가 일반 그래프의 Method로 나타남.
- 긍정: 분열과 미공개 훼손이 함께 있어도 후속 제거에서 분열은 보존되고 훼손 슬롯은 제거 후보에 포함.
- 부정: 슬롯을 만드는 확정 전이를 원하는 훼손 옵션의 확정 획득으로 표시.
- 부정: 현재 후보 3개가 각각 1/3 확률로 등장한다고 가정하거나 전체 경로 확률에 합산.

## 근거

- [AutomaticCraftingKernel](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticCraftingKernel.java): `desecrationActions`, `slotPlan`, `withSelectionComparison`, 훼손 `expand`.
- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): 일반 결과 뒤 훼손 후보 추가와 비교 부착.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `strategy`의 미확정 경계.
- [CommonDesecrationGraphTest](../../../backend/src/test/java/com/poe2craft/shared/crafting/domain/CommonDesecrationGraphTest.java): 공통 graph·엄격 엑잘·분열 보존·6슬롯 관측.
- [AutomaticAbyssSearchTest](../../../backend/src/test/java/com/poe2craft/bootstrap/AutomaticAbyssSearchTest.java): 자동 공통 노드 생성·저장.
