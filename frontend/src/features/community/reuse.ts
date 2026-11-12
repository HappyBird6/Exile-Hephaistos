import { accountScope } from '../account/auth'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { saveBaseFavorite } from '../crafting/base-favorites/repository'
import {
  appendDocument,
  documentFor,
} from '../crafting/session-history/transfer'
import { validStoredFilm } from '../crafting/workbenchHistory'
import { baseKeyForItem } from '../crafting/baseRegistry'
import { loadInitial } from '../crafting/craftingApi'
import { freshSession } from '../crafting/session-library/freshSession'
import type { Post } from './api'
export type ReuseAction = 'import' | 'craft' | 'favorite'
export async function reusePost(
  post: Post,
  cursor: number,
  action: ReuseAction,
  signal: AbortSignal,
) {
  if (
    !validStoredFilm(post.film) ||
    !Number.isInteger(cursor) ||
    !post.film.frames[cursor]
  )
    throw Error('INVALID_SHARED_SESSION')
  const scope = accountScope()
  const film = structuredClone(post.film)
  const frame = film.frames[cursor]!
  const guard = () => {
    signal.throwIfAborted()
    if (scope !== accountScope()) throw Error('ACCOUNT_CHANGED')
  }
  if (action === 'favorite') {
    guard()
    saveBaseFavorite({ ...film, frames: film.frames.slice(0, cursor + 1) })
    return null
  }
  const repo = new IndexedFilmRepository(localStorage, indexedDB, scope)
  try {
    const history = await repo.load()
    guard()
    if (action === 'import') {
      film.id = `community-${post.id}-r${post.revision}`
      if (post.title) film.title = post.title
      const imported = appendDocument(history, documentFor(film))
      await repo.save(imported.history)
      return '/?workbench=history&session=' + encodeURIComponent(imported.id)
    }
    const base = baseKeyForItem(frame.state.baseItemId)
    if (!base) throw Error('UNSUPPORTED_BASE')
    const catalog = await loadInitial(frame.state.itemLevel, signal, base, true)
    guard()
    const next = await freshSession(
      history,
      film,
      frame,
      catalog,
      crypto.randomUUID(),
      signal,
      true,
    )
    guard()
    await repo.save(next)
    return '/?workbench=simulation'
  } finally {
    await repo.close()
  }
}
