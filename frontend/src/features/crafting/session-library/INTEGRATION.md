# 담당 C: History 세션 라이브러리 연결

기준: `6040357faf4e39cc989991d44ef8d04c7119dbd0`. 기존 파일 소유 범위는 `../workbenchHistory.ts`, `../workbenchHistory.test.ts`뿐이며 공통 화면은 수정하지 않았다.

## 연결 API

`SessionLibrary.tsx`는 `open`, `onOpenChange`, `history`, `onSelect`, `onRename`, `onFavoriteChange`, `onDelete`를 부모가 제어한다. `id`, `anchor`, 선택적 `shelf` ref의 의미는 `../base-picker/INTEGRATION.md`와 같다. 기존 `.workbench-film-select` select를 제거하고 History 버튼을 같은 위치/아이템 제어 영역에 둔다. 기존 이전/다음 제작 단계 버튼과 원문 보기, 규칙 불일치 표시 등은 유지한다.

```tsx
import { SessionLibrary } from './session-library/SessionLibrary'
import {
  deleteFilm, undoFilmDeletion, updateFilmMetadata, filmSessionNumber,
} from './workbenchHistory'
import type { FilmDeletion } from './workbenchHistory'

const [lastDeletion, setLastDeletion] = useState<FilmDeletion | null>(null)

<button
  ref={historyAnchor}
  type="button"
  aria-expanded={leftMenu === 'history'}
  aria-controls="workbench-history"
  disabled={applying !== null}
  onClick={() => setLeftMenu(leftMenu === 'history' ? null : 'history')}
>History</button>

<SessionLibrary
  id="workbench-history"
  anchor={historyAnchor}
  shelf={currencyShelf}
  open={leftMenu === 'history'}
  onOpenChange={(open) => setLeftMenu(open ? 'history' : null)}
  history={filmState.history}
  disabled={applying !== null}
  onSelect={(id) => {
    restoreFilm(id)
    setLeftMenu(null)
  }}
  onRename={(id, title) => {
    if (applyingRequest.current || placementPending.current) return
    saveFilms(updateFilmMetadata(filmState.history, id, { title }))
  }}
  onFavoriteChange={(id, favorite) => {
    if (applyingRequest.current || placementPending.current) return
    saveFilms(updateFilmMetadata(filmState.history, id, { favorite }))
  }}
  onDelete={handleDeleteSession}
  {...(lastDeletion ? {
    undo: {
      title: lastDeletion.film.title?.trim()
        ? lastDeletion.film.title
        : t('film.session', { index: filmSessionNumber(lastDeletion.film, lastDeletion.index) }),
      onUndo: handleUndoSession,
    },
  } : {})}
/>
```

History 버튼의 번역은 `sessionLibraryMessages[locale].history`를 사용한다. 기존 6개 언어를 전용 메시지 파일로 제공하며 공통 messages.json은 변경하지 않았다.

## 保存・選択を一度の変更で行う

삭제 시 부모의 `if (applyingRequest.current || placementPending.current) return` 가드를 유지한다. `deleteFilm(filmState.history, id)`의 대상은 **명시된 1개 세션뿐**이다. null 반환이면 아무것도 하지 않는다. UI는 해당 카드에서 삭제→대상 제목을 포함한 확인→삭제 순으로 진행한다. 확인 전에는 onDelete를 호출하지 않는다.

```ts
function handleDeleteSession(id: string) {
  if (applyingRequest.current || placementPending.current) return
  const result = deleteFilm(filmState.history, id)
  if (!result) return
  commitLibraryHistory(result.history)
  setLastDeletion(result.deletion)
}
function handleUndoSession() {
  if (!lastDeletion || applyingRequest.current || placementPending.current)
    return
  commitLibraryHistory(undoFilmDeletion(filmState.history, lastDeletion))
  setLastDeletion(null)
}
```

`commitLibraryHistory(next)`는 다음을 하나의 동기 이벤트 안에서 수행하는 통합용 helper이다. 이 helper는 공통 화면의 draft/query 상태를 다루므로 담당 C가 작성하지 않았다.

