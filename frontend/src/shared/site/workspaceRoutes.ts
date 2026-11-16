export type Workspace =
  'workbench' | 'support' | 'history' | 'community' | 'support-test'
export const workspacePath = (tab: Workspace) =>
  tab === 'support-test'
    ? '/support-test'
    : tab === 'community'
      ? '/community'
      : '/?workbench=' + (tab === 'workbench' ? 'simulation' : tab)
