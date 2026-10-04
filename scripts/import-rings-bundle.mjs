import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/rings-source-bundle-2026-10-04'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n')
const hash=s=>crypto.createHash('sha256').update(s).digest('hex')
const locales={en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'}
export const ringKeys=['kinetic','vitalic','mnemonic','pearl','amethyst','prismatic','ruby-ring','two-stone-fire-cold']
const slugs=['Kinetic_Ring','Vitalic_Ring','Mnemonic_Ring','Pearl_Ring','Amethyst_Ring','Prismatic_Ring','Ruby_Ring','Two-Stone_Ring']
const manifest=read('backend/src/main/resources/catalog/top-bases.json'),overrides=read('backend/src/main/resources/catalog/top-base-essences.json')
const terms=read('frontend/src/shared/i18n/gameTerms.json'),display=read('frontend/src/shared/i18n/modifierTemplates.json'),registry=read('backend/src/main/resources/crafting/registry-v2.json')
const old=read('backend/src/main/resources/catalog/iron-ring/catalog.json'),raw=read('backend/src/main/resources/catalog/iron-ring/base.raw.json'),details=read('backend/src/main/resources/catalog/iron-ring/details.raw.json')
const sourcePool=read(`${root}/Rings.en.json`).data
assert.deepEqual(sourcePool.normal,raw,'Complete Ring pool must match previously verified raw rows')
const fixed={}
for(const row of sourcePool.essence){
  const slug=row.Name.match(/href="([^"]+)"/)[1],action=slug.replaceAll('_the_','_').replaceAll('_of_','_').toUpperCase()
  const d=old.modifiers.find(d=>d.layer==='EXPLICIT' && new URL(d.sourceUrl).searchParams.get('s')?.split('/').at(-1)===row.Code)
  assert(d && d.requiredItemLevel===+row.Level && d.familyIds.join(',')===row.ModFamilyList.join(','),action)
  ;(fixed[action]??=[]).push(d.id)
}
const perfect=read('backend/src/main/resources/catalog/iron-ring/perfect-essences.catalog.json').modifiers
const abyss=read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.catalog.json').modifiers
const breach=read('backend/src/main/resources/catalog/solar-amulet/breach-essence.catalog.json').modifiers
const hysteriaOriginal=old.modifiers.find(d=>new URL(d.sourceUrl).searchParams.get('s')?.split('/').at(-1)==='ManaRegeneration5')
assert(hysteriaOriginal)
const hysteria={...hysteriaOriginal,id:'ring:suffix:essence-hysteria-mana-regeneration',weight:0,tier:1}
display.definitions[hysteria.id]={...display.definitions[hysteriaOriginal.id]}
const special=[...perfect,...abyss,...breach,hysteria]
const replacements={}
for(const d of special){
  const code=new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
  const row=sourcePool.perfect_essence.find(r=>r.Code===code && +r.Level===d.requiredItemLevel && r.ModFamilyList.join(',')===d.familyIds.join(','))
  assert(row,`Exact Ring special source ${d.id}`)
  if(code.startsWith('EssenceAbyss'))assert.equal(clean(row.str),clean(read('backend/src/main/resources/catalog/stocky-mitts/abyss-essence.raw.json').find(r=>r.row.Code===code).row.str))
  else assert.equal(clean(row.str),d.text)
  const slug=row.Name.match(/href="([^"]+)"/)[1]
  const action=slug.replaceAll('_the_','_').replaceAll('_of_','_').toUpperCase()
  ;(replacements[action]??=[]).push(d.id)
}
const summary=[]
for(let index=0;index<ringKeys.length;index++){
 const key=ringKeys[index],slug=slugs[index],source=read(`${root}/${slug}.us.json`),proof=read(`${root}/${slug}.implicit.json`)
 assert.equal(source.fields.Class,'Rings');assert.equal(source.fields.Tags,'ring');assert(proof.family && proof.craftTags.length)
 if(key==='two-stone-fire-cold')assert.equal(source.fields.Type,'Metadata/Items/Rings/FourRing13a')
 for(const detail of details){const spawn=[...detail.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}));const first=spawn.find(s=>['ring','default'].includes(s.tag));assert(first && first.tag==='ring' && first.weight>0,`${key}/${detail.code} ordered source eligibility`);const d=old.modifiers.find(d=>new URL(d.sourceUrl).searchParams.get('s')?.split('/').at(-1)===detail.code);assert(d && d.weight===+detail.row.DropChance,`${key}/${detail.code} published class weight preserved`)}
 const implicitId=`${key}:implicit:${proof.family.toLowerCase()}`,text=proof.implicitText,stats=proof.stats.map(({locality,...s})=>s)
 const implicit={id:implicitId,name:source.name+' implicit',layer:'IMPLICIT',affixType:'NONE',familyIds:[proof.family],requiredItemLevel:1,weight:0,tier:0,text,stats,tags:proof.craftTags,sourceUrl:source.url}
 const modifiers=[implicit,...old.modifiers.filter(d=>d.layer!=='IMPLICIT'),...special]
 const templateKey=`rings.${key}.implicit`,values=[...text.matchAll(/[+]?(?:\(\d+—\d+\)|\d+)/g)].map(m=>m[0])
 assert.equal(values.length,stats.length)
 // Display order for Kinetic is minimum then maximum, while the source stat order is maximum/minimum.
 const valueStats=key==='kinetic'?[stats[1],stats[0]]:stats
 display.definitions[implicitId]={stats,englishText:text,values,template:templateKey,sourceCode:null,valueStats:valueStats.map(s=>({id:s.id,divisor:1}))}
 const requirements={}
 for(const [locale,sourceLocale] of Object.entries(locales)){
  const s=read(`${root}/${slug}.${sourceLocale}.json`);assert.equal(s.fields.Type,source.fields.Type)
  const tail=s.card.slice(s.card.indexOf(s.requirements)+s.requirements.length).trim().split('\n')[0].trim()
  const translatedValues=[...tail.matchAll(/[+]?(?:\(\d+—\d+\)|\d+)/g)].map(m=>m[0]);assert.deepEqual(translatedValues,values,`${key}/${locale} exact implicit numeric placeholders`)
  let template=tail;for(let i=0;i<values.length;i++)template=template.replace(values[i],`{v${i}}`)
  display.templates[locale][templateKey]={name:s.name,template}
  terms[locale][slug]={name:s.name,lines:[],itemKey:s.fields.Type,sourceUrl:s.url}
  requirements[locale]=s.requirements
 }
 const pool=`${key}-ring`,poolRoot=`backend/src/main/resources/catalog/${pool}`;fs.mkdirSync(poolRoot,{recursive:true})
 const rawText=JSON.stringify(raw,null,2)+'\n',detailText=JSON.stringify({ordinary:details,special:sourcePool.perfect_essence,implicit:proof},null,2)+'\n'
 fs.writeFileSync(`${poolRoot}/base.raw.json`,rawText);fs.writeFileSync(`${poolRoot}/details.raw.json`,detailText)
 const prefixes=modifiers.filter(d=>d.affixType==='PREFIX'),suffixes=modifiers.filter(d=>d.affixType==='SUFFIX')
 write(`${poolRoot}/catalog.json`,{metadata:{...old.metadata,snapshotId:`poe2db-${pool}-20261004-${hash(rawText+detailText).slice(0,16)}`,retrievedAt:source.retrievedAt,rawSha256:hash(rawText),detailsSha256:hash(detailText),prefixCount:prefixes.length,suffixCount:suffixes.length,prefixWeight:prefixes.reduce((n,d)=>n+d.weight,0),suffixWeight:suffixes.reduce((n,d)=>n+d.weight,0)},base:{...old.base,id:source.fields.Type,name:source.name,sourceUrl:source.url,implicitModifierId:implicitId},modifiers})
 manifest[key]={key,slug,pool,family:'rings',id:source.fields.Type,name:source.name,sourceUrl:source.url,sourceSha256:source.sha256,sourceTags:['ring'],armour:0,strength:0,requiredLevel:+source.requirements.match(/Level (\d+)/)[1],maximumQuality:20,requirements,implicitModifierId:implicitId,implicitStats:stats,implicitLocalities:proof.stats}
 overrides[key]={fixed,replacements}
 registry.workbenchBases[key]={...registry.workbenchBases.ring,baseItemId:source.fields.Type,source:source.url,sourceSha256:source.sha256,projection:'EXPLICIT_AFFIX_WITH_SOURCE_VARIABLE_IMPLICIT',implicit:implicitId,requiredCharacterLevel:manifest[key].requiredLevel,implicitLocalities:proof.stats,qualityMaximum:20,unsupported:['ARTIFICER','PASTED_MAPPING','COMBAT','DEFERRED_ALLOYS']}
 for(const e of registry.entries){if(e.serviceScope==='DEFERRED')continue;const a=e.action??e.workbenchAction;if(e.category==='ESSENCE'?!Object.hasOwn(fixed,a)&&!Object.hasOwn(replacements,a):!e.supportedBases?.includes('ring')&&e.id!=='Omen_of_the_Blessed')continue;if(!e.supportedBases.includes(key))e.supportedBases.push(key)}
 summary.push({key,id:source.fields.Type,ordinary:raw.length,special:special.length,implicit:implicitId,fixedActions:Object.keys(fixed).length,replacementActions:Object.keys(replacements).length,locales:6,provenance:'EXACT_SOURCE_ROWS_AND_REUSED_VERIFIED_DETAILS',fallbackExceptions:[]})
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json'])write(p,manifest)
for(const p of ['backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json'])write(p,overrides)
write('frontend/src/shared/i18n/gameTerms.json',terms);write('frontend/src/shared/i18n/modifierTemplates.json',display);write('backend/src/main/resources/crafting/registry-v2.json',registry);write(`${root}/import-summary.json`,summary)
console.log(summary)
