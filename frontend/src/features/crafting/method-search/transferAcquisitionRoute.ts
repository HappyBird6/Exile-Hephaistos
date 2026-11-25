import { accountScope } from '../../account/auth'
import { canonical } from '../../account/libraryPlan'
import type { Initial } from '../craftingApi'
import { plannedFrameForWorkbench } from '../session-library/freshSession'
import { IndexedFilmRepository } from '../session-library/IndexedFilmRepository'
import {
  currentFilm,
  filmRulesetStatus,
  forkFilm,
  startFilm,
  validStoredFilm,
  verifiedHistoryState,
  type Film,
  type Films,
} from '../workbenchHistory'

export type RouteDestination = 'library' | 'simulation' | 'community'

/** Compare only the verified recipe. User titles, favorites and timestamps belong to the library. */
function recipeContent(film: Film) {
  return canonical([
    film.rulesetIdentity,
    film.frames,
    ...(film.projectedRouteChance ? [film.projectedRouteChance] : []),
  ])
}

function saveRecipe(history: Films, film: Film, id: string, catalog: Initial) {
  const content = recipeContent(film)
  let savedId = id
  for (let suffix = 1; ; suffix++) {
    const existing = history.films.find((f) => f.id === savedId)
    if (!existing) break
    if (recipeContent(existing) === content) return { history, id: savedId }
    // A user may have continued or reset the saved copy. Preserve those edits.
    savedId = id + '-' + suffix
  }
  const saved = startFilm(
    history,
    film.frames[0]!.state,
    savedId,
    catalog.rulesetIdentity,
    undefined,
    catalog.compatibilityId,
  )
  const source = currentFilm(saved)!
  source.frames = structuredClone(film.frames)
  if (film.projectedRouteChance)
    source.projectedRouteChance = structuredClone(film.projectedRouteChance)
  if (film.title) source.title = Array.from(film.title).slice(0, 64).join('')
  return {
    id: savedId,
    history: { ...saved, active: history.active, cursor: history.cursor },
  }
}

export async function transferAcquisitionRoute(
  film: Film,
  cursor: number,
  destination: RouteDestination,
  catalog: Initial,
  owner: string | null,
  signal: AbortSignal,
) {
  const guard = () => {
    signal.throwIfAborted()
    if (accountScope() !== owner) throw Error('ACCOUNT_CHANGED')
  }
  guard()
  if (
    !Number.isInteger(cursor) ||
    !film.frames[cursor] ||
    !validStoredFilm(film) ||
    filmRulesetStatus(film, catalog.rulesetIdentity) !== 'current'
  )
    throw Error('INVALID_ROUTE_SESSION')

  const verified = {
    ...film,
    frames: film.frames.map((frame) =>
      plannedFrameForWorkbench(frame, catalog),
    ),
  }
  if (
    !verified.frames.every((frame) =>
      verifiedHistoryState(frame.state, catalog),
    )
  )
    throw Error('INVALID_ROUTE_STATE')
  const digest = await crypto.subtle.digest(
    'SHA-256',
    new TextEncoder().encode(canonical([film.id, recipeContent(verified)])),
  )
  const id =
    'recipe-' +
    Array.from(new Uint8Array(digest), (b) =>
      b.toString(16).padStart(2, '0'),
    ).join('')
  guard()
  const repo = new IndexedFilmRepository(
    localStorage,
    globalThis.indexedDB,
    owner,
  )
  try {
    const history = await repo.load()
    guard()
    const saved = saveRecipe(history, verified, id, catalog)
    let next = saved.history
    if (destination === 'simulation') {
      next = forkFilm(
        { ...next, active: saved.id, cursor },
        verified.frames[cursor]!.state,
        crypto.randomUUID(),
        catalog.rulesetIdentity,
        catalog.compatibilityId,
      )
    }
    if (next !== history) await repo.save(next)
    guard()
    if (destination === 'simulation') return '/?workbench=simulation'
    const target =
      destination === 'library'
        ? '/?workbench=history&session='
        : '/community/new?session='
    return target + encodeURIComponent(saved.id)
  } finally {
    await repo.close()
  }
}
