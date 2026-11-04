import { readFileSync } from 'node:fs'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { locales, setLocale } from '../../../../shared/i18n/i18n'
import {
  FeedbackTooltipOutlet,
  FeedbackTooltipProvider,
} from '../../feedback-tooltip/FeedbackTooltipProvider'
import { ChaosRepeatControls } from './ChaosRepeatControls'
import { chaosRepeatControlMessages } from './messages'
import { chaosRepeatCurrencyIds, defaultChaosRepeatMaximumUses } from './types'
import type { ChaosRepeatControlsProps } from './types'

const css = readFileSync(
  'src/features/crafting/chaos-repeat/controls/chaos-repeat-controls.css',
  'utf8',
)
let style: HTMLStyleElement
beforeEach(() => {
  style = document.createElement('style')
  style.textContent = css
  document.head.append(style)
})
afterEach(() => {
  style.remove()
  vi.restoreAllMocks()
})

function props(
  overrides: Partial<ChaosRepeatControlsProps> = {},
): ChaosRepeatControlsProps {
  return {
    enabled: false,
    pattern: '',
    maximumUses: defaultChaosRepeatMaximumUses,
    running: false,
    onEnabledChange: vi.fn(),
    onPatternChange: vi.fn(),
    onMaximumUsesChange: vi.fn(),
    ...overrides,
  }
}

it('edits controlled mode only, leaves execution to the caller and supports a keyboard-generated click', () => {
  const input = props()
  const fetch = vi.spyOn(globalThis, 'fetch')
  const view = render(<ChaosRepeatControls {...input} />)
  const toggle = screen.getByRole('button', {
    name: /OFF.*Chaos Orb.*Continue using/,
  })
  expect(toggle).toHaveAttribute('type', 'button')
  expect(toggle).toHaveAttribute('aria-pressed', 'false')
  expect(toggle).toHaveAccessibleDescription(chaosRepeatControlMessages.en.hint)
  act(() => toggle.focus())
  expect(toggle).toHaveFocus()
  fireEvent.click(toggle, { detail: 0 })
  expect(input.onEnabledChange).toHaveBeenCalledExactlyOnceWith(true)
  expect(toggle).toHaveAttribute('aria-pressed', 'false')
  expect(input.onPatternChange).not.toHaveBeenCalled()
  expect(input.onMaximumUsesChange).not.toHaveBeenCalled()
  expect(fetch).not.toHaveBeenCalled()
  view.rerender(<ChaosRepeatControls {...input} enabled />)
  expect(toggle).toHaveAttribute('aria-pressed', 'true')
  expect(toggle).toHaveAccessibleName(/ON.*Chaos Orb/)
  fireEvent.click(toggle)
  expect(input.onEnabledChange).toHaveBeenLastCalledWith(false)
  expect(fetch).not.toHaveBeenCalled()
})

it('forwards regex-like pattern and raw maximum strings without compiling, trimming or enforcing 500', () => {
  const input = props()
  const view = render(<ChaosRepeatControls {...input} />)
  const pattern = screen.getByLabelText('Pattern')
  const maximum = screen.getByLabelText('Maximum uses')
  expect(maximum).toHaveValue('500')
  expect(maximum).not.toHaveAttribute('max')
  expect(pattern).not.toHaveAttribute('pattern')
  for (const value of ['[(', '  +12 Fire  ', '(?i)test', '']) {
    fireEvent.change(pattern, { target: { value } })
    expect(input.onPatternChange).toHaveBeenLastCalledWith(value)
    view.rerender(<ChaosRepeatControls {...input} pattern={value} />)
    expect(pattern).toHaveValue(value)
  }
  for (const value of ['00501', '500000', '-2', '1e', '']) {
    fireEvent.change(maximum, { target: { value } })
    expect(input.onMaximumUsesChange).toHaveBeenLastCalledWith(value)
    view.rerender(<ChaosRepeatControls {...input} maximumUses={value} />)
    expect(maximum).toHaveValue(value)
  }
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  expect(screen.getByRole('status')).toBeEmptyDOMElement()
})

