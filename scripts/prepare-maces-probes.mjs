import fs from 'node:fs'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005',previous='E:/WORK/Exile-Hephaistos/codex/quivers-qa-20261005'
const write=(p,s)=>fs.writeFileSync(`${q}/${p}`,s,{flag:'wx'})
let api=fs.readFileSync(`${previous}/qa-api-10.mjs`,'utf8')
 .replace("b.family === 'quivers'","['one-hand-maces','two-hand-maces'].includes(b.family)")
 .replace("root.baseItemId.includes('/Quivers/')","root.baseItemId.includes(base.family==='one-hand-maces'?'/OneHandMaces/':'/TwoHandMaces/')")
 .replace('const expected = 100','const expected = 150')
 .replace('quiver-workbench-v1','maces-workbench-v1')
 .replace("check(key+' no Quiver quality cap',(await apply(root,'TRANSMUTATION')).qualityLimit === null)","check(key+' source quality maximum20',(await apply(root,'TRANSMUTATION')).qualityLimit.maximumQuality === 20)")
 .replaceAll('Quiver class','Mace class').replaceAll('Quiver rule','Mace rule')
api=api.replace("  const magic =",`  const raw=JSON.parse(fs.readFileSync('/source/docs/evidence/maces-source-bundle-2026-10-05/'+(base.family==='one-hand-maces'?'One_Hand_Maces':'Two_Hand_Maces')+'.us.json')).data.normal
  const details=JSON.parse(fs.readFileSync('/source/backend/src/main/resources/catalog/'+base.pool+'/details.raw.json')).ordinary
  for(let index=0;index<raw.length;index++) {
    const row=raw[index],proof=details[index]
    const d=Object.values(i.modifiers).find(d=>d.weight>0&&d.requiredItemLevel===+row.Level&&JSON.stringify([...d.familyIds].sort())===JSON.stringify([...row.ModFamilyList].sort())&&JSON.stringify(d.stats)===JSON.stringify(proof.stats.map(({locality,...s})=>s)))
    check(key+' source complete row '+index,Boolean(d))
    check(key+' source weight '+index,d.weight===+row.DropChance)
  }
  const magic =`)
write('qa-api-1.mjs',api)
let browser=fs.readFileSync(`${previous}/qa-browser-10.cjs`,'utf8')
 .replaceAll('113 base selector choices','119 base selector choices').replaceAll('=== 113','=== 119')
 .replace("b.family === 'quivers'","['one-hand-maces','two-hand-maces'].includes(b.family)")
 .replace("=== 'Quivers'","=== (b.family==='one-hand-maces'?'One Hand Maces':'Two Hand Maces')")
 .replaceAll('visceral-quiver','strife-pick').replaceAll('Quiver class','Mace class').replaceAll('unmodified Quiver property','source weapon property')
write('qa-browser-1.cjs',browser)
write('qa-old-filled-1.cjs',fs.readFileSync(`${previous}/qa-old-filled-10.cjs`,'utf8').replaceAll('113 base selector choices','119 base selector choices').replaceAll('=== 113','=== 119'))
fs.copyFileSync(`${previous}/contact-sheets.cjs`,`${q}/contact-sheets.cjs`)
console.log('Mace probes with complete pool/weight, positive/negative paths and locale pixels')
