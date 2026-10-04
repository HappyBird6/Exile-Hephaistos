const fs=require('fs'),assert=require('assert/strict'),{chromium}=require('/qa/node_modules/playwright');
const data=JSON.parse(fs.readFileSync('/evidence/refined-api-results.json')),messages=JSON.parse(fs.readFileSync('/source/frontend/src/shared/i18n/messages.json')),terms=JSON.parse(fs.readFileSync('/source/frontend/src/shared/i18n/gameTerms.json'));
const key='hephaistos.workbench.films.v1',checks=[],errors=[];let browser,page,last;
const check=(n,v)=>{assert(v,n);checks.push(n)};
const ids=['Flesh','Neural','Carapace','Uul-Netols','Xophs','Tuls','Eshs','Chayulas','Reaver','Sibilant','Skittering','Adaptive','Necrotic'].map(s=>'Refined_'+s+'_Catalyst');
const types=['FLESH','NEURAL','CARAPACE','UUL_NETOL','XOPH','TUL','ESH','CHAYULA','REAVER','SIBILANT','SKITTERING','ADAPTIVE','NECROTIC'];
(async()=>{
 browser=await chromium.launch({headless:true});const context=await browser.newContext({viewport:{width:1440,height:1100}});page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>{if(r.url().endsWith('/workbench/apply'))last=JSON.parse(r.postData())});
 await page.goto('http://host.docker.internal:18781',{waitUntil:'networkidle'});await page.locator('.locale-selector select').selectOption('en');
 const history=()=>page.evaluate(k=>JSON.parse(localStorage.getItem(k)),key),active=h=>h.films.find(f=>f.id===h.active),state=async()=>active(await history()).frames.at(-1).state;
 const selectBase=page.locator('.workbench-session-entry button').nth(0),newCraft=page.locator('.workbench-session-entry button').nth(1);
 await selectBase.click();await page.locator('#base-select').selectOption('sapphire');
 check('source rarity selector omits Normal and Unique',JSON.stringify(await page.locator('#sapphire-rarity option').evaluateAll(es=>es.map(e=>e.value)))===JSON.stringify(['MAGIC','RARE']));
 await page.getByLabel(messages.en['ui.sapphire_existing_suffix'],{exact:true}).check();await page.locator('#sapphire-roll').fill('5');
 check('unverified roll blocked',await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).isDisabled());await page.locator('#sapphire-roll').fill('3');
 await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)||'null')?.films.some(f=>f.frames[0].state.baseItemId==='Metadata/Items/Jewels/JewelInt'),key);
 check('source start Magic suffix3',(await state()).rarity==='MAGIC'&&(await state()).explicits[0].values.display_cast_speed_percent===3);
 const sapphireFilm=(await history()).active;
 await page.getByRole('tab',{name:'Catalysts',exact:true}).click();
 const apply=page.locator('.item-slot');
 for(let i=0;i<ids.length;i++){
  const h=await history(),cursor=h.cursor;
  await page.getByRole('button',{name:terms.en[ids[i]].name,exact:true}).click({button:'right'});await apply.click();
  await page.waitForFunction(({key,cursor})=>JSON.parse(localStorage.getItem(key)).cursor===cursor+1,{key,cursor});
  const s=await state();check(types[i]+' applied in browser',s.catalystQuality.type===types[i]&&s.catalystQuality.amount===20);check(types[i]+' original suffix remains3',s.explicits[0].values.display_cast_speed_percent===3);
  check(types[i]+' honest match notice',['SIBILANT','SKITTERING'].includes(types[i])?!await page.getByText(messages.en['notice.catalyst_no_match'],{exact:true}).isVisible():await page.getByText(messages.en['notice.catalyst_no_match'],{exact:true}).isVisible());
 }
 await page.getByRole('button',{name:terms.en.Refined_Skittering_Catalyst.name,exact:true}).click({button:'right'});let cursor=(await history()).cursor;await apply.click({modifiers:['Shift']});await page.waitForFunction(({key,cursor})=>JSON.parse(localStorage.getItem(key)).cursor===cursor+1,{key,cursor});
 cursor=(await history()).cursor;await apply.click({modifiers:['Shift']});await page.waitForFunction(({key,cursor})=>JSON.parse(localStorage.getItem(key)).cursor===cursor+1,{key,cursor});
 check('Shift repeated uses preserve3 and derive4',(await state()).explicits[0].values.display_cast_speed_percent===3&&(await page.locator('.bench-item-card .item-card').innerText()).includes('4% increased Cast Speed'));
 const bytes=JSON.stringify(await history());await page.reload({waitUntil:'networkidle'});check('reload restores Jewel film bytes',JSON.stringify(await history())===bytes);check('reload resolves Sapphire catalog',(await page.locator('.bench-item-card .item-card h2').innerText())==='Sapphire');
 await newCraft.click();await page.waitForFunction(({key,id})=>JSON.parse(localStorage.getItem(key)).active!==id,{key,id:sapphireFilm});check('new craft uses sourced Magic root',(await state()).rarity==='MAGIC'&&(await state()).explicits.length===0);check('old Jewel film retained',(await history()).films.some(f=>f.id===sapphireFilm&&f.frames.length===16));
 await page.locator('.workbench-film-select select').selectOption(sapphireFilm);await page.waitForFunction(({key,id})=>JSON.parse(localStorage.getItem(key)).active===id,{key,id:sapphireFilm});
 for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']){
  await page.locator('.locale-selector select').selectOption(locale);check(locale+' Sapphire source name',await page.locator('.bench-item-card .item-card h2').innerText()===terms[locale].Sapphire.name);
  check(locale+' scope notice',await page.locator('.workbench-feedback').filter({hasText:messages[locale]['notice.sapphire_scope']}).isVisible());check(locale+' catalyst policy disclosed',await page.getByText(messages[locale]['notice.catalyst_policy'],{exact:true}).isVisible());
  for(const width of [1440,390]){await page.setViewportSize({width,height:width===1440?1100:844});check(locale+' '+width+' no overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:'/evidence/'+locale+'-sapphire-'+width+'.png',fullPage:true});}
  const saved=JSON.stringify(await history());await selectBase.click();check(locale+' source base option',await page.locator('#base-select').inputValue()==='sapphire');await page.getByRole('button',{name:messages[locale]['ui.close_item_input'],exact:true}).click();check(locale+' focus restored',await selectBase.evaluate(e=>e===document.activeElement));check(locale+' film unchanged on edit cancel',JSON.stringify(await history())===saved);
 }
 await page.locator('.locale-selector select').selectOption('en');await page.setViewportSize({width:1440,height:1100});
 await selectBase.click();await page.locator('#sapphire-rarity').selectOption('RARE');await page.getByLabel(messages.en['ui.sapphire_existing_suffix'],{exact:true}).uncheck();await page.locator('#starting-quality-type').selectOption('FLESH');await page.locator('#starting-quality-amount').fill('21');
 check('Rare editor rejects quality21',await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).isDisabled());await page.locator('#starting-quality-amount').fill('7');await page.screenshot({path:'/evidence/sapphire-rare-editor.png',fullPage:true});
 const beforeRare=(await history()).active;await page.getByRole('button',{name:messages.en['ui.place_base'],exact:false}).click();await page.waitForFunction(({key,id})=>JSON.parse(localStorage.getItem(key)).active!==id,{key,id:beforeRare});
 check('Rare editor preserves supplied quality7 and empty affixes',(await state()).rarity==='RARE'&&(await state()).explicits.length===0&&(await state()).catalystQuality.type==='FLESH'&&(await state()).catalystQuality.amount===7);
 const h=await history();h.films.push({id:'cap-removal',createdAt:'2026-10-04T00:00:00Z',frames:[{state:data.cap,action:null}]});h.active='cap-removal';h.cursor=0;
 await page.evaluate(({key,h})=>localStorage.setItem(key,JSON.stringify(h)),{key,h});await page.reload({waitUntil:'networkidle'});
 await page.getByRole('tab',{name:'Omen',exact:true}).click();await page.getByRole('button',{name:terms.en.Omen_of_Sinistral_Annulment.name,exact:true}).click();await page.getByRole('button',{name:'Favorite slot 1: empty',exact:true}).click();await page.getByRole('button',{name:'Favorite slot 1: '+terms.en.Omen_of_Sinistral_Annulment.name,exact:true}).click({button:'right'});
 await page.getByRole('tab',{name:'Currency',exact:true}).click();await page.getByRole('button',{name:'Orb of Annulment',exact:true}).click({button:'right'});await apply.click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===1,key);
 const clamped=await history();check('browser cap removal preserves40',(await state()).catalystQuality.amount===40);check('film preserves source before40',active(clamped).frames[0].state.catalystQuality.amount===40);check('film has no obsolete clamp evidence',!active(clamped).frames[1].evidence.assumptions.some(a=>a.id==='unverified-quality-cap-clamp-v1'));
 await page.getByRole('button',{name:messages.en['ui.previous_crafting_step'],exact:true}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===0,key);check('previous frame restores cap40',(await page.locator('.bench-item-card .item-card').innerText()).includes('40%'));
 await page.getByRole('button',{name:messages.en['ui.next_crafting_step'],exact:true}).click();await page.waitForFunction(k=>JSON.parse(localStorage.getItem(k)).cursor===1,key);check('next frame restores preserved40',(await page.locator('.bench-item-card .item-card').innerText()).includes('40%'));
 await page.reload({waitUntil:'networkidle'});check('overcap film reload keeps40',(await state()).catalystQuality.amount===40);
 await page.getByRole('tab',{name:'Catalysts',exact:true}).click();
 for(const [id,type] of [['Flesh_Catalyst','FLESH'],['Neural_Catalyst','NEURAL']]) {const cursor=(await history()).cursor;await page.getByRole('button',{name:terms.en[id].name,exact:true}).click({button:'right'});await apply.click({modifiers:['Shift']});await page.waitForFunction(({key,cursor})=>JSON.parse(localStorage.getItem(key)).cursor===cursor+1,{key,cursor});check(type+' browser reuse/switch preserves40',(await state()).catalystQuality.amount===40&&(await state()).catalystQuality.type===type);check(type+' browser original suffix9 preserved',(await state()).explicits[0].values.additional_strength===9);}
 for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']) {await page.locator('.locale-selector select').selectOption(locale);check(locale+' preserved40 display',(await page.locator('.bench-item-card .item-card').innerText()).includes('40%'));check(locale+' preserved quality notice',await page.locator('.workbench-feedback').filter({hasText:messages[locale]['notice.quality_cap_change']}).isVisible());for(const width of [1440,390]){await page.setViewportSize({width,height:width===1440?1100:844});check(locale+' cap '+width+' no overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await page.screenshot({path:'/evidence/'+locale+'-preserved-cap-'+width+'.png',fullPage:true});}}
 await page.screenshot({path:'/evidence/cap-preserved-film.png',fullPage:true});check('zero page errors',errors.length===0);
 fs.writeFileSync('/evidence/refined-browser-results.json',JSON.stringify({checks,errors,last},null,2));console.log('PASS '+checks.length+' refined browser assertions');await browser.close();
})().catch(async e=>{fs.writeFileSync('/evidence/refined-browser-failure.json',JSON.stringify({error:e.message,checks,errors,last},null,2));if(page)await page.screenshot({path:'/evidence/refined-browser-failure.png',fullPage:true}).catch(()=>{});if(browser)await browser.close();console.error(e);process.exit(1)});
