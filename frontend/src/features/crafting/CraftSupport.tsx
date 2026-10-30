import { SupportToolbarContext } from './supportToolbar'
import { ScenarioTransferProvider } from './method-search/ScenarioTransferProvider'
import { useState } from 'react'
import { CraftStart } from './goal-filter/CraftStart'
import { CurrencyPolicy } from './method-search/CurrencyPolicy'
import { ScenarioImport } from './method-search/ScenarioCodePanel'
export function CraftSupport({ active }: { active: boolean }) {
  const [toolbar, setToolbar] = useState<HTMLElement | null>(null)
  const [shared, setShared] = useState(() => {
      try {
        return Boolean(
          sessionStorage.getItem('hephaistos.scenario.readonly.v1'),
        )
      } catch {
        return false
      }
    }),
    [editor, setEditor] = useState(0)
  return (
    <ScenarioTransferProvider>
      <SupportToolbarContext.Provider value={toolbar}>
        <ScenarioImport
          policyControl={<CurrencyPolicy />}
          settingsControl={
            <span ref={setToolbar} className="support-catalog-slot" />
          }
          onOpenChanged={setShared}
          onCopy={() => setEditor((x) => x + 1)}
        />
        <div hidden={shared}>
          <CraftStart key={editor} active={active && !shared} />
        </div>
      </SupportToolbarContext.Provider>
    </ScenarioTransferProvider>
  )
}
