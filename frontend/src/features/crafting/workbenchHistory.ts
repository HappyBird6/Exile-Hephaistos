import type { Initial } from './craftingApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'

export const historyStorageKey = 'hephaistos.workbench.films.v1'
export const historyStorageLimit = 2_000_000
export type Frame = { state: ConcreteItem; action: string | null }
export type Film = { id: string; createdAt: string; frames: Frame[] }
export type Films = {
  version: 1
  films: Film[]
  active: string | null
  cursor: number
}
export const emptyFilms = (): Films => ({
  version: 1,
  films: [],
  active: null,
  cursor: 0,
})
export const currentFilm = (films: Films) =>
  films.films.find((film) => film.id === films.active)
export const currentFrame = (films: Films) =>
  currentFilm(films)?.frames[films.cursor]

// A future per-user DB repository can replace this boundary without changing film semantics.
export interface FilmRepository {
  load(): Films
  save(films: Films): void
}
export class LocalFilmRepository implements FilmRepository {
  constructor(private storage: Pick<Storage, 'getItem' | 'setItem'>) {}
  load(): Films {
    const raw = this.storage.getItem(historyStorageKey)
    if (!raw) return emptyFilms()
    if (raw.length * 2 > historyStorageLimit)
      throw new Error('Saved history exceeds the supported size.')
    const value = JSON.parse(raw) as Films
    if (
      value?.version !== 1 ||
      !Array.isArray(value.films) ||
      !Number.isInteger(value.cursor) ||
      value.cursor < 0 ||
      !value.films.every(
        (film) =>
          typeof film.id === 'string' &&
          typeof film.createdAt === 'string' &&
          Array.isArray(film.frames) &&
          film.frames.length > 0 &&
          film.frames.every(
            (frame) =>
              frame &&
              typeof frame.state === 'object' &&
              (frame.action === null || typeof frame.action === 'string'),
          ),
      ) ||
      new Set(value.films.map((film) => film.id)).size !== value.films.length ||
      (value.active !== null && (!currentFilm(value) || !currentFrame(value)))
    )
      throw new Error('Saved history is invalid. It has not been overwritten.')
    return value
  }
  save(films: Films) {
    const raw = JSON.stringify(films)
    if (raw.length * 2 > historyStorageLimit)
      throw new Error('History storage limit reached.')
    this.storage.setItem(historyStorageKey, raw)
  }
}

export function startFilm(films: Films, root: ConcreteItem, id: string): Films {
  return {
    version: 1,
    films: [
      ...films.films,
      {
        id,
        createdAt: new Date().toISOString(),
        frames: [{ state: root, action: null }],
      },
    ],
    active: id,
    cursor: 0,
  }
}
export function recordCraft(
  films: Films,
  before: ConcreteItem,
  result: AppliedItem,
  id: string,
): Films {
  if (!result.applied) return films
  let next = films
  const film = currentFilm(next)
  if (
    !film ||
    next.cursor !== film.frames.length - 1 ||
    JSON.stringify(currentFrame(next)?.state) !== JSON.stringify(before)
  )
    next = startFilm(next, before, id)
  const active = currentFilm(next)!
  const frames = [
    ...active.frames,
    { state: result.state, action: result.action },
  ]
  return {
    ...next,
    films: next.films.map((entry) =>
      entry.id === active.id ? { ...entry, frames } : entry,
    ),
    cursor: frames.length - 1,
  }
}
export function viewFrame(films: Films, cursor: number): Films {
  const film = currentFilm(films)
  return film &&
    Number.isInteger(cursor) &&
    cursor >= 0 &&
    cursor < film.frames.length
    ? { ...films, cursor }
    : films
}

// Stored concrete items are untrusted. Do not render or craft from them until the catalog agrees.
export function verifiedHistoryState(
  state: ConcreteItem,
  initial: Initial,
): boolean {
  try {
    if (
      state.snapshotId !== initial.metadata.snapshotId ||
      state.baseItemId !== initial.state.baseItemId ||
      !Number.isInteger(state.itemLevel) ||
      state.itemLevel < 1 ||
      state.itemLevel > 100 ||
      !['NORMAL', 'MAGIC', 'RARE'].includes(state.rarity) ||
      !Array.isArray(state.conditions) ||
      state.conditions.length ||
      !Array.isArray(state.implicits) ||
      state.implicits.length !== 1 ||
      state.implicits[0]?.modifierId !==
        initial.state.implicits[0]?.modifierId ||
      !Array.isArray(state.explicits) ||
      state.explicits.length > 6
    )
      return false
    const families = new Set<string>()
    let p = 0,
      s = 0
    for (const [index, instance] of [
      ...state.implicits,
      ...state.explicits,
    ].entries()) {
      const definition = initial.modifiers[instance.modifierId]
      if (
        !definition?.stats ||
        !instance.values ||
        Object.keys(instance.values).length !== definition.stats.length ||
        !definition.stats.every(
          (stat) =>
            Number.isSafeInteger(instance.values[stat.id]) &&
            instance.values[stat.id]! >= stat.min &&
            instance.values[stat.id]! <= stat.max,
        )
      )
        return false
      if (index) {
        if (
          definition.affixType === 'NONE' ||
          definition.familyIds.some((id) => families.has(id))
        )
          return false
        definition.familyIds.forEach((id) => families.add(id))
        if (definition.affixType === 'PREFIX') p++
        else s++
      }
    }
    const capacity =
      state.rarity === 'NORMAL' ? 0 : state.rarity === 'MAGIC' ? 1 : 3
    return p <= capacity && s <= capacity
  } catch {
    return false
  }
}
