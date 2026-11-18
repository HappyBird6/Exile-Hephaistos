import { createContext } from 'react'
import type { Item } from '../path-tree/types'
export const OpenCraftingSession = createContext<
  | ((item: Item, identity: string, steps: string[]) => Promise<boolean>)
  | undefined
>(undefined)
