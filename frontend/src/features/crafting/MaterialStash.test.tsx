import {
  currencyButton,
  outerMaterialSearch,
  outerMaterialTab,
} from '../../shared/test/workbenchQueries'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import { currencies } from './currencies'
import { additionalStashCurrencies, materials } from './materials'
import tooltips from './materialTooltips.json'

function show() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <CraftingPage />
    </QueryClientProvider>,
  )
}
const tab = outerMaterialTab
const favorite = (slot: number) =>
  screen.getByRole('button', { name: new RegExp(`^Favorite slot ${slot}:`) })
const pick = (name: string) =>
  fireEvent.click(
    within(document.getElementById('material-list')!).getByRole('button', {
      name,
    }),
  )
beforeEach(() => useItemDraft.getState().setBase())

describe('Material stash and shared favorites', () => {
  it('has a sourced description for every visible currency and material', () => {
    const descriptions: Record<
      string,
      { name: string; lines: string[]; sourceUrl: string }
    > = tooltips
    for (const entry of [
      ...currencies,
      ...materials,
      ...additionalStashCurrencies,
    ]) {
      expect(descriptions[entry.id]?.lines.length).toBeGreaterThan(0)
      expect(descriptions[entry.id]?.sourceUrl).toBe(
        `https://poe2db.tw/us/${entry.id}`,
      )
    }
  })
  it('keeps search above scrolling rows and groups only the greater and perfect essence tiers', () => {
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
    ).toEqual(['Greater Essence of the Body', 'Perfect Essence of the Body'])
    expect(
      screen.queryByRole('button', { name: 'Lesser Essence of the Body' }),
    ).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Essence of the Body' }),
    ).not.toBeInTheDocument()
    fireEvent.click(tab('Alloy'))
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
    fireEvent.click(tab('Essence'))
    expect(outerMaterialSearch()).toHaveValue('  greater essence of the body ')
    fireEvent.change(outerMaterialSearch(), { target: { value: '' } })
    expect(
      within(
        screen.getByRole('group', { name: 'Special essences' }),
      ).getAllByRole('button'),
    ).toHaveLength(4)
    expect(
      screen.queryByRole('button', { name: 'Runic Alloy' }),
    ).not.toBeInTheDocument()
  })

  it('keeps all four in-scope special essences in place during matching and empty searches', () => {
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
      fireEvent.change(outerMaterialSearch(), {
        target: { value: query },
      })
      expect(special.getAllByRole('button')).toEqual(buttons)
    }
    expect(screen.getByText('No materials found')).toBeVisible()
  })

  it.each([
    ['Essence', 'Greater Essence of the Body'],
    ['Essence', 'Essence of Hysteria'],
    ['Alloy', 'Runic Alloy'],
    ['Catalysts', 'Flesh Catalyst'],
    ['Liquid Emotions', 'Diluted Liquid Ire'],
  ])(
    'selects %s materials for use on right-click without registering them',
    (label, name) => {
      show()
      fireEvent.click(tab('Essence'))
      pick('Greater Essence of the Mind')
      fireEvent.click(tab(label))
      const material = screen.getByRole('button', { name })
      fireEvent.pointerOver(material)
      expect(screen.getByRole('tooltip')).toBeVisible()
      expect(fireEvent.contextMenu(material)).toBe(false)
      expect(material).toHaveAttribute('aria-pressed', 'true')
      expect(material).toHaveClass('is-selected')
      expect(screen.getByRole('status')).toHaveTextContent(`${name} selected`)
      expect(
        document.querySelector('.workbench-feedback-region'),
      ).toContainElement(screen.getByRole('tooltip'))
      expect(screen.getByRole('tooltip')).toHaveTextContent(name)
      fireEvent.click(favorite(1))
      expect(favorite(1)).toHaveAccessibleName('Favorite slot 1: empty')
      expect(material).toHaveAttribute('aria-pressed', 'false')
      fireEvent.contextMenu(material)
      fireEvent.click(
        screen.getByRole('button', {
          name: 'Use selected currency on the central item',
        }),
      )
      expect(screen.getByRole('status')).toHaveTextContent(
        'Wait for the crafting catalog or retry loading it.',
      )
      expect(useItemDraft.getState().source).toBe('base')
      fireEvent.click(material)
      expect(material).toHaveClass('is-held')
      expect(material).not.toHaveClass('is-selected')
      fireEvent.click(favorite(1))
      expect(favorite(1)).toHaveAccessibleName(`Favorite slot 1: ${name}`)
    },
  )

  it('activates and deactivates a stash Omen directly while left-click registers it in favorites', () => {
    show()
    fireEvent.click(tab('Omen'))
    const omen = within(document.getElementById('material-list')!).getByRole(
      'button',
      { name: 'Omen of Whittling' },
    )
    fireEvent.contextMenu(omen)
    expect(useItemDraft.getState().activeOmens).toContain('Omen_of_Whittling')
    expect(omen).toHaveClass('is-active-omen')
    fireEvent.contextMenu(omen)
    expect(useItemDraft.getState().activeOmens).not.toContain(
      'Omen_of_Whittling',
    )
    fireEvent.click(omen)
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Omen of Whittling',
    )
    fireEvent.contextMenu(favorite(1))
    expect(useItemDraft.getState().activeOmens).toContain('Omen_of_Whittling')
  })

  it('offers sourced quality currencies only on the right without repeating a fixed currency', () => {
    show()
    const right = within(document.getElementById('material-list')!)
    for (const resource of additionalStashCurrencies) {
      const button = right.getByRole('button', { name: resource.name })
      expect(button.querySelector('img')).toHaveAttribute('src', resource.image)
      expect(
        within(
          document.querySelector('.currency-catalog') as HTMLElement,
        ).queryByRole('button', { name: resource.name }),
      ).toBeNull()
    }
    expect(right.queryByRole('button', { name: 'Chaos Orb' })).toBeNull()
    expect(currencyButton('Chaos Orb')).toBeVisible()
  })

  it('does not display native hover titles on empty or occupied favorites', () => {
    show()
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.click(tab('Essence'))
    pick('Greater Essence of the Body')
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.click(favorite(1))
    expect(favorite(1)).not.toHaveAttribute('title')
    fireEvent.pointerOver(favorite(1))
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'Greater Essence of the Body',
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
      fireEvent.change(outerMaterialSearch(), {
        target: { value: 'nonexistent synthetic material' },
      })
      expect(screen.getByText('No materials found')).toBeVisible()
    }
  })

  it('shares explanations across tabs and selections without a tooltip toggle', () => {
    show()
    expect(screen.queryByRole('checkbox', { name: 'Show tooltips' })).toBeNull()
    expect(screen.queryByText(/^\d+ items$/)).not.toBeInTheDocument()
    const chaos = currencyButton('Chaos Orb')
    fireEvent.pointerOver(chaos)
    const feedback = document.querySelector('.workbench-feedback-region')!
    expect(feedback).toContainElement(screen.getByRole('tooltip'))
    expect(screen.getByRole('tooltip')).not.toHaveTextContent('Stack Size')
    expect(
      tooltips.Chaos_Orb.lines.some((line) => line.startsWith('Stack Size:')),
    ).toBe(true)
    fireEvent.contextMenu(chaos)
    fireEvent.pointerOver(currencyButton('Divine Orb'))
    expect(screen.getByRole('tooltip')).toHaveTextContent('Divine Orb')
    fireEvent.click(tab('Essence'))
    const essence = screen.getByRole('button', { name: 'Essence of Hysteria' })
    fireEvent.focus(essence)
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'Energy Shield Recharge Rate',
    )
    expect(screen.getByRole('tooltip')).not.toHaveTextContent('Stack Size')
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('tooltip')).toBeNull()
    expect(essence).not.toHaveAttribute('aria-describedby')
  })

  it('shows sourced descriptions on hover and focus, including favorites', () => {
    show()
    fireEvent.pointerOver(currencyButton('Chaos Orb'))
    expect(screen.getByRole('tooltip')).toHaveTextContent('Chaos Orb')
    expect(currencyButton('Chaos Orb')).toHaveAttribute(
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

  it('keeps prior removals, restores the supported Artificer slot and keeps Hinekora positioned', () => {
    show()
    expect(document.getElementById('material-list')!).toHaveAccessibleName(
      'Currency',
    )
    expect(
      within(document.getElementById('material-list')!).getAllByRole('button'),
    ).toHaveLength(11)
    for (const name of [
      'Mirror of Kalandra',
      'Orb of Chance',
      "Arcanist's Etcher",
      "Glassblower's Bauble",
      "Gemcutter's Prism",
      "Lesser Jeweller's Orb",
      "Greater Jeweller's Orb",
      "Perfect Jeweller's Orb",
    ]) {
      expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
    }
    expect(
      currencies.find((item) => item.id === 'Hinekoras_Lock'),
    ).toMatchObject({ x: 329, y: 140 })
    expect(
      currencies.find((item) => item.id === 'Artificers_Orb'),
    ).toMatchObject({ x: 574, y: 240 })
    expect(document.querySelector('.stash-inspector')).toBeNull()
    expect(document.querySelector('.item-panel')).toBeNull()
    expect(
      screen.getByRole('tabpanel', { name: 'Crafting Workbench' }),
    ).toContainElement(screen.getByRole('article'))
    expect(
      screen.queryByRole('heading', { name: 'Next possible states' }),
    ).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('tab', { name: 'State explorer' }))
    expect(
      screen.getByRole('region', { name: 'Crafting probability explorer' }),
    ).toContainElement(screen.getByRole('article'))
    expect(
      screen.getByRole('heading', { name: 'Next possible states' }),
    ).toBeVisible()
  })

  it('shows only the selected catalog while keeping ten favorites and the current card', () => {
    show()
    const card = screen.getByRole('article')
    for (const [label, count] of [
      ['Essence', 38], // 19 ordinary families × two selectable tiers; special group is separate.
      ['Alloy', 13],
      ['Omen', 18],
      ['Catalysts', 26],
      ['Liquid Emotions', 27],
    ] as const) {
      fireEvent.click(tab(label))
      expect(tab(label)).toHaveAttribute('aria-selected', 'true')
      expect(
        within(document.getElementById('material-list')!).getAllByRole(
          'button',
        ),
      ).toHaveLength(count)
      if (label === 'Essence') {
        expect(
          screen.queryByRole('button', { name: 'Essence of Delirium' }),
        ).not.toBeInTheDocument()
        expect(
          screen.queryByRole('button', { name: 'Essence of Insanity' }),
        ).not.toBeInTheDocument()
        expect(
          within(screen.getByRole('group', { name: 'Special essences' }))
            .getAllByRole('button')
            .map((button) => button.getAttribute('aria-label')),
        ).toEqual([
          'Essence of Hysteria',
          'Essence of Horror',
          'Essence of the Abyss',
          'Essence of the Breach',
        ])
      }
      if (label === 'Omen') {
        expect(
          screen.queryByRole('checkbox', { name: 'Show legacy Omens' }),
        ).toBeNull()
        for (const name of [
          'Omen of Corruption',
          'Omen of Putrefaction',
          'Omen of Answered Prayers',
          'Omen of the Hunt',
          'Omen of Secret Compartments',
          'Omen of Reinforcements',
          'Omen of Homogenising Exaltation',
        ])
          expect(screen.queryByRole('button', { name })).not.toBeInTheDocument()
      }
      expect(
        within(
          screen.getByRole('group', { name: 'Shared material favorites' }),
        ).getAllByRole('button'),
      ).toHaveLength(10)
      expect(screen.getByRole('article')).toBe(card)
      expect(
        within(document.getElementById('material-list')!).queryByRole(
          'button',
          { name: 'Chaos Orb' },
        ),
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
    pick('Greater Essence of the Body')
    fireEvent.click(tab('Currency'))
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Greater Essence of the Body',
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'registered in favorite slot 1',
    )
    fireEvent.click(favorite(2))
    expect(favorite(2)).toHaveAccessibleName('Favorite slot 2: empty')
    expect(favorite(1)).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(tab('Essence'))
    expect(
      screen.getByRole('button', { name: 'Greater Essence of the Body' }),
    ).toBeVisible()
    pick('Greater Essence of the Mind')
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Greater Essence of the Mind',
    )
    fireEvent.click(tab('Omen'))
    expect(favorite(1)).toHaveAccessibleName(
      'Favorite slot 1: Greater Essence of the Mind',
    )
  })

  it('Escape cancels pickup, and selecting a favorite for use cannot overwrite another slot', () => {
    show()
    fireEvent.click(tab('Essence'))
    pick('Greater Essence of the Body')
    fireEvent.keyDown(window, { key: 'Escape' })
    fireEvent.click(favorite(1))
    expect(favorite(1)).toHaveAccessibleName('Favorite slot 1: empty')
    pick('Greater Essence of the Body')
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
      'Select a currency from the stash first.',
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
    pick('Greater Essence of the Body')
    fireEvent.click(favorite(1))
    expect(favorite(1).querySelector('img')).toHaveAttribute(
      'src',
      '/assets/materials/Greater_Essence_of_the_Body.webp',
    )
    expect(favorite(1)).not.toHaveTextContent('Image unavailable')
  })

  it('does not persist favorites after leaving the page and remounting', () => {
    const view = show()
    fireEvent.click(tab('Essence'))
    pick('Greater Essence of the Body')
    fireEvent.click(favorite(10))
    view.unmount()
    show()
    expect(favorite(10)).toHaveAccessibleName('Favorite slot 10: empty')
  })
})
