import fs from './evidence-files.cjs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const root='evidence:amulets-source/full-details'
fs.mkdirSync(root,{recursive:true})
const rows=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/solar-amulet/base.raw.json'))
for(let i=0;i<rows.length;i++){
 const path=`${root}/${i}.json`;if(fs.existsSync(path))continue
 const row=rows[i],r=await fetch(row.hover,{headers:{'User-Agent':'Mozilla/5.0',Referer:'https://poe2db.tw/us/Amulets'}});assert(r.ok,row.hover);const html=await r.text()
 fs.writeFileSync(path,JSON.stringify({row,url:row.hover,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),html},null,2)+'\n')
 if(i%25===0)console.log(i)
}
