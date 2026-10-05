import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
import { orderedSpawnEligible } from './reviewed-catalog-importer.mjs'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const destination=`${root}/details-reconciled/Staves`
fs.mkdirSync(destination,{recursive:true})
const report=[]
for(const file of fs.readdirSync(`${root}/details/Staves`).filter(f=>f.startsWith('normal-'))) {
 const previous=JSON.parse(fs.readFileSync(`${root}/details/Staves/${file}`))
 if(orderedSpawnEligible(previous,['staff','twohand','default']))continue
 const code=previous.code.replace('LevelWeapon','LevelTwoHandWeapon'),url=`https://poe2db.tw/us/hover?s=${encodeURIComponent('Data\\Mods/'+code)}`
 const response=await fetch(url);assert(response.ok)
 const html=await response.text(),detailText=clean(html.match(/<h5 class="card-header">([^]*?)<\/h5>/)?.[1]??'')
 assert.equal(detailText,clean(previous.row.str))
 const fields=Object.fromEntries([...html.matchAll(/<tr><th>([^<]+)<td>([^]*?)(?=<tr>|<\/table>)/g)].map(m=>[clean(m[1]),clean(m[2])]))
 const stats=[...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m=>{const n=clean(m[2]).match(/-?\d+(?:\.\d+)?/g).map(Number);return{id:clean(m[1]).replaceAll(' ','_'),min:n[0],max:n[1],locality:clean(m[3])}})
 const spawn=[...(html.match(/<tr><th>Spawn Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1]??'').matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}))
 assert.deepEqual(stats,previous.stats)
 assert.deepEqual(fields.Family.split(', '),previous.row.ModFamilyList)
 assert.deepEqual(spawn.map(s=>s.tag),previous.row.spawn_no)
 assert(orderedSpawnEligible({url,spawn},['staff','twohand','default']))
 const proof={...previous,url,code,html,fields,stats,spawn,detailText,textLocale:'us',retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),reconciles:`details/Staves/${file}`,reason:'Previous signature fallback matched Wand endpoint; exact Staff source header, family, stats, spawn order verified'}
 fs.writeFileSync(`${destination}/${file}`,JSON.stringify(proof,null,2)+'\n',{flag:'wx'})
 report.push({file,previousCode:previous.code,sourceCode:code,sourceUrl:url,sha256:proof.sha256})
}
assert.equal(report.length,10)
fs.writeFileSync(`${root}/detail-reconciliation.json`,JSON.stringify({passed:true,entries:report},null,2)+'\n',{flag:'wx'})
console.log('Reconciled',report.length,'exact Staff details; original evidence preserved')
