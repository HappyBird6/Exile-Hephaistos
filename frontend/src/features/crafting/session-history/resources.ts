import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import { materials, additionalStashCurrencies } from '../materials'

export const historyResources = [
  ...currencies.map((currency) => ({
    ...currency,
    name: currencyNames[currency.id] ?? currency.id,
    category: 'Currency' as const,
  })),
  ...materials,
  ...additionalStashCurrencies,
]
