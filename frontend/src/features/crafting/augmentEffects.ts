import { modifierMagnitudeValues } from './modifierMagnitudes'
import type { Definition } from './craftingApi'
import {
  augmentRule,
  structuralAugment,
  hasIdolBonded,
  visibleAugmentText,
} from './augmentRules'
import data from './augments.json'
import display from '../../shared/i18n/augmentDisplay.json'
import templates from './augmentEffectTemplates.json'
import { basePolicy } from './baseRegistry'
import { sanctifiedValues } from './sanctification/sanctification'
import type { Locale } from '../../shared/i18n/i18n'
import type { ConcreteItem } from './workbenchApi'

type Effect = { template: string; values: string[] }
type Effects = { classes: string[]; locales: Record<string, Effect[]> }[]
const source = templates as Record<
  string,
  { ordinary: Effects; bonded: Effects }
>

/** Only source-marked effect values scale; durations/conditions outside mod-value spans stay intact. */
export function renderAugmentEffect(
  effect: Effect,
  multiplier: number,
): string {
  return effect.template.replace(/\{v(\d+)\}/g, (_, index: string) =>
    effect.values[Number(index)]!.replace(/-?\d+(?:\.\d+)?/g, (value) =>
      multiplier === 1 ? value : String(Math.round(Number(value) * multiplier)),
    ),
  )
}

/** One shared numeric sum for item properties and socket-effect strength. */
export function itemStatValues(
  state: ConcreteItem,
  definitions: Record<string, Definition> = {},
): Record<string, number> {
  const values: Record<string, number> = {}
  for (const layer of ['implicits', 'explicits'] as const)
    for (const modifier of state[layer])
      for (const [id, value] of Object.entries(
        definitions[modifier.modifierId]
          ? modifierMagnitudeValues(
              state,
              definitions[modifier.modifierId]!,
              sanctifiedValues(state, modifier, layer),
              layer,
            )
          : sanctifiedValues(state, modifier, layer),
      ))
        values[id] = (values[id] ?? 0) + value
  return values
}

export function socketEffectMultiplier(
  state: ConcreteItem,
  definitions: Record<string, Definition> = {},
): number {
  return Math.max(
    0,
    1 +
      (itemStatValues(state, definitions)['local_socketed_items_effect_+%'] ??
        0) /
        100,
  )
}

export function installedAugmentEffects(
  state: ConcreteItem,
  locale: Locale,
  bonded = false,
  definitions: Record<string, Definition> = {},
): {
  slot: number
  id: string
  text: string
  english: string
  bonded: boolean
}[] {
  const itemClass = basePolicy(state.baseItemId)?.itemClass ?? ''
  const genericMultiplier = socketEffectMultiplier(state, definitions)
  return (state.installedAugments ?? []).flatMap((installed) => {
    const entry = data.find((a) => a.id === installed.augmentId)
    if (!entry || !(itemClass in entry.effects)) return []
    const peerRuneEffect =
      entry.kind === 'Rune'
        ? (state.installedAugments ?? [])
            .filter((a) => a.slotIndex !== installed.slotIndex)
            .reduce(
              (sum, a) =>
                sum +
                Math.round(
                  (augmentRule(a.augmentId).runeEffectPercent ?? 0) *
                    genericMultiplier,
                ),
              0,
            )
        : 0
    const multiplier = genericMultiplier + peerRuneEffect / 100
    const activeBonded =
      bonded || (entry.kind === 'Idol' && hasIdolBonded(state))
    return (
      activeBonded ? (['ordinary', 'bonded'] as const) : (['ordinary'] as const)
    ).flatMap((group) => {
      const effectMultiplier =
        group === 'ordinary' && structuralAugment(entry.id) ? 1 : multiplier
      const effects = source[entry.id]?.[group]?.find((row) =>
        row.classes.includes(itemClass),
      )?.locales
      const english = effects?.en
      const localized = effects?.[locale] ?? english
      if (english && localized)
        return [
          {
            slot: installed.slotIndex,
            id: entry.id,
            text: visibleAugmentText(
              localized
                .map((effect) => renderAugmentEffect(effect, effectMultiplier))
                .join('\n'),
            ),
            english: visibleAugmentText(
              english
                .map((effect) => renderAugmentEffect(effect, effectMultiplier))
                .join('\n'),
            ),
            bonded: group === 'bonded',
          },
        ]
      if (group === 'bonded') return []
      // New catalog entries can provide numeric effect sentences before a source-span bundle exists.
      const raw = (entry.effects as Record<string, Record<string, string>>)[
        itemClass
      ]
      const extra = display[entry.id as keyof typeof display]
      const text =
        locale === 'ru' || locale === 'th'
          ? (extra?.[locale]?.effects as Record<string, string> | undefined)?.[
              itemClass
            ]
          : raw?.[locale]
      const scale = (value: string) =>
        value.replace(/-?\d+(?:\.\d+)?/g, (n) =>
          multiplier === 1 ? n : String(Math.round(Number(n) * multiplier)),
        )
      return raw?.en
        ? [
            {
              slot: installed.slotIndex,
              id: entry.id,
              text: scale(text ?? raw.en),
              english: scale(raw.en),
              bonded: false,
            },
          ]
        : []
    })
  })
}

/** Unidentified copied effects are treated as Rune effects, anchored at their imported strength. */
export function observedAugmentEffects(
  state: ConcreteItem,
  bonded = false,
  definitions: Record<string, Definition> = {},
): string[] {
  const generic = socketEffectMultiplier(state, definitions)
  const amount =
    generic +
    (state.installedAugments ?? []).reduce(
      (sum, a) =>
        sum +
        Math.round((augmentRule(a.augmentId).runeEffectPercent ?? 0) * generic),
      0,
    ) /
      100
  const ratio = amount / (state.observations?.augmentEffectScale ?? 1)
  const effects = (state.observations?.augmentEffects ?? []).map((text) =>
    ratio === 1
      ? text
      : text.replace(/-?\d+(?:\.\d+)?/g, (n) =>
          String(Math.round(Number(n) * ratio)),
        ),
  )
  const renderedBonded = installedAugmentEffects(
    state,
    'en',
    bonded,
    definitions,
  )
    .filter((e) => e.bonded)
    .flatMap((e) => e.english.split('\n'))
  return effects.filter((line) => {
    const index = renderedBonded.indexOf(
      line.replace(/\s*\(rune\)$/, '').trim(),
    )
    if (index < 0) return true
    renderedBonded.splice(index, 1)
    return false
  })
}
