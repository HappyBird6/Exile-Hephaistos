import { ScenarioTransferProvider } from './method-search/ScenarioTransferProvider'
import { useState } from 'react'
import { CraftStart } from './goal-filter/CraftStart'
import { CurrencyPolicy } from './method-search/CurrencyPolicy'
import { ScenarioImport } from './method-search/ScenarioCodePanel'
export function CraftSupport({ active }: { active: boolean }) {
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
      <ScenarioImport
        policyControl={<CurrencyPolicy />}
        onOpenChanged={setShared}
        onCopy={() => setEditor((x) => x + 1)}
      />
      <div hidden={shared}>
        <CraftStart key={editor} active={active && !shared} />
      </div>
    </ScenarioTransferProvider>
  )
}
