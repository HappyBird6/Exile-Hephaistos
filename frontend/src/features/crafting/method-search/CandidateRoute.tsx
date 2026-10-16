import { validRoute } from './candidateRoute'
import type { AutomaticAction, AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
import { workspaceMessages } from './workspaceMessages'
import { methodSearchMessages } from './messages'
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
    methods = methodSearchMessages[locale]
  const incoming = view.methods.flatMap((m) =>
    (m.points.find((p) => p.cycles === cycles)?.outcomes ?? [])
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
    <section className="candidate-route" aria-label={copy.route}>
      <h3>{copy.route}</h3>
      <p>{copy.hint}</p>
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
              {e.source} → {e.target}:{' '}
              {view.methods
                .find((m) => m.id === e.method)!
                .initial.concat(
                  view.methods.find((m) => m.id === e.method)!.cycle,
                )
                .map(action)
                .join(' → ')}{' '}
              ({e.kind})
            </option>
          ))}
        </select>
      </label>
      <button onClick={clear} disabled={!route.length}>
        {copy.reset}
      </button>
      {selected !== 0 &&
        (!visible.length ||
          visible[0]!.source !== 0 ||
          visible.at(-1)!.target !== selected) && (
          <p role="status">{copy.disconnected}</p>
        )}
      <ol>
        {visible.map((e, i) => {
          const m = view.methods.find((m) => m.id === e.method)!
          return (
            <li key={`${i}:${e.method}`}>
              <strong>
                {e.source} → {e.target} ({e.kind})
              </strong>
              {m.initial.length > 0 && (
                <>
                  <p>{methods.initial}</p>
                  <ol>
                    {m.initial.map((a, j) => (
                      <li key={j}>{action(a)}</li>
                    ))}
                  </ol>
                </>
              )}
              <p>
                {methods.cycles}: {cycles}
              </p>
              <ol>
                {m.cycle.map((a, j) => (
                  <li key={j}>{action(a)}</li>
                ))}
              </ol>
            </li>
          )
        })}
      </ol>
      <p>{copy.unknown}</p>
    </section>
  )
}
