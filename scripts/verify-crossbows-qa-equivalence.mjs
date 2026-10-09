import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import {execFileSync} from 'node:child_process'

const qa='E:/WORK/Exile-Hephaistos/codex/crossbows-qa-20261005'
const lines=args=>execFileSync('git',args,{encoding:'utf8'}).trim().split('\n').filter(Boolean)
const files=[...lines(['diff','--name-only','HEAD','--','backend','frontend']),...lines(['ls-files','--others','--exclude-standard','--','backend','frontend'])].sort()
const hashes={}
for(const p of files){
 const [module,...rest]=p.split('/')
 const tested=`${qa}/${module}-check/${rest.join('/')}`
 const committed=fs.readFileSync(p)
 assert(committed.equals(fs.readFileSync(tested)),`Final runtime input differs from tested QA copy: ${p}`)
 hashes[p]=crypto.createHash('sha256').update(committed).digest('hex')
}
fs.writeFileSync('docs/evidence/crossbows-source-bundle-2026-10-05/qa-equivalence.json',JSON.stringify({passed:true,files:files.length,qa,hashes},null,2)+'\n',{flag:'wx'})
console.log('Exact tested/runtime input equivalence:',files.length,'files')
