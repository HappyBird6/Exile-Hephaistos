import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'

const root = 'docs/evidence/belts-source-bundle-2026-10-05'
fs.mkdirSync(root, { recursive: true })
const slugs = ['Linen', 'Wide', 'Long', 'Plate', 'Ornate', 'Mail', 'Double', 'Heavy', 'Utility', 'Fine', 'Invoking', 'Sinew', 'Forking', 'Stalking', 'Runemastered_Heavy'].map(s => `${s}_Belt`)
for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
  for (const slug of [...slugs, 'Belts', 'Catalysts']) {
    const path = `${root}/${slug}.${locale}`
    assert(!fs.existsSync(`${path}.json`) && !fs.existsSync(`${path}.html`), 'Preserve previous evidence')
    const url = `https://poe2db.tw/${locale}/${slug}`
    const response = await fetch(url)
    assert(response.ok, `${url}: ${response.status}`)
    const html = await response.text()
    const record = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex') }
    if (slug === 'Belts') {
      const line = html.split('\n').find(l => l.includes('new ModsView('))
      assert(line)
      record.data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
    } else {
      record.variants = html.split(/(?=<div class="newItemPopup NormalPopup)/).slice(1).map(section => {
        const popup = section.match(/^<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
        assert(popup)
        return { name: clean(popup.match(/<span class="lc">([^]*?)<\/span>/)?.[1] ?? ''), card: clean(popup), requirements: clean(popup.match(/<div class="requirements">([^]*?)<\/div>/)?.[1] ?? ''), fields: Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m => [clean(m[1]), clean(m[2])])) }
      }).filter(v => v.fields.Type?.startsWith('Metadata/Items/'))
    }
    fs.writeFileSync(`${path}.html`, html, { flag: 'wx' })
    fs.writeFileSync(`${path}.json`, JSON.stringify(record, null, 2) + '\n', { flag: 'wx' })
  }
}
console.log('Collected Belt identities, complete class pools and Catalyst class-rule evidence in six locales')
