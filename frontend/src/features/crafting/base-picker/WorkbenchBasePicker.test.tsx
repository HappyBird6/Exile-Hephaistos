import { fireEvent, render, screen } from '@testing-library/react'
import { useRef, useState } from 'react'
import { expect, it, vi } from 'vitest'
import { WorkbenchBasePicker } from './WorkbenchBasePicker'
import { basePreview, workbenchBaseChoices } from './baseChoices'
import { overlayPlacement } from './overlayPlacement'
import { baseImageSource } from './baseImageSources'
import { locales, setLocale } from '../../../shared/i18n/i18n'
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
  },
)

it('opens left over the shelf, clamps narrow screens and keeps the rest of the shelf unobscured', () => {
  const placement = overlayPlacement(
    { left: 900, right: 950, top: 150, bottom: 180 },
    { width: 1400, height: 900 },
    520,
    { left: 100, right: 900, top: 100, bottom: 700 },
  )
  expect(placement).toEqual({ left: 374, top: 140, width: 520, maxHeight: 560 })
  const narrow = overlayPlacement(
    { left: 20, right: 80, top: 700, bottom: 730 },
    { width: 320, height: 740 },
  )
  expect(narrow.left).toBe(8)
  expect(narrow.left + narrow.width).toBeLessThanOrEqual(312)
  expect(narrow.top + narrow.maxHeight).toBeLessThanOrEqual(732)
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
          Base
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
  const trigger = screen.getByRole('button', { name: 'Base' })
  fireEvent.click(trigger)
  expect(
    screen.getByRole('dialog', { name: 'Select base' }),
  ).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Equipment category'), {
    target: { value: 'armour' },
  })
  fireEvent.change(screen.getByLabelText('Equipment type'), {
    target: { value: 'Gloves' },
  })
  expect(
    screen.queryByRole('button', { name: /Solar Amulet/ }),
  ).not.toBeInTheDocument()
  const mitts = screen.getByRole('button', { name: /Massive Mitts/ })
  fireEvent.mouseEnter(mitts)
  expect(screen.getByRole('tooltip')).toHaveTextContent('Armour: 187')
  expect(screen.getByRole('tooltip')).toHaveTextContent(
    'Requires: Level 80, 101 Str',
  )
  fireEvent.click(mitts)
  expect(mitts).toHaveAttribute('aria-pressed', 'true')
  fireEvent.change(screen.getByLabelText('Item level'), {
    target: { value: '101' },
  })
  expect(screen.getByRole('button', { name: 'Place base' })).toBeDisabled()
  fireEvent.change(screen.getByLabelText('Item level'), {
    target: { value: '70' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Place base' }))
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
