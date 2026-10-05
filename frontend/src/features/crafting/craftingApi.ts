import { topBase } from './topBases'
export const actions = [
  'TRANSMUTATION',
  'AUGMENTATION',
  'REGAL',
  'EXALTED',
  'ANNULMENT',
  'CHAOS',
] as const
export type Action = (typeof actions)[number]
export const actionNames: Record<Action, string> = {
  TRANSMUTATION: 'Orb of Transmutation',
  AUGMENTATION: 'Orb of Augmentation',
  REGAL: 'Regal Orb',
  EXALTED: 'Exalted Orb',
  ANNULMENT: 'Orb of Annulment',
  CHAOS: 'Chaos Orb',
}
export const currencyActions: Record<string, Action> = {
  Orb_of_Transmutation: 'TRANSMUTATION',
  Orb_of_Augmentation: 'AUGMENTATION',
  Regal_Orb: 'REGAL',
  Exalted_Orb: 'EXALTED',
  Orb_of_Annulment: 'ANNULMENT',
  Chaos_Orb: 'CHAOS',
}
export interface Bucket {
  snapshotId: string
  baseItemId: string
  itemLevel: number
  rarity: 'NORMAL' | 'MAGIC' | 'RARE'
  implicits: {
    modifierId: string
    values: Record<string, number>
    fractured?: boolean
  }[]
  modifierIds: string[]
  conditions: string[]
}
export interface Definition {
  requiredItemLevel?: number
  weight?: number
  id: string
  name: string
  text: string
  tier: number
  affixType: 'NONE' | 'PREFIX' | 'SUFFIX'
  familyIds: string[]
  layer?: string
  stats?: { id: string; min: number; max: number }[]
  tags?: string[]
}
export interface Availability {
  action: Action
  available: boolean
  reason: string
}
export interface Initial {
  augmentSockets?: number | null | undefined
  compatibleSnapshotIds?: string[]
  ruleVersion: string
  metadata: { snapshotId: string; retrievedAt: string; sourceUrl: string }
  id: string
  state: Bucket
  modifiers: Record<string, Definition>
  actions: Availability[]
}
export interface Outcome {
  id: string
  state: Bucket
  probability: number
}
export interface Transition {
  fromId: string
  action: Action
  available: boolean
  reason: string
  outcomes: Outcome[]
}
export interface Exploration {
  nodes: Record<string, Bucket>
  edges: {
    fromId: string
    toId: string
    step: number
    action: Action
    probability: number
  }[]
  terminals: {
    id: string
    step: number
    status: 'COMPLETE' | 'BLOCKED' | 'DEFERRED'
    probability: number
  }[]
  completedProbability: number
  blockedProbability: number
  unexploredProbability: number
  complete: boolean
}
const object = (v: unknown): v is Record<string, unknown> =>
  typeof v === 'object' && v !== null && !Array.isArray(v)
const strings = (v: unknown): v is string[] =>
  Array.isArray(v) && v.every((x) => typeof x === 'string')
const probability = (v: unknown): v is number =>
  typeof v === 'number' && Number.isFinite(v) && v >= 0 && v <= 1 + 1e-10
const action = (v: unknown): v is Action => actions.some((a) => a === v)
const bucket = (v: unknown): v is Bucket =>
  object(v) &&
  typeof v.snapshotId === 'string' &&
  typeof v.baseItemId === 'string' &&
  Number.isInteger(v.itemLevel) &&
  Number(v.itemLevel) >= 1 &&
  Number(v.itemLevel) <= 100 &&
  ['NORMAL', 'MAGIC', 'RARE'].includes(String(v.rarity)) &&
  strings(v.modifierIds) &&
  v.modifierIds.length <= 6 &&
  strings(v.conditions) &&
  Array.isArray(v.implicits) &&
  v.implicits.length <= 1 &&
  v.implicits.every(
    (m) =>
      object(m) &&
      typeof m.modifierId === 'string' &&
      object(m.values) &&
      Object.values(m.values).every(Number.isSafeInteger),
  )
