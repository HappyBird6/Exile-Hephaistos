import { supportsConcreteStateShape } from './workbenchStateShape'
import { isWorkbenchJewel, reviewedBasicJewel } from './basicJewel'
import { verifiedCatalystQuality } from './catalystQuality'
import { maximumQuality, qualityLimitMatches } from './qualityLimit'
import type { Initial } from './craftingApi'
import type { AppliedItem, ConcreteItem } from './workbenchApi'
import { coupledModelsMatch, workbenchActionNames } from './workbenchApi'
import { isRulesetIdentity } from './rulesetIdentity'

export const historyStorageKey = 'hephaistos.workbench.films.v1'
export const historyStorageLimit = 2_000_000
export type CraftEvidence = Pick<
  AppliedItem,
  | 'snapshotId'
  | 'action'
  | 'ruleVersion'
  | 'ledgerVersion'
  | 'events'
  | 'assumptions'
  | 'consumedOmens'
  | 'qualityLimit'
  | 'rulesetIdentity'
>
export type Frame = {
  rulesetIdentity?: string
  state: ConcreteItem
  action: string | null
  evidence?: CraftEvidence
}
export type Film = {
  id: string
  createdAt: string
  frames: Frame[]
  rulesetIdentity?: string
}
export type FilmRulesetStatus =
  'current' | 'mismatch' | 'legacy-unverified' | 'inconsistent'

export function filmRulesetStatus(
  film: Film,
  identity: string,
): FilmRulesetStatus {
  const identities: unknown[] = [film.rulesetIdentity]
  for (const [index, frame] of film.frames.entries()) {
    identities.push(frame.rulesetIdentity)
    if (index > 0 || frame.evidence)
      identities.push(frame.evidence?.rulesetIdentity)
  }
  const present = identities.filter((value) => value !== undefined)
  if (
    present.some((value) => !isRulesetIdentity(value)) ||
    new Set(present).size > 1
  )
    return 'inconsistent'
  if (present.length !== identities.length) return 'legacy-unverified'
  return isRulesetIdentity(identity) && film.rulesetIdentity === identity
    ? 'current'
    : 'mismatch'
}
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
              frame.state !== null &&
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
    // Preserve the exact pre-boundary bytes before the first write, including selection changes.
    const archiveKey = historyStorageKey + '.preserved'
    const previous = this.storage.getItem(historyStorageKey)
    if (previous !== null && this.storage.getItem(archiveKey) === null)
      this.storage.setItem(archiveKey, previous)
    this.storage.setItem(historyStorageKey, raw)
  }
}

