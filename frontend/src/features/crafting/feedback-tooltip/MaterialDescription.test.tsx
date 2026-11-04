import { readFileSync } from 'node:fs'
import { render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import { MaterialDescription } from '../MaterialTooltip'
import { ItemCard } from '../ItemCard'
import { locales, setLocale } from '../../../shared/i18n/i18n'

let style: HTMLStyleElement
beforeEach(() => {
  style = document.createElement('style')
  style.textContent =
    readFileSync('src/features/crafting/crafting.css', 'utf8') +
    readFileSync('src/features/crafting/item-card.css', 'utf8')
  document.head.append(style)
})
afterEach(() => style.remove())

it.each(locales)(
  'puts the existing material source in the name header without losing %s descriptions',
  (locale) => {
    setLocale(locale)
    const { container } = render(<MaterialDescription materialId="Chaos_Orb" />)
    const header = container.querySelector('.material-description__header')!
    const link = within(header as HTMLElement).getByRole('link', {
      name: 'PoE2DB',
    })
    expect(link).toHaveAttribute('href', expect.stringContaining('poe2db.tw'))
    expect(link.previousElementSibling).toHaveClass(
      'material-description__name',
    )
    expect(
      container.querySelectorAll('.material-description p[lang]').length,
    ).toBeGreaterThan(0)
    expect(container).not.toHaveTextContent('Stack Size:')
    expect(getComputedStyle(header).flexWrap).toBe('wrap')
    expect(getComputedStyle(link).marginInlineStart).toBe('auto')
    expect(getComputedStyle(link.previousElementSibling!).color).toBe(
      'rgb(170, 158, 130)',
    )
    expect(
      getComputedStyle(container.querySelector('.material-description')!).color,
    ).toBe('rgb(153, 153, 255)')
    expect(getComputedStyle(link.previousElementSibling!).overflowWrap).toBe(
      'anywhere',
    )
  },
)

it('preserves official availability links and limits the requested palette to material descriptions', () => {
  render(
    <>
      <MaterialDescription materialId="Omen_of_Homogenising_Exaltation" />
      <p role="alert">Failure</p>
      <ItemCard
        item={{
          rarity: 'NORMAL',
          name: 'Base',
          itemClass: 'Test',
          base: 'Base',
          itemLevel: 82,
          properties: [],
          requirements: [],
          modifiers: [],
          flags: [],
        }}
      />
    </>,
  )
  expect(screen.getByRole('link', { name: 'PoE2DB' })).toBeVisible()
  expect(
    screen.getByRole('link', { name: 'Official legacy availability' }),
  ).toHaveAttribute('href', expect.stringContaining('pathofexile.com/forum'))
  expect(getComputedStyle(screen.getByRole('article')).color).toBe(
    'rgb(167, 167, 167)',
  )
  expect(screen.getByRole('alert').closest('.material-description')).toBeNull()
})
