# Workbench 메뉴 위치·빈자리·하단 순서 검증

- 작업일: 2026-11-04 (Asia/Seoul)
- 기준: `d529575daea6a37aa84b78c86eeeaeb8687bf1e1`
- branch: `codex/ui-menu-layout`
- worktree: `C:\Users\SSAFY\.codex\worktrees\0f5f\Exile-Hephaistos`
- 시작 상태: 기준 SHA의 detached HEAD, 미커밋 변경 없음. 전용 branch 생성 후 이 worktree만 수정했다.
- 검증 대상: 이 문서를 포함하는 commit의 frontend source. 관련 검사 후 source를 고정하고 전체 검사를 한 번 실행했다.

## 이력에서 확인한 원래 위치

`eaa04c0e`의 `CraftingPage.tsx`에서 `baseAnchor`는 `.bench-lower` 첫 자식인 `.workbench-session-entry` 안의 첫 버튼이었다. 해당 행은 margin이 없는 flex 행으로, 첫 버튼의 left/top은 `.bench-lower` 콘텐츠 시작점이었다.

동일 이력의 `LeftShelfOverlay.tsx`는 실제 버튼의 `getBoundingClientRect()`와 `currencyShelf`의 rect를 `overlayPlacement`에 전달했다. `overlayPlacement.ts`는 preferred width 520px, 버튼 왼쪽 6px 간격, viewport 8px 여백, shelf 위·아래 경계 및 최대 560px 높이를 사용했다. 두 파일은 `d529575`에서 변경되지 않았음을 diff로 확인했다.

복원은 이동된 버튼을 원위치에 다시 만들지 않고 `.bench-lower` 콘텐츠 시작점을 위치 기준으로 사용한다. viewport rect와 CSS padding/border 사이에는 `rect.width / offsetWidth` 배율을 적용하며, layout 크기가 없는 테스트 환경에서는 1을 사용한다. 기존 75% zoom과 반응형 padding을 그대로 따른다. `overlayPlacement.ts` 자체는 변경하지 않았다.

## 구현 범위와 자체 리뷰

- `CraftingPage.tsx`: 위치 기준 ref만 별도로 연결하고 실제 `baseAnchor`는 그대로 유지했다. 빈 예약 div를 제거했다. 아이템 card → `SessionNotation` → feedback 순서로 옮겼다.
- `base-picker/LeftShelfOverlay.tsx`, `WorkbenchBasePicker.tsx`: 선택적 `placementRegion`을 베이스 메뉴에만 전달한다. 포커스·Escape·밖 클릭은 실제 opener를 계속 참조한다. History/Favorites는 선택적 prop을 사용하지 않아 기존 실제 버튼 rect로 배치된다.
- `crafting.css`: 예약 높이 `calc(1lh + 34px)`와 selector를 제거하고 관련 주석을 수정했다.
- `compact-viewport/compactViewport.css`: 순서 변경을 설명하는 주석만 수정했다. feedback 96px 고정 공간은 유지했다.
- `stash/WorkbenchLayout.test.tsx`: 6개 언어의 기존 4개 control 스타일·순서를 유지하면서 예약 행이 없는지 검사한다. 위치 100%/75%, scroll/resize, 닫기/Escape/밖 클릭 및 실제 opener 포커스 복귀, History/Favorites 위치를 검사한다.
- `WorkbenchIntegration.test.tsx`, `WorkbenchNotationIntegration.test.tsx`, `WorkbenchFailureFeedback.test.tsx`: 새 순서를 검사하면서 저장 상태·활성 징조 보존, 성공/실패 feedback 정책과 tooltip 검증을 유지했다.

자체 리뷰에서 요청한 세 항목 외 control 좌표·모양·색, 화폐/아이템/창고 목업, 75% zoom, 게임 규칙·숫자·저장 형식·이미지·6개 언어 리소스에 변경이 없음을 diff로 확인했다. 전체 기보 보존·클릭 이동·분기 구현은 수정하지 않았다.

