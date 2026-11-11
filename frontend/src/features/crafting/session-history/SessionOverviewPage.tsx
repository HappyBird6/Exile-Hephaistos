import { CraftingCostLine } from '../currency-prices/CurrencyPrices'
import { CurrencyPriceContext } from '../currency-prices/context'
import { useCurrencyPrices } from '../currency-prices/useCurrencyPrices'
import { useRef, useState } from 'react'
import { flushSync } from 'react-dom'
import { useIsFetching, useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { IndexedFilmRepository } from '../session-library/IndexedFilmRepository'
import { SessionFlow } from './SessionFlow'
import { historyResources } from './resources'
import { historyMessages } from './messages'
import { saveBlob } from './transfer'
import { snapshotPages } from './snapshot'
import { sessionTitle } from '../session-library/sessionTitle'
import './history.css'

export function SessionOverviewPage({ id }: { id: string }) {
  const { locale, t } = useI18n()
  const market = useCurrencyPrices()
  const text = historyMessages[locale]
  const catalogsLoading = useIsFetching({ queryKey: ['crafting', 'initial'] })
  const [error, setError] = useState(false)
  const [busy, setBusy] = useState(false)
  const [snapshotOpen, setSnapshotOpen] = useState(false)
  const snapshotRoot = useRef<HTMLDivElement>(null)
  const data = useQuery({
    queryKey: ['session-overview', id],
    retry: false,
    queryFn: async () => {
      const repository = new IndexedFilmRepository()
      try {
        return (
          (await repository.load()).films.find((film) => film.id === id) ?? null
        )
      } finally {
        await repository.close()
      }
    },
  })
  const film = data.data
  const title = film ? sessionTitle(film, locale) : text.title
  const origin =
    new URLSearchParams(location.search).get('from') === 'simulation'
      ? 'simulation'
      : 'history'
  const returnQuery = new URLSearchParams({ workbench: origin })
  if (origin === 'history') {
    returnQuery.set('session', id)
    if (new URLSearchParams(location.search).get('section') === 'favorites')
      returnQuery.set('section', 'favorites')
  }
  async function action(run: () => Promise<void> | void) {
    setBusy(true)
    setError(false)
    try {
      await run()
    } catch {
      setError(true)
    } finally {
      setBusy(false)
    }
  }
  return (
    <CurrencyPriceContext.Provider value={market}>
      <main className="craft-page craft-page--framed history-page">
        <header className="craft-header">
          <a
            className="craft-brand"
            href="/"
            aria-label={t('ui.exile_hephaistos_home')}
          >
            <span className="brand-mark" aria-hidden="true">
              H
            </span>
            <span>
              EXILE <b>HEPHAISTOS</b>
              <small>{t('ui.path_of_exile_2_crafting_workbench')}</small>
            </span>
          </a>
          <div className="history-title-group">
            <span className="history-page-label">{text.title}</span>
            <h1 className="history-title" title={title}>
              {title}
            </h1>
          </div>
          <div className="history-actions">
            <button
              disabled={
                busy ||
                !film ||
                data.isFetching ||
                catalogsLoading > 0 ||
                market.pending
              }
              onClick={() =>
                void action(async () => {
                  flushSync(() => setSnapshotOpen(true))
                  try {
                    if (!snapshotRoot.current) throw Error('snapshot-root')
                    const blobs = await snapshotPages(snapshotRoot.current)
                    blobs.forEach((blob, i) =>
                      saveBlob(
                        blob,
                        blobs.length === 1
                          ? 'session.png'
                          : `session-${i + 1}.png`,
                      ),
                    )
                  } finally {
                    setSnapshotOpen(false)
                  }
                })
              }
            >
              {text.snapshot}
            </button>
          </div>
        </header>
        <div className="history-toolbar">
          <a className="history-back" href={`/?${returnQuery}`}>
            <span aria-hidden="true">←</span> {text.home}
          </a>
          {film && (
            <div className="history-session-summary">
              <CraftingCostLine film={film} cursor={film.frames.length - 1} />
              <span className="history-state-count">
                {film.frames.length} {text.count}
              </span>
            </div>
          )}
        </div>
        {error && <p role="alert">{text.error}</p>}
        {data.isError && <p role="alert">{text.error}</p>}
        {!film && !data.isFetching && <p>{text.missing}</p>}
        {film && (
          <div className="session-flow">
            <img
              className="session-flow-background"
              src="/assets/currency/stash-background.webp"
              alt=""
              aria-hidden="true"
            />
            <SessionFlow film={film} resources={historyResources} />
          </div>
        )}
        {film && snapshotOpen && (
          <div className="history-snapshot-stage" aria-hidden="true">
            <div className="session-flow" ref={snapshotRoot}>
              <img
                className="session-flow-background"
                src="/assets/currency/stash-background.webp"
                alt=""
              />
              <SessionFlow
                film={film}
                resources={historyResources}
                columnCount={4}
              />
            </div>
          </div>
        )}
      </main>
    </CurrencyPriceContext.Provider>
  )
}