function availability(v: unknown): v is Availability[] {
  return (
    Array.isArray(v) &&
    v.length === 6 &&
    new Set(v.map((x) => (object(x) ? x.action : null))).size === 6 &&
    v.every(
      (x) =>
        object(x) &&
        action(x.action) &&
        typeof x.available === 'boolean' &&
        typeof x.reason === 'string',
    )
  )
}
async function request(
  path: string,
  signal: AbortSignal,
  body?: unknown,
): Promise<unknown> {
  const response = await fetch(`/api/v1/crafting/${path}`, {
    signal,
    ...(body === undefined
      ? {}
      : {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(body),
        }),
  })
  if (!response.ok)
    throw new Error(
      response.status === 422
        ? 'This item state or exploration request is not supported.'
        : 'Could not load crafting results. Please retry.',
    )
  return response.json()
}
function invalid(): never {
  throw new Error('Could not verify the crafting response. Please retry.')
}
function reviewedImplicitMatches(base: string, state: Bucket): boolean {
  const reviewed = topBase(base)
  const implicit = state.implicits[0]
  return reviewed?.implicitModifierId
    ? state.implicits.length === 1 &&
        implicit?.modifierId === reviewed.implicitModifierId &&
        implicit.fractured !== true &&
        Object.keys(implicit.values).length ===
          reviewed.implicitStats!.length &&
        reviewed.implicitStats!.every(
          (stat) => implicit.values[stat.id] === stat.max,
        )
    : state.implicits.length === 0
}
export async function loadInitial(
  level: number,
  signal: AbortSignal,
  base:
    | 'solar'
    | 'stocky'
    | 'bow'
    | 'wand'
    | 'body'
    | 'soldier'
    | 'imperial'
    | 'massive'
    | 'sirenscale'
    | 'adherent'
    | 'slipstrike'
    | 'death-mail'
    | 'sleek'
    | 'vile'
    | 'wolfskin'
    | 'ancestral'
    | 'cryptic'
    | 'tasalian'
    | 'drakeskin'
    | 'sekhema'
    | 'blacksteel-boots'
    | 'faithful'
    | 'daggerfoot'
    | 'warmonger'
    | 'guardian'
    | 'gemini'
    | 'fanatic'
    | 'obliterator'
    | 'hallowed'
    | 'bone'
    | 'siphoning'
    | 'volatile'
    | 'galvanic'
    | 'acrid'
    | 'offering'
    | 'critical'
    | 'primordial'
    | 'dueling'
    | 'stellar'
    | 'amber'
    | 'bloodstone'
    | 'lunar'
    | 'azure'
    | 'crimson'
    | 'pearlescent'
    | 'kinetic'
    | 'vitalic'
    | 'mnemonic'
    | 'pearl'
    | 'amethyst'
    | 'prismatic'
    | 'ruby-ring'
    | 'two-stone-fire-cold'
    | 'freebooter'
    | 'gladiatorial'
    | 'grinning'
    | 'polished'
    | 'blacksteel-gloves'
    | 'war-wraps'
    | 'sceptre'
    | 'belt'
    | 'helmet'
    | 'ring'
    | 'time-lost-ruby'
    | 'time-lost-emerald'
    | 'time-lost-sapphire'
    | 'time-lost-diamond'
    | 'ruby'
    | 'emerald'
    | 'diamond'
    | 'sapphire' = 'solar',
): Promise<Initial> {
  const v = await request(
    base === 'solar'
      ? `initial?itemLevel=${level}`
      : `workbench/initial?base=${base}&itemLevel=${level}`,
    signal,
  )
  if (
    !object(v) ||
    !bucket(v.state) ||
    (base !== 'solar'
      ? v.state.baseItemId !==
          (topBase(base)?.id ??
            ([
              'ruby',
              'emerald',
              'diamond',
              'time-lost-ruby',
              'time-lost-emerald',
              'time-lost-sapphire',
              'time-lost-diamond',
            ].includes(base)
              ? (
                  {
                    'time-lost-ruby': 'Metadata/Items/Jewels/JewelRadiusStr',
                    'time-lost-emerald': 'Metadata/Items/Jewels/JewelRadiusDex',
                    'time-lost-sapphire':
                      'Metadata/Items/Jewels/JewelRadiusInt',
                    'time-lost-diamond':
                      'Metadata/Items/Jewels/JewelRadiusDiamond',
                    ruby: 'Metadata/Items/Jewels/JewelStr',
                    emerald: 'Metadata/Items/Jewels/JewelDex',
                    diamond: 'Metadata/Items/Jewels/JewelDiamond',
                  } as Record<string, string>
                )[base]
              : base === 'sapphire'
                ? 'Metadata/Items/Jewels/JewelInt'
                : base === 'stocky'
                  ? 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
                  : base === 'ring'
                    ? 'Metadata/Items/Rings/FourRing1'
                    : base === 'helmet'
                      ? 'Metadata/Items/Armours/Helmets/FourHelmetStr1'
                      : base === 'belt'
                        ? 'Metadata/Items/Belts/FourBelt1'
                        : base === 'sceptre'
                          ? 'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
                          : base === 'body'
                            ? 'Metadata/Items/Armours/BodyArmours/FourBodyStr1'
                            : base === 'wand'
                              ? 'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
                              : 'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1')) ||
        (base === 'ring'
          ? v.state.implicits.length !== 1 ||
            v.state.implicits[0]?.modifierId !==
              'iron-ring:implicit:added-physical-damage-to-attacks' ||
            Object.keys(v.state.implicits[0].values).length !== 2 ||
            v.state.implicits[0].values.attack_minimum_added_physical_damage !==
              1 ||
            v.state.implicits[0].values.attack_maximum_added_physical_damage !==
              4 ||
            v.state.implicits[0].fractured === true
          : base.startsWith('time-lost-')
            ? v.state.implicits.length !== 1 ||
              v.state.implicits[0]?.modifierId !==
                base + ':implicit:base-radius' ||
              Object.keys(v.state.implicits[0]?.values ?? {}).length !== 1 ||
              v.state.implicits[0]?.values.local_jewel_effect_base_radius !==
                1000 ||
              v.state.implicits[0]?.fractured === true
            : !reviewedImplicitMatches(base, v.state))
      : v.state.implicits.length !== 1) ||
    v.state.itemLevel !== level ||
    !object(v.metadata) ||
    typeof v.metadata.sourceUrl !== 'string' ||
    typeof v.metadata.retrievedAt !== 'string' ||
    typeof v.metadata.snapshotId !== 'string' ||
    v.state.snapshotId !== v.metadata.snapshotId ||
    typeof v.id !== 'string' ||
    typeof v.ruleVersion !== 'string' ||
    (v.augmentSockets !== undefined &&
      v.augmentSockets !== null &&
      (base !== 'stocky' || v.augmentSockets !== 0)) ||
    !object(v.modifiers) ||
    !availability(v.actions) ||
    (v.compatibleSnapshotIds !== undefined && !strings(v.compatibleSnapshotIds))
  )
    return invalid()
  if (
    !Object.values(v.modifiers).every(
      (d) =>
        object(d) &&
        typeof d.id === 'string' &&
        typeof d.text === 'string' &&
        typeof d.name === 'string' &&
        Number.isInteger(d.tier) &&
        strings(d.familyIds) &&
        (d.layer === undefined || typeof d.layer === 'string') &&
        (d.tags === undefined || strings(d.tags)) &&
        (d.stats === undefined ||
          (Array.isArray(d.stats) &&
            d.stats.every(
              (s) =>
                object(s) &&
                typeof s.id === 'string' &&
                Number.isFinite(s.min) &&
                Number.isFinite(s.max),
            ))) &&
        ['NONE', 'PREFIX', 'SUFFIX'].includes(String(d.affixType)),
    )
  )
    return invalid()
  return v as unknown as Initial
}
export async function loadActions(
  state: Bucket,
  signal: AbortSignal,
): Promise<Availability[]> {
  const v = await request('actions', signal, state)
  return availability(v) ? v : invalid()
}
export async function loadTransition(
  state: Bucket,
  selected: Action,
  signal: AbortSignal,
): Promise<Transition> {
  const v = await request('transitions', signal, { state, action: selected })
  if (
    !object(v) ||
    typeof v.fromId !== 'string' ||
    v.action !== selected ||
    typeof v.available !== 'boolean' ||
    typeof v.reason !== 'string' ||
    !Array.isArray(v.outcomes) ||
    !v.outcomes.every(
      (o) =>
        object(o) &&
        typeof o.id === 'string' &&
        bucket(o.state) &&
        o.state.snapshotId === state.snapshotId &&
        o.state.baseItemId === state.baseItemId &&
        o.state.itemLevel === state.itemLevel &&
        probability(o.probability),
    )
  )
    return invalid()
  const outcomes = v.outcomes as Outcome[]
  if (
    v.available
      ? Math.abs(outcomes.reduce((sum, o) => sum + o.probability, 0) - 1) > 1e-9
      : outcomes.length !== 0
  )
    return invalid()
  return v as unknown as Transition
}
export async function explore(
  state: Bucket,
  plan: Action[],
  signal: AbortSignal,
): Promise<Exploration> {
  const v = await request('explore', signal, {
    state,
    plan,
    maxNodes: 2000,
    maxEdges: 10000,
    maxMillis: 1000,
  })
  if (
    !object(v) ||
    !object(v.nodes) ||
    !Object.values(v.nodes).every(
      (n) =>
        bucket(n) &&
        n.snapshotId === state.snapshotId &&
        n.baseItemId === state.baseItemId &&
        n.itemLevel === state.itemLevel,
    ) ||
    !Array.isArray(v.edges) ||
    !Array.isArray(v.terminals) ||
    typeof v.complete !== 'boolean' ||
    !probability(v.completedProbability) ||
    !probability(v.blockedProbability) ||
    !probability(v.unexploredProbability) ||
    Math.abs(
      v.completedProbability +
        v.blockedProbability +
        v.unexploredProbability -
        1,
    ) > 1e-9
  )
    return invalid()
  const nodes = v.nodes
  if (
    !v.terminals.every(
      (t) =>
        object(t) &&
        typeof t.id === 'string' &&
        nodes[t.id] &&
        probability(t.probability) &&
        Number.isInteger(t.step) &&
        ['COMPLETE', 'BLOCKED', 'DEFERRED'].includes(String(t.status)),
    )
  )
    return invalid()
  if (
    !v.edges.every(
      (e) =>
        object(e) &&
        typeof e.fromId === 'string' &&
        typeof e.toId === 'string' &&
        nodes[e.fromId] &&
        nodes[e.toId] &&
        Number.isInteger(e.step) &&
        action(e.action) &&
        probability(e.probability),
    )
  )
    return invalid()
  const terminals = v.terminals as Exploration['terminals']
  for (const [status, expected] of [
    ['COMPLETE', v.completedProbability],
    ['BLOCKED', v.blockedProbability],
    ['DEFERRED', v.unexploredProbability],
  ] as const) {
    if (
      Math.abs(
        terminals
          .filter((t) => t.status === status)
          .reduce((sum, t) => sum + t.probability, 0) - expected,
      ) > 1e-9
    )
      return invalid()
  }
  if (v.complete !== (v.unexploredProbability === 0)) return invalid()
  return v as unknown as Exploration
}
