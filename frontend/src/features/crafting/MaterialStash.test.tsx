import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { currencies } from './currencies'
import { materials } from './materials'
import tooltips from './materialTooltips.json'

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
  it('has a sourced description for every visible currency and material', () => {
    const descriptions: Record<
      string,
      { name: string; lines: string[]; sourceUrl: string }
    > = tooltips
    for (const entry of [...currencies, ...materials]) {
      expect(descriptions[entry.id]?.lines.length).toBeGreaterThan(0)
      expect(descriptions[entry.id]?.sourceUrl).toBe(
        `https://poe2db.tw/us/${entry.id}`,
      )
    }
  })
  it('keeps search above scrolling rows and groups the four essence tiers', () => {
    show()
    fireEvent.click(tab('Essence'))
    const search = screen.getByRole('searchbox', { name: 'Search Essence' })
    expect(search.closest('.material-catalog')).toBeNull()
    fireEvent.change(search, {
      target: { value: '  greater essence of the body ' },
    })
    const row = document.querySelector('.essence-row')
    expect(row).not.toBeNull()
    expect(
      [...row!.querySelectorAll('button')].map((b) =>
        b.getAttribute('aria-label'),
      ),
    ).toEqual([
      'Lesser Essence of the Body',
      'Essence of the Body',
      'Greater Essence of the Body',
      'Perfect Essence of the Body',
    ])
    fireEvent.click(tab('Alloy'))
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
    fireEvent.click(tab('Essence'))
    expect(screen.getByRole('searchbox')).toHaveValue(
      '  greater essence of the body ',
    )
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: '' } })
    expect(
      within(
        screen.getByRole('group', { name: 'Special essences' }),
      ).getAllByRole('button'),
    ).toHaveLength(6)
    expect(
      screen.queryByRole('button', { name: 'Runic Alloy' }),
    ).not.toBeInTheDocument()
  })

  it('keeps all six special essences in place during matching and empty searches', () => {
    show()
    fireEvent.click(tab('Essence'))
    const special = within(
      screen.getByRole('group', { name: 'Special essences' }),
    )
    const buttons = special.getAllByRole('button')
    for (const query of [
      'greater essence of the body',
      'hysteria',
      'nonexistent',
    ]) {
      fireEvent.change(screen.getByRole('searchbox'), {
        target: { value: query },
      })
      expect(special.getAllByRole('button')).toEqual(buttons)
    }
    expect(screen.getByText('No materials found')).toBeVisible()
  })

  it.each([
    ['Essence', 'Lesser Essence of the Body'],
    ['Essence', 'Essence of Hysteria'],
    ['Alloy', 'Runic Alloy'],
    ['Omen', 'Omen of Whittling'],
    ['Catalysts', 'Flesh Catalyst'],
    ['Liquid Emotions', 'Diluted Liquid Ire'],
  ])(
    'selects %s materials for use on right-click without registering them',
    (label, name) => {
      show()
      fireEvent.click(tab('Essence'))
      pick('Lesser Essence of the Mind')
      fireEvent.click(tab(label))
      const material = screen.getByRole('button', { name })
      fireEvent.pointerOver(material)
      expect(screen.getByRole('tooltip')).toBeVisible()
      expect(fireEvent.contextMenu(material)).toBe(false)
      expect(material).toHaveAttribute('aria-pressed', 'true')
      expect(material).toHaveClass('is-selected')
      expect(screen.getByRole('status')).toHaveTextContent(`${name} selected`)
      expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
      fireEvent.click(favorite(1))
      expect(favorite(1)).toHaveAccessibleName('Favorite slot 1: empty')
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      )
      expect(screen.getByRole('status')).toHaveTextContent(
        'The item has not changed',
      )
      fireEvent.click(material)
      expect(material).toHaveClass('is-held')
      expect(material).not.toHaveClass('is-selected')
      fireEvent.click(favorite(1))
      expect(favorite(1)).toHaveAccessibleName(`Favorite slot 1: ${name}`)
    },
  )

  it('does not display native hover titles on empty or occupied favorites', () => {
    show()
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.click(tab('Essence'))
    pick('Lesser Essence of the Body')
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.click(favorite(1))
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.pointerOver(favorite(1))
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'Lesser Essence of the Body',
    )
  })

  it('excludes every requested Omen and filters each material tab', () => {
    show()
    fireEvent.click(tab('Omen'))
    expect(
      materials
        .filter((m) => m.category === 'Omen')
        .some((m) =>
          /Saga|Refreshment|Resurgence|Amelioration|Gambling|Bartering|Recombination|Chaotic|Chance|Ancients|Abyssal Echoes/.test(
            m.name,
          ),
        ),
    ).toBe(false)
    for (const label of ['Omen', 'Alloy', 'Catalysts', 'Liquid Emotions']) {
      fireEvent.click(tab(label))
      fireEvent.change(screen.getByRole('searchbox'), {
        target: { value: 'nonexistent synthetic material' },
      })
      expect(screen.getByText('No materials found')).toBeVisible()
    }
  })

  it('toggles tooltips across tabs and removes stack size from display only', () => {
    show()
    const toggle = screen.getByRole('checkbox', { name: 'Show tooltips' })
    expect(toggle).toBeChecked()
    expect(screen.queryByText(/^\d+ items$/)).not.toBeInTheDocument()
    const chaos = screen.getByRole('button', { name: 'Chaos Orb' })
    fireEvent.pointerOver(chaos)
    expect(screen.getByRole('tooltip')).not.toHaveTextContent('Stack Size')
    expect(
      tooltips.Chaos_Orb.lines.some((line) => line.startsWith('Stack Size:')),
    ).toBe(true)
    fireEvent.click(toggle)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    expect(chaos).not.toHaveAttribute('aria-describedby')
    fireEvent.pointerOver(chaos)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.click(tab('Essence'))
    const essence = screen.getByRole('button', { name: 'Essence of Hysteria' })
    fireEvent.focus(essence)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.click(toggle)
    fireEvent.pointerOver(essence)
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'Energy Shield Recharge Rate',
    )
    expect(screen.getByRole('tooltip')).not.toHaveTextContent('Stack Size')
  })

  it('hides tooltips during use selection and restores them after cancellation', () => {
    show()
    const chaos = screen.getByRole('button', { name: 'Chaos Orb' })
    fireEvent.pointerOver(chaos)
    expect(screen.getByRole('tooltip')).toBeInTheDocument()
    fireEvent.contextMenu(chaos)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.pointerOver(screen.getByRole('button', { name: 'Divine Orb' }))
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.click(tab('Essence'))
    const essence = screen.getByRole('button', {
      name: 'Lesser Essence of the Body',
    })
    fireEvent.focus(essence)
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.pointerOver(essence)
    expect(screen.getByRole('tooltip')).toBeInTheDocument()
    fireEvent.click(essence)
    expect(screen.getByRole('tooltip')).toBeInTheDocument()
    fireEvent.click(favorite(1))
    fireEvent.contextMenu(favorite(1))
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
  })

  it('shows sourced descriptions on hover and focus, including favorites', () => {
    show()
    fireEvent.pointerOver(screen.getByRole('button', { name: 'Chaos Orb' }))
    expect(screen.getByRole('tooltip')).toHaveTextContent('Chaos Orb')
    expect(screen.getByRole('button', { name: 'Chaos Orb' })).toHaveAttribute(
      'aria-describedby',
      'material-description',
    )
    expect(
      within(screen.getByRole('tooltip')).getByRole('link'),
    ).toHaveAttribute('href', 'https://poe2db.tw/us/Chaos_Orb')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    fireEvent.click(tab('Omen'))
    pick('Omen of Whittling')
    fireEvent.click(favorite(1))
    fireEvent.focus(favorite(1))
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'lowest level modifier',
    )
  })

  it('closes the modal on its backdrop and cancels browser context menus only in the bench', () => {
    show()
    fireEvent.click(screen.getByRole('button', { name: 'Edit item' }))
    fireEvent.click(screen.getByRole('dialog'))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit item' })).toHaveFocus()
    expect(
      fireEvent.contextMenu(document.querySelector('.stash-canvas')!),
    ).toBe(false)
    expect(
      fireEvent.contextMenu(document.querySelector('.craft-header')!),
    ).toBe(true)
  })

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
      ['Essence', 76],
      ['Alloy', 13],
      ['Omen', 32],
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
    expect(materials).toHaveLength(180)
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
