# D: 카드 표시와 History/Favorite 연결

기준 SHA: `4fb6b89d0f2319444742aac336f9b1a1832b515c`.
구현 branch: `codex/item-history-25-corrections-d-local-20261103`.
작업 경로: `C:/Users/SSAFY/.codex/worktrees/0ebd/Exile-Hephaistos`.

최신 환경 정정에 따라 현재 `.codex/worktrees`에서 구현했다. 최초 지시로 먼저 생성한 `task-6/item-history-25-corrections-d` 및 `codex/item-history-25-corrections-d-20261103`은 기준 SHA 상태로 보존했으며 결과 branch가 아니다. 다른 작업의 파일과 기록을 이동·삭제하지 않았다. 전달 요청의 gpt-6.1-sol high 지정은 확인했지만 실행 모델/effort/tier를 조회·변경하는 도구는 없어 실제 설정은 미확인이다. 사용자 설정은 변경하지 않았다.

## 제공 코드와 연결 패치

공통 소유 소스 `CraftingPage.tsx`, `crafting.css`, `app/App.tsx`, 전역 i18n, 공통 resource seal/manifest/registry는 직접 수정하지 않았다. `COMMON.patch`는 기준 SHA에서 `git apply --check`를 통과하는 정확한 unified diff다. 각 변경 대상의 완성 내용을 메모리에서 Prettier로 구문 분석·정렬한 후 diff를 생성했다. 통합자의 다른 page 변경과 같은 위치가 겹칠 수 있으므로 요구별로 병합하고, 통합 후 page DOM과 전체 검사를 수행한다.

| 요청  | 제공 결과                                                                                                                                                                                                   |
| ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 11    | `COMMON.patch`: 제목 오른쪽 `preview-badge` DOM 제거                                                                                                                                                        |
| 14    | `COMMON.patch`: 표시 전용 `maximum-quality` property와 그 전용 계산/import 제거. `qualityLimit.ts`, 실제 cap 규칙, 실행 가드·응답 검증은 유지                                                               |
| 15    | `item-slot/ItemSlotImage.tsx`: 기존 `BaseImage`와 exact Metadata base ID resolver 재사용. 등록되지 않은 ID/깨진 이미지에 접근 가능한 기존 fallback, base 변경 시 실패 상태 초기화. 출처·로컬 자산 기록 유지 |
| 19–22 | `SessionLibraryButtons.tsx`: History 🕘 / Favorite ⭐ 두 버튼, 6개 언어 accessible name/title, 같은 library ID와 각각의 anchor. `COMMON.patch`가 베이스 선택·새 제작 행 바로 아래에 배치                    |
| 20    | `SessionLibrary.favoritesOnly?: boolean`: 같은 `Films`에서 favorite===true만 표시. 원래 배열 index와 sessionNumber 유지, 필터 전환 자체는 기록·active·cursor를 저장하거나 변경하지 않음                     |
| 21    | `session-library.css`: 사이트의 어두운 배경·금색 테두리, expanded/hover/focus/disabled 스타일                                                                                                               |
| 23    | `COMMON.patch`: 이전/다음 버튼 내부 아이콘만 30px·금색으로 확대, 원래 aria-label·disabled·browse 이벤트 유지                                                                                                |
| 24    | `COMMON.patch`: `placement-label` DOM 제거, 중앙 버튼의 기존 accessible name 유지                                                                                                                           |
| 25    | `COMMON.patch`: 같은 locale-disclosure와 조건을 footer 첫 내용으로 이동, 반응형 줄바꿈. 원래 6개 언어 메시지·출처 링크·workspace tabpanel ID/aria 연결 유지                                                 |

`App.tsx`의 `/admin` 분기에는 원래 locale-disclosure가 없다. 이번 패치는 원래 disclosure를 소유한 CraftingPage footer만 수정하며 admin에 새 고지를 추가하지 않는다. LocaleSelector의 위치·키보드 조작·document.lang 동작도 그대로다.

## 이미지와 library 상태 계약

