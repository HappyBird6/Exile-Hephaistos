const fs=require('fs'),assert=require('assert/strict'),{chromium}=require('/qa/node_modules/playwright');
const messages=require('/source/frontend/src/shared/i18n/messages.json'),terms=require('/source/frontend/src/shared/i18n/gameTerms.json');
const key='hephaistos.workbench.films.v1',checks=[],errors=[];let browser,page;
function check(n,v){assert(v,n);checks.push(n)}
(async()=>{
browser=await chromium.launch({headless:true});page=await(await browser.newContext({viewport:{width:1440,height:1100}})).newPage();page.on('pageerror',e=>errors.push(e.message));
await page.goto('http://host.docker.internal:18781',{waitUntil:'networkidle'});await page.locator('.locale-selector select').selectOption('en');
const hist=()=>page.evaluate(k=>JSON.parse(localStorage.getItem(k)),key);
const current=async()=>{const h=await hist();return h.films.find(f=>f.id===h.active).frames[h.cursor].state};
const selectBase=page.locator('.workbench-session-entry button').nth(0);
await selectBase.click();await page.locator('#base-select').selectOption('sapphire');await page.locator('#sapphire-rarity').selectOption('RARE');
await page.getByLabel(messages.en['ui.sapphire_existing_suffix'],{exact:true}).check();await page.locator('#sapphire-roll').fill('5');
check('invalid roll refused',await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).isDisabled());await page.locator('#sapphire-roll').fill('3');
await page.locator('#starting-quality-type').selectOption('SIBILANT');await page.locator('#starting-quality-amount').fill('20');
await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)||'null')?.films.some(f=>f.frames[0].state.baseItemId==='Metadata/Items/Jewels/JewelInt'),key);
check('starting original3', (await current()).explicits[0].values.display_cast_speed_percent===3);
await page.getByRole('tab',{name:'Currency',exact:true}).click();
async function apply(id){const cursor=(await hist()).cursor;await page.getByRole('button',{name:terms.en[id].name,exact:true}).click({button:'right'});await page.locator('.item-slot').click({modifiers:['Shift']});await page.waitForFunction(({key,cursor})=>JSON.parse(localStorage.getItem(key)).cursor===cursor+1,{key,cursor});}
await apply('Divine_Orb');check('Divine source range',[2,3,4].includes((await current()).explicits[0].values.display_cast_speed_percent));check('Divine quality preserved',(await current()).catalystQuality.amount===20);
await page.locator('.item-slot').click({modifiers:['Shift']});await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===2,key);check('Shift repeat Divine recorded',(await hist()).cursor===2);
await page.keyboard.down('Alt');check('Alt source bounds',/2.*4/.test(await page.locator('.bench-item-card .item-card__modifier-detail summary').innerText()));await page.keyboard.up('Alt');
await page.getByRole('tab',{name:'Omen',exact:true}).click();await page.getByRole('button',{name:terms.en.Omen_of_Dextral_Annulment.name,exact:true}).click();await page.getByRole('button',{name:'Favorite slot 1: empty',exact:true}).click();await page.getByRole('button',{name:'Favorite slot 1: '+terms.en.Omen_of_Dextral_Annulment.name,exact:true}).click({button:'right'});
await page.getByRole('button',{name:terms.en.Omen_of_Whittling.name,exact:true}).click();await page.getByRole('button',{name:'Favorite slot 2: empty',exact:true}).click();await page.getByRole('button',{name:'Favorite slot 2: '+terms.en.Omen_of_Whittling.name,exact:true}).click({button:'right'});
await page.getByRole('tab',{name:'Currency',exact:true}).click();await page.getByRole('button',{name:terms.en.Orb_of_Annulment.name,exact:true}).click({button:'right'});check('local Omen removal candidate',await page.locator('.bench-item-card [data-removal-candidate]').count()===1);await page.screenshot({path:'/evidence/omen-candidate.png',fullPage:true});
await apply('Orb_of_Annulment');check('Annulment removed suffix',(await current()).explicits.length===0);check('Annulment kept quality',(await current()).catalystQuality.amount===20);
const bytes=JSON.stringify(await hist());await page.reload({waitUntil:'networkidle'});check('film reload exact',JSON.stringify(await hist())===bytes);
await page.getByRole('button',{name:messages.en['ui.previous_crafting_step'],exact:true}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===2,key);check('undo restores suffix',(await current()).explicits.length===1);
await page.getByRole('button',{name:messages.en['ui.next_crafting_step'],exact:true}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===3,key);check('redo removes suffix',(await current()).explicits.length===0);
for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']){
await page.locator('.locale-selector select').selectOption(locale);check(locale+'scope truthful',await page.locator('.workbench-feedback').filter({hasText:messages[locale]['notice.sapphire_scope']}).isVisible());
for(const width of [1440,390]){await page.setViewportSize({width,height:width===1440?1100:844});check(locale+width+'no overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:'/evidence/'+locale+'-'+width+'.png',fullPage:true});}
check(locale+'quality retained',(await current()).catalystQuality.amount===20);
}
check('zero page errors',errors.length===0);fs.writeFileSync('/evidence/browser-results.json',JSON.stringify({passed:checks.length,checks,errors},null,2));console.log('PASS '+checks.length);await browser.close();
})().catch(async e=>{fs.writeFileSync('/evidence/browser-failure.json',JSON.stringify({error:e.stack,checks,errors},null,2));if(page)await page.screenshot({path:'/evidence/browser-failure.png',fullPage:true}).catch(()=>{});if(browser)await browser.close();console.error(e);process.exit(1)});
