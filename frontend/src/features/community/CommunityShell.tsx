import { WorkspaceNavigation } from '../../shared/site/WorkspaceNavigation'
import { useEffect, type PropsWithChildren } from 'react'
import { flushDownloads } from './downloads'
import { SiteHeader } from '../../shared/site/SiteHeader'
import { CurrencyPriceContext } from '../crafting/currency-prices/context'
import { useCurrencyPrices } from '../crafting/currency-prices/useCurrencyPrices'
import '../crafting/crafting.css'
import '../crafting/session-library/session-library.css'
import '../crafting/compact-viewport/mobileWorkbench.css'
import './community.css'
export function CommunityShell({ children }: PropsWithChildren) {
  const market = useCurrencyPrices()
  useEffect(() => {
    void flushDownloads()
  }, [])
  return (
    <CurrencyPriceContext.Provider value={market}>
      <main className="craft-page craft-page--framed community-page site-controls">
        <SiteHeader navigation={<WorkspaceNavigation active="community" />} />
        <div
          className="community-content"
          id="community-panel"
          role="tabpanel"
          aria-labelledby="workspace-community"
        >
          {children}
        </div>
      </main>
    </CurrencyPriceContext.Provider>
  )
}
