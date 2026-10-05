import fs from 'node:fs'
import assert from 'node:assert/strict'
const paths=['scripts/import-maces-bundle.mjs','scripts/import-quarterstaves-spears-bundle.mjs']
const source=fs.readFileSync(paths[1],'utf8')
const a=source.indexOf('function bind'),z=source.indexOf('const reports=',a)
const body=source.slice(a,z)
const pattern=source.match(/const numberPattern = [^\n]+/)[0]
const shared="import assert from 'node:assert/strict'\n"+pattern+"\nconst equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b)\nexport function orderedSpawnEligible(detail,tags) {const first=detail.spawn.find(s=>tags.includes(s.tag));assert(first,`${detail.url}: ordered spawn missing`);return first.weight>0}\nexport function createDisplayBinder(display,localeKeys,namespace) {\n"+body.replace('function bind(d,texts,code)','return function bind(d,texts,code)').replace('Object.keys(locales)','localeKeys').replace('`quarterstaves-spears.${d.id}`','`${namespace}.${d.id}`')+'}\n'
fs.writeFileSync('scripts/reviewed-catalog-importer.mjs',shared,{flag:'wx'})
const baselines={}
for(const path of paths) {
 const s=fs.readFileSync(path,'utf8'),a=s.indexOf('function bind'),z=s.indexOf('const reports=',a),namespace=path.includes('quarterstaves')?'quarterstaves-spears':'maces'
 assert.equal(s.slice(a,z).replace('`maces.${d.id}`','`quarterstaves-spears.${d.id}`'),body)
 baselines[namespace]={binder:s.slice(a,z),numberPattern:s.match(/const numberPattern = [^\n]+/)[0]}
 let result=s.slice(0,a)+`const bind=createDisplayBinder(display,Object.keys(locales),'${namespace}')\n`+s.slice(z)
 result="import { createDisplayBinder, orderedSpawnEligible } from './reviewed-catalog-importer.mjs'\n"+result
 result=result.replace(/^const numberPattern = [^\n]+\n/m,'')
 result=result.replace('const eligible=d=>{const first=d.spawn.find(s=>tags.includes(s.tag));assert(first,`${d.url}: ordered spawn missing`);return first.weight>0}','const eligible=d=>orderedSpawnEligible(d,tags)')
 fs.writeFileSync(path,result)
}
fs.writeFileSync('docs/evidence/base-registry-refactor-2026-10-05/importer-baseline.json',JSON.stringify(baselines,null,2)+'\n',{flag:'wx'})
