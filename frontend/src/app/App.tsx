const SupportTestPage = lazy(
  () => import('../features/crafting/support-test/SupportTestPage'),
)
const AcquisitionOverviewPage = lazy(
  () => import('../features/crafting/support-test/AcquisitionOverviewPage'),
)
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
const CommunityOverview = lazy(() =>
  import('../features/community/CommunityOverview').then((m) => ({
    default: m.CommunityOverview,
  })),
)
const CommunityDetail = lazy(() =>
  import('../features/community/CommunityDetail').then((m) => ({
    default: m.CommunityDetail,
  })),
)
import { useAppLocation } from '../shared/navigation/navigation'
import { AccountBootstrap } from '../features/account/AccountBootstrap'
const AdminPage = lazy(() =>
  import('../features/admin/AdminPage').then((m) => ({ default: m.AdminPage })),
)
const PlanOverviewPage = lazy(() =>
  import('../features/crafting/method-search/PlanOverviewPage').then((m) => ({
    default: m.PlanOverviewPage,
  })),
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
  useAppLocation()
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
  useAppLocation()
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

  const plan = location.pathname.match(
    /^\/crafting-plans\/([a-zA-Z0-9-]+)\/overview$/,
  )
  if (plan)
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <PlanOverviewPage id={plan[1]!} />
      </Suspense>
    )

  if (location.pathname === '/support-test')
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <SupportTestPage />
      </Suspense>
    )

  if (location.pathname === '/support-test/overview')
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <AcquisitionOverviewPage />
      </Suspense>
    )

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
  const overview = location.pathname.match(
    /^\/community\/post\/([0-9a-f-]+)\/overview$/i,
  )
  if (overview)
    return (
      <Suspense fallback={<p role="status">…</p>}>
        <CommunityOverview id={overview[1]!} />
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
