import { attemptLabel } from './attemptLabel'
import { methodPoint } from './methodObservation'
import { pathTreeMessages } from '../path-tree/messages'
import { methodSearchMessages } from './messages'
import type { AutomaticArrow, AutomaticNode } from './automaticApi'
import type { ItemCardData } from '../itemCardData'
import type { Selection } from './supportStorage'
import { ItemCard } from '../ItemCard'
import { commonStateCard } from './commonStateCard'
import { tierCandidates, type JunkClassifier } from './graphDisplay'
import { sumBounds } from './outcomeGrouping'
import { probabilityText } from './probabilityPresentation'
import { useI18n } from '../../../shared/i18n/i18n'
import { graphNavigationMessages } from './graphNavigationMessages'
import { graphStateMessages } from './graphStateMessages'
type Arrival = {
  method: AutomaticArrow
  outcome: AutomaticArrow['points'][number]['outcomes'][number]
}
export function GraphCandidates({
  nodes,
  cycles = 1,
  cardFor,
  displayCardFor,
  junk,
  arrivals,
  select,
  choose,
  selected,
  action,
}: {
  cycles?: number
  nodes: AutomaticNode[]
  cardFor: (id: number) => ItemCardData
  displayCardFor: (id: number) => ItemCardData
  junk?: JunkClassifier | undefined
  arrivals: Map<number, Arrival[]>
  select: (id: number) => void
  choose: (s: Selection) => void
  selected: number
  action: (a: AutomaticArrow['cycle'][number]) => string
}) {
  const { locale } = useI18n(),
    nav = graphNavigationMessages[locale],
    copy = graphStateMessages[locale]
  const common = pathTreeMessages[locale],
    methods = methodSearchMessages[locale]
  const kindLabel = (kind: string) =>
    kind === 'GOAL'
      ? common.hit
      : kind === 'LOSS'
        ? methods.loss
        : kind === 'UNRESOLVED'
          ? common.unknown
          : kind === 'DEAD'
            ? common.unavailable
            : methods.checkpoint
  const groups = tierCandidates(nodes, junk)
  return (
    <details className="graph-candidates">
      <summary>
        {nav.variants} ({groups.length})
      </summary>
      {groups.map((group) => {
        const mass = new Map<string, Arrival[]>()
        const seen = new Set<string>()
        for (const node of group)
          for (const arrival of arrivals.get(node.id) ?? []) {
            const key = arrival.method.id + ':' + arrival.outcome.kind
            const unique = key + ':' + node.id
            if (seen.has(unique)) continue
            seen.add(unique)
            mass.set(key, [...(mass.get(key) ?? []), arrival])
          }
        return (
          <section
            key={group[0]!.id}
            data-tier-candidates={group.map((n) => n.id).join(',')}
          >
            <ItemCard
              item={commonStateCard(group.map((n) => displayCardFor(n.id)))}
            />
            {[...mass].map(([key, values]) => {
              const m = values[0]!.method,
                probability = sumBounds(
                  values.map((v) => v.outcome.probability),
                )
              return (
                <p key={key}>
                  {m.initial.concat(m.cycle).map(action).join(' · ')} ·{' '}
                  {nav.source} {m.source} · {kindLabel(values[0]!.outcome.kind)}
                  {(methodPoint(m, cycles)?.cycles ?? 1) > 1 && (
                    <>
                      {' '}
                      · {attemptLabel(locale, methodPoint(m, cycles)!.cycles)}
                    </>
                  )}
                  : {probabilityText(probability.lower, probability)}
                </p>
              )
            })}
            <details>
              <summary>
                {nav.conditions} ({group.length})
              </summary>
              {group.map((candidate) => (
                <section key={candidate.id} data-candidate-id={candidate.id}>
                  <ItemCard item={cardFor(candidate.id)} />
                  <button
                    aria-pressed={candidate.id === selected}
                    onClick={() => select(candidate.id)}
                  >
                    {copy.select}
                  </button>
                  {(arrivals.get(candidate.id) ?? []).map(
                    ({ method, outcome }) => (
                      <button
                        key={method.id + outcome.kind}
                        onClick={() =>
                          choose({
                            method: method.id,
                            source: method.source,
                            target: candidate.id,
                            kind: outcome.kind,
                          })
                        }
                      >
                        {method.initial
                          .concat(method.cycle)
                          .map(action)
                          .join(' · ')}{' '}
                        · {nav.source} {method.source} ·{' '}
                        {kindLabel(outcome.kind)}:{' '}
                        {probabilityText(
                          outcome.probability.lower,
                          outcome.probability,
                        )}
                      </button>
                    ),
                  )}
                </section>
              ))}
            </details>
          </section>
        )
      })}
    </details>
  )
}
