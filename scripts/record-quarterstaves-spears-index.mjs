import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import {execFileSync} from 'node:child_process'
const root='docs/evidence/quarterstaves-spears-source-bundle-2026-10-05'
const files=fs.readdirSync(root).filter(f=>f.endsWith('.html')).map(file=>{
 const indexed=execFileSync('git',['show',`:${root}/${file}`],{maxBuffer:20*1024*1024})
 const sha256=crypto.createHash('sha256').update(indexed).digest('hex')
 assert.equal(sha256,JSON.parse(fs.readFileSync(`${root}/${file.replace('.html','.json')}`,'utf8')).sha256)
 return {file,sha256}
})
assert.equal(files.length,48)
fs.writeFileSync(`${root}/git-index-proofs.json`,JSON.stringify({passed:true,files},null,2)+'\n',{flag:'wx'})
console.log('48 indexed raw source hashes verified')
