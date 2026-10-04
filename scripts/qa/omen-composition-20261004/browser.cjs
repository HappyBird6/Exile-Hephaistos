const fs=require('fs'),assert=require('assert/strict'),{chromium}=require('/qa/node_modules/playwright');
let browser,page,lastExchange=null,lastRestore=null;
const checks=[],errors=[];
const check=(name,ok)=>{assert(ok,name);checks.push(name);};
(async()=>{
  const data=JSON.parse(fs.readFileSync('/evidence/browser-fixtures.json','utf8'));
  browser=await chromium.launch({headless:true});
  const context=await browser.newContext({viewport:{width:1440,height:1000}}),p=await context.newPage();
  page=p;
  p.on('pageerror',e=>errors.push(e.message));
  const requests=[];p.on('request',r=>{if(r.url().includes('/api/'))requests.push(r.url());});
  const key='hephaistos.workbench.films.v1',raw=()=>p.evaluate(k=>localStorage.getItem(k),key),history=async()=>JSON.parse(await raw()),active=h=>h.films.find(f=>f.id===h.active);
  await p.goto('http://host.docker.internal:18281',{waitUntil:'networkidle'});
  const films=Object.entries(data.fixtures).flatMap(([base,f])=>{
    const normal={...f.rare,rarity:'NORMAL',explicits:[]};
    const magic={...f.rare,rarity:'MAGIC',explicits:[f.rare.explicits.at(-1)]};
    return [
      {id:base+'-rare',createdAt:'2026-10-04T00:00:00Z',frames:[{state:f.rare,action:null}]},
      {id:base+'-normal',createdAt:'2026-10-04T00:00:00Z',frames:[{state:normal,action:null}]},
      {id:base+'-magic',createdAt:'2026-10-04T00:00:00Z',frames:[{state:magic,action:null}]},
    ];
  });
  await p.evaluate(({key,films})=>localStorage.setItem(key,JSON.stringify({version:1,films,active:'solar-rare',cursor:0})),{key,films});
  await p.reload({waitUntil:'networkidle'});
  await p.getByRole('tab',{name:'Omen',exact:true}).click();
  check('legacy five hidden by default',await p.getByRole('button',{name:'Omen of Sinistral Alchemy',exact:true}).count()===0);
  await p.getByRole('checkbox',{name:'Show legacy Omens',exact:true}).check();
  const names=['Omen of Whittling','Omen of Sinistral Erasure','Omen of Dextral Erasure','Omen of the Blessed','Omen of Sinistral Alchemy','Omen of Dextral Alchemy','Omen of Sinistral Coronation','Omen of Dextral Coronation','Omen of Greater Annulment','Omen of Sinistral Annulment','Omen of Dextral Annulment','Omen of Greater Exaltation','Omen of Homogenising Exaltation'];
  const buttons={};
  for(const [i,name] of names.entries()) {
    await p.getByRole('button',{name,exact:true}).click();
    await p.getByRole('button',{name:`Favorite slot ${i+1}: empty`,exact:true}).click();
    buttons[name]=p.getByRole('button',{name:`Favorite slot ${i+1}: ${name}`,exact:true});
  }
  const isActive=async name=>await buttons[name].getAttribute('aria-pressed')==='true';
  async function activate(name) {if(!await isActive(name))await buttons[name].click({button:'right'});}
  async function deactivate(name) {if(await isActive(name))await buttons[name].click({button:'right'});}
  async function restore(id){
    lastRestore=id;
    const before=await history(),target=before.films.find(f=>f.id===id);assert(target,'known target film');
    if(before.active!==id) {
      await p.getByLabel('Crafting session').selectOption(id);
      await p.waitForFunction(({key,id,last})=>{
        const h=JSON.parse(localStorage.getItem(key));
        return h.active===id&&h.cursor===last&&document.querySelector('.workbench-film-select select').value===id
          &&document.querySelector('.workbench-film-controls span').textContent===`Step ${last} / ${last}`;
      },{key,id,last:target.frames.length-1});
    }
    const previous=p.getByRole('button',{name:'Previous crafting step',exact:true});
    while((await history()).cursor>0) {
      const next=(await history()).cursor-1;
      await previous.click();
      await p.waitForFunction(({key,id,next})=>{
        const h=JSON.parse(localStorage.getItem(key));
        return h.active===id&&h.cursor===next&&document.querySelector('.workbench-film-controls span').textContent===`Step ${next} / ${h.films.find(f=>f.id===id).frames.length-1}`;
      },{key,id,next});
    }
    check(id+' restore reaches exact root',active(await history()).id===id&&(await history()).cursor===0&&!await previous.isEnabled());
  }
  async function use(name) {
    await p.getByRole('tab',{name:'Currency',exact:true}).click();
    await p.getByRole('button',{name,exact:true}).click();
    const response=p.waitForResponse(r=>r.url().endsWith('/workbench/apply'));
    await p.getByRole('button',{name:'Use selected currency on the central item',exact:true}).click();
    const received=await response,r=await received.json();lastExchange={url:received.url(),status:received.status(),request:received.request().postDataJSON(),response:r};
    await p.locator('.workbench-feedback').filter({hasText:r.applied?/applied\. Current item updated/:/Craft blocked by rule/}).waitFor();
    return r;
  }
  for(const [base,f] of Object.entries(data.fixtures)) {
    await restore(base+'-rare');await activate(names[0]);await activate(names[1]);await activate(names[3]);
    const candidates=await p.locator('[data-removal-candidate]').evaluateAll(es=>es.map(e=>e.dataset.removalCandidate));
    check(base+' local candidate set matches backend',JSON.stringify([...candidates].sort())===JSON.stringify([...f.candidates].sort()));
    check(base+' orange candidate CSS',await p.locator('[data-removal-candidate]').first().evaluate(e=>getComputedStyle(e).color)==='rgb(255, 166, 77)');
    const beforeRequests=requests.length;
    await p.getByRole('article',{name:'Item card'}).hover();await p.mouse.move(50,100);await p.mouse.move(60,110);
    check(base+' hover performs zero API calls',requests.length===beforeRequests);
    const original=JSON.stringify(active(await history()).frames[0]);
    const r=await use('Perfect Chaos Orb');
    check(base+' actual pair response contract and consumption',r.applied&&r.consumedOmens.length===2&&!await isActive(names[0])&&!await isActive(names[1])&&await isActive(names[3]));
    const compositionText=await p.getByText(/Unverified omen composition model/).textContent();
    check(base+' composition label does not invent uniform probability',compositionText.includes('UNVERIFIED')&&!compositionText.includes('each candidate = 1/'));
    check(base+' film records one step preserving old frame',active(await history()).frames.length===2&&JSON.stringify(active(await history()).frames[0])===original);
    check(base+' consumed preview clears',await p.locator('[data-removal-candidate]').count()===0);
  }
  await restore('solar-rare');await activate(names[0]);await activate(names[2]);
  check('symmetric Dextral pair active',await isActive(names[0])&&await isActive(names[2]));
  check('symmetric suffix preview',await p.locator('[data-removal-candidate]').evaluateAll(es=>es.every(e=>e.textContent.includes('S'))));
  const dextral=await use('Greater Chaos Orb');check('actual upgraded Dextral pair accepted',dextral.applied&&dextral.consumedOmens.length===2);
  for(const [name,film,currency] of [
    [names[4],'solar-normal','Orb of Alchemy'],[names[5],'solar-normal','Orb of Alchemy'],
    [names[6],'solar-magic','Greater Regal Orb'],[names[7],'solar-magic','Perfect Regal Orb'],
    [names[8],'solar-rare','Orb of Annulment'],
  ]) {
    await restore(film);await activate(name);const r=await use(currency);
    check(name+' actual UI apply accepted',r.applied&&!await isActive(name)&&active(await history()).frames.length>=2);
  }
  await restore('solar-rare');await activate(names[11]);await activate(names[12]);
  const doubled=await use('Perfect Exalted Orb');
  check('preserved Greater plus Homogenising exception accepts tiered UI action',doubled.applied&&doubled.consumedOmens.length===2&&!await isActive(names[11])&&!await isActive(names[12]));
  await restore('solar-rare');await activate(names[1]);await activate(names[2]);
  check('opposing side conflict stays inactive',await isActive(names[1])&&!await isActive(names[2]));
  check('opposing side distinction announced',await p.getByRole('status').textContent().then(t=>t.includes('Conflicting prefix/suffix')));
  await deactivate(names[1]);await activate(names[0]);await activate(names[1]);
  await p.setViewportSize({width:390,height:844});
  check('390px document has no horizontal overflow',await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
  await p.screenshot({path:'/evidence/narrow.png',fullPage:true});
  await p.setViewportSize({width:1440,height:1000});await p.screenshot({path:'/evidence/desktop.png',fullPage:true});
  const saved=await raw();await p.reload({waitUntil:'networkidle'});
  check('film and evidence reload byte-exactly',await raw()===saved);
  check('no page errors',errors.length===0);
  fs.writeFileSync('/evidence/browser-results.json',JSON.stringify({checks,passed:checks.length,errors,requests:requests.length},null,2));
  console.log(JSON.stringify({passed:checks.length,errors}));await browser.close();
})().catch(async e=>{
  let ui=null;if(page)try{ui=await page.evaluate(()=>({films:localStorage.getItem('hephaistos.workbench.films.v1'),selectedFilm:document.querySelector('.workbench-film-select select')?.value,step:document.querySelector('.workbench-film-controls span')?.textContent,previousDisabled:document.querySelector('[aria-label="Previous crafting step"]')?.disabled,status:document.querySelector('[role="status"]')?.textContent}));}catch{}
  fs.writeFileSync('/evidence/browser-resumption-failure.json',JSON.stringify({checks,errors,lastRestore,lastExchange,ui,error:String(e)},null,2));console.error(e);if(browser)await browser.close();process.exit(1)
});