it.each(chaosRepeatCurrencyIds)(
  'represents %s with the existing Chaos asset and ON/OFF color',
  (currencyId) => {
    const input = props({ currencyId })
    const view = render(<ChaosRepeatControls {...input} />)
    const region = screen.getByRole('region')
    expect(region).toHaveAttribute('data-chaos-repeat-currency', currencyId)
    const image = region.querySelector('img')!
    expect(image).toHaveAttribute(
      'src',
      '/assets/currency/CurrencyRerollRare.webp',
    )
    expect(image).toHaveAttribute('alt', '')
    expect(getComputedStyle(image.parentElement!).filter).toBe('grayscale(1)')
    view.rerender(<ChaosRepeatControls {...input} enabled />)
    expect(getComputedStyle(image.parentElement!).filter).toBe('none')
    expect(screen.getByRole('button')).toHaveAccessibleName(
      currencyId === 'Chaos_Orb'
        ? /ON.*Chaos Orb/
        : currencyId === 'Greater_Chaos_Orb'
          ? /ON.*Greater Chaos Orb/
          : /ON.*Perfect Chaos Orb/,
    )
  },
)

it('shows externally supplied errors and cap without fabricating local validation or success', () => {
  const input = props({
    pattern: '[',
    maximumUses: '2000',
    serverMaximumUses: null,
  })
  const view = render(<ChaosRepeatControls {...input} />)
  expect(screen.queryByText(/Server limit:/)).not.toBeInTheDocument()
  expect(screen.getByLabelText('Pattern')).toHaveAttribute(
    'aria-invalid',
    'false',
  )
  expect(screen.getByRole('status')).toBeEmptyDOMElement()
  view.rerender(
    <ChaosRepeatControls
      {...input}
      serverMaximumUses={1200}
      errors={{
        pattern: 'Pattern rejected by server',
        maximumUses: 'Maximum rejected by server',
        request: 'Request failed',
      }}
    />,
  )
  const pattern = screen.getByLabelText('Pattern')
  const maximum = screen.getByLabelText('Maximum uses')
  expect(pattern).toHaveAttribute('aria-invalid', 'true')
  expect(pattern).toHaveAccessibleDescription(
    expect.stringContaining('Pattern rejected by server'),
  )
  expect(maximum).toHaveAccessibleDescription(
    'Server limit: 1200 Maximum rejected by server',
  )
  expect(maximum).toHaveValue('2000')
  expect(maximum).not.toHaveAttribute('max')
  expect(
    screen.getAllByRole('alert').map((element) => element.textContent),
  ).toEqual([
    'Pattern rejected by server',
    'Maximum rejected by server',
    'Request failed',
  ])
  view.rerender(<ChaosRepeatControls {...input} />)
  expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  expect(screen.getByRole('status')).toBeEmptyDOMElement()
})

it.each([
  { running: true, disabled: false },
  { running: false, disabled: true },
])(
  'locks callbacks and inputs only for external running=$running / disabled=$disabled',
  ({ running, disabled }) => {
    const input = props({ running, disabled, enabled: true })
    const view = render(<ChaosRepeatControls {...input} />)
    expect(screen.getByRole('region')).toHaveAttribute(
      'aria-busy',
      String(running),
    )
    for (const field of screen.getAllByRole('textbox')) {
      expect(field).toBeDisabled()
      fireEvent.change(field, { target: { value: 'changed' } })
    }
    const toggle = screen.getByRole('button')
    expect(toggle).toBeDisabled()
    fireEvent.click(toggle)
    expect(input.onEnabledChange).not.toHaveBeenCalled()
    expect(input.onPatternChange).not.toHaveBeenCalled()
    expect(input.onMaximumUsesChange).not.toHaveBeenCalled()
    const status = screen.getByRole('status')
    expect(status).toHaveAttribute('aria-live', 'polite')
    if (running) {
      expect(status).toHaveTextContent('Applying Chaos…')
      expect(
        status.querySelector('.chaos-repeat-controls__spinner'),
      ).toHaveAttribute('aria-hidden', 'true')
    } else {
      expect(status).toBeEmptyDOMElement()
    }
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
    view.rerender(
      <ChaosRepeatControls {...input} running={false} disabled={false} />,
    )
    expect(status).toBeEmptyDOMElement()
    expect(toggle).toBeEnabled()
  },
)

