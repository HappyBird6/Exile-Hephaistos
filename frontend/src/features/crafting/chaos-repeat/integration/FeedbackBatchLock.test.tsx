import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it } from 'vitest'
import {
  FeedbackTooltipOutlet,
  FeedbackTooltipProvider,
} from '../../feedback-tooltip/FeedbackTooltipProvider'

it('retains the actual disabled Chaos anchor and locks Escape synchronously while source links remain usable', () => {
  let locked = false
  const content = (anchor: HTMLElement | null, disabled: boolean) => (
    <FeedbackTooltipProvider
      enabled
      priorityMaterialId="Chaos_Orb"
      priorityAnchor={anchor}
      priorityMaterialHint={{ materialId: 'Chaos_Orb', text: 'Repeat hint' }}
      isInteractionLocked={() => locked}
    >
      <button data-material-tooltip="Chaos_Orb" disabled={disabled}>
        Actual Chaos
      </button>
      <button data-material-tooltip="Chaos_Orb">Other Chaos slot</button>
      <FeedbackTooltipOutlet />
    </FeedbackTooltipProvider>
  )
  const mounted = render(content(null, false))
  const source = screen.getByRole('button', { name: 'Actual Chaos' })
  const other = screen.getByRole('button', { name: 'Other Chaos slot' })
  locked = true
  mounted.rerender(content(source, true))
  expect(source).toHaveAttribute('aria-describedby', 'material-description')
  expect(other).not.toHaveAttribute('aria-describedby')
  expect(screen.getByRole('tooltip')).toHaveTextContent('Repeat hint')
  fireEvent.pointerOver(other)
  const link = within(screen.getByRole('tooltip')).getByRole('link', {
    name: 'PoE2DB',
  })
  act(() => link.focus())
  expect(link).toHaveFocus()
  expect(link).toHaveAttribute('href', expect.stringContaining('poe2db.tw'))
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(screen.getByRole('tooltip')).toContainElement(link)
  expect(source).toHaveAttribute('aria-describedby', 'material-description')
  locked = false
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(screen.queryByRole('tooltip')).toBeNull()
})
