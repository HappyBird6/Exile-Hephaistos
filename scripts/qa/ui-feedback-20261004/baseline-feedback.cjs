const fs=require('fs'),{chromium}=require('/qa/node_modules/playwright');
(async()=>{
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:1440,height:1400}});
 await page.goto('http://host.docker.internal:19081',{waitUntil:'networkidle'});await page.locator('.locale-selector select').selectOption('es');
 const measure=()=>page.evaluate(()=>Object.fromEntries(['.stash-panel','.workbench-feedback-panel','.stash-canvas'].map(s=>{const e=document.querySelector(s),r=e.getBoundingClientRect();return[s,{bottom:r.bottom+scrollY,height:r.height,margin:getComputedStyle(e).marginBottom}]})));
 await page.getByRole('tab',{name:'Catalizadores',exact:true}).click();const before=await measure();await page.locator('[data-material-tooltip]').first().click({button:'right'});await page.waitForTimeout(300);const selected=await measure();await page.keyboard.press('Escape');await page.waitForTimeout(300);const cleared=await measure();
 fs.writeFileSync('/evidence/baseline-feedback-results.json',JSON.stringify({before,selected,cleared},null,2));console.log({before,selected,cleared});await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
