import fs from 'node:fs'
import assert from 'node:assert/strict'
const qa='E:/WORK/Exile-Hephaistos/codex/boots-qa-20261004'
const services=fs.readFileSync(qa+'/services-after-release-2.txt','utf8').replace(/^\uFEFF/,'').trim().split(/\r?\n/).map(line=>{
 const [Names,Ports,Image,ID]=line.split('|')
 return {Names,Ports,Image,ID}
})
assert.equal(services.length,4)
assert(services.every(s=>s.Names.startsWith('exile-workbench-qa-20261002-')))
assert(services.some(s=>s.Ports.includes('127.0.0.1:18080')))
assert(services.some(s=>s.Ports.includes('127.0.0.1:18081')))
const owner=JSON.parse(fs.readFileSync(qa+'/heavy-qa-owner.json','utf8'))
assert(owner.active)
const release={...owner,active:false,releasedAt:new Date().toISOString(),normalComposeDown:true,existingServices:services,volumesRemoved:false}
assert(!fs.existsSync(qa+'/qa-release.json'))
fs.writeFileSync(qa+'/qa-release.json',JSON.stringify(release,null,2)+'\n')
fs.writeFileSync(qa+'/heavy-qa-owner.json',JSON.stringify(release,null,2)+'\n')
console.log('Boots QA released; four existing services retained')
