import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/belts-source-bundle-2026-10-05/next-equipment'
assert(!fs.existsSync(root), 'Preserve prior candidate evidence'); fs.mkdirSync(root)
const inventory = []
for (const page of ['Items', 'Crossbows', 'One_Hand_Maces', 'Two_Hand_Maces', 'Quarterstaves', 'Spears', 'Staves', 'Talismans', 'Shields', 'Bucklers', 'Foci', 'Quivers', 'Claws', 'Daggers', 'One_Hand_Swords', 'Two_Hand_Swords', 'One_Hand_Axes', 'Two_Hand_Axes', 'Flails', 'Traps']) {
  const url = `https://poe2db.tw/us/${page}`, response = await fetch(url)
  assert(response.ok, `${url}: ${response.status}`)
  const html = await response.text()
  const cards = [...html.matchAll(/<div class="flex-grow-1 ms-2"><a class="whiteitem[^]*?href="([^"]+)"[^]*?<\/a><div>([^]*?)(?=<\/div><\/div><\/div>|$)/g)].map(m => ({ slug: m[1], card: clean(m[0]), requiredLevel: +(m[0].match(/Level (\d+)(?=<|,|\s)/)?.[1] ?? 0) }))
  assert(cards.every(c => c.requiredLevel <= 100), 'Requirement must end at HTML boundary')
  const ordinaryCandidates = cards.filter(c => !/Runeforged|Runemastered/.test(c.card)).sort((a,b) => b.requiredLevel - a.requiredLevel)
  const entry = { page, url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), cards, highestRequirementCandidates: ordinaryCandidates.slice(0,6), availability: 'Candidate listing only; normal rarity flag and class page are not release proof' }
  fs.writeFileSync(`${root}/${page}.html`, html, { flag: 'wx' }); fs.writeFileSync(`${root}/${page}.json`, JSON.stringify(entry,null,2)+'\n', { flag: 'wx' })
  inventory.push(entry)
}
fs.writeFileSync(`${root}/inventory.json`, JSON.stringify(inventory,null,2)+'\n', { flag: 'wx' })
console.log(inventory.map(e => [e.page,e.cards.length,e.highestRequirementCandidates.map(c=>c.slug)]))
