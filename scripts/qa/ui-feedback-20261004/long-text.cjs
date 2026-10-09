const fs=require('fs'),assert=require('assert/strict'),{chromium}=require('/qa/node_modules/playwright');
const messages=require('/source/frontend/src/shared/i18n/messages.json'),terms=require('/source/frontend/src/shared/i18n/gameTerms.json');
(async()=>{
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:390,height:844}}),results=[];
 await page.goto('http://host.docker.internal:19081',{waitUntil:'networkidle'});
 for(const [locale,name] of [['es','Español'],['zh-CN','简体中文 (CN)'],['zh-TW','繁體中文 (TW)'],['ja','日本語']]){
  await page.locator('.locale-trigger').click();await page.getByRole('menuitemradio',{name,exact:true}).click();
  await page.getByRole('tab',{name:messages[locale]['stash.Catalysts'],exact:true}).click();const button=page.locator('[data-material-tooltip]').first();await button.click({button:'right'});
  const notes=page.getByRole('region',{name:messages[locale]['ui.crafting_notes']});await notes.focus();const initial=await notes.evaluate(e=>({scrollTop:e.scrollTop,scrollHeight:e.scrollHeight,clientHeight:e.clientHeight,bottom:e.getBoundingClientRect().bottom+scrollY}));await page.keyboard.press('End');await page.waitForTimeout(300);const end=await notes.evaluate(e=>({scrollTop:e.scrollTop,bottom:e.getBoundingClientRect().bottom+scrollY}));assert(initial.scrollHeight<=initial.clientHeight||end.scrollTop>0,'long notes scroll by keyboard '+locale);assert(Math.abs(initial.bottom-end.bottom)<0.1,'notes scrolling border stable');
  await page.screenshot({path:'/evidence/long-notes-'+locale+'-mobile.png',fullPage:true});await page.keyboard.press('Escape');await page.getByRole('tab',{name:messages[locale]['stash.Currency'],exact:true}).click();const trigger=page.getByRole('button',{name:terms[locale].Orb_of_Transmutation.name,exact:true});await trigger.scrollIntoViewIfNeeded();await trigger.hover();await page.waitForTimeout(100);assert(await page.getByRole('tooltip').isVisible());await page.screenshot({path:'/evidence/tooltip-'+locale+'-mobile-viewport.png',fullPage:false});results.push({locale,initial,end});await page.mouse.move(1,1);await page.waitForTimeout(200);
 }
 fs.writeFileSync('/evidence/long-text-results.json',JSON.stringify(results,null,2));console.log('PASS long text keyboard and tooltip '+results.length);await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
