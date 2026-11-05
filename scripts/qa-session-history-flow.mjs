import assert from 'node:assert/strict'
import fs from 'node:fs'
import { chromium } from '../frontend/node_modules/.history-qa/node_modules/playwright/index.mjs'
const api='http://127.0.0.1:19310', ui='http://127.0.0.1:19311'
const output='docs/verification/session-history-flow-20261105'
fs.mkdirSync(output,{recursive:true})
const checks=[]
const check=(label,value)=>{assert(value,label);checks.push(label)}
const initial=await (await fetch(api+'/api/v1/crafting/workbench/initial?base=soldier&itemLevel=82')).json()
let state={...initial.state,explicits:[],equipmentQuality:initial.equipmentQuality}
const film={id:'qa-history',createdAt:'2026-11-05T00:00:00Z',rulesetIdentity:initial.rulesetIdentity,title:'병사 흉갑 제작',sourceDocument:'Synthetic public fixture\r\nUnknown original line preserved',frames:[{state,action:null,rulesetIdentity:initial.rulesetIdentity}]}
for(const action of ['TRANSMUTATION','AUGMENTATION','REGAL','EXALTED','EXALTED','EXALTED','CHAOS']){
 const r=await fetch(api+'/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json','X-Crafting-Ruleset':initial.rulesetIdentity},body:JSON.stringify({state,action,activeOmens:[]})})
 const result=await r.json();check('craft '+action+' '+film.frames.length,r.ok&&result.applied)
 state=result.state
 film.frames.push({state,action,evidence:result,rulesetIdentity:initial.rulesetIdentity})
}
const repeated=await fetch(api+'/api/v1/crafting/workbench/chaos-repeat',{method:'POST',headers:{'Content-Type':'application/json','X-Crafting-Ruleset':initial.rulesetIdentity},body:JSON.stringify({requestId:crypto.randomUUID(),state:{kind:'CONCRETE',item:state},action:'CHAOS',activeOmens:['Omen_of_Whittling'],pattern:'THIS_PATTERN_NEVER_MATCHES_PUBLIC_FIXTURE',locale:'en',maximumUses:2})})
const batch=await repeated.json();batch.consumedOmens=Object.entries(batch.consumedOmens).flatMap(([id,count])=>Array(count).fill(id));check('actual repeat receipt',repeated.ok&&batch.actualUses===2)
film.frames.push({state:batch.finalState.item,action:'CHAOS',rulesetIdentity:initial.rulesetIdentity,evidence:{...batch.lastAppliedWitness,state:batch.finalState.item,rulesetIdentity:initial.rulesetIdentity,consumedOmens:batch.consumedOmens},chaosRepeat:{version:1,action:'CHAOS',uses:batch.actualUses},chaosRepeatWitness:batch.lastAppliedWitness})
check('actual consumed omen',batch.consumedOmens.includes('Omen_of_Whittling'))
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
try {
 const context=await browser.newContext({viewport:{width:1700,height:1100},deviceScaleFactor:1})
 const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message))
 await page.goto(ui+'/history/shared/'+id)
 await page.waitForSelector('[data-flow-state]:nth-last-child(1)')
 await page.waitForFunction(()=>document.querySelectorAll('[data-flow-state]').length===9&&!document.querySelector('.history-warning'))
 check('no standing implementation notes',await page.locator('main > p').count()===0 && !(await page.locator('main').innerText()).includes('Ruleset'))
 check('only cards and arrows in flow',await page.locator('.session-flow article').count()===9 && await page.locator('[data-flow-edge]').count()===8 && await page.locator('.wb-session-notation').count()===0 && await page.locator('.session-flow details').count()===0)
 await page.getByRole('button',{name:'공유 링크 만들기',exact:true}).click();check('sharing warning only on action',await page.locator('.history-share-confirm').count()===1);await page.locator('.history-share-confirm button').last().click();
 async function capture(label,width,height) {
  await page.setViewportSize({width,height});await page.waitForTimeout(100)
  await page.evaluate(async()=>{await document.fonts.ready;await Promise.all([...document.querySelectorAll('.session-flow img')].map(i=>i.decode().catch(()=>{})))})
  await page.locator('.session-flow').screenshot({path:output+'/'+label+'-page-flow.png'})
  await page.screenshot({path:output+'/'+label+'-page.png',fullPage:true})
  await page.getByRole('button',{name:'PNG 스냅샷 저장',exact:true}).click()
  await page.waitForSelector('.history-pngs a')
  await page.waitForFunction(()=>!document.querySelector('.history-actions button:last-child').disabled)
  const url=await page.locator('.history-pngs a').first().getAttribute('href')
  const bytes=await page.evaluate(async u=>Array.from(new Uint8Array(await(await fetch(u)).arrayBuffer())),url)
  fs.writeFileSync(output+'/'+label+'-snapshot.png',Buffer.from(bytes))
  const geometry=await page.locator('.session-flow').evaluate(root=>{const box=root.getBoundingClientRect();return [...root.querySelectorAll('.session-flow-arrow')].map(svg=>{const r=svg.getBoundingClientRect();return {x:r.x-box.x,y:r.y-box.y,w:r.width,h:r.height,down:r.height>r.width}})})
  const comparison=await page.evaluate(async({source,target,regions})=>{
   async function pixels(url){const image=new Image();image.src=url;await image.decode();const c=document.createElement('canvas');c.width=image.width;c.height=image.height;const g=c.getContext('2d');g.drawImage(image,0,0);return {w:c.width,h:c.height,data:g.getImageData(0,0,c.width,c.height).data}}
   const a=await pixels(source),b=await pixels(target);if(a.w!==b.w||a.h!==b.h)return {sameSize:false,source:[a.w,a.h],snapshot:[b.w,b.h]}
   let difference=0;for(let i=0;i<a.data.length;i++)if(i%4!==3)difference+=Math.abs(a.data[i]-b.data[i])
   function gold(image,r){let count=0;for(let y=Math.max(0,Math.floor(r.y));y<Math.min(image.h,Math.ceil(r.y+r.h));y++){if(r.down&&y>r.y+24&&y<r.y+r.h-20)continue;for(let x=Math.max(0,Math.floor(r.x));x<Math.min(image.w,Math.ceil(r.x+r.w));x++){const k=(y*image.w+x)*4;const red=image.data[k],green=image.data[k+1],blue=image.data[k+2];if(red>150&&green>120&&blue<180&&red>green&&green>blue)count++}}return count}
   return {sameSize:true,width:a.w,height:a.h,meanChannelDifference:difference/(a.w*a.h*3),arrows:regions.map(r=>({source:gold(a,r),snapshot:gold(b,r)}))}
  },{source:'data:image/png;base64,'+fs.readFileSync(output+'/'+label+'-page-flow.png').toString('base64'),target:'data:image/png;base64,'+Buffer.from(bytes).toString('base64'),regions:geometry})
  check(label+' snapshot geometry and colors',comparison.sameSize&&comparison.meanChannelDifference<3)
  check(label+' every arrow retained',comparison.arrows.every(a=>a.source>10&&a.snapshot>a.source*0.5))
  fs.writeFileSync(output+'/'+label+'-comparison.json',JSON.stringify(comparison,null,2))
  check(label+' real snapshot',bytes.length>10000)
  check(label+' no overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth))
 }
 await capture('desktop',1700,1100)
 const positions=await page.locator('[data-flow-state]').evaluateAll(nodes=>nodes.map(n=>({x:n.getBoundingClientRect().x,y:n.getBoundingClientRect().y})))
 check('consumed omen accessible label',await page.locator('[data-flow-edge]').last().getAttribute('aria-label').then(Boolean))
 check('repeat count shown',await page.locator('[data-flow-edge]').last().innerText().then(t=>t.includes('× 2')))
 check('4 column snake',positions[0].x<positions[1].x&&positions[3].x===positions[4].x&&positions[4].x>positions[5].x&&positions[7].x===positions[8].x)
 await capture('mobile',390,844)
 for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']) {
  await page.evaluate(l=>localStorage.setItem('hephaistos.locale.v1',l),locale);await page.reload();await page.waitForSelector('[data-flow-edge]')
  for(const [width,height] of [[1700,1100],[390,844]]) {
   await page.setViewportSize({width,height});await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));check('locale '+locale+' '+width,await page.locator('.session-flow article').count()===9 && await page.locator('[data-flow-edge]').last().innerText().then(t=>t.includes('× 2')))
   check('locale overflow '+locale+' '+width,await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth))
   await page.screenshot({path:output+'/locale-'+locale+'-'+width+'.png',fullPage:true})
  }
 }
 for(const count of [1,4,5,8,9]) {
  await page.route('**/api/v1/session-snapshots/'+id,route=>route.fulfill({json:{...document,film:{...film,frames:film.frames.slice(0,count)}}}))
  await page.reload();await page.waitForSelector('[data-flow-state]');check('state count '+count,await page.locator('[data-flow-state]').count()===count&&await page.locator('[data-flow-edge]').count()===count-1)
  await page.unroute('**/api/v1/session-snapshots/'+id)
 }
 await page.reload();await page.waitForSelector('[data-flow-edge]');await page.setViewportSize({width:900,height:1000});await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));const medium=await page.locator('[data-flow-state]').evaluateAll(ns=>ns.map(n=>n.getBoundingClientRect().x));check('2 column snake',medium[0]<medium[1]&&medium[1]===medium[2]&&medium[2]>medium[3])
 await page.locator('input[type=file]').setInputFiles({name:'session.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify(document))})
 await page.waitForSelector('.history-preview');check('import preview preserves storage',await page.evaluate(()=>localStorage.getItem('hephaistos.workbench.films.v1')===null))
 await page.locator('.history-preview button').first().click();await page.waitForURL('**/history/qa-history')
 check('import keeps exact states and original',await page.evaluate(f=>JSON.stringify(JSON.parse(localStorage.getItem('hephaistos.workbench.films.v1')).films[0])===JSON.stringify(f),film))
 await page.evaluate(()=>localStorage.setItem('hephaistos.locale.v1','ko'));await page.reload();await page.waitForSelector('[data-flow-edge]')
 const download=page.waitForEvent('download');await page.getByRole('button',{name:'세션 내보내기',exact:true}).click();const exported=await download;const raw=JSON.parse(fs.readFileSync(await exported.path(),'utf8'));check('export keeps exact Film',JSON.stringify(raw.film)===JSON.stringify(film))
 await page.getByRole('button',{name:'공유 링크 만들기',exact:true}).click();await page.locator('.history-share-confirm button').first().click();await page.waitForSelector('.history-link a');const sharedUrl=await page.locator('.history-link a').getAttribute('href');const response=await fetch(api+'/api/v1/session-snapshots/'+sharedUrl.split('/').at(-1));check('sharing button saves exact Film',JSON.stringify((await response.json()).film)===JSON.stringify(film))
 const longFilm=structuredClone(film);longFilm.title='병사 흉갑 제작 — 긴 흐름';let longState=longFilm.frames.at(-1).state
 for(let i=0;i<40;i++){const r=await fetch(api+'/api/v1/crafting/workbench/apply',{method:'POST',headers:{'Content-Type':'application/json','X-Crafting-Ruleset':initial.rulesetIdentity},body:JSON.stringify({state:longState,action:'CHAOS',activeOmens:[]})});const result=await r.json();if(!r.ok||!result.applied)throw Error('long fixture');longState=result.state;longFilm.frames.push({state:longState,action:'CHAOS',rulesetIdentity:initial.rulesetIdentity,evidence:result})}
 const longPost=await fetch(api+'/api/v1/session-snapshots',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...document,film:longFilm})});const longId=(await longPost.json()).id
 const high=await browser.newContext({viewport:{width:900,height:1000},deviceScaleFactor:2});const longPage=await high.newPage();await longPage.goto(ui+'/history/shared/'+longId);await longPage.waitForFunction(()=>document.querySelectorAll('[data-flow-state]').length===49&&!document.querySelector('.history-warning'));await longPage.getByRole('button',{name:'PNG 스냅샷 저장',exact:true}).click();await longPage.waitForSelector('.history-pngs a:nth-child(2)');await longPage.waitForFunction(()=>!document.querySelector('.history-actions button:last-child').disabled)
 const longLinks=await longPage.locator('.history-pngs a').evaluateAll(nodes=>nodes.map(n=>n.href));let totalHeight=0
 for(let i=0;i<longLinks.length;i++){const bytes=await longPage.evaluate(async u=>Array.from(new Uint8Array(await(await fetch(u)).arrayBuffer())),longLinks[i]);fs.writeFileSync(output+'/long-snapshot-'+(i+1)+'.png',Buffer.from(bytes));totalHeight+=await longPage.evaluate(async u=>{const image=new Image();image.src=u;await image.decode();return image.height},longLinks[i])}
 const expectedHeight=await longPage.locator('.session-flow').evaluate(n=>(Math.ceil(n.getBoundingClientRect().bottom)-Math.floor(n.getBoundingClientRect().top))*2);check('long flow split preserves total height',Math.abs(totalHeight-expectedHeight)<=longLinks.length)
 await longPage.locator('.session-flow').screenshot({path:output+'/long-page-flow.png'});await high.close()
 check('no browser errors' ,errors.length===0)
 fs.writeFileSync(output+'/checks.json',JSON.stringify({id,checks,errors},null,2))
} finally { await browser.close() }
