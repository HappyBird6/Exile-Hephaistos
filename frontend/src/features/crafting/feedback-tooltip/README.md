# 수동 Workbench 설명과 배치

현재 작업 기준은 `1def96cd9dbc26fadb9e9819409b888e4855132a`다. 수동 Workbench의 게임 규칙·저장 형식·활성 징조·요청 의미는 변경하지 않는다. 이전 commit의 검증 결과는 아래에 별도 보존한다.

## 4행 재료 보관함과 피드백 · 2026-11-05

전체 zoom 0.75, 카드 열 374px와 1000px 반응형 전환을 유지한다. 상단 화폐·아이템은 원래 935×550 좌표를 사용하며 메뉴의 `currencyShelf` ref도 이 평면에 연결한다.

왼쪽 canvas의 열 비율은 `29 / 434 / 23 / 140 / 23 / 257 / 29`다. 즐겨찾기와 3개 컨트롤은 grid 3행의 2·4열을 공유한다. 6열의 `.workbench-stash-column`은 1–3행을 가로지르며 크기 격리로 catalog 내용이 canvas를 늘리지 않게 한다. 상단 margin은 원래 y40을 유지한다.

재료 보관함은 검색·탭 아래 아이템을 최대 4행 높이로 표시한다. 이후 행은 같은 resources 영역에서 스크롤하며 Essence의 별도 특수 재료도 유지한다. 행 높이는 보관함 내부 폭에 따른 container query 단위를 사용한다. 피드백은 보관함 아래 남은 공간을 채운다. 실패 경고는 설명이 있을 때 최대 절반과 독립 스크롤을 유지한다. 이전 오른쪽 200px 고정 피드백 영역은 사용하지 않는다.

오른쪽은 카드 아래 History가 남은 높이를 채운다. History 항목은 내용 높이로 배치하며 긴 기록은 내부 스크롤한다. canvas 하단 24px는 오른쪽 padding과 맞춘다. 좁은 화면에서는 두 열이 세로로 쌓이고, 컨트롤의 최소 208px를 공유 하단 행이 수용한다.

카오스 반복 토글과 정규식 입력 높이는 기존보다 20% 크다. 버튼에는 `1000 x`와 선택한 Chaos 이미지만 표시하며 접근성 이름과 실행 상태는 유지한다. 입력 오른쪽은 상단 Perfect Chaos 슬롯 오른쪽(x283)에 맞추고 화폐 아래 간격을 줄인다.

## 공유 설명의 수명과 우선순위

고정 화폐·재료·즐겨찾기·징조·기보, ItemCard 제거 후보, 이미지 누락, History/Favorites 보조 설명은 공유 feedback에 표시한다. hover/focus가 끝나거나 일반 영역으로 이동해도 마지막 유효 설명을 유지한다. 새 유효 대상의 hover/focus/터치 또는 클릭이 설명을 대체하며, 패널 내부 링크와 스크롤은 다시 producer로 해석하지 않는다. checkbox는 복구하지 않는다.

`CraftingPage`의 실제 `held ?? selected`가 있는 동안 해당 재료 설명을 우선 고정한다. DOM hover로 들고 있는 상태를 추정하지 않는다. 고정 중에는 같은 재료의 다른 슬롯을 포함해 일반 hover가 내용·anchor를 바꾸지 못한다. 클릭·contextmenu·dragstart에서 실제 trigger를 기록하며, 해당 버튼이 연결되고 보이는 동안 `aria-describedby`와 패널 Escape 후 focus 복귀에 사용한다. trigger가 탭 변경으로 사라졌으면 같은 재료의 연결된 visible·enabled 버튼으로만 대체하고, 없으면 설명을 표시하되 숨겨진 버튼으로 focus를 보내지 않는다.

