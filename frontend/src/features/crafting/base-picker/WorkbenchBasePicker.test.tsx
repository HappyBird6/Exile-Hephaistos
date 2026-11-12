import { initialFixture } from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { baseFavoriteMessages } from '../base-favorites/messages'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { useRef, useState } from 'react'
import { expect, it, vi } from 'vitest'
import { WorkbenchBasePicker } from './WorkbenchBasePicker'
import { basePreview, workbenchBaseChoices } from './baseChoices'
import { overlayPlacement } from './overlayPlacement'
import { baseImageSource } from './baseImageSources'
import { locales, setLocale, translate } from '../../../shared/i18n/i18n'
import { basePickerMessages } from './basePickerMessages'
import type { WorkbenchBaseKey } from '../baseRegistry'

it.each(locales)(
  'keeps both filter names accessible without visible labels in %s',
  (locale) => {
    setLocale(locale)
    const messages = basePickerMessages[locale]
    function Example() {
      const anchor = useRef<HTMLButtonElement>(null)
      return (
        <WorkbenchBasePicker
          id="labels"
          anchor={anchor}
          open
          onOpenChange={vi.fn()}
          value="solar"
          onChange={vi.fn()}
          level="80"
          onLevelChange={vi.fn()}
          onPlace={vi.fn()}
        />
      )
    }
    render(<Example />)
    for (const name of [messages.category, messages.subtype]) {
      expect(screen.getByLabelText(name)).toHaveAccessibleName(name)
      expect(screen.queryByText(name)).not.toBeInTheDocument()
    }
    expect(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: translate('ui.select_base', {}, locale),
      }),
    ).toBeEnabled()
  },
)

it('covers the canvas through the base button with equal insets and clamps to the viewport', () => {
  expect(
    overlayPlacement(
      { left: 100, right: 900, top: 100, bottom: 700 },
      { left: 600, right: 740, top: 650, bottom: 690 },
      { width: 1400, height: 900 },
    ),
  ).toEqual({ left: 108, top: 108, width: 784, height: 574 })
  const narrow = overlayPlacement(
    { left: -20, right: 500, top: -10, bottom: 800 },
    { left: 100, right: 200, top: 760, bottom: 790 },
    { width: 320, height: 740 },
  )
  expect(narrow).toEqual({ left: 8, top: 8, width: 304, height: 724 })
})

it('filters equipment subcategories, previews on hover/focus and validates compact item level before placing', () => {
  const place = vi.fn()
  function Example() {
    const anchor = useRef<HTMLButtonElement>(null)
    const [open, setOpen] = useState(false)
    const [value, setValue] = useState<WorkbenchBaseKey>('solar')
    const [level, setLevel] = useState('80')
    return (
      <>
        <button ref={anchor} onClick={() => setOpen(true)}>
          Select base
        </button>
        <WorkbenchBasePicker
          id="bases"
          open={open}
          onOpenChange={setOpen}
          anchor={anchor}
          value={value}
          onChange={setValue}
          level={level}
          onLevelChange={setLevel}
          onPlace={place}
        />
      </>
    )
  }
  render(<Example />)
  const trigger = screen.getByRole('button', { name: 'Select base' })
  fireEvent.click(trigger)
  const dialog = screen.getByRole('dialog', { name: 'Select base' })
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Solar Amulet')
  const select = within(dialog).getByRole('button', { name: 'Select base' })
  fireEvent.change(screen.getByLabelText('Equipment category'), {
    target: { value: 'armour' },
  })
  fireEvent.change(screen.getByLabelText('Equipment type'), {
    target: { value: 'Gloves' },
  })
  expect(
    screen.queryByRole('button', { name: /Solar Amulet/ }),
  ).not.toBeInTheDocument()
  const mitts = screen.getByRole('button', { name: /^Massive Mitts/ })
  fireEvent.mouseEnter(mitts)
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Armour: 187')
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Requires: Level 80, 101 Str')
  const options = dialog.querySelector('.wb-base-picker__options')!
  fireEvent.mouseLeave(mitts, { relatedTarget: options })
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Solar Amulet')
  fireEvent.mouseEnter(mitts)
  fireEvent.mouseLeave(options)
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Solar Amulet')
  fireEvent.mouseEnter(mitts)
  fireEvent.mouseLeave(mitts, {
    relatedTarget: screen.getByRole('region', { name: 'Base information' }),
  })
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Solar Amulet')
  fireEvent.mouseEnter(mitts)
  fireEvent.mouseLeave(mitts)
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Solar Amulet')
  fireEvent.focus(mitts)
  expect(
    screen.getByRole('region', { name: 'Base information' }),
  ).toHaveTextContent('Massive Mitts')
  fireEvent.blur(mitts)
  fireEvent.click(mitts)
  expect(mitts).toHaveAttribute('aria-pressed', 'true')
  fireEvent.change(screen.getByLabelText('Item level'), {
    target: { value: '101' },
  })
  expect(select).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Item level'), {
    target: { value: '70' },
  })
  fireEvent.click(select)
  expect(place).toHaveBeenCalledOnce()
  fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' })
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  expect(trigger).toHaveFocus()
})

