import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import { cleanSource as clean, collectArmourSource } from './armour-source.mjs'
const root = 'docs/evidence/bows-source-bundle-2026-10-04'
fs.mkdirSync(root, { recursive: true })
const locales = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
for (const slug of ['Warmonger_Bow', 'Guardian_Bow', 'Gemini_Bow', 'Fanatic_Bow', 'Obliterator_Bow']) {
  for (const locale of Object.values(locales)) {
    const path = `${root}/${slug}.${locale}.json`
    if (!fs.existsSync(path)) write(path, await collectArmourSource(slug, locale))
  }
}
for (const [locale, source] of Object.entries(locales)) {
  const path = `${root}/Bows.${locale}.json`
  if (fs.existsSync(path)) continue
  const url = `https://poe2db.tw/${source}/Bows`
  const response = await fetch(url)
  assert(response.ok, url)
  const html = await response.text()
  const line = html.split('\n').find(l => l.includes('new ModsView('))
  assert(line)
  const all = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
  write(path, { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), data: { normal: all.normal, essence: all.essence, perfect_essence: all.perfect_essence } })
}
console.log('Preserved thirty base snapshots and six class pool snapshots')
for (const slug of ['Guardian_Bow', 'Gemini_Bow', 'Fanatic_Bow', 'Obliterator_Bow']) {
  const path = `${root}/${slug}.implicit.json`
  if (fs.existsSync(path)) continue
  const url = `https://poe2db.tw/us/${slug}`
  const response = await fetch(url), html = await response.text()
  assert(response.ok)
  const stats = [...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => {
    const numbers = clean(m[2]).match(/-?\d+/g).map(Number)
    return { id: clean(m[1]).replace(/ /g, '_'), min: numbers[0], max: numbers[1], locality: clean(m[3]) }
  })
  assert(stats.length)
  write(path, { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), stats })
}
