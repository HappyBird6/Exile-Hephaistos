import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { SessionDetail } from './SessionDetail'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import type { Film } from '../workbenchHistory'

it.each(['stack', 'columns'] as const)(
  'delegates bounded navigation and reuse in %s layout without changing saved frames',
  (layout) => {
    setLocale('en')
    const frame = { state: concreteInitial(initialFixture), action: null }
    const film: Film = {
      id: 'test',
      createdAt: '2026-11-12T00:00:00Z',
      frames: [frame, frame],
    }
    const before = JSON.stringify(film)
    const navigate = vi.fn()
    const reuse = vi.fn()
    const { rerender } = render(
      <SessionDetail
        layout={layout}
        film={film}
        cursor={0}
        item={<p>Item</p>}
        onNavigate={navigate}
        actions={<button onClick={reuse}>Reuse</button>}
      />,
    )
    expect(
      screen.getByRole('button', { name: 'Previous crafting step' }),
    ).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Next crafting step' }))
    expect(navigate).toHaveBeenCalledWith(1)
    fireEvent.click(screen.getByText('Reuse'))
    expect(reuse).toHaveBeenCalledOnce()
    rerender(
      <SessionDetail
        layout={layout}
        film={film}
        cursor={1}
        item={<p>Item</p>}
        onNavigate={navigate}
        disabled
      />,
    )
    expect(
      screen.getByRole('button', { name: 'Previous crafting step' }),
    ).toBeDisabled()
    expect(
      screen.getByRole('button', { name: 'Next crafting step' }),
    ).toBeDisabled()
    expect(JSON.stringify(film)).toBe(before)
  },
)
