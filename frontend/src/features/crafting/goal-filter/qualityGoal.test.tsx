import { expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
import { GoalFilterPanel } from './GoalFilterPanel'

it('edits the optional minimum quality and explains the boundary above 20', async () => {
  const editor = createGoalFilterEditor(emptyGoal(fixtureContext, 'fixture'))
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <GoalFilterPanel
        editor={editor}
        adapter={mockGoalFilterAdapter}
        context={fixtureContext}
        bases={[]}
        language="en"
        compact
      />
    </QueryClientProvider>,
  )
  const quality = await screen.findByLabelText('Minimum quality')
  fireEvent.change(quality, { target: { value: '20' } })
  expect(editor.getState().goal.general.quality).toBe(20)
  expect(screen.queryByText(/Above 20:/)).not.toBeInTheDocument()
  fireEvent.change(quality, { target: { value: '30' } })
  expect(editor.getState().goal.general.quality).toBe(30)
  expect(screen.getByText(/Above 20:/)).toHaveTextContent(
    'Essence of the Breach',
  )
  fireEvent.change(quality, { target: { value: '41' } })
  expect(quality).toHaveAttribute('aria-invalid', 'true')
  fireEvent.change(quality, { target: { value: '' } })
  expect(editor.getState().goal.general.quality).toBeNull()
  expect(quality).toHaveAttribute('aria-invalid', 'false')
})
