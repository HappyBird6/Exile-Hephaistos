export const craftingWorkspaceTabs = (
  ['workbench', 'support', 'preview', 'history'] as const
).filter(
  (tab) =>
    tab !== 'support' || import.meta.env.VITE_CRAFT_SUPPORT_ENABLED !== 'false',
)
export type Workspace = (typeof craftingWorkspaceTabs)[number] | 'community'
export const workspacePath = (tab: Workspace) =>
  tab === 'community'
    ? '/community'
    : '/?workbench=' + (tab === 'workbench' ? 'simulation' : tab)
