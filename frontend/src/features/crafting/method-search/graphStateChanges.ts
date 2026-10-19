import type { ItemCardData } from '../itemCardData'
export function graphStateChanges(
  current: ItemCardData,
  previous?: ItemCardData,
) {
  const marks: Record<string, 'added' | 'changed'> = {}
  if (!previous) return { marks, removed: [] }
  for (const line of current.modifiers) {
    const old = previous.modifiers.find((v) => v.id === line.id)
    if (!old) marks[line.id] = 'added'
    else if (old.text !== line.text) marks[line.id] = 'changed'
  }
  return {
    marks,
    removed: previous.modifiers.filter(
      (line) => !current.modifiers.some((v) => v.id === line.id),
    ),
  }
}
