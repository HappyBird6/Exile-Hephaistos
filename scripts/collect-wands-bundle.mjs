import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { collectArmourSource, cleanSource } from './armour-source.mjs'

const root = 'docs/evidence/wands-source-bundle-2026-10-05'
assert(!fs.existsSync(root), 'Preserve prior evidence output')
fs.mkdirSync(root)
const slugs = ['Bone', 'Siphoning', 'Volatile', 'Galvanic', 'Acrid', 'Offering', 'Critical', 'Primordial', 'Dueling'].map(s => `${s}_Wand`)
for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
  for (const slug of slugs) {
    const source = await collectArmourSource(slug, locale)
    assert(source.fields['Mods.enable_rarity'].includes('normal, magic, rare'))
    fs.writeFileSync(`${root}/${slug}.${locale}.json`, JSON.stringify(source, null, 2) + '\n')
  }
  const url = `https://poe2db.tw/${locale}/Wands`
  const response = await fetch(url)
  assert(response.ok)
  const html = await response.text()
  const line = html.split('\n').find(l => l.includes('new ModsView('))
  assert(line)
  const data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
  fs.writeFileSync(`${root}/Wands.${locale}.json`, JSON.stringify({ url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), data }, null, 2) + '\n')
}
console.log('Collected nine ordinary Wand candidates and complete six-language class source')
