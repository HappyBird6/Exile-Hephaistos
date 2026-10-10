# Path tree 프론트 통합 인계

기준 계약은 `bb8c624c51106b0047676169bb70362ffe04af7f`이다. 이 namespace만 구현했다. `CraftStart`, `ConnectedGoalFilter`, `editor`, `App`, 번역 루트, 공유 CSS, Backend와 계약 원본은 변경하지 않았다. **프론트 모듈 완료는 Backend 연결 또는 제품 통합 완료가 아니다.** 실제 Solar endpoint의 완전 계산, 서버 취소/재개 경합, 최종 제품 화면은 부모 통합에서 검증해야 한다.

## 공개 인터페이스

```tsx
import {
  PathTree,
  createPathSearchRequest,
  createItemPresentation,
} from '../path-tree'

const request = createPathSearchRequest(item, goal, provenance, activeOmens)
const presentItem = createItemPresentation(
  { name: selectedBase[2], itemClass: selectedBase[1] },
  frozenDefinitions,
)

<PathTree
  request={request}
  inputGeneration={run}
  presentItem={presentItem}
  onRecalculate={reopenSettings}
/>
```

`request`는 시작 버튼에서 한 번에 확정한 입력이다. `createPathSearchRequest`는 기존 optional `fractured=false`, socket/quality의 알려지지 않은 값 `null`만 wire shape에 명시하며 숫자를 보정하지 않는다. 원래 roll·goal AST·provenance를 deep clone하고 안전 정수/관측점을 검증한다. activeOmens가 비어 있지 않으면 거부한다. 관측점은 문자열 `100/300/500`이며 기본 선택은 100이다. 서버가 이 시작 상태를 지원하는지는 endpoint가 결정한다.

`presentItem`은 같은 frozen catalog의 `Definition`들로 만든다. 새로운 stat도 Definition과 실제 values로 렌더링하며 stat별 switch는 없다. 표시 Definition이 없으면 ID를 노출하지 않고 현지화된 확인 불가 문구를 표시한다. 원본 state는 유지한다. `ItemCard`는 기존 공개 컴포넌트를 사용한다.

기본 adapter는 실제 `/api/v1/crafting/path-searches` HTTP 호출이다. 테스트만 `PathSearchAdapter`를 주입한다. production fallback fixture는 없다. request 또는 inputGeneration 변경은 새 세션을 만들고 이전 서버 job 취소를 요청한다. 입력 편집/현재 ruleset·catalog 변경 시에는 아래 통합에서 트리 자체도 즉시 unmount한다.

## 부모가 적용할 공통 파일 패치

### 1. ConnectedGoalFilter.tsx

기존 `ConnectedGoalFilter`와 내부 `ConnectedEditor` 양쪽 props에 아래 optional prop을 추가하고 그대로 전달한다. 기존 사용자는 영향을 받지 않는다.

```tsx
onSearchGoalChange?: (snapshot: {
  goal: GoalFilter
  catalogVersion: string | null
  ready: boolean
}) => void
```

`ConnectedEditor`의 `currentCatalog = useGoalFilterCatalog(...)` 다음에 다음 effect를 추가한다. `goal`은 기존 Zustand에서 읽는 객체이고 별도 목표 편집기를 만들지 않는다.

```tsx
useEffect(() => {
  const catalog = currentCatalog.data
  onSearchGoalChange?.({
    goal: structuredClone(goal),
    catalogVersion: catalog?.catalogVersion ?? null,
    ready:
      currentCatalog.isSuccess &&
      !currentCatalog.isFetching &&
      catalog?.catalogVersion === goal.catalogVersion &&
      catalog.context.snapshotId === context.snapshotId &&
      catalog.context.baseItemId === context.baseItemId &&
      catalog.context.itemLevel === context.itemLevel,
  })
}, [
  goal,
  currentCatalog.data,
  currentCatalog.isSuccess,
  currentCatalog.isFetching,
  context.snapshotId,
  context.baseItemId,
  context.itemLevel,
  onSearchGoalChange,
])
```

