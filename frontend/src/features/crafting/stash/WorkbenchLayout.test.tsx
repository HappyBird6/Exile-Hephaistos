import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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
const menuStyles = [
  'base-picker/left-shelf-overlay.css',
  'base-picker/base-picker.css',
  'workbench-scrollbar.css',
  'session-library/session-library.css',
]
  .map((path) => readFileSync(`src/features/crafting/${path}`, 'utf8'))
  .join('\n')

let styles: HTMLStyleElement
beforeEach(() => {
  useItemDraft.getState().setBase()
  styles = document.createElement('style')
  styles.textContent = pageStyles + stashStyles + compactStyles + menuStyles
  document.head.append(styles)
})
afterEach(() => {
  styles.remove()
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
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
  it('retains the size-containment and internal overflow contract across real large catalogs, filtering and tab changes', () => {
    show()
    const stash = document.querySelector('.workbench-stash-column')!
    const list = stash.querySelector('.workbench-stash__resources')!
    const panel = stash.querySelector('.workbench-stash__panel')!
    const canvas = document.querySelector('.stash-canvas__content')!
    const tabs = screen.getByRole('tablist', { name: 'Material stash tabs' })
    function contract() {
      const declarations = (element: Element, names: string[]) => {
        const computed = getComputedStyle(element)
        return names.map((name) => [name, computed.getPropertyValue(name)])
      }
      return {
        stash: declarations(stash, [
          'contain',
          'grid-row',
          'grid-column',
          'align-self',
          'min-height',
        ]),
        canvas: declarations(canvas, [
          'grid-template-rows',
          'grid-template-columns',
        ]),
        panel: declarations(panel, ['flex', 'min-height']),
        list: declarations(list, [
          'flex',
          'min-height',
          'overflow-y',
          'scrollbar-width',
          'scrollbar-gutter',
          'scrollbar-color',
        ]),
      }
    }
    expect(getComputedStyle(stash).getPropertyValue('contain')).toBe('size')
    expect(getComputedStyle(stash).alignSelf).toBe('stretch')
    expect(getComputedStyle(list).overflowY).toBe('auto')
    expect(list).toHaveClass('wb-scrollbar')
    const baseline = contract()
    for (const label of ['Essence', 'Omen', 'Catalysts', 'Liquid Emotions']) {
      fireEvent.click(within(tabs).getByRole('tab', { name: label }))
      expect(stash.querySelector('.workbench-stash__resources')).toBe(list)
      const count = list.querySelectorAll('button').length
      expect(count).toBeGreaterThan(8)
      expect(contract()).toEqual(baseline)
      const search = within(stash as HTMLElement).getByRole('searchbox')
      fireEvent.change(search, {
        target: { value: '__no_matching_catalog_entry__' },
      })
      expect(list.querySelectorAll('button')).toHaveLength(0)
      expect(
        within(list as HTMLElement).getByText('No materials found'),
      ).toBeVisible()
      expect(contract()).toEqual(baseline)
      fireEvent.change(search, { target: { value: '' } })
      expect(list.querySelectorAll('button')).toHaveLength(count)
      expect(contract()).toEqual(baseline)
    }
    // jsdom has no layout engine. These are sizing/scroll contracts and actual
    // catalog mutations, not fabricated scrollHeight/clientHeight measurements.
    expect(
      getComputedStyle(document.querySelector('.workbench-feedback-region')!)
        .flex,
    ).toBe('1 1 0%')
    expect(canvas.querySelectorAll('.workbench-stash__favorite')).toHaveLength(
      10,
    )
  })
  it('shares stash scroll styling with the actual base, History and Favorites scroll surfaces', () => {
    show()
    expect(ruleStyle('.wb-scrollbar').getPropertyValue('scrollbar-color')).toBe(
      '#867347 #141a15',
    )
    const stash = document.querySelector('.workbench-stash__resources')!
    const stashColor =
      getComputedStyle(stash).getPropertyValue('scrollbar-color')
    function scrollSurface(element: Element, overflowProperty = 'overflow-y') {
      expect(element).toHaveClass('wb-scrollbar')
      const style = getComputedStyle(element)
      expect(style.getPropertyValue('scrollbar-gutter')).toBe('stable')
      expect(style.getPropertyValue('scrollbar-width')).toBe('thin')
      expect(style.getPropertyValue('scrollbar-color')).toBe(stashColor)
      expect(style.getPropertyValue(overflowProperty)).toBe('auto')
    }
    scrollSurface(stash)
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
    const picker = screen.getByRole('dialog')
    const body = picker.querySelector('.wb-shelf-overlay__body')!
    const layout = picker.querySelector('.wb-base-picker')!
    const controls = picker.querySelector('.wb-base-picker__controls')!
    const options = picker.querySelector('.wb-base-picker__options')!
    expect(getComputedStyle(body).overflow).toBe('hidden')
    expect(getComputedStyle(body).getPropertyValue('scrollbar-gutter')).toBe(
      'auto',
    )
    expect(compact(getComputedStyle(layout).gridTemplateColumns)).toBe(
      'minmax(0,1fr)minmax(0,1.4fr)',
    )
    expect(compact(getComputedStyle(options).gridTemplateColumns)).toBe(
      'minmax(0,1fr)',
    )
    scrollSurface(controls)
    scrollSurface(options)
    for (const element of [body, layout, controls, options])
      expect(Number.parseFloat(getComputedStyle(element).minHeight)).toBe(0)
    expect(controls).toContainElement(within(picker).getByRole('searchbox'))
    expect(controls).toContainElement(
      screen.getByRole('spinbutton', { name: 'Item level' }),
    )
    expect(controls).toContainElement(
      within(screen.getByRole('dialog', { name: 'Select base' })).getByRole(
        'button',
        { name: 'Select base' },
      ),
    )
    expect(
      getComputedStyle(picker.querySelector('.wb-base-picker__footer')!)
        .flexDirection,
    ).toBe('column')
    // The narrow fallback follows the actual overlay body width, even on a wide viewport.
    const narrow = [...styles.sheet!.cssRules].find((rule) =>
      rule.cssText.startsWith('@container'),
    ) as CSSGroupingRule
    expect(narrow.cssText).toContain('(max-width: 340px)')
    const narrowLayout = narrow.cssRules[0] as CSSStyleRule
    expect(compact(narrowLayout.style.gridTemplateColumns)).toBe(
      'minmax(0,1fr)',
    )
    expect(compact(narrowLayout.style.gridTemplateRows)).toBe(
      'minmax(0,1fr)minmax(0,1fr)',
    )
    fireEvent.keyDown(picker, { key: 'Escape' })
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
    scrollSurface(document.querySelector('.wb-history-list')!, 'overflow')
    expect(screen.queryByRole('dialog')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Favorites' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('keeps jewel rarity, observed suffix and all validation in the scrollable left column', () => {
    show()
    fireEvent.click(screen.getByRole('button', { name: 'Select base' }))
    const picker = screen.getByRole('dialog')
    const controls = picker.querySelector('.wb-base-picker__controls')!
    fireEvent.change(within(picker).getByRole('searchbox'), {
      target: { value: 'Sapphire' },
    })
    const sapphire = within(picker).getByRole('button', {
      name: 'Sapphire Jewels',
    })
    act(() => sapphire.focus())
    expect(screen.getByRole('tooltip')).toBeInTheDocument()
    fireEvent.keyDown(sapphire, { key: 'Escape' })
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    expect(picker).toBeInTheDocument()
    expect(sapphire).toHaveFocus()
    fireEvent.click(sapphire)
    expect(sapphire).toHaveAttribute('aria-pressed', 'true')
    const rarity = screen.getByLabelText('Rarity')
    expect(controls).toContainElement(rarity)
    fireEvent.change(rarity, { target: { value: 'RARE' } })
    const suffix = screen.getByRole('checkbox', {
      name: translate('ui.sapphire_existing_suffix'),
    })
    expect(controls).toContainElement(suffix)
    expect(getComputedStyle(suffix).width).not.toBe('76px')
    expect(getComputedStyle(suffix).maxWidth).not.toBe('76px')
    fireEvent.click(suffix)
    const roll = screen.getByLabelText(translate('ui.sapphire_cast_speed'))
    expect(controls).toContainElement(roll)
    const place = within(
      screen.getByRole('dialog', { name: 'Select base' }),
    ).getByRole('button', { name: 'Select base' })
    fireEvent.change(roll, { target: { value: '1' } })
    expect(place).toBeDisabled()
    fireEvent.change(roll, { target: { value: '3' } })
    expect(place).toBeEnabled()
    const level = screen.getByRole('spinbutton', { name: 'Item level' })
    for (const value of ['', '0', '1.5', '101']) {
      fireEvent.change(level, { target: { value } })
      expect(place).toBeDisabled()
      expect(level).toHaveAttribute('aria-invalid', 'true')
      expect(controls).toContainElement(within(picker).getByRole('alert'))
    }
    for (const value of ['1', '100']) {
      fireEvent.change(level, { target: { value } })
      expect(place).toBeEnabled()
      expect(within(picker).queryByRole('alert')).not.toBeInTheDocument()
    }
    expect(
      picker.querySelector('.wb-base-picker__options'),
    ).not.toContainElement(place)
  })
  it('connects the preserved category selector to the right content only', () => {
    show()
    const selector = screen.getByRole('tablist', {
      name: 'Material stash tabs',
    })
    expect(within(selector).getAllByRole('tab')).toHaveLength(7)
    expect(document.querySelector('.stash-canvas')).toContainElement(selector)
    const right = document.querySelector<HTMLElement>('.workbench-stash')!
    expect(right).toContainElement(selector)
    expect(document.querySelector('.stash-heading')).toBeNull()
    expect(screen.queryByRole('checkbox', { name: 'Show tooltips' })).toBeNull()
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
      within(selector).getByRole('tab', { name: 'Augments' }),
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
      'repeat(2,auto)',
    )
    expect(getComputedStyle(favorites).position).toBe('relative')
    expect(
      compact(
        getComputedStyle(content.querySelector('.stash-canvas__upper')!)
          .aspectRatio,
      ),
    ).toBe('935/550')
    expect(getComputedStyle(right.parentElement!).position).toBe('relative')
    expect(right.parentElement).toHaveClass('workbench-stash-column')
    expect(
      getComputedStyle(document.querySelector('.stash-viewport')!).overflowX,
    ).toBe('clip')
  })

  it.each(locales)(
    'shares the edit style across three ordered %s controls without reserving the former row',
    (locale) => {
      setLocale(locale)
      show()
      const stash = document.querySelector('.workbench-stash-column')!
      expect(getComputedStyle(stash).getPropertyValue('contain')).toBe('size')
      expect(
        within(stash as HTMLElement).getByRole('searchbox'),
      ).toHaveAccessibleName(
        translate(
          'material.search',
          { category: translate('stash.Currency', {}, locale) },
          locale,
        ),
      )
      const controls = document.querySelector<HTMLElement>(
        '.workbench-item-controls',
      )!
      expect(controls.parentElement).toHaveClass('stash-canvas__content')
      expect([...controls.children].map((node) => node.textContent)).toEqual([
        translate('ui.select_base', {}, locale),
        translate('ui.edit_item', {}, locale),
        translate('ui.new_craft', {}, locale),
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
        'repeat(3,minmax(0,1fr))',
      )
      expect(document.querySelector('.workbench-session-entry')).toBeNull()
      expect(
        document.querySelector('.bench-lower')?.firstElementChild,
      ).toHaveClass('workbench-film-toolbar')
      expect(
        document.querySelector('.workbench-film-toolbar'),
      ).toContainElement(
        screen.getByRole('button', {
          name: translate('ui.previous_crafting_step', {}, locale),
        }),
      )
      expect(
        document.querySelector('.bench-item-card .workbench-film-controls'),
      ).toBeNull()
      expect(pageStyles).not.toContain('.workbench-session-entry')
    },
  )

  it('positions the remaining base menu from the work area and restores focus to its opener', () => {
    vi.stubGlobal('innerWidth', 1400)
    vi.stubGlobal('innerHeight', 900)
    const rects = new Map([
      ['bench-lower', new DOMRect(900, 100, 374, 800)],
      ['stash-canvas__upper', new DOMRect(100, 100, 800, 600)],
    ])
    vi.spyOn(HTMLElement.prototype, 'offsetWidth', 'get').mockImplementation(
      function (this: HTMLElement) {
        return this.className === 'bench-lower' ? 374 : 0
      },
    )
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockImplementation(
      function (this: HTMLElement) {
        if (this.textContent === 'Select base' && this.tagName === 'BUTTON')
          return new DOMRect(600, 650, 140, 40)
        if (this.getAttribute('aria-label') === 'History')
          return new DOMRect(917, 132, 44, 40)
        if (this.getAttribute('aria-label') === 'Favorites')
          return new DOMRect(969, 132, 44, 40)
        return rects.get(this.className) ?? new DOMRect()
      },
    )
    show()
    const opener = screen.getByRole('button', { name: 'Select base' })
    fireEvent.click(opener)
    const picker = screen.getByRole('dialog', { name: 'Select base' })
    expect(picker).toHaveStyle({
      left: '391px',
      top: '124px',
      width: '520px',
      maxHeight: '560px',
    })
    expect(
      within(picker).getByRole('button', { name: 'Close item input' }),
    ).toHaveFocus()
    fireEvent.pointerDown(opener)
    expect(picker).toBeInTheDocument()
    rects.set('bench-lower', new DOMRect(850, 90, 374, 800))
    fireEvent.scroll(window)
    expect(picker).toHaveStyle({ left: '341px', top: '114px' })
    rects.set('bench-lower', new DOMRect(900, 100, 374, 800))
    fireEvent.resize(window)
    expect(picker).toHaveStyle({ left: '391px', top: '124px' })
    rects.set('bench-lower', new DOMRect(900, 100, 374 * 0.75, 800 * 0.75))
    fireEvent.resize(window)
    expect(picker).toHaveStyle({ left: '386.75px', top: '118px' })
    fireEvent.keyDown(picker, { key: 'Escape' })
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(opener).toHaveFocus()
    fireEvent.click(opener)
    fireEvent.click(screen.getByRole('button', { name: 'Close item input' }))
    expect(opener).toHaveFocus()
    fireEvent.click(opener)
    fireEvent.pointerDown(document.body)
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(opener).toHaveFocus()
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(document.querySelector('.wb-history-nav')).toBeVisible()
  })

  it('stretches the canvas with the right column while keeping the lower controls on one track', () => {
    show()
    const viewport = document.querySelector('.stash-viewport')!
    const content = document.querySelector('.stash-canvas__content')!
    const favorites = document.querySelector('.workbench-stash__favorites')!
    const controls = document.querySelector('.workbench-item-controls')!
    const stash = document.querySelector('.workbench-stash-column')!
    expect(getComputedStyle(viewport).alignSelf).toBe('stretch')
    expect(getComputedStyle(viewport).display).toBe('flex')
    expect(getComputedStyle(content).display).toBe('grid')
    expect(getComputedStyle(favorites).gridRow).toBe('3')
    expect(getComputedStyle(controls).gridRow).toBe('3')
    expect(getComputedStyle(stash).gridRow).toBe('1 / 4')
    expect(getComputedStyle(favorites).gridColumn).toBe('2')
    expect(getComputedStyle(controls).gridColumn).toBe('4')
    expect(getComputedStyle(stash).gridColumn).toBe('6')
    expect(
      compact(
        ruleStyle('.stash-viewport .stash-canvas').getPropertyValue(
          'padding-bottom',
        ),
      ),
    ).toBe('24px')
    expect(
      compact(
        ruleStyle(
          '.workbench-item-controls > .item-input-toggle',
        ).getPropertyValue('overflow-wrap'),
      ),
    ).toBe('anywhere')
    const narrow = [...styles.sheet!.cssRules].find(
      (rule) =>
        rule.type === CSSRule.MEDIA_RULE &&
        (rule as CSSMediaRule).conditionText === '(max-width: 1200px)' &&
        [...(rule as CSSMediaRule).cssRules].some(
          (child) =>
            (child as CSSStyleRule).selectorText === '.workbench-item-controls',
        ),
    ) as CSSMediaRule
    expect(
      (narrow.cssRules[0] as CSSStyleRule).style.getPropertyValue('min-height'),
    ).toBe('208px')
    expect(
      getComputedStyle(document.querySelector('.workbench-feedback-region')!)
        .flex,
    ).toBe('1 1 0%')
  })
})
