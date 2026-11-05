import { useState } from 'react'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import {
  gameName,
  locales,
  setLocale,
  translate,
} from '../../../shared/i18n/i18n'
import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import {
  essenceRows,
  essenceGridRows,
  isSelectableMaterial,
  materials,
} from '../materials'
import type { MaterialTab } from '../materials'
import { MaterialTooltip } from '../MaterialTooltip'
import { WorkbenchStash } from './WorkbenchStash'
import { WorkbenchFavorites } from './WorkbenchFavorites'
import {
  visibilityPolicy,
  legacyStashOmenIds,
  excludedStashOmenIds,
} from './visibilityPolicy'
import type {
  WorkbenchResource,
  WorkbenchStashProps,
  WorkbenchFavoritesProps,
} from './stashTypes'

type SurfaceProps = WorkbenchStashProps & WorkbenchFavoritesProps
function StashSurface(props: SurfaceProps) {
  return (
    <>
      <span hidden id={`tab-${props.activeTab}`}>
        {props.activeTab.replaceAll('_', ' ')}
      </span>
      <WorkbenchStash {...props} />
      <div className="stash-canvas__content">
        <WorkbenchFavorites {...props} />
      </div>
    </>
  )
}

const resources: WorkbenchResource[] = [
  ...currencies.map((currency) => ({
    ...currency,
    category: 'Currency' as const,
    name: currencyNames[currency.id],
  })),
  ...materials,
]
const resource = (id: string) => resources.find((entry) => entry.id === id)!
const excludedIds = ['Gnawed_Rib', 'Gnawed_Jawbone', 'Gnawed_Collarbone']
const favorite = (index: number) =>
  within(
    screen.getByRole('group', { name: 'Shared material favorites' }),
  ).getAllByRole('button')[index]!

function props(overrides: Partial<SurfaceProps> = {}): SurfaceProps {
  return {
    resources,
    excludedIds,
    activeTab: 'Currency',
    selectedId: null,
    activeOmenIds: [],
    favorites: Array(10).fill(null),
    held: null,
    onSelect: vi.fn(),
    onPickUp: vi.fn(),
    onFavoriteActivate: vi.fn(),
    onFavoritePlace: vi.fn(),
    onActivateOmen: vi.fn(),
    ...overrides,
  }
}

function ControlledStash() {
  const [activeTab, onTabChange] = useState<MaterialTab>('Currency')
  const [held, setHeld] = useState<WorkbenchResource | null>(null)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [favorites, setFavorites] = useState<(WorkbenchResource | null)[]>(
    Array(10).fill(null),
  )
  return (
    <>
      <div role="tablist" aria-label="Existing category selector">
        {(
          [
            'Currency',
            'Essence',
            'Alloy',
            'Omen',
            'Catalysts',
            'Liquid_Emotions',
          ] as const
        ).map((tab) => (
          <button
            key={tab}
            role="tab"
            aria-selected={activeTab === tab}
            onClick={() => onTabChange(tab)}
          >
            {tab.replaceAll('_', ' ')}
          </button>
        ))}
      </div>
      <StashSurface
        {...props()}
        activeTab={activeTab}
        held={held}
        selectedId={selectedId}
        favorites={favorites}
        onPickUp={(entry) => {
          setHeld(entry)
          setSelectedId(null)
        }}
        onSelect={(entry) => {
          setHeld(null)
          setSelectedId(entry.id)
        }}
        onFavoritePlace={(index, entry) => {
          setFavorites((current) =>
            current.map((value, slot) => (slot === index ? entry : value)),
          )
          setHeld(null)
        }}
        onFavoriteActivate={(_, entry) => setSelectedId(entry?.id ?? null)}
      />
    </>
  )
}

