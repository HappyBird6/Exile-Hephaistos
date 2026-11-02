# 담당 A — 오른쪽 창고 연결

기준 커밋: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
소유 범위: `materials.ts`, `MaterialStash.test.tsx`, 새 `stash/` 디렉터리.
공통 파일·카탈로그·원본 작업트리·서버·저장 기록은 변경하지 않았다.

## 화면 연결 위치

`CraftingPage.tsx`의 `.stash-canvas` 안에 있는 기존 `.favorite-slots` 그룹만
`WorkbenchStash`로 교체한다. 기존 상단 `materialTabs`, 왼쪽 카탈로그, 중앙 아이템,
바깥 workspace 탭은 그대로 둔다. 컴포넌트 CSS는 기존 즐겨찾기의 오른쪽 좌표부터
캔버스 오른쪽 가장자리까지 배치되며, 내부 카탈로그만 스크롤하고 하단 즐겨찾기
2행 × 5칸은 고정한다. `crafting.css`를 수정할 필요가 없다.

새 내부 탭 상태는 기존 `activeTab`과 분리한다.

```tsx
import { WorkbenchStash } from './stash/WorkbenchStash'
import type { WorkbenchResource } from './stash/stashTypes'
import { isSelectableMaterial } from './materials'

const [stashTab, setStashTab] = useState<MaterialTab>('Currency')
const [favorites, setFavorites] = useState<(WorkbenchResource | null)[]>(
  Array(10).fill(null),
)
const [held, setHeld] = useState<WorkbenchResource | null>(null)

const stashResources: WorkbenchResource[] = [
  ...currencies.map((currency) => ({
    ...currency,
    name: currencyNames[currency.id],
    category: 'Currency' as const,
  })),
  ...materials,
]

<WorkbenchStash
  resources={stashResources}
  excludedIds={excludedIdsFromB}
  activeTab={stashTab}
  onTabChange={setStashTab}
  selectedId={selected?.id ?? null}
  activeOmenIds={draft.activeOmens}
  favorites={favorites}
  held={held}
  disabled={applying !== null}
  showLegacyOmens={showLegacyOmens}
  onShowLegacyOmensChange={setShowLegacyOmens}
  onSelect={(resource, pointer) => {
    setPointer(pointer ?? null)
    choose(resource)
  }}
  onPickUp={(resource, pointer) => {
    setPointer(pointer ?? null)
    setHeld(resource)
    setSelected(null)
    setAnnouncement(t('material.picked', { name: name(resource.id, resource.name) }))
  }}
  onFavoriteActivate={(_, resource, pointer) => {
    // pointer is present for right-click/context-menu activation only.
    // Preserve the old left-click choose / right-click Omen toggle distinction.
    if (pointer && resource?.category === 'Omen') {
      toggleOmen(resource.id)
      return
    }
    setPointer(pointer ?? null)
    if (resource) choose(resource)
    else setSelected(null)
  }}
  onFavoritePlace={(index, resource) => {
    // Adapt placeFavorite to accept the explicit resource. Native drag/drop can
    // complete before a React render has committed the updated held state.
    const replaced = favorites[index]
    if (
      replaced &&
      !favorites.some((entry, position) => position !== index && entry?.id === replaced.id)
    )
      draft.setActiveOmens(draft.activeOmens.filter((id) => id !== replaced.id))
    setFavorites((current) =>
      current.map((entry, position) => position === index ? resource : entry),
    )
    setAnnouncement(t('material.registered', {
      name: name(resource.id, resource.name), index: index + 1,
    }))
    setHeld(null)
  }}
/>
```

## 계약과 동작

- `WorkbenchResource`: `{ id, name, image, category: MaterialTab }`.
  통합 시 `held`/`favorites`의 기존 `Material` 타입을 이 타입으로 넓힌다.
- `excludedIds`는 배열 또는 `ReadonlySet<string>`. B가 제공하는 제외 ID를 그대로
  전달한다. A는 갉힌 뼈 ID를 제품 코드에 하드코딩하지 않는다.
- `activeTab`/`onTabChange`, 선택/들기/즐겨찾기/징조 활성 상태는 부모 제어형이다.
  검색어만 내부 탭별로 유지하며, 탭 변경 시 들고 있는 재료를 취소하지 않는다.
- 화폐 왼쪽 클릭은 기존처럼 사용 선택, 다른 자원 왼쪽 클릭은 즐겨찾기 등록용
  들기, 자원 오른쪽 클릭은 사용 선택이다. 즐겨찾기 오른쪽 클릭의 징조 토글은
  부모의 기존 `toggleOmen`을 호출해야 한다.
- `onFavoriteActivate`는 빈 칸 클릭도 전달해 사용 선택을 취소할 수 있게 한다.
  이 콜백의 `pointer`는 오른쪽 클릭에만 존재한다.
- 자원의 기본 드래그도 지원한다. `onFavoritePlace(index, resource)`의 두 번째
  인자를 사용해 배치하고, 이전 징조가 마지막 즐겨찾기에서 제거될 때만 비활성화한다.
  외부 임의 드롭 ID는 수락하지 않는다. 기존 전역 Escape/커서 처리를 유지한다.
- `favorites`는 위치 0–9를 표시한다. 초과 항목을 배열에서 삭제하지 않으므로
  기존 기록을 마이그레이션하거나 잘라 저장하지 않는다. 현재 기준의 즐겨찾기는
  페이지 내 비영속 상태이므로 새 페이지 상태를 10칸으로 초기화하면 된다.
