import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { collectArmourSource } from './armour-source.mjs'

const root = 'docs/evidence/hallowed-source-bundle-2026-10-04'
fs.mkdirSync(root, { recursive: true })
for (const locale of ['us', 'kr', 'jp', 'cn', 'tw', 'sp']) {
  for (const slug of ['Hallowed_Sceptre', 'Sceptres']) {
    const path = `${root}/${slug}.${locale}.json`
    assert(!fs.existsSync(path), 'Preserve prior output')
    if (slug === 'Hallowed_Sceptre') {
      fs.writeFileSync(path, JSON.stringify(await collectArmourSource(slug, locale), null, 2) + '\n')
    } else {
      const url = `https://poe2db.tw/${locale}/${slug}`
      const response = await fetch(url)
      assert(response.ok)
      const html = await response.text()
      const line = html.split('\n').find(l => l.includes('new ModsView('))
      assert(line)
      const data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
      fs.writeFileSync(path, JSON.stringify({ url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), data: { normal: data.normal, essence: data.essence, perfect_essence: data.perfect_essence } }, null, 2) + '\n')
    }
  }
}
