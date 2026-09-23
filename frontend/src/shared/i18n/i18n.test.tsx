import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from './LocaleProvider'
import { LanguageSelector } from './LanguageSelector'
import { useI18n } from './context'
import { initialLocale, localeStorageKey, locales, translate } from './messages'
function Example() {
  const { t } = useI18n()
  return (
    <>
      <LanguageSelector />
      <p role="status">{t('noticeApply', { name: 'Example' })}</p>
    </>
  )
}
afterEach(() => {
  vi.restoreAllMocks()
  localStorage.clear()
})
describe('English locale', () => {
  it('falls back to English for old Korean preferences and unsupported browser languages', () => {
    localStorage.setItem(localeStorageKey, 'ko')
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['ko-KR', 'fr-FR'])
    expect(initialLocale()).toBe('en')
    expect(Object.keys(locales)).toEqual(['en'])
  })
  it('works with blocked storage and hides the selector for one locale', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Denied')
    })
    render(
      <LocaleProvider>
        <Example />
      </LocaleProvider>,
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'The item has not changed',
    )
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
    expect(document.documentElement.lang).toBe('en')
  })
  it('retains parameterized messages for future locale additions', () => {
    expect(translate('en', 'currencyCount', { count: 32 })).toBe(
      '32 currencies',
    )
  })
})
