import { expect, it } from 'vitest'
import type { Locale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { emptyFilms, startFilm } from '../workbenchHistory'
import { sessionTitle } from './sessionTitle'
function film() {
  return {
    ...startFilm(
      emptyFilms(),
      {
        ...concreteInitial(initialFixture),
        baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
      },
      'one',
      initialFixture.rulesetIdentity,
    ).films[0]!,
    createdAt: '2026-11-06T12:00:00Z',
  }
}
it.each<[Locale, string]>([
  ['en', '11/06/2026'],
  ['ko', '2026.11.06'],
  ['zh-CN', '2026/11/06'],
  ['zh-TW', '2026/11/06'],
  ['ja', '2026/11/06'],
  ['es', '06/11/2026'],
])(
  'formats a derived item/date title for %s without writing metadata',
  (locale, date) => {
    const record = film()
    const before = JSON.stringify(record)
    expect(sessionTitle(record, locale)).toMatch(
      new RegExp('\\(' + date.replaceAll('.', '\\.') + ' .+\\)$'),
    )
    expect(JSON.stringify(record)).toBe(before)
    expect(record.title).toBeUndefined()
  },
)
it('uses the item name and preserves manually assigned titles', () => {
  const record = film()
  expect(sessionTitle(record, 'en')).toBe(
    `Solar Amulet(11/06/2026 ${new Intl.DateTimeFormat('en', { hour: '2-digit', minute: '2-digit' }).format(new Date(record.createdAt))})`,
  )
  expect(sessionTitle({ ...record, title: 'My named craft' }, 'ko')).toBe(
    'My named craft',
  )
  expect(sessionTitle({ ...record, createdAt: 'unknown' }, 'en')).toBe(
    'Solar Amulet',
  )
})
