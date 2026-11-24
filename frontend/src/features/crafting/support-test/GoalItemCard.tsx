import { ItemCard } from '../ItemCard'
import { startInputMessages } from '../goal-filter/startInputMessages'
import { modifierTemplates } from '../../../shared/i18n/localeData'
import { useI18n } from '../../../shared/i18n/i18n'

type Bounds = { min: number | null; max: number | null }
type Option = { ref: string; stat: string; unit: string }
type Group = Bounds & {
  op: string
  disabled: boolean
  rows: (Bounds & { ref: string; disabled: boolean; weight: number | null })[]
}
export type GoalCard = {
  id: string
  baseId: string
  templates: Record<string, string>
  base: string
  ilvl: number
  optionRefs: { prefix: Option[]; suffix: Option[] }
  goalGroups: Group[]
  derived: Record<string, { label: string; sum: string[] }> | null
}

function bounds({ min, max }: Bounds, unit = '') {
  if (min !== null && max !== null)
    return min === max ? `= ${min}${unit}` : `${min}${unit}–${max}${unit}`
  if (min !== null) return `≥ ${min}${unit}`
  if (max !== null) return `≤ ${max}${unit}`
  return ''
}

export function GoalItemCard({
  card,
  selected,
  select,
}: {
  card: GoalCard
  selected: boolean
  select: () => void
}) {
  const { name, locale } = useI18n()
  const localized: Record<string, { template: string }> =
    modifierTemplates.templates[locale]
  function label(ref: string) {
    const templateId = card.templates[ref]
    const template = templateId && localized[templateId]?.template
    return template
      ? template.replace('{v0}', '[' + startInputMessages[locale].value + ']')
      : (options.find((option) => option.ref === ref)?.stat ?? ref)
  }
  const base = name(card.baseId, card.base)
  const options = [...card.optionRefs.prefix, ...card.optionRefs.suffix]
  const modifiers = card.goalGroups.flatMap((group, index) =>
    group.disabled
      ? []
      : group.rows
          .filter((row) => !row.disabled)
          .map((row) => {
            const option = options.find((value) => value.ref === row.ref)
            const derived = card.derived?.[row.ref]
            const unit =
              (
                option ??
                options.find((value) => derived?.sum.includes(value.ref))
              )?.unit === 'percent'
                ? '%'
                : ''
            return {
              id: `${index}-${row.ref}`,
              kind: 'explicit' as const,
              affixLabel: [group.op, bounds(group)].filter(Boolean).join(' '),
              text: [
                derived
                  ? 'Σ(' + derived.sum.map(label).join(' + ') + ')'
                  : label(row.ref),
                bounds(row, unit),
                row.weight === null ? '' : `× ${row.weight}`,
              ]
                .filter(Boolean)
                .join(' '),
            }
          }),
  )
  return (
    <div className="experiment-card" data-selected={selected}>
      <ItemCard
        item={{
          rarity: 'RARE',
          name: base,
          base,
          itemLevel: card.ilvl,
          itemClass: card.id,
          properties: [],
          requirements: [],
          modifiers,
          flags: [],
        }}
      />
      <button
        type="button"
        aria-label={`${base} · ${card.id}`}
        aria-pressed={selected}
        onClick={select}
      />
    </div>
  )
}
