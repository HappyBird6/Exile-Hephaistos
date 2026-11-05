import assert from 'node:assert/strict'
import fs from 'node:fs'
import { chromium } from '../frontend/node_modules/.history-qa/node_modules/playwright/index.mjs'
const api='http://127.0.0.1:19310', ui='http://127.0.0.1:19311'
const output='docs/verification/session-history-20261105'
fs.mkdirSync(output,{recursive:true})
const checks=[]
const check=(label,value)=>{assert(value,label);checks.push(label)}
const initial=await (await fetch(api+'/api/v1/crafting/workbench/initial?base=soldier&itemLevel=82')).json()
let state={...initial.state,explicits:[],equipmentQuality:initial.equipmentQuality}
const film={id:'qa-history',createdAt:'2026-11-05T00:00:00Z',rulesetIdentity:initial.rulesetIdentity,title:'Session history · synthetic public item',sourceDocument:'Synthetic public fixture\r\nUnknown original line preserved',frames:[{state,action:null,rulesetIdentity:initial.rulesetIdentity}]}
for(const action of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','EXALTED','EXALTED','CHAOS','CHAOS']){
 const r=await fetch(api+'/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json','X-Crafting-Ruleset':initial.rulesetIdentity},body:JSON.stringify({state,action,activeOmens:[]})})
 const result=await r.json();check('craft '+action+' '+film.frames.length,r.ok&&result.applied)
 state=result.state
 film.frames.push({state,action,evidence:result,rulesetIdentity:initial.rulesetIdentity})
}
const document={format:'exile-session',version:1,exportedAt:'2026-11-05T00:00:00Z',film}
const response=await fetch(api+'/api/v1/session-snapshots',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(document)})
if(response.status!==201) console.log('publish response',response.status,await response.text());check('publish',response.status===201)
const {id}=await response.json()
check('exact shared bytes',JSON.stringify(await(await fetch(api+'/api/v1/session-snapshots/'+id)).json())===JSON.stringify(document))
check('unavailable', (await fetch(api+'/api/v1/session-snapshots/'+'0'.repeat(48))).status===404)
check('oversize response',(await fetch(api+'/api/v1/session-snapshots',{method:'POST',headers:{'Content-Type':'application/json'},body:' '.repeat(2000001)})).status===413)
check('unsafe keys',(await fetch(api+'/api/v1/session-snapshots',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(document).replace('\"film\":','\"__proto__\":{},\"film\":')})).status===422)
check('invalid schema',(await fetch(api+'/api/v1/session-snapshots',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...document,version:2})})).status===422)
const browser=await chromium.launch({headless:true,channel:'msedge'})
try{
 const context=await browser.newContext({viewport:{width:1700,height:1100}})
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message))
 await page.goto(ui+'/history/shared/'+id)
 await page.waitForSelector('.history-cell:nth-child(9)')
 await page.waitForFunction(()=>!document.querySelector('.history-warning'))
 check('shared works with empty browser storage',await page.evaluate(()=>localStorage.getItem('hephaistos.workbench.films.v1')===null))
 await page.getByRole('button',{name:'공유 링크 만들기',exact:true}).click()
 await page.waitForSelector('.history-link a')
 const generated=await page.locator('.history-link a').getAttribute('href')
 const generatedId=generated.split('/').at(-1)
 check('sharing button creates data-backed link',JSON.stringify((await(await fetch(api+'/api/v1/session-snapshots/'+generatedId)).json()).film)===JSON.stringify(film))
 const positions=await page.locator('.history-cell').evaluateAll(nodes=>nodes.map(n=>({x:n.getBoundingClientRect().x,y:n.getBoundingClientRect().y})))
 check('4 columns snake',positions[0].x<positions[1].x&&positions[3].x===positions[4].x&&positions[4].x>positions[5].x&&positions[7].x===positions[8].x)
 await page.screenshot({path:output+'/desktop.png',fullPage:true})
 await page.getByRole('button',{name:'PNG 스냅샷 저장',exact:true}).click()
 await page.waitForSelector('.history-pngs a:nth-child(3)')
 const png=await page.locator('.history-pngs a').first().getAttribute('href')
 const bytes=await page.evaluate(async url=>Array.from(new Uint8Array(await(await fetch(url)).arrayBuffer())),png)
 fs.writeFileSync(output+'/snapshot-1.png',Buffer.from(bytes));check('three PNG pages',await page.locator('.history-pngs a').count()===3)
 await page.setViewportSize({width:900,height:1000})
 const medium=await page.locator('.history-cell').evaluateAll(nodes=>nodes.map(n=>n.getBoundingClientRect().x))
 check('2 column snake',medium[0]<medium[1]&&medium[1]===medium[2]&&medium[2]>medium[3])
 await page.setViewportSize({width:390,height:844})
 check('mobile no horizontal overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth))
 await page.screenshot({path:output+'/mobile.png',fullPage:true})
 for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']){
  await page.evaluate(l=>{localStorage.setItem('hephaistos.locale.v1',l)},locale)
  await page.reload();await page.waitForSelector('.history-cell:nth-child(9)')
  check('locale '+locale,await page.locator('h1').innerText()!=='')
 }
 // Import preview and commit, without touching a real user's browser storage.
 await page.locator('input[type=file]').setInputFiles({name:'session.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(document))})
 await page.waitForSelector('.history-preview')
 check('preview preserves storage',await page.evaluate(()=>localStorage.getItem('hephaistos.workbench.films.v1')===null))
 await page.locator('.history-preview button').first().click();await page.waitForURL('**/history/qa-history')
 check('import retains all frames',await page.evaluate(()=>JSON.parse(localStorage.getItem('hephaistos.workbench.films.v1')).films[0].frames.length===9))
 check('no browser errors',errors.length===0)
 fs.writeFileSync(output+'/checks.json',JSON.stringify({checks,errors},null,2))
}finally{await browser.close()}
