# 목표 조건

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 조건 수와 옵션 수

목표의 행 수를 아이템 옵션 개수로 세지 않는다. 한 modifier가 복수 stat 조건을 만족할 수 있고, 동일 family/조건의 중복은 별개의 슬롯을 요구한다고 단정할 수 없다. 목표 검증은 카탈로그 자격과 최소 필요 explicit modifier 수를 사용한다.

AND는 함께 만족해야 할 조건, OR는 가능한 대안이다. OR의 모든 행을 한 아이템에 동시에 요구하지 않는다. 6행 이하라도 접두/접미·family·수치 조건이 충돌하면 실현 가능한 목표가 아닐 수 있다. 7개 이상의 독립 옵션을 반드시 요구하는 목표는 6옵션 탐색 범위를 벗어난다.

## 입력 검증과 탐색 결과

시작 상태는 [ItemState](item-state.md) 검증을, 목표는 해당 base의 검토 카탈로그를 사용한다. 잘못된 목표 입력, 지원하지 않는 확률 모델, 예산 내 해결되지 않은 탐색은 서로 다른 결과다. 계산 제한을 “확률 0” 또는 “존재하지 않는 아이템”으로 해석하지 않는다.

5·6옵션 목표와 4옵션 출발의 전이, 복합 조건·OR 사례는 [6옵션 검증 기록](../development.md#validation)을 참고한다. 구형 [Support 목표 v1](../support-goal-filter-v1.md)의 제한을 현행 목표 전체에 그대로 적용하지 않는다.


## 구현 근거

[BundledGoalCatalogs.java](../../backend/src/main/java/com/poe2craft/craftingsupport/goal/infrastructure/BundledGoalCatalogs.java) · [SixAffixScenariosTest.java](../../backend/src/test/java/com/poe2craft/bootstrap/SixAffixScenariosTest.java)

[문서 지도](../README.md)
