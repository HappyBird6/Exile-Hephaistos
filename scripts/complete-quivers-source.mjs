import fs from 'node:fs'
import assert from 'node:assert/strict'
import {cleanSource as clean} from './armour-source.mjs'
const root='docs/evidence/quivers-source-bundle-2026-10-05'
const manifest=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json','utf8'))
const report=[]
for(const b of Object.values(manifest).filter(b=>b.family==='quivers')) {
 const html=fs.readFileSync(`${root}/${b.slug}.us.html`,'utf8')
 const block=[...html.matchAll(/<h5 class="card-header">[^]*?<\/h5>\s*<table[^]*?<\/table>/g)].find(m=>m[0].includes('<tr><th>Family'))[0]
 const craft=block.match(/<tr><th>Craft Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1]??''
 const tags=[...craft.matchAll(/<span[^>]*>([^<]+)<\/span>/g)].map(m=>clean(m[1]).toLowerCase())
 const p=`backend/src/main/resources/catalog/${b.pool}/catalog.json`,c=JSON.parse(fs.readFileSync(p,'utf8'))
 c.modifiers.find(d=>d.layer==='IMPLICIT').tags=tags
 fs.writeFileSync(p,JSON.stringify(c,null,2)+'\n')
 const source=JSON.parse(fs.readFileSync(`${root}/${b.slug}.us.json`,'utf8')).variants[0]
 assert.equal(source.fields['Mods.enable_rarity'],'normal, magic, rare, unique')
 assert.equal(source.fields.Class,'Quivers')
 assert.equal(source.fields.Type,b.id)
 report.push({key:b.key,sourceUrl:b.sourceUrl,sourceSha256:b.sourceSha256,normalPopup:true,rarityFlags:source.fields['Mods.enable_rarity'],tags,requiredLevel:b.requiredLevel,implicit:c.modifiers.at(-1)})
}
fs.writeFileSync(`${root}/${process.argv[2]??'source-review.json'}`,JSON.stringify({bases:report,ordinaryRoster:11,selection:'One base per distinct implicit; current class list has no higher ordinary replica for these eleven families',availabilityEvidence:'Current normal popup, class base list and normal/magic/rare flags. Official GGG trade2 endpoint returned403; no independent GGG trade proof was obtained.',failedAvailability:{url:'https://www.pathofexile.com/api/trade2/data/items',status:403},restrictions:'No published Quality.max_quality; generic socket metadata has level9999 for positive sockets. Ordinary usable socket count0; socket state/Artificer/Catalyst unsupported. No Bow weapon-quality alias.'},null,2)+'\n',{flag:'wx'})
