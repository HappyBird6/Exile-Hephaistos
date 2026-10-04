import fs from 'node:fs'
import assert from 'node:assert/strict'

const root = process.argv[2]
assert(root, 'Pass the collected armour evidence directory')
const read = file => JSON.parse(fs.readFileSync(`${root}/${file}`, 'utf8'))
const bundle = read('bundle.json')
assert.equal(bundle.length, 29, '24 slot/archetype representatives and five equal-defence alternatives')
assert.equal(read('failures.json').length, 0)
const ids = new Set(), locales = ['us', 'kr', 'jp', 'cn', 'tw', 'sp']
let assertions = 2
for (const base of bundle) {
  assert(!ids.has(base.id), `Duplicate metadata: ${base.slug}`)
  ids.add(base.id)
  // Keep the failed raw inventory conclusion visible; reviewed normal metadata is checked below.
  assert.equal(base.highestDefence, !['Wolfskin_Mantle', 'Primal_Markings'].includes(base.slug))
  assert.equal(base.reviewStatus, 'BASE_SOURCE_VERIFIED_MODIFIER_POOL_PENDING')
  assert(base.sourceTags.includes('armour'))
  assert.equal(base.maximumQuality, 20)
  assertions += 5
  for (const locale of locales) {
    const source = read(`${base.slug}.${locale}.json`)
    assert.deepEqual(source, base.locales[locale], `${base.slug}/${locale}: stale bundle record`)
    assert.equal(source.fields.Type, base.id)
    assert.match(source.sha256, /^[a-f0-9]{64}$/)
    assert.equal(source.url, `https://poe2db.tw/${locale}/${base.slug}`)
    assert(source.name && source.requirements)
    assert(source.requirements.includes(String(base.requiredLevel)))
    for (const value of Object.values(base.attributes)) assert(source.requirements.includes(String(value)))
    assertions += 6 + Object.keys(base.attributes).length
  }
}
const summary = read('pool-summary.json')
assert.equal(summary.length, 24)
for (const pool of summary) {
  const source = read(`${pool.page}.pool.json`)
  assert.equal(source.normalCount, source.rows.length)
  assert.equal(source.matchedCount, source.rows.filter(r => r.existing).length)
  assert.equal(source.normalCount - source.matchedCount, source.unmatched.length)
  assert.equal(pool.unmatchedCount, source.unmatched.length)
  assert.match(source.sha256, /^[a-f0-9]{64}$/)
  assert.equal(source.status, 'REVIEW_REQUIRED_ORDERED_SPAWN_AND_CLASS_TARGETS_NOT_ASSUMED')
  assertions += 6
}
// Equal defence does not authorize importing metadata intended for a unique item.
const bySlug = Object.fromEntries(bundle.map(b => [b.slug, b]))
assert.match(bySlug.Tethering_Bands.id, /Unique/)
assert.doesNotMatch(bySlug.Adherent_Cuffs.id, /Unique/)
assert(bySlug.War_Wraps.requiredLevel < bySlug.Secured_Wraps.requiredLevel)
assert(bySlug.War_Wraps.attributes.Dex < bySlug.Secured_Wraps.attributes.Dex)
for (const slug of ['Apostle_Leggings', 'Warlock_Leggings', 'Cryptic_Leggings']) {
  assert.deepEqual(bySlug.Faithful_Leggings.properties, bySlug[slug].properties)
  assert(bySlug.Faithful_Leggings.requiredLevel < bySlug[slug].requiredLevel)
  assert(bySlug.Faithful_Leggings.attributes.Str < bySlug[slug].attributes.Str)
  assertions += 3
}
assertions += 4
const reviewed = read('reviewed-roster.json')
assert.equal(reviewed.status, 'SOURCE_PREFLIGHT_ONLY_NOT_RUNTIME_REGISTRATION')
assert.equal(reviewed.preferred.length, 24)
assert(!reviewed.preferred.some(b => b.slug === 'Primal_Markings' || /Unique/.test(b.id)))
const sleek = reviewed.preferred.find(b => b.slug === 'Sleek_Jacket')
assert(sleek)
assert.deepEqual(sleek.properties, { 'Evasion Rating': 285, 'Energy Shield': 87, 'Base Movement Speed': -0.03 })
assert(sleek.requiredLevel < bySlug.Primal_Markings.requiredLevel)
assert.deepEqual(sleek.attributes, bySlug.Primal_Markings.attributes)
for (const locale of locales) {
  assert.deepEqual(read(`Sleek_Jacket.${locale}.json`), sleek.locales[locale])
  assert.equal(sleek.locales[locale].fields.Type, sleek.id)
  assertions += 2
}
assertions += 7
console.log(JSON.stringify({ assertions, bases: bundle.length + 1, localeRecords: (bundle.length + 1) * locales.length, preferredBases: reviewed.preferred.length, pools: summary.length, unresolvedRowOccurrences: summary.reduce((total, p) => total + p.unmatchedCount, 0), unresolvedRowsByPool: summary.filter(p => p.unmatchedCount), runtimeImportAuthorizedByEvidence: false }, null, 2))
