import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { LanguageSelector } from '../../shared/i18n/LanguageSelector'
import { ItemCard } from './ItemCard'
import type { ItemCardData } from './itemCardData'
import { toItemCard } from './itemCardData'
import type { ParsedItemText } from './itemTextApi'

const base: ItemCardData = {
  rarity: 'NORMAL',
  name: 'Synthetic Base',
  base: 'Synthetic Base',
  itemClass: 'Synthetic Amulet',
  itemLevel: null,
  properties: [],
  requirements: [],
  modifiers: [],
  flags: [],
}
function card(item: ItemCardData) {
  return (
    <LocaleProvider initialLanguage="ko">
      <LanguageSelector />
      <ItemCard item={item} />
    </LocaleProvider>
  )
}
describe('ItemCard', () => {
  it('현재 상태 교체로 일반 → 매직 → 레어 이름·값·옵션·플래그를 함께 바꾼다', () => {
    const view = render(card(base))
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(screen.getByText('확인 불가')).toBeVisible()
    view.rerender(
      card({
        ...base,
        rarity: 'MAGIC',
        name: 'Synthetic Magic',
        itemLevel: 42,
        modifiers: [{ id: 'mod', text: 'Synthetic +10', kind: 'explicit' }],
      }),
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--magic')
    expect(screen.queryByText('Synthetic Base')).not.toBeInTheDocument()
    expect(screen.getByText('Synthetic +10')).toBeVisible()
    view.rerender(
      card({
        ...base,
        rarity: 'RARE',
        name: 'Synthetic Rare',
        itemLevel: 82,
        properties: [{ id: 'quality', text: 'Quality: +20%' }],
        modifiers: [
          {
            id: 'mod',
            text: 'Synthetic +25',
            kind: 'fractured',
            detail: 'Verified detail',
          },
        ],
        flags: [{ id: 'corrupted', text: '타락' }],
      }),
    )
    expect(screen.getByRole('article')).toHaveClass('item-card--rare')
    expect(
      screen.getByRole('heading', { name: 'Synthetic Rare' }),
    ).toBeVisible()
    expect(screen.getByText('Synthetic Base')).toBeVisible()
    expect(screen.queryByText('Synthetic +10')).not.toBeInTheDocument()
    expect(screen.queryByText('42')).not.toBeInTheDocument()
    expect(screen.getByText('82')).toBeVisible()
    expect(screen.getByText('Quality: +20%')).toBeVisible()
    expect(screen.getByText('타락')).toHaveClass('item-card__line--corrupted')
    fireEvent.click(screen.getByText('Synthetic +25'))
    expect(screen.getByText('Verified detail')).toBeVisible()
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(
      screen.getByRole('article', { name: 'Item card' }),
    ).toHaveTextContent('Item level: 82')
    expect(screen.getByText('타락')).toBeVisible()
  })
  it('판매 가격은 카드에서 제외하고 미해석 행과 원본 데이터는 보존한다', () => {
    const input: ParsedItemText = {
      text: { originalText: '~b/o 888 mirror', lines: [] },
      locale: 'en',
      itemClass: 'Synthetic',
      rarity: 'RARE',
      rarityText: 'Rare',
      nameLines: [],
      displayName: 'Synthetic',
      displayBase: 'Base',
      itemLevel: 42,
      properties: [],
      requirements: [],
      flags: [],
      warnings: [],
      markedModifiers: [
        {
          number: 4,
          section: 2,
          kind: 'CONTENT',
          raw: 'Synthetic +13 (implicit)',
        },
      ],
      unparsedLines: [
        {
          number: 5,
          section: 3,
          kind: 'CONTENT',
          raw: '<script>unknown</script>',
        },
        { number: 6, section: 4, kind: 'CONTENT', raw: '~b/o 888 mirror' },
        { number: 7, section: 4, kind: 'CONTENT', raw: '-B/O 123 MIRROR' },
      ],
    }
    render(card(toItemCard(input)))
    const displayed = within(screen.getByRole('article'))
    expect(displayed.queryByText(/mirror/i)).not.toBeInTheDocument()
    expect(displayed.getByText('<script>unknown</script>')).toHaveClass(
      'item-card__line--unknown',
    )
    expect(displayed.getByText('Synthetic +13 (implicit)')).toHaveClass(
      'item-card__line--implicit',
    )
    expect(document.querySelector('script')).toBeNull()
    expect(input.unparsedLines).toHaveLength(3)
    expect(input.text.originalText).toBe('~b/o 888 mirror')
  })
})
