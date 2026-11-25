import { expect, it } from 'vitest'
import { useState } from 'react'
import { flatEditorGoal, testRouteGoal } from './testRouteGoal'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { addStat, createGoalFilterEditor, emptyGoal } from './editor'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
import { GoalFilterPanel } from './GoalFilterPanel'
import { qualityWorkGoal } from './qualityWorkGoal'

it('connects the jewellery checkbox to cap work and retains preference across equipment changes', async () => {
  const editor = createGoalFilterEditor(emptyGoal(fixtureContext, 'fixture'))
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const panel = (allowed: boolean) => (
    <QueryClientProvider client={client}>
      <GoalFilterPanel
        editor={editor}
        adapter={mockGoalFilterAdapter}
        context={fixtureContext}
        bases={[]}
        language="en"
        compact
        allowQualityWork={allowed}
      />
    </QueryClientProvider>
  )
  const mounted = render(panel(true))
  const checkbox = await screen.findByRole('checkbox', {
    name: /Use Essence of the Breach/,
  })
  expect(screen.queryByLabelText('Minimum quality')).not.toBeInTheDocument()
  fireEvent.click(checkbox)
  expect(editor.getState().goal.general.quality).toBe(40)
  expect(qualityWorkGoal(editor.getState().goal, true).general.quality).toBe(40)
  mounted.rerender(panel(false))
  expect(checkbox).toBeDisabled()
  expect(checkbox).not.toBeChecked()
  expect(
    qualityWorkGoal(editor.getState().goal, false).general.quality,
  ).toBeNull()
  expect(editor.getState().goal.general.quality).toBe(40)
  mounted.rerender(panel(true))
  expect(checkbox).toBeChecked()
  fireEvent.click(checkbox)
  expect(editor.getState().goal.general.quality).toBeNull()
})

it.each([null, 0, 20, 21, 30, 40])(
  'maps legacy numeric quality %s without mutating stored input',
  (value) => {
    const goal = emptyGoal(fixtureContext, 'fixture')
    goal.general.quality = value
    const restored = JSON.parse(JSON.stringify(goal))
    expect(qualityWorkGoal(restored, true).general.quality).toBe(
      (value ?? 0) > 20 ? 40 : null,
    )
    expect(qualityWorkGoal(restored, false).general.quality).toBeNull()
    expect(restored.general.quality).toBe(value)
  },
)

it('uses a flat target list and only exposes ordinary catalyst choices when Quality 40 is on', async () => {
  const catalog = await mockGoalFilterAdapter.catalog(
    fixtureContext,
    new AbortController().signal,
  )
  const editor = createGoalFilterEditor(
    emptyGoal(fixtureContext, catalog.catalogVersion),
  )
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  function Panel({ allowed }: { allowed: boolean }) {
    const [choice, change] = useState<string | null>(null)
    return (
      <QueryClientProvider client={client}>
        <GoalFilterPanel
          editor={editor}
          adapter={mockGoalFilterAdapter}
          context={fixtureContext}
          bases={[]}
          language="en"
          compact
          allowQualityWork={allowed}
          testQuality={{ choice, change }}
          headerActions={<button>Reset goal</button>}
        />
      </QueryClientProvider>
    )
  }
  const ui = render(<Panel allowed />)
  const toggle = screen.getByRole('button', { name: 'Quality 40' })
  const picker = screen.getByRole('combobox', { name: 'Search stats' })
  expect(picker).toHaveAttribute('placeholder', '+ Add modifier')
  expect(picker.parentElement?.querySelector('span')).toBeNull()
  expect(screen.queryByRole('alert')).toBeNull()
  expect(screen.queryByRole('button', { name: /add group/i })).toBeNull()
  expect(screen.queryByRole('combobox', { name: /group/i })).toBeNull()
  expect(screen.getByRole('button', { name: 'Reset goal' })).toBeEnabled()
  expect(toggle).toHaveAttribute('aria-pressed', 'false')
  expect(screen.queryByRole('combobox', { name: 'Catalyst' })).toBeNull()
  fireEvent.click(toggle)
  const select = screen.getByRole('combobox', { name: 'Catalyst' })
  expect(select).toHaveDisplayValue('Any catalyst')
  expect(select.querySelectorAll('option')).toHaveLength(14)
  expect(
    select.querySelector('option[value="Refined_Flesh_Catalyst"]'),
  ).toBeNull()
  fireEvent.change(select, { target: { value: 'Flesh_Catalyst' } })
  expect(select).toHaveValue('Flesh_Catalyst')
  fireEvent.focus(picker)
  expect(await screen.findByRole('listbox')).toBeVisible()
  expect(screen.queryByRole('combobox', { name: /group/i })).toBeNull()
  const statOptions = await within(screen.getByRole('listbox')).findAllByRole(
    'option',
  )
  fireEvent.click(statOptions[0]!)
  expect(editor.getState().goal.groups).toHaveLength(1)
  expect(editor.getState().goal.groups[0]?.type).toBe('AND')
  expect(editor.getState().goal.groups[0]?.entries).toHaveLength(1)
  fireEvent.click(toggle)
  fireEvent.click(toggle)
  expect(screen.getByRole('combobox', { name: 'Catalyst' })).toHaveDisplayValue(
    'Any catalyst',
  )
  ui.rerender(<Panel allowed={false} />)
  expect(toggle).toBeDisabled()
  expect(toggle).toHaveAttribute('aria-pressed', 'false')
  expect(screen.queryByRole('combobox', { name: 'Catalyst' })).toBeNull()
})