목표 catalog가 변경되어도 기존 goal에 새 version을 덮어쓰지 않는다. ready=false로 설정 원본을 보존하고 사용자가 목표를 다시 검토하도록 한다. 기존 numeric evaluate UI는 path-search 시작 버튼과 별도 요청을 동시에 발행하도록 연결하지 않는다.

### 2. CraftStart.tsx

기존 `Draft`에 `searchGoal`(위 snapshot 또는 null), `searchRequest: CreateRequest | null`을 추가하고 초기값을 null로 둔다. `rootDefinitions`는 이미 있으므로 같은 시점에 고정한다. callback은 `useCallback`으로 안정화한다.

```tsx
const onSearchGoalChange = useCallback(
  (searchGoal: SearchGoalSnapshot) => {
    editor.setState({ searchGoal, searchRequest: null })
  },
  [editor],
)
```

기존 compact `<ConnectedGoalFilter ... />`에 `onSearchGoalChange`를 전달한다. `loadProvenance`는 기존 `basic-paths/api` 공개 함수를 사용하고 TanStack Query key에 `draft.rulesetIdentity`를 포함한다. enabled는 현재 ruleset이 확인된 경우만 true이며 `retry:false`로 둔다. 응답 provenance.rulesetIdentity와 draft.rulesetIdentity가 다르면 시작을 막는다.

기존 `craft-start-primary` 버튼 disabled 조건에 `!draft.searchGoal?.ready`, provenance loading/error/mismatch를 추가한다. 기존 onClick의 `update({started:true,...})` 직전에 다음 생성 코드를 넣고, 같은 update에 searchRequest를 저장한다.

```tsx
const searchRequest = createPathSearchRequest(
  draft.item!,
  draft.searchGoal!.goal,
  provenance.data!,
  activeOmens,
)
// 기존 update 객체에 searchRequest 추가. rootItem/rootDefinitions와 같은 클릭 snapshot.
```

실패한 입력을 임의 수정하지 않는다. helper 예외는 간결한 6언어 입력 안내로 처리하고 버튼 이후 상태를 변경하지 않는다. 기존 animation/focus 코드와 접힌 설정 UX는 유지한다.

현재 파일 하단 `{draft.started && draft.root && (...)}`의 빈 트리 전체를 다음으로 교체한다. 기존 별도 root card를 중복 표시하지 않는다.

```tsx
{
  draft.started &&
    !draft.editing &&
    draft.searchRequest &&
    draft.rootDefinitions && (
      <PathTree
        request={draft.searchRequest}
        inputGeneration={run}
        presentItem={createItemPresentation(
          { name: draft.root!.base!, itemClass: draft.root!.itemClass },
          draft.rootDefinitions,
        )}
        onRecalculate={() => {
          update({ editing: true, searchRequest: null })
          requestAnimationFrame(() => startButton.current?.focus())
        }}
      />
    )
}
```

`saveItem`/base 변경/원문 import/목표 변경/설정 다시 열기/현재 ruleset 또는 catalog 무효화 지점에서 `searchRequest:null`로 만든다. 서버는 cancel endpoint로 취소되고, 브라우저 AbortSignal만 보내는 것으로 대체하지 않는다. 부모 설정을 편집 중이면 search component는 mount하지 않는다. `PathTree`는 내부 서버 상태를 Query로, 부모 편집 상태는 기존 Zustand로 유지한다. App 메뉴·전역 번역·공유 CSS 패치는 필요 없다.

## 동작 경계