## 실행 검사

명령 작업 경로는 모두 위 worktree의 `frontend`다. Node `v24.21.0`, npm `11.19.0`을 사용했다.

| 검사                                            | 결과                                                                                                          |
| ----------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `npm ci --cache .npm-cache`                     | 제한 환경에서 DNS `ENOTFOUND`로 실패. 정상 호스트 승인 실행에서 239 packages 설치, exit 0. lockfile 변경 없음 |
| `npm run typecheck`                             | 통과, exit 0                                                                                                  |
| `npm run lint`                                  | 통과, exit 0                                                                                                  |
| `npm run format:check`                          | 통과, exit 0                                                                                                  |
| 관련 Vitest (`--run --maxWorkers=1`, 아래 대상) | 정상 호스트에서 10 files / 71 tests 통과, exit 0, 37.89s                                                      |
| `npm run test -- --run --maxWorkers=1`          | 정상 호스트에서 156 files / 2,583 tests 전부 통과, exit 0, 347.25s. 전체 실행 1회                             |
| `npm run build`                                 | 통과, exit 0. 436 modules, Vite build 1.16s                                                                   |
| `git diff --check`                              | 통과                                                                                                          |

관련 Vitest 대상:

```text
src/features/crafting/base-picker/WorkbenchBasePicker.test.tsx
src/features/crafting/stash/WorkbenchLayout.test.tsx
src/features/crafting/WorkbenchIntegration.test.tsx
src/features/crafting/WorkbenchNotationIntegration.test.tsx
src/features/crafting/WorkbenchFailureFeedback.test.tsx
src/features/crafting/session-library
src/features/crafting/session-notation
src/features/crafting/compact-viewport/compactViewport.test.ts
```

최초 제한 환경의 관련 Vitest 시도는 Vite IPC 임시 파일 `ENOENT`로 9 suites / 0 tests였으며 assertion 실패와 구분한다. 이 첫 시도의 추가 compact viewport 필터는 잘못된 경로였고, 정상 호스트 재실행에는 실제 경로를 사용해 위 10개 파일을 확인했다. 정상 호스트 재실행에서 실제 assertion 실패는 없었다. `pretest`의 6개 언어 표시 coverage도 통과했다.

설치 시 npm audit는 기존 잠금 의존성에 18건(14 moderate, 4 high)을 보고했다. 의존성 업데이트는 수행하지 않았다. Build는 500kB 초과 chunk 경고를 보고했으며 build 실패는 없었다. 산출물은 `frontend/dist`에 보존한다.

보존한 dist는 총 379 files이며, 진입 자산의 SHA-256은 다음과 같다.

```text
assets/index-CEPfp1YU.js  50DB0C8F002F6BE26717559F4E1411FCA82563C948D7452868ADDFA6DF17A798
assets/index-Cs24rpeq.css D1B60E56CBC9466F705E61F8FB4FFF51BE8AD8461DB3F2B24D1EEB10CF3F6099
```

## 미실행과 작업 경계

- 브라우저 자동 QA는 사용자 지시로 보류했다. 실제 브라우저 시각 검증 완료를 주장하지 않는다. 위치/키보드 검사는 jsdom에서 수행했다.
- Backend/Windows 실행 스크립트/Compose 검사는 해당 영역을 변경하지 않아 실행하지 않았다.
- 서버·Docker·원격·DB·Redis·volume·사용자 설정은 접근하거나 변경하지 않았다. 내부 하위 작업자를 만들지 않았다.
- 최종 검증 후 이 Worker가 만든 `.npm-cache`, `node_modules/.vite`, `node_modules/.vite-temp`만 경로·소유 범위를 확인해 정리했다. worktree와 검증된 dist는 보존했다.
- 로컬 commit은 Worker 완료 경계다. 원격 통합·push·서버 반영은 부모 작업에서 처리한다.
