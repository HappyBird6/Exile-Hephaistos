const fs=require('fs'),assert=require('node:assert/strict'),{chromium}=require('/qa/node_modules/playwright')
const terms=require('/source/frontend/src/shared/i18n/gameTerms.json')
;(async()=>{
 const browser=await chromium.launch(),context=await browser.newContext(),page=await context.newPage(),checks=[]
 await page.goto('http://host.docker.internal:19181',{waitUntil:'networkidle'})
 for(const l of ['en','ko','zh-CN','zh-TW','ja','es'])for(const width of [1440,390]){
  await page.evaluate(l=>localStorage.setItem('hephaistos.locale.v1',l),l)
  await page.setViewportSize({width,height:width===1440?1100:844})
  await page.reload({waitUntil:'networkidle'});await page.locator('#tab-Liquid_Emotions').click()
  const term=terms[l].Ancient_Liquid_Paranoia
  await page.locator('.material-search').fill(term.name)
  const button=page.getByRole('button',{name:term.name,exact:true})
  await button.scrollIntoViewIfNeeded();await button.hover()
  const tip=page.getByRole('tooltip');await tip.waitFor({state:'visible'})
  const bounds=await tip.boundingBox();assert(bounds.x>=0&&bounds.x+bounds.width<=width+1)
  assert((await tip.innerText()).includes(term.lines[0]))
  await page.screenshot({path:'/evidence/tooltip-layout-'+l+'-'+width+'.png',fullPage:false})
  checks.push({locale:l,width,bounds})
 }
 fs.writeFileSync('/evidence/tooltip-layout-results.json',JSON.stringify(checks,null,2));await browser.close();console.log('PASS '+checks.length)
})().catch(e=>{console.error(e);process.exit(1)})
