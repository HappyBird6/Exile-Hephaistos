import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { currencies } from './currencies'
import { materials } from './materials'

function show() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <LocaleProvider initialLanguage="en">
        <CraftingPage />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}
const tab = (name: string) => screen.getByRole('tab', { name })
const favorite = (slot: number) =>
  screen.getByRole('button', { name: new RegExp(`^Favorite slot ${slot}:`) })
const pick = (name: string) =>
  fireEvent.click(
    within(screen.getByRole('tabpanel')).getByRole('button', { name }),
  )
beforeEach(() => useItemDraft.getState().setBase())

describe('Material stash and shared favorites', () => {
  it('removes all requested currencies and moves Hinekora to the former Chance position', () => {
    show()
    expect(screen.getByRole('tabpanel')).toHaveAccessibleName('Currency')
    expect(
      within(screen.getByRole('tabpanel')).getAllByRole('button'),
    ).toHaveLength(21)
    for (const name of [
      'Mirror of Kalandra',
      'Orb of Chance',
      "Arcanist's Etcher",
      "Armourer's Scrap",
      "Blacksmith's Whetstone",
      "Glassblower's Bauble",
      "Gemcutter's Prism",
      "Artificer's Orb",
      "Lesser Jeweller's Orb",
      "Greater Jeweller's Orb",
      "Perfect Jeweller's Orb",
    ]) {
      expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
    }
    expect(
      currencies.find((item) => item.id === 'Hinekoras_Lock'),
    ).toMatchObject({ x: 329, y: 140 })
    expect(document.querySelector('.stash-inspector')).toBeNull()
    expect(document.querySelector('.item-panel')).toBeNull()
    expect(document.querySelector('.bench-item-card')?.children).toHaveLength(1)
    expect(document.querySelector('.bench-item-card')?.firstElementChild).toBe(
      screen.getByRole('article'),
    )
    expect(
      document.querySelector('.bench-lower')?.lastElementChild,
    ).toBeEmptyDOMElement()
  })

  it('shows only the selected catalog while keeping fifteen favorites and the current card', () => {
    show()
    const card = screen.getByRole('article')
    for (const [label, count] of [
      ['Essence', 95],
      ['Omen', 50],
      ['Catalysts', 26],
      ['Liquid Emotions', 27],
    ] as const) {
      fireEvent.click(tab(label))
      expect(tab(label)).toHaveAttribute('aria-selected', 'true')
      expect(
        within(screen.getByRole('tabpanel')).getAllByRole('button'),
      ).toHaveLength(count)
      expect(
        within(
          screen.getByRole('group', { name: 'Shared material favorites' }),
        ).getAllByRole('button'),
      ).toHaveLength(15)
      expect(screen.getByRole('article')).toBe(card)
      expect(
        screen.queryByRole('button', { name: 'Chaos Orb' }),
      ).not.toBeInTheDocument()
    }
    expect(materials).toHaveLength(198)
    expect(new Set(materials.map((item) => item.id)).size).toBe(
      materials.length,
    )
  })

  it('carries materials across tabs, places and overwrites favorites without consuming source entries', () => {
    show()
    fireEvent.click(tab('Essence'))
    pick('Lesser Essence of the Body')
    fireEvent.click(tab('Currency'))
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Lesser Essence of the Body',
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'registered in favorite slot 1',
    )
    fireEvent.click(favorite(2))
    expect(favorite(2)).toHaveAccessibleName('Favorite slot 2: empty')
    fireEvent.click(tab('Essence'))
    expect(
      screen.getByRole('button', { name: 'Lesser Essence of the Body' }),
    ).toBeVisible()
    pick('Lesser Essence of the Mind')
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Lesser Essence of the Mind',
    )
    fireEvent.click(tab('Omen'))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Lesser Essence of the Mind',
    )
  })

  it('Escape cancels pickup, and selecting a favorite for use cannot overwrite another slot', () => {
    show()
    fireEvent.click(tab('Essence'))
    pick('Lesser Essence of the Body')
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName('Favorite slot 1: empty')
    pick('Lesser Essence of the Body')
    fireEvent.click(favorite(1))
    fireEvent.contextMenu(favorite(1))
    expect(favorite(1)).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(favorite(2))
    expect(favorite(2)).toHaveAccessibleName('Favorite slot 2: empty')
    const original = screen.getByRole('article').textContent
    fireEvent.click(
      screen.getByRole('button', {
        name: 'Use selected currency on the central item',
      }),
    )
    expect(screen.getByRole('article').textContent).toBe(original)
    expect(screen.getByRole('status')).toHaveTextContent(
      'The item has not changed',
    )
  })

  it('navigates tabs by keyboard and closes input with focus returned to its trigger', () => {
    show()
    tab('Currency').focus()
    fireEvent.keyDown(tab('Currency'), { key: 'ArrowRight' })
    expect(tab('Essence')).toHaveFocus()
    fireEvent.keyDown(tab('Essence'), { key: 'End' })
    expect(tab('Liquid Emotions')).toHaveFocus()
    fireEvent.keyDown(tab('Liquid Emotions'), { key: 'ArrowRight' })
    expect(tab('Currency')).toHaveFocus()
    const edit = screen.getByRole('button', { name: 'Edit item' })
    fireEvent.click(edit)
    expect(
      screen.getByRole('button', { name: 'Close item input' }),
    ).toHaveFocus()
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(edit).toHaveFocus()
    expect(edit).toHaveAttribute('aria-expanded', 'false')
  })

  it('replaces a failed favorite image with the next material image', () => {
    show()
    fireEvent.click(tab('Essence'))
    pick('Perfect Essence of the Mind')
    fireEvent.click(favorite(1))
    const missing = favorite(1).querySelector('img')
    if (!missing) throw new Error('Expected favorite image')
    fireEvent.error(missing)
    expect(favorite(1)).toHaveTextContent('Image unavailable')
    pick('Lesser Essence of the Body')
    fireEvent.click(favorite(1))
    expect(favorite(1).querySelector('img')).toHaveAttribute(
      'src',
      '/assets/materials/Lesser_Essence_of_the_Body.webp',
    )
    expect(favorite(1)).not.toHaveTextContent('Image unavailable')
  })

  it('does not persist favorites after leaving the page and remounting', () => {
    const view = show()
    fireEvent.click(tab('Essence'))
    pick('Lesser Essence of the Body')
    fireEvent.click(favorite(15))
    view.unmount()
    show()
    expect(favorite(15)).toHaveAccessibleName('Favorite slot 15: empty')
  })
})
