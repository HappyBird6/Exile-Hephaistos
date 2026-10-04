import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/amulets-source-bundle-2026-10-04'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const write=(p,v)=>fs.writeFileSync(p,JSON.stringify(v,null,2)+'\n')
const hash=s=>crypto.createHash('sha256').update(s).digest('hex')
const implicitLine=s=>s.requirements?s.card.slice(s.card.indexOf(s.requirements)+s.requirements.length).trim().split('\n')[0].trim():s.card.split('\n').map(l=>l.trim()).find(l=>l.startsWith(s.fields.Class)).slice(s.fields.Class.length)
const keys=['stellar','amber','bloodstone','lunar','azure','crimson','pearlescent']
const slugs=keys.map(k=>k[0].toUpperCase()+k.slice(1)+'_Amulet')
const locales={en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'}
const manifest=read('backend/src/main/resources/catalog/top-bases.json'), overrides=read('backend/src/main/resources/catalog/top-base-essences.json')
const terms=read('frontend/src/shared/i18n/gameTerms.json'), display=read('frontend/src/shared/i18n/modifierTemplates.json'), registry=read('backend/src/main/resources/crafting/registry-v2.json')
const old=read('backend/src/main/resources/catalog/solar-amulet/catalog.json'),raw=read('backend/src/main/resources/catalog/solar-amulet/base.raw.json'),details=read('backend/src/main/resources/catalog/solar-amulet/details.raw.json')
const pool=read(`${root}/Amulets.en.json`).data
assert.deepEqual(pool.normal,raw,'Complete Amulet class pool must equal verified Solar ordinary source')
const ordinary=old.modifiers.filter(d=>d.layer==='EXPLICIT')
for(const row of raw){const d=ordinary.find(d=>d.sourceUrl===row.hover);assert(d && d.requiredItemLevel===+row.Level && d.weight===+row.DropChance && d.text===clean(row.str) && d.familyIds.join(',')===row.ModFamilyList.join(','));assert(row.spawn_no.includes('amulet'))}
for(let i=0;i<raw.length;i++){
 const proof=read(`${root}/full-details/${i}.json`);assert.deepEqual(proof.row,raw[i])
 const spawn=[...proof.html.matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}))
 const first=spawn.find(s=>['amulet','default'].includes(s.tag));assert(first && first.tag==='amulet' && first.weight>0)
}
for(const locale of Object.keys(locales)){
 const rows=read(`${root}/Amulets.${locale}.json`).data.normal;assert.equal(rows.length,raw.length)
 for(let i=0;i<raw.length;i++){
  const d=ordinary.find(d=>d.sourceUrl===raw[i].hover),b=display.definitions[d.id]
  assert.equal(rows[i].Level,raw[i].Level);assert.deepEqual(rows[i].ModFamilyList,raw[i].ModFamilyList)
  assert.equal(display.templates[locale][b.template].template.replace(/\{v(\d+)\}/g,(_,n)=>b.values[+n]),clean(rows[i].str))
 }
}
const fixed={}, replacements={}, special=[]
const action=row=>row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_','_').replaceAll('_of_','_').toUpperCase()
for(const row of pool.essence){const d=ordinary.find(d=>d.requiredItemLevel===+row.Level && d.familyIds.join(',')===row.ModFamilyList.join(',') && (action(row).endsWith('_INFINITE') || d.text===clean(row.str)));assert(d, row.Code);(fixed[action(row)]??=[]).push(d.id)}
for(const kind of ['perfect-infinite','perfect-enhancement','breach-essence','abyss-essence']){
 const defs=read(`backend/src/main/resources/catalog/solar-amulet/${kind}.catalog.json`).modifiers
 const proof=read(`backend/src/main/resources/catalog/solar-amulet/${kind}.raw.json`)
 for(const d of defs){
  const code=new URL(d.sourceUrl).searchParams.get('s').split('/').at(-1)
  const row=pool.perfect_essence.find(r=>r.Code===code && +r.Level===d.requiredItemLevel && r.ModFamilyList.join(',')===d.familyIds.join(',') && (kind==='abyss-essence'||kind==='perfect-infinite'||clean(r.str)===d.text))
  assert(row, d.id);assert(d.weight===0);special.push(d);(replacements[action(row)]??=[]).push(d.id)
 }
 assert(proof)
}
const hysteriaRow=pool.perfect_essence.find(r=>r.Code==='DamageTakenGainedAsLife4_')
const hysteria=ordinary.find(d=>d.familyIds.join(',')===hysteriaRow.ModFamilyList.join(',') && d.requiredItemLevel===+hysteriaRow.Level && d.text===clean(hysteriaRow.str))
assert(hysteria)
const hysteriaSpecial={...hysteria,id:'amulet:suffix:essence-hysteria-life-recoup',weight:0,tier:1}
display.definitions[hysteriaSpecial.id]={...display.definitions[hysteria.id]}
special.push(hysteriaSpecial);replacements.ESSENCE_HYSTERIA=[hysteriaSpecial.id]
const summary=[]
for(let i=0;i<keys.length;i++){
 const key=keys[i],slug=slugs[i],source=read(`${root}/${slug}.us.json`),proof=read(`${root}/${slug}.implicit.json`)
 assert.equal(source.fields.Class,'Amulets');assert.equal(source.fields.Tags,'amulet')
 const implicitText=implicitLine(source)
 const stats=proof.stats.map(({locality,...s})=>s),implicitId=`${key}:implicit:${proof.family.toLowerCase()}`
 const tags=proof.craftTags.map(t=>t==='energy shield'?'energyshield':t)
 const implicit={id:implicitId,name:source.name+' implicit',layer:'IMPLICIT',affixType:'NONE',familyIds:[proof.family],requiredItemLevel:1,weight:0,tier:0,text:implicitText,stats,tags,sourceUrl:source.url}
 const values=[...implicitText.matchAll(/[+]?(?:\(\d+—\d+\)|\d+)/g)].map(m=>m[0])
 assert.equal(values.length,stats.length)
 for(let n=0;n<stats.length;n++){
  const bounds=values[n].match(/\d+/g).map(Number),divisor=key==='crimson'?60:1
  assert.equal(bounds[0]*divisor,stats[n].min,`${key} source minimum display units`)
  assert.equal((bounds[1]??bounds[0])*divisor,stats[n].max,`${key} source maximum display units`)
 }
 const templateKey=`amulets.${key}.implicit`
 display.definitions[implicitId]={stats,englishText:implicitText,values,template:templateKey,sourceCode:null,valueStats:stats.map(s=>({id:s.id,divisor:key==='crimson'?60:1}))}
 const requirements={}
 for(const [locale,sourceLocale] of Object.entries(locales)){
  const s=read(`${root}/${slug}.${sourceLocale}.json`);assert.equal(s.fields.Type,source.fields.Type)
  const tail=implicitLine(s)
  for(const v of values)assert(tail.includes(v),`${key}/${locale} exact range ${v}`)
  let template=tail;for(let n=0;n<values.length;n++)template=template.replace(values[n],`{v${n}}`)
  display.templates[locale][templateKey]={name:s.name,template};terms[locale][slug]={name:s.name,lines:[],itemKey:s.fields.Type,sourceUrl:s.url};requirements[locale]=s.requirements
 }
 const modifiers=[implicit,...ordinary,...special],poolName=`${key}-amulet`,poolRoot=`backend/src/main/resources/catalog/${poolName}`
 fs.mkdirSync(poolRoot,{recursive:true})
 const rawText=JSON.stringify(raw,null,2)+'\n', detailText=JSON.stringify({ordinary:details,special:pool.perfect_essence,implicit:{...proof,implicitText}},null,2)+'\n'
 fs.writeFileSync(`${poolRoot}/base.raw.json`,rawText);fs.writeFileSync(`${poolRoot}/details.raw.json`,detailText)
 const prefixes=modifiers.filter(d=>d.affixType==='PREFIX'),suffixes=modifiers.filter(d=>d.affixType==='SUFFIX')
 write(`${poolRoot}/catalog.json`,{metadata:{...old.metadata,snapshotId:`poe2db-${poolName}-20261004-${hash(rawText+detailText).slice(0,16)}`,retrievedAt:source.retrievedAt,rawSha256:hash(rawText),detailsSha256:hash(detailText),prefixCount:prefixes.length,suffixCount:suffixes.length,prefixWeight:prefixes.reduce((n,d)=>n+d.weight,0),suffixWeight:suffixes.reduce((n,d)=>n+d.weight,0)},base:{...old.base,id:source.fields.Type,name:source.name,sourceUrl:source.url,implicitModifierId:implicitId},modifiers})
 manifest[key]={key,slug,pool:poolName,family:'amulets',id:source.fields.Type,name:source.name,sourceUrl:source.url,sourceSha256:source.sha256,sourceTags:['amulet'],armour:0,strength:0,requiredLevel:+(source.requirements.match(/Level (\d+)/)?.[1]??0),maximumQuality:20,requirements,implicitModifierId:implicitId,implicitStats:stats,implicitLocalities:proof.stats}
 overrides[key]={fixed,replacements}
 registry.workbenchBases[key]={...registry.workbenchBases.solar,ruleVersion:'distinct-amulets-workbench-v1',ledgerVersion:'amulet-unverified-numeric-assumptions-v1',baseItemId:source.fields.Type,source:source.url,sourceSha256:source.sha256,projection:'EXPLICIT_AFFIX_WITH_SOURCE_VARIABLE_IMPLICIT',implicit:implicitId,requiredCharacterLevel:manifest[key].requiredLevel,implicitLocalities:proof.stats,qualityMaximum:20,unsupported:['ARTIFICER','PASTED_MAPPING','COMBAT','DEFERRED_ALLOYS']}
 for(const e of registry.entries){if(e.serviceScope==='DEFERRED')continue;const a=e.action??e.workbenchAction;if(e.category==='ESSENCE'?!Object.hasOwn(fixed,a)&&!Object.hasOwn(replacements,a):!e.supportedBases?.includes('solar')&&e.id!=='Omen_of_the_Blessed')continue;if(!e.supportedBases.includes(key))e.supportedBases.push(key)}
 summary.push({key,id:source.fields.Type,ordinary:raw.length,special:special.length,implicit:implicitId,locales:6,fixedActions:Object.keys(fixed).length,replacementActions:Object.keys(replacements).length,tagNormalization:key==='lunar'?{'energy shield':'energyshield'}:{},numericDivisor:key==='crimson'?60:1})
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json'])write(p,manifest)
for(const p of ['backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json'])write(p,overrides)
write('frontend/src/shared/i18n/gameTerms.json',terms);write('frontend/src/shared/i18n/modifierTemplates.json',display);write('backend/src/main/resources/crafting/registry-v2.json',registry);write(`${root}/import-summary.json`,summary)
console.log(summary)
