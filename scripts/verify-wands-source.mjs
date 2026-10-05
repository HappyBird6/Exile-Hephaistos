import fs from 'node:fs'
import assert from 'node:assert/strict'
import { cleanSource as clean } from './armour-source.mjs'

const root = 'docs/evidence/wands-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const old = read('backend/src/main/resources/catalog/attuned-wand/catalog.json')
const raw = read('backend/src/main/resources/catalog/attuned-wand/base.raw.json')
const details = read('backend/src/main/resources/catalog/attuned-wand/details.raw.json')
const current = read(`${root}/Wands.us.json`).data
assert.deepEqual(current.normal, raw, 'Class pool changed; review before reuse')
const report = { bases: [], ordinaryRows: raw.length, policy: old.metadata.weightPolicy, localeDiscrepancies: [], blockers: [] }
for (const [name, skill] of Object.entries({ Bone: 'Bone Blast', Siphoning: 'Power Siphon', Volatile: 'Volatile Dead', Galvanic: 'Galvanic Field', Acrid: 'Decompose', Offering: 'Exsanguinate', Critical: 'Chaos Bolt', Primordial: 'Wither', Dueling: 'Spellslinger' })) {
  const source = read(`${root}/${name}_Wand.us.json`)
  assert(source.fields['Mods.enable_rarity'].startsWith('normal, magic, rare'))
  assert(source.card.includes(`Grants Skill: ${skill}`))
  const tags = source.fields.Tags.split(', ')
  const eligible = [], excluded = []
  for (const detail of details) {
    const spawn = [...detail.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
    const first = spawn.find(s => tags.includes(s.tag) || s.tag === 'default')
    assert(first, detail.code)
    const definition = old.modifiers.find(d => d.sourceUrl === detail.url)
    assert(definition && definition.requiredItemLevel === +detail.row.Level)
    assert.deepEqual(definition.familyIds, detail.row.ModFamilyList)
    assert.equal(definition.weight, +detail.row.DropChance)
    assert.equal(definition.text, clean(detail.row.str).replaceAll('\n', ''))
    ;(first.weight > 0 ? eligible : excluded).push({ id: definition.id, code: detail.code, firstMatchingSpawn: first })
  }
  for (const locale of ['kr', 'jp', 'cn', 'tw', 'sp']) {
    const local = read(`${root}/${name}_Wand.${locale}.json`)
    assert.equal(local.fields.Type, source.fields.Type)
    const numeric = s => [...s.matchAll(/\d+/g)].map(m => +m[0])
    if (JSON.stringify(numeric(local.requirements)) !== JSON.stringify(numeric(source.requirements))) report.localeDiscrepancies.push({ name, locale, canonicalEnglish: source.requirements, published: local.requirements })
  }
  report.bases.push({ name, id: source.fields.Type, skill, requirements: source.requirements, tags, eligible, excluded, ordinaryAvailability: source.fields['Mods.enable_rarity'] })
}
const output = `${root}/applicability.json`
assert(!fs.existsSync(output), 'Preserve existing verification output')
fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n')
console.log(report.bases.map(b => `${b.name}: ${b.eligible.length} eligible / ${b.excluded.length} excluded`).join('\n'))
console.log(`${report.localeDiscrepancies.length} locale requirement discrepancies preserved`)