- 구조는 schema와 의미 검증을 모두 통과해야 한다. jobId/clientRequestId/fingerprint/full provenance/revision/recovery identity가 일치하지 않으면 표시하지 않는다.
- graph는 고정 revision 별 누적 페이지이며 동일 ID의 동일 내용만 중복 제거한다. 오래된 페이지, 변조된 동일 ID, 순환 cursor를 거부한다. 새 revision은 그래프를 새 snapshot으로 교체한다.
- 취소 acknowledgement 이후 이전 read가 AbortSignal을 무시해도 실행 epoch 검사로 폐기한다. resume는 이전 질량을 더하지 않고 새 snapshot으로 교체한다. REVISION_CONFLICT는 조회 후 의도가 유효하면 새 commandId로 한 번 재시도한다. 네트워크 acknowledgement 유실은 동일 body/key로 한 번 재시도한다.
- 부분 확률은 lower–upper 범위와 부분 계산 안내를 표시하고 확정 추천을 붙이지 않는다. 확정 순위가 있을 때만 추천을 강조하며 최대 5개만 표시한다. 원래 후보 번호는 관측점 변경에도 유지한다.
- 동일 state 카드는 한 번 표시하며 실행 phase는 합산하지 않는다. REPEAT는 링크로 접고 무한 재귀 렌더링하지 않는다. forward/반복은 실제 edge만 표시한다. root만 먼저 도착해도 표시한다.
- 복구 선택은 해당 execution의 실제 양의 확률 역방향 경로에서 찾은 이전 state만 제공한다. 최종 유효성은 서버가 판단한다. 복구는 별도 Query job이며 본경로와 합산하지 않는다. 부모 revision이 바뀌면 복구 선택·job을 무효화한다.
- 만료/버전 변경/삭제된 stat은 기존 실행을 무효화하고 재계산 안내만 제공한다. 자동 migration/자동 재계산은 없다.
- 모든 분수 비교·검증·백분율 표시가 BigInt 기반이다. 표시 백분율은 절삭 근사이며 1 미만을 100%로 표시하지 않는다.

## 검증 실행

`Verify.Dockerfile`은 검증 전용이다. 제품 Dockerfile/CI/의존성을 변경하지 않는다. 기존 pinned Node 이미지와 기존 `npm ci` layer를 재사용한다. 테스트가 참조하는 Backend resource와 docs/evidence는 이미지에 읽기 자료로만 복사한다. DB/서버 컨테이너·볼륨은 사용하지 않는다.

```powershell
docker build -f frontend/src/features/crafting/path-tree/Verify.Dockerfile -t exile-path-tree-verify:v1 .
docker run --rm exile-path-tree-verify:v1
```

검증 명령은 lint, typecheck, format:check, 전체 test -- --run, build 순서다. npm ci는 기존 동일 lockfile의 CACHED layer로 확인했다. 공유 schema와 생성 schema의 동일성, 모든 정상/negative synthetic fixture, 분수·ranking·page·취소/재개·복구·6언어 및 ItemCard/키보드 focus를 테스트한다. 새 평범한 stat은 Definition 데이터만 추가하는 회귀 테스트를 포함한다.

IAB 생성 시 `Browser is not available: iab`가 반환되어 실제 브라우저/모바일 시각 검증은 미실행이다. jsdom은 레이아웃 검증의 대체물이 아니다. 제품 메뉴나 개발 fixture 페이지를 추가하지 않았다. 반응형 CSS와 reduced-motion 규칙은 포함하지만 통합 후 IAB 검증이 남는다.

## 최종 검증 결과 (2026-10-10)

- 기존 동일 lockfile의 `npm ci` Docker layer: CACHED 재사용.
- 전체 `npm run lint`, `npm run typecheck`, `npm run format:check`: 통과.
- 기본 `npm run test -- --run`: 2,098 통과, 기존 UI 테스트 5건의 5,000ms timeout. 새 namespace 테스트는 모두 통과했다.
- 기대값/시간 제한/skip을 변경하지 않고 `npm run test -- --run --maxWorkers=1`로 전체 재검증: **88 files / 2,103 tests 통과**, 실패·skip 0. 이 중 path-tree 4 files / 55 tests다.
- `npm run build`: 통과. 기존 큰 bundle에 대한 Vite 500kB 경고는 남는다. 공통 App에 아직 연결하지 않은 모듈이므로 이것을 최종 제품 통합 빌드라고 표현하지 않는다.
- 자체 리뷰: 소유 범위, production fixture import 없음, 전이/CDF 구분, 독립 복구 cache, mutation 멱등 키, 늦은 응답 epoch, 읽기 전용 공통 파일 경계를 확인했다. 독립 리뷰는 수행하지 않았다.
- Backend/Compose/Windows script 변경이 없어 해당 검증은 실행하지 않았다. 기존 18090 서버·DB·볼륨은 변경하지 않았다.
