import fs from 'node:fs'
import assert from 'node:assert/strict'
import { cleanSource } from './armour-source.mjs'
const path = 'frontend/src/shared/i18n/modifierTemplates.json'
const catalog = JSON.parse(fs.readFileSync(path, 'utf8'))
const binding = catalog.definitions['forking-belt:implicit:base']
for (const [locale, remote] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
  const html = fs.readFileSync(`docs/evidence/belts-source-bundle-2026-10-05/Forking_Belt.${remote}.html`, 'utf8')
  const texts = [...html.matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].filter(m => m[0].includes('<tr><th>Family')).map(m => cleanSource(m[1]))
  const translated = texts.join('\n')
  let index = 0
  const template = translated.replace(/[+]?(?:\(-?\d+[—–−?-]-?\d+\)|\d+(?:\.\d+)?)/g, () => `{v${index++}}`)
  assert.equal(index, binding.values.length)
  assert.equal(template.replace(/\{v(\d+)\}/g, (_, n) => binding.values[+n]), translated)
  catalog.templates[locale][binding.template].template = template
}
fs.writeFileSync(path, JSON.stringify(catalog, null, 2) + '\n')
