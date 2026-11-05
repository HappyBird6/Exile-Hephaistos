import assert from 'node:assert/strict'
import fs from 'node:fs'
import { chromium } from '../.toolchains/socket-inside-qa/node_modules/playwright/index.mjs'

const origin = process.env.SOCKET_INSIDE_UI ?? 'http://127.0.0.1:19411'
const output = 'docs/verification/socket-inside-20261105'
fs.mkdirSync(output, { recursive: true })
const checks = []
const check = (label, value) => { assert(value, label); checks.push(label) }
const browser = await chromium.launch()
const page = await browser.newPage({viewport:{width:1440,height:960}})
const ids = () => page.locator('.workbench-stash__resources button').evaluateAll((es) => es.map(e=>e.dataset.materialTooltip))
const has = (id) => page.locator(`.workbench-stash__resources [data-material-tooltip="${id}"]`).count()
const toggle = () => page.locator('.workbench-stash__filter input')
async function tab(id) { await page.locator('#tab-'+id).click() }
async function place(itemClass) {
  const before = await page.evaluate(() => JSON.parse(localStorage.getItem('hephaistos.workbench.films.v1') ?? 'null')?.active)
  await page.locator('.workbench-item-controls button').first().click()
  await page.locator('.wb-base-picker__filters select').nth(1).selectOption(itemClass)
  const option = page.locator('.wb-base-picker__option').first()
  await option.click()
  const name = await option.locator('.wb-base-picker__label').evaluate(e=>e.firstChild.textContent.trim())
  await page.locator('.wb-base-picker__footer button').focus()
  await page.locator('.wb-base-picker__footer button').click()
  await page.locator('.wb-base-picker').waitFor({state:'hidden'})
  await page.waitForFunction(({before,name})=>{
    const history=JSON.parse(localStorage.getItem('hephaistos.workbench.films.v1')??'null')
    return history?.active && history.active!==before && document.querySelector('.item-slot > span:last-child')?.textContent.trim()===name && !document.querySelector('.item-slot').disabled
  },{before,name})
}
async function use(id) {
  await page.locator(`[data-material-tooltip="${id}"]`).first().click()
  await page.waitForSelector('.item-slot.is-ready')
  const response = page.waitForResponse(r=>r.url().endsWith('/workbench/apply'))
  await page.locator('.item-slot').click()
  const result=await (await response).json()
  check('rarity operation '+id,result.applied===true)
  await page.waitForFunction(()=>!document.querySelector('.item-slot').disabled && !document.querySelector('.item-slot.is-ready'))
}
async function dense(label) {
  const result=await page.locator('.workbench-stash__resources').evaluate(el=>{
    const rects=[...el.querySelectorAll('button')].map(e=>e.getBoundingClientRect())
    const first=rects.slice(0,4)
    return {count:rects.length,noPlaceholders:!el.querySelector('.essence-row > span'),dense:rects.every((r,i)=>Math.abs(r.x-first[i%4].x)<1 && (i<4||r.y>rects[i-4].y))}
  })
  check(label,result.count>4&&result.dense&&result.noPlaceholders)
}
try {
  await page.goto(origin)
  await page.locator('.locale-trigger').click()
  await page.getByRole('menuitemradio',{name:'English',exact:true}).click()
  check('checkbox is below search and defaults on',await toggle().isChecked()&&await toggle().evaluate(e=>e.closest('label').previousElementSibling.matches('.workbench-stash__search')))
  for(const id of ['Preserved_Cranium','Preserved_Rib','Preserved_Jawbone','Preserved_Collarbone','Armourers_Scrap','Blacksmiths_Whetstone']) check('unselected full Currency: '+id,await has(id)===1)
  await place('Body Armours')
  for(const id of ['Preserved_Rib','Ancient_Rib','Armourers_Scrap'])check('Body Currency included: '+id,await has(id)===1)
  for(const id of ['Preserved_Cranium','Preserved_Jawbone','Preserved_Collarbone','Blacksmiths_Whetstone'])check('Body Currency excluded: '+id,await has(id)===0)
  await tab('Essence')
  check('Body includes Armour-common and Body-only Essence',await has('Greater_Essence_of_Enhancement')===1&&await has('Perfect_Essence_of_the_Body')===1)
  check('Body excludes weapon Essence',await has('Greater_Essence_of_Flames')===0)
  await dense('Body Essence fills rows without filtered gaps')
  const normal=await ids()
  await use('Orb_of_Transmutation')
  check('Magic uses same type filter',JSON.stringify(await ids())===JSON.stringify(normal))
  await use('Regal_Orb')
  check('Rare uses same type filter',JSON.stringify(await ids())===JSON.stringify(normal))
  const search=page.locator('.workbench-stash__search')
  await search.fill('increased Armour, Evasion and Energy Shield')
  check('tooltip search includes Armour-common Essence',await has('Greater_Essence_of_Enhancement')===1)
  await toggle().uncheck()
  check('filter off preserves description search and restores other target',await search.inputValue()==='increased Armour, Evasion and Energy Shield'&&await has('Perfect_Essence_of_Enhancement')===1)
  await toggle().check()
  check('filter on preserves search and reapplies type',await has('Greater_Essence_of_Enhancement')===1&&await has('Perfect_Essence_of_Enhancement')===0)
  await search.fill('')
  await page.screenshot({path:output+'/filter-body-desktop.png',fullPage:true})
  await page.setViewportSize({width:320,height:844})
  await dense('320px Essence remains densely packed')
  check('320px filter has no horizontal overflow',await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth))
  await page.screenshot({path:output+'/filter-body-320.png',fullPage:true})
  await page.setViewportSize({width:1440,height:960})
  await tab('Omen')
  const omens=await ids()
  await toggle().uncheck()
  check('Omen exception independent of type toggle',JSON.stringify(await ids())===JSON.stringify(omens))
  await toggle().check()
  await place('Bows')
  await tab('Currency')
  check('Bow has Jawbone and Whetstone only for relevant families',await has('Preserved_Jawbone')===1&&await has('Blacksmiths_Whetstone')===1&&await has('Preserved_Rib')===0&&await has('Armourers_Scrap')===0)
  await tab('Essence')
  check('Bow changes Essence targets without rarity gating',await has('Greater_Essence_of_Flames')===1&&await has('Greater_Essence_of_Enhancement')===0)
  await dense('Bow Essence packs filtered families')
  await place('Wands')
  await tab('Currency')
  check('Wand has Jawbone but no martial quality or armour quality',await has('Preserved_Jawbone')===1&&await has('Blacksmiths_Whetstone')===0&&await has('Armourers_Scrap')===0)
  await place('Amulets')
  check('Amulet has Collarbone, not armour/weapon quality or Rib',await has('Preserved_Collarbone')===1&&await has('Altered_Collarbone')===1&&await has('Armourers_Scrap')===0&&await has('Preserved_Rib')===0)
  await tab('Augments')
  check('type filter hides Rune on Amulet',await has('Perfect_Desert_Rune')===0)
  await toggle().uncheck()
  check('filter off restores Perfect Rune while lower tiers stay hidden',await has('Perfect_Desert_Rune')===1&&await has('Desert_Rune')===0)
  fs.writeFileSync(output+'/filter-checks.json',JSON.stringify({origin,checks,passed:true},null,2)+'\n')
  console.log(JSON.stringify({passed:checks.length}))
} catch(e) {
  console.log((await page.locator('body').innerText()).slice(0,2600))
  await page.screenshot({path:output+'/filter-failure.png',fullPage:true})
  throw e
} finally {await browser.close()}
