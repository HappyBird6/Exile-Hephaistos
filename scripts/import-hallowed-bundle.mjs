import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'

const root = 'docs/evidence/hallowed-source-bundle-2026-10-04'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const source = read(`${root}/Hallowed_Sceptre.us.json`)
const old = read('backend/src/main/resources/catalog/rattling-sceptre/catalog.json')
const raw = read('backend/src/main/resources/catalog/rattling-sceptre/base.raw.json')
const details = read('backend/src/main/resources/catalog/rattling-sceptre/details.raw.json')
const special = read('backend/src/main/resources/catalog/rattling-sceptre/perfect-essences.catalog.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const pool = read(`${root}/Sceptres.us.json`).data
assert.deepEqual(pool.normal, raw)
assert.equal(source.fields.Type, 'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13')
assert.equal(source.fields.Tags, 'sceptre, onehand')
assert.equal(source.requirements, 'Requires:  Level 65, 114 Int')
assert.equal(source.fields['Quality.max_quality'], '20')
assert(source.card.includes('Spirit: 100') && source.card.includes('Grants Skill: Skeletal Warrior'))
assert.equal(source.fields['Mods.enable_rarity'], 'normal, magic, rare, unique')
for (let i = 0; i < raw.length; i++) {
  assert.deepEqual(details[i].row, raw[i])
  const spawn = [...details[i].html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
  const first = spawn.find(s => ['sceptre', 'onehand', 'default'].includes(s.tag))
  assert(first && first.tag !== 'default' && first.weight > 0, raw[i].Name)
  const d = old.modifiers.find(d => d.sourceUrl === details[i].url)
  assert(d && d.requiredItemLevel === +raw[i].Level && d.familyIds.join(',') === raw[i].ModFamilyList.join(',') && d.weight === +raw[i].DropChance && d.text === clean(raw[i].str).replaceAll('\n', ''))
}
const requirements = {}, sourceProperties = {}, sourceRequirements = {}
for (const [locale, remote] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
  const s = read(`${root}/Hallowed_Sceptre.${remote}.json`)
  assert.equal(s.fields.Type, source.fields.Type)
  const card = s.card.split('\n').map(line => line.trim()).find(line => line.includes(s.requirements))
  const at = card.indexOf(s.requirements)
  const spirit = card.slice(s.fields.Class.length, at)
  const skill = card.slice(at + s.requirements.length).trim()
  assert(spirit.endsWith('100') && skill)
  sourceProperties[locale] = [spirit, skill]
  sourceRequirements[locale] = s.requirements
  // Spanish public card is stale (36 Str / 89 Int); retain it in evidence, display
  // the verified current English numeric requirement using its source vocabulary.
  requirements[locale] = locale === 'es' ? 'Requiere:  Nivel 65, 114 Int' : s.requirements
  terms[locale].Hallowed_Sceptre = { name: s.name, lines: [spirit, skill], itemKey: s.fields.Type, sourceUrl: s.url }
  const rows = read(`${root}/Sceptres.${remote}.json`).data.normal
  assert.equal(rows.length, raw.length)
  for (let i = 0; i < rows.length; i++) {
    assert.equal(rows[i].Level, raw[i].Level)
    assert.deepEqual(rows[i].ModFamilyList, raw[i].ModFamilyList)
    const d = old.modifiers.find(d => d.sourceUrl === details[i].url)
    const b = display.definitions[d.id]
    assert.equal(display.templates[locale][b.template].template.replace(/\{v(\d+)\}/g, (_, n) => b.values[+n]).replaceAll('\n', ''), clean(rows[i].str).replaceAll('\n', ''))
  }
}
const modifiers = [...old.modifiers, ...special.modifiers]
const poolRoot = 'backend/src/main/resources/catalog/hallowed-sceptre'
if (fs.existsSync(`${poolRoot}/catalog.json`)) assert.equal(read(`${poolRoot}/catalog.json`).base.id, source.fields.Type, 'Never replace a different catalog')
if (!fs.existsSync(poolRoot)) fs.mkdirSync(poolRoot)
const rawText = JSON.stringify(raw, null, 2) + '\n'
const detailText = JSON.stringify({ ordinary: details, special: read('backend/src/main/resources/catalog/rattling-sceptre/perfect-essences.raw.json'), base: source }, null, 2) + '\n'
fs.writeFileSync(`${poolRoot}/base.raw.json`, rawText)
fs.writeFileSync(`${poolRoot}/details.raw.json`, detailText)
write(`${poolRoot}/catalog.json`, { metadata: { ...old.metadata, snapshotId: `poe2db-hallowed-20261004-${hash(rawText + detailText).slice(0, 16)}`, retrievedAt: source.retrievedAt, rawSha256: hash(rawText), detailsSha256: hash(detailText), suffixCount: old.metadata.suffixCount + special.modifiers.length }, base: { ...old.base, id: source.fields.Type, name: source.name, sourceUrl: source.url }, modifiers })
const bases = read('backend/src/main/resources/catalog/top-bases.json')
if (bases.hallowed) assert.equal(bases.hallowed.id, source.fields.Type)
bases.hallowed = { key: 'hallowed', slug: 'Hallowed_Sceptre', pool: 'hallowed-sceptre', family: 'sceptres', id: source.fields.Type, name: source.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: ['sceptre', 'onehand'], armour: 0, strength: 0, intelligence: 114, requiredLevel: 65, maximumQuality: 20, requirements, sourceProperties }
for (const p of ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json']) write(p, bases)
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
overrides.hallowed = { fixed: { LESSER_ESSENCE_COMMAND: ['rattling-sceptre:prefix:agitative'], ESSENCE_COMMAND: ['rattling-sceptre:prefix:provocative'], GREATER_ESSENCE_COMMAND: ['rattling-sceptre:prefix:motivating'] }, replacements: { PERFECT_ESSENCE_COMMAND: ['rattling-sceptre:suffix:essence-aura-magnitude'] } }
for (const row of pool.essence) {
  const action = row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_', '_').replaceAll('_of_', '_').toUpperCase()
  if (action.endsWith('_INFINITE')) continue // Existing Sceptre scope excludes this special target set.
  const d = modifiers.find(d => d.sourceUrl.endsWith('Mods%2F' + row.Code) && d.requiredItemLevel === +row.Level && d.familyIds.join(',') === row.ModFamilyList.join(','))
  assert(d, row.Code)
  assert.equal(d.text, clean(row.str))
  const ids = overrides.hallowed.fixed[action] ??= []
  if (!ids.includes(d.id)) ids.push(d.id)
}
assert(pool.perfect_essence.some(r => r.Code === 'EssenceAuraEffect1' && +r.Level === 72 && clean(r.str) === special.modifiers[0].text))
for (const p of ['backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) write(p, overrides)
write('frontend/src/shared/i18n/gameTerms.json', terms)
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
registry.workbenchBases.hallowed = { ...registry.workbenchBases.sceptre, baseItemId: source.fields.Type, source: source.url, sourceSha256: source.sha256, requiredCharacterLevel: 65, ruleVersion: 'sceptre-workbench-essence-v1', ledgerVersion: 'sceptre-unverified-numeric-assumptions-v1' }
for (const e of registry.entries) if (e.id !== 'Omen_of_the_Blessed' && e.serviceScope !== 'DEFERRED' && (e.supportedBases?.includes('sceptre') || Object.hasOwn(overrides.hallowed.fixed, e.action ?? e.workbenchAction)) && !e.supportedBases.includes('hallowed')) e.supportedBases.push('hallowed')
write('backend/src/main/resources/crafting/registry-v2.json', registry)
write(`${root}/verification.json`, { passed: true, ordinary: raw.length, special: special.modifiers.length, localeTemplates: 6 * raw.length, builtInSkill: 'Skeletal Warrior', spirit: 100, canonicalRequirements: { level: 65, intelligence: 114 }, sourceRequirements, SpanishRequirementDiscrepancy: { policy: 'Preserve raw source; use current English numeric requirements with source Spanish labels', raw: sourceRequirements.es, display: requirements.es }, omittedSameSkill: ['Lupine', 'Ochre', 'Devouring', 'Devotional', 'Aromatic', 'Pious'], retainedLegacy: 'Rattling Sceptre', weightPolicy: old.metadata.weightPolicy })
console.log('Verified Hallowed Skeletal Warrior family: 150 ordinary + 1 special, six locales')
