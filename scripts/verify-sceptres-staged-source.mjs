import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import {execFileSync} from 'node:child_process'
const root='docs/evidence/sceptres-source-bundle-2026-10-05'
const files=fs.readdirSync(root).filter(p=>p.endsWith('.html'))
assert.equal(files.length,37)
const manifest=files.map(name=>{
 const p=`${root}/${name}`,bytes=fs.readFileSync(p),staged=execFileSync('git',['show',`:${p}`],{maxBuffer:4*1024*1024})
 assert(bytes.equals(staged),`Captured source bytes changed in staging: ${name}`)
 const sha256=crypto.createHash('sha256').update(bytes).digest('hex')
 const sourceJson=p.replace(/\.html$/,'.json')
 if(fs.existsSync(sourceJson)) assert.equal(JSON.parse(fs.readFileSync(sourceJson)).sha256,sha256)
 return {file:p,bytes:bytes.length,sha256}
})
fs.writeFileSync('docs/evidence/sceptres-runtime-bundle-2026-10-05/staged-source-preservation.json',JSON.stringify({passed:true,exactHtmlFiles:37,files:manifest},null,2)+'\n',{flag:'wx'})
console.log('All37 captured source HTML files staged with exact original bytes')
