# Goal filter

시작 아이템·목표 편집과 수치 판정을 담당한다. [목표 조건](../../../../../docs/current/goals.md), [자동 탐색](../../../../../docs/current/automatic-planning.md), [아이템 상태](../../../../../docs/current/item-state.md)를 따른다.

- `CraftStart.tsx`: 시작 아이템·목표 설정과 자동 계산 연결.
- `GoalFilterPanel.tsx`, `editor.ts`: 그룹·수치 조건 편집과 입력 상태.
- `numericApi.ts`, `api.ts`: catalog·판정·추천 응답 검증, 취소·identity 경계.
- [공유 fixture](../../../../../contracts/support-goal-filter-v1/README.md): 합성 판정 계약. 게임 규칙 근거와 구분한다.

기존 입력·원문·목표의 catalogVersion을 보존하고 늦은 응답을 폐기한다. 표시 행 수를 explicit 옵션 수로 계산하지 않는다. 구형 API의 wire 의미와 현재 자동 계산의 지원 범위를 구분한다.