it('uses reviewed range text for implicits instead of pretending a base preview has rolled values', () => {
  const solar = workbenchBaseChoices('en').find(
    (choice) => choice.key === 'solar',
  )!
  const preview = basePreview(solar, 80, 'en')
  expect(preview.modifiers[0]?.text).toBe('+(10—15) to Spirit')
  expect(basePreview(solar, 80, 'ko').modifiers[0]?.text).toContain('정신력')
  expect(baseImageSource('Metadata/Items/Amulets/FourAmulet9')).toBe(
    '/assets/currency/solar-amulet.webp',
  )
})

it('separates plain base favorites from configured preview favorites', () => {
  setLocale('en')
  const save = vi.fn()
  function Example() {
    const anchor = useRef<HTMLButtonElement>(null)
    return (
      <WorkbenchBasePicker
        id="favorite-modes"
        anchor={anchor}
        open
        onOpenChange={vi.fn()}
        value="solar"
        onChange={vi.fn()}
        level="80"
        onLevelChange={vi.fn()}
        onPlace={vi.fn()}
        onSaveFavorite={save}
        setup={{
          initial: initialFixture,
          item: concreteInitial(initialFixture),
          affixes: [],
          onChange: vi.fn(),
        }}
      />
    )
  }
  render(<Example />)
  fireEvent.click(
    screen.getByRole('button', {
      name: `${baseFavoriteMessages.en.add}: Solar Amulet`,
    }),
  )
  expect(save).toHaveBeenLastCalledWith('solar')
  fireEvent.click(
    screen.getByRole('button', { name: baseFavoriteMessages.en.add }),
  )
  expect(save).toHaveBeenLastCalledWith('solar', true)
})

it('uses the visible phone viewport and keeps confirmation outside scrolling content', () => {
  setLocale('en')
  const width = window.innerWidth
  Object.defineProperty(window, 'innerWidth', {
    configurable: true,
    value: 390,
  })
  const visual = new EventTarget()
  Object.assign(visual, {
    width: 390,
    height: 500,
    offsetTop: 20,
    offsetLeft: 0,
  })
  vi.stubGlobal('visualViewport', visual)
  function Example() {
    const anchor = useRef<HTMLButtonElement>(null)
    return (
      <>
        <button ref={anchor}>Open</button>
        <WorkbenchBasePicker
          id="phone-base"
          anchor={anchor}
          open
          onOpenChange={vi.fn()}
          value="solar"
          onChange={vi.fn()}
          level="82"
          onLevelChange={vi.fn()}
          onPlace={vi.fn()}
        />
      </>
    )
  }
  try {
    const view = render(<Example />)
    const dialog = screen.getByRole('dialog')
    expect(dialog.style.height).toBe('484px')
    expect(dialog.style.top).toBe('28px')
    expect(
      within(dialog)
        .getByRole('button', { name: 'Select base' })
        .closest('footer'),
    ).not.toBeNull()
    expect(document.body.style.overflow).toBe('hidden')
    Object.assign(visual, { height: 300 })
    visual.dispatchEvent(new Event('resize'))
    expect(dialog.style.height).toBe('284px')
    view.unmount()
    expect(document.body.style.overflow).not.toBe('hidden')
  } finally {
    Object.defineProperty(window, 'innerWidth', {
      configurable: true,
      value: width,
    })
    vi.unstubAllGlobals()
  }
})