- 기존 비공개/제외 재료가 즐겨찾기에 들어 있으면 내용은 보존하고 신규 사용만
  막는다. 새 자원으로 교체할 수 있다. 기록·원본 카탈로그를 지우지 않는다.
- 기존 `MaterialTooltip`의 `data-material-tooltip` 계약과 6언어 번역을 재사용한다.
  부모의 기존 툴팁 토글 및 `!held && !selected` 표시 조건을 유지한다.

## 기존 왼쪽 카탈로그와 에센스

- `materials`의 180개 ID/이미지/원문은 그대로다. 새 `isSelectableMaterial`은
  일반 에센스의 Greater/Perfect만 신규 선택에 허용하고, 기존 특수 에센스는 유지한다.
- `essenceRows(query)`는 기존 19계열별 4종 대신 상위·완벽 2종을 반환한다.
  기존 화면의 선택 목록도 이 변경을 적용받는다. `currentMaterials`를 다른
  신규 선택 UI에 쓰면 `isSelectableMaterial(m)`도 필터 조건에 추가한다.
- 왼쪽 화폐 선택 목록도 B의 `excludedIds`로 필터해야 갉힌 뼈를 왼쪽에서 다시
  선택할 수 없다. `currencies.ts`/원본 카탈로그에서 ID를 삭제하지 않는다.
- 현재 왼쪽 화폐 목록의 뼈 화폐 좌표도 오른쪽 영역에 있으므로, 화면 연결 시
  기존 절대 좌표의 뼈 슬롯을 제거하고 새 내부 창고에서만 렌더한다. 숙련공의
  오브 역시 새 창고로 옮길 항목이다. 기존 버튼 목록에서
  `!isBone(currency.id) && currency.id !== 'Artificers_Orb'`로 렌더 위치만 제한하면
  된다. 원본 화폐 데이터와 일반 화폐 탭은 유지한다.
- 기존 왼쪽 에센스 `.essence-row`는 공통 CSS에 4열로 선언돼 있다. 두 종류를
  넓게 쓰려면 통합 담당이 `repeat(2, minmax(0, 1fr))`로 바꾼다. A의 전용 CSS는
  기존 상단/왼쪽 UI 선택자를 덮어쓰지 않는다.
- 게임 번역 카탈로그 확인: `gameTerms.json`의 ko.`Artificers_Orb`.name은
  **숙련공의 오브**다. 별도 이름의 화폐를 추가하거나 다른 ID로 치환하지 않았다.
- 기존 `MaterialStash.test.tsx`는 에센스 선택 변경만 반영했다. 통합 후에는 상단과
  내부에 같은 이름의 탭·검색창·화폐가 함께 존재하므로 기존 테스트의 질의를
  바깥/안쪽 영역으로 한정하고, 즐겨찾기 기대값 15를 10으로 변경해야 한다.
  새 `stash/WorkbenchStash.test.tsx`가 독립 컴포넌트의 10칸 계약을 검증한다.

## 이미지와 검증 범위

이 담당에서는 사용자 이미지 첨부가 필요하지 않다. 원본의 뼈 화폐 `image: ''`
항목은 기존 `CurrencyImage`의 번역 이름 fallback으로 표시한다. 임의 이미지를
만들거나 공통 asset manifest를 수정하지 않았다.

브라우저 검증은 위임 조건에 따라 수행하지 않는다. 서버/API 동작과 공통 화면
연결은 통합 담당의 최종 검증 대상이다. 이 컴포넌트는 API를 추가 호출하지 않는다.

## 담당 A 검증 결과 (2026-11-02)

- `npm ci`: 성공. 원본 작업트리의 `node_modules`를 공유하지 않고 별도로 설치했다.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`: 성공.
- `pretest` 표시 번역 검사: 기존 6언어 각각 220/220개 이름, 예외 없음.
- 전체 테스트: `npm run test -- --run --pool=threads --maxWorkers=2
--reporter=json --outputFile=<작업 폴더>/stash-full-tests.json`.
  총 2,428개 중 2,424개 통과, 4개 실패. 직접 관련된 `WorkbenchStash`,
  `MaterialStash`, `serviceScope`의 33개는 모두 통과했다.
- 실패 4개 파일을 `--pool=threads --maxWorkers=1`로 별도 재실행:
  **38/38개 통과**, 타임아웃과 테스트 기대값 변경 없음.
  대상은 `DesecrationCurrency.test.tsx`, `WorkbenchExtension.test.tsx`,
  `goal-filter/CraftStart.catalogRecovery.test.tsx`,
  `method-search/AutomaticMethodSearch.test.tsx`다.
  전체 실행에서 앞의 세 파일은 5초 타임아웃, 마지막 파일은 비동기 준비 상태에서
  버튼을 찾지 못했다. 전체 실행 결과를 단일 실행 전부 성공으로 표현하지 않는다.
- Windows 샌드박스의 기본 임시 경로에서 Vitest 모듈 임시 파일 ENOENT가 발생해,
  테스트 프로세스의 `TEMP`/`TMP`를 배정된 작업 폴더의 `verification-temp`로
  지정했다. 저장소 설정이나 전역 환경 변수는 변경하지 않았다.
- 자체 리뷰와 `git diff --check`: 완료. 공통 소유 파일 변경 없음.
- 브라우저·서버·실제 운영 API·backend 검증: 담당 범위와 위임 조건상 미실행.
