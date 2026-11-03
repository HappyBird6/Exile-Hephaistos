> Superseded layout: the original proposal below rearranged currencies and is retained as history. The design restoration uses the original catalog coordinates, CSS zoom 0.75 on the workbench, original 1000px stacking breakpoint, and a lower extension inside the same canvas for favorites. See docs/evidence/workbench-design-restoration-20261103.md. Do not apply the historical COMMON patches again.

# 화면·캔버스 배치 A — 요구 1·2·3·5·6·10

기준: `e82e246de18df1688e0eaba8a858b57a3c5b60ff`.
branch: `codex/compact-viewport-a-20261103`.
전용 worktree: `C:/Users/SSAFY/.codex/worktrees/8b6b/Exile-Hephaistos`.
시작 시 detached HEAD, 기준 SHA 일치, 미커밋 변경 없음을 확인했다.

## 소유 범위와 공통 연결

소유 소스는 `stash/`의 CSS·타입·즐겨찾기 주석·테스트,
`compact-viewport/`, `item-card.css`다. 공통 `CraftingPage.tsx`와
`crafting.css`는 원본을 수정하지 않았다. 이 문서 및 patch는 이번 명시적
통합 계약 요청에 따라 작성한다. 기존 stash/INTEGRATION.md와 과거 patch는
이전 작업 이력으로 보존하며 **이번에는 적용하지 않는다**.

1. `COMMON_PAGE.patch`: CSS/좌표 helper import, 화폐 좌표 재배치,
   Favorites를 `.stash-canvas__content` 직접 자식으로 이동,
   `.workbench-favorites-region` 및 `stash-canvas--with-favorites` 제거,
   실패 feedback을 `.bench-lower`의 `.bench-item-card` 다음 직접 자식으로 이동.
2. `COMMON_CSS.patch`: 기존 600px 강제 canvas minimum 및 사용하지 않는
   `.workbench-favorites-region` CSS 제거. 새 override는 아래 import로 제공한다.

```tsx
import { compactCurrencyPositions } from './compact-viewport/coordinates'
import './crafting.css'
import './compact-viewport/compactViewport.css'

// fixedCurrencies.map 내부. 데이터 catalog 좌표는 변경하지 않는다.
const position = compactCurrencyPositions[currency.id] ?? currency
// 기존 button style의 left/top만 교체한다.
style={{
  left: `${position.x / 9.35}%`,
  top: `${position.y / 5.5}%`,
}}
```

두 patch는 기준 페이지에서 적용 가능하다. root가 이미 Favorites/feedback을
옮겼다면 patch를 중복 적용하지 말고 CSS import와 좌표 연결만 합친다.
root가 compactViewport.css 내용을 crafting.css 맨 끝에 직접 합치는 것도
동등하지만, import와 복사를 동시에 하지는 않는다. 테스트는 전용 CSS 파일을
읽으므로 파일 자체는 보존한다. 공통 CSS 기존 최소 폭 삭제를 먼저 적용한다.

```powershell
git apply --check frontend/src/features/crafting/compact-viewport/COMMON_PAGE.patch
git apply frontend/src/features/crafting/compact-viewport/COMMON_PAGE.patch
git apply --check frontend/src/features/crafting/compact-viewport/COMMON_CSS.patch
git apply frontend/src/features/crafting/compact-viewport/COMMON_CSS.patch
```

## 75% 폭과 카드 10% 확대의 기준

기존 마지막 `.craft-page` 규칙의 max-width는 1800px다. 새 상한은
1350px이며 정확히 75%다. 일반 데스크톱 폭은 `75vw`, 1280px 부근에서는
1000px 하한을 두어 조작 영역을 지나치게 좁히지 않는다. viewport가
1000px보다 작으면 width 100%와 기존 24px/12px padding을 사용한다.
광고, 광고 placeholder, 추가 side panel은 만들지 않았다.

| viewport | 기존 외곽 폭 | 새 외곽 폭 | 새 내부 폭(기존 padding 적용) |
| -------- | ------------ | ---------- | ----------------------------- |
| 1920px   | 1800px       | 1350px     | 1254px                        |
| 1440px   | 1440px       | 1080px     | 984px                         |
| 1280px   | 1280px       | 1000px     | 904px                         |
| 375px    | 375px        | 375px      | 351px                         |

