import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'

const root = 'docs/evidence/sceptres-source-bundle-2026-10-05'
assert(!fs.existsSync(root) || fs.readdirSync(root).length === 0, 'Preserve prior evidence')
if (!fs.existsSync(root)) fs.mkdirSync(root)
for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
  for (const slug of ['Stoic_Sceptre', 'Omen_Sceptre', 'Shrine_Sceptre', 'Clasped_Sceptre', 'Wrath_Sceptre', 'Sceptres']) {
    const url = `https://poe2db.tw/${locale}/${slug}`
    const response = await fetch(url)
    assert(response.ok, url)
    const html = await response.text()
    const metadata = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex') }
    if (slug === 'Sceptres') {
      const line = html.split('\n').find(l => l.includes('new ModsView('))
      assert(line)
      metadata.data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
    } else {
      metadata.variants = html.split(/(?=<div class="newItemPopup NormalPopup)/).slice(1).map(section => {
        const popup = section.match(/^<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
        assert(popup)
        const fields = Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m => [clean(m[1]), clean(m[2])]))
        const skillLink = popup.match(/class="implicitMod"[^]*?href="([^"]+)"[^>]*>([^]*?)<\/a>/)
        assert(skillLink)
        return { name: clean(popup.match(/<span class="lc">([^]*?)<\/span>/)[1]), card: clean(popup), requirements: clean(popup.match(/<div class="requirements">([^]*?)<\/div>/)[1]), fields, skill: clean(skillLink[2]), skillUrl: new URL(skillLink[1], url).href }
      }).filter(v => v.fields.Type?.startsWith('Metadata/Items/'))
      fs.writeFileSync(`${root}/${slug}.${locale}.html`, html, { flag: 'wx' })
    }
    fs.writeFileSync(`${root}/${slug}.${locale}.json`, JSON.stringify(metadata, null, 2) + '\n', { flag: 'wx' })
  }
}
console.log('Collected all Sceptre variants and six locale class pools')
