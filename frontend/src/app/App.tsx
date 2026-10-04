import { useI18n } from '../shared/i18n/i18n'
import { useEffect } from 'react'
import { CraftingPage } from '../features/crafting/CraftingPage'
import { LocaleSelector } from '../shared/i18n/LocaleSelector'

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
      <main>
        <nav aria-label={t('ui.main_navigation')}>
          <a href="/">{t('ui.crafting_workbench')}</a>
        </nav>
        <LocaleSelector />
        <h1>{t('ui.admin')}</h1>
        <p>{t('ui.no_admin_tools_are_available_yet')}</p>
      </main>
    )
  }
  return <CraftingPage />
}
