import { createDisplayBinder, orderedSpawnEligible } from './reviewed-catalog-importer.mjs'
import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/maces-source-bundle-2026-10-05'
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'))
const write = (p,v) => fs.writeFileSync(p, JSON.stringify(v,null,2)+'\n')
const hash = s => crypto.createHash('sha256').update(s).digest('hex')
const locales = { en:'us', ko:'kr', ja:'jp', 'zh-CN':'cn', 'zh-TW':'tw', es:'sp' }
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
let page
const sourceTexts = (kind,i) => Object.fromEntries(Object.entries(locales).map(([l,r])=>[l,clean(read(`${root}/${page}.${r}.json`).data[kind][i].str)]))
const bind=createDisplayBinder(display,Object.keys(locales),'maces')
const reports=[]
for (page of ['One_Hand_Maces','Two_Hand_Maces']) {
const data = read(`${root}/${page}.us.json`).data
const oneHand = page === 'One_Hand_Maces'
const family = oneHand ? 'one-hand-maces' : 'two-hand-maces'
const names = oneHand ? ['Fortified Hammer','Strife Pick','Akoyan Club'] : ['Ruination Maul','Fanatic Greathammer','Tawhoan Greatclub']
const tags=['mace',oneHand?'one_hand_weapon':'two_hand_weapon',oneHand?'onehand':'twohand','weapon','default']
const eligible=d=>orderedSpawnEligible(d,tags)
const ordinary=[]
for(let i=0;i<data.normal.length;i++) {
  const detail=read(`${root}/details/${page}/normal-${i}.json`),row=data.normal[i]
  assert(eligible(detail),`${i}: complete ordinary pool eligibility`)
  const affixType=+row.ModGenerationTypeID===1?'PREFIX':'SUFFIX'
  const s=stats(detail),text=clean(row.str)
  const reusable=existing.find(d=>d.layer==='EXPLICIT'&&d.affixType===affixType&&d.requiredItemLevel===+row.Level&&equal(d.familyIds,row.ModFamilyList)&&equal(d.stats,s)&&d.text===text)
  const tiers=[...new Set(data.normal.filter(r=>r.ModGenerationTypeID===row.ModGenerationTypeID&&r.ModFamilyList.join(',')===row.ModFamilyList.join(',')).map(r=>+r.Level))].sort((a,b)=>b-a)
  const d={id:reusable?.id??`${family}:${affixType.toLowerCase()}:${slugify(row.Name)}:${detail.code??hash(JSON.stringify(s)).slice(0,12)}`,name:clean(row.Name),layer:'EXPLICIT',affixType,familyIds:row.ModFamilyList,requiredItemLevel:+row.Level,weight:+row.DropChance,tier:tiers.indexOf(+row.Level)+1,text,stats:s,tags:row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]),sourceUrl:detail.url}
  assert(Number.isInteger(d.weight)&&d.weight>0,'Published selection weight required')
  bind(d,sourceTexts('normal',i),detail.code)
  ordinary.push(d)
}
const fixed={},replacements={},special=[],excluded=[]
for(const kind of ['essence','perfect_essence'])for(let i=0;i<data[kind].length;i++) {
  const row=data[kind][i],detail=read(`${root}/details/${page}/${kind}-${i}.json`)
  const action=row.Name.match(/href="([^"]+)"/)[1].replaceAll('_the_','_').replaceAll('_of_','_').toUpperCase()
  const entry=registry.entries.find(e=>(e.action??e.workbenchAction)===action&&e.category==='ESSENCE'&&e.serviceScope!=='DEFERRED')
  if (!entry||row.IsAlloy||(kind==='essence'&&!eligible(detail))) { excluded.push({kind,code:row.Code,action,reason:!entry?'Existing excluded/deferred action':row.IsAlloy?'Excluded Alloy':'Ordered spawn denies Mace'});continue }
  let d=ordinary.find(d=>equal(d.stats,stats(detail))&&d.requiredItemLevel===+row.Level&&equal(d.familyIds,row.ModFamilyList))
  if(kind==='essence') {
    assert(d,`${action}: exact ordinary target required`)
    ;(fixed[action]??=[]).push(d.id)
  } else {
    if(action==='ESSENCE_HYSTERIA') {
      assert(d && eligible(detail),'Hysteria must use an exact ordinary eligible Mace target')
      assert.equal(d.text,clean(row.str))
      ;(replacements[action]??=[]).push(d.id)
      continue
    }
    assert(detail.spawn.every(s=>s.weight===0),'Special target must not enter ordinary generation')
    const text=row.Code.startsWith('EssenceAbyss')?detail.detailText:clean(row.str),s=stats(detail),affixType=+row.ModGenerationTypeID===1?'PREFIX':'SUFFIX'
    const reusable=existing.find(d=>d.layer==='EXPLICIT'&&d.affixType===affixType&&equal(d.stats,s)&&equal(d.familyIds,row.ModFamilyList)&&d.text===text&&d.requiredItemLevel===+row.Level&&d.weight===0)
    d={id:reusable?.id??`${family}:${affixType.toLowerCase()}:essence:${row.Code}`,name:clean(row.Name),layer:'EXPLICIT',affixType,familyIds:row.ModFamilyList,requiredItemLevel:+row.Level,weight:0,tier:1,text,stats:s,tags:row.mod_no.map(m=>m.match(/data-tag="([^"]+)"/)[1]),sourceUrl:detail.url}
    if(row.Code.startsWith('EssenceAbyss')) {
      assert(reusable,'Exact previously reviewed full Abyss target required')
      assert.deepEqual(display.definitions[d.id].stats,d.stats)
      assert.equal(display.definitions[d.id].englishText,text)
    } else bind(d,sourceTexts(kind,i),row.Code)
    if(!special.some(s=>s.id===d.id))special.push(d)
    ;(replacements[action]??=[]).push(d.id)
  }
}
const parseImplicit = html => [...html.matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].filter(m=>m[0].includes('<tr><th>Family')).map(m=>({text:clean(m[1]),family:clean(m[0].match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)[1]),unscalable:m[0].includes('Unscalable Value'),stats:[...m[0].matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span>/g)].map(m=>{const n=clean(m[2]).match(/-?\d+/g).map(Number);return{id:clean(m[1]).replaceAll(' ','_'),min:n[0],max:n[1],locality:clean(m[3])}})}))
const report={ordinary:ordinary.length,reused:ordinary.filter(d=>!d.id.startsWith(family+':')).length,fixed,replacements,excluded,bases:[],weightPolicy:'POE2DB_AS_PUBLISHED',combatSimulation:false}
for(const name of names) {
  const key=slugify(name),slug=name.replaceAll(' ','_'),source=read(`${root}/${slug}.us.json`),b=source.variants[0]
  assert(!bases[key]);assert.equal(source.variants.length,1);assert.equal(b.fields.Class,page.replaceAll('_',' '));assert(b.fields.Type.endsWith('Endgame'));assert.equal(b.fields['Quality.max_quality'],'20')
  assert.equal(b.fields['Sockets.socket_info'],oneHand?'1:5:100':'1:5:100, 1:5:100 2:5:100')
  assert.equal(b.fields['Mods.enable_rarity'],'normal, magic, rare, unique')
  assert(read(`${root}/${page}.us.json`).cards.some(c=>c.slug===slug),'Ordinary class base list required')
  const proof=parseImplicit(fs.readFileSync(`${root}/${slug}.us.html`,'utf8'))
  assert.equal(proof.length,1)
  assert(proof.every(p=>p.stats.length),'Never omit canonical implicit stats')
  const implicitId=proof.length?`${key}:implicit:base`:''
  let implicit
  if(proof.length) {
    const implicitBlock=[...fs.readFileSync(`${root}/${slug}.us.html`,'utf8').matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].find(m=>m[0].includes('<tr><th>Family'))[0]
    const craft=implicitBlock.match(/<tr><th>Craft Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1]??''
    const implicitTags=[...craft.matchAll(/<span[^>]*>([^<]+)<\/span>/g)].map(m=>clean(m[1]).toLowerCase())
    implicit={id:implicitId,name:b.name,layer:'IMPLICIT',affixType:'NONE',familyIds:proof.map(p=>p.family),requiredItemLevel:1,weight:0,tier:0,text:proof.map(p=>p.text).join('\n'),stats:proof.flatMap(p=>p.stats.map(({locality,...s})=>s)),tags:implicitTags,sourceUrl:source.url}
    const texts=Object.fromEntries(Object.entries(locales).map(([l,r])=>[l,parseImplicit(fs.readFileSync(`${root}/${slug}.${r}.html`,'utf8')).map(p=>p.text).join('\n')]))
    bind(implicit,texts,null)
  }
  const requirements={},sourceProperties={}
  for(const [l,r]of Object.entries(locales)) {
    const s=read(`${root}/${slug}.${r}.json`),local=s.variants[0]
    assert.equal(local.fields.Type,b.fields.Type)
    requirements[l]=local.requirements
    const popup=fs.readFileSync(`${root}/${slug}.${r}.html`,'utf8').match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)[0]
    const properties=[...popup.matchAll(/<div class="property">([^]*?)<\/div>/g)].map(m=>clean(m[1]))
    assert(properties.length>=4,`${slug}/${l}: complete weapon properties`)
    sourceProperties[l]=properties
    terms[l][slug]={name:local.name,lines:properties,itemKey:b.fields.Type,sourceUrl:s.url}
  }
  const modifiers=[...ordinary,...special,...(implicit?[implicit]:[])]
  const destination=`backend/src/main/resources/catalog/${key}`
  fs.mkdirSync(destination)
  const rawText=JSON.stringify(data.normal,null,2)+'\n',detailText=JSON.stringify({ordinary:data.normal.map((_,i)=>read(`${root}/details/${page}/normal-${i}.json`)),implicit:proof,special},null,2)+'\n'
  fs.writeFileSync(`${destination}/base.raw.json`,rawText,{flag:'wx'});fs.writeFileSync(`${destination}/details.raw.json`,detailText,{flag:'wx'})
  const affixes=t=>modifiers.filter(d=>d.affixType===t)
  write(`${destination}/catalog.json`,{metadata:{snapshotId:`poe2db-${key}-20261005-${hash(rawText+detailText).slice(0,16)}`,retrievedAt:source.retrievedAt,sourceUrl:`https://poe2db.tw/us/${page}`,weightPolicy:'POE2DB_AS_PUBLISHED',rawSha256:hash(rawText),detailsSha256:hash(detailText),prefixCount:affixes('PREFIX').length,suffixCount:affixes('SUFFIX').length,prefixWeight:affixes('PREFIX').reduce((n,d)=>n+d.weight,0),suffixWeight:affixes('SUFFIX').reduce((n,d)=>n+d.weight,0)},base:{id:b.fields.Type,name:b.name,sourceUrl:source.url,implicitModifierId:implicitId,magicPrefixes:1,magicSuffixes:1,rarePrefixes:3,rareSuffixes:3},modifiers})
  bases[key]={key,slug,pool:key,family,id:b.fields.Type,name:b.name,sourceUrl:source.url,sourceSha256:source.sha256,sourceTags:b.fields.Tags.split(', '),armour:0,strength:+b.requirements.match(/(\d+) Str/)[1],dexterity:0,requiredLevel:+b.requirements.match(/Level (\d+)/)[1],maximumQuality:20,requirements,sourceProperties,implicitModifierId:implicitId,implicitStats:implicit?.stats??[],implicitLocalities:proof.flatMap(p=>p.stats),physicalDamage:b.card.match(/Physical Damage: ([\d-]+)/)[1],criticalHitChance:+b.card.match(/Critical Hit Chance: ([\d.]+)/)[1],attacksPerSecond:+b.card.match(/Attacks per Second: ([\d.]+)/)[1],weaponRange:+b.card.match(/Weapon Range: ([\d.]+)/)[1],ordinarySocketMaximum:oneHand?1:2,classId:+data.baseitem.name}
  overrides[key]={fixed,replacements}
  registry.workbenchBases[key]={...registry.workbenchBases.bow,ruleVersion:'maces-workbench-v1',ledgerVersion:'maces-unverified-numeric-assumptions-v1',baseItemId:b.fields.Type,source:source.url,sourceSha256:source.sha256,projection:'EXPLICIT_AFFIX_WITH_SOURCE_MACE_IMPLICIT',implicit:implicitId||'NONE_IN_REVIEWED_BASE',weaponProperties:sourceProperties.en,requiredCharacterLevel:bases[key].requiredLevel,requiredStrength:bases[key].strength,requiredDexterity:0,augmentSockets:null,ordinarySocketMaximum:bases[key].ordinarySocketMaximum}
  for(const e of registry.entries) {
    if(e.serviceScope==='DEFERRED')continue
    const a=e.action??e.workbenchAction
    if(e.category==='ESSENCE'?!fixed[a]&&!replacements[a]:!e.supportedBases?.includes('bow'))continue
    if(['ALLOY','CATALYST'].includes(e.category)||a==='ARTIFICER'||e.id==='Omen_of_the_Blessed')continue
    if(!e.supportedBases.includes(key))e.supportedBases.push(key)
  }
  if(proof.some(p=>p.stats.some(s=>s.min!==s.max)))registry.entries.find(e=>e.id==='Omen_of_the_Blessed').supportedBases.push(key)
  report.bases.push({key,id:b.fields.Type,implicit:proof,ordinary:ordinary.length,special:special.length})
}
reports.push({page,classId:+data.baseitem.name,...report})
existing.push(...ordinary,...special)
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json'])write(p,bases)
for(const p of ['backend/src/main/resources/catalog/top-base-essences.json','frontend/src/features/crafting/topBaseEssences.json'])write(p,overrides)
write('backend/src/main/resources/crafting/registry-v2.json',registry);write('frontend/src/shared/i18n/gameTerms.json',terms);write('frontend/src/shared/i18n/modifierTemplates.json',display);write(`${root}/verification.json`,reports)
console.log(reports)
