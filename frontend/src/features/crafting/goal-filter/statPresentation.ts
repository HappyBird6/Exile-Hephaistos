import type { Locale } from '../../../shared/i18n/i18n'
import {
  localizedModifierText,
  localizedModifierValueLabel,
} from '../localizedModifiers'
import type { Catalog, Stat } from './types'
import { startInputMessages } from './startInputMessages'

/** Display joins use canonical source and layer, never translated text or family alone. */
export function presentGoalStat(
  stat: Stat,
  catalog: Catalog,
  locale: Locale,
): Stat & { searchText: string } {
  if (stat.kind === 'PSEUDO') return { ...stat, searchText: stat.label }
  const sources = Object.values(catalog.sourceModifiers ?? {})
    .filter(
      (d) =>
        d.layer === stat.kind &&
        d.stats?.some((s) => stat.sourceStatIds.includes(s.id)),
    )
    .sort(
      (a, b) =>
        Number((b.requiredItemLevel ?? 1) <= catalog.context.itemLevel) -
          Number((a.requiredItemLevel ?? 1) <= catalog.context.itemLevel) ||
        (a.stats?.length ?? 0) - (b.stats?.length ?? 0) ||
        a.tier - b.tier ||
        a.id.localeCompare(b.id),
    )
  const first = sources[0]
  const label = first
    ? localizedModifierValueLabel(
        first,
        stat.sourceStatIds[0]!,
        startInputMessages[locale].value,
        locale,
      )
    : stat.label
  return {
    ...stat,
    label,
    searchText: [
      label,
      stat.label,
      ...sources.flatMap((d) => [
        d.name,
        d.text,
        localizedModifierText(d, undefined, locale),
      ]),
    ].join(' '),
  }
}
