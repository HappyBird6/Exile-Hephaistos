import { useRef, useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { SiteHeader } from '../../../shared/site/SiteHeader'
import { SiteFooter } from '../../../shared/site/SiteFooter'
import { WorkspaceNavigation } from '../../../shared/site/WorkspaceNavigation'
import { useI18n } from '../../../shared/i18n/i18n'
import { CurrencyPriceContext } from '../currency-prices/context'
import { useCurrencyPrices } from '../currency-prices/useCurrencyPrices'
import { GoalItemCard, type GoalCard } from './GoalItemCard'
import { experimentText } from './messages'
import './experiment.css'

const tabs = ['prompt', 'input', 'answer'] as const
type Tab = (typeof tabs)[number]
const api = '/api/v1/support-test/'

class RequestError extends Error {
  constructor(readonly status: number) {
    super(String(status))
  }
}
async function request<T>(path: string, options: RequestInit): Promise<T> {
  const response = await fetch(api + path, options)
  if (!response.ok) throw new RequestError(response.status)
  return response.json()
}

export default function SupportTestPage() {
  const { locale, t, name } = useI18n()
  const copy = experimentText[locale]
  const market = useCurrencyPrices()
  const [selection, setSelection] = useState('')
  const [tab, setTab] = useState<Tab>('prompt')
  const [replies, setReplies] = useState<
    Record<string, { answer: string } | { error: number }>
  >({})
  const inFlight = useRef(false)
  const index = useQuery({
    queryKey: ['support-test', 'cards'],
    queryFn: ({ signal }) =>
      request<{ cards: GoalCard[]; configured: boolean }>('cards', { signal }),
  })
  const card =
    index.data?.cards.find((item) => item.id === selection) ??
    index.data?.cards[0]
  const id = card?.id ?? ''
  const preview = useQuery({
    queryKey: ['support-test', 'preview', id],
    enabled: !!id,
    queryFn: ({ signal }) =>
      request<{ prompt: string; inputJson: string }>(
        'cards/' + encodeURIComponent(id),
        { signal },
      ),
  })
  const send = useMutation({
    mutationFn: (cardId: string) =>
      request<{ answer: string }>('send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ cardId }),
      }),
    retry: false,
    onSuccess: (reply, cardId) =>
      setReplies((previous) => ({ ...previous, [cardId]: reply })),
    onError: (error, cardId) =>
      setReplies((previous) => ({
        ...previous,
        [cardId]: { error: error instanceof RequestError ? error.status : 0 },
      })),
    onSettled: () => {
      inFlight.current = false
    },
  })
  function submit() {
    if (!id || !preview.data || !index.data?.configured || inFlight.current)
      return
    inFlight.current = true
    setTab('answer')
    send.mutate(id)
  }
  const reply = replies[id]
  const waiting = send.isPending && send.variables === id
  function panel() {
    if (tab === 'answer') {
      if (waiting) return <p role="status">{copy.waiting}</p>
      if (!reply) return <p>{copy.empty}</p>
      if ('answer' in reply) return <pre>{reply.answer}</pre>
      return (
        <p className="site-actions" role="alert">
          {reply.error === 429 ? copy.limited : copy.failed}
        </p>
      )
    }
    if (preview.isError)
      return (
        <p className="site-actions" role="alert">
          {t('error.load', { subject: copy[tab] })}{' '}
          <button onClick={() => void preview.refetch()}>
            {t('ui.retry')}
          </button>
        </p>
      )
    if (!preview.data) return <p role="status">…</p>
    return (
      <pre>
        {tab === 'prompt' ? preview.data.prompt : preview.data.inputJson}
      </pre>
    )
  }
  return (
    <CurrencyPriceContext.Provider value={market}>
      <main className="craft-page craft-page--framed site-controls">
        <SiteHeader
          navigation={<WorkspaceNavigation active="support-test" />}
        />
        <section
          role="tabpanel"
          id="panel-support-test"
          aria-labelledby="workspace-support-test"
          className="workspace-panel experiment-panel"
        >
          <h1>{copy.goals}</h1>
          {index.isError && (
            <p className="site-actions" role="alert">
              {t('error.load', { subject: copy.goals })}{' '}
              <button onClick={() => void index.refetch()}>
                {t('ui.retry')}
              </button>
            </p>
          )}
          {index.isPending && <p role="status">…</p>}
          <div className="experiment-cards">
            {index.data?.cards.map((item) => (
              <GoalItemCard
                key={item.id}
                card={item}
                selected={item.id === id}
                select={() => setSelection(item.id)}
              />
            ))}
          </div>
          {card && (
            <>
              <div className="experiment-toolbar site-actions">
                <h2>
                  {name(card.baseId, card.base)} · {id}
                </h2>
                <button
                  type="button"
                  disabled={
                    !preview.data || !index.data?.configured || send.isPending
                  }
                  onClick={submit}
                >
                  {copy.send}
                </button>
              </div>
              {!index.data?.configured && <p role="status">{copy.missing}</p>}
              <div
                className="experiment-tabs site-actions"
                role="tablist"
                aria-label={copy.title}
              >
                {tabs.map((key, i) => (
                  <button
                    type="button"
                    key={key}
                    role="tab"
                    id={'experiment-' + key}
                    aria-selected={tab === key}
                    aria-controls={'experiment-pane-' + key}
                    tabIndex={tab === key ? 0 : -1}
                    onClick={() => setTab(key)}
                    onKeyDown={(event) => {
                      const next =
                        event.key === 'ArrowRight'
                          ? tabs[(i + 1) % 3]
                          : event.key === 'ArrowLeft'
                            ? tabs[(i + 2) % 3]
                            : event.key === 'Home'
                              ? tabs[0]
                              : event.key === 'End'
                                ? tabs[2]
                                : undefined
                      if (next) {
                        event.preventDefault()
                        setTab(next)
                        document.getElementById('experiment-' + next)?.focus()
                      }
                    }}
                  >
                    {['A', 'B', 'C'][i]} · {copy[key]}
                  </button>
                ))}
              </div>
              {tabs.map((key) => (
                <section
                  key={key}
                  role="tabpanel"
                  id={'experiment-pane-' + key}
                  aria-labelledby={'experiment-' + key}
                  hidden={tab !== key}
                  tabIndex={0}
                >
                  {tab === key && panel()}
                </section>
              ))}
            </>
          )}
        </section>
        <SiteFooter />
      </main>
    </CurrencyPriceContext.Provider>
  )
}
