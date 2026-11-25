import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { SessionDetail } from './SessionDetail'
import { setLocale } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import type { Film } from '../workbenchHistory'
import { useState } from 'react'

it('moves only the visible session within its steps and leaves typing alone', () => {
  setLocale('en')
  const frame = { state: concreteInitial(initialFixture), action: null }
  const film: Film = {
    id: 'steps',
    createdAt: '2026-11-12T00:00:00Z',
    frames: [frame, frame],
  }
  const hiddenNavigate = vi.fn()
  function Sessions() {
    const [cursor, setCursor] = useState(0)
    return (
      <>
        <div hidden>
          <SessionDetail
            film={film}
            cursor={0}
            onNavigate={hiddenNavigate}
            item="Hidden"
          />
        </div>
        <SessionDetail
          film={film}
          cursor={cursor}
          onNavigate={setCursor}
          item={<input aria-label="Name" />}
        />
      </>
    )
  }
  render(<Sessions />)
  const previous = screen.getByRole('button', {
    name: 'Previous crafting step',
  })
  const next = screen.getByRole('button', { name: 'Next crafting step' })
  expect(fireEvent.keyDown(document.body, { key: 'q' })).toBe(true)
  fireEvent.keyDown(screen.getByRole('textbox'), { key: 'e' })
  fireEvent.keyDown(document.body, { key: 'e', ctrlKey: true })
  expect(previous).toBeDisabled()
  fireEvent.keyDown(document.body, { key: 'E' })
  expect(next).toBeDisabled()
  expect(fireEvent.keyDown(document.body, { key: 'e' })).toBe(true)
  fireEvent.keyDown(document.body, { key: 'ㅂ', code: 'KeyQ' })
  expect(next).toBeEnabled()
  expect(hiddenNavigate).not.toHaveBeenCalled()
})

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
