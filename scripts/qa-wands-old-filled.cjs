;(async()=>{
 browser=await chromium.launch({headless:true})
 page=await(await browser.newContext({viewport:{width:1440,height:1100}})).newPage()
 page.on('pageerror',e=>errors.push(e.message))
 await page.goto('http://host.docker.internal:20181',{waitUntil:'networkidle'})
 for(const base of ['wand','sceptre','hallowed']){
  const old=oldInitials[base],d=Object.values(old.modifiers).find(d=>d.weight > 0 && d.requiredItemLevel <= 82 && d.stats.length > 1)
  check(base+' real compound legacy fixture',Boolean(d))
  const state={...concrete(old),rarity:'RARE',explicits:[{modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:true}]}
  await seed(state,'old-filled-'+base)
  assert.deepEqual(active(await history()).frames[0].state,state)
  check(base+' legacy rolls/fracture/snapshot preserved',true)
  await page.keyboard.down('Alt')
  await page.keyboard.up('Alt')
  assert.deepEqual(active(await history()).frames[0].state,state)
 }
 check('no browser errors',errors.length === 0)
 fs.writeFileSync('/evidence/old-filled-results.json',JSON.stringify({passed:true,count:checks.length,checks,errors},null,2)+'\n',{flag:'wx'})
 await browser.close()
 console.log(checks.length,'filled legacy film checks passed')
})().catch(async e=>{console.error(e);if(browser)await browser.close();process.exit(1)})
