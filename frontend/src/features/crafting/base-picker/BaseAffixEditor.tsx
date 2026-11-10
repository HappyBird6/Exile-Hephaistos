import { useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import type { PoolReference } from '../workbenchReferenceApi'
import {
  localizedModifierText,
  localizedModifierValueLabel,
} from '../localizedModifiers'
import { modifierRollDivisor, storedModifierRoll } from '../localizedModifiers'
import { modifierPoolMessages } from '../modifier-pool/modifierPoolMessages'
import { verifiedHistoryState } from '../workbenchHistory'
import {
  startingAffixOptions,
  startingAffixAllowed,
  startingAffixSource,
  startingItem,
} from './baseAffixSetup'
import type { StartingAffix } from './baseAffixSetup'
export type BaseAffixEditorProps = {
  initial: Initial
  item: ConcreteItem
  reference?: PoolReference | undefined
  affixes: StartingAffix[]
  onChange: (affixes: StartingAffix[]) => void
  disabled?: boolean | undefined
}
export function BaseAffixEditor(props: BaseAffixEditorProps) {
  return (
    <div className="wb-base-affixes">
      {(['PREFIX', 'SUFFIX'] as const).map((side) => (
        <AffixSide key={side} {...props} side={side} />
      ))}
    </div>
  )
}
function AffixSide({
  initial,
  item,
  reference,
  affixes,
  onChange,
  disabled,
  side,
}: BaseAffixEditorProps & { side: 'PREFIX' | 'SUFFIX' }) {
  const { locale, t } = useI18n(),
    text = modifierPoolMessages[locale]
  const [selected, setSelected] = useState(''),
    [values, setValues] = useState<Record<string, number>>({})
  const label = side === 'PREFIX' ? text.prefix : text.suffix
  const options = (reference ? startingAffixOptions(initial, reference) : [])
    .filter(
      (o) =>
        o.definition.affixType === side &&
        startingAffixAllowed(initial, item, affixes, o),
    )
    .sort((a, b) =>
      localizedModifierText(a.definition, undefined, locale).localeCompare(
        localizedModifierText(b.definition, undefined, locale),
        locale,
      ),
    )
  const choice = options.find((o) => o.key === selected)
  const next: StartingAffix | undefined = choice
    ? {
        modifierId: choice.definition.id,
        values,
        kind: choice.kind,
        source: choice.source,
      }
    : undefined
  const canAdd =
    next &&
    verifiedHistoryState(
      startingItem(initial, item.rarity, [...affixes, next]),
      initial,
    )
  return (
    <fieldset
      className="wb-base-affix"
      disabled={disabled || !reference || item.rarity === 'NORMAL'}
    >
      <legend>{label}</legend>
      <div className="wb-base-affix__choice">
        <select
          aria-label={label}
          value={choice ? selected : ''}
          onChange={(e) => {
            setSelected(e.target.value)
            const d = options.find((o) => o.key === e.target.value)?.definition
            setValues(
              Object.fromEntries((d?.stats ?? []).map((s) => [s.id, s.min])),
            )
          }}
        >
          <option value="">—</option>
          {options.map((o) => (
            <option key={o.key} value={o.key}>
              {localizedModifierText(o.definition, undefined, locale)}
              {o.definition.tier > 0 ? ` · T${o.definition.tier}` : ''}
              {o.source ? ` — ${startingAffixSource(o.source, locale)}` : ''}
            </option>
          ))}
        </select>
        <button
          type="button"
          aria-label={`${t('ui.add_manual_modifier')} · ${label}`}
          disabled={!canAdd}
          onClick={() => {
            if (next && canAdd) {
              onChange([...affixes, next])
              setSelected('')
              setValues({})
            }
          }}
        >
          ＋
        </button>
      </div>
      {choice && (
        <div className="wb-base-affix__rolls">
          {choice.definition.stats?.map((stat) => {
            const divisor = modifierRollDivisor(choice.definition, stat.id)
            return (
              <input
                key={stat.id}
                type="number"
                aria-label={localizedModifierValueLabel(
                  choice.definition,
                  stat.id,
                  locale,
                )}
                min={stat.min / divisor}
                max={stat.max / divisor}
                step={1 / divisor}
                value={(values[stat.id] ?? stat.min) / divisor}
                onChange={(e) =>
                  setValues({
                    ...values,
                    [stat.id]: storedModifierRoll(
                      e.target.valueAsNumber,
                      divisor,
                    ),
                  })
                }
              />
            )
          })}
        </div>
      )}
      {affixes
        .filter((a) => initial.modifiers[a.modifierId]?.affixType === side)
        .map((a) => (
          <div className="wb-base-affix__selected" key={a.modifierId}>
            <span>
              {localizedModifierText(
                initial.modifiers[a.modifierId]!,
                a.values,
                locale,
              )}
              {a.source && (
                <small>{startingAffixSource(a.source, locale)}</small>
              )}
            </span>
            <button
              type="button"
              aria-label={`${t('ui.remove')} · ${localizedModifierText(initial.modifiers[a.modifierId]!, a.values, locale)}`}
              onClick={() => onChange(affixes.filter((x) => x !== a))}
            >
              ×
            </button>
          </div>
        ))}
    </fieldset>
  )
}
