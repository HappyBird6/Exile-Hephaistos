import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'

const root = process.argv[2]
assert(root && fs.existsSync(`${root}/bundle.json`), 'Collect base evidence first')
const clean = s => s.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/[—–]/g, '-').replace(/\s+/g, ' ').trim()
const signature = r => JSON.stringify([r.Name, String(r.Level), String(r.ModGenerationTypeID), r.ModFamilyList, clean(r.str)])
const known = new Map()
const catalogRoot = 'backend/src/main/resources/catalog'
for (const dir of fs.readdirSync(catalogRoot)) {
  const path = `${catalogRoot}/${dir}`
  if (!fs.statSync(path).isDirectory() || !fs.existsSync(`${path}/base.raw.json`)) continue
  const raw = JSON.parse(fs.readFileSync(`${path}/base.raw.json`))
  const catalog = JSON.parse(fs.readFileSync(`${path}/catalog.json`))
  // Jewel evidence uses another source format; it cannot prove an armour table row.
  if (!Array.isArray(raw)) continue
  for (const row of raw) {
    const definition = catalog.modifiers.find(d => d.name === row.Name && d.requiredItemLevel === Number(row.Level) && JSON.stringify(d.familyIds) === JSON.stringify(row.ModFamilyList) && clean(d.text) === clean(row.str))
    if (definition) known.set(signature(row), { pool: dir, definition })
  }
}
const bundle = JSON.parse(fs.readFileSync(`${root}/bundle.json`))
const summary = []
for (const page of [...new Set(bundle.map(b => b.page))]) {
  const path = `${root}/${page}.pool.json`
  assert(!fs.existsSync(path), 'Preserve previous pool audit')
  const url = `https://poe2db.tw/us/${page}`
  const response = await fetch(url)
  assert(response.ok, `${page}: ${response.status}`)
  const html = await response.text()
  const line = html.split('\n').find(l => l.includes('new ModsView('))
  assert(line, `${page}: missing modifier table`)
  const data = JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
  assert(data.normal.length > 0)
  const rows = data.normal.map(row => ({ row, existing: known.get(signature(row)) ?? null }))
  const record = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), sourceDisclaimer: clean(html.match(/[^<>]*Modifier weight[^]*?(?=<\/)/)?.[0] ?? ''), normalCount: rows.length, matchedCount: rows.filter(r => r.existing).length, unmatched: rows.filter(r => !r.existing).map(r => r.row), rows, status: 'REVIEW_REQUIRED_ORDERED_SPAWN_AND_CLASS_TARGETS_NOT_ASSUMED' }
  fs.writeFileSync(path, JSON.stringify(record, null, 2) + '\n')
  summary.push({ page, normalCount: record.normalCount, matchedCount: record.matchedCount, unmatchedCount: record.unmatched.length })
  fs.writeFileSync(`${root}/pool-summary.json`, JSON.stringify(summary, null, 2) + '\n')
  console.log(page, record.normalCount, record.matchedCount, record.unmatched.length)
}
