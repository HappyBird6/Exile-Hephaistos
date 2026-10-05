import fs from 'node:fs'
import assert from 'node:assert/strict'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/belts-source-bundle-2026-10-05/next-equipment'
const inventory = JSON.parse(fs.readFileSync(`${root}/inventory.json`, 'utf8'))
const reviewed = inventory.map(entry => {
  const html = fs.readFileSync(`${root}/${entry.page}.html`, 'utf8')
  const cards = [...html.matchAll(/<div class="flex-grow-1 ms-2"><a class="whiteitem[^]*?href="([^"]+)"[^]*?<\/a><div>([^]*?)(?=<\/div><\/div><\/div>|$)/g)].map(m => ({ slug: m[1], card: clean(m[0]), requiredLevel: +(m[0].match(/Level (\d+)(?=<|,|\s)/)?.[1] ?? 0) }))
  assert(cards.every(c=>c.requiredLevel<=100), 'Requirement extraction must stop at HTML boundary')
  const candidates = cards.filter(c=>!/Runeforged|Runemastered/.test(c.card)).sort((a,b)=>b.requiredLevel-a.requiredLevel)
  return { page: entry.page, sourceUrl: entry.url, sourceSha256: entry.sha256, highestRequirementCandidates: candidates.slice(0,6), availability: entry.availability }
})
const q=reviewed.find(e=>e.page==='Quivers').highestRequirementCandidates.find(c=>c.slug==='Penetrating_Quiver')
assert.equal(q.requiredLevel,55,'Adjacent100% implicit is not part of required Level55')
fs.writeFileSync(`${root}/reviewed-candidates.json`,JSON.stringify(reviewed,null,2)+'\n',{flag:'wx'})
console.log('Reviewed bounded candidate requirements at source HTML boundaries; retained original inventory')
