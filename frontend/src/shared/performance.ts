/** Browser operational budgets; these never change game rules or random outcomes. */
export const performanceSettings = {
  // Parsed immutable catalogs shared by base pickers and overview cards (catalogReference).
  catalogRetentionEntries: 32,
  catalogRequestTimeoutMs: 15_000,
  // Immutable reference retention in TanStack Query (ModifierPoolPanel).
  referenceRetentionMs: 10 * 60_000,
  // Wait for rapid item changes to settle before requesting a visible pool (ModifierPoolPanel).
  poolDebounceMs: 120,
  // Read shared prices every five minutes while visible (useCurrencyPrices).
  pricePollMs: 5 * 60_000,
  // Briefly poll after manual refresh, which runs outside the HTTP request (useCurrencyPrices).
  priceRefreshPollMs: 2_000,
  priceRefreshWindowMs: 60_000,
  // Yield during expensive DOM copying (session-history/snapshot).
  renderingSliceMs: 8,
} as const

export const yieldToBrowser = () =>
  new Promise<void>((resolve) => setTimeout(resolve, 0))
