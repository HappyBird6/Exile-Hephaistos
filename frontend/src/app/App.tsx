import { SessionOverviewPage } from '../features/crafting/session-history/SessionOverviewPage'
import { useI18n } from '../shared/i18n/i18n'
import { useEffect } from 'react'
import { CraftingPage } from '../features/crafting/CraftingPage'
import { LocaleSelector } from '../shared/i18n/LocaleSelector'
import '../features/crafting/crafting.css'

export function App() {
  const { t } = useI18n()
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
      <main className="craft-page craft-page--framed">
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
          <nav className="workspace-nav" aria-label={t('ui.main_navigation')}>
            <a href="/">{t('ui.crafting_workbench')}</a>
            <a className="workspace-admin" href="/admin" aria-current="page">
              {t('ui.admin')}
            </a>
          </nav>
          <LocaleSelector />
        </header>
        <h1>{t('ui.admin')}</h1>
      </main>
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
    return <SessionOverviewPage id={id} />
  }
  return <CraftingPage />
}
