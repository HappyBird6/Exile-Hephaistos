export type Workspace = 'workbench' | 'support' | 'history' | 'community'
export const workspacePath = (tab: Workspace) =>
  tab === 'community'
    ? '/community'
    : '/?workbench=' + (tab === 'workbench' ? 'simulation' : tab)