`slotBaseItemId`는 현재 `concrete.baseItemId`가 있으면 이를 사용한다. concrete 전에는 인식된 가져오기 또는 base 선택의 현재 initial state ID를 사용하며, 알 수 없는 paste에 Solar 이미지를 지정하지 않는다. 표시 이름/slug로 이미지를 추측하지 않는다. import·선택·history 복원에서 최신 catalog/query revision 경계를 유지해야 한다.

두 trigger는 `SessionLibraryButtons.openView`의 `'history' | 'favorites' | null` 값을 제어한다. 부모의 `leftMenu`에 `'favorites'`를 추가하고 같은 SessionLibrary에 `favoritesOnly={leftMenu === 'favorites'}`를 전달한다. anchor도 실제 opener의 ref로 바꿔야 한다. 모드 변경 시 내부 편집/삭제 확인 상태를 remount하고 기존 LeftShelfOverlay의 Escape/밖 클릭/닫기 및 opener focus 복귀를 재사용한다.

`history`에는 전체 `filmState.history`를 전달한다. 부모에서 필터된 Films를 만들면 원본의 legacy 번호·active 관계를 훼손할 수 있으므로 사용하지 않는다. onSelect/onRename/onFavoriteChange/onDelete/Undo와 `commitLibraryHistory`는 기존 한 벌을 그대로 사용한다. 삭제 후 필터 결과가 비어도 Undo는 남으며, 별 해제는 전체 기록을 삭제하지 않는다. 실제 저장 기록은 열거나 변경하지 않았고 jsdom fixture만 사용했다.

## C 축성 계약

`ItemCardData` 타입 확장 없이 `flags: [{ id: 'sanctified', text: 'SANCTIFIED' }]`를 전달하면 카드 맨 아래에 6개 언어로 표시한다. 기존 영어 파싱 flag `Sanctified`도 인식한다. flags 외 modifier/property 문장은 이 상태 표시로 변환하지 않는다. 카드 입력과 raw source는 변경하지 않는다.

`COMMON.patch`는 기존 concrete 카드의 비어 있던 flags에서 `conditions.includes('SANCTIFIED')`를 연결한다. 통합 중 다른 flags가 추가되었다면 기존 flags에 이 행을 합치고 덮어쓰지 않는다. 축성 배율·defence·확률을 D가 계산하지 않는다. C의 `sanctifiedValues(state, modifier, layer)`를 root의 localizedModifierText 이전에 정확히 한 번 연결하고, `derivedDefences` 결과 역시 root가 properties로 전달한다.

**C와 함께 필요한 history 검증:** `HISTORY-SANCTIFICATION.patch`는 C의 `validSanctification` 및 ConcreteItem.sanctification 계약에 의존한다. C commit을 반영한 후 함께 적용한다. 기존 verifiedHistoryState의 conditions 제한에 SANCTIFIED singleton을 허용하고, 메타가 있을 때 C helper로 catalog 대상·factor의 완전성을 검증한다. 메타가 없는 과거 SANCTIFIED 기록도 원래 값을 보존해 읽으며, 알 수 없는 배율을 만들지 않는다. raw modifier.values의 catalog 범위 검증과 CORRUPTED+desecrated 검증은 유지한다. C 코드가 아직 없는 D 단독 branch에는 적용하지 않았으므로 이 연결은 통합 검증 대상이다. C도 `sanctification/history.patch`를 제공한다고 전달받았으므로 같은 조건/import hunk는 한 번만 병합한다. 특히 basic jewel의 reviewedBasicJewel 등 다른 조건 제한도 C가 허용하는 베이스 범위와 함께 확인한다. 이 패치 없이 SANCTIFIED 기록의 완전한 재사용을 완료했다고 표시하면 안 된다.

storage key/version, frame/evidence, metadata, sourceDocument, rulesetIdentity, .preserved 백업 동작은 D 구현에서 변경하지 않았다. 원문과 원래 modifier.values 및 새 sanctification 메타는 기존 spread/structuredClone 경로로 보존된다. 실제 저장 파일 54개·DB/Redis/volume·설정에 접근하거나 변경하지 않았다.

## 검증과 자체 리뷰

명령 실행 위치: 위 작업 경로의 `frontend` (patch 검사는 저장소 root). Node `v24.21.0`, npm `11.19.0`.

