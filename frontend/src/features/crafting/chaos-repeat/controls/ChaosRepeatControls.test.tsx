import { readFileSync } from 'node:fs'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { locales, setLocale } from '../../../../shared/i18n/i18n'
import { ChaosRepeatControls } from './ChaosRepeatControls'
import { chaosRepeatControlMessages } from './messages'
import { chaosRepeatCurrencyIds } from './types'
import type { ChaosRepeatControlsProps } from './types'
function props(
  overrides: Partial<ChaosRepeatControlsProps> = {},
): ChaosRepeatControlsProps {
  return {
    enabled: false,
    pattern: '',
    running: false,
    onEnabledChange: vi.fn(),
    onPatternChange: vi.fn(),
    ...overrides,
  }
}
afterEach(() => {
  setLocale('en')
  vi.restoreAllMocks()
})
it.each(locales)(
  'renders only the fixed-use button and pattern input in %s',
  (locale) => {
    act(() => setLocale(locale))
    render(<ChaosRepeatControls {...props()} />)
    expect(screen.getByRole('group').children).toHaveLength(2)
    expect(screen.getAllByRole('button')).toHaveLength(1)
    expect(screen.getAllByRole('textbox')).toHaveLength(1)
    expect(screen.getByRole('button')).toHaveTextContent('1000 x')
    expect(screen.getByRole('button')).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByRole('textbox')).toHaveAttribute(
      'placeholder',
      chaosRepeatControlMessages[locale].patternPlaceholder,
    )
    expect(screen.queryByRole('spinbutton')).toBeNull()
    expect(screen.queryByRole('switch')).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(screen.queryByText('ON')).toBeNull()
    expect(screen.queryByText('OFF')).toBeNull()
  },
)
it('uses the exact Korean placeholder and toggles through a keyboard-generated click without changing input', () => {
  act(() => setLocale('ko'))
  const input = props()
  const network = vi.spyOn(globalThis, 'fetch')
  render(<ChaosRepeatControls {...input} />)
  expect(screen.getByRole('textbox')).toHaveAttribute(
    'placeholder',
    '정규식 패턴을 입력해주세요',
  )
  const button = screen.getByRole('button')
  act(() => button.focus())
  expect(button).toHaveFocus()
  fireEvent.click(button, { detail: 0 })
  expect(input.onEnabledChange).toHaveBeenCalledExactlyOnceWith(true)
  expect(input.onPatternChange).not.toHaveBeenCalled()
  expect(network).not.toHaveBeenCalled()
})
it.each(chaosRepeatCurrencyIds)(
  'keeps the selected %s variant on the use button',
  (currencyId) => {
    const input = props({ currencyId })
    render(<ChaosRepeatControls {...input} />)
    expect(screen.getByRole('group')).toHaveAttribute(
      'data-chaos-repeat-currency',
      currencyId,
    )
    expect(screen.getByRole('group').querySelector('img')).toHaveAttribute(
      'alt',
      '',
    )
    fireEvent.click(screen.getByRole('button'))
    expect(input.onEnabledChange).toHaveBeenCalledExactlyOnceWith(true)
  },
)
it('forwards regex source unchanged and leaves rejection text in the shared feedback area', () => {
  const input = props()
  const view = render(<ChaosRepeatControls {...input} />)
  for (const value of ['[(', '  +12 Fire  ', '(?i)test', '']) {
    fireEvent.change(screen.getByRole('textbox'), { target: { value } })
    expect(input.onPatternChange).toHaveBeenLastCalledWith(value)
    view.rerender(<ChaosRepeatControls {...input} pattern={value} />)
    expect(screen.getByRole('textbox')).toHaveValue(value)
  }
  view.rerender(
    <ChaosRepeatControls {...input} errors={{ pattern: 'Rejected' }} />,
  )
  expect(screen.getByRole('textbox')).toHaveAttribute('aria-invalid', 'true')
  expect(screen.queryByText('Rejected')).toBeNull()
})
it.each([
  { running: true, disabled: false },
  { running: false, disabled: true },
])('locks both assets with $running / $disabled', (state) => {
  const input = props(state)
  const view = render(<ChaosRepeatControls {...input} />)
  expect(screen.getByRole('button')).toBeDisabled()
  expect(screen.getByRole('textbox')).toBeDisabled()
  fireEvent.click(screen.getByRole('button'))
  fireEvent.change(screen.getByRole('textbox'), { target: { value: 'x' } })
  expect(input.onEnabledChange).not.toHaveBeenCalled()
  expect(input.onPatternChange).not.toHaveBeenCalled()
  expect(
    Boolean(document.querySelector('.chaos-repeat-controls__spinner')),
  ).toBe(state.running)
  view.rerender(
    <ChaosRepeatControls {...input} running={false} disabled={false} />,
  )
  expect(screen.getByRole('button')).toBeEnabled()
  expect(screen.getByRole('textbox')).toBeEnabled()
})
it('keeps a responsive undecorated wrapper and reduced-motion calculating indicator', () => {
  const css = readFileSync(
    'src/features/crafting/chaos-repeat/controls/chaos-repeat-controls.css',
    'utf8',
  )
  expect(css).toContain('flex-wrap: wrap')
  expect(css).toContain('border: 0')
  expect(css).toContain('background: none')
  expect(css).toContain('prefers-reduced-motion')
})
