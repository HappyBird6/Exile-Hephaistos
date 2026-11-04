import { readFileSync } from 'node:fs'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { gameName, locales, setLocale } from '../../../shared/i18n/i18n'
import { currencies } from '../currencies'
import { currencyNames } from '../currencyNames'
import { materials, additionalStashCurrencies } from '../materials'
import { MaterialTooltip } from '../MaterialTooltip'
import type { WorkbenchResource } from '../stash/stashTypes'
import {
  currentFilm,
  LocalFilmRepository,
  recordCraft,
  viewFrame,
} from '../workbenchHistory'
import { craft, freeze, root, start, twoSteps } from './fixtures.test-support'
import { sessionNotationMessages } from './messages'
import { SessionNotation } from './SessionNotation'

const resources: WorkbenchResource[] = [
  ...currencies.map((currency) => ({
    ...currency,
    name: currencyNames[currency.id]!,
    category: 'Currency' as const,
  })),
  ...materials,
  ...additionalStashCurrencies,
]

describe('icon-based session notation', () => {
  it.each(locales)(
    'localizes the heading, limits, accessible names and current step in %s',
    async (locale) => {
      const { history } = await twoSteps()
      const film = structuredClone(currentFilm(history)!)
      delete film.frames[1]!.evidence
      // A missing old consumption array is deliberately different from an explicit [].
      Object.assign(film.frames[2]!.evidence!, { consumedOmens: undefined })
      setLocale(locale)
      const text = sessionNotationMessages[locale]
      render(<SessionNotation film={film} cursor={2} resources={resources} />)
      expect(
        screen.getByRole('region', { name: text.title }),
      ).toBeInTheDocument()
      expect(document.querySelector('.wb-session-notation__scope')).toBeNull()
      expect(screen.getByText(text.omitted)).toBeInTheDocument()
      expect(screen.getByText(text.omensMissing)).toBeInTheDocument()
      expect(document.querySelector('.wb-session-notation__current')).toBeNull()
      expect(screen.getByLabelText(text.incomplete)).toBeInTheDocument()
      const label = gameName(
        'Orb_of_Augmentation',
        'Orb of Augmentation',
        locale,
      )
      expect(screen.getByRole('img', { name: label })).toHaveAttribute(
        'data-material-tooltip',
        'Orb_of_Augmentation',
      )
      expect(screen.queryByText(label)).not.toBeInTheDocument()
    },
  )

  it('shows the consumed omens and currency in one numbered row with no visible name list', async () => {
    const result = await craft()
    const consumed = [
      'Omen_of_Greater_Exaltation',
      'Omen_of_Sinistral_Exaltation',
    ]
    const history = recordCraft(
      start(),
      root,
      {
        ...result,
        action: 'EXALTED',
        consumedOmens: consumed,
        remainingOmens: ['Omen_of_Whittling'],
      },
      'unused',
    )
    const { container } = render(
      <SessionNotation
        film={currentFilm(history)}
        cursor={history.cursor}
        resources={resources}
      />,
    )
    const row = screen.getByRole('listitem', { name: 'Step 1' })
    expect(row).toHaveAttribute('aria-current', 'step')
    expect(row).toHaveAttribute('data-frame-index', '1')
    expect(within(row).getAllByRole('img')).toHaveLength(3)
    expect(
      Array.from(row.querySelectorAll('[data-material-tooltip]')).map((icon) =>
        icon.getAttribute('data-material-tooltip'),
      ),
    ).toEqual([...consumed, 'Exalted_Orb'])
    expect(row.textContent).toBe('1.+')
    expect(
      container.querySelector('[data-material-tooltip="Omen_of_Whittling"]'),
    ).toBeNull()
    expect(container.querySelector('[title]')).toBeNull()
    expect(container.querySelector('[role="tooltip"]')).toBeNull()
    expect(container.querySelector('button')).toBeNull()
  })

  it('reuses the root tooltip delegation for pointer, focus and Escape', async () => {
    const { history } = await twoSteps()
    render(
      <>
        <SessionNotation
          film={currentFilm(history)}
          cursor={1}
          resources={resources}
        />
        <MaterialTooltip />
      </>,
    )
    const icon = screen.getByRole('img', { name: 'Orb of Transmutation' })
    expect(icon).toHaveAttribute('tabindex', '0')
    fireEvent.pointerOver(icon.querySelector('img')!)
    expect(screen.getAllByRole('tooltip')).toHaveLength(1)
    expect(
      within(screen.getByRole('tooltip')).getByText('Orb of Transmutation'),
    ).toBeInTheDocument()
    expect(icon).toHaveAttribute('aria-describedby', 'material-description')
    expect(
      within(screen.getByRole('tooltip')).getByRole('link', { name: 'PoE2DB' }),
    ).toHaveAttribute('href', expect.stringContaining('poe2db.tw'))
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(screen.queryByRole('tooltip')).not.toBeInTheDocument()
    expect(icon).not.toHaveAttribute('aria-describedby')
    fireEvent.focusIn(icon)
    expect(screen.getByRole('tooltip')).toBeInTheDocument()
  })

  it('retains all rows and moves only the highlight after undo, redo and restoring saved history', async () => {
    const { history, second } = await twoSteps()
    const repository = new LocalFilmRepository(localStorage)
    repository.save(viewFrame(history, 1))
    const restored = repository.load()
    const { rerender } = render(
      <SessionNotation
        film={currentFilm(restored)}
        cursor={restored.cursor}
        resources={resources}
      />,
    )
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
    expect(screen.getByRole('listitem', { name: 'Step 1' })).toHaveAttribute(
      'aria-current',
      'step',
    )
    expect(
      screen.getByRole('listitem', { name: 'Step 2' }),
    ).not.toHaveAttribute('aria-current')
    rerender(
      <SessionNotation
        film={currentFilm(history)}
        cursor={2}
        resources={resources}
      />,
    )
    expect(screen.getAllByRole('listitem')).toHaveLength(2)
    expect(
      screen.getByRole('listitem', { name: 'Step 1' }),
    ).not.toHaveAttribute('aria-current')
    expect(screen.getByRole('listitem', { name: 'Step 2' })).toHaveAttribute(
      'aria-current',
      'step',
    )
    const branch = recordCraft(viewFrame(history, 0), root, second, 'branch')
    rerender(
      <SessionNotation
        film={currentFilm(branch)}
        cursor={branch.cursor}
        resources={resources}
      />,
    )
    expect(screen.getAllByRole('listitem')).toHaveLength(1)
    expect(
      screen.getByRole('img', { name: 'Orb of Augmentation' }),
    ).toBeInTheDocument()
    rerender(
      <SessionNotation film={undefined} cursor={0} resources={resources} />,
    )
    expect(
      screen.getByText(sessionNotationMessages.en.empty),
    ).toBeInTheDocument()
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument()
  })

  it('uses neutral missing-image glyphs and never creates tooltip IDs outside the supplied registry', async () => {
    const { history } = await twoSteps()
    const film = {
      ...currentFilm(history)!,
      frames: currentFilm(history)!.frames.slice(0, 2),
    }
    const { container, rerender } = render(
      <SessionNotation film={film} cursor={1} resources={resources} />,
    )
    const image = container.querySelector('img')!
    fireEvent.error(image)
    const failed = screen.getByRole('img', {
      name: 'Orb of Transmutation · Image unavailable',
    })
    expect(failed).toHaveAttribute(
      'data-material-tooltip',
      'Orb_of_Transmutation',
    )
    expect(failed).toHaveTextContent('◇')
    expect(container.querySelector('img')).toBeNull()
    rerender(<SessionNotation film={film} cursor={1} resources={[]} />)
    const unknown = screen.getByRole('img', {
      name: 'Unidentified currency · Image unavailable',
    })
    expect(unknown).not.toHaveAttribute('data-material-tooltip')
    expect(container.querySelector('[title]')).toBeNull()
    rerender(
      <SessionNotation
        film={film}
        cursor={1}
        resources={resources.map((resource) => ({ ...resource, image: '' }))}
      />,
    )
    expect(
      screen.getByRole('img', {
        name: 'Orb of Transmutation · Image unavailable',
      }),
    ).toHaveTextContent('◇')
  })

  it('reacts to locale changes without mutating Film or writing storage', async () => {
    const { history } = await twoSteps()
    const original = JSON.stringify(history)
    freeze(history)
    const write = vi.spyOn(Storage.prototype, 'setItem')
    const { rerender } = render(
      <SessionNotation
        film={currentFilm(history)}
        cursor={2}
        resources={resources}
      />,
    )
    expect(write).not.toHaveBeenCalled()
    act(() => setLocale('ko'))
    expect(
      screen.getByRole('heading', { name: sessionNotationMessages.ko.title }),
    ).toBeInTheDocument()
    write.mockClear()
    rerender(
      <SessionNotation
        film={currentFilm(history)}
        cursor={1}
        resources={resources}
      />,
    )
    fireEvent.focusIn(screen.getAllByRole('img')[0]!)
    expect(write).not.toHaveBeenCalled()
    expect(JSON.stringify(history)).toBe(original)
    write.mockRestore()
  })
})

