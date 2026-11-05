import { readFileSync } from 'node:fs'
import { render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it } from 'vitest'
import { MaterialDescription } from '../MaterialTooltip'
import { ItemCard } from '../ItemCard'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { augments } from '../socketAugments'
import { augmentFeedbackLines } from '../augmentFeedback'

let style: HTMLStyleElement
beforeEach(() => {
  style = document.createElement('style')
  style.textContent =
    readFileSync('src/features/crafting/crafting.css', 'utf8') +
    readFileSync('src/features/crafting/item-card.css', 'utf8')
  document.head.append(style)
})
afterEach(() => style.remove())

it('groups Rune feedback into sourced equipment categories without invented jewellery effects', () => {
  setLocale('ko')
  const { container } = render(
    <MaterialDescription materialId="Perfect_Desert_Rune" />,
  )
  const lines = [...container.querySelectorAll('p')].map((p) => p.textContent)
  expect(lines).toEqual([
    '무도 무기: 화염 피해 17~20 추가',
    '마법봉 또는 지팡이: 피해의 12%를 추가 화염 피해로 획득',
    '방어구: 화염 저항 +22%',
  ])
  expect(container).not.toHaveTextContent('Gloves:')
  expect(container).not.toHaveTextContent('장신구')
})

it.each(locales)(
  'retains every reviewed effect when grouping %s, including differing translations',
  (locale) => {
    for (const entry of augments) {
      const lines = augmentFeedbackLines(entry.id, locale)!
      expect(lines.length).toBeGreaterThan(0)
      for (const effect of Object.values(entry.effects)) {
        const text = effect[locale] ?? effect.en
        expect(lines.some((line) => line.endsWith(`: ${text}`))).toBe(true)
      }
    }
  },
)

it('keeps the reviewed part qualifier when a grouped Soul Core effect only applies to Boots', () => {
  const lines = augmentFeedbackLines('Soul_Core_of_Xopec', 'en')!
  expect(lines.some((line) => line.startsWith('Armour (Boots):'))).toBe(true)
  expect(lines.some((line) => line.startsWith('Armour:'))).toBe(false)
})

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

it.each(locales)(
  'keeps actual Augment limits and source without internal scope notes in %s',
  (locale) => {
    setLocale(locale)
    const entry = augments.find((a) => a.selectionEnabled && a.limit)!
    const index = entry.properties.en!.findIndex((line) =>
      line.startsWith('Limited to:'),
    )
    const limit =
      entry.properties[locale]?.[index] ?? entry.properties.en![index]!
    const view = render(<MaterialDescription materialId={entry.id} />)
    expect(screen.getByText(limit)).toBeVisible()
    expect(screen.getByRole('link', { name: 'PoE2DB' })).toBeVisible()
    expect(view.container.querySelector('.translation-fallback')).toBeNull()
    expect(view.container).not.toHaveTextContent(
      /character-wide|캐릭터 전체|角色全部|全装備|todo el equipo/,
    )
    view.unmount()
  },
)
