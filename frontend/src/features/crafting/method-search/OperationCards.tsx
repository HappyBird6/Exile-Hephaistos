import { operationKind, operationGroups } from './operationGrouping'
import { useState } from 'react'
import type { ReactNode } from 'react'
import type {
  AutomaticAction,
  AutomaticArrow,
  AutomaticView,
} from './automaticApi'
import { useI18n } from '../../../shared/i18n/i18n'
import { operationMessages } from './operationMessages'
import { methodPoint } from './methodObservation'
import { probabilityText } from './probabilityPresentation'
import { sumBounds } from './outcomeGrouping'

export function OperationCards({
  methods,
  cycles,
  action,
  render,
  recommendations = [],
}: {
  methods: AutomaticArrow[]
  cycles: number
  action: (a: AutomaticAction) => string
  render: (method: AutomaticArrow, index: number) => ReactNode
  recommendations?: AutomaticView['recommendations']
}) {
  const { locale } = useI18n(),
    copy = operationMessages[locale]
  const [chosen, setChosen] = useState<Record<string, string>>({})
  return operationGroups(methods, cycles, recommendations).map((group) => {
    const method =
      group.variants.find((v) => v.id === chosen[group.id]) ??
      group.variants[0]!
    const kind = operationKind(method)
    return (
      <section className="operation-card" key={group.id}>
        <h4>
          {kind === 'other'
            ? method.initial.concat(method.cycle).map(action).join(' → ')
            : copy[kind]}
        </h4>
        <p>
          {kind === 'add'
            ? copy.addHelp
            : kind === 'other'
              ? copy.inspect
              : copy.replaceHelp}
        </p>
        {group.variants.length > 1 && (
          <details className="operation-variants">
            <summary>
              {copy.compare} ({group.variants.length})
            </summary>
            <p>{copy.separate}</p>
            <div role="group" aria-label={copy.compare}>
              {group.variants.map((variant) => {
                const goals =
                  methodPoint(variant, cycles)?.outcomes.filter(
                    (o) => o.kind === 'GOAL',
                  ) ?? []
                const mass = goals.length
                  ? sumBounds(goals.map((o) => o.probability))
                  : null
                return (
                  <button
                    key={variant.id}
                    type="button"
                    aria-pressed={method.id === variant.id}
                    onClick={() =>
                      setChosen((previous) => ({
                        ...previous,
                        [group.id]: variant.id,
                      }))
                    }
                  >
                    <span>
                      {variant.initial
                        .concat(variant.cycle)
                        .map(action)
                        .join(' → ')}
                    </span>
                    <span>
                      {copy.chance}:{' '}
                      {mass ? probabilityText(mass.lower, mass) : '—'}
                    </span>
                  </button>
                )
              })}
            </div>
          </details>
        )}
        {render(method, 0)}
      </section>
    )
  })
}
