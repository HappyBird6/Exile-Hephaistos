import { useEffect, type PropsWithChildren } from 'react'
import { flushDownloads } from './downloads'
import { SiteHeader } from '../../shared/site/SiteHeader'
import { useI18n } from '../../shared/i18n/i18n'
import { sessionLibraryMessages } from '../crafting/session-library/sessionLibraryMessages'
import { CurrencyPriceContext } from '../crafting/currency-prices/context'
import { useCurrencyPrices } from '../crafting/currency-prices/useCurrencyPrices'
import { CurrencyTicker } from '../crafting/currency-prices/CurrencyPrices'
import { communityText } from './messages'
import '../crafting/crafting.css'
import '../crafting/session-library/session-library.css'
import '../crafting/compact-viewport/mobileWorkbench.css'
import './community.css'
export function CommunityShell({ children }: PropsWithChildren) {
  const { locale, t } = useI18n(),
    text = communityText(locale),
    market = useCurrencyPrices()
  useEffect(() => {
    void flushDownloads()
  }, [])
  return (
    <CurrencyPriceContext.Provider value={market}>
      <main className="craft-page craft-page--framed community-page site-controls">
        <SiteHeader
          navigation={
            <nav className="workspace-nav" aria-label={t('ui.main_navigation')}>
              <div className="site-workspace-links">
                <a href="/?workbench=simulation">{t('workspace.workbench')}</a>
                {import.meta.env.VITE_CRAFT_SUPPORT_ENABLED !== 'false' && (
                  <a href="/?workbench=support">{t('workspace.support')}</a>
                )}
                <a href="/?workbench=history">
                  {sessionLibraryMessages[locale].history}
                </a>
                <a href="/community" aria-current="page">
                  {text.board}
                </a>
              </div>
              <CurrencyTicker />
            </nav>
          }
        />
        <div className="community-content">{children}</div>
      </main>
    </CurrencyPriceContext.Provider>
  )
}