1. `next.active`/선택 프레임이 현재와 달라졌다면, **next에서** 활성 세션과 `next.cursor` 프레임을 읽는다. `restoreFilm`의 기존 baseItemId→base key 해석을 재사용하되 최신 registry/topBaseKey와 legacy identity를 보존한다. `draft.setBase(state.itemLevel, base)` 후 `saveFilms(next, useItemDraft.getState().baseRevision)`를 한 번 호출한다. cursor는 helper가 반환한 값을 유지한다.
2. 비활성 세션을 삭제/복구하여 활성 프레임이 그대로면 baseRevision을 바꾸지 않고 `saveFilms(next)`만 호출한다. 동일 프레임의 선택/원문/현재 제작 상태를 보존한다.
3. 마지막 활성 세션을 삭제해 `next.active === null`이면 `saveFilms(next, currentRevision)`와 함께 현재 baseRevision의 `workbenchKey`/`mappingKey` 캐시와 이전 derived item 상태를 정리하여 삭제된 아이템을 계속 제작하지 않게 한다. 저장되지 않은 새 빈 베이스로 전환하는 선택도 가능하지만 새 세션을 임의 생성하지 않는다.
4. 선택 변경 때 기존 `restoreFilm`처럼 `setSelected(null)`, `setHeld(null)`, `setPointer(null)`, announcement 정리를 한다. 새 서버 요청을 만들거나 원문을 재파싱하여 identity를 바꾸지 않는다.

**주의:** `saveFilms(result.history)` 직후 기존 `restoreFilm(result.history.active)`를 호출하면 그 함수의 이전 `filmState.history` closure가 삭제한 세션을 다시 저장할 수 있다. `restoreFilm`을 `(id, sourceHistory = filmState.history)`로 일반화하거나 위 helper에서 **next 한 벌**로 draft와 저장을 갱신한다. 삭제가 저장 오류/blockedSaving 때문에 영속화되지 않으면 사용자에게 기존 오류를 유지하고 undo 토큰을 버리지 않는다.

Undo 토큰은 패널이 닫혀도 부모 상태에 유지한다. 즉시 복구가 가능하며 세션 목록/제목/별 변경 및 새 세션 생성 이후에도 기존 레코드만 제자리에 끼워 넣는다. 새로 선택한 세션은 덮어쓰지 않는다. 페이지 재로드 후의 Undo 영속화는 이 구현 범위가 아니며 기존 repository의 `.preserved` 보존 동작은 그대로이다.

## 데이터 호환성

- storage key `hephaistos.workbench.films.v1`, version 1, frames/evidence/manual/abyss/sourceDocument/rulesetIdentity 검증·보존 경계는 유지한다.
- `Film.title?: string`, `favorite?: boolean`, `sessionNumber?: number`, `Films.nextSessionNumber?: number`만 추가했다. 기존 레코드는 load 시 migration/재저장하지 않는다. 미지의 사용자 메모 등 추가 필드도 spread로 보존한다.
- `updateFilmMetadata(history, id, {title})`는 사용자가 입력한 문자열을 잘라내거나 trim하지 않는다. 빈 문자열/공백 제목의 화면 표시는 기존 번역 `film.session`으로 되돌아간다. 즐겨찾기 별은 true일 때 노랑, 아니면 회색이다.
- `startFilm`은 기록별 sessionNumber와 다음 번호를 저장한다. legacy 기록은 기존 순서의 index+1을 사용하고 삭제 시 기존 번호를 고정한다. 삭제해도 남은 세션의 이름/번호가 당겨지거나 삭제한 마지막 번호를 재사용하지 않는다.
- `deleteFilm`은 활성 세션이면 인접 세션의 마지막 단계, 목록이 비면 active=null/cursor=0을 선택한다. 비활성 삭제는 기존 활성/cursor를 유지한다.
- `undoFilmDeletion`은 삭제 당시 프레임/원문/제목/별/규칙 식별자를 그대로 복구한다. 중간에 추가한 세션과 다른 세션의 편집을 덮지 않는다.

컴포넌트는 소형 직사각 카드로 되어 있으며 `ItemCard` 전체를 세션 카드로 사용하지 않는다. baseItemId의 실제 이미지를 `../base-picker/BaseImage.tsx`에서 공유한다. 이미지 대체/출처 안내는 base-picker의 문서를 참고한다. 실제 사용자 localStorage는 QA에 접근하거나 삭제하지 않았다. 단위 테스트는 jsdom fixture 저장소만 사용했다.
