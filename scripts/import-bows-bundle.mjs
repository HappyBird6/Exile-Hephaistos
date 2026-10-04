import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/bows-source-bundle-2026-10-04'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const locales = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const manifest = read('backend/src/main/resources/catalog/top-bases.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const old = read('backend/src/main/resources/catalog/crude-bow/catalog.json')
const raw = read('backend/src/main/resources/catalog/crude-bow/base.raw.json')
const details = read('backend/src/main/resources/catalog/crude-bow/details.raw.json')
const ordinary = read(`${root}/Bows.en.json`).data
assert.deepEqual(ordinary.normal, raw, 'Complete Bow ordinary source pool must match the preserved catalog')
const perfect = read('backend/src/main/resources/catalog/crude-bow/perfect-essences.catalog.json').modifiers
const abyss = read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.catalog.json').modifiers
const fixed = Object.fromEntries([...fs.readFileSync('backend/src/main/java/com/poe2craft/crafting/domain/BowEssenceTargets.java','utf8').matchAll(/WorkbenchCurrency\.(\w+),\s*List.of\("([^"]+)"\)/g)].map(m => [m[1], [m[2]]]))
assert.equal(Object.keys(fixed).length, 21)
const replacements = {}
for (const d of [...perfect, ...abyss]) {
  const code = new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
  const row = ordinary.perfect_essence.find(r => r.Code === code && +r.Level === d.requiredItemLevel && r.ModFamilyList.join(',') === d.familyIds.join(','))
  assert(row, `Exact Bow special source required: ${d.id}`)
  if (code.startsWith('EssenceAbyss')) {
    const cached = read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.raw.json').find(r => r.row.Code === code)
    assert(cached, 'Exact previously verified Abyss detail required')
    assert.equal(clean(row.str), clean(cached.row.str), 'Published abbreviated Mark must retain its verified full detail')
  } else assert.equal(clean(row.str), d.text)
  const slug = row.Name.match(/href="([^"]+)"/)[1]
  const action = slug === 'Essence_of_the_Abyss' ? 'ESSENCE_ABYSS' : slug.replace('Perfect_Essence_of_', 'PERFECT_ESSENCE_').toUpperCase()
  ;(replacements[action] ??= []).push(d.id)
}
for (const [action, ids] of Object.entries(fixed)) {
  const d = old.modifiers.find(d => d.id === ids[0])
  const code = new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
  assert(ordinary.essence.some(r => r.Code === code && +r.Level === d.requiredItemLevel && r.ModFamilyList.join(',') === d.familyIds.join(',')), action)
}
const labels = {
  en: ['Physical Damage', 'Critical Hit Chance', 'Attacks per Second'],
  ko: ['물리 피해', '치명타 명중 확률', '초당 공격 횟수'],
  ja: ['物理ダメージ', 'クリティカルヒット率', '秒間アタック回数'],
  'zh-CN': ['物理伤害', '暴击率', '每秒攻击次数'],
  'zh-TW': ['物理傷害', '暴擊率', '每秒攻擊次數'],
  es: ['Daño físico', 'Probabilidad de impacto crítico', 'Ataques por segundo'],
}
const families = { guardian: 'Chain', gemini: 'AdditionalArrows', fanatic: 'WeaponImplicitDamageType', obliterator: 'ProjectileRange' }
const summary = []
for (const key of ['warmonger', 'guardian', 'gemini', 'fanatic', 'obliterator']) {
  const slug = `${key[0].toUpperCase()}${key.slice(1)}_Bow`, source = read(`${root}/${slug}.us.json`)
  assert.equal(source.fields.Class, 'Bows')
  assert(source.fields.Type.endsWith('Endgame'))
  const tags = source.fields.Tags.split(', ')
  for (const detail of details) {
    const spawn = [...detail.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
    const first = spawn.find(s => [...tags, 'default'].includes(s.tag))
    assert(first?.weight > 0, `Base-specific spawn required: ${key}/${detail.code}`)
  }
  const implicitId = families[key] ? `${key}-bow:implicit:${families[key].toLowerCase()}` : ''
  const modifiers = [...old.modifiers, ...perfect, ...abyss]
  const requirements = {}, sourceProperties = {}
  let implicit
  if (implicitId) {
    const proof = read(`${root}/${slug}.implicit.json`)
    const text = source.card.slice(source.card.indexOf(source.requirements) + source.requirements.length).trim()
    implicit = { id: implicitId, name: source.name, layer: 'IMPLICIT', affixType: 'NONE', familyIds: [families[key]], requiredItemLevel: 1, weight: 0, tier: 0, text, stats: proof.stats.map(({ locality, ...s }) => s), tags: key === 'gemini' ? ['attack'] : [], sourceUrl: source.url }
    modifiers.push(implicit)
    const templateKey = `bows.${key}.implicit`
    const values = key === 'guardian' ? ['(25—35)'] : key === 'gemini' ? ['+50'] : key === 'obliterator' ? ['50'] : ['64', '28']
    display.definitions[implicitId] = { stats: implicit.stats, englishText: text, values, template: templateKey, sourceCode: null, valueStats: implicit.stats.map(s => ({ id: s.id, divisor: key === 'obliterator' ? -1 : 1 })) }
    for (const [locale, sourceLocale] of Object.entries(locales)) {
      const s = read(`${root}/${slug}.${sourceLocale}.json`)
      let text = s.card.slice(s.card.indexOf(s.requirements) + s.requirements.length).trim()
      if (locale !== 'en') text = text.replace(new RegExp(`\\s*${source.name}$`), '').trim()
      const template = key === 'guardian' ? text.replace('(25—35)', '{v0}') : key === 'gemini' ? text.replace('+50', '{v0}') : key === 'obliterator' ? text.replace('50', '{v0}') : text.replace('[64]', '[{v0}]').replace('[28]', '[{v1}]')
      display.templates[locale][templateKey] = { name: s.name, template }
    }
  }
  for (const [locale, sourceLocale] of Object.entries(locales)) {
    const s = read(`${root}/${slug}.${sourceLocale}.json`)
    assert.equal(s.fields.Type, source.fields.Type)
    terms[locale][slug] = { name: s.name, lines: [], itemKey: s.fields.Type, sourceUrl: s.url }
    requirements[locale] = s.requirements
    sourceProperties[locale] = labels[locale].map(label => {
      const m = s.card.match(new RegExp(`${label}: [\\d.%-]+`))
      assert(m, `${key}/${locale}/${label}`)
      return m[0]
    })
  }
  const pool = `${key}-bow`, poolRoot = `backend/src/main/resources/catalog/${pool}`
  fs.mkdirSync(poolRoot, { recursive: true })
  const rawText = JSON.stringify(raw, null, 2) + '\n'
  const detailText = JSON.stringify({ ordinary: details, special: ordinary.perfect_essence, implicit: implicitId ? read(`${root}/${slug}.implicit.json`) : null }, null, 2) + '\n'
  fs.writeFileSync(`${poolRoot}/base.raw.json`, rawText)
  fs.writeFileSync(`${poolRoot}/details.raw.json`, detailText)
  const prefixes = modifiers.filter(d => d.affixType === 'PREFIX'), suffixes = modifiers.filter(d => d.affixType === 'SUFFIX')
  write(`${poolRoot}/catalog.json`, { metadata: { ...old.metadata, snapshotId: `poe2db-${pool}-20261004-${hash(rawText + detailText).slice(0,16)}`, retrievedAt: source.retrievedAt, rawSha256: hash(rawText), detailsSha256: hash(detailText), prefixCount: prefixes.length, suffixCount: suffixes.length, prefixWeight: prefixes.reduce((n,d)=>n+d.weight,0), suffixWeight: suffixes.reduce((n,d)=>n+d.weight,0) }, base: { ...old.base, id: source.fields.Type, name: source.name, sourceUrl: source.url, implicitModifierId: implicitId }, modifiers })
  manifest[key] = { key, slug, pool, family: 'bows', id: source.fields.Type, name: source.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: tags, armour: 0, strength: 0, dexterity: +source.card.match(/(\d+) Dex/)[1], requiredLevel: +source.card.match(/Level (\d+)/)[1], maximumQuality: 20, requirements, sourceProperties, implicitModifierId: implicitId, implicitStats: implicit?.stats ?? [], implicitLocalities: implicitId ? read(`${root}/${slug}.implicit.json`).stats : [], physicalDamage: source.card.match(/Physical Damage: ([\d-]+)/)[1], criticalHitChance: +source.card.match(/Critical Hit Chance: ([\d.]+)/)[1], attacksPerSecond: +source.card.match(/Attacks per Second: ([\d.]+)/)[1] }
  overrides[key] = { fixed, replacements }
  registry.workbenchBases[key] = { ...registry.workbenchBases.bow, baseItemId: source.fields.Type, source: source.url, sourceSha256: source.sha256, implicit: implicitId || 'NONE_IN_REVIEWED_BASE', requiredCharacterLevel: manifest[key].requiredLevel, requiredDexterity: manifest[key].dexterity, weaponProperties: sourceProperties.en, implicitLocalities: manifest[key].implicitLocalities, augmentSockets: null }
  for (const e of registry.entries) {
    if (e.serviceScope === 'DEFERRED') continue
    const action = e.action ?? e.workbenchAction
    if (e.category === 'ESSENCE' ? !Object.hasOwn(fixed, action) && !Object.hasOwn(replacements, action) : !e.supportedBases?.includes('bow')) continue
    if (['ALLOY','CATALYST'].includes(e.category) || ['ARTIFICER','ESSENCE_HORROR'].includes(action)) continue
    if (e.id === 'Omen_of_the_Blessed' && key !== 'guardian') continue
    if (!e.supportedBases.includes(key)) e.supportedBases.push(key)
  }
  if (key === 'guardian') {
    const blessed = registry.entries.find(e => e.id === 'Omen_of_the_Blessed')
    if (!blessed.supportedBases.includes(key)) blessed.supportedBases.push(key)
  }
  summary.push({ key, ordinary: raw.length, special: perfect.length + abyss.length, implicit: implicitId, fixedActions: Object.keys(fixed).length, replacementActions: Object.keys(replacements).length })
}
for (const p of ['backend/src/main/resources/catalog/top-bases.json', 'frontend/src/features/crafting/topBases.json']) write(p, manifest)
for (const p of ['backend/src/main/resources/catalog/top-base-essences.json', 'frontend/src/features/crafting/topBaseEssences.json']) write(p, overrides)
write('frontend/src/shared/i18n/gameTerms.json', terms)
write('frontend/src/shared/i18n/modifierTemplates.json', display)
write('backend/src/main/resources/crafting/registry-v2.json', registry)
write(`${root}/import-summary.json`, summary)
console.log(summary)
