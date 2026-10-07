# Goal filter v1 fixtures

공통 규칙은 [계약 문서](../../docs/support-goal-filter-v1.md)에 있다. `fixtures.json`의 값과 stat/modifier/base ID는 모두 합성 데이터이며 production catalog나 게임의 수치 확률을 증명하지 않는다.

`python contracts/support-goal-filter-v1/verify_fixtures.py`로 JSON, 판정 24건, validation 9건 및 API 예제의 질량 보존을 확인한다. 이 독립 oracle은 앱 구현 테스트가 아니다. FE/BE 담당은 같은 fixture를 자신의 구현 테스트에서 읽어 비교한다.

판정 case는 entryTemplates 참조를 펼치고 case의 type/range/weights/disabledEntries를 적용해 그룹 하나를 만든다. observedStats의 누락 key는 검증된 아이템의 ABSENT이고 명시 null은 UNKNOWN이다. `expected`는 해당 그룹의 판정이다. API 예제는 이미 펼친 wire shape다. `recommendPartialResponse`는 미래의 검증된 수치 분포 구현을 위한 응답 shape이며 현재 엔진의 출력이나 지원 선언이 아니다.

추가 통합 회귀 요구는 `handoffRegressionRequirements`에 있다. 실제 catalog 매핑, 전체 ItemState 적격성, disabled 그룹과 여러 그룹 결합, production API 및 legacy 엔진 회귀는 후속 FE/BE 구현 검증 범위다.


Production Solar numeric addition now returns COMPLETE/PARTIAL under the declared `solar-numeric-addition-v1` / `uniform-integer-roll-v1` model. Historical synthetic unsupported examples remain unsupported fixtures; they do not describe current production capabilities. `probability.interpretation` and `assumptions` explain model versus game evidence. Comparisons include exactMass (success/failure/unresolved/terminalFailure/illegalActionFailure rational numerator and denominator); display numbers are approximations. Empty sequence means Stop. Partial ranking is uncertified; comparedSequences may be less than finite totalSequences.
