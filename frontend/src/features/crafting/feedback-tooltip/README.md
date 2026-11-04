# 수동 Workbench 공유 설명

`FeedbackTooltipProvider`는 수동 Workbench에서만 활성화한다. 재료의 원문·번역·fallback 고지·PoE2DB와 공식 출처 링크는 `MaterialDescription`을 재사용하며, 게임 규칙이나 저장 형식은 변경하지 않는다.

- 고정 화폐, 재료 창, 즐겨찾기, 활성 징조, 기보: 기존 `data-material-tooltip`을 공통 패널로 위임한다. 선택 중에도 패널은 화폐 적용 클릭을 가리지 않으므로 설명을 계속 제공한다.
- 베이스 메뉴: `BasePreviewTooltip`의 ItemCard를 공통 패널에 표시한다. 메뉴에서 포커스가 빠져 producer가 해제되어도 패널에 있는 설명은 유지한다. Escape에서는 producer의 hover 상태도 닫아 focus 복귀로 재생성되지 않게 한다. 실제 재hover·focus·터치에서는 다시 연다.
- ItemCard의 제거 후보 설명, CurrencyImage의 누락 이미지 설명, History/Favorites 버튼: 수동 Workbench에서 `title` 대신 `data-feedback-tooltip`을 사용한다. ItemCard 제거 후보는 키보드로도 접근할 수 있다. 패널 내부 설명은 다시 producer로 해석하지 않는다.

실패 경고는 기존 `applicationFailure`의 수명과 `role=alert`를 유지한다. hover·focus·Escape는 실패 상태, 아이템, 활성 징조, film 또는 저장 보호 상태를 변경하지 않는다. 설명이 있으면 경고를 상단 최대 절반에 남기고 설명은 나머지 영역에서 따로 스크롤한다. 설명을 닫아도 경고는 그대로 남는다. 기존 새 선택·성공·base revision 변경에 따른 실패 해제 정책은 유지한다. hover 내용은 live alert로 알리지 않고 원래 trigger의 `aria-describedby`에 연결하며, 다른 기존 설명 ID는 보존한다.

trigger 또는 패널을 벗어나면 200ms 후 설명을 닫는다. 패널 hover·focus와 출처 링크 포커스는 닫힘을 보류하며, 내부 스크롤은 설명을 지우지 않는다. 패널에서 Escape를 누르면 trigger로 포커스를 돌리고 설명을 닫는다. 원래 trigger의 Escape와 메뉴의 닫기·밖 클릭은 기존 경로를 유지한다. 설명 패널 클릭은 base 메뉴의 밖 클릭으로 취급하지 않는다. touch pointerdown도 재료·베이스 설명을 연다.

이전/다음/현재 단계는 `SessionLibraryButtons`의 남은 폭에 배치하며 좁은 폭에서는 줄바꿈한다. 원래 단계 행을 회수해 피드백 높이를 96px에서 144px로 늘렸다. Base/History/Favorites는 기존 `basePlacementRegion`과 기존 위치 계산을 공유하되 실제 opener ref는 focus·닫기에 유지한다.

CraftSupport·Explorer는 이 공유 패널의 범위 밖이다. 탭 전환 시 Provider를 remount하지 않아 화면 내부 입력·focus를 보존하고, 비활성화 시 설명을 해제한다. Provider가 없거나 비활성인 MaterialTooltip·BasePreviewTooltip은 기존 body portal을 사용하고 `useFeedbackTitle`은 기존 native title을 유지한다. goal-filter·method-search의 기존 title은 해당 화면에 그대로 남는다. 브라우저의 실제 배치·터치 장치·스크린리더 확인은 자동 UI 테스트의 범위를 넘는다.

## 검증 기록 · 2026-11-04

기준 `61692dbb2c56ef06334c1245aaf08486d315ef1a`에서 B의 `7651aaedd40a6babe2b9e99cc40a7823b47934c1`을 fast-forward한 합본이다. 검증 경로는 이 격리 worktree의 `frontend`다.

- `npm ci`: 통과. 처음 제한 실행에서는 registry DNS 오류가 났고 일반 로컬 재시도에서 설치했다. lockfile·의존성 버전은 변경하지 않았다.
- 직접 영향 검사: 13 files / 136 tests 통과. 최종 탭·실제 메뉴·오류 공존 보정 후 4 files / 36 tests 통과, 이미지 누락 설명과 출처 동시 표시 보정 후 공유 패널 11 tests 통과.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`: 통과. build는 500 kB 초과 chunk 경고를 출력했다.
- 전체 Vitest는 `npm run test -- --run --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/ui-a/full-vitest.json`으로 1회 실행했다. **157 files 중 154 통과·3 실패, 2601 tests 중 2598 통과·3 실패**다. 전체 통과로 표현하지 않는다.
- 전체 실행의 실패는 `App.test.tsx`의 `shows the reduced Currency tab`, `WorkbenchIntegration.test.tsx`의 `places item controls beside favorites with History first in the lower work area`, `WorkbenchExtension.test.tsx`의 `activates the reviewed Whittling pair and updates all local candidates without hover requests`다. 앞의 두 실패 원문은 `Unable to find an accessible element with the role "checkbox" and name "Show tooltips"`, 마지막 실패 원문은 `Unable to find an element with the title: Eligible Whittling removal candidate.`였다.
- 이후 위 세 테스트 파일만 수정했다. checkbox 부재와 공유 패널 설명을 검증하고, 제거 후보 개수 검증은 원래대로 유지하면서 focus 시 공유 설명과 네트워크 요청 부재를 추가 확인했다. 해당 세 파일을 `--maxWorkers=1`로 재실행하여 **3 files / 21 tests 통과**했다. 결과는 `node_modules/.cache/ui-a/final-impact-vitest.json`이다. 부모 지시에 따라 전체 검사는 반복하지 않았다.
- 전체 실행 뒤 테스트 기대값 보정 전후의 제품 파일(`frontend/src`의 `.ts/.tsx/.css/.json`, `.test.` 제외) fingerprint는 모두 `3162B05FAA6213FE0093795EF9412B7AADD3257E193BAC01585AACD36AB50CD2`다. 경로순 정렬 후 각 상대경로와 파일 SHA256을 결합한 SHA256이며, 제품 소스 동등성을 확인했다. 테스트 보정 뒤 lint·typecheck·format을 다시 검사했다.

최초 제한 Vitest 실행은 Vite 임시 파일 ENOENT로 수집하지 못했다. 일반 로컬 검사를 사용했고 skip이나 규칙 완화는 추가하지 않았다. 전체 실행 중 기존 drag 테스트의 jsdom 좌표에서 `NaN` style 경고가 출력됐다. 사용자 지시로 브라우저 자동 QA·서버·Docker·원격 push와 Backend/Compose 검사는 실행하지 않았다. 실사용 DB·Redis·볼륨·저장 파일은 접근·변경하지 않았다. 설치용 자기 cache만 정리하고 worktree·node_modules·dist는 리뷰를 위해 보존했다.