it('lays out six steps in two columns in row order and retains only the current highlight', async () => {
  const { history } = await twoSteps()
  const film = currentFilm(history)!
  const repeated = {
    ...film,
    frames: [
      film.frames[0]!,
      ...Array.from({ length: 6 }, () => film.frames[1]!),
    ],
  }
  const style = document.createElement('style')
  style.textContent = readFileSync(
    'src/features/crafting/session-notation/session-notation.css',
    'utf8',
  )
  document.head.append(style)
  try {
    render(<SessionNotation film={repeated} cursor={6} resources={resources} />)
    expect(screen.getByRole('heading', { name: 'History' })).toBeInTheDocument()
    const rows = screen.getAllByRole('listitem')
    expect(rows.map((row) => row.getAttribute('data-frame-index'))).toEqual([
      '1',
      '2',
      '3',
      '4',
      '5',
      '6',
    ])
    const steps = screen.getByRole('list')
    expect(steps).toHaveClass('wb-scrollbar')
    const layout = getComputedStyle(steps)
    expect(layout.overflowY).toBe('auto')
    expect(layout.gridTemplateColumns).toBe('repeat(2, minmax(0, 1fr))')
    expect(layout.gridAutoFlow).toBe('row')
    expect(rows.filter((row) => row.hasAttribute('aria-current'))).toEqual([
      rows[5],
    ])
    expect(document.querySelector('.wb-session-notation__current')).toBeNull()
  } finally {
    style.remove()
  }
})

