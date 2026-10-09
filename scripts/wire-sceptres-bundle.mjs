import fs from 'node:fs'
import assert from 'node:assert/strict'
const bases=JSON.parse(fs.readFileSync('frontend/src/features/crafting/topBases.json','utf8'))
const additions=Object.values(bases).filter(b=>b.family==='sceptres' && b.key!=='hallowed')
const edit=(p,f)=>{const s=fs.readFileSync(p,'utf8'),n=f(s);assert.notEqual(s,n,p);fs.writeFileSync(p,n)}
edit('backend/src/main/java/com/poe2craft/item/ReviewedSceptres.java',s=>s.replace('Map.of("hallowed", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13")',`Map.ofEntries(Map.entry("hallowed", "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre13"),\n${additions.map(b=>`Map.entry("${b.key}","${b.id}")`).join(',\n')})`).replace('Reviewed representative of the Skeletal Warrior family','Reviewed ordinary Sceptre skill families'))
for(const p of ['frontend/src/features/crafting/CraftingPage.tsx','frontend/src/features/crafting/craftingApi.ts','frontend/src/features/crafting/draft.ts']) edit(p,s=>{
 s=s.replace(/^(\s*)\| 'hallowed'/gm,(_,i)=>`${i}| 'hallowed'\n${additions.map(b=>`${i}| '${b.key}'`).join('\n')}`)
 if(p.endsWith('draft.ts')) s=s.replace(/(\s*hallowed: 'Item[^\n]+,)/,`$1\n${additions.map(b=>`  '${b.key}': 'Item Class: Sceptres\\nRarity: Normal\\n${b.name}',`).join('\n')}`)
 if(p.endsWith('CraftingPage.tsx')) s=s.replace("  hallowed: 'Hallowed_Sceptre',",`  hallowed: 'Hallowed_Sceptre',\n${additions.map(b=>`  '${b.key}': '${b.slug}',`).join('\n')}`)
 return s
})
console.log('Wired seven distinct Sceptre IDs through shared reviewed infrastructure')
