const AdminPage = lazy(() =>
  import('../features/admin/AdminPage').then((m) => ({ default: m.AdminPage })),
)
const SessionOverviewPage = lazy(() =>
  import('../features/crafting/session-history/SessionOverviewPage').then(
    (m) => ({ default: m.SessionOverviewPage }),
  ),
)
import { useI18n } from '../shared/i18n/i18n'
import { useEffect, lazy, Suspense } from 'react'
const CraftingPage = lazy(() =>
  import('../features/crafting/CraftingPage').then((m) => ({
    default: m.CraftingPage,
  })),
)
import '../features/crafting/crafting.css'

export function App() {
  const { locale } = useI18n()
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  useEffect(() => {
    if (window.location.pathname === '/admin/crawling') {
      window.history.replaceState(null, '', '/admin')
    }
  }, [])
  if (
    window.location.pathname === '/admin' ||
    window.location.pathname === '/admin/crawling'
  ) {
    return (
      <Suspense fallback={<div role="status">…</div>}>
        <AdminPage />
      </Suspense>
    )
  }

  const local = window.location.pathname.match(/^\/history\/([^/]+)$/)
  if (local) {
    let id = ''
    try {
      id = decodeURIComponent(local[1]!)
    } catch {
      /* Invalid URL stays unavailable. */
    }
    return (
      <Suspense
        fallback={
          <div role="status" aria-busy="true">
            …
          </div>
        }
      >
        <SessionOverviewPage id={id} />
      </Suspense>
    )
  }
  return (
    <Suspense
      fallback={
        <div role="status" aria-busy="true">
          …
        </div>
      }
    >
      <CraftingPage />
    </Suspense>
  )
}