export function startFilm(
  films: Films,
  root: ConcreteItem,
  id: string,
  rulesetIdentity: string,
): Films {
  if (!isRulesetIdentity(rulesetIdentity))
    throw new Error('Ruleset identity is required.')
  return {
    version: 1,
    films: [
      ...films.films,
      {
        id,
        rulesetIdentity,
        createdAt: new Date().toISOString(),
        frames: [
          { state: structuredClone(root), action: null, rulesetIdentity },
        ],
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
    !isRulesetIdentity(result.rulesetIdentity) ||
    (film && filmRulesetStatus(film, result.rulesetIdentity) !== 'current')
  )
    throw new Error(
      'Saved ruleset is unverified or inconsistent. Start a new craft; the original record is preserved.',
    )
  if (
    !film ||
    next.cursor !== film.frames.length - 1 ||
    JSON.stringify(currentFrame(next)?.state) !== JSON.stringify(before)
  )
    next = startFilm(next, before, id, result.rulesetIdentity)
  const active = currentFilm(next)!
  const frames = [
    ...active.frames,
    {
      state: structuredClone(result.state),
      rulesetIdentity: result.rulesetIdentity,
      action: result.action,
      evidence: structuredClone({
        snapshotId: result.snapshotId,
        rulesetIdentity: result.rulesetIdentity,
        action: result.action,
        ruleVersion: result.ruleVersion,
        ledgerVersion: result.ledgerVersion,
        events: result.events,
        assumptions: result.assumptions,
        consumedOmens: result.consumedOmens,
        ...(result.qualityLimit ? { qualityLimit: result.qualityLimit } : {}),
      }),
    },
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

// Invalid optional evidence does not discard item frames or overwrite existing storage.
export function verifiedFrameEvidence(
  frame: Frame,
  initial: Initial,
): CraftEvidence | undefined {
  try {
    const e = frame.evidence
    if (
      !e ||
      frame.rulesetIdentity !== initial.rulesetIdentity ||
      e.rulesetIdentity !== initial.rulesetIdentity ||
      (e.qualityLimit !== undefined &&
        (e.qualityLimit === null
          ? maximumQuality(frame.state, initial.modifiers) !== null
          : !qualityLimitMatches(
              e.qualityLimit,
              frame.state,
              initial.modifiers,
            ))) ||
      !verifiedHistoryState(frame.state, initial) ||
      e.action !== frame.action ||
      !Object.hasOwn(workbenchActionNames, e.action) ||
      (e.snapshotId !== frame.state.snapshotId &&
        !initial.compatibleSnapshotIds?.includes(e.snapshotId)) ||
      typeof e.ruleVersion !== 'string' ||
      !e.ruleVersion.trim() ||
      typeof e.ledgerVersion !== 'string' ||
      !e.ledgerVersion.trim() ||
      !Array.isArray(e.consumedOmens) ||
      !e.consumedOmens.every((id) => typeof id === 'string') ||
      !Array.isArray(e.events) ||
      !Array.isArray(e.assumptions)
    )
      return undefined
    const instances = [...frame.state.implicits, ...frame.state.explicits]
    if (
      !e.events.every((event) => {
        if (
          ![
            'ADD',
            'REMOVE',
            'REROLL_IMPLICIT',
            'REROLL_EXPLICIT',
            'FRACTURE',
          ].includes(event.kind) ||
          !initial.modifiers[event.modifierId] ||
          !event.values ||
          !Number.isFinite(event.selectionProbability) ||
          event.selectionProbability < 0 ||
          event.selectionProbability > 1 ||
          !Object.values(event.values).every(Number.isSafeInteger)
        )
          return false
        if (event.kind === 'REMOVE')
          return Object.keys(event.values).length === 0
        const item = instances.find((m) => m.modifierId === event.modifierId)
        return (
          !!item &&
          Object.keys(event.values).length ===
            Object.keys(item.values).length &&
          Object.entries(item.values).every(
            ([id, value]) => event.values[id] === value,
          )
        )
      }) ||
      !e.assumptions.every((a) => {
        if (
          typeof a.id !== 'string' ||
          typeof a.candidateUnit !== 'string' ||
          typeof a.reason !== 'string' ||
          !Number.isSafeInteger(a.n) ||
          a.n <= 0 ||
          !Array.isArray(a.candidates) ||
          !a.candidates.every((id) => typeof id === 'string') ||
          !(a.min === null || Number.isSafeInteger(a.min)) ||
          !(a.max === null || Number.isSafeInteger(a.max)) ||
          typeof a.sourceUrl !== 'string'
        )
          return false
        return ['https:', 'http:'].includes(new URL(a.sourceUrl).protocol)
      }) ||
      !coupledModelsMatch({ ...e, state: frame.state }, initial.modifiers)
    )
      return undefined
    return e
  } catch {
    return undefined
  }
}

// Stored concrete items are untrusted. Do not render or craft from them until the catalog agrees.
export function verifiedHistoryState(
  state: ConcreteItem,
  initial: Initial,
): boolean {
  try {
    if (
      !supportsConcreteStateShape(state) ||
      (isWorkbenchJewel(state.baseItemId) && !reviewedBasicJewel(state)) ||
      !verifiedCatalystQuality(state, initial.modifiers) ||
      (state.snapshotId !== initial.metadata.snapshotId &&
        !initial.compatibleSnapshotIds?.includes(state.snapshotId)) ||
      state.baseItemId !== initial.state.baseItemId ||
      !Number.isInteger(state.itemLevel) ||
      state.itemLevel < 1 ||
      state.itemLevel > 100 ||
      !['NORMAL', 'MAGIC', 'RARE'].includes(state.rarity) ||
      !Array.isArray(state.conditions) ||
      state.conditions.length ||
      !Array.isArray(state.implicits) ||
      state.implicits.length !== initial.state.implicits.length ||
      state.implicits.some((m) => m.fractured) ||
      state.implicits[0]?.modifierId !==
        initial.state.implicits[0]?.modifierId ||
      !Array.isArray(state.explicits) ||
      state.explicits.length > 6 ||
      state.explicits.filter((m) => m.fractured).length > 1 ||
      (state.explicits.some((m) => m.fractured) && state.rarity !== 'RARE')
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
        (index >= state.implicits.length &&
          instance.fractured !== undefined &&
          typeof instance.fractured !== 'boolean') ||
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
      if (index >= state.implicits.length) {
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
      state.rarity === 'NORMAL'
        ? 0
        : state.rarity === 'MAGIC'
          ? 1
          : isWorkbenchJewel(state.baseItemId)
            ? 2
            : 3
    return isWorkbenchJewel(state.baseItemId)
      ? reviewedBasicJewel(state)
      : p <= capacity && s <= capacity
  } catch {
    return false
  }
}
