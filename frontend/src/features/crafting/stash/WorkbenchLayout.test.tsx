import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { CraftingPage } from '../CraftingPage'
import { useItemDraft } from '../draft'
import { readFileSync } from 'node:fs'
const stashStyles = readFileSync(
  'src/features/crafting/stash/workbenchStash.css',
  'utf8',
)
const pageStyles = readFileSync('src/features/crafting/crafting.css', 'utf8')

let styles: HTMLStyleElement
const originalWidth = window.innerWidth
beforeEach(() => {
  useItemDraft.getState().setBase()
  styles = document.createElement('style')
  styles.textContent = pageStyles + stashStyles
  document.head.append(styles)
})
afterEach(() => {
  styles.remove()
  Object.defineProperty(window, 'innerWidth', {
    configurable: true,
    value: originalWidth,
  })
})
function show() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const compact = (value: string) => value.replace(/\s/g, '')

describe('single category selector and whole-surface favorite strip', () => {
  it('connects the preserved category selector to the right content only', () => {
    show()
    const selector = screen.getByRole('tablist', {
      name: 'Material stash tabs',
    })
    expect(within(selector).getAllByRole('tab')).toHaveLength(6)
    expect(document.querySelector('.workbench-stash__tabs')).toBeNull()
    const right = document.querySelector<HTMLElement>('.workbench-stash')!
    expect(within(right).queryByRole('tablist')).toBeNull()
    const fixedShelf = document.querySelector<HTMLElement>('.currency-catalog')!
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

  it('picks across categories, drags into the separate bottom strip and preserves Omen activation', () => {
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
    fireEvent.click(
      within(right).getByRole('button', { name: 'Omen of Whittling' }),
    )
    fireEvent.click(slots[0]!)
    fireEvent.contextMenu(slots[0]!)
    expect(useItemDraft.getState().activeOmens).toContain('Omen_of_Whittling')
    expect(slots[0]).toHaveClass('is-active-omen')
  })

  it.each([1280, 800, 375])(
    'keeps right content and global 2×5 favorites in distinct DOM/style boundaries at %ipx',
    (width) => {
      Object.defineProperty(window, 'innerWidth', {
        configurable: true,
        value: width,
      })
      show()
      act(() => window.dispatchEvent(new Event('resize')))
      const board = document.querySelector<HTMLElement>('.stash-board')!
      const region = document.querySelector<HTMLElement>(
        '.workbench-favorites-region',
      )!
      const right = document.querySelector<HTMLElement>('.workbench-stash')!
      const favorites = screen.getByRole('group', {
        name: 'Shared material favorites',
      })
      expect(board.nextElementSibling).toBe(region)
      expect(region.parentElement).toBe(board.parentElement)
      expect(region).toContainElement(favorites)
      for (const container of [
        board,
        right,
        document.querySelector('.stash-viewport')!,
        document.querySelector('.bench-lower')!,
      ])
        expect(container).not.toContainElement(favorites)
      expect(within(favorites).getAllByRole('button')).toHaveLength(10)
      expect(compact(getComputedStyle(favorites).gridTemplateColumns)).toBe(
        'repeat(5,minmax(0,1fr))',
      )
      expect(compact(getComputedStyle(favorites).gridTemplateRows)).toBe(
        'repeat(2,auto)',
      )
      expect(getComputedStyle(favorites).position).not.toBe('absolute')
      expect(getComputedStyle(region).width).toBe('100%')
      const resources = right.querySelector<HTMLElement>(
        '.workbench-stash__resources',
      )!
      expect(compact(getComputedStyle(resources).gridTemplateColumns)).toBe(
        'repeat(4,minmax(0,1fr))',
      )
      expect(getComputedStyle(resources).overflowY).toBe('auto')
      expect(getComputedStyle(right).position).toBe('absolute')
      expect(
        getComputedStyle(document.querySelector('.stash-viewport')!).overflowX,
      ).toBe('auto')
    },
  )
})
