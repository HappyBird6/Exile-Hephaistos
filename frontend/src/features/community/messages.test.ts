import { expect, it } from 'vitest'
import { locales } from '../../shared/i18n/i18n'
import { revisionText } from './revisionMessages'
import { communityText } from './messages'
it('provides every board label in all supported languages', () => {
  for (const locale of locales) {
    const text = { ...communityText(locale), ...revisionText(locale) }
    expect(Object.keys(text)).toEqual(
      Object.keys({ ...communityText('en'), ...revisionText('en') }),
    )
    for (const value of Object.values(text))
      expect(typeof value === 'string' && value.length > 0).toBe(true)
  }
})
