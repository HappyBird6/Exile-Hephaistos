# 화폐와 제작 규칙

실행 정의는 [workbench-definitions](../game-data/crafting/workbench-definitions-v1.json), 재료 등록·지원 범위·가정은 [registry](../game-data/crafting/registry-v2.json), 자동 사용 정책은 [automatic-currency-policy](../game-data/crafting/automatic-currency-policy-v1.json)를 따른다. 등록은 실행 지원을 뜻하지 않으며 DEFERRED를 임의로 활성화하지 않는다.

## 적용 자격

베이스·희귀도·레벨·접두/접미 슬롯·family 충돌·분열·훼손·징조 조건을 함께 검증한다. 베이스별 자격은 [base-policies](../game-data/crafting/policies/base-support.json)와 [supported-base-sets](../game-data/crafting/supported-base-sets-v1.json)에 있다. 이름만으로 화폐 효과·대상·최소 레벨을 추정하지 않는다.

화폐 action과 재료 ID는 서로 다른 식별 공간이다. 징조도 실행 key와 표시용 재료 ID를 구분한다. 이번에 부여한 짧은 ID는 아직 실행·통신에 사용하지 않는다.

## 에센스 교체

일반 상태와 숨겨진 훼손 슬롯 모두 같은 교체 규칙을 사용한다. 가능한 결과 옵션을 균등 선택하고, 해당 쪽 슬롯이 가득 찼다면 같은 쪽의 비분열·징조 조건을 만족하는 옵션에서 제거한다. 다른 family 충돌을 임의 재추첨으로 숨기지 않는다. 결과별 제거 후보 수가 다르면 경로 확률도 다르다. 합금·액체 감정은 각각의 선택 규칙을 유지한다.

고정 결과·대상 목록은 [top-base-essences](../game-data/crafting/essences/targets.json), [equipment-currencies](../game-data/crafting/alloys/targets.json), 각 pool의 catalog JSON에 있다. 배열 순서는 동작 계약이므로 정렬하거나 임의로 중복을 제거하지 않는다.

## 품질·축성·숨겨진 상태

상한이 낮아졌다는 이유로 보존 대상으로 정한 기존 품질이나 Crafted 옵션을 삭제하지 않는다. 기폭제 자격·품질 한도는 베이스별이다. 품질 소수 처리는 현재 사용자 정책인 절댓값 반올림 후 부호 복원을 사용하며, 게임 정밀도의 검증 결과로 주장하지 않는다.

축성은 비분열 명시 속성에만 적용한다. 고정·분열 옵션과 구조용 수치는 축성 계수 생성 및 표시 계산에서 제외한다. 구버전 계수는 저장 근거로 읽되 해당 제외 대상의 계산에 사용하지 않는다. 공통 사례는 [projection-cases](../../contracts/sanctification/projection-cases.json)다.

숨겨진 훼손 상태를 표시 문자열로 추측하거나 자동 공개하지 않는다. 감정·드러내기는 검토된 자격·슬롯·보존 계약을 따르며, 수동 표시와 일반 제작 실행을 구분한다.
