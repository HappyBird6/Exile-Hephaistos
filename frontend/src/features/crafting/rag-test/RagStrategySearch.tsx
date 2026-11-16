import { useContext, useEffect, useRef, useState } from 'react'
import type { PathTreeProps } from '../path-tree/PathTree'
import { gameName, useI18n } from '../../../shared/i18n/i18n'
import { localizedAction } from '../localizedCrafting'
import { modifierPickerMessages } from '../goal-filter/modifierPickerMessages'
import type { WorkbenchAction } from '../workbenchApi'
import type { AutomaticAction } from '../method-search/automaticApi'
import { ItemCard } from '../ItemCard'
import { ragMessages } from './messages'
import { RagAvailabilityContext } from './availabilityContext'
import { compareStrategies, RagApiError, type Report } from './api'

export function RagStrategySearch(props: PathTreeProps) {
  const { locale } = useI18n()
  return <Search key={props.request.clientRequestId + locale} {...props} />
}
function Search({
  request,
  abstractQuality = 0,
  onRecalculate,
  presentItem,
}: PathTreeProps) {
  const { locale } = useI18n(),
    copy = ragMessages[locale],
    ready = useContext(RagAvailabilityContext)
  const [mode, setMode] = useState<'ALL' | 'RETRIEVED'>('ALL')
  const [initial, setInitial] = useState(true),
    [busy, setBusy] = useState(false)
  const [report, setReport] = useState<Report | null>(null),
    [error, setError] = useState(''),
    [expanded, setExpanded] = useState(false)
  const pending = useRef<AbortController | null>(null)
  useEffect(() => () => pending.current?.abort(), [])
  async function generate() {
    if (pending.current || !ready) return
    const controller = new AbortController()
    pending.current = controller
    setBusy(true)
    setError('')
    setReport(null)
    setExpanded(false)
    try {
      const result = await compareStrategies(
        request,
        mode,
        initial,
        abstractQuality,
        locale,
        controller.signal,
      )
      if (!controller.signal.aborted) setReport(result)
    } catch (e) {
      if (controller.signal.aborted) return
      setError(
        e instanceof RagApiError && e.status === 429
          ? copy.cooldown
          : e instanceof RagApiError &&
              ['RAG_DISABLED', 'RAG_KEY_MISSING'].includes(e.code)
            ? copy.configure
            : copy.error,
      )
    } finally {
      if (!controller.signal.aborted) {
        pending.current = null
        setBusy(false)
      }
    }
  }
  function actionLabel(a: AutomaticAction) {
    if (a.operation === 'DESECRATE')
      return (
        copy.desecrate +
        ' · ' +
        (a.side === 'PREFIX'
          ? modifierPickerMessages[locale].prefix
          : modifierPickerMessages[locale].suffix)
      )
    if (a.purpose === 'QUALITY_BASE_CHECKPOINT') return copy.quality20
    if (a.purpose === 'QUALITY_CHECKPOINT') return copy.quality40
    const currency = localizedAction(a.currency as WorkbenchAction)
    const omens = a.omens.map((o) =>
      gameName(
        'Omen_of_' +
          o
            .toLowerCase()
            .split('_')
            .map((part) => part[0]!.toUpperCase() + part.slice(1))
            .join('_'),
        o,
      ),
    )
    return [currency, ...omens].join(' + ')
  }
  return (
    <div className="rag-results">
      <div className="rag-controls">
        <label>
          {copy.mode}
          <select
            value={mode}
            disabled={busy}
            onChange={(e) => {
              setMode(e.target.value as typeof mode)
              setReport(null)
            }}
          >
            <option value="ALL">{copy.all}</option>
            <option value="RETRIEVED">{copy.retrieved}</option>
          </select>
        </label>
        <label className="rag-stage">
          <input
            type="checkbox"
            checked={initial}
            disabled={busy}
            onChange={(e) => {
              setInitial(e.target.checked)
              setReport(null)
            }}
          />
          {copy.initial}
        </label>
        <button onClick={() => void generate()} disabled={busy || !ready}>
          {busy ? copy.busy : copy.generate}
        </button>
        <button onClick={onRecalculate}>{copy.edit}</button>
      </div>
      {error && <p role="alert">{error}</p>}
      {busy && (
        <p role="status" className="rag-test-notice">
          {copy.busy}
        </p>
      )}
      <div className="rag-results-layout">
        <aside className="rag-input-card">
          <ItemCard
            item={presentItem(request.start.item, locale)}
            baseItemId={request.start.item.baseItemId}
          />
        </aside>
        <section
          className="rag-strategies"
          aria-label={copy.strategies}
          aria-busy={busy}
        >
          {report && (
            <>
              <div className="rag-results-heading">
                <h2>
                  {copy.strategies} <span>{report.strategies.length}</span>
                </h2>
                {report.cached && <small>{copy.cached}</small>}
              </div>
              {report.proposalStatus === 'INSUFFICIENT_EVIDENCE' ? (
                <p role="status">{copy.insufficient}</p>
              ) : (
                !report.strategies.length && <p role="status">{copy.empty}</p>
              )}
              {(expanded
                ? report.strategies
                : report.strategies.slice(0, 6)
              ).map((strategy, index) => (
                <article key={index} className="rag-strategy-card">
                  <div className="rag-strategy-title">
                    <span className="rag-strategy-number">{index + 1}</span>
                    <h3>
                      {[...strategy.initial, ...strategy.cycle]
                        .map(actionLabel)
                        .join(' → ')}
                    </h3>
                  </div>
                  <dl>
                    {strategy.initial.length > 0 && (
                      <>
                        <dt>{copy.preparation}</dt>
                        <dd>{strategy.initial.map(actionLabel).join(' → ')}</dd>
                      </>
                    )}
                    <dt>{copy.acquisition}</dt>
                    <dd>{strategy.cycle.map(actionLabel).join(' → ')}</dd>
                  </dl>
                  <p>{strategy.explanation}</p>
                  <details>
                    <summary>{copy.documents}</summary>
                    <ul>
                      {strategy.documents.map((id) => (
                        <li key={id}>{id}</li>
                      ))}
                    </ul>
                  </details>
                </article>
              ))}
              {!expanded && report.strategies.length > 6 && (
                <button onClick={() => setExpanded(true)}>{copy.more}</button>
              )}
              <details className="rag-comparison">
                <summary>
                  {copy.comparison} ·{' '}
                  {report.candidateMatch
                    ? copy.same
                    : report.proposalStatus !== 'COMPLETE' ||
                        report.validationErrors.length
                      ? copy.unverified
                      : copy.different}
                </summary>
                <dl>
                  <dt>{copy.expected}</dt>
                  <dd>{report.reference.length}</dd>
                  <dt>{copy.missing}</dt>
                  <dd>{report.missing.length}</dd>
                  <dt>{copy.extra}</dt>
                  <dd>{report.extra.length}</dd>
                </dl>
                <p>{report.executionMatch ? copy.same : copy.unverified}</p>
                <p>
                  {report.model} · {report.corpusVersion} ·{' '}
                  {report.inputTokens + report.outputTokens} tokens
                </p>
                <ul>
                  {report.documents.map((id) => (
                    <li key={id}>{id}</li>
                  ))}
                </ul>
              </details>
            </>
          )}
        </section>
      </div>
    </div>
  )
}
