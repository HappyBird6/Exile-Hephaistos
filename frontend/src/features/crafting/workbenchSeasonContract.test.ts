import { readFileSync } from 'node:fs'
import { expect, it } from 'vitest'
import {
  workbenchActionNames,
  workbenchCurrencyActions,
  workbenchOmens,
} from './workbenchApi'
const read = (name: string) =>
  JSON.parse(readFileSync(`../backend/src/main/resources/${name}`, 'utf8'))
it('connects every executable currency to a frontend action name and rejects retired action IDs', () => {
  const definitions = read('crafting/workbench-definitions-v1.json') as {
    currencies: { id: string }[]
  }
  const actions = new Set(definitions.currencies.map((c) => c.id))
  for (const action of actions)
    expect(workbenchActionNames, action).toHaveProperty(action)
  for (const [id, action] of Object.entries(workbenchCurrencyActions))
    expect(actions.has(action), id).toBe(true)
  const registered = read('crafting/registry-v2.json') as {
    entries: {
      id: string
      action?: string
      category: string
      serviceScope: string
    }[]
  }
  for (const entry of registered.entries.filter(
    (e) => e.serviceScope === 'ACTIVE' && e.action && e.category !== 'OMEN',
  )) {
    expect(actions.has(entry.action!), entry.id).toBe(true)
    expect(workbenchCurrencyActions[entry.id], entry.id).toBe(entry.action)
  }
})
it('keeps ordinary omen triggers aligned with the executable definitions', () => {
  const definitions = read('crafting/workbench-definitions-v1.json') as {
    omens: { id: string; trigger: string }[]
  }
  for (const omen of definitions.omens)
    expect(workbenchOmens.find((o) => o.id === omen.id)?.trigger, omen.id).toBe(
      omen.trigger,
    )
})
