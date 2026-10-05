import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
const root = 'docs/evidence/belts-source-bundle-2026-10-05'
for (const [name,url] of [['Golden_Obi','https://poe2db.tw/us/Golden_Obi'],['GGG-0.5.0','https://www.pathofexile.com/forum/view-thread/3932540'],['GGG-0.5.5','https://www.pathofexile.com/forum/view-forum/2212']]) {
  const response = await fetch(url); assert(response.ok, `${url}: ${response.status}`)
  const html = await response.text()
  fs.writeFileSync(`${root}/${name}.html`,html,{flag:'wx'})
  fs.writeFileSync(`${root}/${name}.json`,JSON.stringify({url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex')},null,2)+'\n',{flag:'wx'})
}
console.log('Collected unique provenance and current official release checkpoint')