describe('right-hand controlled workbench stash', () => {
  it('offers the actual Artificer and every retained abyss currency in its currency tab', () => {
    render(<StashSurface {...props()} />)
    const catalog = within(screen.getByRole('tabpanel', { name: 'Currency' }))
    expect(
      catalog.getByRole('button', { name: "Artificer's Orb" }),
    ).toBeVisible()
    for (const id of [
      'Preserved_Rib',
      'Ancient_Rib',
      'Preserved_Jawbone',
      'Ancient_Jawbone',
      'Preserved_Collarbone',
      'Ancient_Collarbone',
      'Preserved_Cranium',
      'Fracturing_Orb',
    ])
      expect(
        catalog.getByRole('button', { name: resource(id).name }),
      ).toBeVisible()
    for (const id of excludedIds)
      expect(
        catalog.queryByRole('button', { name: resource(id).name }),
      ).toBeNull()
    expect(screen.queryByRole('tablist')).toBeNull()
    expect(screen.getByRole('group').querySelectorAll('button')).toHaveLength(
      10,
    )
    expect(document.querySelector('.workbench-stash')).not.toContainElement(
      screen.getByRole('group'),
    )
    expect(document.querySelector('.workbench-stash__name')).toBeNull()
    for (const locale of locales) {
      act(() => setLocale(locale))
      const localized = gameName('Chaos_Orb', 'Chaos Orb', locale)
      const chaos = screen.getByRole('button', { name: localized })
      expect(chaos.querySelector('img')).toHaveAttribute(
        'src',
        resource('Chaos_Orb').image,
      )
      expect(chaos).not.toHaveTextContent(localized)
      expect(chaos).toHaveAttribute('data-material-tooltip', 'Chaos_Orb')
    }
    act(() => setLocale('ko'))
    expect(screen.getByRole('button', { name: '숙련공의 오브' })).toBeVisible()
  })

  it('shows a generic missing-image notice instead of the item name while retaining its accessible identity', () => {
    const callbacks = props({
      resources: [{ ...resource('Chaos_Orb'), image: '' }],
    })
    const view = render(<StashSurface {...callbacks} />)
    const chaos = screen.getByRole('button', { name: 'Chaos Orb' })
    expect(chaos.querySelector('.currency-name-fallback')).toBeNull()
    expect(chaos.querySelector('.currency-missing')).toHaveTextContent(
      'Image unavailable',
    )
    expect(chaos).not.toHaveTextContent('Chaos Orb')
    expect(chaos).toHaveAccessibleName('Chaos Orb')
    expect(chaos).toHaveAttribute('data-material-tooltip', 'Chaos_Orb')
    expect(chaos.querySelector('.workbench-stash__image')).toContainElement(
      chaos.querySelector('.currency-missing'),
    )
    expect(chaos.querySelector('.workbench-stash__name')).toBeNull()
    for (const locale of locales) {
      act(() => setLocale(locale))
      const localized = gameName('Chaos_Orb', 'Chaos Orb', locale)
      expect(chaos).toHaveAccessibleName(localized)
      expect(chaos).not.toHaveTextContent(localized)
      expect(chaos.querySelector('.currency-missing')).toHaveTextContent(
        translate('ui.image_unavailable', {}, locale),
      )
    }
    act(() => setLocale('en'))
    view.rerender(
      <StashSurface {...callbacks} resources={[resource('Chaos_Orb')]} />,
    )
    expect(chaos.querySelector('img')).toHaveAttribute(
      'src',
      resource('Chaos_Orb').image,
    )
    fireEvent.error(chaos.querySelector('img')!)
    expect(chaos.querySelector('.currency-missing')).toHaveTextContent(
      'Image unavailable',
    )
    expect(chaos.querySelector('.currency-missing')).toHaveAttribute(
      'title',
      'Chaos Orb · Image unavailable',
    )
    expect(chaos).toHaveAccessibleName('Chaos Orb')
    expect(chaos).not.toHaveTextContent('Chaos Orb')
  })

  it('limits new ordinary essence choices to greater/perfect while retaining every source ID', () => {
    const lower = resource('Lesser_Essence_of_the_Body')
    const base = resource('Essence_of_the_Body')
    render(<StashSurface {...props({ activeTab: 'Essence' })} />)
    expect(screen.queryByRole('button', { name: lower.name })).toBeNull()
    expect(screen.queryByRole('button', { name: base.name })).toBeNull()
    expect(
      screen.getByRole('button', { name: 'Greater Essence of the Body' }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Perfect Essence of the Body' }),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Essence of Hysteria' }),
    ).toBeVisible()
    expect(materials).toHaveLength(259)
    expect(isSelectableMaterial(lower)).toBe(false)
    expect(isSelectableMaterial(base)).toBe(false)
    expect(essenceRows('')).toHaveLength(19)
    expect(essenceRows('').flat()).toHaveLength(38)
    expect(
      essenceRows('greater essence of the body')[0]?.map((m) => m?.id),
    ).toEqual(['Greater_Essence_of_the_Body', 'Perfect_Essence_of_the_Body'])
  })

  it('keeps search per category controlled by the existing selector', () => {
    render(<ControlledStash />)
    const currency = screen.getByRole('tab', { name: 'Currency' })
    fireEvent.click(screen.getByRole('tab', { name: 'Essence' }))
    fireEvent.change(screen.getByRole('searchbox'), {
      target: { value: 'perfect' },
    })
    expect(
      screen.queryByRole('button', { name: 'Greater Essence of the Body' }),
    ).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Alloy' }))
    expect(screen.getByRole('searchbox')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Essence' }))
    expect(screen.getByRole('searchbox')).toHaveValue('perfect')
    fireEvent.click(currency)
    fireEvent.change(screen.getByRole('searchbox'), {
      target: { value: 'nonexistent material' },
    })
    expect(screen.getByText('No materials found')).toBeVisible()
  })

  it('preserves currency click selection and material pickup versus right-click selection', () => {
    const callbacks = props()
    const view = render(<StashSurface {...callbacks} />)
    const chaos = screen.getByRole('button', { name: 'Chaos Orb' })
    expect(chaos).toHaveAttribute('type', 'button')
    act(() => chaos.focus())
    expect(chaos).toHaveFocus()
    fireEvent.click(chaos, { detail: 0 })
    expect(callbacks.onSelect).toHaveBeenCalledWith(
      resource('Chaos_Orb'),
      undefined,
    )
    expect(callbacks.onPickUp).not.toHaveBeenCalled()
    view.rerender(<StashSurface {...callbacks} activeTab="Alloy" />)
    const alloy = screen.getByRole('button', { name: 'Runic Alloy' })
    fireEvent.click(alloy)
    expect(callbacks.onPickUp).toHaveBeenCalledWith(
      resource('Runic_Alloy'),
      undefined,
    )
    expect(fireEvent.contextMenu(alloy, { clientX: 20, clientY: 30 })).toBe(
      false,
    )
    expect(callbacks.onSelect).toHaveBeenLastCalledWith(
      resource('Runic_Alloy'),
      {
        x: 20,
        y: 30,
      },
    )
  })

  it('carries a picked material across tabs and replaces favorites without consuming its source', () => {
    render(<ControlledStash />)
    fireEvent.click(screen.getByRole('tab', { name: 'Alloy' }))
    fireEvent.click(screen.getByRole('button', { name: 'Runic Alloy' }))
    fireEvent.click(screen.getByRole('tab', { name: 'Currency' }))
    fireEvent.click(favorite(9))
    expect(favorite(9)).toHaveAccessibleName('Favorite slot 10: Runic Alloy')
    fireEvent.contextMenu(favorite(9))
    expect(favorite(9)).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(favorite(8))
    expect(favorite(8)).toHaveAccessibleName('Favorite slot 9: empty')
    expect(favorite(9)).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(screen.getByRole('tab', { name: 'Alloy' }))
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: 'Adaptive Alloy' }))
    fireEvent.click(favorite(9))
    expect(favorite(9)).toHaveAccessibleName('Favorite slot 10: Adaptive Alloy')
  })

  it('accepts a local resource drag and refuses arbitrary external drop IDs', () => {
    const callbacks = props()
    const view = render(<StashSurface {...callbacks} />)
    const data = new Map<string, string>()
    const dataTransfer = {
      setData: (type: string, value: string) => data.set(type, value),
      getData: (type: string) => data.get(type) ?? '',
      effectAllowed: '',
      dropEffect: '',
    }
    fireEvent.drop(favorite(0), { dataTransfer })
    expect(callbacks.onFavoritePlace).not.toHaveBeenCalled()
    fireEvent.dragStart(screen.getByRole('button', { name: 'Chaos Orb' }), {
      dataTransfer,
    })
    view.rerender(<StashSurface {...callbacks} held={resource('Chaos_Orb')} />)
    expect(fireEvent.dragOver(favorite(0), { dataTransfer })).toBe(false)
    fireEvent.drop(favorite(0), { dataTransfer })
    expect(callbacks.onFavoritePlace).toHaveBeenCalledExactlyOnceWith(
      0,
      resource('Chaos_Orb'),
    )
    view.rerender(<StashSurface {...callbacks} held={null} />)
    fireEvent.drop(favorite(1), { dataTransfer })
    expect(callbacks.onFavoritePlace).toHaveBeenCalledTimes(1)
  })

  it('preserves retained unavailable favorites and permits replacing them without new use', () => {
    const callbacks = props({
      favorites: [
        resource('Gnawed_Rib'),
        resource('Lesser_Essence_of_the_Body'),
      ],
    })
    const view = render(<StashSurface {...callbacks} />)
    expect(favorite(0)).toHaveAccessibleName(
      `Favorite slot 1: ${resource('Gnawed_Rib').name}`,
    )
    expect(favorite(0)).toHaveAttribute('aria-disabled', 'true')
    fireEvent.click(favorite(0))
    fireEvent.contextMenu(favorite(1))
    expect(callbacks.onFavoriteActivate).not.toHaveBeenCalled()
    view.rerender(
      <StashSurface {...callbacks} held={resource('Runic_Alloy')} />,
    )
    fireEvent.click(favorite(0))
    expect(callbacks.onFavoritePlace).toHaveBeenCalledWith(
      0,
      resource('Runic_Alloy'),
    )
  })

  it('delegates active Omen favorites, shows sourced tooltips and resets failed images', () => {
    const callbacks = props({
      activeOmenIds: ['Omen_of_Whittling'],
      favorites: [resource('Omen_of_Whittling')],
    })
    const view = render(
      <>
        <StashSurface {...callbacks} />
        <MaterialTooltip />
      </>,
    )
    expect(favorite(0)).toHaveClass('is-active-omen')
    fireEvent.contextMenu(favorite(0), { clientX: 5, clientY: 10 })
    expect(callbacks.onActivateOmen).toHaveBeenCalledWith(
      resource('Omen_of_Whittling'),
      { x: 5, y: 10 },
    )
    fireEvent.pointerOver(favorite(0))
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      'lowest level modifier',
    )
    expect(favorite(0)).not.toHaveAttribute('title')
    fireEvent.error(favorite(0).querySelector('img')!)
    expect(favorite(0)).toHaveTextContent('Image unavailable')
    view.rerender(
      <StashSurface {...callbacks} favorites={[resource('Runic_Alloy')]} />,
    )
    expect(favorite(0).querySelector('img')).toHaveAttribute(
      'src',
      '/assets/materials/Runic_Alloy.webp',
    )
  })

  it('permanently excludes legacy and the five requested Omen IDs without deleting retained favorites', () => {
    const hiddenIds = [...legacyStashOmenIds, ...excludedStashOmenIds]
    for (const id of hiddenIds) {
      const retained = resource(id)
      expect(retained).toBeDefined()
      expect(visibilityPolicy(retained)).toBe(false)
      const callbacks = props({
        activeTab: 'Omen',
        favorites: [retained],
        showLegacyOmens: true,
      })
      const view = render(<StashSurface {...callbacks} />)
      expect(
        screen.queryByRole('checkbox', { name: 'Show legacy Omens' }),
      ).toBeNull()
      expect(
        within(screen.getByRole('tabpanel')).queryByRole('button', {
          name: retained.name,
        }),
      ).toBeNull()
      expect(favorite(0)).toHaveAccessibleName(
        `Favorite slot 1: ${retained.name}`,
      )
      expect(favorite(0)).toHaveAttribute('aria-disabled', 'true')
      fireEvent.click(favorite(0))
      fireEvent.contextMenu(favorite(0))
      expect(callbacks.onFavoriteActivate).not.toHaveBeenCalled()
      expect(callbacks.onActivateOmen).not.toHaveBeenCalled()
      view.unmount()
    }
  })

  it('activates an Omen directly on right-click while left-click still picks it up', () => {
    const callbacks = props({
      activeTab: 'Omen',
      favorites: [resource('Omen_of_Whittling')],
    })
    const view = render(<StashSurface {...callbacks} />)
    const omen = within(screen.getByRole('tabpanel')).getByRole('button', {
      name: 'Omen of Whittling',
    })
    fireEvent.click(omen)
    expect(callbacks.onPickUp).toHaveBeenCalledWith(
      resource('Omen_of_Whittling'),
      undefined,
    )
    fireEvent.contextMenu(omen, { clientX: 5, clientY: 10 })
    fireEvent.contextMenu(favorite(0), { clientX: 15, clientY: 20 })
    expect(callbacks.onActivateOmen).toHaveBeenNthCalledWith(
      1,
      resource('Omen_of_Whittling'),
      { x: 5, y: 10 },
    )
    expect(callbacks.onActivateOmen).toHaveBeenNthCalledWith(
      2,
      resource('Omen_of_Whittling'),
      { x: 15, y: 20 },
    )
    expect(callbacks.onSelect).not.toHaveBeenCalled()
    view.rerender(<StashSurface {...callbacks} disabled />)
    fireEvent.contextMenu(screen.getByRole('tabpanel').querySelector('button')!)
    fireEvent.contextMenu(favorite(0))
    expect(callbacks.onActivateOmen).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('searchbox')).toBeDisabled()
  })

  it('excludes only actual fixed shelf Currency IDs from the right tab, leaving favorites selectable', () => {
    const callbacks = props({
      fixedCurrencyIds: ['Chaos_Orb', 'Runic_Alloy'],
      favorites: [resource('Chaos_Orb')],
    })
    const view = render(<StashSurface {...callbacks} />)
    const panel = screen.getByRole('tabpanel')
    expect(
      within(panel).queryByRole('button', { name: 'Chaos Orb' }),
    ).toBeNull()
    expect(
      within(panel).getByRole('button', { name: "Artificer's Orb" }),
    ).toBeVisible()
    expect(
      within(panel).getByRole('button', { name: 'Preserved Rib' }),
    ).toBeVisible()
    expect(favorite(0)).toHaveAttribute('aria-disabled', 'false')
    fireEvent.click(favorite(0))
    expect(callbacks.onFavoriteActivate).toHaveBeenCalledWith(
      0,
      resource('Chaos_Orb'),
    )
    view.rerender(<StashSurface {...callbacks} activeTab="Alloy" />)
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
  })

  it('packs two tier pairs into four-column rows and keeps special essences separate', () => {
    render(<StashSurface {...props({ activeTab: 'Essence' })} />)
    expect(essenceGridRows('')).toHaveLength(10)
    expect(essenceGridRows('').flat().filter(Boolean)).toHaveLength(38)
    expect(essenceGridRows('')[9]?.slice(2)).toEqual([null, null])
    expect(document.querySelectorAll('.essence-row')).toHaveLength(10)
    expect(
      document.querySelector('.essence-row')?.querySelectorAll('button'),
    ).toHaveLength(4)
    expect(
      essenceGridRows('greater essence of the body')[0]?.map(
        (entry) => entry?.id ?? null,
      ),
    ).toEqual([
      'Greater_Essence_of_the_Body',
      'Perfect_Essence_of_the_Body',
      null,
      null,
    ])
    expect(
      within(
        screen.getByRole('group', { name: 'Special essences' }),
      ).getAllByRole('button'),
    ).toHaveLength(4)
  })
})
