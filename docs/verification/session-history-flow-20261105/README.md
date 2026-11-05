# 세션 제작 흐름 디자인과 실제 DOM PNG

2026-11-05. 기준 `a98906e6b80d22fd3b6238b21be016633123e2a1`, branch `codex/session-history-flow-design`.

## 변경과 자체 리뷰

- 기존 작업대의 로고·헤더·탭·타이포·올리브색 버튼·금색 경계와 같은 CSS를 사용한다. 별도 청회색 패널을 제거했다.
- 흐름 본문은 실제 ItemCard·아이템 이미지와 화폐 라벨이 있는 SVG 연결 화살표만 표시한다. 기본 4열 ㄹ 방향, 중간 2열·Mobile 1열이며 읽기 순서는 시간순이다. 기존 기보의 화폐·소비 징조·반복 횟수를 재사용한다.
- 별도 History 목록, 내부 ID·Ruleset, 저장된 데이터/원시 JSON 펼침, 원문 펼침과 상시 저장 구현 안내를 제거했다. 원문·미해석 정보·모든 Frame은 기존 저장본과 JSON 파일에 보존한다. 관측된 소켓·효과·스킬은 카드 속성으로 표시한다.
- 공유 범위와 원문 포함 안내는 공유 버튼을 누른 시점에만 간결하게 표시한다. 실제 오류·가져오기 검증·보존 경고는 유지한다. 개발상 설명은 사이트에 추가하지 않았다.
- PNG는 텍스트 재작성 대신 같은 DOM·computed style·이미지·폰트·현재 viewport의 줄바꿈을 캡처한다. Chromium에서 빠지던 중첩 SVG는 실제 벡터를 같은 크기로 래스터화해 포함한다. 소수점 좌표의 외곽 픽셀과 징조 아이콘 행의 줄바꿈도 검증했다. 한 장이면 바로 다운로드하고 다중 파일 링크는 상단에 표시한다. 본문에 PNG 미리보기를 중복 추가하지 않는다.
- 긴 흐름은 카드 행 경계에서 분할한다. 각 연결 화살표는 온전히 다음 페이지에 남긴다. 실제 49개 상태의 고해상도 분할에서도 전체 높이를 보존했다. 새로운 중간 제작 상태를 추정하지 않는다.

## 검증

- Frontend lint·typecheck·format:check·build 통과. 의존성·잠금 파일 변경 없음.
- 히스토리·파일·기보·ItemCard 직접 영향 검사 5개 파일, 63개 통과. 전체 무거운 suite와 Backend 전수 검사는 UI 변경 범위상 반복하지 않았다. Backend 실행 코드는 변경하지 않았다.
- 실제 독립 API와 Edge: [65개 확인](checks.json), pageerror 없음. 실제 Chaos 반복 2회와 Omen of Whittling 소비, 파일 export/import의 정확한 Film 보존, 서버 공유의 정확한 Film 조회를 포함한다.
- 6개 언어를 Desktop 1700px와 Mobile 390px에서 확인했다. 1·4·5·8·9개 상태와 900px의 2열 방향 전환, 긴 화폐 라벨, 횟수·징조를 확인했다.
- 같은 viewport·같은 상태의 페이지 흐름과 실제 저장 PNG를 직접 열어 비교했다. 캡처 크기가 같고 8개 화살표의 정방향·역방향·행 전환 선과 화살촉을 모두 확인했다. 카드 경계·아이콘·폰트·색·줄바꿈을 비교했으며 pixel-identical이라고 주장하지 않는다. [Desktop 수치](desktop-comparison.json) · [Mobile 수치](mobile-comparison.json).

## 같은 내용의 화면과 PNG

| 구분 | 페이지 흐름 | 실제 PNG |
|---|---|---|
| Desktop | [페이지](desktop-page-flow.png) | [저장 PNG](desktop-snapshot.png) |
| Mobile | [페이지](mobile-page-flow.png) | [저장 PNG](mobile-snapshot.png) |
| 긴 흐름, 49개 실제 상태 | [전체 페이지](long-page-flow.png) | [첫 장](long-snapshot-1.png), [둘째 장](long-snapshot-2.png) |

[Desktop 전체 UI](desktop-page.png) · [Mobile 전체 UI](mobile-page.png). 샘플은 공개 합성 아이템인 ‘병사 흉갑 제작’이며 실제 사용자 제목·데이터는 변경하지 않았다. [재현 검사](../../../scripts/qa-session-history-flow.mjs).

## 실제 9001 변경 전 확인

동일 공개 세션의 [변경 전 실제 화면](deployed-before.png)과 [문구·펼침 조사](before-notes.json)를 기록했다. 상시 로컬 저장/localhost 안내·원문 공유 설명·상태 개수, 별도 History 목록 1개, ‘저장된 데이터’ 8개와 원문 펼침 1개를 실제 확인했다. 9개 펼침을 열어 원시 데이터가 보이는 것도 확인했다. 반영 후 결과는 후속 서버 검증 기록에 남긴다.
