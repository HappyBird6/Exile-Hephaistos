import { useQuery } from '@tanstack/react-query'
import { SiteHeader } from '../../../shared/site/SiteHeader'
import { SiteFooter } from '../../../shared/site/SiteFooter'
import { WorkspaceNavigation } from '../../../shared/site/WorkspaceNavigation'
import { useI18n } from '../../../shared/i18n/i18n'
import { CraftStart } from '../goal-filter/CraftStart'
import { RagStrategySearch } from './RagStrategySearch'
import { ragMessages } from './messages'
import { ragAvailability } from './api'
import { RagAvailabilityContext } from './availabilityContext'
import './rag-test.css'

export function SupportRagTestPage() {
  const { locale } = useI18n(),
    copy = ragMessages[locale]
  const status = useQuery({
    queryKey: ['support-rag', 'availability'],
    queryFn: ({ signal }) => ragAvailability(signal),
    staleTime: 60_000,
    retry: false,
    refetchOnWindowFocus: false,
  })
  const ready = Boolean(status.data?.enabled && status.data.configured)
  return (
    <main className="craft-page craft-page--framed rag-test-page">
      <SiteHeader navigation={<WorkspaceNavigation active="support-test" />} />
      <section
        id="panel-support-test"
        className="workspace-panel rag-test-panel"
        role="tabpanel"
        aria-labelledby="workspace-support-test"
      >
        <header className="rag-test-heading">
          <div>
            <h1>{copy.title}</h1>
            <p>{copy.intro}</p>
          </div>
          <span className="rag-test-badge">RAG</span>
        </header>
        {!status.isPending && !ready && (
          <div className="rag-test-notice" role="status">
            <p>{status.isError ? copy.error : copy.configure}</p>
            <button
              onClick={() => void status.refetch()}
              disabled={status.isFetching}
            >
              {copy.retry}
            </button>
          </div>
        )}
        <RagAvailabilityContext.Provider value={ready}>
          <CraftStart
            active
            persistDraft={false}
            SearchComponent={RagStrategySearch}
            startLabel={copy.confirm}
          />
        </RagAvailabilityContext.Provider>
      </section>
      <SiteFooter />
    </main>
  )
}
