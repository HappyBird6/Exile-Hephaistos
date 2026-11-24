import type { Initial } from '../craftingApi'
import type { Film, Films, Frame } from '../workbenchHistory'
import { startFilm, currentFilm } from '../workbenchHistory'
import { verifiedHistoryState } from '../workbenchHistory'
import { checkSessionCompatibility } from '../workbenchReferenceApi'
/** The search wire may omit untouched equipment quality. The manual checkpoint records the catalog's initial zero. */
export function plannedFrameForWorkbench(
  frame: Frame,
  catalog: Initial,
): Frame {
  return frame.state.equipmentQuality == null && catalog.equipmentQuality === 0
    ? { ...frame, state: { ...frame.state, equipmentQuality: 0 } }
    : frame
}
/** Shared checkpoint-to-new-craft boundary; preserves sockets, hidden slots and quality. */
export async function freshSession(
  history: Films,
  source: Pick<Film, 'compatibilityId'>,
  frame: Frame,
  catalog: Initial,
  id: string,
  signal: AbortSignal,
  verify = false,
) {
  let state = frame.state,
    abyss = frame.abyss
  if (
    verify ||
    frame.rulesetIdentity !== catalog.rulesetIdentity ||
    !verifiedHistoryState(state, catalog)
  ) {
    const checked = await checkSessionCompatibility(
      catalog,
      state,
      abyss,
      source.compatibilityId,
      signal,
    )
    if (!checked.compatible || !checked.state)
      throw Error('INCOMPATIBLE_SESSION')
    state = checked.state
    abyss = checked.abyss ?? undefined
  }
  signal.throwIfAborted()
  const next = startFilm(
    history,
    state,
    id,
    catalog.rulesetIdentity,
    undefined,
    catalog.compatibilityId,
  )
  if (abyss) currentFilm(next)!.frames[0]!.abyss = structuredClone(abyss)
  return next
}
