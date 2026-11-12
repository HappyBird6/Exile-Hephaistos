import { expect, it } from 'vitest'
import {
  listParams,
  listQuery,
  authorHref,
  authorReturn,
} from './listNavigation'
it('applies the current version only when no version was selected, and excludes view context from API keys', () => {
  expect(listQuery(listParams('?selected=one', '0.5.5'))).toBe('version=0.5.5')
  expect(listQuery(listParams('?version=&selected=one', '0.5.5'))).toBe('')
  expect(
    listQuery(
      listParams(
        '?sort=popular&authorFrom=%2Fcommunity&author=one&version=0.5.5',
      ),
    ),
  ).toBe(listQuery(listParams('?version=0.5.5&author=one&sort=popular')))
})
it('returns to the original board filters, page and preview even after changing author', () => {
  const original =
    '?version=0.5.5&sort=popular&itemClass=Amulets&page=2&selected=post'
  const first = authorHref('member-a', '/community', original)
  expect(authorReturn(first.slice(first.indexOf('?')))).toBe(
    '/community' + original,
  )
  const second = authorHref(
    'member-b',
    '/community',
    first.slice(first.indexOf('?')),
  )
  expect(authorReturn(second.slice(second.indexOf('?')))).toBe(
    '/community' + original,
  )
  expect(authorReturn('?authorFrom=https%3A%2F%2Fevil.test')).toBe('/community')
  expect(authorReturn('?authorFrom=%2F%2Fevil.test')).toBe('/community')
})
