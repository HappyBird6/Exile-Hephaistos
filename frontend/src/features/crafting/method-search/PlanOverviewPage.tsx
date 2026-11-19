import type { Film } from '../workbenchHistory'
import { SessionOverviewPage } from '../session-history/SessionOverviewPage'
import { CurrencyPriceContext } from '../currency-prices/context'
import { useCurrencyPrices } from '../currency-prices/useCurrencyPrices'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { historyMessages } from '../session-history/messages'
import { SessionOverviewLayout } from '../session-history/SessionOverviewLayout'
import { automaticApi } from './automaticApi'
import { rankedPlans } from './rankedPlans'
import { SessionSteps } from './RankedCraftingSessions'
import type { Selection } from './supportStorage'
import { usePlanAction } from './usePlanAction'

export function PlanOverviewPage({ id }: { id: string }) {
  const { locale } = useI18n()
  const market = useCurrencyPrices()
  const action = usePlanAction()
  const params = new URLSearchParams(location.search)
  const identity = params.get('rules') ?? ''
  const text = historyMessages[locale]
  const film = useQueryClient().getQueryData<Film>([
    'plan-film',
    location.pathname + location.search,
  ])
  const query = useQuery({
    queryKey: ['plan-overview', id, identity],
    retry: false,
    enabled: !film,
    queryFn: ({ signal }) => automaticApi.get(id, identity, signal),
  })
  const view = query.data
  const cycles = Number(params.get('cycles')) || 100
  const plan =
    view && rankedPlans(view, cycles).find((p) => p.id === params.get('plan'))
  if (film)
    return (
      <SessionOverviewPage
        id={id}
        source={{
          film,
          title: film.title ?? text.title,
          backHref:
            '/?' +
            new URLSearchParams({
              workbench: 'support',
              job: id,
              plan: params.get('plan') ?? '',
            }),
          backLabel: text.home,
        }}
      />
    )
  if (!view || !plan)
    return (
      <SessionOverviewLayout
        title={text.title}
        backLabel={text.home}
        backHref="/?workbench=support"
      >
        <p role="status">{query.isPending ? '…' : text.missing}</p>
      </SessionOverviewLayout>
    )
  const route: Selection[] = []
  let terminal: { source: number; key: string } | null = null
  try {
    let source = 0
    for (const [kind, target] of JSON.parse(params.get('path') ?? '[]')) {
      const method = plan.decisions.get(source)
      if (
        !method ||
        typeof target !== 'number' ||
        typeof kind !== 'string' ||
        route.length >= 64
      )
        break
      route.push({ source, method, kind, target })
      source = target
    }
    const end = JSON.parse(params.get('terminal') ?? 'null')
    if (end?.source === source && typeof end.key === 'string') terminal = end
  } catch {
    /* Links with missing outcome data still show the starting item. */
  }
  return (
    <CurrencyPriceContext.Provider value={market}>
      <SessionSteps
        overview
        plan={plan}
        action={action}
        view={view}
        identity={identity}
        allowStart={false}
        cycles={cycles}
        route={route}
        terminal={terminal}
        setTerminal={() => {}}
        onRoute={() => {}}
      />
    </CurrencyPriceContext.Provider>
  )
}