it('submits quality alone with an inactive AND group and restores unchecked draft rows before adding a stat', async () => {
  const goal = emptyGoal(fixtureContext, 'fixture')
  expect(testRouteGoal(goal, true)).toMatchObject({
    general: { quality: 40 },
    groups: [{ type: 'AND', disabled: true, entries: [] }],
  })
  expect(testRouteGoal(goal, false)).toMatchObject({
    general: { quality: null },
    groups: [{ type: 'AND', disabled: false, entries: [] }],
  })
  goal.groups[0]!.entries.push({
    id: 'unchecked',
    statId: 'cold',
    unit: 'percent',
    range: { min: 42, max: null },
    weight: null,
    disabled: true,
  })
  const requestGoal = testRouteGoal(goal, true)
  const restored = flatEditorGoal(
    requestGoal,
    emptyGoal(fixtureContext, 'fixture'),
  )
  expect(restored.groups[0]).toMatchObject({
    disabled: false,
    entries: [{ id: 'unchecked', disabled: true }],
  })
  const catalog = await mockGoalFilterAdapter.catalog(
    fixtureContext,
    new AbortController().signal,
  )
  const editor = createGoalFilterEditor(restored)
  editor.getState().edit((g) =>
    addStat(
      g.groups[0]!,
      catalog.stats.find((s) => s.kind === 'EXPLICIT')!,
    ),
  )
  expect(testRouteGoal(editor.getState().goal, true).groups[0]).toMatchObject({
    disabled: false,
    entries: [{ id: 'unchecked', disabled: true }, { disabled: false }],
  })
  expect(requestGoal.groups[0]!.disabled).toBe(true)
  expect(goal.groups[0]!.entries[0]?.range.min).toBe(42)
})

it('carries independent AND groups into one list without reinterpreting duplicate ranges or alternative groups', () => {
  const blank = emptyGoal(fixtureContext, 'fixture')
  const group = blank.groups[0]!
  const row = {
    id: 'life',
    statId: 'life',
    unit: 'flat',
    range: { min: 1, max: null },
    disabled: false,
    weight: null,
  }
  const saved = {
    ...blank,
    groups: [
      { ...group, entries: [row] },
      {
        ...group,
        id: 'second',
        disabled: true,
        entries: [{ ...row, id: 'cold', statId: 'cold' }],
      },
    ],
  }
  expect(flatEditorGoal(saved, blank).groups).toMatchObject([
    {
      disabled: false,
      entries: [
        { id: 'life', disabled: false },
        { id: 'cold', disabled: true },
      ],
    },
  ])
  saved.groups[1]!.entries[0]!.statId = 'life'
  saved.groups[1]!.entries[0]!.range.min = 3
  expect(flatEditorGoal(saved, blank)).toBe(blank)
  expect(saved.groups).toHaveLength(2)
  saved.groups[1]!.type = 'COUNT'
  saved.groups[1]!.entries[0]!.statId = 'cold'
  expect(flatEditorGoal(saved, blank)).toBe(blank)
})
