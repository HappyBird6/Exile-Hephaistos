import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const root = 'docs/evidence/hallowed-source-bundle-2026-10-04'
const c = read('backend/src/main/resources/catalog/hallowed-sceptre/catalog.json')
for (const [file, digest] of [['base.raw.json', c.metadata.rawSha256], ['details.raw.json', c.metadata.detailsSha256]]) assert.equal(crypto.createHash('sha256').update(fs.readFileSync('backend/src/main/resources/catalog/hallowed-sceptre/' + file)).digest('hex'), digest)
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
for (const [l, remote] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
  const row = read(`${root}/Sceptres.${remote}.json`).data.perfect_essence.find(r => r.Code === 'EssenceAuraEffect1')
  const binding = display.definitions['rattling-sceptre:suffix:essence-aura-magnitude']
  assert.equal(display.templates[l][binding.template].template.replace(/\{v(\d+)\}/g, (_, n) => binding.values[+n]).replaceAll('\n', ''), clean(row.str).replaceAll('\n', ''))
}
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
assert.equal(registry.entries.filter(e => e.serviceScope === 'DEFERRED').length, 50)
assert(!registry.entries.filter(e => e.serviceScope === 'DEFERRED').some(e => e.supportedBases?.includes('hallowed')))
assert.equal(c.modifiers.filter(d => d.weight > 0).length, 150)
assert.equal(c.modifiers.filter(d => d.weight === 0).length, 1)
console.log('Hallowed checksums, special six-language templates, deferred50 and exact 151 definitions verified')
