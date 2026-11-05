// Reviewed base identities/implicits from source; explicit pools remain the existing sealed snapshot.
import fs from '../evidence-files.cjs'
import assert from 'node:assert/strict'
import { createHash } from 'node:crypto'
import { cleanSource as clean } from '../armour-source.mjs'
const root = 'evidence:base-audit'
const read = (path) => JSON.parse(fs.readFileSync(path, 'utf8'))
const json = (value) => JSON.stringify(value, null, 2) + '\n'
const hash = (value) => createHash('sha256').update(value).digest('hex')
const locales = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const plan = read(`${root}/plan.json`)
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const policies = read('backend/src/main/resources/catalog/base-policies.json')
const essence = read('backend/src/main/resources/catalog/top-base-essences.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const imageSources = read('frontend/src/features/crafting/base-picker/baseImageSources.json')
const templates = { str: 'soldier', dex: 'slipstrike', int: 'vile', str_dex: 'death-mail', str_int: 'wolfskin', dex_int: 'sleek' }
const rareOnly = new Set(['Dusk_Amulet','Gloam_Amulet','Penumbra_Amulet','Tenebrous_Amulet','Corona_Amulet'])
const exampleLabels = { en: 'Temporary example (user policy)', ko: '임시 예시 (사용자 정책)', ja: '仮の例（ユーザー方針）', 'zh-CN': '临时示例（用户策略）', 'zh-TW': '暫時範例（使用者方針）', es: 'Ejemplo temporal (política del usuario)' }
const sourceSkillPatterns = { en: /Grants Skill: Level \d+ [\s\S]+?(?= Grants Skill:|-1 Prefix|-1 Suffix|\n|$)/, ko: /스킬 부여: 레벨 \d+ [\s\S]+?(?= 스킬 부여:|접두어|접미어|\n|$)/, ja: /付与するスキル[^\n]+/, 'zh-CN': /获得技能[^\n]+/, 'zh-TW': /賦予技能[^\n]+/, es: /Otorga la habilidad[^\n]+/ }
const previousRecords = fs.existsSync(`${root}/import-summary.json`) ? read(`${root}/import-summary.json`) : []
const records = []
const header = (proof) => proof.html.match(/^<h5[^]*?<\/h5>/)[0].replace(/<span class=['"]ndash['"]>([^]*?)<\/span>/g, '$1')
const numericSpans = (proof) => [...header(proof).matchAll(/<span class=['"]mod-value['"]>([^]*?)<\/span>/g)].map((match) => clean(match[1]))
for (const slug of plan.collect) {
  const source = read(`${root}/${slug}.us.json`)
  const family = source.variants[0].fields.Class
  const armour = family === 'Body Armours'
  const archetype = armour ? plan.armourAudit.find((row) => row.slug === slug).page.replace('Body_Armours_', '') : undefined
  const templateKey = armour ? templates[archetype] : family === 'Amulets' ? 'stellar' : 'amethyst'
  const template = bases[templateKey]
  assert(template && ['Body Armours','Amulets','Rings'].includes(family), slug)
  assert.equal(source.proofs.length, source.variants.length, `${slug}: one exact implicit proof per identity`)
  for (const [variantIndex, variant] of source.variants.entries()) {
    assert(variant.fields['Mods.enable_rarity'].includes('rare'), `${slug}: craftable source rarity`)
    const suffix = source.variants.length > 1 ? ['fire','cold','lightning'][variantIndex] : null
    const key = slug.toLowerCase().replaceAll('_', '-') + (suffix ? `-${suffix}` : '')
    if (bases[key]) { records.push(previousRecords.find((record) => record.key === key) ?? { key, alreadyImported: true }); continue }
    const termKey = slug + (suffix ? `_${suffix}` : '')
    const proof = source.proofs[variantIndex]
    const stats = proof.stats.map(({ locality, ...stat }) => stat)
    assert(stats.length && new Set(stats.map((stat) => stat.id)).size === stats.length, `${key}: exact stat IDs`)
    const implicitId = `${key}:implicit:base`
    const implicit = { id: implicitId, name: variant.name + ' implicit', layer: 'IMPLICIT', affixType: 'NONE', familyIds: [proof.family], requiredItemLevel: 1, weight: 0, tier: 0, text: proof.text, stats, tags: proof.tags.map((tag) => tag === 'energy shield' ? 'energyshield' : tag), sourceUrl: source.url }
    const templateRoot = `backend/src/main/resources/catalog/${template.pool}`
    const previous = read(`${templateRoot}/catalog.json`)
    const ordinary = previous.modifiers.filter((definition) => definition.layer !== 'IMPLICIT')
    if (policies.baseOverrides[templateKey]?.legacyCatalog === 'BODY') {
      for (const name of fs.readdirSync(templateRoot).filter((name) => name.endsWith('.catalog.json'))) for (const definition of read(`${templateRoot}/${name}`).modifiers) {
        assert(!ordinary.some((row) => row.id === definition.id)); ordinary.push(definition)
      }
    }
    const modifiers = [implicit, ...ordinary]
    const prefixDelta = stats.find((stat) => stat.id === 'local_maximum_prefixes_allowed_+')?.max ?? 0
    const suffixDelta = stats.find((stat) => stat.id === 'local_maximum_suffixes_allowed_+')?.max ?? 0
    const affixLimits = { magicPrefixes: Math.max(0, 1 + prefixDelta), magicSuffixes: Math.max(0, 1 + suffixDelta), rarePrefixes: 3 + prefixDelta, rareSuffixes: 3 + suffixDelta }
    assert(affixLimits.rarePrefixes >= 1 && affixLimits.rareSuffixes >= 1 && affixLimits.rarePrefixes + affixLimits.rareSuffixes <= 6, `${key}: sourced capacity`)
    const pool = key
    const raw = fs.readFileSync(`${templateRoot}/base.raw.json`)
    const details = json({ explicitPoolProvenance: { template: templateKey, pool: template.pool, snapshotId: previous.metadata.snapshotId, rawSha256: hash(raw), detailsSha256: previous.metadata.detailsSha256, note: 'Existing reviewed class/archetype explicit pool; no new game weights inferred.' }, sourceBase: source, variantIndex })
    const count = (side) => ordinary.filter((definition) => definition.affixType === side)
    const catalog = { metadata: { ...previous.metadata, snapshotId: `poe2db-${key}-20261102-${hash(raw + details).slice(0,16)}`, retrievedAt: source.retrievedAt, rawSha256: hash(raw), detailsSha256: hash(details), prefixCount: count('PREFIX').length, suffixCount: count('SUFFIX').length, prefixWeight: count('PREFIX').reduce((n, definition) => n + definition.weight, 0), suffixWeight: count('SUFFIX').reduce((n, definition) => n + definition.weight, 0) }, base: { ...previous.base, id: variant.fields.Type, name: variant.name, sourceUrl: source.url, implicitModifierId: implicitId, ...affixLimits }, modifiers }
    const values = numericSpans(proof)
    const templateId = `base-audit.${key}.implicit`
    const valueStats = values.map((value) => {
      const bounds = value.replace(/[()]/g, '').match(/[+-]?\d+(?:\.\d+)?/g).map(Number)
      const first = bounds[0], last = bounds.at(-1)
      for (const stat of stats) for (const divisor of [1, 10, 100, 60, -1]) if (Math.abs(Math.min(stat.min / divisor, stat.max / divisor) - first) < 1e-9 && Math.abs(Math.max(stat.min / divisor, stat.max / divisor) - last) < 1e-9) return { id: stat.id, divisor }
      throw new Error(`${key}: no exact canonical display unit for ${value}`)
    })
    display.definitions[implicitId] = { stats, englishText: proof.text, values, template: templateId, sourceCode: source.url, ...(values.length ? { valueStats } : {}) }
    const requirements = {}, sourceProperties = {}, skillLines = {}
    for (const [locale, remote] of Object.entries(locales)) {
      const captured = read(`${root}/${slug}.${remote}.json`)
      const local = captured.variants[variantIndex], localProof = captured.proofs[variantIndex]
      assert.equal(local.fields.Type, variant.fields.Type, `${key}/${locale}: exact identity`)
      assert.deepEqual(localProof.stats.map(({ locality, ...stat }) => stat), stats, `${key}/${locale}: exact canonical stats`)
      const localNumbers = numericSpans(localProof)
      assert.equal(localNumbers.length, values.length, `${key}/${locale}: display numbers`)
      let numberIndex = 0
      const localizedText = clean(header(localProof).replace(/<span class=['"]mod-value['"]>([^]*?)<\/span>/g, () => `{v${numberIndex++}}`))
      display.templates[locale][templateId] = { name: local.name, template: localizedText }
      terms[locale][termKey] = { name: local.name, lines: [], itemKey: variant.fields.Type, sourceUrl: captured.url }
      requirements[locale] = local.requirements
      const englishHasSkill = /Grants Skill:/.test(variant.card)
      if (englishHasSkill) {
        // The first source-listed candidate is an explicit user-authorized temporary example.
        // Preserve locale source text without pretending a randomly rolled skill was observed.
        const section = local.card.split(local.requirements).at(-1).trim()
        const first = section.match(sourceSkillPatterns[locale])?.[0]?.trim()
        const fallback = section.split(/ {2,}|\n/).find((line) => line.trim())?.trim()
        const line = first ?? fallback
        assert(line, `${key}/${locale}: temporary example source`)
        // English is bounded precisely; other locales retain first source segment for review.
        skillLines[locale] = [line]
        sourceProperties[locale] = [`${exampleLabels[locale]}: ${line}`]
      }
    }
    const requirement = variant.requirements
    const number = (label) => +(variant.card.match(new RegExp(`${label}: ([\\d.]+)`))?.[1] ?? 0)
    const base = { key, slug: termKey, pool, family: template.family, id: variant.fields.Type, name: variant.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: variant.fields.Tags.split(', ').filter(Boolean), armour: number('Armour'), evasion: number('Evasion Rating'), energyShield: number('Energy Shield'), strength: +(requirement.match(/(\d+) Str/)?.[1] ?? 0), dexterity: +(requirement.match(/(\d+) Dex/)?.[1] ?? 0), intelligence: +(requirement.match(/(\d+) Int/)?.[1] ?? 0), requiredLevel: +(requirement.match(/Level (\d+)/)?.[1] ?? 0), maximumQuality: template.maximumQuality, requirements, implicitModifierId: implicitId, implicitStats: stats, implicitLocalities: proof.stats, affixLimits, ...(archetype ? { archetype } : {}), ...(Object.keys(skillLines).length ? { sourceProperties, skillLines, temporaryBaseOption: { kind: 'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE', source: source.url, rationale: 'User explicitly requested one temporary valid option; no skill roll or probability is inferred.' } } : {}) }
    bases[key] = base
    policies.baseOverrides[key] = { initializeImplicit: true, snapshotDate: '20261102', snapshotRetrievedAt: 'SOURCE', ...(rareOnly.has(slug) ? { rareOnly: true } : {}) }
    if (essence[templateKey]) essence[key] = structuredClone(essence[templateKey])
    else {
      const classTemplate = read('backend/src/main/resources/catalog/death-mail/catalog.json')
      const sourceCode = (definition) => new URL(definition.sourceUrl).searchParams.get('s')?.replaceAll('\\', '/').split('/').at(-1)
      essence[key] = Object.fromEntries(Object.entries(essence['death-mail']).map(([kind, actions]) => [kind, Object.fromEntries(Object.entries(actions).flatMap(([action, ids]) => {
        const mapped = ids.map((id) => {
          const old = classTemplate.modifiers.find((definition) => definition.id === id)
          return ordinary.find((definition) => sourceCode(definition) === sourceCode(old) && definition.requiredItemLevel === old.requiredItemLevel && JSON.stringify(definition.stats) === JSON.stringify(old.stats))?.id
        })
        return mapped.every(Boolean) ? [[action, mapped]] : []
      }))]))
    }
    for (const ids of [...Object.values(essence[key].fixed), ...Object.values(essence[key].replacements)]) for (const id of ids) assert(ordinary.some((definition) => definition.id === id), `${key}: essence target ${id}`)
    registry.workbenchBases[key] = { ...registry.workbenchBases[templateKey], baseItemId: variant.fields.Type, source: source.url, sourceSha256: source.sha256, implicit: implicitId, implicitLocalities: proof.stats, requiredCharacterLevel: base.requiredLevel, projection: 'EXPLICIT_AFFIX_WITH_SOURCE_VARIABLE_IMPLICIT' }
    for (const entry of registry.entries) if (entry.serviceScope !== 'DEFERRED' && entry.supportedBases?.includes(templateKey)) entry.supportedBases.push(key)
    imageSources[variant.fields.Type] = { image: source.image, source: `${root}/${slug}.us.json`, sourceSha256: hash(fs.readFileSync(`${root}/${slug}.us.json`)) }
    records.push({ key, slug, variantIndex, template: templateKey, name: variant.name, korean: terms.ko[termKey].name, rareOnly: rareOnly.has(slug), affixLimits, files: { raw, details, catalog } })
  }
}
for (const key of ['kinetic','mnemonic','vitalic','two-stone-fire-cold']) policies.baseOverrides[key] = { ...policies.baseOverrides[key], newSelection: false }
for (const key of ['sinew-belt','invoking-belt','forking-belt']) policies.baseOverrides[key] = { ...policies.baseOverrides[key], rareOnly: true }
for (const record of records.filter((record) => record.files)) {
  const folder = `backend/src/main/resources/catalog/${record.key}`
  fs.mkdirSync(folder, { recursive: true })
  fs.writeFileSync(`${folder}/base.raw.json`, record.files.raw)
  fs.writeFileSync(`${folder}/details.raw.json`, record.files.details)
  fs.writeFileSync(`${folder}/catalog.json`, json(record.files.catalog))
}
for (const [path, value] of [
  ['backend/src/main/resources/catalog/top-bases.json', bases], ['frontend/src/features/crafting/topBases.json', bases],
  ['backend/src/main/resources/catalog/base-policies.json', policies], ['frontend/src/features/crafting/basePolicies.json', policies],
  ['backend/src/main/resources/catalog/top-base-essences.json', essence], ['frontend/src/features/crafting/topBaseEssences.json', essence],
  ['backend/src/main/resources/crafting/registry-v2.json', registry], ['frontend/src/shared/i18n/gameTerms.json', terms], ['frontend/src/shared/i18n/modifierTemplates.json', display],
  ['frontend/src/features/crafting/base-picker/baseImageSources.json', imageSources],
]) fs.writeFileSync(path, json(value))
fs.writeFileSync(`${root}/import-summary.json`, json(records.map(({ files, ...record }) => record)))
console.log(JSON.stringify({ imported: records.filter((record) => record.files).length, totalTopBases: Object.keys(bases).length, records: records.map(({ files, ...record }) => record) }, null, 2))
