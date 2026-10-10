import { StrictMode } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { PathTree } from './PathTree'
import { fixture, jobFixture, requestFixture } from './fixtures.test-support'
import type { JobSnapshot } from './types'
import type { PathSearchAdapter } from './api'
import type { ItemPresentation } from './presentation'
import { pathTreeMessages } from './messages'
const present: ItemPresentation = (item) => ({
  name: 'Amulet',
  base: 'Amulet',
  itemClass: 'Amulets',
  rarity: item.rarity,
  itemLevel: item.itemLevel,
  properties: [],
  requirements: [],
  flags: [],
  modifiers: item.explicits.flatMap((m) =>
    Object.values(m.values).map((value, i) => ({
      id: String(i),
      text: `Power ${value}`,
      kind: 'explicit' as const,
    })),
  ),
})
function renderTree(job = jobFixture()) {
  const adapter: PathSearchAdapter = {
    id: 'ui-fixture',
    create: vi.fn(async () => job),
    read: vi.fn(async () => job),
    graph: vi.fn(async () => job.graph),
    mutate: vi.fn(async () => job),
    recover: vi.fn(async () => fixture<JobSnapshot>('conditional-recovery')),
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  })
  const props = {
    request: requestFixture(),
    inputGeneration: 1,
    presentItem: present,
    adapter,
    onRecalculate: vi.fn(),
  }
  const view = render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <PathTree {...props} />
      </QueryClientProvider>
    </StrictMode>,
  )
  return { ...view, adapter, props, client }
}
beforeEach(() => setLocale('en'))
describe('path tree', () => {
  it('shows a finite loop, outcome chances and focusable shared item references without internal IDs', async () => {
    const { container, adapter } = renderTree()
    await screen.findByText('Recommended among the paths compared')
    expect(adapter.create).toHaveBeenCalledTimes(1)
    expect(
      screen.getByRole('heading', { name: 'Crafting paths' }),
    ).toHaveFocus()
    expect(container.textContent).not.toMatch(
      /synthetic:|SYNTHETIC|phase|provenance|policyId/,
    )
    expect(screen.getAllByText(/Chance of this outcome: ≈50%/)).toHaveLength(2)
    const repeat = screen.getByRole('link', { name: 'Repeat · Item state 1' })
    fireEvent.click(repeat)
    expect(screen.getByRole('region', { name: 'Item state 1' })).toHaveFocus()
    expect(container.querySelectorAll('.path-tree__node')).toHaveLength(2)
  })
  it('changes recommendation and rank for each observation including ties', async () => {
    const job = fixture<JobSnapshot>('observation-ranking-and-ties')
    const { container } = renderTree(job)
    await screen.findByRole('group', {
      name: 'Chance of reaching the goal within 100 currency uses',
    })
    for (const n of ['100', '300', '500']) {
      fireEvent.click(screen.getByRole('radio', { name: n }))
      expect(
        screen.getByRole('group', {
          name: `Chance of reaching the goal within ${n} currency uses`,
        }),
      ).toBeInTheDocument()
      const count = job.rankings
        .find((r) => r.attempts === n)!
        .entries.filter((e) => e.rank === 1).length
      expect(
        container.querySelectorAll('.path-tree__recommended'),
      ).toHaveLength(count)
    }
  })
  it('labels partial bounds without a certified recommendation', async () => {
    renderTree(fixture<JobSnapshot>('partial'))
    await screen.findByText(
      'Partial calculation — probabilities and order may change.',
    )
    expect(
      screen.queryByText('Recommended among the paths compared'),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /Path 1/ }).textContent,
    ).toContain(' – ')
  })
  it('shows unsupported without fabricated candidates', async () => {
    renderTree(fixture<JobSnapshot>('unsupported'))
    await screen.findByText('This starting item or goal is not supported yet.')
    expect(
      screen.queryByRole('button', { name: /Path 1/ }),
    ).not.toBeInTheDocument()
  })
  it('selects a failure and an actual earlier item, with separate recovery results', async () => {
    const job = fixture<JobSnapshot>('recovery-parent')
    const { adapter } = renderTree(job)
    const failure = await screen.findByRole('combobox', {
      name: 'Choose an item that has not reached the goal',
    })
    const option = within(failure).getAllByRole(
      'option',
    )[1] as HTMLOptionElement
    fireEvent.change(failure, { target: { value: option.value } })
    const target = screen.getByRole('combobox', {
      name: 'Choose an earlier item to return to',
    })
    const checkpoint = within(target).getAllByRole(
      'option',
    )[1] as HTMLOptionElement
    fireEvent.change(target, { target: { value: checkpoint.value } })
    fireEvent.click(
      screen.getByRole('button', { name: 'Calculate return chance' }),
    )
    await waitFor(() => expect(adapter.recover).toHaveBeenCalled())
    expect(vi.mocked(adapter.recover).mock.calls[0]![1]).toEqual(
      expect.objectContaining({
        parentRevision: job.revision,
        failureExecutionId: option.value,
        checkpointStateId: checkpoint.value,
      }),
    )
    expect(
      screen.getAllByText(
        'From the selected item only. This return chance is separate from the main goal chance.',
      ).length,
    ).toBeGreaterThan(0)
  })
  for (const locale of locales)
    it(`has complete dedicated ${locale} copy and native keyboard controls`, async () => {
      setLocale(locale)
      renderTree()
      await screen.findByText(pathTreeMessages[locale].recommended)
      expect(screen.getAllByRole('radio')).toHaveLength(3)
      expect(Object.keys(pathTreeMessages[locale])).toEqual(
        Object.keys(pathTreeMessages.en),
      )
      expect(Object.values(pathTreeMessages[locale]).every(Boolean)).toBe(true)
    })
})
