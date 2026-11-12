const CommunityList = lazy(() =>
  import('../features/community/CommunityList').then((m) => ({
    default: m.CommunityList,
  })),
)
const CommunityCompose = lazy(() =>
  import('../features/community/CommunityCompose').then((m) => ({
    default: m.CommunityCompose,
  })),
)
const CommunityDetail = lazy(() =>
  import('../features/community/CommunityDetail').then((m) => ({
    default: m.CommunityDetail,
  })),
)
import { AccountBootstrap } from '../features/account/AccountBootstrap'
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

  if (location.pathname.startsWith('/admin')) return <AppContent />
  return (
    <AccountBootstrap>
      <AppContent />
    </AccountBootstrap>
  )
}
function AppContent() {
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

  if (location.pathname === '/community')
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <CommunityList />
      </Suspense>
    )
  if (location.pathname === '/community/new')
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <CommunityCompose />
      </Suspense>
    )
  const post = location.pathname.match(/^\/community\/post\/([0-9a-f-]+)$/i)
  if (post)
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <CommunityDetail id={post[1]!} />
      </Suspense>
    )
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
