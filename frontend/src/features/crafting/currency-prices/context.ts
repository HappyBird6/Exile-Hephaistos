import { createContext, useContext } from 'react'
import type { PriceView } from './api'
export type CurrencyMarket = {
  data: PriceView | undefined
  pending: boolean
  refreshing: boolean
  refresh: () => void
  error: boolean
}
export const CurrencyPriceContext = createContext<CurrencyMarket | null>(null)
export const useCurrencyMarket = () => useContext(CurrencyPriceContext)
