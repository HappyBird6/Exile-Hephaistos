const fs = require('node:fs'), assert = require('node:assert/strict')
const { chromium } = require('/qa/node_modules/playwright')
const terms = require('/source/frontend/src/shared/i18n/gameTerms.json')
const bases = require('/source/frontend/src/features/crafting/topBases.json')
const initials = JSON.parse(fs.readFileSync('/evidence/api-initials.json'))
const oldInitials = JSON.parse(fs.readFileSync('/evidence/baseline125-api-initials.json'))
const key = 'hephaistos.workbench.films.v1', checks = [], errors = [], captures = []
const check = (name, value) => { assert(value, name); checks.push(name) }
const locales = ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es']
fs.mkdirSync('/evidence/cards', { recursive: true })
const localeNames = { en: 'English', ko: '한국어', 'zh-CN': '简体中文 (CN)', 'zh-TW': '繁體中文 (TW)', ja: '日本語', es: 'Español' }
let browser, page
const history = () => page.evaluate((k) => JSON.parse(localStorage.getItem(k)), key)
const raw = () => page.evaluate((k) => localStorage.getItem(k), key)
const active = (h) => h.films.find((f) => f.id === h.active)
async function locale(l) {
  await page.locator('.locale-trigger').click()
  await page.getByRole('menuitemradio', { name: localeNames[l], exact: true }).click()
  await page.waitForFunction((l) => document.documentElement.lang === l, l)
}
async function place(base) {
  const before = (await history())?.films.length ?? 0
  await page.locator('.workbench-session-entry button').first().click()
  check('125 base selector choices', await page.locator('#base-select option').count() === 125)
  await page.locator('#base-select').selectOption(base)
  await page.locator('#base-level').fill('82')
  await page.locator('dialog[open] .base-form .primary-action').click()
  await page.waitForFunction(({ key, before }) => JSON.parse(localStorage.getItem(key))?.films.length > before, { key, before })
  await page.waitForSelector('.bench-item-card')
}
async function use(id, shift = false) {
  await page.locator(id.includes('Essence') ? '#tab-Essence' : id.includes('Catalyst') ? '#tab-Catalysts' : '#tab-Currency').click()
  const button = page.getByRole('button', { name: terms.en[id].name, exact: true })
  await button.click({ button: id.includes('Essence') || id.includes('Catalyst') ? 'right' : 'left' })
  const response = page.waitForResponse((r) => r.url().endsWith('/workbench/apply'))
  await page.locator('.item-slot').click({ modifiers: shift ? ['Shift'] : [] })
  const r = await response, result = await r.json()
  check(id + ' HTTP200', r.status() === 200)
  await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  if (result.applied) await page.waitForFunction(({ key, count }) => {
    const h = JSON.parse(localStorage.getItem(key)), f = h.films.find((f) => f.id === h.active)
    return f.frames[h.cursor].state.explicits.length === count
  }, { key, count: result.state.explicits.length })
  captures.push(result)
  return result
}
async function seed(state, id) {
  const h = { version: 1, films: [{ id, createdAt: '2026-10-04T00:00:00Z', frames: [{ state, action: null }] }], active: id, cursor: 0 }
  await page.evaluate(({ key, h }) => localStorage.setItem(key, JSON.stringify(h)), { key, h })
  await page.reload({ waitUntil: 'networkidle' })
  await page.waitForSelector('.bench-item-card')
}
const concrete = (i) => { const s = { ...i.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete s.modifierIds; return s }
;(async()=>{
 browser=await chromium.launch({headless:true})
 page=await(await browser.newContext({viewport:{width:1440,height:1100}})).newPage()
 page.on('pageerror',e=>errors.push(e.message))
 await page.goto('http://frontend:8080',{waitUntil:'networkidle'})
 for(const base of ["solar","stocky","sapphire","time-lost-sapphire","soldier","imperial","massive","freebooter","tasalian","warmonger","kinetic","stellar","hallowed","bone","linen-belt","siege-crossbow","tawhoan-tower-shield","desert-buckler","tasalian-focus","visceral-quiver","fortified-hammer","ruination-maul","aegis-quarterstaff","grand-spear"]){
  const old=oldInitials[base],d=Object.values(old.modifiers).find(d=>d.weight > 0 && d.requiredItemLevel <= 82 && d.stats.length >= 1)
  check(base+' real sourced legacy fixture',Boolean(d))
  const state={...concrete(old),rarity:'RARE',explicits:[{modifierId:d.id,values:Object.fromEntries(d.stats.map(s=>[s.id,s.min])),fractured:true}]}
  await seed(state,'old-filled-'+base)
  assert.deepEqual(active(await history()).frames[0].state,state)
  check(base+' legacy rolls/fracture/snapshot preserved',true)
  for(const l of locales) {
    await locale(l)
    assert.deepEqual(active(await history()).frames[0].state,state)
    await page.locator('.bench-item-card').screenshot({path:'/evidence/cards/legacy-'+base+'-'+l+'.png'})
    if(l==='en')await page.screenshot({path:'/evidence/legacy-'+base+'-'+l+'.png',fullPage:true})
    check(base+' '+l+' legacy film and screenshot',true)
  }
  await page.keyboard.down('Alt')
  await page.keyboard.up('Alt')
  assert.deepEqual(active(await history()).frames[0].state,state)
 }
 check('no browser errors',errors.length === 0)
 fs.writeFileSync('/evidence/old-filled-results.json',JSON.stringify({passed:true,count:checks.length,checks,errors},null,2)+'\n',{flag:'wx'})
 await browser.close()
 console.log(checks.length,'filled legacy film checks passed')
})().catch(async e=>{console.error(e);if(browser)await browser.close();process.exit(1)})
