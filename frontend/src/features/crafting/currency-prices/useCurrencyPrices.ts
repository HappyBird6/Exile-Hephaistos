import { useRef } from 'react'
import { performanceSettings as budget } from '../../../shared/performance'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { loadCurrencyPrices } from './api'
import type { CurrencyMarket } from './context'
const key = ['economy', 'prices'] as const
export function useCurrencyPrices(): CurrencyMarket {
  const client = useQueryClient()
  const refreshUntil = useRef(0)
  const mutation = useMutation({
    mutationFn: () => loadCurrencyPrices(undefined, true),
    onMutate: () => client.cancelQueries({ queryKey: key, exact: true }),
    onSuccess: (data) => {
      refreshUntil.current = data.refreshing
        ? Date.now() + budget.priceRefreshWindowMs
        : 0
      client.setQueryData(key, data)
    },
  })
  const query = useQuery({
    queryKey: key,
    queryFn: async ({ signal }) => {
      const data = await loadCurrencyPrices(
        signal,
        false,
        Date.now() < refreshUntil.current,
        client.getQueryData<import('./api').PriceView>(key),
      )
      if (!data.refreshing) refreshUntil.current = 0
      else if (!refreshUntil.current)
        refreshUntil.current = Date.now() + budget.priceRefreshWindowMs
      return data
    },
    enabled: !mutation.isPending,
    retry: false,
    staleTime: budget.pricePollMs,
    refetchInterval: () =>
      Date.now() < refreshUntil.current
        ? budget.priceRefreshPollMs
        : budget.pricePollMs,
    refetchIntervalInBackground: false,
  })
  return {
    data: query.data,
    pending: query.isPending,
    refreshing:
      mutation.isPending || query.isFetching || query.data?.refreshing === true,
    error: query.isError || mutation.isError,
    refresh: () => {
      if (!mutation.isPending) mutation.mutate()
    },
  }
}
