import {
  act,
  fireEvent,
  render,
  screen,
  within,
  waitFor,
} from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { useState } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  FeedbackTooltipProvider,
  FeedbackTooltipOutlet,
} from './FeedbackTooltipProvider'
import { MaterialTooltip } from '../MaterialTooltip'
import { CurrencyImage } from '../CurrencyImage'
import { ItemCard } from '../ItemCard'
import { BasePreviewTooltip } from '../base-picker/BasePreviewTooltip'
import { CraftingPage } from '../CraftingPage'
import { basePreview, workbenchBaseChoices } from '../base-picker/baseChoices'
import { locales, setLocale, translate } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import { outerMaterialSearch } from '../../../shared/test/workbenchQueries'

afterEach(() => vi.useRealTimers())

function Fixture({ enabled = true }: { enabled?: boolean }) {
  const [preview, setPreview] = useState<HTMLElement | null>(null)
  const choice = workbenchBaseChoices('en')[0]!
  return (
    <FeedbackTooltipProvider enabled={enabled}>
      <button
        data-material-tooltip="Chaos_Orb"
        aria-describedby="existing-note"
      >
        Chaos
      </button>
      <span id="existing-note">Existing note</span>
      <button
        onFocus={(event) => setPreview(event.currentTarget)}
        onMouseEnter={(event) => setPreview(event.currentTarget)}
        onMouseLeave={() => setPreview(null)}
        onBlur={() => setPreview(null)}
      >
        Preview
      </button>
      {preview && (
        <BasePreviewTooltip
          onDismiss={() => setPreview(null)}
          id="preview"
          label="Base preview"
          anchor={preview}
          item={basePreview(choice, 82, 'en')}
          slug={choice.slug}
        />
      )}
      <button
        data-material-tooltip="Chaos_Orb"
        aria-label="Missing image currency"
      >
        <CurrencyImage id="Chaos_Orb" image="/missing.png" name="Chaos Orb" />
      </button>
      <ItemCard
        item={{
          rarity: 'NORMAL',
          name: 'Test',
          itemClass: 'Test',
          base: 'Test',
          itemLevel: 82,
          properties: [],
          requirements: [],
          modifiers: [
            {
              id: 'candidate',
              text: 'Candidate modifier',
              removalCandidate: true,
            },
          ],
          flags: [],
        }}
      />
      <div className="workbench-feedback-region">
        <p role="alert">Failed application</p>
        <FeedbackTooltipOutlet />
      </div>
      <MaterialTooltip />
    </FeedbackTooltipProvider>
  )
}

it('keeps failures, descriptions, source links and existing aria descriptions together until dismissed', () => {
  vi.useFakeTimers()
  render(<Fixture />)
  const anchor = screen.getByRole('button', { name: 'Chaos' })
  fireEvent.pointerOver(anchor)
  const tooltip = screen.getByRole('tooltip')
  expect(document.querySelector('.workbench-feedback-region')).toContainElement(
    tooltip,
  )
  expect(screen.getByRole('alert')).toHaveTextContent('Failed application')
  expect(anchor).toHaveAttribute(
    'aria-describedby',
    'existing-note material-description',
  )
  const link = within(tooltip).getByRole('link', { name: 'PoE2DB' })
  fireEvent.pointerOut(anchor, { relatedTarget: link })
  act(() => link.focus())
  act(() => vi.advanceTimersByTime(250))
  expect(link).toHaveFocus()
  expect(tooltip).toBeInTheDocument()
  fireEvent.scroll(tooltip)
  expect(tooltip).toBeInTheDocument()
  fireEvent.keyDown(link, { key: 'Escape' })
  expect(screen.queryByRole('tooltip')).toBeNull()
  expect(anchor).toHaveFocus()
  expect(anchor).toHaveAttribute('aria-describedby', 'existing-note')
  expect(screen.getByRole('alert')).toBeVisible()
  fireEvent.pointerOver(anchor)
  act(() => anchor.blur())
  fireEvent.pointerOut(anchor, { relatedTarget: document.body })
  act(() => vi.advanceTimersByTime(250))
  expect(screen.getByRole('tooltip')).toHaveTextContent('Chaos Orb')
  fireEvent.pointerOver(screen.getByText('Candidate modifier'))
  expect(screen.getByRole('tooltip')).toHaveTextContent(
    translate('omen.candidate_title'),
  )
})

it('shows touch descriptions and routes missing-image and removal-candidate titles without native tooltips', () => {
  render(<Fixture />)
  fireEvent.pointerDown(screen.getByRole('button', { name: 'Chaos' }), {
    pointerType: 'touch',
  })
  expect(screen.getByRole('tooltip')).toHaveTextContent('Chaos Orb')
  fireEvent.error(document.querySelector('img[src="/missing.png"]')!)
  const missing = screen.getByText('Image unavailable')
  expect(missing).not.toHaveAttribute('title')
  fireEvent.pointerOver(missing)
  expect(screen.getByRole('tooltip')).toHaveTextContent(
    'Chaos Orb · Image unavailable',
  )
  fireEvent.focusIn(
    screen.getByRole('button', { name: 'Missing image currency' }),
  )
  expect(screen.getByRole('tooltip')).toHaveTextContent('Image unavailable')
  expect(
    within(screen.getByRole('tooltip')).getByRole('link', { name: 'PoE2DB' }),
  ).toBeVisible()
  const candidate = screen.getByText('Candidate modifier')
  expect(candidate).not.toHaveAttribute('title')
  expect(candidate).toHaveAttribute('tabindex', '0')
  fireEvent.focusIn(candidate)
  expect(screen.getByRole('tooltip')).toHaveTextContent(
    translate('omen.candidate_title'),
  )
})

