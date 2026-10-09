import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'
const base='e80d63fca33b9702b192cf23970b03fb8891eed5'
const root='docs/evidence/base-registry-refactor-2026-10-05'
const hash=b=>crypto.createHash('sha256').update(b).digest('hex')
const paths=execFileSync('git',['ls-tree','-r','--name-only',base,'backend/src/main/resources/catalog','backend/src/main/resources/crafting','frontend/src/shared/i18n','frontend/src/features/crafting/topBases.json','frontend/src/features/crafting/topBaseEssences.json'],{encoding:'utf8'}).trim().split('\n')
const proof=[]
for(const path of paths) {
 const old=execFileSync('git',['show',`${base}:${path}`],{maxBuffer:32*1024*1024})
 const now=fs.readFileSync(path)
 assert(now.equals(old),path+' existing source/catalog/display data changed')
 proof.push({path,sha256:hash(now)})
}
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const initials=JSON.parse(fs.readFileSync(`${q}/baseline125-api-initials.json`))
assert.equal(Object.keys(initials).length,125)
assert.equal(new Set(Object.values(initials).map(i=>i.state.baseItemId)).size,125)
fs.writeFileSync(`${root}/preservation-final.json`,JSON.stringify({passed:true,baseHead:base,totalBases:125,existingFilesUnchanged:proof.length,files:proof},null,2)+'\n',{flag:'wx'})
console.log(proof.length,'existing catalog/registry/i18n source files byte-identical;125 IDs preserved')
