import type { Film } from '../workbenchHistory'
import {
  SessionOverviewLayout,
  SessionFlowSurface,
} from './SessionOverviewLayout'
import { accountScope } from '../../account/auth'
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
import '../compact-viewport/mobileWorkbench.css'

export function SessionOverviewPage({
  id,
  source,
}: {
  id: string
  source?: {
    film: Film
    title: string
    backHref: string
    backLabel: string
  }
}) {
  const { locale } = useI18n()
  const market = useCurrencyPrices()
  const text = historyMessages[locale]
  const catalogsLoading = useIsFetching({ queryKey: ['crafting', 'initial'] })
  const [error, setError] = useState(false)
  const [busy, setBusy] = useState(false)
  const [snapshotOpen, setSnapshotOpen] = useState(false)
  const snapshotRoot = useRef<HTMLDivElement>(null)
  const data = useQuery({
    queryKey: ['session-overview', id],
    enabled: !source,
    retry: false,
    queryFn: async () => {
      const repository = new IndexedFilmRepository(
        localStorage,
        globalThis.indexedDB,
        accountScope(),
      )
      try {
        return (
          (await repository.load()).films.find((film) => film.id === id) ?? null
        )
      } finally {
        await repository.close()
      }
    },
  })
  const film = source?.film ?? data.data
  const title =
    source?.title ?? (film ? sessionTitle(film, locale) : text.title)
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
      <SessionOverviewLayout
        title={title}
        backHref={source?.backHref ?? `/?${returnQuery}`}
        backLabel={source?.backLabel ?? text.home}
        actions={
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
        }
        summary={
          film && (
            <div className="history-session-summary">
              <CraftingCostLine
                film={film}
                cursor={film.frames.length - 1}
                sessionSummary
              />
              <span className="history-state-count">
                {film.frames.length} {text.count}
              </span>
            </div>
          )
        }
      >
        {error && <p role="alert">{text.error}</p>}
        {!source && data.isError && <p role="alert">{text.error}</p>}
        {!film && !data.isFetching && <p>{text.missing}</p>}
        {film && (
          <SessionFlowSurface>
            <SessionFlow film={film} resources={historyResources} />
          </SessionFlowSurface>
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
      </SessionOverviewLayout>
    </CurrencyPriceContext.Provider>
  )
}
