import { useMemo, useReducer } from 'react'
import type { ReactNode } from 'react'
import {
  ScenarioTransferContext,
  emptyTransfer,
  transferReducer,
} from './scenarioTransfer'
export function ScenarioTransferProvider({
  children,
}: {
  children: ReactNode
}) {
  const [state, dispatch] = useReducer(transferReducer, emptyTransfer)
  const actions = useMemo(
    () => ({
      begin: (owner: string) => dispatch({ type: 'begin', owner }),
      ready: (owner: string, code: string, status: string) =>
        dispatch({ type: 'ready', owner, code, status }),
      clear: (owner: string) => dispatch({ type: 'clear', owner }),
      failed: (owner: string) => dispatch({ type: 'failed', owner }),
      edit: (text: string) => dispatch({ type: 'edit', text }),
      useGenerated: () => dispatch({ type: 'generated' }),
    }),
    [],
  )
  return (
    <ScenarioTransferContext.Provider value={{ state, ...actions }}>
      {children}
    </ScenarioTransferContext.Provider>
  )
}
