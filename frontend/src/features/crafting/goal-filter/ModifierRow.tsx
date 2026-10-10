import { useI18n } from '../../../shared/i18n/i18n'
import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import {
  localizedModifierText,
  localizedModifierValueLabel,
} from '../localizedModifiers'
import { modifierPickerMessages } from './modifierPickerMessages'
import { craftStartMessages } from './craftStartMessages'
import { startInputMessages } from './startInputMessages'
import { changeModifierTier } from './modifierGroups'

type Modifier = ConcreteItem['explicits'][number]

export function ModifierRow({
  definition,
  modifier,
  tiers,
  eligible,
  index,
  onChange,
  onIssue,
  onRemove,
}: {
  definition: Definition
  modifier: Modifier
  tiers: Definition[]
  eligible: ReadonlySet<string>
  index: number
  onChange: (modifier: Modifier) => void
  onIssue: (issue: string) => void
  onRemove: () => void
}) {
  const { locale } = useI18n()
  const copy = craftStartMessages[locale]
  const tierCopy = modifierPickerMessages[locale]
  const label = localizedModifierText(definition, undefined, locale)
  return (
    <div className="craft-start-rolls" role="group" aria-label={label}>
      <strong title={label}>{label}</strong>
      <select
        aria-label={tierCopy.tier + ': ' + label}
        value={modifier.modifierId}
        onChange={(event) => {
          const next = tiers.find(
            (t) => t.id === event.target.value && eligible.has(t.id),
          )
          if (next) onChange(changeModifierTier(modifier, next))
        }}
      >
        {tiers.map((t) => (
          <option key={t.id} value={t.id} disabled={!eligible.has(t.id)}>
            T{t.tier}
          </option>
        ))}
      </select>
      <div className="craft-start-values">
        {definition.stats?.map((stat) => {
          const valueLabel = localizedModifierValueLabel(
            definition,
            stat.id,
            startInputMessages[locale].value,
            locale,
          )
          return (
            <input
              key={stat.id}
              type="number"
              min={stat.min}
              max={stat.max}
              aria-label={valueLabel}
              title={valueLabel + ' (' + stat.min + '–' + stat.max + ')'}
              value={modifier.values[stat.id]}
              onChange={(event) => {
                const value = Number(event.target.value)
                if (
                  !event.target.value ||
                  !Number.isSafeInteger(value) ||
                  value < stat.min ||
                  value > stat.max
                ) {
                  onIssue(
                    `Roll must be an integer from ${stat.min} to ${stat.max}.`,
                  )
                  return
                }
                onChange({
                  ...modifier,
                  values: { ...modifier.values, [stat.id]: value },
                })
              }}
            />
          )
        })}
      </div>
      <button
        aria-label={`${copy.removeLabel} ${index + 1}`}
        onClick={onRemove}
      >
        {copy.remove}
      </button>
    </div>
  )
}
