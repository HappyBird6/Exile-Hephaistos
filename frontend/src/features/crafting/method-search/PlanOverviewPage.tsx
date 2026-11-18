import { useQuery } from '@tanstack/react-query'
import { useI18n } from '../../../shared/i18n/i18n'
import { baseClass, baseKeyForItem } from '../baseRegistry'
import { loadInitial } from '../craftingApi'
import { createItemPresentation } from '../path-tree/presentation'
import { historyMessages } from '../session-history/messages'
import { SessionOverviewLayout } from '../session-history/SessionOverviewLayout'
import { automaticApi } from './automaticApi'
import { presentAutomaticItem } from './automaticTierLaws'
import { rankedPlans } from './rankedPlans'
import { SessionSteps } from './RankedCraftingSessions'
import type { Selection } from './supportStorage'
import { usePlanAction } from './usePlanAction'

export function PlanOverviewPage({ id }: { id: string }) {
  const { locale, name } = useI18n()
  const action = usePlanAction()
  const params = new URLSearchParams(location.search)
  const identity = params.get('rules') ?? ''
  const text = historyMessages[locale]
  const query = useQuery({
    queryKey: ['plan-overview', id, identity],
    retry: false,
    queryFn: async ({ signal }) => {
      const view = await automaticApi.get(id, identity, signal)
      const item = view.nodes[0]?.item
      const key = item && baseKeyForItem(item.baseItemId)
      if (!item || !key) throw Error('missing-base')
      return {
        view,
        item,
        key,
        initial: await loadInitial(item.itemLevel, signal, key),
      }
    },
  })
  const result = query.data
  const cycles = Number(params.get('cycles')) || 100
  const plan =
    result &&
    rankedPlans(result.view, cycles).find((p) => p.id === params.get('plan'))
  if (!result || !plan)
    return (
      <SessionOverviewLayout
        title={text.title}
        backLabel={text.home}
        backHref="/?workbench=support"
      >
        <p role="status">{query.isPending ? '…' : text.missing}</p>
      </SessionOverviewLayout>
    )
  const { view, item, key, initial } = result
  const present = createItemPresentation(
    { name: name(item.baseItemId, key), itemClass: baseClass(key) },
    initial.modifiers,
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
    <SessionSteps
      overview
      plan={plan}
      action={action}
      view={view}
      start={item}
      identity={identity}
      allowStart={false}
      cycles={cycles}
      route={route}
      terminal={terminal}
      setTerminal={() => {}}
      onRoute={() => {}}
      card={(n) => presentAutomaticItem(n, locale, present, view.nodes)}
    />
  )
}