it('offers native keyboard buttons for every frame without nesting focusable icons', async () => {
  const { history } = await twoSteps()
  const onNavigate = vi.fn()
  const { rerender } = render(
    <SessionNotation
      film={currentFilm(history)}
      cursor={0}
      resources={resources}
      onNavigate={onNavigate}
    />,
  )
  const future = screen.getByRole('button', { name: 'Step 2' })
  future.focus()
  expect(future).toHaveFocus()
  expect(future).toHaveAttribute('type', 'button')
  expect(future.querySelector('[tabindex]')).toBeNull()
  fireEvent.click(future, { detail: 0 })
  expect(onNavigate).toHaveBeenCalledWith(2)
  expect(screen.getAllByRole('listitem')).toHaveLength(2)
  expect(document.querySelector('li[aria-current]')).toBeNull()
  rerender(
    <SessionNotation
      film={currentFilm(history)}
      cursor={2}
      resources={resources}
      onNavigate={onNavigate}
      disabled
    />,
  )
  expect(screen.getByRole('button', { name: 'Step 2' })).toBeDisabled()
  onNavigate.mockClear()
  fireEvent.click(screen.getByRole('button', { name: 'Step 1' }))
  expect(onNavigate).not.toHaveBeenCalled()
})

it('retains separate omen and currency hover tooltips inside a clickable step', async () => {
  const result = await craft()
  const history = recordCraft(
    start(),
    root,
    {
      ...result,
      action: 'EXALTED',
      consumedOmens: ['Omen_of_Greater_Exaltation'],
    },
    'unused',
  )
  const onNavigate = vi.fn()
  render(
    <>
      <SessionNotation
        film={currentFilm(history)}
        cursor={1}
        resources={resources}
        onNavigate={onNavigate}
      />
      <MaterialTooltip onlyWithin=".wb-session-notation" />
    </>,
  )
  fireEvent.mouseOver(
    screen.getByRole('img', { name: 'Omen of Greater Exaltation' }),
  )
  expect(screen.getByRole('tooltip')).toHaveTextContent(
    'Omen of Greater Exaltation',
  )
  expect(onNavigate).not.toHaveBeenCalled()
  fireEvent.mouseOver(screen.getByRole('img', { name: 'Exalted Orb' }))
  expect(screen.getByRole('tooltip')).toHaveTextContent('Exalted Orb')
  fireEvent.click(screen.getByRole('button', { name: 'Step 1' }))
  expect(onNavigate).toHaveBeenCalledWith(1)
})
