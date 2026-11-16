# 공통 정책과 화폐 후보

문서 ID: `support.common-policy` · 적용: 현재 자동 탐색 전체 · 검색: 항상 포함.

## 적용 조건과 경계

이 문서는 [corpus.json](../corpus.json)의 기준 버전에서 자동 탐색할 행동을 제한한다. 수동 제작 작업대의 화폐 사용을 제한하는 문서가 아니다. 아래의 정책상 허용, 게임상 적용 가능, 확률 계산 지원은 서로 다르다. 정책에서 허용돼도 실제 kernel이 미지원/사용 불가를 반환할 수 있다.

정책 원본은 [automatic-currency-policy-v1.json](../../../backend/src/main/resources/crafting/automatic-currency-policy-v1.json)이다. 기준 버전은 총 187항목이며 `USE` 33개, `CONDITIONAL` 16개, `EXCLUDED` 109개, `DEFERRED` 29개다. 개별 허용 목록은 이 원본을 조회한다.

- `USE/CONDITIONAL`만 후보 목록의 출발점이다. `EXCLUDED/DEFERRED`는 새 후보로 추가하지 않는다.
- 분열 화폐와 연금술·소멸, 개별 기폭제는 현재 자동 정책에서 제외한다. 이미 분열된 아이템을 입력하는 지원과 분열 화폐를 탐색하는 지원은 다르다.
- 좌측/우측 말소·절사 징조는 현재 보류다. 관련 Java 코드나 과거 테스트가 존재해도 활성 후보로 복구하지 않는다.
- `AutomaticCurrencyPolicy`의 `MANUAL_ONLY_CURRENCIES`와 `OMEN:LIGHT` 제외, 보조 재료 alias를 유지한다. 미검토 ID를 허용으로 추정하지 않는다.
- 최대 6옵션은 현재 탐색 범위다. 각 장비의 실제 슬롯 제한은 엔진이 별도로 확인한다. 탐색 경계 밖 질량을 성공/실패 확률에서 조용히 지우지 않는다.

## 행동 목록 구성

`AutomaticCraftingAction.candidates`는 정책의 원래 순서로 화폐를 읽는다. `OMEN:`·`EXTRA:` 항목은 단독 화폐 행동으로 만들지 않는다.

| 화폐/종류 | 기본 purpose와 추가 변형 |
|---|---|
| 진화 | `RARITY_UPGRADE` |
| 제왕·엑잘 | `TEMPORARY_AFFIX_FOR_REMOVAL` 및 `GOAL_ACQUISITION` |
| 카오스 | `ORDINARY`, `GOAL_ACQUISITION`, `TARGET_PREPARATION`이 구성될 수 있으나 실제 사용은 정책/kernel이 필터링 |
| 균열 에센스 | 일반 행동에 더해 `QUALITY_CHECKPOINT` |
| 나머지 | `ORDINARY` |

제왕은 좌/우 대관식, 엑잘은 좌/우 찬미, 교체형 에센스는 좌/우 결정화 변형을 구성한다. 제왕·엑잘의 징조 변형은 임시 추가/목표 획득 purpose를 구분한다. 카오스의 절사·좌/우 말소 조합은 구성 코드가 있어도 현재 징조 정책에서 걸러진다.

현재 inventory의 징조 허용 검사는 `Context("amulets", purpose, currency)`를 사용한다. 이후 kernel은 실제 장비 family로 화폐와 징조를 재검증한다. 이 하드코딩은 [검토 기록](../review-notes.md)에 남기며, 현재 동등성 이관에서 임의로 바꾸지 않는다.

## 장비와 purpose 제한

| 조건 ID | 현재 서비스 허용 범위 |
|---|---|
| `RARITY_UPGRADE` | 목걸이 + 진화 + 등급 상승 purpose |
| `FIRST_AFFIX_ACQUISITION` | 목표 획득 purpose. 상태/시작 단계 제한은 [카오스 문서](first-affix-chaos.md) |
| `TEMPORARY_REMOVAL` | 현재 실행 코드는 임시 추가 또는 목표 획득 purpose 허용. 엑잘에는 추가 엄격 제한 적용 |
| `TEMPORARY_REGAL_PREFIX/SUFFIX` | 위 purpose와 제왕의 조합 |
| `TEMPORARY_EXALTED_PREFIX/SUFFIX` | 위 purpose와 엑잘의 조합 |
| `HYSTERIA_PARTS` | 장갑·장화·화살통 |
| `PRISMATIC_PARTS` | 장갑 |
| `SWIFT_PARTS` | 장갑·반지·방패·버클러·집중구 |
| `SOVEREIGN_PARTS` | 반지·목걸이·허리띠 |
| `EXPANSIVE_PARTS` | 장화 |
| `CYCLONIC_PARTS` | 장화·장갑·투구 |
| `MYSTIC_PARTS` | 투구·장갑·장화·화살통 |
| `BREACH_QUALITY` | 정책 판정 자체는 허용, 품질 작업의 상태 자격은 별도 검증 |

표는 현재 `AutomaticCurrencyPolicy.evaluate`의 추출이며 새로운 게임 적용 범위를 선언하지 않는다. JSON 설명의 제왕 제한과 실제 purpose 허용의 차이는 [검토 기록](../review-notes.md)을 따른다.

## 금지와 사례

- 긍정: 이미 분열된 희귀 시작 아이템에서 허용된 카오스 후보를 검토한다.
- 부정: 위 사례를 이유로 분열 화폐까지 자동 후보에 추가한다.
- 부정: 절사에 대한 과거 공략/코드를 검색했다는 이유로 현재 보류를 해제한다.
- 부정: 지원 목록에 있다는 이유만으로 해당 아이템에 사용 가능하거나 정확한 확률이 있다고 단정한다.

## 근거

- [AutomaticCurrencyPolicy](../../../backend/src/main/java/com/poe2craft/shared/crafting/domain/AutomaticCurrencyPolicy.java): `candidates`, `evaluate`, `withinExplorationBoundary`.
- [AutomaticCraftingAction](../../../backend/src/main/java/com/poe2craft/shared/crafting/domain/AutomaticCraftingAction.java): `candidates`.
- [AutomaticCurrencyPolicyTest](../../../backend/src/test/java/com/poe2craft/shared/crafting/domain/AutomaticCurrencyPolicyTest.java): 화폐 범위·purpose·장비 제한.
