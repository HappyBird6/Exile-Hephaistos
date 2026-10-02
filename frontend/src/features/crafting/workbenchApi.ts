import { currencyActions, actionNames } from './craftingApi'
import type { Action, Bucket, Definition, Initial } from './craftingApi'

export type WorkbenchAction =
  | Action
  | `GREATER_${Exclude<Action, 'ANNULMENT'>}`
  | `PERFECT_${Exclude<Action, 'ANNULMENT'>}`
  | 'DIVINE'
  | 'ALCHEMY'
  | 'FRACTURING'
  | 'LESSER_ESSENCE_BODY'
  | 'ESSENCE_BODY'
  | 'GREATER_ESSENCE_BODY'
  | 'LESSER_ESSENCE_MIND'
  | 'ESSENCE_MIND'
  | 'GREATER_ESSENCE_MIND'
  | 'LESSER_ESSENCE_RUIN'
  | 'ESSENCE_RUIN'
  | 'GREATER_ESSENCE_RUIN'
  | 'LESSER_ESSENCE_INFINITE'
  | 'ESSENCE_INFINITE'
  | 'GREATER_ESSENCE_INFINITE'
  | 'LESSER_ESSENCE_INSULATION'
  | 'ESSENCE_INSULATION'
  | 'GREATER_ESSENCE_INSULATION'
  | 'LESSER_ESSENCE_THAWING'
  | 'ESSENCE_THAWING'
  | 'GREATER_ESSENCE_THAWING'
  | 'LESSER_ESSENCE_GROUNDING'
  | 'ESSENCE_GROUNDING'
  | 'GREATER_ESSENCE_GROUNDING'
const fixedEssenceModifiers: Partial<Record<WorkbenchAction, string>> = {
  LESSER_ESSENCE_BODY: 'amulet:prefix:healthy',
  ESSENCE_BODY: 'amulet:prefix:robust',
  GREATER_ESSENCE_BODY: 'amulet:prefix:rotund',
  LESSER_ESSENCE_MIND: 'amulet:prefix:azure',
  ESSENCE_MIND: 'amulet:prefix:opalescent',
  GREATER_ESSENCE_MIND: 'amulet:prefix:gentian',
  LESSER_ESSENCE_RUIN: 'amulet:suffix:of-the-lost',
  ESSENCE_RUIN: 'amulet:suffix:of-banishment',
  GREATER_ESSENCE_RUIN: 'amulet:suffix:of-expulsion',
  LESSER_ESSENCE_INSULATION: 'amulet:suffix:of-the-salamander',
  ESSENCE_INSULATION: 'amulet:suffix:of-the-kiln',
  GREATER_ESSENCE_INSULATION: 'amulet:suffix:of-the-volcano',
  LESSER_ESSENCE_THAWING: 'amulet:suffix:of-the-penguin',
  ESSENCE_THAWING: 'amulet:suffix:of-the-yeti',
  GREATER_ESSENCE_THAWING: 'amulet:suffix:of-the-polar-bear',
  LESSER_ESSENCE_GROUNDING: 'amulet:suffix:of-the-squall',
  ESSENCE_GROUNDING: 'amulet:suffix:of-the-thunderhead',
  GREATER_ESSENCE_GROUNDING: 'amulet:suffix:of-the-maelstrom',
}
const choiceEssenceModifiers: Partial<
  Record<WorkbenchAction, readonly string[]>
