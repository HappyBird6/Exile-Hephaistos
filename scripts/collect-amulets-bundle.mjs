import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import { collectArmourSource, cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/amulets-source-bundle-2026-10-04'
fs.mkdirSync(root, { recursive: true })
const write = (p, v) => { assert(!fs.existsSync(p), 'Preserve prior output'); fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n') }
for (const slug of ['Stellar_Amulet', 'Amber_Amulet', 'Bloodstone_Amulet', 'Lunar_Amulet', 'Azure_Amulet', 'Crimson_Amulet', 'Pearlescent_Amulet']) {
  for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
    const path = `${root}/${slug}.${locale}.json`
    if (!fs.existsSync(path)) write(path, await collectArmourSource(slug, locale))
  }
  const path = `${root}/${slug}.implicit.json`
  if (fs.existsSync(path)) continue
  const url = `https://poe2db.tw/us/${slug}`, response = await fetch(url)
  assert(response.ok)
  const html = await response.text()
  const stats = [...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => {
    const n = clean(m[2]).match(/-?\d+/g).map(Number)
    return { id: clean(m[1]).replace(/ /g, '_'), min: n[0], max: n[1], locality: clean(m[3]) }
  })
  const family = clean(html.match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)?.[1] ?? '')
  const tagSection = html.match(/<tr><th>Craft Tags<td>([^]*?)<\/table>/)?.[1] ?? ''
  const craftTags = [...tagSection.matchAll(/<span class='badge bg-primary'>([^]*?)<\/span>/g)].map(m => clean(m[1]).toLowerCase())
  const implicitText = clean(html.match(/<h5 class="card-header">([^]*?)<\/h5>/g)?.find(s => !s.includes(' /7')) ?? '')
  assert(stats.length && family && craftTags.length)
  write(path, { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), stats, family, craftTags, implicitText, html })
}
for (const [locale, source] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
  const path = `${root}/Amulets.${locale}.json`
  if (fs.existsSync(path)) continue
  const url = `https://poe2db.tw/${source}/Amulets`, response = await fetch(url)
  assert(response.ok)
  const html = await response.text(), line = html.split('\n').find(l => l.includes('new ModsView('))
  assert(line)
  const data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
  write(path, { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), data: { normal: data.normal, essence: data.essence, perfect_essence: data.perfect_essence } })
}