카드 열은 기존 340px에서 374px로 10% 확대한다. item-card 본문은
13→14.3px, header 17→18.7px, 상세/affix 11→12.1px다.
stash 이름 9→12px, 검색 11→12px, 탭 10→12px도 보정했다.
카드의 tier table은 긴 원문을 줄바꿈하여 수평 overflow를 막는다.
숫자·원문·언어별 문자열·자산·출처·저장값은 변경하지 않는다.

## 935×550 plane 안의 좌표 계약 — 반드시 함께 연결

원래 화폐 좌표를 그대로 유지하면 5열×2행 즐겨찾기를 놓을 빈 직사각형이
없다. 같은 74×74 칸과 90/100 간격으로 10칸을 놓으려면 434×174 영역이
필요하다. 따라서 **좌표 helper를 연결하지 않고 새 Favorites CSS만 사용하면
기존 아래쪽 화폐와 겹친다**.

전체 비율과 오른쪽 stash x=652 시작점은 보존하며, 표시 좌표만 다음처럼
재배치한다. 게임 효과와 catalog 원본의 x/y는 바꾸지 않는다.

| 영역               | x        | y          | 폭×높이     | 간격                             |
| ------------------ | -------- | ---------- | ----------- | -------------------------------- |
| 고정 Currency 21개 | 29+90×열 | 40+100×행  | 각 74×74    | 7열×3행                          |
| Favorites 10개     | 29+90×열 | 340+100×행 | 각 74×74    | 5열×2행                          |
| item-placement     | 489      | 340        | 154×174     | 하단 edit button 포함 plane 내부 |
| 오른쪽 stash       | 652      | 12px inset | 가로 끝 923 | 기존 기준 유지                   |

`compactCurrencyRows`는 기존 21개 ID를 한 번씩 포함하고 normal/Greater/Perfect
세 칸을 같은 행의 연속 열로 유지한다. root는 기존 `fixedCurrencies` 필터를
그대로 사용한다. 새 화폐 추가나 목록 변경은 없다. CSS slot 크기는 실제
container 폭에 비례하며 Favorites/Currency 모두 같은 border-box 정사각형이다.
예: canvas 880px이면 두 종류 모두 약 69.65px다.

필수 DOM은 다음과 같다. WorkbenchFavorites props 계약은 그대로다.

```tsx
<div className="stash-viewport">
  <div className="stash-canvas">
    <div className="stash-canvas__content" ref={currencyShelf}>
      <div className="currency-catalog">{/* 기존 버튼 + 새 표시 좌표 */}</div>
      <div className="item-placement">{/* 기존 버튼과 입력 opener */}</div>
      <WorkbenchStash {...existingStashProps} />
      <WorkbenchFavorites {...existingFavoriteProps} />
    </div>
  </div>
</div>
```

## 반응형·overlay·lower 계약

- `.workbench-layout`을 named inline-size container `workbench`로 둔다.
  내부 폭 1114px 이하에서는 canvas/card를 세로로 놓는다. 이는 최소 canvas
  740px + 확대 카드 374px 기준이다. 카드 자체는 최대 374px를 유지한다.
- `.stash-viewport`은 named inline-size container `stash`다. 폭 739px
  이하에서는 고정 좌표를 해제하고 Currency/item/stash/Favorites를 자연 높이
  grid로 배치한다. Currency와 Favorites는 똑같은 5열/8px gap을 사용한다.
  Favorites는 항상 2행이며 각 칸은 Currency와 같은 정사각형이다.
  CSS order를 사용하지 않아 시각 순서와 기존 DOM의 Tab focus 순서가 일치한다.
  오른쪽 목록만 기존 overflow-y auto를 사용하고 stash 최대 높이는 480px다.
- `.currency-slot`의 left/top은 공통 페이지 inline style이므로 좁은 container
  안에서만 `auto !important`로 해제한다. 클릭·drag ID·즐겨찾기 순서·기존
  저장 리소스·6개 언어·tablist 키보드 handler는 유지한다.
- transform/scale/zoom을 추가하지 않았다. `clientX/clientY`,
  `getBoundingClientRect`, MaterialTooltip document 위임 및 cursor fixed
  좌표를 그대로 사용할 수 있다. `currencyShelf` ref는 content에 유지한다.
  LeftShelfOverlay가 읽는 shelf rect도 실제 reflow된 content 크기다.