> = {
  LESSER_ESSENCE_INFINITE: [
    'amulet:suffix:of-the-wrestler',
    'amulet:suffix:of-the-lynx',
    'amulet:suffix:of-the-student',
  ],
  ESSENCE_INFINITE: [
    'amulet:suffix:of-the-lion',
    'amulet:suffix:of-the-falcon',
    'amulet:suffix:of-the-augur',
  ],
  GREATER_ESSENCE_INFINITE: [
    'amulet:suffix:of-the-goliath',
    'amulet:suffix:of-the-leopard',
    'amulet:suffix:of-the-sage',
  ],
}
export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {
  ...currencyActions,
  Divine_Orb: 'DIVINE',
  Orb_of_Alchemy: 'ALCHEMY',
  Fracturing_Orb: 'FRACTURING',
  Lesser_Essence_of_the_Body: 'LESSER_ESSENCE_BODY',
  Essence_of_the_Body: 'ESSENCE_BODY',
  Greater_Essence_of_the_Body: 'GREATER_ESSENCE_BODY',
  Lesser_Essence_of_the_Mind: 'LESSER_ESSENCE_MIND',
  Essence_of_the_Mind: 'ESSENCE_MIND',
  Greater_Essence_of_the_Mind: 'GREATER_ESSENCE_MIND',
  Lesser_Essence_of_Ruin: 'LESSER_ESSENCE_RUIN',
  Essence_of_Ruin: 'ESSENCE_RUIN',
  Greater_Essence_of_Ruin: 'GREATER_ESSENCE_RUIN',
  Lesser_Essence_of_the_Infinite: 'LESSER_ESSENCE_INFINITE',
  Essence_of_the_Infinite: 'ESSENCE_INFINITE',
  Greater_Essence_of_the_Infinite: 'GREATER_ESSENCE_INFINITE',
  Lesser_Essence_of_Insulation: 'LESSER_ESSENCE_INSULATION',
  Essence_of_Insulation: 'ESSENCE_INSULATION',
  Greater_Essence_of_Insulation: 'GREATER_ESSENCE_INSULATION',
  Lesser_Essence_of_Thawing: 'LESSER_ESSENCE_THAWING',
  Essence_of_Thawing: 'ESSENCE_THAWING',
  Greater_Essence_of_Thawing: 'GREATER_ESSENCE_THAWING',
  Lesser_Essence_of_Grounding: 'LESSER_ESSENCE_GROUNDING',
  Essence_of_Grounding: 'ESSENCE_GROUNDING',
  Greater_Essence_of_Grounding: 'GREATER_ESSENCE_GROUNDING',
}
export const workbenchActionNames: Record<WorkbenchAction, string> = {
  ...actionNames,
  DIVINE: 'Divine Orb',
  ALCHEMY: 'Orb of Alchemy',
  FRACTURING: 'Fracturing Orb',
  LESSER_ESSENCE_BODY: 'Lesser Essence of the Body',
  ESSENCE_BODY: 'Essence of the Body',
  GREATER_ESSENCE_BODY: 'Greater Essence of the Body',
  LESSER_ESSENCE_MIND: 'Lesser Essence of the Mind',
  ESSENCE_MIND: 'Essence of the Mind',
  GREATER_ESSENCE_MIND: 'Greater Essence of the Mind',
  LESSER_ESSENCE_RUIN: 'Lesser Essence of Ruin',
  ESSENCE_RUIN: 'Essence of Ruin',
  GREATER_ESSENCE_RUIN: 'Greater Essence of Ruin',
  LESSER_ESSENCE_INFINITE: 'Lesser Essence of the Infinite',
  ESSENCE_INFINITE: 'Essence of the Infinite',
  GREATER_ESSENCE_INFINITE: 'Greater Essence of the Infinite',
  LESSER_ESSENCE_INSULATION: 'Lesser Essence of Insulation',
  ESSENCE_INSULATION: 'Essence of Insulation',
  GREATER_ESSENCE_INSULATION: 'Greater Essence of Insulation',
  LESSER_ESSENCE_THAWING: 'Lesser Essence of Thawing',
  ESSENCE_THAWING: 'Essence of Thawing',
  GREATER_ESSENCE_THAWING: 'Greater Essence of Thawing',
  LESSER_ESSENCE_GROUNDING: 'Lesser Essence of Grounding',
  ESSENCE_GROUNDING: 'Essence of Grounding',
  GREATER_ESSENCE_GROUNDING: 'Greater Essence of Grounding',
} as Record<WorkbenchAction, string>
for (const [id, base] of Object.entries(currencyActions)) {
  if (base === 'ANNULMENT') continue
  for (const tier of ['Greater', 'Perfect'] as const) {
    const action = `${tier.toUpperCase()}_${base}` as WorkbenchAction
    workbenchCurrencyActions[`${tier}_${id}`] = action
    workbenchActionNames[action] = `${tier} ${actionNames[base]}`
  }
}
export const workbenchOmens = [
  {
    id: 'Omen_of_Greater_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add two modifiers; requires two free slots in Workbench',
  },
  {
    id: 'Omen_of_Sinistral_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Exaltation',
    trigger: 'EXALTED',
    effect: 'Add only suffixes',
  },
  {
    id: 'Omen_of_Sinistral_Annulment',
    trigger: 'ANNULMENT',
    effect: 'Remove only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Annulment',
    trigger: 'ANNULMENT',
    effect: 'Remove only suffixes',
  },
  {
    id: 'Omen_of_Sinistral_Erasure',
    trigger: 'CHAOS',
    effect: 'Remove only prefixes',
  },
  {
    id: 'Omen_of_Dextral_Erasure',
    trigger: 'CHAOS',
    effect: 'Remove only suffixes',
  },
  {
    id: 'Omen_of_Whittling',
    trigger: 'CHAOS',
    effect: 'Remove the lowest modifier level; tied candidates are uniform',
  },
  {
    id: 'Omen_of_the_Blessed',
    trigger: 'DIVINE',
    effect: 'Reroll only implicit values',
  },
] as const
export const baseWorkbenchAction = (action: WorkbenchAction) =>
  action.replace(/^(GREATER|PERFECT)_/, '') as
    Action | 'DIVINE' | 'ALCHEMY' | 'FRACTURING'

