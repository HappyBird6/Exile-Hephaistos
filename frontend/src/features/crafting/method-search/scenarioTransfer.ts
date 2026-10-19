import { createContext } from 'react'
export type TransferState = {
  owner: string | null
  text: string
  generated: string | null
  dirty: boolean
  pending: boolean
  status: string
  failed: boolean
}
export const emptyTransfer: TransferState = {
  owner: null,
  text: '',
  generated: null,
  dirty: false,
  pending: false,
  status: '',
  failed: false,
}
export type TransferAction =
  | { type: 'begin'; owner: string }
  | { type: 'ready'; owner: string; code: string; status: string }
  | { type: 'clear' | 'failed'; owner: string }
  | { type: 'edit'; text: string }
  | { type: 'generated' }
export function transferReducer(
  state: TransferState,
  action: TransferAction,
): TransferState {
  switch (action.type) {
    case 'begin':
      return {
        ...state,
        owner: action.owner,
        pending: true,
        generated: null,
        status: '',
        failed: false,
        text: state.dirty ? state.text : '',
      }
    case 'ready':
      return state.owner !== action.owner
        ? state
        : {
            ...state,
            pending: false,
            generated: action.code,
            status: action.status,
            text: state.dirty ? state.text : action.code,
          }
    case 'clear':
      return state.owner !== action.owner
        ? state
        : {
            ...state,
            owner: null,
            pending: false,
            generated: null,
            status: '',
            text: state.dirty ? state.text : '',
          }
    case 'failed':
      return state.owner !== action.owner
        ? state
        : { ...state, pending: false, failed: true, generated: null }
    case 'edit':
      return { ...state, text: action.text, dirty: true }
    case 'generated':
      return { ...state, text: state.generated ?? '', dirty: false }
  }
}
export type Transfer = {
  state: TransferState
  begin: (owner: string) => void
  ready: (owner: string, code: string, status: string) => void
  clear: (owner: string) => void
  failed: (owner: string) => void
  edit: (text: string) => void
  useGenerated: () => void
}
export const ScenarioTransferContext = createContext<Transfer | null>(null)