놓기·해제·취소로 우선 재료가 없어지면 고정 설명을 지우고 이후 정상 hover 교체를 허용한다. workspace·locale, base revision, 활성 film·cursor, 재료 탭, 활성 징조, 아이템 입력 열기/닫기가 바뀌면 일반 설명을 초기화한다. 실제로 계속 들고 있는 재료가 있으면 새 context에서 그 재료 설명을 다시 고정한다. 성공 후 선택 해제·History/세션 복원 등은 Page의 기존 상태 전이를 따른다. hover는 실패 경고·아이템·활성 징조·저장 데이터를 변경하지 않는다. 실패 경고는 `applicationFailure`의 기존 수명과 `role=alert`로 설명과 공존한다.

공유 패널 Escape는 설명을 해제하고, 필요한 경우 실제 source로 focus를 복귀한다. 이벤트 전파를 막지 않아 Page의 기존 selected/held 취소와 실패 해제도 실행된다. Provider는 탭 전환 때 remount하지 않는다. CraftSupport·Explorer 및 Provider가 없는 독립 사용은 기존 body portal/native title 경로를 유지한다.

## 독립 베이스 미리보기

`BasePreviewTooltip`은 항상 기존 fixed body portal, 좌/우 공간 검사, viewport clamp 계산과 ItemCard 표현을 사용한다. 공유 feedback에는 베이스 ItemCard를 중복 등록하지 않는다. 이전 일반 설명이나 집은 재료 설명은 베이스 hover가 시작됐다는 이유로 지우지 않는다.

베이스 옵션의 hover/focus/touch가 floating preview를 연다. 일반 hover leave는 닫고, preview 자체로 pointer/focus를 옮기면 유지해 긴 내용을 스크롤할 수 있다. 실제 floating node를 `aria-describedby`가 참조한다. preview의 Escape는 source에 focus를 돌린 뒤 hover 상태를 닫아 다시 열리지 않게 한다. 옵션의 첫 Escape는 미리보기, 다음 Escape는 메뉴를 닫는 기존 경로다. 메뉴의 실제 opener·닫기·밖 클릭 복귀는 유지한다. 존재하지 않던 베이스 외부 URL은 만들지 않았다.

## 재료 header와 요청 색상

기존 PoE2DB URL을 재료 이름과 같은 `.material-description__header`의 오른쪽 끝에 배치한다. 이름은 줄바꿈·단어 분리를 허용하며 링크는 줄을 옮길 수 있고 오른쪽 정렬을 유지한다. 번역/fallback 고지·원문 lang·공식 legacy 링크·6 locale은 유지한다.

이름은 정확히 `#aa9e82`, 설명은 `#9999ff`이며 `.material-description` 전용 class로 한정한다. 일반 ItemCard·실패 alert 색은 바꾸지 않는다. 상대 휘도 `(L밝음+0.05)/(L어두움+0.05)`로 계산한 대비는 다음과 같다.

| 배경                                                | 이름        | 설명        |
| --------------------------------------------------- | ----------- | ----------- |
| 오른쪽 gradient 밝은 끝 #252a23                     | 5.52:1      | 5.83:1      |
| 오른쪽 gradient 어두운 끝 #151b19                   | 6.59:1      | 6.95:1      |
| stash-board #191d1b                                 | 6.43:1      | 6.78:1      |
| 독립 재료 tooltip #100f0ff5를 위 gradient 위에 합성 | 7.17–7.20:1 | 7.56–7.59:1 |

합성 수치는 기존 CSS 색의 계산값이며 브라우저 pixel 측정은 아니다. 출처 링크 focus는 기존 밝은 outline, preview focus는 전역 `:focus-visible`을 사용한다. 자동 브라우저 QA·실제 장치·스크린리더 확인은 사용자 보류 범위다.

## UI 배치 검증 · 2026-11-05

