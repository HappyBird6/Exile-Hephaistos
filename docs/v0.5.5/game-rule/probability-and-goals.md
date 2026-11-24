# 확률·목표·자동 제작

[registry의 assumptionLedger](../game-data/crafting/registry-v2.json)와 [자동 화폐 정책](../game-data/crafting/automatic-currency-policy-v1.json)이 명시한 범위에서 계산한다. 게시 weight와 1/N 가정은 구분한다. 알려진/미확인 weight를 근거 없이 섞거나 미지원 결합분포를 균등화하지 않는다.

조건 줄 수와 필요한 옵션 수는 다르다. 복합 modifier가 여러 조건을 충족할 수 있으며 목표의 그룹 연산과 최소 필요 옵션 계산을 적용한다. 고정 옵션은 목표 선택에서 제외한다. [goalfilter 정의](../game-data/crafting/goalfilter/definitions-v1.json)의 직접 stat 단위·변환과 pseudo의 source 참조를 보존한다. Total 화염·냉기·번개 저항과 Total 힘·민첩·지능은 목표 선택과 새 계산에서 제외한다. 과거 결과는 당시 수치로 열람할 수 있다. 자세한 제작지원 계산은 [경로 규칙](support-test-routes.md)을 따른다.

5·6옵션 시작 상태도 카탈로그 자격을 검증한다. 추가 전이는 빈 슬롯과 적격 후보가 있을 때만 가능하다. 확률이 높은 한 방법과 전체 전략의 완료는 구분하며 미완성 경로를 성공으로 표현하지 않는다. 수동 동작과 자동 탐색의 허용 정책은 다를 수 있다.

사용자 정책의 세부 정정·예외는 [사용자 정책](user-policy.md)에 보존한다. 과거 자료의 고정 횟수·개수를 현재 제한으로 복원하지 않는다. API의 반복 횟수·본문·패턴 제한은 게임 확률 규칙과 별개이며 루트 README의 자동 요약과 contracts 원본을 따른다.
