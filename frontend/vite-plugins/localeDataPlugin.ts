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
  const locales = Object.keys(messages)
  const prefix = '\0locale-data:'
  return {
    name: 'locale-data',
    buildStart() {
      for (const name of [
        'messages.json',
        'gameTerms.json',
        'modifierTemplates.json',
      ])
        this.addWatchFile(path.join(root, name))
    },
    async handleHotUpdate(context) {
      if (
        ['messages.json', 'gameTerms.json', 'modifierTemplates.json'].some(
          (name) => context.file === path.join(root, name),
        )
      ) {
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
      if (locale !== 'index') {
        if (!locales.includes(locale)) throw Error('Unknown locale data')
        return `export default ${JSON.stringify({ messages: messages[locale], terms: terms[locale], templates: modifiers.templates[locale] })}`
      }
      const initial = eager ? locales : ['en']
      return `${initial.map((l) => `import ${l.replaceAll('-', '_')} from 'virtual:locale-data/${l}'`).join('\n')}
const loaded = new Set(${JSON.stringify(initial)});
export const data={messages:{},terms:{},modifiers:${JSON.stringify({ ...modifiers, templates: {} })}};
function install(locale,value){data.messages[locale]=value.messages;data.terms[locale]=value.terms;data.modifiers.templates[locale]=value.templates;loaded.add(locale)}
${locales.map((l) => `data.messages[${JSON.stringify(l)}]={};data.terms[${JSON.stringify(l)}]={};data.modifiers.templates[${JSON.stringify(l)}]={};`).join('\n')}
${initial.map((l) => `install(${JSON.stringify(l)},${l.replaceAll('-', '_')});`).join('\n')}
const loaders={${locales.map((l) => `${JSON.stringify(l)}:()=>import('virtual:locale-data/${l}')`).join(',')}};
const pending=new Map();
export function isLocaleLoaded(locale){return loaded.has(locale)}
export function loadLocaleData(locale){if(loaded.has(locale))return Promise.resolve();if(!loaders[locale])return Promise.reject(Error('Unknown locale'));if(!pending.has(locale))pending.set(locale,loaders[locale]().then(m=>install(locale,m.default)).finally(()=>pending.delete(locale)));return pending.get(locale)}
`
    },
  }
}
