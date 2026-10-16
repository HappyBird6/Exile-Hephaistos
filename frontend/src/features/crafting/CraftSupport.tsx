import { CraftStart } from './goal-filter/CraftStart'
import { CurrencyPolicy } from './method-search/CurrencyPolicy'

export function CraftSupport({ active }: { active: boolean }) {
  return (
    <>
      <CurrencyPolicy />
      <CraftStart active={active} />
    </>
  )
}
