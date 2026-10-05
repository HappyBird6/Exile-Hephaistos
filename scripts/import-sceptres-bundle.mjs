import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/sceptres-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p, v) => fs.writeFileSync(p, JSON.stringify(v, null, 2) + '\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const roster = [['stoic','Stoic','2','Discipline'],['omen','Omen','4','Malice'],['shrine-fire','Shrine','6a','Purity of Fire'],['shrine-ice','Shrine','6b','Purity of Ice'],['shrine-lightning','Shrine','6c','Purity of Lightning'],['clasped','Clasped','8','Heart of Ice'],['wrath','Wrath','10','Fulmination']]
const old = read('backend/src/main/resources/catalog/hallowed-sceptre/catalog.json')
const raw = read('backend/src/main/resources/catalog/rattling-sceptre/base.raw.json')
const details = read('backend/src/main/resources/catalog/rattling-sceptre/details.raw.json')
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const templates = read('frontend/src/shared/i18n/modifierTemplates.json')
assert.deepEqual(read(`${root}/Sceptres.us.json`).data.normal, raw, 'Review changed class pool')
for(const locale of ['us','kr','jp','cn','tw','sp']) {
  const current=read(`${root}/Sceptres.${locale}.json`).data
  const prior=read(`docs/evidence/hallowed-source-bundle-2026-10-04/Sceptres.${locale}.json`).data
  assert.deepEqual(current.essence,prior.essence,'Review changed fixed Essence source')
  assert.deepEqual(current.perfect_essence,prior.perfect_essence,'Review changed Perfect Essence source')
}
const report = { bases: [], localeDiscrepancies: [], unknownEligible: [], excludedUnique: 'FourSceptreUnique1 / Impurity', displayOnlySkills: true }
for (const [key,name,suffix,skill] of roster) {
  assert(!bases[key], 'Preserve existing registrations')
  const id = `Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre${suffix}`
  const source = read(`${root}/${name}_Sceptre.us.json`)
  const b = source.variants.find(v => v.fields.Type === id)
  assert(b && b.skill === skill && b.fields['Mods.enable_rarity'].startsWith('normal, magic, rare'))
  const tags = b.fields.Tags.split(', ')
  const eligible = details.filter(d => {
    const spawn = [...d.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({tag:m[1],weight:+m[2]}))
    const first = spawn.find(s => tags.includes(s.tag) || s.tag === 'default')
    assert(first, d.code)
    const definition = old.modifiers.find(m => m.sourceUrl === d.url)
    assert(definition, `Unmapped eligible semantics ${d.code}`)
    assert.equal(definition.weight, +d.row.DropChance)
    // Spawn Tags establish applicability; the independently published class
    // DropChance is the reviewed numeric selection weight (these differ).
    assert.equal(definition.text, clean(d.row.str).replaceAll('\n',''))
    return first.weight > 0
  })
  const ids = new Set(eligible.map(d => old.modifiers.find(m => m.sourceUrl === d.url).id))
  const modifiers = old.modifiers.filter(m => ids.has(m.id) || m.weight === 0)
  const pool = `${key}-sceptre`, destination = `backend/src/main/resources/catalog/${pool}`
  assert(!fs.existsSync(destination), 'Preserve previous catalog output')
  fs.mkdirSync(destination)
  const rawText = JSON.stringify(eligible.map(d => d.row), null, 2) + '\n'
  const detailText = JSON.stringify({ordinary:eligible,base:{...source,variant:b}},null,2)+'\n'
  fs.writeFileSync(`${destination}/base.raw.json`,rawText,{flag:'wx'})
  fs.writeFileSync(`${destination}/details.raw.json`,detailText,{flag:'wx'})
  const count = type => modifiers.filter(m => m.affixType === type).length
  const weight = type => modifiers.filter(m => m.affixType === type).reduce((n,m)=>n+m.weight,0)
  write(`${destination}/catalog.json`,{metadata:{...old.metadata,snapshotId:`poe2db-${pool}-20261005-${hash(rawText+detailText).slice(0,16)}`,retrievedAt:source.retrievedAt,rawSha256:hash(rawText),detailsSha256:hash(detailText),prefixCount:count('PREFIX'),suffixCount:count('SUFFIX'),prefixWeight:weight('PREFIX'),suffixWeight:weight('SUFFIX')},base:{...old.base,id,name:b.name,sourceUrl:source.url},modifiers})
  const requirements = {}, sourceProperties = {}, slug = key.startsWith('shrine-') ? `Shrine_Sceptre_${suffix}` : `${name}_Sceptre`
  for (const [locale,remote] of Object.entries({en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'})) {
    const localSource = read(`${root}/${name}_Sceptre.${remote}.json`)
    const local = localSource.variants.find(v=>v.fields.Type===id)
    assert(local)
    const line = local.card.split('\n').map(l=>l.trim()).find(l=>l.includes(local.requirements))
    const at = line.indexOf(local.requirements)
    const spirit = line.slice(local.fields.Class.length,at)
    const granted = line.slice(at+local.requirements.length).trim()
    assert(spirit.endsWith('100') && granted)
    requirements[locale]=local.requirements
    if (JSON.stringify(local.requirements.match(/\d+/g)) !== JSON.stringify(b.requirements.match(/\d+/g))) {
      report.localeDiscrepancies.push({key,locale,published:local.requirements,canonical:b.requirements})
      assert.equal(locale,'es','Explicit locale reconciliation required')
      requirements[locale]=`Requiere:  Nivel ${b.fields.DropLevel}${+(b.requirements.match(/(\d+) Str/)?.[1]??0)?`, ${b.requirements.match(/(\d+) Str/)[1]} Fue`:''}, ${b.requirements.match(/(\d+) Int/)[1]} Int`
    }
    sourceProperties[locale]=[spirit,granted]
    terms[locale][slug]={name:local.name,lines:[spirit,granted],itemKey:id,sourceUrl:localSource.url}
    const rows=read(`${root}/Sceptres.${remote}.json`).data.normal
    assert.equal(rows.length,raw.length)
    for(let i=0;i<raw.length;i++) {
      assert.deepEqual(rows[i].ModFamilyList,raw[i].ModFamilyList)
      const d=old.modifiers.find(m=>m.sourceUrl===details[i].url), binding=templates.definitions[d.id]
      assert.equal(templates.templates[locale][binding.template].template.replace(/\{v(\d+)\}/g,(_,n)=>binding.values[+n]).replaceAll('\n',''),clean(rows[i].str).replaceAll('\n',''))
    }
  }
  bases[key]={key,slug,pool,family:'sceptres',id,name:b.name,sourceUrl:source.url,sourceSha256:source.sha256,sourceTags:tags,armour:0,strength:+(b.requirements.match(/(\d+) Str/)?.[1]??0),intelligence:+b.requirements.match(/(\d+) Int/)[1],requiredLevel:+b.fields.DropLevel,maximumQuality:20,requirements,sourceProperties}
  overrides[key]=structuredClone(overrides.hallowed)
  for(const ids of Object.values(overrides[key].fixed)) assert(ids.every(id=>modifiers.some(m=>m.id===id)))
  registry.workbenchBases[key]={...registry.workbenchBases.hallowed,baseItemId:id,source:source.url,sourceSha256:source.sha256,requiredCharacterLevel:+b.fields.DropLevel}
  for(const e of registry.entries) if(e.serviceScope!=='DEFERRED' && e.supportedBases?.includes('hallowed')) e.supportedBases.push(key)
  report.bases.push({key,id,skill,skillUrl:b.skillUrl,tags,eligible:eligible.length,excluded:details.length-eligible.length,weightPolicy:old.metadata.weightPolicy})
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json']) write(p,bases)
for(const p of ['backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json']) write(p,overrides)
write('frontend/src/shared/i18n/gameTerms.json',terms)
write('backend/src/main/resources/crafting/registry-v2.json',registry)
write(`${root}/verification.json`,report)
console.log(report)
