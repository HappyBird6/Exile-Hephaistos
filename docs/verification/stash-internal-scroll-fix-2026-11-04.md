# Workbench stash 내부 스크롤 회귀 수정

2026-11-04. 기준은 현재 서버 UI `3f4495e664d8112011cc5dcd9f17330446167a97`이며,
clean 상태의 카오스 준비 branch `codex/chaos-repeat-controls`와 commit `ecb65d27`을 보존하고
별도 `codex/stash-internal-scroll-fix` branch를 만들었다. 이 수정과 dist에는 카오스 준비 코드가 없다.

## 원인과 최소 수정

사용자는 많은 창고 항목이 있을 때 내부 스크롤 대신 창고 길이가 늘어나는 현상을 보고했다.
브라우저 재현은 보류 지시에 따라 하지 않았으며, 다음은 실제 commit diff와 CSS 크기 결정 규칙의 분석이다.

- `1def96cd9dbc26fadb9e9819409b888e4855132a`의 `.workbench-stash`는 absolute이고,
  상단 935×550 평면에 대한 `height: calc(704 / 550 * 100%)`가 있어 목록의 크기와 독립된 영역이었다.
- `3f4495e`에서 200px feedback 및 양쪽 하단 정렬을 위해 relative grid item으로 바뀌었다.
  창고는 `grid-row: 1 / 4`, `align-self: stretch`이고 기존 height는 제거되었다.
  부모 `.stash-canvas__content`의 행은 `auto minmax(min-content, 1fr) auto`이며,
  그 전에는 내용의 intrinsic size 기여를 차단하는 경계가 없었다.
- `.workbench-stash__resources`의 `overflow-y: auto`, `min-height: 0`은 남아 있었지만,
  부모 영역을 산정하는 단계에서 창고 목록의 intrinsic 높이까지 참여할 수 있었다.
  따라서 내부 스크롤 속성이 있다는 사실만으로 외부 창고 높이가 제한되지는 않았다.
- **제품 변경은 `.workbench-stash`에 `contain: size` 한 선언과 설명 주석을 추가한 것뿐이다.**
  창고 내용이 grid 행의 intrinsic size를 늘리지 못하게 하고, 기존 상단 평면·여유 행·하단 행과
  오른쪽 열이 정한 grid 영역 안으로 창고가 stretch되게 한다.
  안쪽 flex panel과 resources의 기존 `min-height: 0` / `overflow-y: auto`가 그 영역을 사용한다.

