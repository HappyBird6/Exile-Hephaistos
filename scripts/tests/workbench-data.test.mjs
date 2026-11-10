import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { mirrors, synchronize, browserData } from '../sync-workbench-data.mjs'
test('checks every mirror without repairing drift or deleting source data', () => {
 const root=fs.mkdtempSync(path.join(os.tmpdir(),'workbench-data-'))
 try {
  for(const [source,target] of mirrors) {
   const src=path.join(root,'backend/src/main/resources/catalog',source)
   const dst=path.join(root,'frontend/src/features/crafting',target)
   fs.mkdirSync(path.dirname(src),{recursive:true});fs.mkdirSync(path.dirname(dst),{recursive:true})
   const data=source==='base-policies.json'?{legacy:{sapphire:{initialText:'Rarity: Magic'}}}:{season:'next',removed:[],added:['new']}
   fs.writeFileSync(src,JSON.stringify(data));fs.writeFileSync(dst,JSON.stringify(browserData(source,data)))
  }
  synchronize(root)
  const target=path.join(root,'frontend/src/features/crafting/equipmentCurrencies.json')
  fs.writeFileSync(target,'{}')
  assert.throws(()=>synchronize(root),/equipmentCurrencies/)
  assert.equal(fs.readFileSync(target,'utf8'),'{}')
  synchronize(root,true);synchronize(root)
 }finally{fs.rmSync(root,{recursive:true,force:true})}
})
