import { fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { LanguageSelector } from '../../shared/i18n/LanguageSelector'
import { ItemCard } from './ItemCard'
import type { ItemCardData } from './itemCardData'
import { toItemCard } from './itemCardData'
import type { Item } from './itemModels'

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
    <LocaleProvider initialLanguage="en">
      <LanguageSelector />
      <ItemCard item={item} />
    </LocaleProvider>
  )
}
describe('ItemCard', () => {
  it('renders server metadata without guessing tiers from modifier sentences', () => {
    const source = {
      number: 10,
      section: 3,
      kind: 'CONTENT' as const,
      raw: 'Synthetic +13 (implicit)',
    }
    const metadata = {
      number: 9,
      section: 3,
      kind: 'CONTENT' as const,
      raw: '{ Fractured Prefix Modifier "Test" (Tier: 1) — Life }',
    }
    const parsed: Item = {
      text: { originalText: 'source', lines: [metadata, source] },
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
      markedModifiers: [],
      flags: [],
      warnings: [],
      unparsedLines: [metadata],
      modifiers: [
        {
          text: 'Synthetic +13',
          type: 'FRACTURED',
          source,
          metadata,
          affix: 'PREFIX',
          tier: 1,
          affixName: 'Test',
        },
      ],
    }
    render(card(toItemCard(parsed)))
    expect(screen.getByText('P1')).toBeVisible()
    expect(
      screen.getByText('Synthetic +13').closest('.item-card__line'),
    ).toHaveClass('item-card__line--fractured')
    expect(
      screen.queryByText(metadata.raw, { selector: '.item-card__line' }),
    ).not.toBeInTheDocument()
    fireEvent.click(screen.getByText('Synthetic +13'))
    expect(screen.getByText(/Tier: 1/)).toBeVisible()
  })
  it('updates rarity, names, values and flags together when props change', () => {
    const view = render(card(base))
    expect(screen.getByRole('article')).toHaveClass('item-card--normal')
    expect(screen.getByText('Unknown')).toBeVisible()
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
        flags: [{ id: 'corrupted', text: 'Corrupted' }],
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
    expect(screen.getByText('Corrupted')).toHaveClass(
      'item-card__line--corrupted',
    )
    fireEvent.click(screen.getByText('Synthetic +25'))
    expect(screen.getByText('Verified detail')).toBeVisible()
    expect(
      screen.getByRole('article', { name: 'Item card' }),
    ).toHaveTextContent('Item level: 82')
    expect(screen.getByText('Corrupted')).toBeVisible()
  })
  it('omits sale prices and preserves unresolved source text', () => {
    const input: Item = {
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
      modifiers: [
        {
          text: 'Synthetic +13',
          type: 'IMPLICIT',
          source: {
            number: 4,
            section: 2,
            kind: 'CONTENT',
            raw: 'Synthetic +13 (implicit)',
          },
          metadata: null,
          affix: null,
          tier: null,
          affixName: null,
        },
      ],
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
    expect(
      displayed.getByText('Synthetic +13').closest('.item-card__line'),
    ).toHaveClass('item-card__line--implicit')
    expect(document.querySelector('script')).toBeNull()
    expect(input.unparsedLines).toHaveLength(3)
    expect(input.text.originalText).toBe('~b/o 888 mirror')
  })
})
