import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import {cleanSource as clean} from './armour-source.mjs'
const root='docs/evidence/quivers-source-bundle-2026-10-05'
const previous=JSON.parse(fs.readFileSync(`${root}/details/Quivers/normal-57.json`,'utf8'))
for(const locale of ['us','kr','jp','cn','tw','sp']) {
 const url=`https://poe2db.tw/${locale}/hover?s=${encodeURIComponent('Data\\Mods/IncreasedAttackSpeed1')}`
 const r=await fetch(url)
 if(!r.ok)continue
 const html=await r.text()
 const stats=[...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m=>{const n=clean(m[2]).match(/-?\d+(?:\.\d+)?/g).map(Number);return{id:clean(m[1]).replaceAll(' ','_'),min:n[0],max:n[1],locality:clean(m[3])}})
 const spawn=[...(html.match(/<tr><th>Spawn Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1]??'').matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m=>({tag:m[1],weight:+m[2]}))
 assert.deepEqual(stats,[{id:'attack_speed_+%',min:5,max:7,locality:'Global'}])
 assert.deepEqual(spawn,[{tag:'gloves',weight:1},{tag:'quiver',weight:1},{tag:'default',weight:0}])
 const record={row:previous.row,url,textLocale:locale,detailText:clean(html.match(/<h5 class="card-header">([^]*?)<\/h5>/)[1]),retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),html,code:'IncreasedAttackSpeed1',stats,spawn,correction:'Previous same-name weapon fallback is ineligible; source Quiver row spawn_no and global attack-speed peers require IncreasedAttackSpeed1. Original evidence remains intact.'}
 fs.writeFileSync(`${root}/details/Quivers/normal-57.corrected.json`,JSON.stringify(record,null,2)+'\n',{flag:'wx'})
 console.log(url);break
}
assert(fs.existsSync(`${root}/details/Quivers/normal-57.corrected.json`))
