import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
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
      'stash-canvas__content',
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
})
