import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/offhands-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p,v) => fs.writeFileSync(p, JSON.stringify(v,null,2)+'\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const locales = { en:'us', ko:'kr', ja:'jp', 'zh-CN':'cn', 'zh-TW':'tw', es:'sp' }
const [key,slug,page,family] = process.argv.slice(2)
assert(key && slug && page && family)
const data = read(`${root}/${page}.us.json`).data
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const overrides = read('backend/src/main/resources/catalog/top-base-essences.json')
const registry = read('backend/src/main/resources/crafting/registry-v2.json')
const terms = read('frontend/src/shared/i18n/gameTerms.json')
const display = read('frontend/src/shared/i18n/modifierTemplates.json')
const existing = []
for (const dir of fs.readdirSync('backend/src/main/resources/catalog')) {
  const p = `backend/src/main/resources/catalog/${dir}`
  if (!fs.statSync(p).isDirectory()) continue
  for (const f of fs.readdirSync(p).filter(f=>f==='catalog.json'||f.endsWith('.catalog.json'))) existing.push(...(read(`${p}/${f}`).modifiers??[]))
}
const stats = d => d.stats.map(({locality,...s})=>s)
const slugify = s => s.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'')
const equal = (a,b) => JSON.stringify(a)===JSON.stringify(b)
const numberPattern = /[+]?(?:\(-?\d+(?:\.\d+)?[—–]-?\d+(?:\.\d+)?\)|-?\d+(?:\.\d+)?)/g
const sourceTexts = (kind,i) => Object.fromEntries(Object.entries(locales).map(([l,r])=>{
 const original=data[kind][i],rows=read(`${root}/${page}.${r}.json`).data[kind]
 const row=original.Code?rows.find(row=>row.Code===original.Code):rows[i]
 assert(row,`${page}/${kind}/${original.Code??i}/${l}: locale target required`)
 assert.equal(row.Level,original.Level);assert.equal(row.ModGenerationTypeID,original.ModGenerationTypeID);assert.deepEqual(row.ModFamilyList,original.ModFamilyList)
 return [l,clean(row.str)]
}))
function bind(d,texts,code) {
  if (display.definitions[d.id]) {
    assert.equal(display.definitions[d.id].englishText,d.text)
    assert.deepEqual(display.definitions[d.id].stats,d.stats)
    for (const l of Object.keys(locales)) {
      const b=display.definitions[d.id]
      assert.equal(display.templates[l][b.template].template.replace(/\{v(\d+)\}/g,(_,n)=>b.values[+n]).replaceAll('\n',''),texts[l].replaceAll('\n',''))
    }
    return
  }
  const template = `offhands.${d.id}`
  if (code === 'AdditionalAmmo1' || (d.layer==='IMPLICIT' && d.stats.every(s=>s.min===s.max))) {
    if(code==='AdditionalAmmo1')assert.deepEqual(d.stats,[{id:'base_number_of_crossbow_bolts',min:1,max:1}])
    display.definitions[d.id]={stats:d.stats,englishText:d.text,values:[],template,sourceCode:code}
    for(const [l,text]of Object.entries(texts))display.templates[l][template]={name:d.name,template:text}
    return
  }
  const values = [...d.text.matchAll(numberPattern)].map(m=>m[0])
  const available = [...d.stats]
  const valueStats = values.map(value=>{
    const n=value.match(/-?\d+(?:\.\d+)?/g).map(Number)
    const range=n.length===1?[n[0],n[0]]:n
    const candidates=available.flatMap(s=>[1,-1,100,-100,60,-60].filter(divisor=>equal([s.min/divisor,s.max/divisor].sort((a,b)=>a-b),range)).map(divisor=>({id:s.id,divisor})))
    assert.equal(candidates.length,1,`${d.id}: source span must have one unambiguous stat binding: ${value}`)
    const chosen=candidates[0]
    available.splice(available.findIndex(s=>s.id===chosen.id),1)
    return chosen
  })
  assert(available.every(s=>s.min===s.max),`${d.id}: undisplayed variable stat needs a reviewed binding`)
  display.definitions[d.id]={stats:d.stats,englishText:d.text,values,template,sourceCode:code??null,valueStats}
  for (const [l,text] of Object.entries(texts)) {
    const localValues=[...text.matchAll(numberPattern)].map(m=>m[0])
    assert.deepEqual(localValues,values,`${d.id}/${l}: exact numeric spans`)
    let i=0
    const translated=text.replace(numberPattern,()=>`{v${i++}}`)
    display.templates[l][template]={name:d.name,template:translated}
    assert.equal(translated.replace(/\{v(\d+)\}/g,(_,n)=>values[+n]),text)
  }
}
const source=read(`${root}/${slug}.us.json`),base=source.variants[0]
assert.equal(source.variants.length,1)
assert.equal(base.fields.Class,{shields:'Shields',bucklers:'Bucklers',foci:'Foci'}[family])
assert(base.fields.Type.endsWith('Endgame'))
assert.equal(base.fields['Mods.inventory_type'],'Offhand')
assert.equal(base.fields['Quality.max_quality'],'20')
assert.equal(base.fields['Sockets.socket_info'],'1:5:100')
const tags=[...base.fields.Tags.split(', '),...base.fields['Base.tag'].split(', ')]
assert(!tags.includes('weapon'))
const eligible=d=>{const first=d.spawn.find(s=>tags.includes(s.tag));assert(first,`${d.url}: ordered spawn missing`);return first.weight>0}
const ordinary=[],special=[],fixed={},replacements={},excluded=[]
for(let i=0;i<data.normal.length;i++) {
 const row=data.normal[i],detail=read(`${root}/details/${page}/normal-${i}.json`)
 assert(eligible(detail),`${key}/${i}: complete ordinary eligibility`)
 const s=stats(detail),text=clean(row.str),affixType=+row.ModGenerationTypeID===1?'PREFIX':'SUFFIX'
 const reusable=existing.find(d=>d.layer==='EXPLICIT'&&d.affixType===affixType&&d.requiredItemLevel===+row.Level&&equal(d.familyIds,row.ModFamilyList)&&equal(d.stats,s)&&d.text===text)
 const tiers=[...new Set(data.normal.filter(r=>r.ModGenerationTypeID===row.ModGenerationTypeID&&r.ModFamilyList.join(',')===row.ModFamilyList.join(',')).map(r=>+r.Level))].sort((a,b)=>b-a)
 const d={id:reusable?.id??`offhand:${affixType.toLowerCase()}:${slugify(row.Name)}:${detail.code??hash(JSON.stringify(s)).slice(0,12)}`,name:clean(row.Name),layer:'EXPLICIT',affixType,familyIds:row.ModFamilyList,requiredItemLevel:+row.Level,weight:+row.DropChance,tier:tiers.indexOf(+row.Level)+1,text,stats:s,tags:row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]),sourceUrl:detail.url}
 assert(Number.isInteger(d.weight)&&d.weight>0,'Published weight required')
 bind(d,sourceTexts('normal',i),detail.code)
 ordinary.push(d)
}
for(const kind of ['essence','perfect_essence'])for(let i=0;i<data[kind].length;i++) {
 const row=data[kind][i],detail=read(`${root}/details/${page}/${kind}-${i}.json`)
 const action=row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_','_').replaceAll('_of_','_').toUpperCase()
 const entry=registry.entries.find(e=>(e.action??e.workbenchAction)===action&&e.category==='ESSENCE'&&e.serviceScope!=='DEFERRED')
 if(!entry||row.IsAlloy||(kind==='essence'&&!eligible(detail))) {excluded.push({kind,code:row.Code,action,reason:!entry?'Existing excluded/deferred action':row.IsAlloy?'Excluded Alloy':'Ordered spawn denies base'});continue}
 let d=ordinary.find(d=>equal(d.stats,stats(detail))&&d.requiredItemLevel===+row.Level&&equal(d.familyIds,row.ModFamilyList))
 if(kind==='essence') {
  assert(d,`${key}/${action}: exact ordinary target required`)
  ;(fixed[action]??=[]).push(d.id)
 } else {
  if(action==='ESSENCE_HYSTERIA') {assert(d && eligible(detail),'Hysteria must target an exact source-valid ordinary modifier');assert.equal(d.text,clean(row.str));(replacements[action]??=[]).push(d.id);continue}
  assert(detail.spawn.every(s=>s.weight===0),'Special results cannot enter ordinary pool')
  const s=stats(detail),text=row.Code.startsWith('EssenceAbyss')?detail.detailText:clean(row.str),affixType=+row.ModGenerationTypeID===1?'PREFIX':'SUFFIX'
  const reusable=existing.find(d=>d.layer==='EXPLICIT'&&d.affixType===affixType&&equal(d.stats,s)&&equal(d.familyIds,row.ModFamilyList)&&d.text===text&&d.requiredItemLevel===+row.Level&&d.weight===0)
  d={id:reusable?.id??`offhand:${affixType.toLowerCase()}:essence:${row.Code}`,name:clean(row.Name),layer:'EXPLICIT',affixType,familyIds:row.ModFamilyList,requiredItemLevel:+row.Level,weight:0,tier:1,text,stats:s,tags:row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]),sourceUrl:detail.url}
  if(row.Code.startsWith('EssenceAbyss')) {assert(reusable,'Full reviewed Abyss definition required');assert.deepEqual(display.definitions[d.id].stats,d.stats);assert.equal(display.definitions[d.id].englishText,text)} else bind(d,sourceTexts(kind,i),row.Code)
  if(!special.some(s=>s.id===d.id))special.push(d)
  ;(replacements[action]??=[]).push(d.id)
 }
}
const requirements={},sourceProperties={},skillLines={}
for(const [locale,r]of Object.entries(locales)) {
 const local=read(`${root}/${slug}.${r}.json`).variants[0]
 assert.equal(local.fields.Type,base.fields.Type)
 const popup=fs.readFileSync(`${root}/${slug}.${r}.html`,'utf8').match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)[0]
 const properties=[...popup.matchAll(/<div class="property">([^]*?)<\/div>/g)].map(m=>clean(m[1])).slice(1)
 const skills=[...popup.matchAll(/<div class="implicitMod">([^]*?)<\/div>/g)].map(m=>clean(m[1]))
 assert.equal(skills.length,family==='foci'?0:1,'Built-in skill display must be preserved')
 requirements[locale]=local.requirements
 sourceProperties[locale]=[...properties,...skills]
 skillLines[locale]=skills
 terms[locale][slug]={name:local.name,lines:[...properties,...skills],itemKey:base.fields.Type,sourceUrl:`https://poe2db.tw/${r}/${slug}`}
}
const modifiers=[...ordinary,...special],destination=`backend/src/main/resources/catalog/${key}`
assert(!bases[key]);fs.mkdirSync(destination)
const rawText=JSON.stringify(data.normal,null,2)+'\n',detailText=JSON.stringify({ordinary:data.normal.map((_,i)=>read(`${root}/details/${page}/normal-${i}.json`)),special},null,2)+'\n'
fs.writeFileSync(`${destination}/base.raw.json`,rawText,{flag:'wx'});fs.writeFileSync(`${destination}/details.raw.json`,detailText,{flag:'wx'})
const affixes=t=>modifiers.filter(d=>d.affixType===t)
write(`${destination}/catalog.json`,{metadata:{snapshotId:`poe2db-${key}-20261005-${hash(rawText+detailText).slice(0,16)}`,retrievedAt:source.retrievedAt,sourceUrl:`https://poe2db.tw/us/${page}`,weightPolicy:'POE2DB_AS_PUBLISHED',rawSha256:hash(rawText),detailsSha256:hash(detailText),prefixCount:affixes('PREFIX').length,suffixCount:affixes('SUFFIX').length,prefixWeight:affixes('PREFIX').reduce((n,d)=>n+d.weight,0),suffixWeight:affixes('SUFFIX').reduce((n,d)=>n+d.weight,0)},base:{id:base.fields.Type,name:base.name,sourceUrl:source.url,implicitModifierId:'',magicPrefixes:1,magicSuffixes:1,rarePrefixes:3,rareSuffixes:3},modifiers})
const card=base.card,number=label=>+(card.match(new RegExp(`${label}: (-?[\\d.]+)`))?.[1]??0)
bases[key]={key,slug,pool:key,family,id:base.fields.Type,name:base.name,sourceUrl:source.url,sourceSha256:source.sha256,sourceTags:base.fields.Tags.split(', '),armour:number('Armour'),evasion:number('Evasion Rating'),energyShield:number('Energy Shield'),baseMovementSpeed:number('Base Movement Speed'),blockChance:number('Block chance'),strength:+(base.requirements.match(/(\d+) Str/)?.[1]??0),dexterity:+(base.requirements.match(/(\d+) Dex/)?.[1]??0),intelligence:+(base.requirements.match(/(\d+) Int/)?.[1]??0),requiredLevel:+base.requirements.match(/Level (\d+)/)[1],maximumQuality:20,requirements,sourceProperties,skillLines,implicitModifierId:'',implicitStats:[],ordinarySocketMaximum:1,classId:+data.baseitem.name,grantedSkill:family==='foci'?null:family==='bucklers'?'Parry':'Raise_Shield'}
overrides[key]={fixed,replacements}
if(!card.includes('Base Movement Speed:'))delete bases[key].baseMovementSpeed
registry.workbenchBases[key]={...registry.workbenchBases.bow,ruleVersion:'offhand-workbench-v1',ledgerVersion:'offhand-unverified-numeric-assumptions-v1',baseItemId:base.fields.Type,source:source.url,sourceSha256:source.sha256,projection:'EXPLICIT_AFFIX_WITH_SOURCE_OFFHAND_DISPLAY',implicit:'NONE_CANONICAL_IN_REVIEWED_BASE',sourceProperties:sourceProperties.en,requiredCharacterLevel:bases[key].requiredLevel,requiredStrength:bases[key].strength,requiredDexterity:bases[key].dexterity,requiredIntelligence:bases[key].intelligence,augmentSockets:null,ordinarySocketMaximum:1}
delete registry.workbenchBases[key].weaponProperties
for(const e of registry.entries) {
 if(e.serviceScope==='DEFERRED')continue
 const a=e.action??e.workbenchAction
 if(e.category==='ESSENCE'?!fixed[a]&&!replacements[a]:!e.supportedBases?.includes('bow'))continue
 if(['ALLOY','CATALYST'].includes(e.category)||a==='ARTIFICER'||e.id==='Omen_of_the_Blessed')continue
 if(!e.supportedBases.includes(key))e.supportedBases.push(key)
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json'])write(p,bases)
for(const p of ['backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json'])write(p,overrides)
write('backend/src/main/resources/crafting/registry-v2.json',registry);write('frontend/src/shared/i18n/gameTerms.json',terms);write('frontend/src/shared/i18n/modifierTemplates.json',display)
write(`${root}/${key}.verification.json`,{base:bases[key],ordinary:ordinary.length,reused:ordinary.filter(d=>!d.id.startsWith('offhand:')).length,special:special.length,fixed,replacements,excluded,weightPolicy:'POE2DB_AS_PUBLISHED',combatSimulation:false,implicitPolicy:'Source built-in block/property/skill text only; no synthesized canonical item stat without source proof'})
console.log(key,ordinary.length,special.length,Object.keys(fixed).length,Object.keys(replacements).length)
