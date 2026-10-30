import { expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createGoalFilterEditor, emptyGoal } from './editor'
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