- canvas에 추가 favorite row나 spacer 높이를 만들지 않는다. 작은 화면의
  세로 높이는 실제 grid 내용이 차지한다. 기존 stashSpace(최대 24px) 로직은
  변경하지 않았다. 강제 min-width 600px는 공통 CSS patch에서 제거한다.
- `.bench-lower`는 단일 `minmax(0, 1fr)` grid, auto height, gap 12px다.
  **모든 직접 자식**은 `grid-column: 1 / -1`, `min-width: 0`, `max-width: 100%`다.
  root가 `.bench-item-card` → 실패 feedback → C SessionNotation 순으로
  직접 자식을 붙이면, 기보의 class/props와 무관하게 전체 열을 사용한다.
  실패 feedback의 기존 84px 고정 높이와 내부 scroll은 제거한다.
  C는 기보 최상단에 고정 min-width를 설정하지 말고 긴 행을 wrap하거나
  자체 영역에서만 scroll하도록 한다. lower에 전체 고정 높이를 주지 않는다.

## 검증 및 자체 리뷰

자체 검증 복사본은 같은 worktree의
`.verification-compact/`이며 별도 Git checkout/worktree가 아니다.
공통 파일 patch와 필요한 contract/backend 리소스의 복사본만 사용한다.
이 폴더는 commit 대상이 아니다.

브라우저·서버·push·Docker·DB·volume 조작은 하지 않았다. 브라우저 픽셀 QA는
이번 위임에서 금지되어 미실행이다. jsdom 및 좌표 산술 검사를 실브라우저
시각 검증으로 표현하지 않는다. Backend·Compose·Windows 실행 스크립트는
변경하지 않았으며 관련 검증은 이번 소유 범위에 해당하지 않는다.
전체 frontend suite는 root의 최종 통합에서 한 번 실행한다.

- `npm ci --offline --no-audit --no-fund`: 호스트 기존 cache 사용 시 통과,
  239개 설치. sandbox cache에는 zustand tarball이 없어 첫 시도는 설치 불가였다.
  package.json/lockfile 변경 없음.
- 원본 소유 branch: `npm run lint`, `npm run typecheck`,
  `npm run format:check`, `npm run build` 모두 통과.
  이후 추가한 이 문서와 최종 CSS도 Prettier 개별 check 통과.
- 원본 소유 branch:
  `npm run test -- --run src/features/crafting/compact-viewport/compactViewport.test.ts src/features/crafting/stash/WorkbenchStash.test.tsx --maxWorkers=1`
  **15/15 통과**. 6언어 display coverage pretest도 통과.
  sandbox에서는 IPC ENOENT로 0개가 실행되어 호스트 단일 worker로 실행했다.
- 공통 patch를 적용한 복사본:
  `npx vitest --run src/features/crafting/stash/WorkbenchLayout.test.tsx src/features/crafting/compact-viewport/compactViewport.test.ts --maxWorkers=1`
  **6/6 통과**. 좌표 테스트 3개가 위 15개와 중복되므로 총 고유 18개다.
  초기 복사본에는 저장소 외부 import contract/backend fixture가 없어
  suite loading이 실패했다. 필요한 리소스를 복사한 최종 실행은 모두 통과했으며
  기대값이나 테스트 범위를 완화하지 않았다.
- 같은 복사본의 `npm run build`: 통과. 기존 대형 번들 경고만 있다.
  공통 페이지와 새 CSS import/좌표 연결을 포함한 build다.
- 공통 patch의 CraftingPage.tsx 개별 ESLint 검사도 통과.
- 원본의 두 patch 모두 `git apply --check` 통과.
  `git diff --check` 통과.
- 자체 리뷰: 다른 소유 파일 미수정, 21개 Currency ID/5개 tier family 보존,
  모든 74×74 칸/90×100 간격/plane 내부 무충돌, Favorites 2×5 DOM,
  카테고리·키보드·drag·Omen·기존 저장값 보존, 긴 카드 행 줄바꿈,
  feedback/notation 높이 제한 해제, fixed tooltip/cursor 좌표를 확인했다.
- root 필수 통합: 이번 좌표 helper 및 CSS 연결, C SessionNotation 배치,
  전체 frontend 검사와 실제 브라우저의 desktop/mobile·6언어 시각 확인.
  이 branch만 cherry-pick하면 공통 페이지 연결 전 상태다. 특히 새 좌표 helper를
  생략하면 Favorites와 기존 화폐가 겹치므로 CSS만 따로 반영하지 않는다.
