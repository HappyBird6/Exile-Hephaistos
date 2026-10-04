import fs from 'node:fs'
import { execFileSync } from 'node:child_process'
const head='18a2a3d4235e7ebf3e45c81f4fb6fe5f115c93b0'
const paths={terms:'frontend/src/shared/i18n/gameTerms.json',modifiers:'frontend/src/shared/i18n/modifierTemplates.json',ui:'frontend/src/shared/i18n/messages.json'}
const current=Object.fromEntries(Object.entries(paths).map(([k,p])=>[k,JSON.parse(fs.readFileSync(p,'utf8'))]))
const baseline=Object.fromEntries(Object.entries(paths).map(([k,p])=>[k,JSON.parse(execFileSync('git',['show',head+':'+p],{encoding:'utf8',maxBuffer:20*1024*1024}))]))
const locales=Object.keys(current.terms),inventory=JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json','utf8')).entries
const result={baseHead:head,before:{},after:{}}
for(const[stage,data]of Object.entries({before:baseline,after:current}))for(const l of locales)result[stage][l]={uiKeys:Object.keys(data.ui[l]).length,registeredNames:inventory.filter(e=>data.terms[l][e.id]?.name).length,activeNames:inventory.filter(e=>e.serviceScope!=='DEFERRED'&&data.terms[l][e.id]?.name).length,deferredNames:inventory.filter(e=>e.serviceScope==='DEFERRED'&&data.terms[l][e.id]?.name).length,liquidNames:inventory.filter(e=>e.category==='LIQUID_EMOTION'&&e.id!=='Liquid_Verisium'&&data.terms[l][e.id]?.name).length,liquidDescriptions:inventory.filter(e=>e.category==='LIQUID_EMOTION'&&e.id!=='Liquid_Verisium'&&data.terms[l][e.id]?.lines.length).length,modifierBindings:Object.keys(data.modifiers.definitions).length,templates:Object.keys(data.modifiers.templates[l]).length,compoundMappings:Object.values(data.modifiers.definitions).filter(b=>b.valueStats&&b.stats.length>1&&b.stats.some(s=>s.min!==s.max)).length}
const single=Object.entries(current.modifiers.definitions).filter(([id,b])=>b.stats.length===1&&b.stats[0].min!==b.stats[0].max&&!b.valueStats&&b.values.length===1).filter(([id,b])=>{const nums=b.values[0].match(/-?\d+(?:\.\d+)?/g)?.map(Number);return nums?.length===2&&(nums[0]!==b.stats[0].min||nums[1]!==b.stats[0].max)})
result.sourceUnitSingleStat=single.map(([id,b])=>({id,text:b.englishText,stats:b.stats,values:b.values}))
fs.writeFileSync('docs/evidence/i18n-display-coverage-2026-10-04.json',JSON.stringify(result,null,2)+'\n')
console.log(JSON.stringify({before:result.before,after:result.after,singleStatSourceUnitGaps:single.length,samples:result.sourceUnitSingleStat.slice(0,7)},null,2))
