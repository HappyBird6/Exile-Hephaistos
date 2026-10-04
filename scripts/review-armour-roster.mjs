import fs from 'node:fs'
import assert from 'node:assert/strict'
import { collectArmourSource, sourceLocales, sourceProperties } from './armour-source.mjs'

const root = process.argv[2]
assert(root && fs.existsSync(`${root}/bundle.json`))
assert(!fs.existsSync(`${root}/reviewed-roster.json`), 'Preserve previous review')
const raw = JSON.parse(fs.readFileSync(`${root}/bundle.json`))
const sleek = { slug: 'Sleek_Jacket', page: 'Body_Armours_dex_int', locales: {} }
for (const locale of sourceLocales) {
  assert(!fs.existsSync(`${root}/Sleek_Jacket.${locale}.json`))
  sleek.locales[locale] = await collectArmourSource(sleek.slug, locale)
  fs.writeFileSync(`${root}/Sleek_Jacket.${locale}.json`, JSON.stringify(sleek.locales[locale], null, 2) + '\n')
}
const source = sleek.locales.us
sleek.id = source.fields.Type
sleek.properties = sourceProperties(source.card)
sleek.requiredLevel = Number(source.requirements.match(/Level (\d+)/)[1])
sleek.attributes = Object.fromEntries(['Dex', 'Int'].map(a => [a, Number(source.requirements.match(new RegExp(`(\\d+) ${a}`))[1])]))
sleek.sourceTags = source.fields.Tags.split(', ')
sleek.maximumQuality = Number(source.fields['Quality.max_quality'])
sleek.reviewStatus = 'BASE_SOURCE_VERIFIED_MODIFIER_POOL_PENDING'
const outlier = JSON.parse(fs.readFileSync(`${root}/Ornate_Ringmail.outlier.json`))
assert.match(outlier.fields.Type, /Unique/)
assert.match(outlier.fields.Icon, /Uniques\/Loreweave/)
const rejected = new Set(['Tethering_Bands', 'Primal_Markings', 'Adherent_Cuffs', 'Secured_Wraps', 'Apostle_Leggings', 'Warlock_Leggings', 'Cryptic_Leggings'])
// Adherent is the conservative normal-metadata recommendation; Tethering remains unverified.
rejected.delete('Adherent_Cuffs')
const preferred = [...raw.filter(b => !rejected.has(b.slug)), sleek]
assert.equal(preferred.length, 24)
for (const base of preferred) {
  const inventory = JSON.parse(fs.readFileSync(`docs/evidence/top-base-inventory-2026-10-04/${base.page}.json`))
  const cards = inventory.cards.filter(c => !/^Runeforged_|^Runemastered_/.test(c.slug) && c.slug !== 'Ornate_Ringmail' && c.slug !== 'Tethering_Bands')
  for (const label of ['Armour', 'Evasion Rating', 'Energy Shield']) {
    const maximum = Math.max(...cards.map(c => sourceProperties(c.card)[label] ?? 0))
    assert.equal(base.properties[label] ?? 0, maximum, `${base.slug}: not highest reviewed normal defence`)
  }
  assert.doesNotMatch(base.id, /Unique/)
}
const recommendations = [
  { slot: 'Body_Armours_dex_int', preferred: 'Sleek_Jacket', alternative: 'Primal_Markings', rationale: '285 Evasion/87 ES at level 65 and 67 Dex/Int versus 268/82 at level 70 with the same attributes. Source popup carries no additional implicit text. Modifier/class verification still pending.' },
  { slot: 'Body_Armours_str_int', preferred: 'Wolfskin_Mantle', excluded: 'Ornate_Ringmail', rationale: '749/220 inventory outlier uses FourBodyStrIntUnique1 and Uniques/Loreweave icon. Do not infer normal acquisition from Mods.enable_rarity.' },
  { slot: 'Gloves_str_int', preferred: 'Adherent_Cuffs', excluded: 'Tethering_Bands', rationale: 'Both 98 Armour/27 ES and 55 Str/Int; Tethering level 65 uses FourGlovesStrIntUnique1. Retain normal Endgame metadata Adherent at level 80 pending a source-backed acquisition audit of Tethering.' },
  { slot: 'Gloves_dex_int', preferred: 'War_Wraps', alternatives: ['Secured_Wraps'], rationale: 'Both 94 Evasion/29 ES; War requires level 65 and 44 Dex/Int versus level 80 and 55 Dex/Int. Keep separate historical identities.' },
  { slot: 'Boots_str_int', preferred: 'Faithful_Leggings', alternatives: ['Apostle_Leggings', 'Warlock_Leggings', 'Cryptic_Leggings'], rationale: 'All 134 Armour/37 ES; Faithful level 65 and 47 Str/Int has lower requirements than level 70/51, 75/56, 80/59 alternatives. No additional implicit text in reviewed source popups.' },
]
fs.writeFileSync(`${root}/reviewed-roster.json`, JSON.stringify({ status: 'SOURCE_PREFLIGHT_ONLY_NOT_RUNTIME_REGISTRATION', preferred, recommendations }, null, 2) + '\n')
console.log(JSON.stringify({ preferred: preferred.map(b => b.slug), recommendations }, null, 2))
