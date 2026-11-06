import { deleteFilm, undoFilmDeletion } from '../workbenchHistory'
import type { Films } from '../workbenchHistory'

export const deletionUndoMilliseconds = 60_000

/** Record removal and its recovery token are persisted in a single history write. */
export function queueSessionDeletion(
  history: Films,
  id: string,
  now = Date.now(),
): Films | null {
  const result = deleteFilm(history, id)
  if (!result) return null
  return {
    ...result.history,
    pendingDeletion: {
      deletion: result.deletion,
      expiresAt: now + deletionUndoMilliseconds,
    },
  }
}
export function clearDeletionUndo(history: Films): Films {
  if (!history.pendingDeletion) return history
  const next = { ...history }
  delete next.pendingDeletion
  return next
}
export function restoreDeletedSession(history: Films, now = Date.now()): Films {
  const pending = history.pendingDeletion
  if (!pending || pending.expiresAt <= now) return clearDeletionUndo(history)
  return clearDeletionUndo(undoFilmDeletion(history, pending.deletion))
}
