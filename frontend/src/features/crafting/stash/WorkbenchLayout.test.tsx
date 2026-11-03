import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import { locales, setLocale, translate } from '../../../shared/i18n/i18n'
import { readFileSync } from 'node:fs'
const stashStyles = readFileSync(
  'src/features/crafting/stash/workbenchStash.css',
  'utf8',
)
const pageStyles = readFileSync('src/features/crafting/crafting.css', 'utf8')
const compactStyles = readFileSync(
  'src/features/crafting/compact-viewport/compactViewport.css',
  'utf8',
)

let styles: HTMLStyleElement
beforeEach(() => {
  useItemDraft.getState().setBase()
  styles = document.createElement('style')
  styles.textContent = pageStyles + stashStyles + compactStyles
  document.head.append(styles)
})
afterEach(() => {
  styles.remove()
})
function show() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const compact = (value: string) => value.replace(/\s/g, '')
function ruleStyle(selector: string) {
  const rules = [...styles.sheet!.cssRules].filter(
    (rule) => rule.type === CSSRule.STYLE_RULE,
  ) as CSSStyleRule[]
  return rules.reverse().find((rule) => rule.selectorText === selector)!.style
}

describe('single canvas category selector and favorites inside the coordinate plane', () => {
  it('connects the preserved category selector to the right content only', () => {
    show()
    const selector = screen.getByRole('tablist', {
      name: 'Material stash tabs',
    })
    expect(within(selector).getAllByRole('tab')).toHaveLength(6)
    expect(document.querySelector('.stash-canvas')).toContainElement(selector)
    const right = document.querySelector<HTMLElement>('.workbench-stash')!
    expect(right).toContainElement(selector)
    expect(document.querySelector('.stash-heading')).toBeNull()
    const tooltipToggle = screen.getByRole('checkbox', {
      name: 'Show tooltips',
    })
    expect(right).not.toContainElement(tooltipToggle)
    expect(tooltipToggle.closest('label')?.parentElement).toHaveClass(
      'workbench-item-controls',
    )
    const fixedShelf = document.querySelector<HTMLElement>('.currency-catalog')!
    const fixedIds = new Set(
      [...fixedShelf.querySelectorAll('[data-material-tooltip]')].map(
        (button) => button.getAttribute('data-material-tooltip'),
      ),
    )
    for (const button of right.querySelectorAll('[data-material-tooltip]'))
      expect(fixedIds.has(button.getAttribute('data-material-tooltip'))).toBe(
        false,
      )
    const originalChaos = within(fixedShelf).getByRole('button', {
      name: 'Chaos Orb',
    })
    for (const [tab, button] of [
      ['Essence', 'Greater Essence of the Body'],
      ['Alloy', 'Runic Alloy'],
      ['Catalysts', 'Flesh Catalyst'],
    ] as const) {
      const trigger = within(selector).getByRole('tab', { name: tab })
      fireEvent.click(trigger)
      const panel = within(right).getByRole('tabpanel', { name: tab })
      expect(trigger).toHaveAttribute('aria-controls', panel.id)
      expect(panel).toHaveAttribute('aria-labelledby', trigger.id)
      expect(within(panel).getByRole('button', { name: button })).toBeVisible()
      expect(
        within(fixedShelf).getByRole('button', { name: 'Chaos Orb' }),
      ).toBe(originalChaos)
    }
    const essence = within(selector).getByRole('tab', { name: 'Essence' })
    essence.focus()
    fireEvent.keyDown(essence, { key: 'End' })
    expect(
      within(selector).getByRole('tab', { name: 'Liquid Emotions' }),
    ).toHaveFocus()
    fireEvent.keyDown(document.activeElement!, { key: 'ArrowRight' })
    expect(
      within(selector).getByRole('tab', { name: 'Currency' }),
    ).toHaveFocus()
    expect(
      within(right).getByRole('button', { name: "Artificer's Orb" }),
    ).toBeVisible()
    for (const name of ['Gnawed Rib', 'Gnawed Jawbone', 'Gnawed Collarbone'])
      expect(within(right).queryByRole('button', { name })).toBeNull()
  })

  it('picks across categories, drags into the coordinate plane and preserves Omen activation', () => {
    show()
    const right = document.querySelector<HTMLElement>('.workbench-stash')!
    const selector = screen.getByRole('tablist', {
      name: 'Material stash tabs',
    })
    const favorites = screen.getByRole('group', {
      name: 'Shared material favorites',
    })
    const slots = within(favorites).getAllByRole('button')
    fireEvent.click(within(selector).getByRole('tab', { name: 'Alloy' }))
    const data = new Map<string, string>()
    const dataTransfer = {
      setData: (type: string, value: string) => data.set(type, value),
      getData: (type: string) => data.get(type) ?? '',
      effectAllowed: '',
      dropEffect: '',
    }
    fireEvent.dragStart(
      within(right).getByRole('button', { name: 'Runic Alloy' }),
      { dataTransfer },
    )
    fireEvent.click(within(selector).getByRole('tab', { name: 'Essence' }))
    expect(fireEvent.dragOver(slots[9]!, { dataTransfer })).toBe(false)
    fireEvent.drop(slots[9]!, { dataTransfer })
    expect(slots[9]).toHaveAccessibleName('Favorite slot 10: Runic Alloy')
    fireEvent.click(within(selector).getByRole('tab', { name: 'Omen' }))
    const omen = within(right).getByRole('button', {
      name: 'Omen of Whittling',
    })
    fireEvent.contextMenu(omen)
    expect(useItemDraft.getState().activeOmens).toContain('Omen_of_Whittling')
    fireEvent.contextMenu(omen)
    expect(useItemDraft.getState().activeOmens).not.toContain(
      'Omen_of_Whittling',
    )
    fireEvent.click(omen)
    fireEvent.click(slots[0]!)
    fireEvent.contextMenu(slots[0]!)
    expect(useItemDraft.getState().activeOmens).toContain('Omen_of_Whittling')
    expect(slots[0]).toHaveClass('is-active-omen')
  })

  it('mounts all ten favorite slots directly inside the unchanged coordinate plane', () => {
    show()
    const favorites = screen.getByRole('group', {
      name: 'Shared material favorites',
    })
    const content = document.querySelector<HTMLElement>(
      '.stash-canvas__content',
    )!
    const right = document.querySelector<HTMLElement>('.workbench-stash')!
    expect(favorites.parentElement).toBe(content)
    expect(document.querySelector('.workbench-favorites-region')).toBeNull()
    expect(right).not.toContainElement(favorites)
    expect(within(favorites).getAllByRole('button')).toHaveLength(10)
    expect(compact(getComputedStyle(favorites).gridTemplateColumns)).toBe(
      'repeat(5,minmax(0,1fr))',
    )
    expect(compact(getComputedStyle(favorites).gridTemplateRows)).toBe(
      'repeat(2,minmax(0,1fr))',
    )
    expect(getComputedStyle(favorites).position).toBe('absolute')
    expect(compact(getComputedStyle(content).aspectRatio)).toBe('935/550')
    expect(getComputedStyle(right).position).toBe('absolute')
    expect(
      getComputedStyle(document.querySelector('.stash-viewport')!).overflowX,
    ).toBe('clip')
  })

  it.each(locales)(
    'shares the edit style across four ordered %s controls and reserves the original History row',
    (locale) => {
      setLocale(locale)
      show()
      const controls = document.querySelector<HTMLElement>(
        '.workbench-item-controls',
      )!
      expect(controls.parentElement).toHaveClass('stash-canvas__content')
      expect([...controls.children].map((node) => node.textContent)).toEqual([
        translate('ui.select_base', {}, locale),
        translate('ui.edit_item', {}, locale),
        translate('ui.new_craft', {}, locale),
        translate('ui.show_tooltips', {}, locale),
      ])
      const edit = screen.getByRole('button', {
        name: translate('ui.edit_item', {}, locale),
      })
      const editStyle = getComputedStyle(edit)
      for (const element of controls.children) {
        expect(element).toHaveClass('item-input-toggle')
        const style = getComputedStyle(element)
        for (const property of [
          'border',
          'borderRadius',
          'backgroundColor',
          'color',
          'fontSize',
          'padding',
          'width',
          'height',
        ] as const)
          expect(style[property]).toBe(editStyle[property])
      }
      expect(compact(getComputedStyle(controls).gridTemplateRows)).toBe(
        'repeat(4,minmax(0,1fr))',
      )
      const reserved = document.querySelector('.workbench-session-entry')!
      expect(reserved).toHaveAttribute('aria-hidden', 'true')
      expect(reserved).toBeEmptyDOMElement()
      expect(reserved.nextElementSibling).toHaveClass(
        'wb-session-library-buttons',
      )
      expect(compact(ruleStyle('.workbench-session-entry').height)).toBe(
        'calc(1lh+34px)',
      )
    },
  )

  it('keeps percentage control bounds between favorites and stash and expands only the narrow canvas bottom', () => {
    show()
    function bounds(selector: string, width: number) {
      const style = ruleStyle(selector)
      function dimension(property: 'left' | 'top' | 'width' | 'height') {
        const percentage = Number(
          compact(style[property]).match(/^calc\(([\d.]+)%\)$/)![1],
        )
        const extent =
          property === 'left' || property === 'width'
            ? width
            : (width * 550) / 935
        return (percentage / 100) * extent
      }
      return {
        left: dimension('left'),
        top: dimension('top'),
        width: selector === '.workbench-stash' ? 0 : dimension('width'),
        height: dimension('height'),
      }
    }
    for (const width of [224, 330, 424, 640, 935]) {
      const favorites = bounds('.workbench-stash__favorites', width)
      const controls = bounds('.workbench-item-controls', width)
      const stash = bounds('.workbench-stash', width)
      expect(controls.left).toBeGreaterThan(favorites.left + favorites.width)
      expect(controls.left + controls.width).toBeLessThan(stash.left)
      expect(controls.top).toBeCloseTo(favorites.top)
      expect(controls.top + controls.height).toBeCloseTo(
        favorites.top + favorites.height,
      )
    }
    const narrow = [...styles.sheet!.cssRules].find(
      (rule) =>
        rule.type === CSSRule.MEDIA_RULE &&
        [...(rule as CSSMediaRule).cssRules].some(
          (child) =>
            child.type === CSSRule.STYLE_RULE &&
            (child as CSSStyleRule).selectorText === '.workbench-item-controls',
        ),
    ) as CSSMediaRule
    const rules = [...narrow.cssRules] as CSSStyleRule[]
    expect(narrow.conditionText).toBe('(max-width: 1200px)')
    const height = compact(rules[0]!.style.height)
    expect(height).toMatch(/^max\([\d.]+%,208px\)$/)
    expect(Number(height.match(/[\d.]+/)![0])).toBeCloseTo((174 / 550) * 100, 3)
    const padding = compact(rules[1]!.style.getPropertyValue('padding-bottom'))
    const percentages = [...padding.matchAll(/([\d.]+)%/g)].map((match) =>
      Number(match[1]),
    )
    expect(percentages).toHaveLength(2)
    expect(percentages[0]).toBeCloseTo((214 / 935) * 100, 3)
    expect(percentages[1]).toBeCloseTo((20 / 935) * 100, 3)
    expect(padding).toMatch(/\+208px\)$/)
    for (const width of [320, 375, 390, 600, 1001, 1200]) {
      const canvas =
        width <= 1000
          ? Math.min(width / 0.75 - (width <= 480 ? 24 : 48), 1100)
          : Math.min(width - 96, 1100) - 374
      const controls = bounds('.workbench-item-controls', canvas)
      const expandedHeight = Math.max(controls.height, 208)
      const bottomPadding = Math.max(
        (percentages[0]! / 100) * canvas,
        (percentages[1]! / 100) * canvas + 208,
      )
      expect(controls.top + expandedHeight).toBeLessThanOrEqual(
        (canvas * 550) / 935 + bottomPadding + 0.01,
      )
    }
    expect(compact(ruleStyle('.workbench-item-controls input').width)).toBe(
      'max(12%,12px)',
    )
    expect(
      Number.parseFloat(
        ruleStyle(
          '.workbench-item-controls .tooltip-toggle span',
        ).getPropertyValue('min-width'),
      ),
    ).toBe(0)
    expect(
      ruleStyle(
        '.workbench-item-controls > .item-input-toggle',
      ).getPropertyValue('overflow-wrap'),
    ).toBe('anywhere')
    expect(
      Number(ruleStyle('.item-placement').left.match(/[\d.]+/)![0]),
    ).toBeCloseTo((371 / 935) * 100, 3)
  })
})
