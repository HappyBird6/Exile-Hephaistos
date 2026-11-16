# 일반 목걸이 등급 상승

문서 ID: `support.rarity-upgrade` · 적용: 일반 목걸이 시작 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 적용 조건

현재 서비스 정책은 family `amulets`에서 기본 TRANSMUTATION의 `RARITY_UPGRADE` purpose를 허용한다. 실제 일반→마법 적용 자격·추가 옵션 분포는 기존 엔진이 검사한다. 다른 장비 family나 상위/완벽 진화로 확대하지 않는다. 연금술은 제외 상태를 유지한다.

현재 목표 편집기는 목표 희귀도를 Rare로 정규화하지만 시작 아이템은 Normal/Magic/Rare를 받을 수 있다. 이것은 게임의 필수 목표 희귀도가 아니라 기존 서비스 입력 정책이다.

## 후보 구성

1. READY인 진화 행동을 준비 후보로 모은다.
2. 그 결과에서 실제 후속 획득 행동이 적용 가능하고, 준비 전 상태보다 목표를 진전시키는 대표 결과가 있으면 두 단계를 묶은 `Choice(initial=[진화], cycle=[후속])`를 검토한다.
3. 후속은 목표 획득 purpose이거나 교체 옵션을 가진 행동이어야 한다. 일반 품질 작업을 임의 후속으로 연결하지 않는다.
4. 일반 후보/준비 연결 결과가 하나도 없으면 단독 진화 `Choice(initial=[], cycle=[진화])`를 남긴다. 후속 결과가 있는 경우 단독 진화까지 무조건 추가하지 않는다.

이 과정은 마법 결과 이후 제왕 또는 적법한 에센스 경로를 이어갈 수 있도록 한다. 후속 화폐는 실제 inventory·목표·상태에 따라 결정하며 “진화 다음은 항상 제왕”으로 고정하지 않는다.

## 종료와 금지

단독 등급 상승은 희귀도 변화 자체가 checkpoint가 될 수 있다. 그렇다고 그 checkpoint를 새 탐색의 시작 단계로 자동 승격하지 않는다. 준비 중 목표 달성은 기존 `STOP_ON_FIRST_HIT`과 품질 예외를 따른다.

- 일반 목걸이에서 Rare 목표로 진행하기 위해 연금술을 추가하지 않는다.
- 마법 1옵션→제왕 결과를 항상 카오스로 연결하지 않는다. 실제 변경 가능 옵션 수를 검사한다.
- 등급 상승이라는 이유로 슬롯·레벨·배타 계열·확률을 생략하지 않는다.

## 비교 사례

- 긍정: Normal 태양 목걸이에서 진화를 거쳐 Rare 생명력 목표에 도달하는 적법한 경로.
- 부정: 같은 `RARITY_UPGRADE`를 목걸이 외 장비에 정책 검토 없이 적용.
- 부정: 후속 획득 후보와 별개로 모든 단독 진화 결과를 항상 중복 열거하여 탐색 순서/예산 사용을 변경.

## 근거

- [AutomaticCurrencyPolicy](../../../backend/src/main/java/com/poe2craft/shared/crafting/domain/AutomaticCurrencyPolicy.java): `RARITY_UPGRADE` 판정.
- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): 준비 후보와 결과가 비었을 때의 단독 등급 상승.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `rarityCheckpoint`, `classify`.
- [목표 편집기](../../../frontend/src/features/crafting/goal-filter/editor.ts): `rareGoal`.
- [AutomaticNormalStartTest](../../../backend/src/test/java/com/poe2craft/bootstrap/AutomaticNormalStartTest.java): `normalSolarFindsRareLifeWithoutAlchemy`.