근거: [W3C CSS Containment Level 2, Size Containment](https://www.w3.org/TR/css-contain-2/#size-containment)는
size containment의 intrinsic 크기 계산에서 내용을 빈 것으로 취급하며 이 효과가 grid track 크기 계산에도 적용됨을 명시한다.
이 작업은 그 규칙을 현재 spanning grid item에 적용한 것이다. 실제 브라우저 픽셀 측정 근거와 구분한다.

고정 임의 px 높이·새 max-height·새 overflow clipping·layout/paint containment는 추가하지 않았다.
Page JSX와 compact/global CSS는 수정하지 않았다. 다음 기존 계약을 보존한다.

- feedback 200px, 75% zoom, 374px 오른쪽 열, 1000px 반응형 전환.
- 상단 935×550 좌표, 기존 7개 x track과 40px 상대 상단 여백, 36px 하단 정렬.
- 동일 하단 행의 즐겨찾기 2×5·정사각형 slot 규격·세 버튼과 좁은 화면 최소 208px.
- 단일 `.wb-scrollbar`의 `stable` / `thin` / `#867347 #141a15`와 resources의 실제 스크롤 소유권.
- 재료 데이터·검색·탭·6 locale, 독립 base hover, 공유 feedback의 설명 유지·실제 집은 대상 우선.

## 변경 파일

- `frontend/src/features/crafting/stash/workbenchStash.css`
- `frontend/src/features/crafting/stash/WorkbenchLayout.test.tsx`
- `frontend/src/features/crafting/compact-viewport/compactViewport.test.ts`
- 이 검증 문서.

## 실행 검사와 근거

작업 경로: `C:\Users\SSAFY\.codex\worktrees\6d82\Exile-Hephaistos\frontend`.
package/lockfile는 기준과 동일하며 기존 설치된 node_modules를 재사용했다.

```text
npm run test -- --run src/features/crafting/stash src/features/crafting/compact-viewport src/features/crafting/feedback-tooltip src/features/crafting/base-picker --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/stash-scroll-fix/impact-vitest.json
```

- host 영향 검사: **11 files / 83 tests 통과**, 58.26초, exit 0. 최초 실행에서 통과했다.
- 신규 DOM 검사에서 실제 Essence/Omen/Catalysts/Liquid Emotions 목록이 각각 8개 초과임을 확인하고,
  검색으로 0개가 된 경우·전체 복원·다음 탭 전환에 걸쳐 동일한 grid 크기 결정 선언과 containment,
  내부 flex/min-height/overflow/scrollbar 계약이 유지되는지 검사했다.
- 6 locale의 실제 검색 접근성 이름 및 containment, 기존 10개 favorite slot과 feedback 200px,
  기존 좁은 화면 규칙·좌표·하단 정렬·툴팁·선택 우선 검사도 통과했다.
- `npm run lint`, `npm run typecheck`, `npm run format:check`: exit 0.
- `npm run build`: host exit 0. 500kB 초과 chunk 경고가 있다.
- `git diff --check`: 통과. 새 문서는 staged 검사에서도 확인한다.
- 기존 drag DOM 검사에서 jsdom 좌표 때문에 `NaN is an invalid value for the left css style property` 경고가 나왔다.
  기준 작업 기록에도 있던 경고이며, 실패·skip·규칙 완화로 처리하지 않았다.
- Vitest와 build는 이전 제한 환경의 IPC 문제를 근거로 처음부터 승인된 host 실행을 사용했다.
  이번 작업의 DNS/IPC/assertion 실패는 없었다.

기존 전체 159 files / 2614 tests와 후속 locale 8 tests 통과는
[기준 UI 검증 기록](../../frontend/src/features/crafting/feedback-tooltip/README.md)의 서로 다른 소스에 대한 이력이다.
이번 소스의 전체 통과로 표현하지 않는다. 제품 변경이 한 class의 sizing 선언뿐이고,
직접 영향 및 상호작용 83개 검사와 전체 정적 검사·build를 수행하므로 전체 Vitest는 반복하지 않는다.

## 자체 리뷰와 한계

자체 리뷰에서 기준 diff·수정 선언·기존 grid stretch/min-height/scroll 경로와 실제 검증 소스를 대조했다.
외부 영역의 크기 결정에서 목록 내용만 배제하는 최소 수정이며, 크기와 위치의 기존 계약은 보존했다.

jsdom은 실제 layout 엔진이 없다. 신규 검사는 가짜 geometry나 scrollHeight/clientHeight를 만들지 않았다.
DOM/CSSOM의 계약과 실제 항목·검색 변화는 확인했지만, 브라우저의 높이 불변·내부 실제 스크롤·픽셀 넘침·실제 장치 동작을
실측 완료로 주장하지 않는다. 브라우저 자동 QA는 사용자 보류로 미실행이다.

서버·Docker 조회/재시작·배포·DB·Redis·볼륨·실제 54개 기록·설정·원격 push는 조작하지 않았다.
Backend/Compose 검사는 변경 영역이 아니므로 미실행이다. `npm ci`는 동일 의존성 재사용에 따라 미실행이다.
worktree·node_modules·dist·cache·검증 로그를 root 배포 리뷰까지 보존한다.
로그는 이 worktree의 `frontend/node_modules/.cache/stash-scroll-fix/`에 있으며,
`impact-vitest.json`, `build.log`, `verification.json`에 결과와 최종 소스/dist 해시를 보존한다.
