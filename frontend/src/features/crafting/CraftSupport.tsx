import { SupportToolbarContext } from './supportToolbar'
import { useState } from 'react'
import { CraftStart } from './goal-filter/CraftStart'
import { CurrencyPolicy } from './method-search/CurrencyPolicy'
export function CraftSupport({ active }: { active: boolean }) {
  const [toolbar, setToolbar] = useState<HTMLElement | null>(null)
  return (
    <SupportToolbarContext.Provider value={toolbar}>
      <div className="support-controls">
        <CurrencyPolicy />
        <span ref={setToolbar} className="support-catalog-slot" />
      </div>
      <CraftStart active={active} />
    </SupportToolbarContext.Provider>
  )
}
