import fs from 'node:fs'
import path from 'node:path'
import type { Plugin } from 'vite'

/** Split only presentation dictionaries. Canonical source files and backend digest inputs stay intact. */
export function localeDataPlugin(eager = false): Plugin {
  const root = path.resolve(import.meta.dirname, '../src/shared/i18n')
  const read = (name: string) =>
    JSON.parse(fs.readFileSync(path.join(root, name), 'utf8'))
  const messages = read('messages.json'),
    terms = read('gameTerms.json'),
    modifiers = read('modifierTemplates.json')
  const augmentRows = read('../../features/crafting/augments.json') as Array<{
    id: string
    names: Record<string, string>
    properties: Record<string, string[]>
    effects: Record<string, Record<string, string>>
    sources?: unknown
  }>
  const augmentDisplay = read('augmentDisplay.json')
  const augmentEffects = read(
    '../../features/crafting/augmentEffectTemplates.json',
  ) as Record<
    string,
    Record<
      string,
      Array<{ classes: string[]; locales: Record<string, unknown> }>
    >
  >
  const augmentCommon = augmentRows.map((row) => {
    const {
      sources: _sources,
      names: _names,
      properties: _properties,
      effects,
      ...common
    } = row
    void _sources
    void _names
    void _properties
    return {
      ...common,
      names: {},
      properties: {},
      effects: Object.fromEntries(Object.keys(effects).map((key) => [key, {}])),
    }
  })
  const effectsCommon = Object.fromEntries(
    Object.entries(augmentEffects).map(([id, groups]) => [
      id,
      Object.fromEntries(
        Object.entries(groups).map(([group, rows]) => [
          group,
          rows.map((row) => ({ ...row, locales: {} })),
        ]),
      ),
    ]),
  )
  function augmentLocale(locale: string) {
    return augmentRows.map((row) => ({
      id: row.id,
      name: augmentDisplay[row.id]?.[locale]?.name ?? row.names[locale],
      properties:
        augmentDisplay[row.id]?.[locale]?.properties ?? row.properties[locale],
      effects: Object.fromEntries(
        Object.entries(row.effects).map(([key, values]) => [
          key,
          augmentDisplay[row.id]?.[locale]?.effects?.[key] ?? values[locale],
        ]),
      ),
    }))
  }
  function effectLocale(locale: string) {
    return Object.fromEntries(
      Object.entries(augmentEffects).map(([id, groups]) => [
        id,
        Object.fromEntries(
          Object.entries(groups).map(([group, rows]) => [
            group,
            rows.map((row) => row.locales[locale] ?? null),
          ]),
        ),
      ]),
    )
  }
  const definitionGroups: Record<string, Record<string, unknown>> = {}
  for (const [id, value] of Object.entries(modifiers.definitions))
    (definitionGroups[id.split(':')[0]!] ??= {})[id] = value
  const locales = Object.keys(messages)
  const watched = [
    'messages.json',
    'gameTerms.json',
    'modifierTemplates.json',
    'augmentDisplay.json',
    '../../features/crafting/augments.json',
    '../../features/crafting/augmentEffectTemplates.json',
  ].map((name) => path.resolve(root, name))
  const prefix = '\0locale-data:'
  return {
    name: 'locale-data',
    buildStart() {
      for (const file of watched) this.addWatchFile(file)
    },
    async handleHotUpdate(context) {
      if (watched.includes(context.file)) {
        await context.server.restart()
        return []
      }
    },
    resolveId(id) {
      if (id === 'virtual:locale-data') return prefix + 'index'
      if (id.startsWith('virtual:locale-data/'))
        return prefix + id.slice('virtual:locale-data/'.length)
    },
    load(id) {
      if (!id.startsWith(prefix)) return
      const locale = id.slice(prefix.length)
      if (locale.startsWith('definitions/'))
        return `export default ${JSON.stringify(definitionGroups[locale.slice('definitions/'.length)] ?? {})}`
      if (locale !== 'index') {
        if (!locales.includes(locale)) throw Error('Unknown locale data')
        return `export default ${JSON.stringify({ messages: messages[locale], terms: terms[locale], templates: modifiers.templates[locale], augments: augmentLocale(locale), augmentEffects: effectLocale(locale) })}`
      }
      const initial = eager ? locales : ['en']
      return `${initial.map((l) => `import ${l.replaceAll('-', '_')} from 'virtual:locale-data/${l}'`).join('\n')}
const loaded = new Set(${JSON.stringify(initial)});
export const data={messages:{},terms:{},modifiers:${JSON.stringify({ ...modifiers, definitions: eager ? modifiers.definitions : {}, templates: {} })},augments:${JSON.stringify(augmentCommon)},augmentEffects:${JSON.stringify(effectsCommon)}};
function install(locale,value){
for(let i=0;i<data.augments.length;i++){const row=data.augments[i],translated=value.augments[i];row.names[locale]=translated.name;row.properties[locale]=translated.properties;for(const [key,text]of Object.entries(translated.effects))row.effects[key][locale]=text}
for(const [id,groups]of Object.entries(value.augmentEffects))for(const [group,rows]of Object.entries(groups))rows.forEach((row,i)=>{if(row)data.augmentEffects[id][group][i].locales[locale]=row});
data.messages[locale]=value.messages;data.terms[locale]=value.terms;data.modifiers.templates[locale]=value.templates;loaded.add(locale)}
${locales.map((l) => `data.messages[${JSON.stringify(l)}]={};data.terms[${JSON.stringify(l)}]={};data.modifiers.templates[${JSON.stringify(l)}]={};`).join('\n')}
${initial.map((l) => `install(${JSON.stringify(l)},${l.replaceAll('-', '_')});`).join('\n')}
const loaders={${locales.map((l) => `${JSON.stringify(l)}:()=>import('virtual:locale-data/${l}')`).join(',')}};
const definitionLoaders={${Object.keys(definitionGroups)
        .map(
          (key) =>
            `${JSON.stringify(key)}:()=>import(${JSON.stringify('virtual:locale-data/definitions/' + key)})`,
        )
        .join(',')}};
const definitionPending=new Map();
export async function loadModifierDefinitions(ids){await Promise.all([...new Set(ids.map(id=>id.split(':')[0]))].map(group=>{if(!definitionLoaders[group])return;if(!definitionPending.has(group))definitionPending.set(group,definitionLoaders[group]().then(m=>Object.assign(data.modifiers.definitions,m.default)).catch(e=>{definitionPending.delete(group);throw e}));return definitionPending.get(group)}))}
const pending=new Map();
export function isLocaleLoaded(locale){return loaded.has(locale)}
export function loadLocaleData(locale){if(loaded.has(locale))return Promise.resolve();if(!loaders[locale])return Promise.reject(Error('Unknown locale'));if(!pending.has(locale))pending.set(locale,loaders[locale]().then(m=>install(locale,m.default)).finally(()=>pending.delete(locale)));return pending.get(locale)}
`
    },
  }
}
