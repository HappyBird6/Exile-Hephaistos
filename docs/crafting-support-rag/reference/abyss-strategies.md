# 별도 추천 코드에 남아 있는 세 훼손 전략

문서 ID: `support.reference.abyss-strategies` · 상태: **참고 전용, 기본 검색 제외**.

이 문서는 `AbyssStrategyPlanner.recommend`의 판단을 보존한다. 주 자동 탐색의 `GoalDirectedPlanner`는 현재 [공통 훼손 전이](../knowledge/desecration.md)를 사용한다. 별도 추천 API가 존재한다는 이유로 아래 세 후보가 현재 주 그래프에 그대로 삽입된다고 해석하지 않는다.

## 공통 전제

별도 추천 함수는 Rare, 명시 옵션 6개 미만, 기폭제 품질 없음, 기본 주얼 아님인 상태를 검사한다. 전체 목표가 이미 MATCH이거나 UNKNOWN/UNSUPPORTED이면 추천하지 않는다. `addUnrevealed`가 실제 슬롯 자격을 검사한다.

유효 옵션은 제거했을 때 기존 MATCH 조건이 사라지는지로 판정한다. 분열된 유효 옵션은 한쪽 슬롯의 유효 개수에는 포함되지만 제거 위험의 후보에는 들어가지 않는다.

graph의 별도 조회는 구체적인 노드만 허용한다. 여러 tier·roll 범위·여러 멤버로 압축된 노드에 임의 아이템을 만들어 추천하지 않는다.

## 1. 한쪽 슬롯 선완성 — `COMPLETE_SIDE`

- 조건: 해당 쪽 슬롯이 2개이고 두 슬롯 모두 목표에 유효하다. 미공개 훼손을 같은 쪽에 추가할 수 있고, 그 상태에서 다음 엑잘 후보 풀이 비어 있지 않다.
- 제안: 훼손 슬롯으로 그 쪽을 채운 뒤 반대쪽 제작을 검토한다.
- 한계: 제거 화폐의 피해를 막는 보호가 아니다. 이 helper의 엑잘 제안이 주 자동 탐색의 엄격 엑잘 정책을 통과한다는 보장은 없다.

## 2. 고가중치 비목표 계열 차단 — `BLOCK_HIGH_WEIGHT`

- 조건: 훼손 준비 후 다음 엑잘 풀에서 최대 수치 기준으로 기존 목표를 보존하며 MATCH 행을 늘릴 후보가 있다.
- 비교: `AbyssProbabilityComparison`으로 특정 비목표 옵션을 실제 선택했을 때 차단되는 계열을 계산한다. 조건부 선택 확률이 기본보다 높을 때 제안한다.
- 한계: 그 비목표 옵션을 드러내기 후보로 관측할 확률이 아니다. 목표 수치까지 맞힐 확률이나 전체 경로 확률로 합산하지 않는다.

## 3. 제거 위험 분산 — `SHARE_REMOVAL_RISK`

- 조건: 보존할 유효 옵션이 있고, 기존 상태와 훼손 준비 상태 모두에서 다음 행동이 가능하다. 실제 helper는 기본 행동(`baseAction != null`)과 심연 에센스를 건너뛰고 화폐/징조 중복을 제거한다.
- 비교: 기존 MATCH 조건을 보존하는 제거 분기 수/전체 제거 분기 수가 준비 후 증가하면 제안한다.
- 한계: 임의의 모든 화폐에 위험 분산이 성립한다고 가정하지 않는다. 분열은 제거 후보가 아니며, 안전 비율과 전체 목표 성공 확률은 다르다.

## 호출 경계

현재 `AutomaticMethodGraph.abyssStrategies` → kernel의 별도 추천 → `AbyssStrategyPlanner` 경로가 남아 있다. `AutomaticCraftingKernel.automaticAbyss`는 소스에 있지만 주 graph의 후보 발견은 이 메서드를 호출하지 않는다. 주 graph는 `choices`에 공통 DESECRATE 작업을 추가한다.

이전 설명에 있는 “세 전략을 별도 부착한다”와 이후 “공통 상태 전이로 통합한다”를 하나의 현재 실행 규칙으로 섞지 않는다. 향후 세 전략을 명시적인 주 자동 후보로 다시 도입한다면 동등성 이관과 별개의 변경이다.

## 근거

- [AbyssStrategyPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AbyssStrategyPlanner.java): `recommend`, `safe`, `preserves`.
- [AutomaticCraftingKernel](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticCraftingKernel.java): `abyssStrategies`, `automaticAbyss`.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `abyssStrategies`, `discover`.
- [사용자 규칙 원장](../../current/user-crafting-rules.md): 2026-10-29 세 전략과 2026-10-30 공통 전이 정정.
