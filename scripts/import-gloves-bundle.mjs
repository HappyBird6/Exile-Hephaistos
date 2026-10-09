import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'

const evasion = process.argv.includes('--evasion')
const helmets = process.argv.includes('--helmets')
const slot = helmets ? 'Helmets' : 'Gloves'
const proofRoot = helmets ? 'docs/evidence/evasion-helmets-runtime-bundle-2026-10-04' : evasion ? 'docs/evidence/evasion-gloves-runtime-bundle-2026-10-04' : 'docs/evidence/gloves-runtime-bundle-2026-10-04'
fs.mkdirSync(proofRoot, { recursive: true })
const sourceRoot = 'docs/evidence/armour-source-bundle-2026-10-04'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const hash = text => crypto.createHash('sha256').update(text).digest('hex')
const write = (p, value) => fs.writeFileSync(p, JSON.stringify(value, null, 2) + '\n')
const normalize = text => clean(text).replace(/[—–]/g, '-').replace(/\s+/g, '')
const sources = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const candidates = helmets ? [
  { key: 'freebooter', slug: 'Freebooter_Cap', archetype: 'dex', pool: 'freebooter-cap' },
  { key: 'gladiatorial', slug: 'Gladiatorial_Helm', archetype: 'str_dex', pool: 'gladiatorial-helm' },
  { key: 'grinning', slug: 'Grinning_Mask', archetype: 'dex_int', pool: 'grinning-mask' },
] : evasion ? [
  { key: 'polished', slug: 'Polished_Bracers', archetype: 'dex', pool: 'polished-bracers' },
  { key: 'blacksteel-gloves', slug: 'Blacksteel_Gauntlets', archetype: 'str_dex', pool: 'blacksteel-gauntlets' },
  { key: 'war-wraps', slug: 'War_Wraps', archetype: 'dex_int', pool: 'war-wraps' },
] : [
  { key: 'massive', slug: 'Massive_Mitts', archetype: 'str', pool: 'massive-mitts' },
  { key: 'sirenscale', slug: 'Sirenscale_Gloves', archetype: 'int', pool: 'sirenscale-gloves' },
  { key: 'adherent', slug: 'Adherent_Cuffs', archetype: 'str_int', pool: 'adherent-cuffs' },
]
const manifest = read('backend/src/main/resources/catalog/top-bases.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json'), summary = []
const detailsByCode = new Map()
function dataFrom(html) {
  const line = html.split('\n').find(l => l.includes('new ModsView('))
  assert(line)
  return JSON.parse(line.slice(line.indexOf('new ModsView(') + 13, line.lastIndexOf(');')))
}
async function page(archetype, locale) {
  const path = `${proofRoot}/${slot}_${archetype}.${locale}.json`
  if (fs.existsSync(path)) return read(path).data
  const url = `https://poe2db.tw/${sources[locale]}/${slot}_${archetype}`
  const response = await fetch(url)
  assert(response.ok, url)
  const html = await response.text(), all = dataFrom(html)
  const data = { normal: all.normal, essence: all.essence, perfect_essence: all.perfect_essence }
  write(path, { url, retrievedAt: new Date().toISOString(), sha256: hash(html), data })
  return data
}
function fields(html) {
  return Object.fromEntries([...html.matchAll(/<tr><th>([^]*?)<td>([^]*?)(?=<tr>|<\/table>)/g)].map(m => [clean(m[1]), clean(m[2])]))
}
function parseDetail(html) {
  const f = fields(html)
  const stats = [...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => {
    const numbers = clean(m[2]).match(/-?\d+/g).map(Number)
    assert.equal(numbers.length, 2)
    return { id: clean(m[1]).replace(/ /g, '_'), min: numbers[0], max: numbers[1], locality: clean(m[3]) }
  })
  const spawn = [...html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: Number(m[2]) }))
  const effect = clean(html.match(/<h5[^>]*>([^]*?)<\/h5>/)?.[1] ?? '')
  return { fields: f, stats, spawn, effect }
}
async function codeDetail(code) {
  if (detailsByCode.has(code)) return detailsByCode.get(code)
  const path = `${proofRoot}/detail-${code}.json`
  let record
  if (fs.existsSync(path)) record = read(path)
  else {
    const url = `https://poe2db.tw/us/hover?s=${encodeURIComponent('Data\\Mods/' + code)}`
    const response = await fetch(url), html = await response.text()
    assert(response.ok, url)
    record = { code, url, retrievedAt: new Date().toISOString(), sha256: hash(html), html, parsed: parseDetail(html) }
    write(path, record)
  }
  detailsByCode.set(code, record)
  return record
}
function matches(record, row) {
  const p = record.parsed, f = p.fields
  return f.Name === row.Name && Number(f['Req. level']?.match(/^\d+/)?.[0]) === Number(row.Level) && f.Family === row.ModFamilyList.join(', ') && normalize(p.effect) === normalize(row.str)
}
function spans(html) {
  const parts = []; let start, depth = 0
  for (const m of html.matchAll(/<span\b[^>]*>|<\/span>/g)) {
    if (start !== undefined) {
      depth += m[0].startsWith('</') ? -1 : 1
      if (!depth) { parts.push({ start, end: m.index + m[0].length, value: clean(html.slice(start, m.index + m[0].length)) }); start = undefined }
    } else if (/class=['"]mod-value['"]/.test(m[0])) { start = m.index; depth = 1 }
  }
  return parts
}
function rowIdentity(row) {
  return JSON.stringify([row.ModGenerationTypeID, row.ModFamilyList, String(row.Level), row.spawn_no, row.fossil_no, spans(row.str).map(s => normalize(s.value))])
}
function localizedRow(rows, row) {
  const matches = rows.filter(r => rowIdentity(r) === rowIdentity(row))
  assert.equal(matches.length, 1, `Unique locale row required: ${row.Name}/${row.Level}`)
  return matches[0]
}
function template(row) {
  const values = spans(row.str); let html = row.str
  for (const [i, span] of [...values.entries()].reverse()) html = html.slice(0, span.start) + `{v${i}}` + html.slice(span.end)
  return { template: clean(html), name: clean(row.Name) }
}
function ranges(row) {
  return spans(row.str).map(s => { const n = s.value.match(/-?\d+/g).map(Number); return n.length === 1 ? [n[0], n[0]] : n })
}
function idFor(pool, row) { return `${pool}:${row.ModGenerationTypeID === '1' ? 'prefix' : 'suffix'}:${row.Name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/-$/, '')}` }
for (const base of candidates) {
  const prior = read(`${sourceRoot}/${slot}_${base.archetype}.pool.json`)
  const locales = {}
  for (const locale of Object.keys(sources)) locales[locale] = await page(base.archetype, locale)
  const normal = locales.en.normal
  assert.equal(normal.length, prior.normalCount)
  for (let i = 0; i < normal.length; i++) assert.equal(rowIdentity(normal[i]), rowIdentity(prior.rows[i].row), 'Ordinary source pool changed')
  const baseSource = read(`${sourceRoot}/${base.slug}.us.json`)
  const tags = baseSource.fields.Tags.split(', ')
  // Extra base-specific tags must never silently change the archetype's published pool.
  const extra = tags.filter(t => ![`${base.archetype}_armour`, helmets ? 'helmet' : 'gloves', 'armour'].includes(t))
  assert(normal.every(r => extra.every(t => !r.spawn_no.includes(t))))
  const modifiers = [], proofs = [], detailRecords = [], defRows = new Map()
  const currentRoot = `backend/src/main/resources/catalog/${base.pool}`
  const cachedCatalog = fs.existsSync(`${currentRoot}/catalog.json`) ? read(`${currentRoot}/catalog.json`) : null
  const cachedProofs = cachedCatalog ? read(`${currentRoot}/details.raw.json`).proofs : []
  for (let index = 0; index < normal.length; index++) {
    const row = normal[index]
    let priorMatch = prior.rows[index].existing
    if (helmets && !priorMatch) {
      for (const key of ['polished', 'blacksteel-gloves', 'war-wraps']) {
        const pool = manifest[key].pool
        const shared = read(`backend/src/main/resources/catalog/${pool}/catalog.json`).modifiers.filter(d => d.name === row.Name && d.familyIds.join(',') === row.ModFamilyList.join(',') && d.requiredItemLevel === +row.Level && d.affixType === (row.ModGenerationTypeID === '1' ? 'PREFIX' : 'SUFFIX') && normalize(d.text) === normalize(row.str) && JSON.stringify(d.tags) === JSON.stringify(row.fossil_no))
        if (shared.length === 1) { priorMatch = { pool, definition: shared[0] }; break }
      }
    }
    let definition, proof
    const cachedProof = cachedProofs.find(p => p.row.Name === row.Name && rowIdentity(p.row) === rowIdentity(row) && normalize(p.row.str) === normalize(row.str))
    if (cachedProof) {
      definition = structuredClone(cachedCatalog.modifiers.find(d => d.id === cachedProof.id))
      assert(definition, `Cached definition required: ${cachedProof.id}`)
      proof = cachedProof.proof
    } else if (priorMatch) {
      definition = structuredClone(priorMatch.definition)
      proof = { method: 'EXACT_EXISTING_FAMILY_LEVEL_GENERATION_EFFECT_MATCH', originalPool: priorMatch.pool, originalId: definition.id }
    } else {
      let detail
      const family = row.ModFamilyList[0], effect = clean(row.str)
      let prefix
      const defence = effect.includes('Evasion')
        ? effect.includes('Armour') ? 'ArmourAndEvasion' : effect.includes('Energy Shield') ? 'EvasionAndEnergyShield' : 'EvasionRating'
        : effect.includes('Armour') ? 'ArmourAndEnergyShield' : 'EnergyShield'
      if (family === 'BaseLocalDefences') prefix = `LocalIncreased${defence}`
      else if (family === 'DefencesPercent') prefix = `LocalIncreased${defence}${['EvasionRating', 'EnergyShield'].includes(defence) ? 'Percent' : ''}`
      else if (family === 'BaseLocalDefencesAndLife') prefix = `LocalIncreased${defence === 'EvasionRating' ? 'Evasion' : defence}AndLife`
      else if (family === 'EnergyShieldRegeneration') prefix = 'EnergyShieldRechargeRate'
      else if (family === 'EvasionAppliesToDeflection') prefix = 'EvasionAppliesToDeflection'
      if (prefix && !evasion && !helmets) {
        const siblings = normal.filter(r => r.ModFamilyList[0] === family && (family !== 'BaseLocalDefences' || !clean(r.str).includes('Armour'))).sort((a, b) => +a.Level - +b.Level)
        const ordinal = siblings.indexOf(row) + 1
        const numbers = family === 'EnergyShieldRegeneration' ? [4, 3, 2, 1, 5, 6, 7, 8] : [ordinal]
        for (const n of numbers) {
          for (const suffix of ['', '_', '__']) {
            const candidate = await codeDetail(`${prefix}${n}${suffix}`)
            if (matches(candidate, row)) { detail = candidate; break }
          }
          if (detail) break
        }
      }
      let stats
      if (!detail && (evasion || helmets)) {
        const url = row.hover.replace('https://cdn.poe2db.tw/', 'https://poe2db.tw/')
        const sourceId = row.hover.split('/').at(-1)
        const path = `${proofRoot}/detail-source-${sourceId}.json`
        let candidate
        if (fs.existsSync(path)) candidate = read(path)
        else {
          const response = await fetch(url), html = await response.text()
          assert(response.ok, url)
          const parsed = parseDetail(html)
          candidate = { code: parsed.fields.Code ?? null, sourceId, url, retrievedAt: new Date().toISOString(), sha256: hash(html), html, parsed }
          write(path, candidate)
        }
        assert(matches(candidate, row), `Exact cached source detail required: ${row.Name}`)
        detail = candidate
      }
      if (detail) {
        assert(detail.parsed.stats.length)
        assert.deepEqual(detail.parsed.spawn.map(s => s.tag), row.spawn_no, `${row.Name}: ordered spawn changed`)
        const first = detail.parsed.spawn.find(s => [...tags, 'default'].includes(s.tag))
        assert(first?.weight > 0, `${row.Name}: not eligible`)
        stats = detail.parsed.stats.map(({ locality, ...stat }) => stat)
        proof = { method: detail.code ? 'EXACT_PUBLIC_CODE_DETAIL' : 'EXACT_PUBLIC_SOURCE_ID_DETAIL', code: detail.code, sourceId: detail.sourceId, url: detail.url, sha256: detail.sha256, parsed: detail.parsed }
        detailRecords.push(detail)
      } else if (family === 'BaseLocalDefences' && ranges(row).length === 2) {
        // Published hybrid flat defence combines two separately source-verified local units.
        const armour = read('backend/src/main/resources/catalog/stocky-mitts/catalog.json').modifiers.find(d => d.familyIds.includes('BaseLocalDefences') && d.name === 'Lacquered')
        const es = await codeDetail('LocalIncreasedEnergyShield1')
        assert.equal(es.parsed.stats[0].id, 'local_energy_shield')
        assert.equal(es.parsed.stats[0].locality, 'Local')
        assert.equal(armour.stats[0].id, 'local_base_physical_damage_reduction_rating')
        const ev = effect.includes('Evasion') ? await codeDetail('LocalIncreasedEvasionRating1') : null
        if (ev) { assert.equal(ev.parsed.stats[0].id, 'local_base_evasion_rating'); assert.equal(ev.parsed.stats[0].locality, 'Local') }
        const rs = ranges(row); assert.equal(rs.length, 2)
        const units = effect.includes('Armour') ? [armour.stats[0].id, ev ? ev.parsed.stats[0].id : es.parsed.stats[0].id] : [ev.parsed.stats[0].id, es.parsed.stats[0].id]
        stats = units.map((id, i) => ({ id, min: rs[i][0], max: rs[i][1] }))
        proof = { method: 'PUBLISHED_HYBRID_FLAT_DEFENCE_EXACT_LOCAL_UNITS_AND_RANGES', armourSource: armour.sourceUrl, energyShieldCode: es.code, energyShieldSource: es.url, evasionCode: ev?.code, evasionSource: ev?.url, rowSource: row.hover }
      } else throw new Error(`Unresolved ${base.key}: ${row.Name}/${row.Level}/${effect}`)
      const siblingLevels = normal.filter(r => r.ModFamilyList[0] === family).map(r => +r.Level)
      const tier = [...new Set(siblingLevels)].filter(l => l > +row.Level).length + 1
      definition = { id: idFor(base.pool, row), name: row.Name, layer: 'EXPLICIT', affixType: row.ModGenerationTypeID === '1' ? 'PREFIX' : 'SUFFIX', familyIds: row.ModFamilyList, requiredItemLevel: +row.Level, weight: 1, tier, text: clean(row.str), stats, tags: row.fossil_no, sourceUrl: detail?.url ?? row.hover }
    }
    assert.equal(definition.name, row.Name, 'Stable source affix name required')
    assert.equal(definition.requiredItemLevel, +row.Level, 'Stable source level required')
    assert.equal(definition.affixType, row.ModGenerationTypeID === '1' ? 'PREFIX' : 'SUFFIX')
    assert.deepEqual([...definition.familyIds].sort(), [...row.ModFamilyList].sort())
    assert.deepEqual([...definition.tags].sort(), [...row.fossil_no].sort())
    assert.equal(normalize(definition.text), normalize(row.str), 'Complete source effect required')
    // New ordinary snapshots explicitly model 1/N; historical source weights are untouched.
    definition.weight = 1
    modifiers.push(definition); proofs.push({ id: definition.id, row, proof }); defRows.set(definition.id, row)
    if (!display.definitions[definition.id]) {
      const values = spans(row.str).map(s => s.value), key = `gloves.${hash(rowIdentity(row)).slice(0, 20)}`
      const rs = ranges(row), valueStats = []
      for (const bounds of rs) {
        const matches = definition.stats.filter(s => s.min === bounds[0] && s.max === bounds[1] && !valueStats.some(v => v.id === s.id))
        assert.equal(matches.length, 1, `Ambiguous display stat order: ${row.Name}`)
        valueStats.push({ id: matches[0].id, divisor: 1 })
      }
      display.definitions[definition.id] = { stats: definition.stats, englishText: definition.text, values, template: key, sourceCode: proof.code ?? null, valueStats }
      for (const locale of Object.keys(sources)) display.templates[locale][key] = template(localizedRow(locales[locale].normal, row))
    }
  }
  // Class-restricted special outcomes are reused only if this exact published class table contains them.
  const special = []
  for (const name of helmets ? ['perfect-essences'] : ['abyss-essence', 'perfect-grounding-opulence']) {
    const existing = read(`backend/src/main/resources/catalog/${helmets ? 'rusted-greathelm' : 'stocky-mitts'}/${name}.catalog.json`)
    for (const d of existing.modifiers) {
      const code = new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
      const matches = locales.en.perfect_essence.filter(r => r.Code === code && r.ModFamilyList.join(',') === d.familyIds.join(',') && +r.Level === d.requiredItemLevel && +r.ModGenerationTypeID === (d.affixType === 'PREFIX' ? 1 : 2))
      assert(matches.length, `Missing ${base.key} special class source: ${d.id}`)
      const detail = await codeDetail(code)
      assert.equal(normalize(detail.parsed.effect), normalize(d.text), `${code}: changed special source detail`)
      modifiers.push(d); special.push({ id: d.id, sourceRows: matches })
    }
  }
  const poolRoot = `backend/src/main/resources/catalog/${base.pool}`
  fs.mkdirSync(poolRoot, { recursive: true })
  const rawText = JSON.stringify(normal, null, 2) + '\n', detailText = JSON.stringify({ proofs, special }, null, 2) + '\n'
  fs.writeFileSync(`${poolRoot}/base.raw.json`, rawText); fs.writeFileSync(`${poolRoot}/details.raw.json`, detailText)
  const prefixes = modifiers.filter(d => d.affixType === 'PREFIX'), suffixes = modifiers.filter(d => d.affixType === 'SUFFIX')
  const metadata = { snapshotId: `poe2db-${base.pool}-uniform-20261004-${hash(rawText + detailText).slice(0, 16)}`, retrievedAt: new Date().toISOString(), sourceUrl: `https://poe2db.tw/us/${slot}_${base.archetype}`, weightPolicy: 'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES', rawSha256: hash(rawText), detailsSha256: hash(detailText), prefixCount: prefixes.length, suffixCount: suffixes.length, prefixWeight: prefixes.reduce((n, d) => n + d.weight, 0), suffixWeight: suffixes.reduce((n, d) => n + d.weight, 0) }
  // A cached re-import must not pretend the sources were fetched again.
  metadata.retrievedAt = fs.existsSync(`${poolRoot}/catalog.json`)
    ? read(`${poolRoot}/catalog.json`).metadata.retrievedAt
    : read(`${proofRoot}/${slot}_${base.archetype}.en.json`).retrievedAt
  write(`${poolRoot}/catalog.json`, { metadata, base: { id: baseSource.fields.Type, name: baseSource.name, sourceUrl: baseSource.url, implicitModifierId: '', magicPrefixes: 1, magicSuffixes: 1, rarePrefixes: 3, rareSuffixes: 3 }, modifiers })
  const requirements = {}
  for (const [locale, sourceLocale] of Object.entries(sources)) {
    const source = read(`${sourceRoot}/${base.slug}.${sourceLocale}.json`)
    terms[locale][base.slug] = { name: source.name, lines: [], itemKey: baseSource.fields.Type, sourceUrl: source.url }
    requirements[locale] = source.requirements
  }
  const sourceCard = baseSource.card
  manifest[base.key] = { ...base, family: 'gloves', id: baseSource.fields.Type, name: baseSource.name, sourceUrl: baseSource.url, sourceSha256: baseSource.sha256, sourceTags: tags, armour: +(sourceCard.match(/Armour: (\d+)/)?.[1] ?? 0), energyShield: +(sourceCard.match(/Energy Shield: (\d+)/)?.[1] ?? 0), requiredLevel: +sourceCard.match(/Level (\d+)/)[1], strength: +(sourceCard.match(/(\d+) Str/)?.[1] ?? 0), intelligence: +(sourceCard.match(/(\d+) Int/)?.[1] ?? 0), maximumQuality: 20, requirements }
  if (evasion || helmets) { manifest[base.key].evasion = +(sourceCard.match(/Evasion Rating: (\d+)/)?.[1] ?? 0); manifest[base.key].dexterity = +(sourceCard.match(/(\d+) Dex/)?.[1] ?? 0) }
  if (helmets) manifest[base.key].family = 'helmets'
  const fixed = {}, replacements = { ESSENCE_HYSTERIA: [] }
  for (const row of locales.en.essence) {
    const slug = row.Name.match(/href="([^"]+)"/)?.[1]
    const action = slug?.replace(/^Lesser_Essence_of_(?:the_)?/, 'LESSER_ESSENCE_').replace(/^Greater_Essence_of_(?:the_)?/, 'GREATER_ESSENCE_').replace(/^Essence_of_(?:the_)?/, 'ESSENCE_').toUpperCase()
    if (!action || row.IsAlloy || row.Removes) continue
    const detail = await codeDetail(row.Code)
    const targets = normal.filter(r => matches(detail, r))
    if (targets.length !== 1) continue // Other archetype alternatives share one published Essence row.
    const d = modifiers.find(d => defRows.get(d.id) === targets[0]); assert(d)
    fixed[action] ??= []
    if (!fixed[action].includes(d.id)) fixed[action].push(d.id)
  }
  if (!helmets) {
  const hysteria = locales.en.perfect_essence.find(r => r.Code === 'CriticalMultiplier4')
  assert(hysteria)
  const hysteriaDetail = await codeDetail(hysteria.Code)
  replacements.ESSENCE_HYSTERIA = modifiers.filter(d => d.weight && normalize(d.text) === normalize(hysteriaDetail.parsed.effect) && d.requiredItemLevel === +hysteria.Level).map(d => d.id)
  assert.equal(replacements.ESSENCE_HYSTERIA.length, 1)
  replacements.ESSENCE_ABYSS = modifiers.filter(d => d.weight === 0 && d.familyIds.includes('EssenceAbyss')).map(d => d.id)
  assert.equal(replacements.ESSENCE_ABYSS.length, 2)
  } else {
    delete replacements.ESSENCE_HYSTERIA
    for (const entry of special) {
      const slug = entry.sourceRows[0].Name.match(/href="([^"]+)"/)?.[1]
      assert(slug?.startsWith('Perfect_Essence_of_'), `Published Perfect Essence action required: ${entry.id}`)
      const action = slug.replace(/^Perfect_Essence_of_(?:the_)?/, 'PERFECT_ESSENCE_').toUpperCase()
      replacements[action] = [entry.id]
    }
  }
  overrides[base.key] = { fixed, replacements }
  summary.push({ key: base.key, ordinary: normal.length, special: special.length, newMapped: proofs.filter(p => p.proof.method !== 'EXACT_EXISTING_FAMILY_LEVEL_GENERATION_EFFECT_MATCH').length, fixedEssences: Object.keys(fixed) })
  console.log(base.key, summary.at(-1))
}
write('backend/src/main/resources/catalog/top-bases.json', manifest)
write('frontend/src/features/crafting/topBases.json', manifest)
write('backend/src/main/resources/catalog/top-base-essences.json', overrides)
write('frontend/src/features/crafting/topBaseEssences.json', overrides)
write('frontend/src/shared/i18n/gameTerms.json', terms)
write('frontend/src/shared/i18n/modifierTemplates.json', display)
write(`${proofRoot}/import-summary.json`, summary)
