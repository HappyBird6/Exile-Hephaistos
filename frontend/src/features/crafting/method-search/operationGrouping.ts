import type { AutomaticAction, AutomaticArrow } from './automaticApi'
export function operationKind(method: AutomaticArrow) {
  const currency = method.cycle.at(-1)?.currency ?? ''
  if (/^(GREATER_|PERFECT_)?EXALTED$/.test(currency)) return 'add'
  if (/^(GREATER_|PERFECT_)?CHAOS$/.test(currency))
    return method.initial.length ? 'prepare' : 'replace'
  return 'other'
}
export function operationGroups(methods: AutomaticArrow[]) {
  const groups = new Map<string, AutomaticArrow[]>()
  for (const method of methods) {
    const signature = (actions: AutomaticAction[]) =>
      actions.map((a) => [
        a.currency.replace(/^(GREATER_|PERFECT_)/, ''),
        a.purpose,
      ])
    const key =
      operationKind(method) === 'other'
        ? method.id
        : JSON.stringify([
            method.source,
            signature(method.initial),
            signature(method.cycle),
          ])
    groups.set(key, [...(groups.get(key) ?? []), method])
  }
  return [...groups].map(([id, variants]) => ({ id, variants }))
}