export interface ConcreteItem extends Omit<Bucket, 'modifierIds'> {
  explicits: {
    modifierId: string
    values: Record<string, number>
    fractured?: boolean
  }[]
}
export interface MappingResult {
  mapped: boolean
  state: ConcreteItem | null
  issues: { lineNumber: number; message: string }[]
}
export async function mapSolarText(
  text: string,
  signal: AbortSignal,
  definitions: Record<string, Definition>,
): Promise<MappingResult> {
  const response = await fetch('/api/v1/crafting/workbench/map-text', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
  })
  if (!response.ok)
    throw new Error(
      'Could not validate this item for crafting. The displayed text is preserved.',
    )
  const v = (await response.json()) as MappingResult
  if (
    typeof v?.mapped !== 'boolean' ||
    !Array.isArray(v.issues) ||
    !v.issues.every(
      (i) => Number.isInteger(i.lineNumber) && typeof i.message === 'string',
    ) ||
    (v.mapped
      ? !v.state || !Array.isArray(v.state.explicits) || v.issues.length !== 0
      : v.state !== null)
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  if (
    v.state &&
    (typeof v.state.snapshotId !== 'string' ||
      typeof v.state.baseItemId !== 'string' ||
      !Number.isInteger(v.state.itemLevel) ||
      v.state.itemLevel < 1 ||
      v.state.itemLevel > 100 ||
      !['NORMAL', 'MAGIC', 'RARE'].includes(v.state.rarity) ||
      !Array.isArray(v.state.implicits) ||
      v.state.implicits.length !== 1 ||
      !Array.isArray(v.state.conditions) ||
      v.state.conditions.length !== 0 ||
      v.state.explicits.length > 6 ||
      ![...v.state.implicits, ...v.state.explicits].every(
        (m) =>
          m &&
          definitions[m.modifierId]?.stats &&
          typeof m.values === 'object' &&
          m.values !== null &&
          Object.keys(m.values).length ===
            definitions[m.modifierId]!.stats!.length &&
          definitions[m.modifierId]!.stats!.every(
            (s) =>
              Number.isSafeInteger(m.values[s.id]) &&
              m.values[s.id]! >= s.min &&
              m.values[s.id]! <= s.max,
          ),
      ))
  )
    throw new Error(
      'Could not verify the item mapping. The displayed text is preserved.',
    )
  return v
}
export interface RollAssumption {
  id: string
  candidateUnit: string
  n: number
  candidates: string[]
  min: number | null
  max: number | null
  sourceUrl: string
  reason: string
}
export interface AppliedItem {
  ruleVersion: string
  ledgerVersion: string
  snapshotId: string
  state: ConcreteItem
  action: WorkbenchAction
  applied: boolean
  reason: string
  events: {
    kind: 'ADD' | 'REMOVE' | 'REROLL_IMPLICIT' | 'REROLL_EXPLICIT' | 'FRACTURE'
    modifierId: string
    values: Record<string, number>
    selectionProbability: number
  }[]
  consumedOmens: string[]
  remainingOmens: string[]
  assumptions: RollAssumption[]
}
export function concreteInitial(initial: Initial): ConcreteItem {
  return {
    snapshotId: initial.state.snapshotId,
    baseItemId: initial.state.baseItemId,
    itemLevel: initial.state.itemLevel,
    rarity: initial.state.rarity,
    implicits: initial.state.implicits,
    conditions: initial.state.conditions,
    explicits: [],
  }
}
export async function applyCurrency(
  state: ConcreteItem,
  action: WorkbenchAction,
  definitions: Record<string, Definition>,
  signal: AbortSignal,
  activeOmens: string[] = [],
): Promise<AppliedItem> {
  const response = await fetch('/api/v1/crafting/workbench/apply', {
    method: 'POST',
    signal,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ state, action, activeOmens }),
  })
  if (!response.ok)
    throw new Error(
      'Could not apply currency. Your item is unchanged. Please retry.',
    )
  const v = (await response.json()) as AppliedItem
  const next = v?.state
  const baseAction = baseWorkbenchAction(action)
  const essenceCandidates = fixedEssenceModifiers[action]
    ? [fixedEssenceModifiers[action]!]
    : (choiceEssenceModifiers[action] ?? [])
  const sameModifiers = (
    a: ConcreteItem['explicits'],
    b: ConcreteItem['explicits'],
  ) =>
    Array.isArray(a) &&
    Array.isArray(b) &&
    a.length === b.length &&
    a.every(
      (m, i) =>
        m.modifierId === b[i]?.modifierId &&
        Boolean(m.fractured) === Boolean(b[i]?.fractured) &&
        JSON.stringify(m.values) === JSON.stringify(b[i]?.values),
    )
  const validValues = (values: Record<string, number>) =>
    values &&
    typeof values === 'object' &&
    Object.values(values).every(Number.isSafeInteger)
  if (
    !next ||
    v.action !== action ||
    typeof v.applied !== 'boolean' ||
    typeof v.reason !== 'string' ||
    typeof v.ruleVersion !== 'string' ||
    typeof v.ledgerVersion !== 'string' ||
    v.snapshotId !== state.snapshotId ||
    next.snapshotId !== state.snapshotId ||
    next.baseItemId !== state.baseItemId ||
    next.itemLevel !== state.itemLevel ||
    !['NORMAL', 'MAGIC', 'RARE'].includes(next.rarity) ||
    (action !== 'DIVINE' && !sameModifiers(next.implicits, state.implicits)) ||
    !Array.isArray(next.implicits) ||
    next.implicits.length !== 1 ||
    next.implicits.some((m) => m.fractured) ||
    next.implicits[0]?.modifierId !== state.implicits[0]?.modifierId ||
    !next.implicits.every((m) =>
      definitions[m.modifierId]?.stats?.every(
        (r) =>
          Number.isSafeInteger(m.values[r.id]) &&
          m.values[r.id]! >= r.min &&
          m.values[r.id]! <= r.max,
      ),
    ) ||
    !Array.isArray(next.conditions) ||
    next.conditions.length !== 0 ||
    !Array.isArray(next.explicits) ||
    next.explicits.length > 6 ||
    !next.explicits.every(
      (m) =>
        definitions[m.modifierId] &&
        validValues(m.values) &&
        (m.fractured === undefined || typeof m.fractured === 'boolean') &&
        definitions[m.modifierId]!.stats?.length ===
          Object.keys(m.values).length &&
        definitions[m.modifierId]!.stats?.every(
          (s) =>
            Number.isSafeInteger(m.values[s.id]) &&
            m.values[s.id]! >= s.min &&
            m.values[s.id]! <= s.max,
        ),
    ) ||
    !Array.isArray(v.events) ||
    !v.events.every(
      (e) =>
        [
          'ADD',
          'REMOVE',
          'REROLL_IMPLICIT',
          'REROLL_EXPLICIT',
          'FRACTURE',
        ].includes(e.kind) &&
        definitions[e.modifierId] &&
        validValues(e.values) &&
        Number.isFinite(e.selectionProbability) &&
        e.selectionProbability > 0 &&
        e.selectionProbability <= 1,
    ) ||
    !Array.isArray(v.consumedOmens) ||
    !Array.isArray(v.remainingOmens) ||
    [...v.consumedOmens, ...v.remainingOmens].length !== activeOmens.length ||
    new Set([...v.consumedOmens, ...v.remainingOmens]).size !==
      activeOmens.length ||
    ![...v.consumedOmens, ...v.remainingOmens].every((id) =>
      activeOmens.includes(id),
    ) ||
    (!v.applied && v.consumedOmens.length !== 0) ||
    !Array.isArray(v.assumptions) ||
    !v.assumptions.every(
      (a) =>
        typeof a.id === 'string' &&
        typeof a.candidateUnit === 'string' &&
        Number.isSafeInteger(a.n) &&
        a.n > 0 &&
        Array.isArray(a.candidates) &&
        a.candidates.every((c) => typeof c === 'string') &&
        typeof a.reason === 'string' &&
        typeof a.sourceUrl === 'string',
    )
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const families = new Set<string>()
  let prefixes = 0
  let suffixes = 0
  for (const instance of next.explicits) {
    const d = definitions[instance.modifierId]!
    if (d.familyIds.some((id) => families.has(id)) || d.affixType === 'NONE')
      throw new Error(
        'Could not verify the applied item. Your item is unchanged. Please retry.',
      )
    d.familyIds.forEach((id) => families.add(id))
    if (d.affixType === 'PREFIX') prefixes++
    else suffixes++
  }
  const capacity =
    next.rarity === 'NORMAL' ? 0 : next.rarity === 'MAGIC' ? 1 : 3
  const expectedRarity =
    baseAction === 'TRANSMUTATION'
      ? 'MAGIC'
      : baseAction === 'REGAL' ||
          action === 'ALCHEMY' ||
          essenceCandidates.length > 0
        ? 'RARE'
        : state.rarity
  const expectedCount =
    action === 'ALCHEMY'
      ? 4
      : state.explicits.length +
        (baseAction === 'ANNULMENT'
          ? -1
          : baseAction === 'CHAOS' ||
              action === 'DIVINE' ||
              action === 'FRACTURING'
            ? 0
            : action === 'EXALTED' &&
                activeOmens.includes('Omen_of_Greater_Exaltation')
              ? 2
              : 1)
  if (
    prefixes > capacity ||
    suffixes > capacity ||
    (v.applied &&
      (next.rarity !== expectedRarity ||
        next.explicits.length !== expectedCount)) ||
    (!v.applied &&
      (!sameModifiers(next.explicits, state.explicits) ||
        next.rarity !== state.rarity ||
        !sameModifiers(next.implicits, state.implicits) ||
        v.events.length !== 0 ||
        v.assumptions.length !== 0))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  if (
    action === 'DIVINE' &&
    (next.explicits.map((m) => m.modifierId).join('|') !==
      state.explicits.map((m) => m.modifierId).join('|') ||
      (activeOmens.includes('Omen_of_the_Blessed') &&
        !sameModifiers(next.explicits, state.explicits)))
  )
    throw new Error(
      'Could not verify the applied item. Your item is unchanged. Please retry.',
    )
  const locks = next.explicits.filter((m) => m.fractured)
  if (
    v.applied &&
    essenceCandidates.length > 0 &&
    (state.rarity !== 'MAGIC' ||
      v.events.length !== 1 ||
      v.events[0]?.kind !== 'ADD' ||
      !essenceCandidates.includes(v.events[0]?.modifierId ?? '') ||
      v.events[0]?.selectionProbability !== 1 / essenceCandidates.length ||
      (essenceCandidates.length > 1 &&
        !v.assumptions.some(
          (a) =>
            a.id === 'uniform-essence-choice-v1' &&
            a.n === essenceCandidates.length &&
            a.candidates.length === essenceCandidates.length &&
            new Set(a.candidates).size === essenceCandidates.length &&
            a.candidates.every((id) => essenceCandidates.includes(id)),
        )) ||
      v.consumedOmens.length !== 0 ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      JSON.stringify(v.events[0]?.values) !==
        JSON.stringify(
          next.explicits.find((m) => m.modifierId === v.events[0]?.modifierId)
            ?.values,
        ))
  )
    throw new Error(
      'Could not verify the guaranteed essence modifier. Your item is unchanged. Please retry.',
    )
  if (
    v.applied &&
    action === 'EXALTED' &&
    activeOmens.includes('Omen_of_Greater_Exaltation') &&
    (state.rarity !== 'RARE' ||
      state.explicits.length > 4 ||
      !sameModifiers(
        state.explicits
          .map((old) =>
            next.explicits.find((m) => m.modifierId === old.modifierId)!,
          )
          .filter(Boolean),
        state.explicits,
      ) ||
      v.events.length !== 2 ||
      new Set(v.events.map((event) => event.modifierId)).size !== 2 ||
      v.events.some(
        (event) =>
          event.kind !== 'ADD' ||
          state.explicits.some((old) => old.modifierId === event.modifierId) ||
          JSON.stringify(event.values) !==
            JSON.stringify(
              next.explicits.find((m) => m.modifierId === event.modifierId)
                ?.values,
            ),
      ) ||
      !v.consumedOmens.includes('Omen_of_Greater_Exaltation'))
  )
    throw new Error(
      'Could not verify both added modifiers. Your item is unchanged. Please retry.',
    )
  const previousLocks = state.explicits.filter((m) => m.fractured)
  if (
    locks.length > 1 ||
    (locks.length && next.rarity !== 'RARE') ||
    previousLocks.some(
      (locked) =>
        !next.explicits.some(
          (m) =>
            m.modifierId === locked.modifierId &&
            m.fractured &&
            JSON.stringify(m.values) === JSON.stringify(locked.values),
        ),
    ) ||
    (v.applied &&
      action === 'FRACTURING' &&
      (locks.length !== 1 ||
        previousLocks.length !== 0 ||
        state.rarity !== 'RARE' ||
        state.explicits.length < 4 ||
        next.explicits.some(
          (m, i) =>
            m.modifierId !== state.explicits[i]?.modifierId ||
            JSON.stringify(m.values) !==
              JSON.stringify(state.explicits[i]?.values),
        ) ||
        v.events.length !== 1 ||
        v.events[0]?.kind !== 'FRACTURE' ||
        v.events[0]?.modifierId !== locks[0]?.modifierId ||
        v.events[0]?.selectionProbability !== 1 / state.explicits.length)) ||
    (action !== 'FRACTURING' && locks.length !== previousLocks.length)
  )
    throw new Error(
      'Could not verify the Fractured modifier. Your item is unchanged. Please retry.',
    )
  return v
}

export function rolledText(
  definition: Definition,
  values: Record<string, number>,
): string {
  const stat = definition.stats?.length === 1 ? definition.stats[0] : undefined
  const range = /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013?-]\s*(-?\d+(?:\.\d+)?)\)/
  const match = definition.text.match(range)
  if (
    stat &&
    match &&
    Number(match[1]) === stat.min &&
    Number(match[2]) === stat.max
  )
    return definition.text.replace(range, String(values[stat.id]))
  if (stat && stat.min === stat.max) return definition.text
  return `${definition.name}: ${Object.entries(values)
    .map(
      ([id, value]) => `${id.replaceAll('_', ' ')} = ${value} (source units)`,
    )
    .join(', ')}`
}
