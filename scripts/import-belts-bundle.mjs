import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/belts-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const roster = ['Linen', 'Wide', 'Long', 'Plate', 'Ornate', 'Mail', 'Double', 'Heavy', 'Utility', 'Fine', 'Invoking', 'Sinew', 'Forking']
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const old = read('backend/src/main/resources/catalog/rawhide-belt/catalog.json')
const raw = read('backend/src/main/resources/catalog/rawhide-belt/base.raw.json')
const details = read('backend/src/main/resources/catalog/rawhide-belt/details.raw.json')
const sourcePool = read(`${root}/Belts.us.json`).data
assert.deepEqual(sourcePool.normal, raw, 'Review changed complete Belt pool')
const code = d => new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
const action = row => row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_', '_').replaceAll('_of_', '_').toUpperCase()
const fixed = {}, replacements = {}
for (const row of sourcePool.essence) {
  const d = old.modifiers.find(d => code(d) === row.Code)
  if (!d) {
    assert(action(row).endsWith('_INFINITE') && ['Dexterity', 'Intelligence'].includes(row.ModFamilyList[0]) && !row.spawn_no.includes('belt'), `Unreviewed fixed target ${row.Code}`)
    continue
  }
  assert(d && d.requiredItemLevel === +row.Level)
  assert.deepEqual(d.familyIds, row.ModFamilyList)
  ;(fixed[action(row)] ??= []).push(d.id)
}
const perfect = read('backend/src/main/resources/catalog/rawhide-belt/perfect-essences.catalog.json').modifiers
const abyss = read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.catalog.json').modifiers
const original = old.modifiers.find(d => code(d) === 'StunThreshold10')
const hysteria = { ...original, id: 'belt:suffix:essence-hysteria-stun-threshold', weight: 0, tier: 1 }
display.definitions[hysteria.id] = structuredClone(display.definitions[original.id])
const special = [...perfect, ...abyss, hysteria]
for (const d of special) {
  const row = sourcePool.perfect_essence.find(row => row.Code === code(d))
  assert(row && +row.Level === d.requiredItemLevel)
  assert.deepEqual(row.ModFamilyList, d.familyIds)
  const published = code(d).startsWith('EssenceAbyss') ? read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.raw.json').find(p => p.row.Code === code(d)).row.str : d.text
  assert.equal(clean(row.str).replaceAll('\n', ''), clean(published).replaceAll('\n', ''))
  ;(replacements[action(row)] ??= []).push(d.id)
}
const parse = html => [...html.matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].filter(m => m[0].includes('<tr><th>Family')).map(m => ({
  text: clean(m[1]), family: clean(m[0].match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)[1]),
  tags: [...(m[0].match(/<tr><th>Craft Tags<td>([^]*?)(?=<\/table>)/)?.[1] ?? '').matchAll(/<span class='badge bg-primary'>([^]*?)<\/span>/g)].map(m => clean(m[1]).toLowerCase()),
  stats: [...m[0].matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => { const n = clean(m[2]).match(/-?\d+/g).map(Number); return { id: clean(m[1]).replaceAll(' ', '_'), min: n[0], max: n[1], locality: clean(m[3]) } }),
}))
const report = { bases: [], excluded: ['Golden Obi: Demigod provenance', 'Stalking: unsupported socket bonus transfer', 'Runemastered Heavy: Unique provenance and deferred Alloy mechanics'], catalystSupported: false, qualityMaximum: null, charmSlotProperty: 'Source range 1–3 preserved as display property; no rolled slot state or slot probabilities inferred', localeDiscrepancies: [] }
for (const name of roster) {
  const key = `${name.toLowerCase()}-belt`, slug = `${name}_Belt`, pool = key
  assert(!bases[key])
  const source = read(`${root}/${slug}.us.json`), b = source.variants[0]
  assert.equal(source.variants.length, 1)
  assert.equal(b.fields.Class, 'Belts'); assert.equal(b.fields.Tags, 'belt')
  assert(!b.fields.Type.includes('Unique'))
  const proof = parse(fs.readFileSync(`${root}/${slug}.us.html`, 'utf8'))
  assert.equal(proof.length, ['Invoking', 'Sinew', 'Forking'].includes(name) ? 2 : 1)
  const stats = proof.flatMap(p => p.stats.map(({ locality, ...s }) => s))
  assert(stats.length && new Set(stats.map(s => s.id)).size === stats.length)
  const implicitId = `${key}:implicit:base`, text = proof.map(p => p.text).join('\n')
  const implicit = { id: implicitId, name: b.name + ' implicit', layer: 'IMPLICIT', affixType: 'NONE', familyIds: proof.map(p => p.family), requiredItemLevel: 1, weight: 0, tier: 0, text, stats, tags: [...new Set(proof.flatMap(p => p.tags))], sourceUrl: source.url }
  const eligible = details.filter(d => {
    const spawn = [...d.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
    const first = spawn.find(s => ['belt', 'default'].includes(s.tag)); assert(first, d.code)
    const definition = old.modifiers.find(m => code(m) === d.code)
    assert(definition && definition.weight === +d.row.DropChance)
    return first.weight > 0
  })
  assert.equal(eligible.length, raw.length)
  const modifiers = [implicit, ...old.modifiers, ...special]
  const templateKey = `belts.${key}.implicit`
  const values = [...text.matchAll(/[+]?(?:\(-?\d+[—?]-?\d+\)|\d+(?:\.\d+)?)/g)].map(m => m[0])
  const ordered = name === 'Forking' ? [stats[1], stats[0], stats[2]] : stats
  assert.equal(values.length, ordered.length)
  display.definitions[implicitId] = { stats, englishText: text, values, template: templateKey, sourceCode: null, valueStats: ordered.map(s => ({ id: s.id, divisor: name === 'Fine' ? 60 : s.max < 0 ? -1 : 1 })) }
  if (name === 'Fine') delete display.definitions[implicitId].valueStats // Fixed source text retains published 0.17; canonical stat remains 10/minute.
  const requirements = {}, sourceProperties = {}
  for (const [locale, remote] of Object.entries({ en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' })) {
    const localSource = read(`${root}/${slug}.${remote}.json`), local = localSource.variants[0]
    assert.equal(local.fields.Type, b.fields.Type)
    const localProof = parse(fs.readFileSync(`${root}/${slug}.${remote}.html`, 'utf8'))
    assert.deepEqual(localProof.map(p => p.stats.map(({ locality, ...s }) => s)), proof.map(p => p.stats.map(({ locality, ...s }) => s)), 'Locale stat IDs and canonical ranges must agree; locality labels are translated')
    const translated = localProof.map(p => p.text).join('\n')
    const localValues = [...translated.matchAll(/[+]?(?:\(-?\d+[—?]-?\d+\)|\d+(?:\.\d+)?)/g)].map(m => m[0])
    assert.deepEqual(name === 'Fine' ? localValues.filter(v => v !== '1') : localValues, values, `${key}/${locale} exact source numbers`)
    let template = translated
    let valueIndex = 0
    template = template.replace(/[+]?(?:\(-?\d+[—–−]-?\d+\)|\d+(?:\.\d+)?)/g, value => name === 'Fine' && value === '1' ? value : `{v${valueIndex++}}`)
    assert.equal(valueIndex, values.length)
    assert.equal(template.replace(/\{v(\d+)\}/g, (_, n) => values[+n]), translated)
    display.templates[locale][templateKey] = { name: local.name, template }
    requirements[locale] = local.requirements
    const popup = fs.readFileSync(`${root}/${slug}.${remote}.html`, 'utf8').match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)[0]
    const slots = [...popup.matchAll(/<div class="implicitMod">([^]*?)<\/div>/g)].map(m => clean(m[1])).find(s => /1[—?]3/.test(s))
    assert(slots, `${key}/${locale} preserve source slot range`)
    sourceProperties[locale] = [slots]
    terms[locale][slug] = { name: local.name, lines: [slots], itemKey: b.fields.Type, sourceUrl: localSource.url }
    const rows = read(`${root}/Belts.${remote}.json`).data.normal
    assert.equal(rows.length, raw.length)
    for (let i = 0; i < raw.length; i++) {
      assert.deepEqual(rows[i].ModFamilyList, raw[i].ModFamilyList)
      const binding = display.definitions[old.modifiers.find(m => code(m) === details[i].code).id]
      assert.equal(display.templates[locale][binding.template].template.replace(/\{v(\d+)\}/g, (_, n) => binding.values[+n]).replaceAll('\n', ''), clean(rows[i].str).replaceAll('\n', ''))
    }
  }
  const destination = `backend/src/main/resources/catalog/${pool}`
  if (!fs.existsSync(destination)) fs.mkdirSync(destination)
  const rawText = JSON.stringify(raw, null, 2) + '\n', detailText = JSON.stringify({ ordinary: eligible, implicit: proof, special: sourcePool.perfect_essence }, null, 2) + '\n'
  const preserve = (path, content) => { if (fs.existsSync(path)) assert.equal(fs.readFileSync(path, 'utf8'), content, 'Preserve generated evidence from earlier interrupted import'); else fs.writeFileSync(path, content, { flag: 'wx' }) }
  preserve(`${destination}/base.raw.json`, rawText); preserve(`${destination}/details.raw.json`, detailText)
  const affixes = type => modifiers.filter(m => m.affixType === type)
  preserve(`${destination}/catalog.json`, JSON.stringify({ metadata: { ...old.metadata, snapshotId: `poe2db-${pool}-20261005-${hash(rawText + detailText).slice(0,16)}`, retrievedAt: source.retrievedAt, rawSha256: hash(rawText), detailsSha256: hash(detailText), prefixCount: affixes('PREFIX').length, suffixCount: affixes('SUFFIX').length, prefixWeight: affixes('PREFIX').reduce((n,m)=>n+m.weight,0), suffixWeight: affixes('SUFFIX').reduce((n,m)=>n+m.weight,0) }, base: { ...old.base, id: b.fields.Type, name: b.name, sourceUrl: source.url, implicitModifierId: implicitId }, modifiers }, null, 2) + '\n')
  const requiredLevel = +(b.requirements.match(/Level (\d+)/)?.[1] ?? 0)
  bases[key] = { key, slug, pool, family: 'belts', id: b.fields.Type, name: b.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: ['belt'], armour: 0, strength: 0, requiredLevel, maximumQuality: 0, requirements, sourceProperties, implicitModifierId: implicitId, implicitStats: stats, implicitLocalities: proof.flatMap(p => p.stats) }
  overrides[key] = { fixed, replacements }
  registry.workbenchBases[key] = { ...registry.workbenchBases.belt, baseItemId: b.fields.Type, source: source.url, sourceSha256: source.sha256, projection: 'EXPLICIT_AFFIX_WITH_SOURCE_VARIABLE_IMPLICIT', variableImplicit: implicitId, charmSlots: 'SOURCE_PROPERTY_RANGE_WITH_FIXED_BREACH_STAT_PRESERVED', requiredCharacterLevel: requiredLevel }
  for (const e of registry.entries) {
    if (e.serviceScope === 'DEFERRED') continue
    const a = e.action ?? e.workbenchAction
    if (e.category === 'ESSENCE' ? !fixed[a] && !replacements[a] : !e.supportedBases?.includes('belt') && !['Divine_Orb', 'Omen_of_the_Blessed'].includes(e.id)) continue
    if (!e.supportedBases.includes(key)) e.supportedBases.push(key)
  }
  report.bases.push({ key, id: b.fields.Type, ordinary: eligible.length, special: special.length, implicit: proof, fixedActions: Object.keys(fixed).length, replacementActions: Object.keys(replacements).length, weightPolicy: old.metadata.weightPolicy })
}
for (const p of ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json']) write(p, bases)
for (const p of ['backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) write(p, overrides)
write('backend/src/main/resources/crafting/registry-v2.json', registry)
write('frontend/src/shared/i18n/gameTerms.json', terms)
write('frontend/src/shared/i18n/modifierTemplates.json', display)
write(`${root}/verification.json`, report)
console.log('Imported thirteen Belt identities with complete source pool and six locales')