- `npm ci`, lint, typecheck, format 검사와 Frontend build를 통과했다. 레이아웃·피드백 직접 영향 5 files / 45 tests도 통과했다.
- 전체 검사는 170 files / 2993 tests 중 167 files / 2983 tests 통과, 10 tests 실패였다. 1개는 Page 배치 변경에 따른 source hash 차이로, golden 출력은 수정하지 않고 Page hash만 갱신했다. 이후 표시 동등성 1 file / 118 tests와 format 검사를 통과했다. 전체 검사를 다시 실행한 결과로 합산하지 않는다.
- 남은 입력 취소 포커스 8개와 Sapphire 다섯 번째 옵션 검증 1개는 기준 HEAD `9751cd7`의 `CraftingPage`로 교체한 비교 검사에서도 동일하게 재현됐다(2 files / 32 tests 중 9 실패). 현재 UI 소스는 비교 후 원래대로 복구했다. 이번 범위에서는 해당 로직과 기대값을 수정하지 않았다.
- 실제 브라우저에서 320px의 6개 언어와 390/600/1000/1001/1440px를 확인했다. 4행 제한·내부 스크롤, 입력 오른쪽 정렬, 재료 설명의 Escape focus 복귀, 모바일 즐겨찾기 겹침 및 가로 넘침을 검사했다.
- Frontend Docker 이미지를 빌드해 localhost:9001에 반영하고 데스크톱·모바일 화면을 확인했다. Backend·PostgreSQL·Redis와 기존 볼륨은 유지했다. Backend 코드는 변경하지 않아 Backend 검사는 실행하지 않았다.
- 기존 큰 bundle 경고와 jsdom drag의 `NaN` style 경고가 남는다.

## 이번 UI 조정 검증 기록 · 2026-11-04

- `1def96cd`와 동일한 package/lockfile 및 이전 `npm ci` 성공의 node_modules를 재사용했다. 의존성·설정은 수정하지 않았다.
- 초기 영향 검사 8 files / 76 tests에서는 74 통과·2 실패였다. 변경된 집기 우선순위 기대값과 CSS 열 비율 파서 문제를 보정했다. 이어 9 files / 84 tests에서는 83 통과·1 파서 실패였으며, 중복 CSS selector를 정확히 읽도록 고친 단독 배치 검사 3 tests가 통과했다. 중간 lint의 가변 상태와 테스트 DOM 타입·CSS format 지적도 수정했다. 실패를 skip·기대값 완화로 숨기지 않았다.
- 고정 소스에서 전체 `npm run test -- --run --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/ui-a/height-preview-full-vitest.json` **159 files / 2614 tests 모두 통과**(394.00s)했다. 전체 실행의 제품 fingerprint는 `57B4E705CFCFF58CD2607A4172C7F485C77D874E630F0121B339460386ED4DF7`이다.
- 전체 실행 뒤 읽기 리뷰에서 확인한 locale 수명만 최소 보정했다. 제품 차이는 Page `contextKey`에 `locale` 한 항목 추가이며, 일반 UI 문자열은 해제하고 실제 집은 재료는 새 언어로 유지한다. 이를 포함한 `FeedbackPersistence.test.tsx` 최종 **1 file / 8 tests 통과**. 결과는 `node_modules/.cache/ui-a/height-preview-locale-final.json`이다. 부모 지시에 따라 전체 검사를 반복하지 않았다. 따라서 전체 2614 통과와 최종 locale 영향 8 통과는 서로 다른 소스의 결과로 구분한다.
- locale 보정 뒤 `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`를 확인했다. 최종 제품 fingerprint는 `D52EF90E2E635AC5495F4BCA1083DA926D4D257E8D9F421855B796626138F51F`이며 알고리즘은 아래 이전 기록과 같다. 최종 build의 dist 파일별 해시는 `node_modules/.cache/ui-a/height-preview-dist-sha256.json`에 보존한다.
- build의 500 kB 초과 chunk 경고와 기존 drag 테스트의 jsdom `NaN` style 경고는 남는다. 브라우저 자동 QA는 사용자 보류이며 실제 layout·터치 장치·스크린리더 확인을 자동 테스트 통과로 대체하지 않는다. 서버·Docker·원격 push·Backend/Compose 검사는 수행하지 않았고, DB·Redis·볼륨·설정·54개 실제 저장 기록은 변경하지 않았다. worktree·node_modules·dist와 검증 로그를 부모 리뷰까지 보존한다. 카오스 기능은 이 변경에 포함하지 않는다.

