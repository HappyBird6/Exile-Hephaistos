import { SiteHeader } from '../../../shared/site/SiteHeader'
import { SiteFooter } from '../../../shared/site/SiteFooter'
import { WorkspaceNavigation } from '../../../shared/site/WorkspaceNavigation'
import { CurrencyPriceContext } from '../currency-prices/context'
import { useCurrencyPrices } from '../currency-prices/useCurrencyPrices'
import { CraftStart } from '../goal-filter/CraftStart'
import { AcquisitionRouteResults } from '../method-search/AcquisitionRouteResults'

export default function SupportTestPage() {
  const market = useCurrencyPrices()
  return (
    <CurrencyPriceContext.Provider value={market}>
      <main className="craft-page craft-page--framed site-controls">
        <SiteHeader
          navigation={<WorkspaceNavigation active="support-test" />}
        />
        <section
          role="tabpanel"
          id="panel-support-test"
          aria-labelledby="workspace-support-test"
          className="workspace-panel"
        >
          <CraftStart active testResults={AcquisitionRouteResults} />
        </section>
        <SiteFooter />
      </main>
    </CurrencyPriceContext.Provider>
  )
}
