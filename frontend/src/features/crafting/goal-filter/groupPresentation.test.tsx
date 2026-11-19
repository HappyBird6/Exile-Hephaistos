import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
import { GoalFilterPanel } from './GoalFilterPanel'
import { goalFilterMessages } from './i18n'
import type { GoalFilterLanguage } from './i18n'
import { groupTypes } from './types'
import { locales } from '../../../shared/i18n/i18n'

const expectedLabels = [
  'And',
  'Not',
  'If',
  'Count',
  'Weighted Sum',
  'Weighted Sum v2',
]

async function renderEditor(
  language: GoalFilterLanguage,
  restoredCount = false,
) {
  const catalog = await mockGoalFilterAdapter.catalog(
    fixtureContext,
    new AbortController().signal,
  )
  const goal = emptyGoal(fixtureContext, catalog.catalogVersion)
  if (restoredCount) {
    goal.groups[0]!.type = 'COUNT'
    goal.groups[0]!.range = { min: 1, max: null }
  }
  const editor = createGoalFilterEditor(goal)
  render(
    <QueryClientProvider client={new QueryClient()}>
      <GoalFilterPanel
        adapter={mockGoalFilterAdapter}
        context={fixtureContext}
        editor={editor}
        bases={[]}
        language={language}
        compact
      />
    </QueryClientProvider>,
  )
  return editor
}

describe('trade stat filter groups', () => {
  it.each([
    ['COUNT', 'Row count'],
    ['WEIGHTED_V1', 'User score'],
    ['WEIGHTED_V2', 'User score'],
  ])(
    'keeps the %s group bounds editable when its rows are collapsed',
    async (type, rangeLabel) => {
      const editor = await renderEditor('en')
      fireEvent.change(
        screen.getByRole('combobox', { name: 'Add Stat Group' }),
        {
          target: { value: type },
        },
      )
      expect(screen.getAllByText('Stat Filters', { exact: true })).toHaveLength(
        1,
      )
      const group = within(screen.getByRole('group', { name: 'Group 2' }))
      fireEvent.click(group.getByRole('button', { name: 'Collapse' }))
      expect(
        group.queryByRole('combobox', { name: 'Search stats' }),
      ).not.toBeInTheDocument()
      const bounds = within(group.getByRole('group', { name: rangeLabel }))
      expect(bounds.getByLabelText('Max')).toBeVisible()
      fireEvent.change(bounds.getByLabelText('Min'), { target: { value: '2' } })
      expect(editor.getState().goal.groups[1]!.range).toEqual({
        min: 2,
        max: null,
      })
      fireEvent.click(group.getByRole('button', { name: 'Expand' }))
      expect(bounds.getByLabelText('Min')).toHaveValue('2')
    },
  )
  it.each(locales)(
    'shows English operations with localized range controls in %s',
    async (locale) => {
      const editor = await renderEditor(locale, true)
      const t = goalFilterMessages[locale]
      const first = screen.getByRole('combobox', { name: t.group + ' 1' })
      expect(first).toHaveValue('COUNT')
      const bounds = within(screen.getByRole('group', { name: t.count }))
      expect(bounds.getByLabelText(t.min)).toHaveValue('1')
      expect(bounds.getByLabelText(t.max)).toHaveValue('')
      const add = screen.getByRole('combobox', { name: t.addGroup })
      const types = [
        'AND',
        'NOT',
        'IF',
        'COUNT',
        'WEIGHTED_V1',
        'WEIGHTED_V2',
      ] as const
      expect(groupTypes).toEqual(types)
      expect(
        within(add)
          .getAllByRole('option')
          .slice(1)
          .map((o) => o.textContent),
      ).toEqual(expectedLabels)
      expect(
        within(first)
          .getAllByRole('option')
          .map((o) => o.textContent),
      ).toEqual(expectedLabels)
      for (const type of types) {
        fireEvent.change(add, { target: { value: type } })
        expect(editor.getState().goal.groups.at(-1)!.type).toBe(type)
      }
      fireEvent.change(bounds.getByLabelText(t.min), { target: { value: '2' } })
      expect(editor.getState().goal.groups[0]!.range).toEqual({
        min: 2,
        max: null,
      })
    },
  )

  it('edits independent repeated rows in the chosen group, with weight before min and max', async () => {
    const editor = await renderEditor('en')
    expect(screen.getByRole('combobox', { name: 'Group 1' })).toHaveValue('AND')
    fireEvent.change(screen.getByRole('combobox', { name: 'Add Stat Group' }), {
      target: { value: 'WEIGHTED_V2' },
    })
    const second = within(screen.getByRole('group', { name: 'Group 2' }))
    expect(
      second.getByRole('combobox', { name: 'Group 2' }),
    ).toHaveDisplayValue('Weighted Sum v2')
    const search = second.getByRole('combobox', { name: 'Search stats' })
    for (let i = 0; i < 2; i++) {
      fireEvent.click(search)
      fireEvent.click(
        await screen.findByRole('option', { name: /total cold resistance/i }),
      )
    }
    expect(editor.getState().goal.groups[0]!.entries).toHaveLength(0)
    const rows = second.getAllByRole('group', {
      name: /total cold resistance/i,
    })
    const fields = within(rows[0]!).getAllByRole('textbox')
    expect(fields.map((field) => field.getAttribute('placeholder'))).toEqual([
      'Weight',
      'Min',
      'Max',
    ])
    fields.forEach((field) => expect(field).toHaveValue(''))
    fireEvent.change(fields[0]!, { target: { value: '-0.5' } })
    fireEvent.change(within(rows[1]!).getByLabelText('Min'), {
      target: { value: '15' },
    })
    const entries = editor.getState().goal.groups[1]!.entries
    expect(entries[0]!.id).not.toBe(entries[1]!.id)
    expect(
      entries.map(({ weight, range }) => [weight, range.min, range.max]),
    ).toEqual([
      [-0.5, null, null],
      [null, 15, null],
    ])
    fireEvent.change(fields[0]!, { target: { value: '' } })
    expect(editor.getState().goal.groups[1]!.entries[0]!.weight).toBeNull()
    fireEvent.change(second.getByRole('combobox', { name: 'Group 2' }), {
      target: { value: 'COUNT' },
    })
    expect(second.queryByLabelText('Weight')).not.toBeInTheDocument()
    expect(editor.getState().goal.groups[1]!.entries[1]!.range.min).toBe(15)
    const first = within(screen.getByRole('group', { name: 'Group 1' }))
    fireEvent.click(first.getByRole('combobox', { name: 'Search stats' }))
    fireEvent.click(
      await screen.findByRole('option', { name: /total cold resistance/i }),
    )
    expect(
      editor.getState().goal.groups.map((group) => group.entries.length),
    ).toEqual([1, 2])
  })
})
