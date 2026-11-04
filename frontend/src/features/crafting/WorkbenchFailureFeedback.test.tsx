import { readFileSync } from 'node:fs'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import {
  fixtureFetch,
  initialFixture,
} from '../../shared/test/craftingFixtures'
import { currencyButton } from '../../shared/test/workbenchQueries'
import { historyStorageKey } from './workbenchHistory'
import { setLocale, locales, translate } from '../../shared/i18n/i18n'
import { serviceText } from './serviceMessages'
import { abyssCurrencyReason } from './abyss-workbench/currencyMessages'
import { boneText } from './desecrationCurrency'

let client: QueryClient
beforeEach(() => {
  localStorage.clear()
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  vi.stubGlobal('fetch', fixtureFetch)
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
function show() {
  render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const panel = () =>
  document.querySelector<HTMLElement>('.workbench-feedback-panel')
function apply(name: string) {
  fireEvent.click(currencyButton(name))
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
}
it('keeps currency explanations out of selection and successful crafting while saving the result', async () => {
  show()
  expect(panel()).toBeNull()
  fireEvent.click(currencyButton('Orb of Transmutation'))
  expect(panel()).toBeNull()
  fireEvent.click(
    screen.getByRole('button', {
      name: 'Use selected currency on the central item',
    }),
  )
  await waitFor(() =>
    expect(localStorage.getItem(historyStorageKey)).toContain('MAGIC'),
  )
  expect(panel()).toBeNull()
})
it('shows only a failed-use reason, preserves history and clears it on a new selection', async () => {
  show()
  const style = document.createElement('style')
  style.textContent = readFileSync(
    'src/features/crafting/compact-viewport/compactViewport.css',
    'utf8',
  )
  document.head.append(style)
  const region = document.querySelector<HTMLElement>(
    '.workbench-feedback-region',
  )!
  const history = region.previousElementSibling
  expect(history).toHaveClass('wb-session-notation')
  expect(region.nextElementSibling).toBeNull()
  expect(getComputedStyle(region).height).toBe('200px')
  style.remove()
  const before = localStorage.getItem(historyStorageKey)
  useItemDraft.getState().setActiveOmens(['Omen_of_Whittling'])
  apply('Orb of Annulment')
  await waitFor(() => expect(panel()).toHaveAttribute('role', 'alert'))
  expect(panel()?.textContent?.trim()).not.toBe('')
  expect(document.querySelector('.bench-lower')).toContainElement(panel())
  expect(
    document.querySelector('.stash-viewport .workbench-feedback-panel'),
  ).toBeNull()
  expect(localStorage.getItem(historyStorageKey)).toBe(before)
  expect(useItemDraft.getState().activeOmens).toEqual(['Omen_of_Whittling'])
  expect(panel()).not.toHaveTextContent(/unchanged|preserved/)
  expect(screen.getByRole('status')).not.toHaveTextContent(
    /unchanged|preserved/,
  )
  expect(panel()?.parentElement).toBe(region)
  expect(region.previousElementSibling).toBe(history)
  fireEvent.click(currencyButton('Orb of Transmutation'))
  expect(panel()).toBeNull()
  expect(document.querySelector('.workbench-feedback-region')).toBe(region)
  expect(region.previousElementSibling).toBe(history)
})

it.each(locales)(
  'removes only preservation notices from %s failure paths and keeps their reasons',
  (locale) => {
    const reason = 'This currency cannot be used on this rarity.'
    const legacy = `Craft blocked by rule: ${reason} Your item is unchanged; active omens are preserved.`
    const current = `Craft blocked by rule: ${reason}`
    expect(serviceText(legacy, locale)).toBe(serviceText(current, locale))
    expect(serviceText(current, locale)).toContain(serviceText(reason, locale))
    const notices = [
      ...(
        [
          'notice.no_rule',
          'notice.wait_catalog',
          'notice.apply_error',
          'error.apply_retry',
        ] as const
      ).map((key) => translate(key, {}, locale)),
      serviceText(legacy, locale),
      serviceText(
        'Could not verify the applied item. Your item is unchanged. Please retry.',
        locale,
      ),
      abyssCurrencyReason('UNSUPPORTED_CURRENCY', locale),
      boneText[locale][6],
    ]
    for (const notice of notices) {
      expect(notice).not.toMatch(
        /unchanged|has not changed|no ha cambiado|no cambia|未更改|未改变|未變更|未改變|保持不变|保持不變|変更され|保持され|변경되지|그대로|유지됩니다|se conservan|no cambian/,
      )
      expect(notice).not.toBe('')
    }
    expect(
      serviceText(
        'Could not apply currency. Your item is unchanged. Please retry.',
        locale,
      ),
    ).toBe(translate('error.apply_retry', {}, locale))
  },
)

it('shows the Korean rule failure without redundant preservation text in feedback or the live announcement', async () => {
  setLocale('ko')
  show()
  fireEvent.click(
    document.querySelector('[data-material-tooltip="Orb_of_Annulment"]')!,
  )
  fireEvent.click(
    screen.getByRole('button', { name: '선택한 화폐를 중앙 아이템에 사용' }),
  )
  await waitFor(() => expect(panel()).toHaveAttribute('role', 'alert'))
  expect(panel()).toHaveTextContent('규칙에 의해 제작 차단:')
  expect(panel()).not.toHaveTextContent(/아이템은 변경되지|활성 징조는 유지/)
  expect(screen.getByRole('status')).not.toHaveTextContent(
    /아이템은 변경되지|활성 징조는 유지/,
  )
})
