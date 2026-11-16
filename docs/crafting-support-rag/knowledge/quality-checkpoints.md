# 품질 20·40 작업

문서 ID: `support.quality-checkpoints` · 적용: 기존 자동 품질 목표·균열 준비 · 의존: [공통 정책](common-policy.md), [목표 진전](goal-progress.md).

## 이 작업의 의미

현재 자동 품질은 **종류·수량을 정하지 않은 추상 작업**이다. 개별 기폭제를 실제 횟수만큼 사용하는 모델로 해석하지 않는다. `ESSENCE_BREACH`라는 행동 ID를 사용하더라도 `QUALITY_BASE_CHECKPOINT/QUALITY_CHECKPOINT` purpose이면 실제 균열 에센스 사용과 다르다.

현재 목표 범위는 0~40이다. 완료 품질은 추상 작업량과 실제 `catalystQuality.amount` 중 큰 값이다. 품질 목표가 있으면 기존 목표 판정 행 뒤에 품질 판정이 붙는다. 20 이하 지원은 `baseQualitySupported`, 20 초과는 추가로 `BaseRegistry.maximumQualityBreach`를 확인한다.

## 작업 선택 조건

| 현재 조건 | 생성/우선 처리 |
|---|---|
| 목표가 1~20, 아직 품질 미달, 베이스 지원 | 기본 품질 작업 `QUALITY_BASE_CHECKPOINT` 후보 추가 |
| 목표가 20 초과, 아직 품질 미달, 품질 상한 옵션 없음 | 적법한 일반 `ESSENCE_BREACH` 후보를 목표 진전 조건의 예외로 허용 |
| 목표가 20 초과, 아직 품질 미달, 품질 상한 옵션 있음 | `QUALITY_CHECKPOINT`만 먼저 반환 |
| planner 평가상 전체 목표 MATCH이고 품질 상한 옵션 있음 | 품질 작업 후보만 반환하는 기존 특례 유지 |

품질 상한 감지는 현재 `hasQualityCap`: `QualityLimitRules.BREACH_ID`인 옵션에서 `QualityLimitRules.STAT_ID`의 값이 정확히 20인지 확인한다. 이름이 비슷하거나 다른 출처의 상한 옵션을 자동으로 같은 것으로 해석하지 않는다.

위 표의 전체 목표 MATCH 특례는 “완료한 아이템에 항상 품질 작업을 다시 넣는다”는 뜻이 아니다. graph는 상한 옵션이 있고 작업 품질이 40 미만일 때 완료 노드를 보류하며, 이미 완료로 판정된 노드는 후보 발견을 다시 하지 않는다. graph의 `qualityProgress`·노드 생성과 planner 판단을 함께 유지한다.

## 실행과 예외

- 기본 작업은 아이템 옵션을 바꾸지 않고 추상 품질을 최소 20으로 만든다. 40 작업은 추상 품질을 40으로 만든다. 목표가 30이어도 현재 작업 단위는 40이며, 정확히 30까지만 작업하는 새 모델을 만들지 않는다.
- 40 작업 실행에는 실제 해당 상한 옵션이 있고 검증된 최대 품질이 40 이상이어야 한다. 상한을 확보하기 전에 작업을 실행하지 않는다.
- 품질을 위한 상한 확보는 이미 맞춘 옵션을 교체하는 예외 경로가 존재한다. 일반 목표 보존 조건만으로 이 후보를 제거하지 않는다. 실제 전이와 checkpoint 처리는 기존 엔진에 맡긴다.
- 40 작업 완료 전에 상한 옵션이 제거되는 결과는 `QUALITY_CAP_REMOVAL_BEFORE_40` 미해결 질량으로 남긴다. 버리거나 실패 확률 0으로 바꾸지 않는다.
- 품질 작업 후 상한 옵션이 남아 있다는 이유로 완료를 막거나 강제 제거하지 않는다. 40 품질이 이미 확인된 상태에 같은 작업 비용을 다시 부여하지 않는다.
- 소모 기록은 `WORK:QUALITY_20`·`WORK:QUALITY_40`이다. 실제 기폭제 가격/수량이나 균열 에센스 한 개 가격으로 임의 치환하지 않는다.

이 특례가 실제 게임의 품질 효과·수치 투영을 모두 구현했다는 뜻은 아니다. HTTP에서 받아들이는 상태 범위와 도메인 단위 검사 범위도 다를 수 있다.

## 비교 사례

- 긍정: 품질 20 목표는 균열 옵션을 새로 확보하지 않고 기본 작업 경로를 사용.
- 긍정: 품질 30 목표는 상한을 확보하고 40 작업 후 완료 판정.
- 부정: 품질 작업을 위해 기존 MATCH 옵션이 교체되는 후보를 일반 보존 규칙으로 모두 삭제.
- 부정: 품질 작업을 실제 특정 기폭제 N회 사용으로 추천하거나 비용에 포함.

## 근거

- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): 품질 우선 반환·균열 예외.
- [AutomaticCraftingKernel](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticCraftingKernel.java): `workedQuality`, `hasQualityCap`, 품질 목적의 `expand`.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `qualityProgress`, 노드 완료 판정.
- [AutomaticMethodExecution](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodExecution.java): 품질 상태·종료 처리·`materialUses`.
- [AutomaticQualityGoalsTest](../../../backend/src/test/java/com/poe2craft/bootstrap/AutomaticQualityGoalsTest.java): 20/40 작업, 상한 잔존, 재획득, 저장 복원.
