import fs from 'node:fs'
const read = (p) => fs.readFileSync(p, 'utf8')
const write = (p, t) => fs.writeFileSync(p, t)
const root = 'docs/evidence/top-base-bundle-2026-10-04'
const bundle = JSON.parse(read(`${root}/bundle.json`))
const localeIds = { us: 'en', kr: 'ko', jp: 'ja', cn: 'zh-CN', tw: 'zh-TW', sp: 'es' }
const termsPath = 'frontend/src/shared/i18n/gameTerms.json'
const terms = JSON.parse(read(termsPath))
const data = {}
for (const b of bundle) {
  const requirements = {}
  for (const [sourceLocale, locale] of Object.entries(localeIds)) {
    const source = JSON.parse(read(`${root}/${b.slug}.${sourceLocale}.json`))
    const requirement = source.card.slice(source.card.indexOf(sourceLocale === 'us' ? 'Requires:' : '__MISSING__'))
    terms[locale][b.slug] = { name: source.name, lines: [], itemKey: b.id, sourceUrl: source.url }
    // Preserve published source text; do not infer translated stat names.
    requirements[locale] = source.requirements || (sourceLocale === 'us' ? requirement : '')
  }
  const { eligibleCodes, locales, ...base } = b
  data[b.key] = { ...base, requirements }
}
write(termsPath, JSON.stringify(terms, null, 2) + '\n')
write('frontend/src/features/crafting/topBases.json', JSON.stringify(data, null, 2) + '\n')
write('backend/src/main/resources/catalog/top-bases.json', JSON.stringify(data, null, 2) + '\n')
const registryPath = 'backend/src/main/resources/crafting/registry-v2.json'
const registry = JSON.parse(read(registryPath))
for (const [key, base] of Object.entries(data)) {
  registry.workbenchBases[key] = {
    ...registry.workbenchBases[base.family],
    baseItemId: base.id,
    source: base.sourceUrl,
    sourceSha256: base.sourceSha256,
    baseArmour: base.armour,
    requiredCharacterLevel: base.requiredLevel,
    requiredStrength: base.strength,
    qualityMaximum: base.maximumQuality,
    implicit: 'NONE_IN_REVIEWED_BASE',
    augmentSockets: null,
    projection: 'EXPLICIT_AFFIX_ONLY',
  }
  for (const entry of registry.entries) {
    if (entry.supportedBases?.includes(base.family) && !entry.supportedBases.includes(key)) entry.supportedBases.push(key)
  }
}
write(registryPath, JSON.stringify(registry, null, 2) + '\n')
