# Workbench UI 피드백 반영

2026-10-04. 제작 규칙과 catalog, film 저장 구조는 변경하지 않는다.

- Admin은 workspace navigation 오른쪽의 링크 형태로 표시한다. 실제 `/admin` 이동, 최소 페이지와 `/admin/crawling` 주소 정리, 서버 요청 없는 접근 동작을 유지한다.
- 언어 선택은 dark palette의 compact popover다. English, 한국어, 简体中文, 繁體中文, 日本語, Español 순서와 Korean 기본값·기존 locale 저장 키를 유지한다. menuitemradio 선택 상태, 방향키·Home·End·Escape·Tab, focus 복귀와 외부 pointer/focus 닫기를 지원한다.
- 기존 stash texture는 `.stash-viewport`에만 적용한다. `.bench-lower`는 별도 녹회색 gradient와 경계로 ItemCard의 글자 대비를 유지한다.
- Select Base와 New Craft는 `.bench-lower` 첫 영역이다. 기존 dialog, 입력 취소, focus 복귀, 새 film 시작 동작은 유지한다.
- 남는 viewport 높이가 있으면 stash-panel 아래에 최대 24px의 여백을 둔다. 여백을 확보하기 위해 추가 스크롤을 만들지 않는다. canvas 안쪽의 margin을 바깥으로 옮겼다.
- 가변 설명은 84px 높이의 Crafting notes 영역에서 읽는다. 긴 내용은 내부 스크롤로 보존하며 keyboard focus와 6개 언어 label을 제공한다. tooltip은 body portal에 표시하고 locale 변경 시 위치를 재계산한다.

## 원인 확인

변경 전 `7d27d6c`의 isolated runtime에서 screenshot과 DOM 경계를 측정했다. 순수 tooltip hover는 1440×1400에서 문서 기준 하단 이동 0px였다. 작은 viewport에서 Playwright `hover()`의 자동 스크롤은 화면 좌표를 움직였으므로 문서 좌표와 높이를 분리했다.

실제 layout 변화는 Spanish Catalyst 선택 설명에서 확인했다. 설명 높이가 12px → 74px로 증가하면서 stash-panel 하단이 1111.671875px → 1173.671875px, 즉 62px 이동했다. Escape 후 설명이 30px 남아 하단도 1129.671875px에 남았다. 일정 높이의 설명 영역은 이 변화를 흡수하고, body portal은 tooltip을 workbench DOM 흐름에서 분리한다.

## 검증

별도 Docker Compose project `exile-ui-feedback-20261004`와 localhost 19080/19081, disposable PostgreSQL tmpfs, 새 Playwright context를 사용한다. 기존 18080/18081 서비스, 사용자 browser storage와 DB volume은 사용하거나 변경하지 않는다. Backend는 게시된 master의 jar를 별도 복사하여 사용하며 Backend 소스는 변경하지 않는다.

Frontend 필수 검사와 browser QA 결과 및 screenshot은 전용 QA 폴더에 보존한다. browser QA는 6개 언어와 1920×1080, 1440×900, 1440×1100, 1440×1400, 1024×768, 390×844에서 실행한다. repeated hover, selected 설명, navigation/Admin/back/forward, base/new craft, film reload, Shift/Alt, undo/redo와 orange preview를 확인한다.

실패 이력은 QA의 `failure-history.json`에 보존한다. 첫 lint의 indexed access 줄바꿈 문제와 새 focus 테스트의 React `act` 누락을 수정했다. assertion·지원 규칙·기대값을 완화하지 않았다. 자체 리뷰와 시각 검사를 수행하며 게시·배포는 별도 사용자 지시 범위다.

최종 `npm ci`, lint, typecheck, format check, 56개 파일의 1,741개 테스트와 build가 통과했다. browser 733개 검사가 통과했고 page error는 없었다. 36개 조합의 hover·설명 선택·Escape 후 문서 기준 하단 이동은 모두 0px였다. Spanish 1440×1400에서 stash 아래 실제 여백은 24px이고, 높이가 부족한 조합에서는 0px로 추가 여백을 강제하지 않는다. Spanish mobile 설명은 172px 전체 내용 중 84px viewport에서 End 키로 88px 스크롤했고 경계 이동은 0px였다. Chinese 두 종류와 Japanese도 같은 keyboard 검증을 통과했다.

6개 viewport contact sheet, 6개 언어 menu, desktop/mobile 원본, 긴 설명과 실제 tooltip viewport, Admin screenshot을 시각 검사했다. tooltip의 full-page 캡처는 자체 scroll dismiss 동작 때문에 사라져 새 viewport 캡처를 사용했으며 기존 캡처도 보존했다. Backend·Windows 실행 스크립트 검사는 해당 소스 변경이 없어 실행하지 않았다. 기존 큰 i18n bundle에 대한 Vite 경고는 남아 있으며 범위 밖 chunk splitting은 추가하지 않았다. 세부 수치는 [검증 증거](evidence/workbench-ui-feedback-validation-2026-10-04.json)를 따른다.
