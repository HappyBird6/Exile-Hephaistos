import { useState } from 'react'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { setLocale } from '../../../shared/i18n/i18n'
import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import { essenceRows, isSelectableMaterial, materials } from '../materials'
import type { MaterialTab } from '../materials'
import { MaterialTooltip } from '../MaterialTooltip'
import { WorkbenchStash } from './WorkbenchStash'
import type { WorkbenchResource, WorkbenchStashProps } from './stashTypes'

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

function props(
  overrides: Partial<WorkbenchStashProps> = {},
): WorkbenchStashProps {
  return {
    resources,
    excludedIds,
    activeTab: 'Currency',
    onTabChange: vi.fn(),
    selectedId: null,
    activeOmenIds: [],
    favorites: Array(10).fill(null),
    held: null,
    onSelect: vi.fn(),
    onPickUp: vi.fn(),
    onFavoriteActivate: vi.fn(),
    onFavoritePlace: vi.fn(),
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
    <WorkbenchStash
      {...props()}
      activeTab={activeTab}
      onTabChange={onTabChange}
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
  )
}

describe('right-hand controlled workbench stash', () => {
  it('offers the actual Artificer and every retained abyss currency in its currency tab', () => {
    render(<WorkbenchStash {...props()} />)
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
    expect(screen.getByRole('tablist').children).toHaveLength(6)
    expect(screen.getByRole('group').querySelectorAll('button')).toHaveLength(
      10,
    )
    expect(document.querySelector('.workbench-stash')?.lastElementChild).toBe(
      screen.getByRole('group'),
    )
    act(() => setLocale('ko'))
    expect(screen.getByRole('button', { name: '숙련공의 오브' })).toBeVisible()
  })

  it('limits new ordinary essence choices to greater/perfect while retaining every source ID', () => {
    const lower = resource('Lesser_Essence_of_the_Body')
    const base = resource('Essence_of_the_Body')
    render(<WorkbenchStash {...props({ activeTab: 'Essence' })} />)
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
    expect(materials).toHaveLength(180)
    expect(isSelectableMaterial(lower)).toBe(false)
    expect(isSelectableMaterial(base)).toBe(false)
    expect(essenceRows('')).toHaveLength(19)
    expect(essenceRows('').flat()).toHaveLength(38)
    expect(
      essenceRows('greater essence of the body')[0]?.map((m) => m?.id),
    ).toEqual(['Greater_Essence_of_the_Body', 'Perfect_Essence_of_the_Body'])
  })

  it('keeps search per internal tab and moves between tabs with keyboard focus', () => {
    render(<ControlledStash />)
    const currency = screen.getByRole('tab', { name: 'Currency' })
    currency.focus()
    fireEvent.keyDown(currency, { key: 'ArrowRight' })
    expect(screen.getByRole('tab', { name: 'Essence' })).toHaveFocus()
    fireEvent.change(screen.getByRole('searchbox'), {
      target: { value: 'perfect' },
    })
    expect(
      screen.queryByRole('button', { name: 'Greater Essence of the Body' }),
    ).toBeNull()
    fireEvent.click(screen.getByRole('tab', { name: 'Alloy' }))
    expect(screen.getByRole('searchbox')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'Runic Alloy' })).toBeVisible()
    fireEvent.click(screen.getByRole('tab', { name: 'Essence' }))
    expect(screen.getByRole('searchbox')).toHaveValue('perfect')
    const essence = screen.getByRole('tab', { name: 'Essence' })
    fireEvent.keyDown(essence, { key: 'End' })
    expect(screen.getByRole('tab', { name: 'Liquid Emotions' })).toHaveFocus()
    fireEvent.keyDown(screen.getByRole('tab', { name: 'Liquid Emotions' }), {
      key: 'ArrowRight',
    })
    expect(currency).toHaveFocus()
    fireEvent.change(screen.getByRole('searchbox'), {
      target: { value: 'nonexistent material' },
    })
    expect(screen.getByText('No materials found')).toBeVisible()
  })

  it('preserves currency click selection and material pickup versus right-click selection', () => {
    const callbacks = props()
    const view = render(<WorkbenchStash {...callbacks} />)
    fireEvent.click(screen.getByRole('button', { name: 'Chaos Orb' }))
    expect(callbacks.onSelect).toHaveBeenCalledWith(
      resource('Chaos_Orb'),
      undefined,
    )
    expect(callbacks.onPickUp).not.toHaveBeenCalled()
    view.rerender(<WorkbenchStash {...callbacks} activeTab="Alloy" />)
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
    render(<WorkbenchStash {...callbacks} />)
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
    expect(fireEvent.dragOver(favorite(0), { dataTransfer })).toBe(false)
    fireEvent.drop(favorite(0), { dataTransfer })
    expect(callbacks.onFavoritePlace).toHaveBeenCalledExactlyOnceWith(
      0,
      resource('Chaos_Orb'),
    )
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
    const view = render(<WorkbenchStash {...callbacks} />)
    expect(favorite(0)).toHaveAccessibleName(
      `Favorite slot 1: ${resource('Gnawed_Rib').name}`,
    )
    expect(favorite(0)).toHaveAttribute('aria-disabled', 'true')
    fireEvent.click(favorite(0))
    fireEvent.contextMenu(favorite(1))
    expect(callbacks.onFavoriteActivate).not.toHaveBeenCalled()
    view.rerender(
      <WorkbenchStash {...callbacks} held={resource('Runic_Alloy')} />,
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
        <WorkbenchStash {...callbacks} />
        <MaterialTooltip />
      </>,
    )
    expect(favorite(0)).toHaveClass('is-active-omen')
    fireEvent.contextMenu(favorite(0), { clientX: 5, clientY: 10 })
    expect(callbacks.onFavoriteActivate).toHaveBeenCalledWith(
      0,
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
      <WorkbenchStash {...callbacks} favorites={[resource('Runic_Alloy')]} />,
    )
    expect(favorite(0).querySelector('img')).toHaveAttribute(
      'src',
      '/assets/materials/Runic_Alloy.webp',
    )
  })

  it('respects disabled controls and the existing opt-in legacy Omen filter', () => {
    const callbacks = props({
      activeTab: 'Omen',
      onShowLegacyOmensChange: vi.fn(),
      favorites: [resource('Omen_of_Whittling')],
    })
    const view = render(<WorkbenchStash {...callbacks} />)
    expect(
      screen.queryByRole('button', { name: 'Omen of Homogenising Exaltation' }),
    ).toBeNull()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Show legacy Omens' }))
    expect(callbacks.onShowLegacyOmensChange).toHaveBeenCalledWith(true)
    view.rerender(<WorkbenchStash {...callbacks} showLegacyOmens disabled />)
    expect(
      screen.getByRole('button', { name: 'Omen of Homogenising Exaltation' }),
    ).toHaveTextContent('(Legacy)')
    fireEvent.click(screen.getByRole('button', { name: 'Omen of Whittling' }))
    fireEvent.contextMenu(
      screen.getByRole('button', { name: 'Omen of Whittling' }),
    )
    fireEvent.contextMenu(favorite(0))
    expect(callbacks.onSelect).not.toHaveBeenCalled()
    expect(callbacks.onPickUp).not.toHaveBeenCalled()
    expect(callbacks.onFavoriteActivate).not.toHaveBeenCalled()
    expect(screen.getByRole('tab', { name: 'Currency' })).toBeDisabled()
  })
})
