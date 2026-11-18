import { createContext } from 'react'
import type { Film } from '../workbenchHistory'
export const OpenCraftingSession = createContext<
  ((film: Film) => Promise<boolean>) | undefined
>(undefined)
