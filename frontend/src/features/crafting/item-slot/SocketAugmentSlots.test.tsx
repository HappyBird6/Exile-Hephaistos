import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { canInstallAugment, socketText } from '../socketAugments'
import { stashDragType } from '../stash/stashDragType'
import { SocketAugmentSlots } from './SocketAugmentSlots'

const item = {
  ...concreteInitial(initialFixture),
  baseItemId: 'Metadata/Items/Armours/Gloves/FourGlovesStr1',
  implicits: [],
  augmentSockets: 3,
}
const id = 'Perfect_Desert_Rune'
const transfer = (value = id) => ({
  types: [stashDragType],
  getData: () => value,
  dropEffect: 'none',
})

it.each(locales)(
  'keeps %s slot names, installed icons and separate central click targets',
  (locale) => {
    setLocale(locale)
    const install = vi.fn()
    const central = vi.fn()
    render(
      <div>
        <button onClick={central}>Central</button>
        <SocketAugmentSlots
          item={{
            ...item,
            installedAugments: [{ slotIndex: 1, augmentId: 'Desert_Rune' }],
          }}
          augmentId={id}
          ready
          onInstall={install}
        />
      </div>,
    )
    const slots = screen.getAllByRole('button').slice(1)
    expect(slots).toHaveLength(3)
    expect(slots[0]).toHaveAccessibleName(
      `${socketText().socket} 1: ${socketText().empty}`,
    )
    expect(slots[1]!.querySelector('img')).not.toBeNull()
    expect(slots[0]!.closest('button button')).toBeNull()
    fireEvent.click(slots[1]!)
    expect(install).toHaveBeenCalledExactlyOnceWith(1, id)
    expect(central).not.toHaveBeenCalled()
  },
)

it('accepts only a matching active Augment drag and clears the target after drop', () => {
  const install = vi.fn()
  render(
    <SocketAugmentSlots
      item={item}
      augmentId={id}
      draggedAugmentId={id}
      ready
      onInstall={install}
    />,
  )
  const slot = screen.getAllByRole('button')[2]!
  const dataTransfer = transfer()
  expect(fireEvent.dragOver(slot, { dataTransfer })).toBe(false)
  expect(slot).toHaveClass('is-drop-target')
  fireEvent.dragLeave(slot)
  expect(slot).not.toHaveClass('is-drop-target')
  fireEvent.drop(slot, { dataTransfer: transfer('Perfect_Storm_Rune') })
  expect(install).not.toHaveBeenCalled()
  fireEvent.drop(slot, { dataTransfer })
  expect(install).toHaveBeenCalledExactlyOnceWith(2, id)
  expect(slot).not.toHaveClass('is-drop-target')
})

it('does not show eligibility or accept a drop while disabled', () => {
  const install = vi.fn()
  const view = render(
    <SocketAugmentSlots
      item={item}
      augmentId={id}
      draggedAugmentId={id}
      ready
      onInstall={install}
    />,
  )
  view.rerender(
    <SocketAugmentSlots
      item={item}
      augmentId={id}
      draggedAugmentId={id}
      ready={false}
      onInstall={install}
    />,
  )
  for (const slot of screen.getAllByRole('button')) {
    expect(slot).toBeDisabled()
    expect(slot).not.toHaveClass('is-available')
    fireEvent.click(slot)
    fireEvent.drop(slot, { dataTransfer: transfer() })
  }
  expect(install).not.toHaveBeenCalled()
})

it('ties eligibility to selectable IDs, sockets, base class and verified ordinary state', () => {
  expect(canInstallAugment(item, id)).toBe(true)
  for (const unsupported of [undefined, 'Unknown', 'Desert_Rune'])
    expect(canInstallAugment(item, unsupported)).toBe(false)
  expect(canInstallAugment({ ...item, augmentSockets: 0 }, id)).toBe(false)
  expect(
    canInstallAugment(
      { ...item, baseItemId: 'Metadata/Items/Amulets/FourAmulet9' },
      id,
    ),
  ).toBe(false)
  expect(canInstallAugment({ ...item, conditions: ['SANCTIFIED'] }, id)).toBe(
    false,
  )
  expect(
    canInstallAugment(
      {
        ...item,
        observations: { socketCount: 3, augmentEffects: [], grantedSkills: [] },
      },
      id,
    ),
  ).toBe(false)
})

it('allows Ancient replacement only in its existing slot and rejects a restricted drop elsewhere', () => {
  const install = vi.fn()
  const id = 'Carved_Mischief'
  render(
    <SocketAugmentSlots
      item={{
        ...item,
        installedAugments: [{ slotIndex: 1, augmentId: 'Carved_Majesty' }],
      }}
      augmentId={id}
      draggedAugmentId={id}
      ready
      onInstall={install}
    />,
  )
  const slots = screen.getAllByRole('button')
  expect(slots[0]).toBeDisabled()
  expect(slots[0]).not.toHaveClass('is-available')
  expect(slots[1]).toBeEnabled()
  expect(slots[1]).toHaveClass('is-available')
  expect(fireEvent.dragOver(slots[0]!, { dataTransfer: transfer(id) })).toBe(
    true,
  )
  fireEvent.drop(slots[0]!, { dataTransfer: transfer(id) })
  expect(install).not.toHaveBeenCalled()
  fireEvent.click(slots[1]!)
  expect(install).toHaveBeenCalledExactlyOnceWith(1, id)
})
