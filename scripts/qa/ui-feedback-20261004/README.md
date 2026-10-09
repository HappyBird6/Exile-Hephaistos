# UI feedback browser QA

`browser.cjs`는 격리 runtime의 `http://host.docker.internal:19081`에 연결한다. source를 `/source:ro`, 새 QA 출력 디렉터리를 `/evidence`에 mount하고 기존 `exile-workbench-browser:20261002` 이미지의 Playwright를 사용한다. 각 실행은 새 browser context를 만들며 사용자 browser profile을 열지 않는다. Backend/API는 게시된 master jar의 별도 복사본과 disposable DB를 사용한다.

- `baseline-feedback.cjs`: 변경 전 native locale selector를 사용하는 runtime에서 Spanish Catalyst 설명의 경계 이동을 측정한다.
- `browser.cjs`: 6개 locale × 6개 viewport, menu keyboard/focus, repeated tooltip hover, 설명 선택, film/Shift/Alt/undo/redo/orange preview, base/new craft 및 Admin routing 검증과 screenshot.
- `long-text.cjs`: Spanish/CJK mobile 설명의 End-key 내부 스크롤과 고정 경계, 실제 tooltip viewport screenshot. full-page 캡처의 scroll은 tooltip을 닫으므로 tooltip screenshot은 viewport 방식이다.
- `contact-sheets.cjs`: 전체 screenshot을 locale별로 모아 시각 검사를 돕는다.

frontend 필수 검사는 독립 소스 복사본에서 순차 수행하고 formatter 결과를 source로 되돌린 뒤 byte 단위 동등성을 확인한다. `.env`, 사용자 DB volume, 18080/18081 서비스와 다른 작업의 출력 디렉터리는 사용하거나 덮어쓰지 않는다. `QA_RUN`을 지정하면 browser 실패 증거 파일 이름을 구분할 수 있다.

이번 검증 출력은 `codex/ui-feedback-qa-20261004`에 보존했다. 요약은 `docs/evidence/workbench-ui-feedback-validation-2026-10-04.json`, UI 명세와 원인 분석은 `docs/workbench-ui-feedback-2026-10-04.md`에 있다. Backend·Windows 실행 스크립트는 변경하지 않았다.
