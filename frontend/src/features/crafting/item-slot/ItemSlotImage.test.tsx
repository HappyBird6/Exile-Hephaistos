import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { baseImageSource } from '../base-picker/baseImageSources'
import { ItemSlotImage } from './ItemSlotImage'

const solar = 'Metadata/Items/Amulets/FourAmulet9'
const gloves = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'

it('replaces the exact base art when the current item props change', () => {
  const view = render(<ItemSlotImage baseItemId={solar} label="Solar Amulet" />)
  expect(screen.getByRole('img', { name: 'Solar Amulet' })).toHaveAttribute(
    'src',
    baseImageSource(solar),
  )
  expect(
    fireEvent.dragStart(screen.getByRole('img', { name: 'Solar Amulet' })),
  ).toBe(false)
  view.rerender(<ItemSlotImage baseItemId={gloves} label="Stocky Mitts" />)
  expect(screen.getByRole('img', { name: 'Stocky Mitts' })).toHaveAttribute(
    'src',
    baseImageSource(gloves),
  )
  expect(
    screen.queryByRole('img', { name: 'Solar Amulet' }),
  ).not.toBeInTheDocument()
  view.rerender(<ItemSlotImage baseItemId={solar} label="Restored amulet" />)
  expect(screen.getByRole('img', { name: 'Restored amulet' })).toHaveAttribute(
    'src',
    baseImageSource(solar),
  )
})

it('uses an accessible fallback for absent or broken art and recovers for another base', () => {
  const view = render(
    <ItemSlotImage baseItemId="unknown" label="Unknown base" />,
  )
  expect(screen.getByRole('img', { name: 'Unknown base' }).tagName).toBe('SPAN')
  view.rerender(<ItemSlotImage baseItemId={solar} label="Solar Amulet" />)
  fireEvent.error(screen.getByRole('img', { name: 'Solar Amulet' }))
  expect(screen.getByRole('img', { name: 'Solar Amulet' }).tagName).toBe('SPAN')
  view.rerender(<ItemSlotImage baseItemId={gloves} label="Stocky Mitts" />)
  expect(screen.getByRole('img', { name: 'Stocky Mitts' })).toHaveAttribute(
    'src',
    baseImageSource(gloves),
  )
})
