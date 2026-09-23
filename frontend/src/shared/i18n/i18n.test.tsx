import { fireEvent, render, screen } from '@testing-library/react'
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
describe('언어 설정', () => {
  it('저장 설정, 지원 브라우저 언어, 영어 fallback 순으로 선택한다', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR', 'ko-KR'])
    expect(initialLocale()).toBe('ko')
    localStorage.setItem(localeStorageKey, 'en')
    expect(initialLocale()).toBe('en')
    localStorage.setItem(localeStorageKey, 'unsupported')
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR'])
    expect(initialLocale()).toBe('en')
  })
  it('저장소 읽기·쓰기가 실패해도 즉시 언어를 전환한다', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('Denied')
    })
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('Denied')
    })
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['ko-KR'])
    render(
      <LocaleProvider>
        <Example />
      </LocaleProvider>,
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      '아이템은 변경되지 않았습니다',
    )
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(screen.getByLabelText('Language')).toHaveValue('en')
    expect(screen.getByRole('status')).toHaveTextContent(
      'The item has not changed',
    )
    expect(document.documentElement.lang).toBe('en')
  })
  it('설정 언어만 저장하고 다음 mount에서 복원한다', () => {
    const view = render(
      <LocaleProvider initialLanguage="ko">
        <Example />
      </LocaleProvider>,
    )
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(localStorage.length).toBe(1)
    expect(localStorage.getItem(localeStorageKey)).toBe('en')
    view.unmount()
    render(
      <LocaleProvider>
        <Example />
      </LocaleProvider>,
    )
    expect(screen.getByLabelText('Language')).toHaveValue('en')
  })
  it('모든 번역의 placeholder와 화폐 label key가 일치한다', () => {
    for (const key of Object.keys(locales.ko.messages)) {
      const ko =
        Object.entries(locales.ko.messages).find(
          ([entry]) => entry === key,
        )?.[1] ?? ''
      const en =
        Object.entries(locales.en.messages).find(
          ([entry]) => entry === key,
        )?.[1] ?? ''
      expect(
        [...ko.matchAll(/\{\w+\}/g)].map((match) => match[0]).sort(),
      ).toEqual([...en.matchAll(/\{\w+\}/g)].map((match) => match[0]).sort())
    }
    expect(Object.keys(locales.en.currencies)).toEqual(
      Object.keys(locales.ko.currencies),
    )
    expect(translate('en', 'currencyCount', { count: 32 })).toBe(
      '32 currencies',
    )
  })
})