- `npm ci`: 첫 sandbox 실행은 DNS ENOTFOUND 실패. 같은 lockfile로 일반 로컬 재시도 성공. package/lockfile 변경 없음.
- `npm run lint`, `npm run typecheck`, `npm run format:check`: 통과.
- 직접 관련 검사: `npm run test -- --run src/features/crafting/ItemCard.test.tsx src/features/crafting/item-slot/ItemSlotImage.test.tsx src/features/crafting/session-library/SessionLibrary.test.tsx src/features/crafting/session-library/SessionLibraryFavorites.test.tsx src/features/crafting/workbenchHistory.test.ts`: 5 files / 36 tests 통과. 첫 sandbox 시도는 Vite temp cache ENOENT로 test 수집 실패, 일반 로컬 재시도 통과. 테스트용 fixture 외 실제 localStorage/저장 파일을 사용하지 않음.
- 최종 이미지 drag 방지까지 반영한 같은 5개 파일을 `--maxWorkers=1`로 재검사: 5 files / 36 tests 통과, 22.83초. 변경된 image 모듈의 ESLint/Prettier도 재검사했다.
- `npm run build`: 통과. 기존 크기 경고 있음. 새 image/library component의 실제 page 연결은 제공 patch를 root에 반영한 뒤 검증해야 함.
- `git apply --check` COMMON.patch 및 HISTORY-SANCTIFICATION.patch: 통과. 공통 대상 소스는 수정하지 않음.
- 이미 시작한 전체 frontend 검사는 최종 결과를 아래에 기록한다. 이후 root가 통합 후 전체 검사를 한 번 수행한다는 조정을 받았다. D 단독 결과를 통합 완료로 표현하지 않는다.
- Backend/DB 통합/codegen/Compose 검사는 D 변경 영역이 아니며 미실행. 특히 DB 통합/codegen은 종전 차단 및 no-socket 제약에 따라 수행하지 않음. Docker/socket/브라우저 자동 QA/서버 시작·변경/remote push 모두 미수행.

자체 리뷰: 필터를 원본 index 계산 이후 적용하는지, filter로 active/cursor를 바꾸지 않는지, Undo가 빈 필터에서도 보이는지, 실제 opener ref와 Escape 복귀, flags 외 문구/원본 값을 건드리지 않는지, exact base ID 교체와 broken fallback, 6개 언어의 label/title을 diff 및 fixture 테스트와 대조했다. 독립 리뷰를 수행했다고 주장하지 않는다.

### 전체 frontend 검사 결과

`npm run test -- --run`은 142 files 중 135 pass / 7 fail, 2,499 tests 중 2,489 pass / 10 fail, 174.58초로 종료했다. C/A/B와 동시 호스트 실행 중 시작한 검사이며, root의 전체 검사 일원화 안내 전에 시작해 강제 종료하지 않고 결과를 보존했다.

실패 파일과 항목:

- `CraftingExplorer.test.tsx` 2건: grouped tier 탐색에서 Preview Orb of Transmutation을 찾지 못함, 선택 base 적용 검사 timeout.
- `DesecrationCurrency.test.tsx` 2건: first-tab bone 적용·복원에서 rare 카드 대기 실패와 timeout.
- `ItemTextImport.test.tsx` 1건: FUTURE type 응답 거부 검사 timeout.
- `MaterialStash.test.tsx` 1건: 검색 위치/essence 그룹 검사 timeout.
- `WorkbenchExtension.test.tsx` 1건: Whittling pair 검사 timeout.
- `method-search/scenarioCode.test.ts` 2건: expansion bomb timeout 후 다음 slot 해제 검사 SCENARIO_BUSY.
- `method-search/stateProjection.test.tsx` 1건: terminal 카드/줌 검사 timeout.

D 관련 5개 파일은 이 전체 실행에서도 실패 목록에 없다. 동시 부하와 timeout 연관은 관찰에 따른 가능성이며, 기준 소스의 동일 조건 전수 비교를 수행하지 않아 전부 기존 실패라고 단정하지 않는다. 요청에 따라 root가 통합 후 `--maxWorkers=1` 전수에서 비교한다. D는 실패를 skip·timeout 완화·기대값 변경으로 숨기지 않았고, 무관한 테스트/구현을 수정하지 않았다.
