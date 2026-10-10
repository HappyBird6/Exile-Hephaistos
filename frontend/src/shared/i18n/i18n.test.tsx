import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import messages from './messages.json'
import terms from './gameTerms.json'
import { LocaleSelector } from './LocaleSelector'
import { CraftingPage } from '../../features/crafting/CraftingPage'
import { AppProviders } from '../../app/AppProviders'
import {
  defaultLocale,
  locales,
  localeStorageKey,
  resolveLocale,
  setLocale,
  translate,
  gameName,
  gameTerm,
  matchesGameName,
  formatNumber,
  formatPercent,
  formatDate,
  localizedSource,
} from './i18n'

describe('locale infrastructure', () => {
  it('defaults to Korean and validates persisted values', () => {
    expect(defaultLocale).toBe('ko')
    expect(resolveLocale(null)).toBe('ko')
    expect(resolveLocale('untrusted')).toBe('ko')
    for (const locale of locales) expect(resolveLocale(locale)).toBe(locale)
  })
  it('has matching keys and interpolation placeholders in all six UI dictionaries', () => {
    const keys = Object.keys(messages.en).sort()
    for (const locale of locales) {
      expect(Object.keys(messages[locale]).sort()).toEqual(keys)
      for (const key of keys as (keyof typeof messages.en)[]) {
        const placeholders = (value: string) =>
          (value.match(/\{\w+\}/g) ?? []).sort()
        expect(placeholders(messages[locale][key]), `${locale}:${key}`).toEqual(
          placeholders(messages.en[key]),
        )
      }
    }
  })
  it('interpolates and pluralizes service text', () => {
    expect(translate('crafts', { count: 1 }, 'en')).toBe('1 craft')
    expect(translate('crafts', { count: 2 }, 'en')).toBe('2 crafts')
    expect(translate('crafts', { count: 1 }, 'ko')).toBe('제작 1회')
    expect(translate('film.step', { step: 2, total: 3 }, 'es')).toBe(
      'Paso 2 / 3',
    )
  })
  it('joins game terms by canonical IDs and metadata, with English fallback', () => {
    expect(gameName('Orb_of_Transmutation', 'Orb of Transmutation', 'ko')).toBe(
      '진화의 오브',
    )
    expect(
      gameName('Metadata/Items/Amulets/FourAmulet9', 'Solar Amulet', 'ko'),
    ).toBe('태양의 목걸이')
    expect(gameName('unknown-id', 'Unverified English name', 'ja')).toBe(
      'Unverified English name',
    )
    expect(gameTerm('unknown-id', 'ja').fallback).toBe(true)
    for (const locale of locales) {
      for (const [id, term] of Object.entries(terms[locale])) {
        expect(term.itemKey, `${locale}:${id}`).toBe(
          terms.en[id as keyof typeof terms.en].itemKey,
        )
      }
    }
  })
  it('searches localized names, English names and canonical IDs', () => {
    expect(
      matchesGameName('Omen_of_Whittling', 'Omen of Whittling', '절사', 'ko'),
    ).toBe(true)
    expect(
      matchesGameName(
        'Omen_of_Whittling',
        'Omen of Whittling',
        'Whittling',
        'ko',
      ),
    ).toBe(true)
    expect(
      matchesGameName(
        'Omen_of_Whittling',
        'Omen of Whittling',
        'Omen_of_',
        'ko',
      ),
    ).toBe(true)
  })
  it('formats values and links using the selected locale', () => {
    expect(formatNumber(1234.5, undefined, 'es')).toBe(
      new Intl.NumberFormat('es').format(1234.5),
    )
    expect(formatPercent(0.125, 6, 'ja')).toBe('12.5%')
    expect(formatDate('2026-10-04T23:00:00Z', 'ko')).toBe(
      new Intl.DateTimeFormat('ko', {
        dateStyle: 'medium',
        timeZone: 'UTC',
      }).format(new Date('2026-10-04T23:00:00Z')),
    )
    expect(
      localizedSource('https://poe2db.tw/us/Omen_of_Whittling', 'zh-TW'),
    ).toBe('https://poe2db.tw/tw/Omen_of_Whittling')
  })
  it('orders the menu and changes language without rewriting saved craft data', () => {
    setLocale('ko')
    const saved = '{"version":1,"canonicalGameIds":["Orb_of_Transmutation"]}'
    window.localStorage.setItem('i18n-test-film', saved)
    render(<LocaleSelector />)
    fireEvent.click(screen.getByRole('button', { name: /언어/ }))
    expect(
      screen
        .getAllByRole('menuitemradio')
        .map((option) => option.getAttribute('lang')),
    ).toEqual([...locales])
    fireEvent.click(
      screen.getByRole('menuitemradio', { name: '简体中文 (CN)' }),
    )
    expect(document.documentElement.lang).toBe('zh-CN')
    expect(window.localStorage.getItem(localeStorageKey)).toBe('zh-CN')
    expect(window.localStorage.getItem('i18n-test-film')).toBe(saved)
    window.localStorage.removeItem('i18n-test-film')
  })
  it('keeps probability disclosure on calculation tabs and omits it from setup-only Support', () => {
    render(
      <AppProviders>
        <CraftingPage />
      </AppProviders>,
    )
    const disclaimer =
      'Model probability uses published PoE2DB table weights, not verified game odds.'
    for (const tab of ['Crafting Workbench', 'State explorer']) {
      fireEvent.click(screen.getByRole('tab', { name: new RegExp(tab) }))
      expect(screen.getByText(disclaimer)).toBeVisible()
    }
    fireEvent.click(screen.getByRole('tab', { name: 'Craft Support' }))
    expect(screen.queryByText(disclaimer)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'State explorer' }))
    fireEvent.click(screen.getByRole('button', { name: /Language/ }))
    fireEvent.click(screen.getByRole('menuitemradio', { name: '한국어' }))
    expect(
      screen.getByText(messages.ko['probability.disclaimer']),
    ).toBeVisible()
  })
})
