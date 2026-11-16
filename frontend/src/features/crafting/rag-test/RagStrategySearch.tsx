import { useContext, useEffect, useRef, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import type { PathTreeProps } from '../path-tree/PathTree'
import { gameName, useI18n } from '../../../shared/i18n/i18n'
import { localizedAction } from '../localizedCrafting'
import { modifierPickerMessages } from '../goal-filter/modifierPickerMessages'
import type { WorkbenchAction } from '../workbenchApi'
import type { AutomaticAction } from '../method-search/automaticApi'
import { ragMessages } from './messages'
import { RagAvailabilityContext } from './availabilityContext'
import {
  preparePrompt,
  generateStrategies,
  RagApiError,
  type StrategyResponse,
} from './api'

export function RagStrategySearch(props: PathTreeProps) {
  const { locale } = useI18n()
  return <Search key={props.request.clientRequestId + locale} {...props} />
}
function Search({ request, abstractQuality = 0 }: PathTreeProps) {
  const { locale } = useI18n(),
    copy = ragMessages[locale],
    ready = useContext(RagAvailabilityContext)
  // Preparing evidence never calls the model. Keep an in-flight preparation shared across remounts.
  const preview = useQuery({
    queryKey: ['support-rag', 'prompt', request, abstractQuality, locale],
    queryFn: () => preparePrompt(request, abstractQuality, locale),
    enabled: ready,
    staleTime: Infinity,
    retry: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  })
  const [response, setResponse] = useState<StrategyResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const pending = useRef<AbortController | null>(null)
  useEffect(() => () => pending.current?.abort(), [])
  const expired =
    error instanceof RagApiError && error.code === 'RAG_PROMPT_EXPIRED'
  const failure = error ?? preview.error
  const errorText =
    failure instanceof RagApiError && failure.status === 429
      ? copy.cooldown
      : expired
        ? copy.expired
        : copy.error
  async function generate() {
    if (pending.current || !ready) return
    if (preview.isError || expired) {
      setError(null)
      setResponse(null)
      void preview.refetch()
      return
    }
    if (!preview.data) return
    const controller = new AbortController()
    pending.current = controller
    setBusy(true)
    setError(null)
    setResponse(null)
    try {
      const result = await generateStrategies(
        preview.data.promptId,
        controller.signal,
      )
      if (!controller.signal.aborted) setResponse(result)
    } catch (e) {
      if (!controller.signal.aborted) setError(e)
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
    <div className="rag-results-layout">
      <section
        className="rag-prompt"
        aria-label={copy.prompt}
        aria-busy={preview.isFetching}
      >
        <h2>{copy.prompt}</h2>
        <textarea
          aria-label={copy.prompt}
          readOnly
          spellCheck={false}
          value={
            preview.data
              ? `SYSTEM\n${preview.data.prompt.system}\n\nUSER\n${preview.data.prompt.user}`
              : ''
          }
          placeholder={preview.isFetching ? copy.preparing : ''}
        />
        <button
          onClick={() => void generate()}
          disabled={
            busy ||
            !ready ||
            preview.isFetching ||
            (!preview.data && !preview.isError)
          }
        >
          {busy
            ? copy.busy
            : preview.isError || expired
              ? copy.reloadPrompt
              : copy.generate}
        </button>
        {failure != null && (
          <p role="alert">
            {errorText}
            {failure instanceof RagApiError && ` (${failure.code})`}
          </p>
        )}
      </section>
      <section
        className="rag-strategies"
        aria-label={copy.strategies}
        aria-busy={busy}
      >
        <h2>{copy.strategies}</h2>
        {busy ? (
          <p role="status">{copy.busy}</p>
        ) : (
          !response && <p className="rag-response-empty">{copy.waiting}</p>
        )}
        {response && (
          <>
            {response.status === 'INSUFFICIENT_EVIDENCE' ? (
              <p role="status">{copy.insufficient}</p>
            ) : (
              !response.strategies.length && <p role="status">{copy.empty}</p>
            )}
            {response.strategies.map((strategy, index) => (
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
              </article>
            ))}
          </>
        )}
      </section>
    </div>
  )
}
