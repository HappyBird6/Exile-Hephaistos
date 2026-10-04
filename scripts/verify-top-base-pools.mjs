import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const clean = (s) => s.replace(/<[^>]*>/g, '').replace(/&amp;/g, '&').replace(/&nbsp;/g, ' ').trim()
const signature = (r) => JSON.stringify([r.Name, r.Level, r.ModGenerationTypeID, r.ModFamilyList, r.DropChance, clean(r.str), r.spawn_no])
for (const [page, pool] of [['Body_Armours_str', 'rusted-cuirass'], ['Helmets_str', 'rusted-greathelm']]) {
  const url = `https://poe2db.tw/us/${page}`
  const html = await (await fetch(url)).text()
  const line = html.split('\n').find((l) => l.includes('new ModsView('))
  assert(line, `${page}: ModsView not found`)
  const data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
  const old = JSON.parse(fs.readFileSync(`backend/src/main/resources/catalog/${pool}/base.raw.json`))
  const before = old.map(signature).sort(), after = data.normal.map(signature).sort()
  const result = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), currentNormalCount: data.normal.length, existingNormalCount: old.length, exactPublishedRowsMatch: JSON.stringify(before) === JSON.stringify(after), normal: data.normal }
  fs.writeFileSync(`docs/evidence/top-base-bundle-2026-10-04/${pool}.pool-proof.json`, JSON.stringify(result, null, 2) + '\n')
  assert.deepEqual(after, before, `${page}: published modifier pool changed`)
  console.log(page, after.length, 'complete published rows match')
}
