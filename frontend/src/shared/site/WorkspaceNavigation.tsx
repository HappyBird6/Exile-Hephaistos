import { workspacePath, type Workspace } from './workspaceRoutes'
import { useEffect } from 'react'
import { useI18n } from '../i18n/i18n'
import { navigate } from '../navigation/navigation'
import { communityText } from '../../features/community/messages'
import { sessionLibraryMessages } from '../../features/crafting/session-library/sessionLibraryMessages'
import { CurrencyTicker } from '../../features/crafting/currency-prices/CurrencyPrices'
const tabs = (['workbench', 'support', 'history', 'community'] as const).filter(
  (tab) =>
    tab !== 'support' || import.meta.env.VITE_CRAFT_SUPPORT_ENABLED !== 'false',
)
const panels = {
  workbench: 'workbench-panel-simulation',
  support: 'panel-support',
  history: 'workbench-panel-history',
  community: 'community-panel',
}
let focusTarget: Workspace | null = null
export function WorkspaceNavigation({
  active,
  disabled = false,
  onSelect = (tab) => navigate(workspacePath(tab)),
}: {
  active: Workspace
  disabled?: boolean
  onSelect?: (tab: Workspace) => void
}) {
  const { locale, t } = useI18n()
  useEffect(() => {
    if (focusTarget === active) {
      document.getElementById('workspace-' + active)?.focus()
      focusTarget = null
    }
  }, [active])
  return (
    <nav className="workspace-nav" aria-label={t('ui.main_navigation')}>
      <div role="tablist" aria-label={t('ui.crafting_workspace')}>
        {tabs.map((tab, index) => (
          <button
            key={tab}
            type="button"
            role="tab"
            id={'workspace-' + tab}
            aria-selected={active === tab}
            aria-controls={panels[tab]}
            tabIndex={active === tab ? 0 : -1}
            disabled={disabled}
            onClick={() => onSelect(tab)}
            onKeyDown={(event) => {
              if (
                disabled ||
                !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)
              )
                return
              event.preventDefault()
              const next =
                event.key === 'Home'
                  ? tabs[0]!
                  : event.key === 'End'
                    ? tabs.at(-1)!
                    : tabs[
                        (index +
                          (event.key === 'ArrowRight' ? 1 : -1) +
                          tabs.length) %
                          tabs.length
                      ]!
              focusTarget = next
              onSelect(next)
            }}
          >
            {tab === 'community'
              ? communityText(locale).board
              : tab === 'history'
                ? sessionLibraryMessages[locale].history
                : t(`workspace.${tab}`)}
          </button>
        ))}
      </div>
      <CurrencyTicker />
    </nav>
  )
}
