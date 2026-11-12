import { WorkspaceNavigation } from '../../shared/site/WorkspaceNavigation'
import { useEffect, type PropsWithChildren } from 'react'
import { flushDownloads } from './downloads'
import { useQueryClient } from '@tanstack/react-query'
import { accountScope } from '../account/auth'
import { cachePostStats } from './queries'
import { SiteHeader } from '../../shared/site/SiteHeader'
import { CurrencyPriceContext } from '../crafting/currency-prices/context'
import { useCurrencyPrices } from '../crafting/currency-prices/useCurrencyPrices'
import '../crafting/crafting.css'
import '../crafting/session-library/session-library.css'
import '../crafting/compact-viewport/mobileWorkbench.css'
import './community.css'
export function CommunityShell({ children }: PropsWithChildren) {
  const client = useQueryClient()
  const market = useCurrencyPrices()
  useEffect(() => {
    const owner = accountScope()
    void flushDownloads((id, stats) => {
      if (owner === accountScope()) cachePostStats(client, id, owner, stats)
    })
  }, [client])
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
