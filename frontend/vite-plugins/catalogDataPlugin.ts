import fs from 'node:fs'
import path from 'node:path'
import type { Plugin } from 'vite'
/** Generate immutable pool bundles from canonical registrations, without a second hand-maintained catalog. */
export function catalogDataPlugin(eager = false): Plugin {
  const root = path.resolve(import.meta.dirname, '../src/features/crafting')
  const read = (name: string) =>
    JSON.parse(fs.readFileSync(path.join(root, name), 'utf8'))
  const definitions = read('basicJewelDefinitions.json'),
    reveal = read('manualRevealDefinitions.json'),
    essences = read('topBaseEssences.json'),
    bases = read('topBases.json'),
    policies = read('basePolicies.json')
  const materialNames = Object.fromEntries(
    Object.entries(read('materialTooltips.json')).map(([id, value]) => [
      id,
      (value as { name: string }).name,
    ]),
  )
  const byId: Record<string, { key: string; pool: string }> = {}
  for (const [key, base] of Object.entries(bases) as [
    string,
    { id: string; pool: string },
  ][])
    byId[base.id] = { key, pool: base.pool }
  for (const [key, base] of Object.entries(policies.legacy) as [
    string,
    { id: string },
  ][])
    byId[base.id] ??= { key, pool: key }
  type Group = {
    definitions: Record<string, unknown>
    reveal: {
      definitions: Record<string, unknown>
      bases: Record<string, string[]>
    }
    essences: Record<string, unknown>
  }
  const groups: Record<string, Group> = {}
  const index: Record<string, string> = {}
  for (const [id, base] of Object.entries(byId)) {
    const group = (groups[base.pool] ??= {
      definitions: {},
      reveal: { definitions: {}, bases: {} },
      essences: {},
    })
    index[id] = base.pool
    if (essences[base.key]) group.essences[base.key] = essences[base.key]
    if (reveal.bases[id]) {
      group.reveal.bases[id] = reveal.bases[id]
      for (const key of reveal.bases[id])
        group.reveal.definitions[key] = reveal.definitions[key]
    }
  }
  for (const [key, value] of Object.entries(definitions) as [
    string,
    { baseItemId: string },
  ][]) {
    const group = index[value.baseItemId]
    if (!group) throw Error(`Unregistered base: ${value.baseItemId}`)
    groups[group]!.definitions[key] = value
  }
  return {
    name: 'catalog-data',
    async handleHotUpdate(context) {
      if (
        [
          'basicJewelDefinitions.json',
          'manualRevealDefinitions.json',
          'topBaseEssences.json',
          'topBases.json',
          'basePolicies.json',
          'materialTooltips.json',
        ].some((file) => context.file === path.join(root, file))
      ) {
        await context.server.restart()
        return []
      }
    },
    resolveId(id) {
      if (id.startsWith('virtual:crafting-data')) return '\0' + id
    },
    load(id) {
      if (!id.startsWith('\0virtual:crafting-data')) return
      const group = id.slice('\0virtual:crafting-data/'.length)
      if (id !== '\0virtual:crafting-data')
        return `export default ${JSON.stringify(groups[group])}`
      return `export const materialNames=${JSON.stringify(materialNames)};export const definitions=${JSON.stringify(eager ? definitions : {})};export const reveal=${JSON.stringify(eager ? reveal : { definitions: {}, bases: {} })};export const essences=${JSON.stringify(eager ? essences : {})};
const index=${JSON.stringify(index)};const loaders={${Object.keys(groups)
        .map(
          (key) =>
            `${JSON.stringify(key)}:()=>import(${JSON.stringify('virtual:crafting-data/' + key)})`,
        )
        .join(',')}};const pending=new Map();
export async function loadBaseData(base){const key=index[base];if(!key||${eager})return;if(!pending.has(key))pending.set(key,loaders[key]().then(({default:data})=>{Object.assign(definitions,data.definitions);Object.assign(reveal.definitions,data.reveal.definitions);Object.assign(reveal.bases,data.reveal.bases);Object.assign(essences,data.essences)}).catch(error=>{pending.delete(key);throw error}));await pending.get(key)}`
    },
    buildStart() {
      for (const file of [
        'basicJewelDefinitions.json',
        'manualRevealDefinitions.json',
        'topBaseEssences.json',
        'topBases.json',
        'basePolicies.json',
        'materialTooltips.json',
      ])
        this.addWatchFile(path.join(root, file))
    },
  }
}
