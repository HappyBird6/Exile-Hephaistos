import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { loadCurrencyPrices } from './api'
import type { CurrencyMarket } from './context'
const key = ['economy', 'prices'] as const
export function useCurrencyPrices(): CurrencyMarket {
  const client = useQueryClient()
  const mutation = useMutation({
    mutationFn: () => loadCurrencyPrices(undefined, true),
    onMutate: () => client.cancelQueries({ queryKey: key, exact: true }),
    onSuccess: (data) => {
      client.setQueryData(key, data)
    },
  })
  const query = useQuery({
    queryKey: key,
    queryFn: ({ signal }) => loadCurrencyPrices(signal),
    enabled: !mutation.isPending,
    retry: false,
    staleTime: 60_000,
    refetchInterval: 60_000,
  })
  return {
    data: query.data,
    pending: query.isPending,
    refreshing: mutation.isPending || query.isFetching,
    error: query.isError || mutation.isError,
    refresh: () => {
      if (!mutation.isPending) mutation.mutate()
    },
  }
}