it('keeps the base ItemCard independent and closes it on hover leave', () => {
  render(<Fixture />)
  const anchor = screen.getByRole('button', { name: 'Preview' })
  fireEvent.mouseEnter(anchor)
  const tooltip = screen.getByRole('tooltip')
  expect(tooltip).toHaveAttribute('id', 'preview')
  expect(tooltip).toHaveClass('wb-base-picker__preview')
  expect(within(tooltip).getByRole('article')).toBeVisible()
  expect(
    document.querySelector('.workbench-feedback-region'),
  ).not.toContainElement(tooltip)
  expect(anchor).toHaveAttribute('aria-describedby', 'preview')
  fireEvent.mouseLeave(anchor, { relatedTarget: document.body })
  expect(screen.queryByRole('tooltip')).toBeNull()
  expect(anchor).not.toHaveAttribute('aria-describedby')
})

it.each(locales)('retains sourced material explanations in %s', (locale) => {
  setLocale(locale)
  render(<Fixture />)
  fireEvent.focusIn(screen.getByRole('button', { name: 'Chaos' }))
  expect(screen.getByRole('tooltip')).not.toHaveTextContent('Stack Size:')
  expect(
    within(screen.getByRole('tooltip')).getByRole('link', { name: 'PoE2DB' }),
  ).toHaveAttribute('href', expect.stringContaining('poe2db.tw'))
})

it('keeps standalone tooltip compatibility without a manual workbench destination', () => {
  render(<Fixture enabled={false} />)
  fireEvent.mouseOver(screen.getByRole('button', { name: 'Chaos' }))
  expect(screen.getByRole('tooltip')).toHaveClass('material-tooltip')
  expect(
    document.querySelector('.workbench-feedback-region'),
  ).not.toContainElement(screen.getByRole('tooltip'))
  expect(screen.getByText('Candidate modifier')).toHaveAttribute(
    'title',
    translate('omen.candidate_title'),
  )
})

it('preserves workspace inputs and keyboard focus without routing descriptions into a hidden manual panel', async () => {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  client.setQueryData(
    ['crafting', 'initial', 'solar', 82, 'manual'],
    initialFixture,
  )
  const mounted = render(
    <QueryClientProvider client={client}>
      <CraftingPage />
    </QueryClientProvider>,
  )
  fireEvent.change(outerMaterialSearch(), { target: { value: 'Essence' } })
  const manualSearch = outerMaterialSearch()
  fireEvent.pointerOver(
    document.querySelector('[data-material-tooltip="Chaos_Orb"]')!,
  )
  expect(screen.getByRole('tooltip')).toHaveClass('workbench-feedback-tooltip')
  const explorer = screen.getByRole('tab', { name: /State explorer/ })
  fireEvent.click(explorer)
  // Outside the manual workspace the context is disabled; standalone tooltip
  // producers keep their body portal (covered above), never the hidden outlet.
  expect(document.querySelector('.workbench-feedback-tooltip')).toBeNull()
  expect(document.querySelector('.craft-page')).not.toHaveClass(
    'craft-page--workbench',
  )
  fireEvent.keyDown(explorer, { key: 'Home' })
  expect(screen.getByRole('tab', { name: /Crafting Workbench/ })).toHaveFocus()
  expect(outerMaterialSearch()).toBe(manualSearch)
  expect(outerMaterialSearch()).toHaveValue('Essence')
  expect(screen.queryByRole('tooltip')).toBeNull()
  // The real base menu uses an independent, scrollable preview.
  const opener = screen.getByRole('button', { name: 'Select base' })
  fireEvent.click(opener)
  const menu = screen.getByRole('dialog', { name: 'Select base' })
  const base = within(menu).getByRole('button', { name: /Massive Mitts/ })
  fireEvent.pointerDown(base, { pointerType: 'touch' })
  const tooltip = screen.getByRole('tooltip')
  expect(
    document.querySelector('.workbench-feedback-region'),
  ).not.toContainElement(tooltip)
  expect(tooltip).toHaveClass('wb-base-picker__preview')
  expect(base).toHaveAttribute('aria-describedby', tooltip.id)
  expect(menu.querySelector('[title]')).toBeNull()
  expect(
    document.querySelector('.material-tooltip, .workbench-feedback-tooltip'),
  ).toBeNull()
  act(() => tooltip.focus())
  fireEvent.keyDown(tooltip, { key: 'Escape' })
  expect(base).toHaveFocus()
  await waitFor(() => expect(screen.queryByRole('tooltip')).toBeNull())
  expect(base).not.toHaveAttribute('aria-describedby')
  expect(menu).toBeInTheDocument()
  fireEvent.mouseEnter(base)
  expect(screen.getByRole('tooltip')).toBeVisible()
  fireEvent.keyDown(base, { key: 'Escape' })
  expect(screen.queryByRole('tooltip')).toBeNull()
  fireEvent.keyDown(base, { key: 'Escape' })
  expect(screen.queryByRole('dialog')).toBeNull()
  expect(opener).toHaveFocus()
  mounted.unmount()
  client.clear()
})
