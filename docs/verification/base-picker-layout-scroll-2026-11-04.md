# 베이스 선택 메뉴 레이아웃·공통 스크롤 검증

2026-11-04 · 기준 `85d1353ee52aa77652397aa2a1149ca15aad80d8`.
이 문서와 함께 commit되는 frontend 최종 소스를 대상으로 한 Worker 자체 검증 기록이다.
전체 검사 시작 이후 frontend 소스·테스트·설정·잠금 파일을 변경하지 않았다.

## 변경 파일

- `frontend/src/features/crafting/base-picker/WorkbenchBasePicker.tsx`
- `frontend/src/features/crafting/base-picker/WorkbenchBasePicker.test.tsx`
- `frontend/src/features/crafting/base-picker/base-picker.css`
- `frontend/src/features/crafting/base-picker/LeftShelfOverlay.tsx`
- `frontend/src/features/crafting/base-picker/left-shelf-overlay.css`
- `frontend/src/features/crafting/stash/WorkbenchStash.tsx`
- `frontend/src/features/crafting/stash/WorkbenchLayout.test.tsx`
- `frontend/src/features/crafting/stash/workbenchStash.css`
- `frontend/src/features/crafting/workbench-scrollbar.css`
- 이 검증 문서.

## 구현 범위

- 베이스 메뉴 왼쪽 열: 카테고리·소분류 필터, 검색, 아이템 레벨, 기존 `extraControls`, Place 버튼, 레벨 오류.
- 오른쪽 열: 기존 베이스 옵션을 한 열로 표시하고 스크롤한다. 데이터·필터·검색·선택·게임 규칙은 그대로다.
- 필터의 보이는 라벨을 제거하고 기존 6개 locale의 이름을 `aria-label`로 보존한다.
- `.wb-scrollbar` 한 정의에 기존 stash의 `stable`, `thin`, `#867347 #141a15`를 옮겼다.
  stash resources, 베이스 옵션·왼쪽 컨트롤, History/Favorites의 실제 overlay body에서 재사용한다.
- 베이스 overlay body는 바깥 스크롤을 끄고 두 열의 필요한 스크롤만 사용한다.
  `min-height: 0`으로 높이 제한 안에서 축소하고 왼쪽 필드·버튼은 축소하지 않아 스크롤로 접근한다.
  실제 본문 너비가 340px 이하이면 두 영역을 위아래 한 열로 전환한다.
- 레벨 입력의 기존 76px 스타일은 전용 `.wb-base-picker__level`로 한정하여,
  footer에 들어온 주얼 suffix 체크박스에 숫자 입력 스타일이 적용되지 않는다.
- 메뉴 위치 계산·placementRegion·75% zoom 계산·tooltip portal·키보드 handler는 변경하지 않았다.
  History/Favorites 데이터 처리·기보·피드백 순서·색상·테두리도 변경하지 않았다.

## 명령과 결과

작업 경로: `C:\Users\SSAFY\.codex\worktrees\6d82\Exile-Hephaistos\frontend`.
Node `v24.21.0`, npm `11.19.0`. 모든 최종 통과 명령의 종료 코드는 0이다.

| 명령                                             | 결과                                                                                                                        |
| ------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------- |
| `npm ci --cache .npm-cache --no-audit --no-fund` | 제한 환경에서 registry DNS `ENOTFOUND`로 실패하고 cleanup `EPERM` 경고가 있었다. 승인된 host 실행은 239 packages 설치 성공. |
| 아래 관련 검사, 제한 환경                        | 9 suites 로딩 실패, 테스트 실행 0개. IPC 임시 파일 `ENOENT`. 실제 assertion 실패와 구분한다.                                |
| 아래 관련 검사, host 최종                        | 14 files / 100 tests 통과.                                                                                                  |
| `npm run lint`                                   | 통과.                                                                                                                       |
| `npm run typecheck`                              | 통과.                                                                                                                       |
| `npm run format:check`                           | 통과.                                                                                                                       |
| `npm run build`                                  | host 통과. chunk 500KB 초과 경고 있음.                                                                                      |
| `npm run test -- --run --maxWorkers=1`           | host 전체 1회 실행, 156 files / 2,591 tests 통과. 354.61초.                                                                 |
| `git diff --check`                               | 통과.                                                                                                                       |

최종 관련 검사:

```text
npm run test -- --run src/features/crafting/base-picker src/features/crafting/stash src/features/crafting/session-library src/features/crafting/Workbench.test.tsx src/features/crafting/WorkbenchIntegration.test.tsx src/features/crafting/WorkbenchExtension.test.tsx src/features/crafting/WorkbenchNotationIntegration.test.tsx src/features/crafting/WorkbenchFailureFeedback.test.tsx --maxWorkers=1
```

그 전 host 관련 검사에서 신규 검사의 문제로 실제 assertion 실패가 있었다.
첫 실행은 59 통과·2 실패(색상 직렬화, 검색 대상 중복), 다음은 98 통과·2 실패(0 단위 표기, Sapphire Ring/Jewel 이름 중복),
다음은 99 통과·1 실패(overlay `overflow` shorthand와 `overflow-y` 구분)였다.
대상 메뉴와 정확한 아이템 이름을 지정하고 CSSOM 원래 선언과 계산값을 구분해 수정했다.
기존 테스트를 삭제·skip하거나 기존 데이터·선택·키보드 기대값을 완화하지 않았다.

## 확인 근거와 한계

- 신규 6 locale 접근성 검사와 실제 CraftingPage DOM 검사로 왼쪽 필드 위치, 실제 스크롤 요소의 공통 스타일,
  비스크롤 베이스 외곽 body, 한 열 옵션, 좁은 본문의 container rule을 확인했다.
- 주얼 rarity·관측 suffix·roll 유효성, 레벨 `빈 값/0/1.5/101` 거부와 `1/100` 허용,
  옵션 focus tooltip·첫 Escape의 tooltip 해제·메뉴 유지·기존 선택 상태를 확인했다.
- 기존 통합 검사로 Place, 검색·필터, 메뉴 열기·닫기·바깥 클릭, focus 복귀, resize/scroll 및 75% zoom 위치를 확인했다.
- 빌드 CSS에서 `.wb-scrollbar` 및 해당 색상 선언이 각각 한 번 포함됨을 확인했다.
- 브라우저 자동 QA는 사용자 보류로 실행하지 않았다. jsdom에는 실제 배치 엔진이 없으므로
  작은 화면·짧은 높이에서의 픽셀 단위 겹침·수평 넘침·네이티브 focus 스크롤을 실브라우저 검증 완료로 표현하지 않는다.
- Backend, Windows 실행 스크립트, Compose 검사는 변경 영역이 아니므로 미실행.
  서버·Docker·DB·Redis·저장 데이터·원격 push는 조작하지 않았다.
- 생성물은 이 worktree의 `frontend/node_modules`, `frontend/dist`, `frontend/.npm-cache`,
  `frontend/node_modules/.vite`, `frontend/node_modules/.vite-temp`이다.
  검사 완료 후 각 절대 경로가 이 Worker의 frontend 아래인지 확인하고 cache 세 경로만 삭제했다.
  `node_modules`와 `dist`는 리뷰용으로 보존했다. 공유 cache·타인 자료는 정리하지 않았다.
