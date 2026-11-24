export type Workspace =
  'workbench' | 'support' | 'support-test' | 'history' | 'community'
export const workspacePath = (tab: Workspace) =>
  tab === 'community' || tab === 'support-test'
    ? '/' + tab
    : '/?workbench=' + (tab === 'workbench' ? 'simulation' : tab)
