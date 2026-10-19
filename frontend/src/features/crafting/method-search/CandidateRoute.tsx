import { isRepeatable, methodPoint } from './methodObservation'
import { guideMessages } from './guideMessages'
import { removesModifier } from './methodRisk'
import { probabilityText } from './probabilityPresentation'
import { validRoute } from './routeSelection'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
import { workspaceMessages } from './workspaceMessages'
import { useI18n } from '../../../shared/i18n/i18n'

export function CandidateRoute({
  view,
  selected,
  cycles,
  route,
  choose,
  clear,
  action,
}: {
  view: AutomaticView
  selected: number
  cycles: number
  route: Selection[]
  choose: (s: Selection) => void
  clear: () => void
  action: (a: AutomaticAction) => string
}) {
  const { locale } = useI18n(),
    copy = workspaceMessages[locale],
    guide = guideMessages[locale]
  const incoming = view.methods.flatMap((m) =>
    (methodPoint(m, cycles)?.outcomes ?? [])
      .filter(
        (o) =>
          o.target === selected && o.kind !== 'UNRESOLVED' && o.kind !== 'DEAD',
      )
      .map((o) => ({
        method: m.id,
        source: m.source,
        target: selected,
        kind: o.kind,
      })),
  )
  const visible = validRoute(view, route, cycles) ? route : []
  return (
    <section className="candidate-route" aria-label={guide.route}>
      <h3>{guide.route}</h3>
      {!visible.length && selected === 0 && <p>{guide.empty}</p>}
      <details>
        <summary>{guide.alternative}</summary>
        <label>
          {copy.incoming}
          <select
            aria-label={copy.incoming}
            value=""
            onChange={(e) => {
              const edge = incoming[Number(e.target.value)]
              if (edge) choose(edge)
            }}
          >
            <option value="">{copy.choose}</option>
            {incoming.map((e, i) => (
              <option key={`${e.method}:${e.kind}`} value={i}>
                {view.methods
                  .find((m) => m.id === e.method)!
                  .initial.concat(
                    view.methods.find((m) => m.id === e.method)!.cycle,
                  )
                  .map(action)
                  .join(' → ')}{' '}
              </option>
            ))}
          </select>
        </label>
        <button onClick={clear} disabled={!route.length}>
          {guide.reset}
        </button>
      </details>
      {selected !== 0 &&
        (!visible.length ||
          visible[0]!.source !== 0 ||
          visible.at(-1)!.target !== selected) && (
          <p role="status">{guide.empty}</p>
        )}
      <ol>
        {visible.map((e, i) => {
          const m = view.methods.find((m) => m.id === e.method)!
          return (
            <li key={`${i}:${e.method}`}>
              <strong>
                {m.initial.concat(m.cycle).map(action).join(' → ')}
              </strong>
              <p className="craft-chance">
                <span>
                  {isRepeatable(m)
                    ? (m.initial.length
                        ? m.initial.map(action).join(' → ') +
                          ': ' +
                          guide.once +
                          ' · ' +
                          m.cycle.map(action).join(' → ') +
                          ': '
                        : '') + guide.within.replace('{n}', String(cycles))
                    : guide.once}
                </span>
                {' · '}
                {guide.chance}:{' '}
                {(() => {
                  const o = methodPoint(m, cycles)?.outcomes.find(
                    (o) => o.target === e.target && o.kind === e.kind,
                  )
                  return o
                    ? probabilityText(o.probability.lower, o.probability)
                    : '—'
                })()}
              </p>
              {removesModifier(m) && (
                <p className="craft-risk">
                  {guide.risk} <a href="#crafting-policy">{guide.scope}</a>
                </p>
              )}
            </li>
          )
        })}
      </ol>
    </section>
  )
}
