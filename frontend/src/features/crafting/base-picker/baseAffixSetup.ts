import { basePreview } from './baseChoices'
import type { BaseChoice } from './baseChoices'
import { defenceDisplay } from '../defenceDisplay'
import { localizedModifierText } from '../localizedModifiers'
import { catalystProjection } from '../catalystQuality'
import { preciseModifierProjection } from '../sanctification/preciseModifierProjection'
import type { Initial, Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { concreteInitial } from '../workbenchApi'
import type { PoolReference } from '../workbenchReferenceApi'
import rules from '../augmentCrafting.json'
import { baseRarityAllowed } from '../baseRegistry'
import { isWorkbenchJewel, jewelCapacity } from '../basicJewel'
import { modifierPoolMessages } from '../modifier-pool/modifierPoolMessages'
import { augmentName } from '../socketAugments'
import { uiText } from '../../../shared/i18n/i18n'
import type { Locale } from '../../../shared/i18n/i18n'
export type StartingAffix = {
  modifierId: string
  values: Record<string, number>
  kind: 'ordinary' | 'crafted' | 'desecrated'
  source: string
}
export type StartingAffixOption = {
  key: string
  definition: Definition
  kind: StartingAffix['kind']
  source: string
}
export function startingItem(
  initial: Initial,
  rarity: ConcreteItem['rarity'],
  affixes: StartingAffix[],
): ConcreteItem {
  const root = concreteInitial(initial)
  return {
    ...root,
    rarity: baseRarityAllowed(root.baseItemId, rarity) ? rarity : 'RARE',
    explicits: affixes.map((a) => ({
      modifierId: a.modifierId,
      values: a.values,
      ...(a.kind === 'crafted' ? { crafted: true } : {}),
    })),
    desecrated: affixes
      .filter((a) => a.kind === 'desecrated')
      .map((a, i) => ({
        id: `starting-${i}`,
        modifierId: a.modifierId,
        side: initial.modifiers[a.modifierId]!.affixType as 'PREFIX' | 'SUFFIX',
      })),
  }
}
export function startingAffixOptions(
  initial: Initial,
  reference: PoolReference,
): StartingAffixOption[] {
  const choices = new Map<string, StartingAffixOption>()
  for (const entry of reference.entries) {
    const definition = initial.modifiers[entry.modifierId]
    if (!definition || definition.layer === 'IMPLICIT') continue
    let sources: string[] = [],
      kind: StartingAffix['kind'] = 'ordinary'
    if (entry.kind === 'ordinary') {
      if (definition.tags?.includes('augment_pool')) {
        sources = Object.entries(rules.effects)
          .filter(
            ([, r]) =>
              'pool' in r && definition.tags?.includes(r.pool + '_pool'),
          )
          .map(([id]) => id)
      } else if ((definition.weight ?? 0) > 0) sources = ['']
    } else if (
      entry.kind === 'desecrated' &&
      definition.tags?.includes('reveal_only')
    ) {
      sources = ['Desecrated']
      kind = 'desecrated'
    } else if (entry.kind === 'essence') {
      kind = 'crafted'
      sources = [
        ...new Set(
          entry.actions.map((a) =>
            a.includes('ESSENCE')
              ? 'Essence'
              : a.includes('ALLOY')
                ? 'Alloy'
                : 'Liquid Emotions',
          ),
        ),
      ]
    }
    for (const source of sources) {
      const key = definition.id + '|' + source
      choices.set(key, { key, definition, kind, source })
    }
  }
  return [...choices.values()]
}
export function startingAffixSource(source: string, locale: Locale): string {
  return source === 'Desecrated'
    ? modifierPoolMessages[locale].desecrated
    : source in rules.effects
      ? augmentName(source)
      : uiText(source, locale)
}
export function startingAffixAllowed(
  initial: Initial,
  item: ConcreteItem,
  choices: StartingAffix[],
  option: StartingAffixOption,
): boolean {
  const d = option.definition,
    side = d.affixType
  if (
    item.rarity === 'NORMAL' ||
    !['PREFIX', 'SUFFIX'].includes(side) ||
    d.requiredItemLevel! > item.itemLevel ||
    (option.kind === 'desecrated' && item.rarity !== 'RARE')
  )
    return false
  const capacity = isWorkbenchJewel(item.baseItemId)
    ? jewelCapacity(item, side)
    : item.rarity === 'MAGIC'
      ? side === 'PREFIX'
        ? (initial.baseRules?.magicPrefixes ?? 1)
        : (initial.baseRules?.magicSuffixes ?? 1)
      : side === 'PREFIX'
        ? (initial.baseRules?.rarePrefixes ?? 3)
        : (initial.baseRules?.rareSuffixes ?? 3)
  if (
    choices.filter((a) => initial.modifiers[a.modifierId]?.affixType === side)
      .length >= capacity
  )
    return false
  if (
    choices.some(
      (a) =>
        a.modifierId === d.id ||
        initial.modifiers[a.modifierId]?.familyIds.some((f) =>
          d.familyIds.includes(f),
        ),
    )
  )
    return false
  if (
    option.kind === 'crafted' &&
    choices.filter((a) => a.kind === 'crafted').length >=
      (isWorkbenchJewel(item.baseItemId) || item.augmentSockets == null ? 1 : 2)
  )
    return false
  return true
}

export function startingPreview(
  choice: BaseChoice,
  initial: Initial,
  item: ConcreteItem,
  affixes: StartingAffix[],
  locale: Locale,
) {
  const preview = basePreview(choice, item.itemLevel, locale)
  const computed = defenceDisplay(item, initial.modifiers, locale)
  const labels = new Set(computed.properties.map((p) => p.text.split(':')[0]))
  return {
    ...preview,
    rarity: item.rarity,
    properties: [
      ...preview.properties.filter((p) => !labels.has(p.text.split(':')[0])),
      ...computed.properties,
    ],
    modifiers: [...item.implicits, ...item.explicits].map((m, index) => {
      const d = initial.modifiers[m.modifierId]!,
        implicit = index < item.implicits.length,
        a = affixes.find((a) => a.modifierId === m.modifierId)
      const values = item.sanctification
        ? preciseModifierProjection(
            item,
            d,
            m,
            implicit ? 'implicits' : 'explicits',
          ).values
        : catalystProjection(d, m.values, item.catalystQuality, item).values
      return {
        id: m.modifierId,
        text: localizedModifierText(d, values, locale),
        kind: implicit
          ? ('implicit' as const)
          : a?.kind === 'crafted'
            ? ('crafted' as const)
            : a?.kind === 'desecrated'
              ? ('desecrated' as const)
              : ('explicit' as const),
        sourceLabel: a?.source
          ? startingAffixSource(a.source, locale)
          : undefined,
      }
    }),
  }
}