## 이전 commit 1def96cd 검증 기록 · 2026-11-04

기준 `61692dbb2c56ef06334c1245aaf08486d315ef1a`에서 B의 `7651aaedd40a6babe2b9e99cc40a7823b47934c1`을 fast-forward한 합본이다. 검증 경로는 이 격리 worktree의 `frontend`다.

- `npm ci`: 통과. 처음 제한 실행에서는 registry DNS 오류가 났고 일반 로컬 재시도에서 설치했다. lockfile·의존성 버전은 변경하지 않았다.
- 직접 영향 검사: 13 files / 136 tests 통과. 최종 탭·실제 메뉴·오류 공존 보정 후 4 files / 36 tests 통과, 이미지 누락 설명과 출처 동시 표시 보정 후 공유 패널 11 tests 통과.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`: 통과. build는 500 kB 초과 chunk 경고를 출력했다.
- 전체 Vitest는 `npm run test -- --run --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/ui-a/full-vitest.json`으로 1회 실행했다. **157 files 중 154 통과·3 실패, 2601 tests 중 2598 통과·3 실패**다. 전체 통과로 표현하지 않는다.
- 전체 실행의 실패는 `App.test.tsx`의 `shows the reduced Currency tab`, `WorkbenchIntegration.test.tsx`의 `places item controls beside favorites with History first in the lower work area`, `WorkbenchExtension.test.tsx`의 `activates the reviewed Whittling pair and updates all local candidates without hover requests`다. 앞의 두 실패 원문은 `Unable to find an accessible element with the role "checkbox" and name "Show tooltips"`, 마지막 실패 원문은 `Unable to find an element with the title: Eligible Whittling removal candidate.`였다.
- 이후 위 세 테스트 파일만 수정했다. checkbox 부재와 공유 패널 설명을 검증하고, 제거 후보 개수 검증은 원래대로 유지하면서 focus 시 공유 설명과 네트워크 요청 부재를 추가 확인했다. 해당 세 파일을 `--maxWorkers=1`로 재실행하여 **3 files / 21 tests 통과**했다. 결과는 `node_modules/.cache/ui-a/final-impact-vitest.json`이다. 부모 지시에 따라 전체 검사는 반복하지 않았다.
- 전체 실행 뒤 테스트 기대값 보정 전후의 제품 파일(`frontend/src`의 `.ts/.tsx/.css/.json`, `.test.` 제외) fingerprint는 모두 `3162B05FAA6213FE0093795EF9412B7AADD3257E193BAC01585AACD36AB50CD2`다. 경로순 정렬 후 각 상대경로와 파일 SHA256을 결합한 SHA256이며, 제품 소스 동등성을 확인했다. 테스트 보정 뒤 lint·typecheck·format을 다시 검사했다.

최초 제한 Vitest 실행은 Vite 임시 파일 ENOENT로 수집하지 못했다. 일반 로컬 검사를 사용했고 skip이나 규칙 완화는 추가하지 않았다. 전체 실행 중 기존 drag 테스트의 jsdom 좌표에서 `NaN` style 경고가 출력됐다. 사용자 지시로 브라우저 자동 QA·서버·Docker·원격 push와 Backend/Compose 검사는 실행하지 않았다. 실사용 DB·Redis·볼륨·저장 파일은 접근·변경하지 않았다. 설치용 자기 cache만 정리하고 worktree·node_modules·dist는 리뷰를 위해 보존했다.