it.each(locales)(
  'labels inputs, busy state and shared explanation in %s',
  (locale) => {
    setLocale(locale)
    const messages = chaosRepeatControlMessages[locale]
    render(<ChaosRepeatControls {...props({ running: true })} />)
    expect(screen.getByRole('region', { name: messages.mode })).toHaveAttribute(
      'data-feedback-tooltip',
      messages.hint,
    )
    expect(screen.getByLabelText(messages.pattern)).toBeInTheDocument()
    expect(screen.getByLabelText(messages.maximumUses)).toHaveValue('500')
    expect(screen.getByRole('button')).toHaveAccessibleDescription(
      messages.hint,
    )
    expect(screen.getByRole('status')).toHaveTextContent(messages.running)
    if (locale === 'ko')
      expect(messages.hint).toBe('옵션에 패턴이 등장할때까지 카오스 사용')
  },
)

it.each(['hover', 'focus', 'touch', 'click'] as const)(
  'uses shared feedback for %s without a second tooltip',
  (interaction) => {
    const input = props({ onHintInteraction: vi.fn() })
    render(
      <FeedbackTooltipProvider enabled>
        <ChaosRepeatControls {...input} />
        <FeedbackTooltipOutlet />
      </FeedbackTooltipProvider>,
    )
    const toggle = screen.getByRole('button')
    if (interaction === 'hover') fireEvent.pointerOver(toggle)
    if (interaction === 'focus') fireEvent.focus(toggle)
    if (interaction === 'touch')
      fireEvent.pointerDown(toggle, { pointerType: 'touch' })
    if (interaction === 'click') fireEvent.click(toggle)
    expect(screen.getAllByRole('tooltip')).toHaveLength(1)
    expect(screen.getByRole('tooltip')).toHaveTextContent(
      chaosRepeatControlMessages.en.hint,
    )
    expect(input.onHintInteraction).toHaveBeenCalledWith('Chaos_Orb')
    expect(toggle).not.toHaveAttribute('title')
  },
)

it.each(['Chaos_Orb', 'Exalted_Orb'])(
  'preserves the existing %s priority while reporting the hint for future composition',
  (priorityMaterialId) => {
    const input = props({ onHintInteraction: vi.fn() })
    render(
      <FeedbackTooltipProvider enabled priorityMaterialId={priorityMaterialId}>
        <ChaosRepeatControls {...input} />
        <FeedbackTooltipOutlet />
      </FeedbackTooltipProvider>,
    )
    const tooltip = screen.getByRole('tooltip')
    const previous = tooltip.textContent
    fireEvent.pointerOver(screen.getByRole('button'))
    fireEvent.pointerDown(screen.getByRole('button'), { pointerType: 'touch' })
    expect(tooltip.textContent).toBe(previous)
    expect(
      within(tooltip).queryByText(chaosRepeatControlMessages.en.hint),
    ).not.toBeInTheDocument()
    expect(input.onHintInteraction).toHaveBeenCalledWith('Chaos_Orb')
  },
)

it('wraps controls within a narrow host and provides static reduced-motion busy feedback', () => {
  render(
    <div style={{ width: 120 }}>
      <ChaosRepeatControls
        {...props({
          running: true,
          serverMaximumUses: 10000,
          errors: { request: 'VeryLongUnbrokenExternalErrorMessage' },
        })}
      />
    </div>,
  )
  const root = screen.getByRole('region')
  expect(getComputedStyle(root).maxWidth).toBe('100%')
  expect(getComputedStyle(root).overflowWrap).toBe('anywhere')
  expect(Number.parseFloat(getComputedStyle(root).minWidth)).toBe(0)
  for (const selector of [
    '.chaos-repeat-controls__main',
    '.chaos-repeat-controls__limit-row',
    '.chaos-repeat-controls__toggle',
  ]) {
    expect(getComputedStyle(root.querySelector(selector)!).flexWrap).toBe(
      'wrap',
    )
  }
  for (const input of screen.getAllByRole('textbox')) {
    expect(getComputedStyle(input).maxWidth).toBe('100%')
    expect(Number.parseFloat(getComputedStyle(input).minWidth)).toBe(0)
  }
  const spinner = root.querySelector('.chaos-repeat-controls__spinner')!
  expect(getComputedStyle(spinner).animation).toContain('infinite')
  const reducedMotion = [...style.sheet!.cssRules].find(
    (rule) => rule.type === CSSRule.MEDIA_RULE,
  ) as CSSMediaRule
  expect(reducedMotion.conditionText).toBe('(prefers-reduced-motion: reduce)')
  expect((reducedMotion.cssRules[0] as CSSStyleRule).style.animation).toBe(
    'none',
  )
  expect(screen.getByRole('status')).toHaveTextContent('Applying Chaos…')
})
