# 작업대 화면과 사용 흐름

현재 연결과 보존 계약은 [통합 유지보수 문서](../../../development.md)를 따른다. 이전 버전의 화면 캡처·수집·배포 로그는 남기지 않는다. 현재 Frontend 검사 결과는 [release/checks/frontend.json](../release/checks/frontend.json)에 있다.

| 확인할 기능 | 관련 자료와 검사 |
|---|---|
| 붙여넣기·관측 상태 | 통합 문서의 베이스/텍스트 입력, ItemTextImport·ItemTextService 검사 |
| 세션·기보·반복 횟수 | 통합 문서의 Film/세션/기보, workbenchHistory·session-notation 검사 |
| 창고·메뉴·내부 스크롤 | feedback-tooltip 설명, stash·base-picker·compact-viewport 검사 |
| 선택·오류·툴팁 피드백 | feedback-tooltip의 공통 설명 수명과 우선순위, 관련 DOM 검사 |

새로운 화면 근거가 생기면 import/history/layout/feedback에 기능별로 둔다. 데이터가 없는 자리를 과거 실행 결과로 채우거나 jsdom 결과를 실제 브라우저 확인으로 표시하지 않는다.
