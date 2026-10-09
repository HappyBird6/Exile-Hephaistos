import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { GoalFilterPanel } from './GoalFilterPanel'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { fixtureContext, mockGoalFilterAdapter } from './mock'
const editor = createGoalFilterEditor(
  emptyGoal(fixtureContext, 'goal-filter-fixture-v1'),
)
createRoot(document.getElementById('root')!).render(
  <QueryClientProvider client={new QueryClient()}>
    <GoalFilterPanel
      editor={editor}
      adapter={mockGoalFilterAdapter}
      context={fixtureContext}
      bases={[
        { id: 'fixture-base', label: 'Synthetic fixture base' },
        { id: 'unsupported-base', label: 'Unsupported base' },
      ]}
    />
  </QueryClientProvider>,
)
