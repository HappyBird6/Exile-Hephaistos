# 담당 A — 탭/즐겨찾기 배치 정정

기준: `8524b6b781651379a6cff9108525282a0691df25`.
원본 통합 작업트리가 clean임을 확인한 후 새 브랜치/작업트리에서 수정했다.
이 명세는 앞선 내부 탭 설계를 대체한다.

## 최종 구조

- 기존 `.stash-heading .material-tabs` 한 벌만 유지한다.
- 기존 `activeTab`이 우측 `WorkbenchStash`의 `#material-list`를 제어한다.
  별도의 `stashTab`, 내부 탭 UI, 두 번째 탭 선택 상태를 제거했다.
- 왼쪽 기본 화폐 창고는 고정하고, 기존 상단 탭을 바꾸면 오른쪽 콘텐츠만 바뀐다.
  숙련공의 오브와 심연 화폐는 우측 화폐 4열 목록에 포함된다.
- `WorkbenchFavorites`를 새로 분리하고 `.stash-board` 바로 다음 형제인
  `.workbench-favorites-region`에 배치했다. 창고와 아이템 정보 영역 전체 아래의
  독립 영역이며, 우측 패널/스크롤 창고의 자식이 아니다.
- 즐겨찾기는 2행 × 5칸이다. 전체 하단 영역 내부에서 최대 480px 너비로 정렬한다.
  모바일에서도 창고의 600px 최소 너비/가로 스크롤을 상속하지 않는다.
- 화폐는 4열 내부 스크롤, 갉힌 뼈 세 종류는 기존 B 정책 prop으로 제외한다.
  일반 에센스는 상위·완벽만 제공하며, 기존 특수 에센스/계열 검색은 보존한다.
- 화폐 사용 선택, 재료 들기, 카테고리 간 들기 유지, 즐겨찾기 교체, 징조 활성화,
  키보드 상단 탭 이동, 소스/레거시 안내를 유지했다.
- 뼈/에센스 ID, 저장 세션/원문/제목/메모, 카탈로그, DB/설정은 변경하지 않았다.

## 연결 계약

`WorkbenchStashProps`는 `activeTab`을 받되 `onTabChange`와 즐겨찾기 prop을 더 이상
받지 않는다. 기존 상단 탭만 상태를 변경한다. `aria-controls="material-list"`와
`aria-labelledby="tab-{activeTab}"` 연결을 유지했다.

`WorkbenchFavoritesProps`는 기존 선택/징조/held/disabled/excludedIds 상태와
favorites/onFavoriteActivate/onFavoritePlace를 받는다. 실제 페이지 연결까지
이 커밋에 포함했다. 네이티브 드래그는 `onPickUp`으로 부모가 보유한 held와
드래그 payload ID를 비교하므로 별도 전역 상태나 탭 상태가 필요 없다.

공통 `CraftingPage.tsx`는 창고/탭/즐겨찾기 영역과 해당 영역의 import/state만
수정했다. C 담당의 베이스/세션 컴포넌트·호출·카탈로그는 수정하지 않았다.
`crafting.css`에는 전체 하단 영역의 스타일만 추가했다. C의 수정과 통합할 때
베이스/세션 관련 변경을 그대로 유지하면 된다.

## 테스트 및 통합자가 적용할 기대값 패치

`stash/TEST_EXPECTATIONS.patch`는 이번 소유 범위 밖 테스트 두 곳의 수정안이다.
원본 테스트 파일에 적용하지 않았고, `git apply --ignore-space-change --check`로 현재 기준에서 적용
가능함을 확인했다. 통합 담당이 다음 명령으로 적용한다.

```powershell
git apply --ignore-space-change frontend/src/features/crafting/stash/TEST_EXPECTATIONS.patch
```

- `MaterialStash.test.tsx`: `#material-list`가 이제 우측 화폐 목록이므로 기본 21개에
  숙련공 1개와 갉힌 종류 제외 심연 화폐 8개를 합친 **30개**를 기대한다.
- `WorkbenchIntegration.test.tsx` 첫 테스트: 없어진 내부 탭을 누르는 대신 기존
  상단 탭을 누르고, 즐겨찾기는 전체 하단 영역에서 찾도록 바꾼다.
- 나머지 베이스/세션 통합 테스트와 해당 컴포넌트는 이 패치도 수정하지 않는다.

## 검증

- `npm ci --offline`로 독립 의존성을 설치했다.
- 신규/개편된 `WorkbenchStash` 9개 동작 테스트와 실제 `CraftingPage`의
  `WorkbenchLayout` 5개 테스트로 탭 한 벌, 우측 콘텐츠 연결, 전체 하단 형제 경계,
  드래그/징조, 1280/800/375px의 DOM·CSS 규약을 확인한다.
- 스타일 테스트는 Vitest CSS stub 대신 실제 CSS 파일을 읽어 적용한 뒤
  `getComputedStyle`을 확인한다. 브라우저 픽셀 배치 검증으로 표현하지 않는다.
- `TEMP`/`TMP`는 프로세스 한정 전용 검증 폴더로 지정한다.
- 서버 시작/교체, push, 브라우저 검증, 실제 운영 API/데이터 조작은 하지 않았다.
- 사용자 이미지 첨부가 필요한 변경은 없다.

## 최종 결과

- typecheck, lint, build, format:check 통과. 기존 큰 번들 경고는 빌드 실패가 아니다.
- 관련 7개 파일 **59개 중 57개 통과**, 신규/개편 창고·실제 페이지 경계 테스트는
  **14/14개 통과**했다. DesecrationCurrency/Homogenising/WorkbenchExtension도 통과했다.
- 남은 2개 실패는 위 패치가 대상으로 하는 구 UI 기대값이다. 이 테스트 파일의
  직접 변경은 이번 소유 범위 밖이므로 **미실행/통합 담당 적용 대기** 상태다.
  이 결과를 전체 테스트 성공으로 보고하지 않는다.
- 결과 보고서: 배정 작업 폴더 `stash-layout-release-tests.json`.
- 패치는 현재 CRLF 작업 파일에도 적용되도록 `--ignore-space-change`로 확인했다.
  통합자가 적용한 뒤 두 테스트 파일을 기존 Prettier로 정리하고 관련 테스트를
  다시 실행해야 한다. 원본 테스트나 실패 기대값을 임의 삭제/skip하지 않았다.
- 자체 리뷰 및 `git diff --check` 완료. 전체 2천여 개 테스트의 재실행은 하지 않았고,
  이번 화면 수정의 직접 관련 범위 및 프로젝트 정적 검증/빌드를 수행했다.
