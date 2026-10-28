# 그래프 UI

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 후속 상태 강조

상태 노드에 마우스를 올리면 해당 노드부터 표시된 그래프의 방향을 따라 도달하는 후속 노드·연결선·화폐 방법 라벨을 함께 강조한다. outgoing edge와 방문 집합을 사용하므로 합류·분기·순환에서 반복 탐색을 막는다. 역방향 조상이나 도달하지 않는 형제 경로는 포함하지 않는다.

키보드 focus-visible에도 같은 강조를 제공한다. hover가 focus보다 우선하며, 마우스와 키보드 강조 원인이 모두 사라지면 원복된다. 패닝 중에는 후속 강조를 숨긴다. 이 강조는 화면 projection의 연결 관계를 표현하며 계산된 모든 내부 상태를 추가 탐색하는 기능은 아니다.

## 기존 상호작용

노드 클릭 선택·추천 경로와 후속 강조는 별도로 관리한다. hover 해제 때문에 클릭 선택을 지우지 않는다. 드래그 패닝, Ctrl+휠 줌, 툴팁 동작을 유지한다. 줌 범위는 10%–200%이며 일반 휠을 Ctrl 줌으로 취급하지 않는다.

후속 강조는 UI 상태만 바꾸며 확률·계산 엔진·저장 자료를 변경하지 않는다. 기존 hover/focus·해제·선택 회귀 테스트는 stateProjection.test.tsx와 관련 그래프 테스트에서 확인한다.

## 화면 언어

한국어 기본의 6개 locale을 지원한다. [지원 범위](support.md)의 언어 fallback과 영어 아이템 텍스트 입력 경계를 구분한다. 초기 영어 UI 전용 설명·자산 출처는 [초기 UI 원문](../archive/2026-10/spec-snapshots-49190b5/docs__crafting-ui.md.txt)에 보존한다.


## 구현 근거

[downstream.ts](../../frontend/src/features/crafting/method-search/downstream.ts) · [GroupedMethodGraph.tsx](../../frontend/src/features/crafting/method-search/GroupedMethodGraph.tsx) · [useGraphWheelZoom.ts](../../frontend/src/features/crafting/method-search/useGraphWheelZoom.ts)

[문서 지도](../README.md)
