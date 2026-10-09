import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/wands-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const reviewed = read(`${root}/applicability.json`)
const old = read('backend/src/main/resources/catalog/attuned-wand/catalog.json')
const raw = read('backend/src/main/resources/catalog/attuned-wand/base.raw.json')
const details = read('backend/src/main/resources/catalog/attuned-wand/details.raw.json')
const special = read('backend/src/main/resources/catalog/attuned-wand/perfect-essences.catalog.json')
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const templates = read('frontend/src/shared/i18n/modifierTemplates.json')
const pools = read(`${root}/Wands.us.json`).data
for (const b of reviewed.bases) {
  const key = b.name.toLowerCase(), slug = `${b.name}_Wand`, pool = `${key}-wand`
  assert(!bases[key], 'Preserve prior registrations')
  const source = read(`${root}/${slug}.us.json`)
  const ids = new Set(b.eligible.map(e => e.id))
  const ordinary = old.modifiers.filter(d => ids.has(d.id))
  const modifiers = [...ordinary, ...special.modifiers]
  const eligibleDetails = details.filter(d => ordinary.some(m => m.sourceUrl === d.url))
  const rawText = JSON.stringify(eligibleDetails.map(d => d.row), null, 2) + '\n'
  const detailText = JSON.stringify({ ordinary: eligibleDetails, base: source, special: read('backend/src/main/resources/catalog/attuned-wand/perfect-essences.raw.json') }, null, 2) + '\n'
  const destination = `backend/src/main/resources/catalog/${pool}`
  const preserved = fs.existsSync(destination)
  if (!preserved) fs.mkdirSync(destination)
  for (const [name, text] of [['base.raw.json', rawText], ['details.raw.json', detailText]]) {
    if (preserved) assert.equal(fs.readFileSync(`${destination}/${name}`, 'utf8'), text, 'Preserve existing source')
    else fs.writeFileSync(`${destination}/${name}`, text)
  }
  const count = type => modifiers.filter(d => d.affixType === type).length
  const weight = type => modifiers.filter(d => d.affixType === type).reduce((n, d) => n + d.weight, 0)
  const catalog = { metadata: { ...old.metadata, snapshotId: `poe2db-${pool}-20261005-${hash(rawText + detailText).slice(0,16)}`, retrievedAt: source.retrievedAt, rawSha256: hash(rawText), detailsSha256: hash(detailText), prefixCount: count('PREFIX'), suffixCount: count('SUFFIX'), prefixWeight: weight('PREFIX'), suffixWeight: weight('SUFFIX') }, base: { ...old.base, id: b.id, name: source.name, sourceUrl: source.url }, modifiers }
  if (preserved) assert.deepEqual(read(`${destination}/catalog.json`), catalog, 'Preserve existing catalog')
  else write(`${destination}/catalog.json`, catalog)
  const requirements = {}, sourceProperties = {}
  for (const [locale, remote] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
    const s = read(`${root}/${slug}.${remote}.json`)
    const line = s.card.split('\n').map(l => l.trim()).find(l => l.includes(s.requirements))
    const skill = line.slice(line.indexOf(s.requirements) + s.requirements.length).trim()
    assert(skill)
    sourceProperties[locale] = [skill]
    const discrepancy = reviewed.localeDiscrepancies.find(d => d.name === b.name && d.locale === remote)
    requirements[locale] = discrepancy ? (b.name === 'Bone' ? 'Requiere:  Nivel 2' : 'Requiere:  Nivel 38, 68 Int') : s.requirements
    assert(!discrepancy || remote === 'sp', 'Explicit reconciliation required')
    terms[locale][slug] = { name: s.name, lines: [skill], itemKey: b.id, sourceUrl: s.url }
    const rows = read(`${root}/Wands.${remote}.json`).data.normal
    assert.equal(rows.length, raw.length)
    for (let i = 0; i < raw.length; i++) {
      assert.deepEqual(rows[i].ModFamilyList, raw[i].ModFamilyList)
      const definition = old.modifiers.find(d => d.sourceUrl === details[i].url)
      const binding = templates.definitions[definition.id]
      assert.equal(templates.templates[locale][binding.template].template.replace(/\{v(\d+)\}/g, (_, n) => binding.values[+n]).replaceAll('\n',''), clean(rows[i].str).replaceAll('\n',''))
    }
  }
  bases[key] = { key, slug, pool, family: 'wands', id: b.id, name: source.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: b.tags, armour: 0, strength: 0, intelligence: +(source.requirements.match(/(\d+) Int/)?.[1] ?? 0), requiredLevel: +source.fields.DropLevel, maximumQuality: 20, requirements, sourceProperties }
  overrides[key] = { fixed: {}, replacements: { PERFECT_ESSENCE_SORCERY: [special.modifiers[0].id], PERFECT_ESSENCE_ALACRITY: [special.modifiers[1].id] } }
  for (const row of pools.essence) {
    const action = row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_', '_').replaceAll('_of_', '_').toUpperCase()
    if (action.endsWith('_INFINITE')) continue
    const d = ordinary.find(d => d.sourceUrl.endsWith('Mods%2F' + row.Code))
    assert(d && d.requiredItemLevel === +row.Level && d.text === clean(row.str))
    ;(overrides[key].fixed[action] ??= []).push(d.id)
  }
  registry.workbenchBases[key] = { ...registry.workbenchBases.wand, baseItemId: b.id, source: source.url, sourceSha256: source.sha256, requiredCharacterLevel: bases[key].requiredLevel }
  for (const entry of registry.entries) if (entry.serviceScope !== 'DEFERRED' && entry.supportedBases?.includes('wand')) entry.supportedBases.push(key)
}
for (const p of ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json']) write(p, bases)
for (const p of ['backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) write(p, overrides)
write('frontend/src/shared/i18n/gameTerms.json', terms)
write('backend/src/main/resources/crafting/registry-v2.json', registry)
console.log('Imported nine tag-filtered ordinary Wand families with stable modifier IDs')
