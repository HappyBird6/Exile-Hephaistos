import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/staves-talismans-source-bundle-2026-10-05'
const pages = ['Staves', 'Talismans']
const slugs = ['Permafrost_Staff', 'Reflecting_Staff', 'Dark_Staff', 'Ravenous_Staff', 'Perching_Staff', 'Sanctified_Staff', 'Maji_Talisman', 'Fungal_Talisman', 'Jade_Talisman']
fs.mkdirSync(root, { recursive: true })
for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
  for (const slug of [...pages, ...slugs]) {
    const path = `${root}/${slug}.${locale}`
    if (fs.existsSync(`${path}.json`)) continue
    assert(!fs.existsSync(`${path}.html`), 'Preserve interrupted evidence')
    const url = `https://poe2db.tw/${locale}/${slug}`
    const response = await fetch(url)
    assert(response.ok, `${url}: ${response.status}`)
    const html = await response.text()
    const record = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex') }
    if (pages.includes(slug)) {
      const line = html.split('\n').find(l => l.includes('new ModsView('))
      assert(line)
      record.data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
      record.cards = [...html.matchAll(/<div class="flex-grow-1 ms-2"><a class="whiteitem[^]*?href="([^"]+)"[^]*?<\/a><div>([^]*?)(?=<\/div><\/div><\/div>|$)/g)].map(m => ({ slug: m[1], card: clean(m[0]) }))
    } else {
      record.variants = html.split(/(?=<div class="newItemPopup NormalPopup)/).slice(1).map(section => {
        const popup = section.match(/^<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
        assert(popup)
        return { name: clean(popup.match(/<span class="lc">([^]*?)<\/span>/)?.[1] ?? ''), card: clean(popup), requirements: clean(popup.match(/<div class="requirements">([^]*?)<\/div>/)?.[1] ?? ''), properties: [...popup.matchAll(/<div class="property">([^]*?)<\/div>/g)].map(m => clean(m[1])), fields: Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m => [clean(m[1]), clean(m[2])])) }
      }).filter(v => v.fields.Type?.startsWith('Metadata/Items/'))
      assert.equal(record.variants.length, 1, slug)
    }
    fs.writeFileSync(`${path}.html`, html, { flag: 'wx' })
    fs.writeFileSync(`${path}.json`, JSON.stringify(record, null, 2) + '\n', { flag: 'wx' })
    console.log(`${slug}/${locale}`)
  }
}
