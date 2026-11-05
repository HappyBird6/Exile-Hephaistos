import assert from 'node:assert/strict'
import fs from 'node:fs'
import { chromium } from '../.toolchains/socket-inside-qa/node_modules/playwright/index.mjs'

// Uses only synthetic item storage in fresh browser contexts, never the user's browser.
const origin = process.env.SOCKET_INSIDE_UI ?? 'http://127.0.0.1:19411'
const output = 'docs/verification/socket-inside-20261105'
const historyKey = 'hephaistos.workbench.films.v1'
const localeNames = { en: 'English', ko: '한국어', 'zh-CN': '简体中文 (CN)', 'zh-TW': '繁體中文 (TW)', ja: '日本語', es: 'Español' }
const checks = []
const check = (label, value) => { assert(value, label); checks.push(label) }
fs.mkdirSync(output, { recursive: true })
const browser = await chromium.launch({ headless: true })
let diagnosticPage
async function locale(page, value) {
  if (await page.locator('html').getAttribute('lang') === value) return
  await page.locator('.locale-trigger').click()
  await page.getByRole('menuitemradio', { name: localeNames[value], exact: true }).click()
}
async function chooseRune(page, id = 'Perfect_Desert_Rune') {
  await page.locator('#tab-Augments').click()
  await page.locator('.workbench-stash__search').fill(id.replaceAll('_', ' '))
  return page.locator(`#material-list [data-material-tooltip="${id}"]`)
}
async function beginDrag(page, source, slot) {
  const from = await source.boundingBox(), to = await slot.boundingBox()
  assert(from && to)
  await page.mouse.move(from.x + from.width / 2, from.y + from.height / 2)
  await page.mouse.down()
  await page.mouse.move(from.x + from.width / 2 + 10, from.y + from.height / 2 + 10, { steps: 3 })
  await page.mouse.move(to.x + to.width / 2, to.y + to.height / 2, { steps: 10 })
  await page.mouse.move(to.x + to.width / 2 + 1, to.y + to.height / 2 + 1)
  await page.waitForSelector('.wb-socket.is-drop-target')
}
async function geometry(page, label) {
  const result = await page.locator('.item-placement').evaluate((placement) => {
    const item = placement.querySelector('.item-slot').getBoundingClientRect()
    const slots = [...placement.querySelectorAll('.wb-socket')].map((e) => { const r = e.getBoundingClientRect(); return { x: r.x, y: r.y, width: r.width, height: r.height, inside: r.left > item.left && r.top > item.top && r.right < item.right && r.bottom < item.bottom } })
    return { slots, upperLeft: slots[0].x - item.x < item.width / 3 && slots[0].y - item.y < item.height / 4, nested: Boolean(placement.querySelector('button button')) }
  })
  check(label + ': three sockets inside upper-left frame', result.slots.length === 3 && result.slots.every((s) => s.inside) && result.upperLeft)
  check(label + ': vertical sockets, separate buttons', result.slots.every((s) => s.x === result.slots[0].x) && result.slots[0].y < result.slots[1].y && result.slots[1].y < result.slots[2].y && !result.nested)
  check(label + ': no horizontal overflow', await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  return result
}
const errors = []
try {
  const seedPage = await browser.newPage({ viewport: { width: 1440, height: 960 } })
  diagnosticPage = seedPage
  await seedPage.goto(origin)
  await seedPage.waitForSelector('.item-slot')
  await locale(seedPage, 'en')
  await seedPage.locator('.workbench-item-controls button').first().click()
  await seedPage.locator('.wb-base-picker__filters select').nth(1).selectOption('Body Armours')
  await seedPage.locator('.wb-base-picker__option').first().click()
  await seedPage.locator('.wb-base-picker__footer button').click()
  await seedPage.locator('.wb-base-picker').waitFor({ state: 'hidden' })
  await seedPage.waitForFunction((key) => {
    const history = JSON.parse(localStorage.getItem(key) ?? 'null')
    const film = history?.films.find(f => f.id === history.active)
    return film?.frames[history.cursor]?.state.baseItemId.includes('/BodyArmours/')
  }, historyKey)
  for (let count = 1; count <= 3; count++) {
    await seedPage.waitForFunction(() => !document.querySelector('.item-slot').disabled)
    await seedPage.locator('[data-material-tooltip="Artificers_Orb"]').first().click()
    await seedPage.waitForSelector('.item-slot.is-ready')
    const response = seedPage.waitForResponse((r) => r.url().endsWith('/workbench/apply'))
    await seedPage.locator('.item-slot').click()
    check('seed socket count ' + count, (await (await response).json()).state.augmentSockets === count)
    await seedPage.waitForFunction((n) => document.querySelectorAll('.wb-socket').length === n && !document.querySelector('.item-slot').disabled, count)
  }
  const seed = await seedPage.evaluate((key) => localStorage.getItem(key), historyKey)
  assert(seed)
  await seedPage.context().close()
  const geometries = []
  for (const [language] of Object.entries(localeNames)) for (const width of [1440, 320]) {
    const label = `${language}-${width}`
    const context = await browser.newContext({ viewport: { width, height: width === 320 ? 844 : 960 }, reducedMotion: 'reduce' })
    await context.addInitScript(({ key, value }) => { if (!localStorage.getItem(key)) localStorage.setItem(key, value) }, { key: historyKey, value: seed })
    const page = await context.newPage()
    diagnosticPage = page
    page.on('pageerror', (e) => errors.push(e.message))
    let requests = 0, release = () => {}, pause = false
    await page.route('**/augments/install', async (route) => {
      requests++
      if (pause) await new Promise((resolve) => { release = resolve })
      await route.continue()
    })
    try {
      await page.goto(origin)
      await page.waitForSelector('.wb-sockets')
      await locale(page, language)
      const slots = page.locator('.wb-socket'), slot = slots.first()
      geometries.push({ label, ...await geometry(page, label) })
      check(label + ': default disabled with bright rim', await slot.isDisabled() && await slot.evaluate((e) => getComputedStyle(e).filter === 'brightness(1.25)'))
      check(label + ': default golden item frame', await page.locator('.item-slot').evaluate((e) => getComputedStyle(e).borderTopColor === 'rgb(195, 165, 104)'))
      await page.locator('.item-placement').screenshot({ path: `${output}/${label}-default.png` })
      const source = await chooseRune(page)
      check(label + ': base filter preserved', await page.locator('#material-list [data-material-tooltip="Soul_Core_of_Xopec"]').count() === 0)
      await source.click()
      await page.waitForSelector('.wb-socket.is-available')
      check(label + ': selected eligible Rune gets extra highlight', await page.locator('.wb-socket.is-available').count() === 3 && await page.locator('.item-slot.is-augment-ready').count() === 1)
      await page.keyboard.press('Escape')
      check(label + ': Escape restores default', await page.locator('.wb-socket.is-available').count() === 0 && await slot.isDisabled())
      await beginDrag(page, source, slot)
      await page.locator('.item-placement').screenshot({ path: `${output}/${label}-drag.png` })
      check(label + ': drag highlights actual slot, no animation', await slot.evaluate((e) => getComputedStyle(e).animationName === 'none') && await page.locator('.wb-socket.is-available').count() === 3)
      await page.keyboard.press('Escape')
      await page.mouse.up()
      await page.waitForFunction(() => !document.querySelector('.wb-socket.is-available'))
      check(label + ': native drag cancellation makes no request', requests === 0)
      await beginDrag(page, source, slot)
      pause = true
      const installed = page.waitForResponse((r) => r.url().endsWith('/augments/install'))
      await page.mouse.up()
      await page.waitForFunction(() => [...document.querySelectorAll('.wb-socket')].every((e) => e.disabled))
      check(label + ': busy disables highlighting and central item', await page.locator('.wb-socket.is-available').count() === 0 && await page.locator('.item-slot').isDisabled())
      pause = false; release()
      const result = await installed
      check(label + ': drop performs one slot install', result.status() === 200 && requests === 1 && (await result.json()).state.installedAugments[0].slotIndex === 0)
      await page.waitForSelector('.wb-socket.is-filled img')
      await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
      check(label + ': installed icon retained, available highlight cleared', await page.locator('.wb-socket.is-filled img').count() === 1 && await page.locator('.wb-socket.is-available').count() === 0)
      await page.locator('.item-placement').screenshot({ path: `${output}/${label}-installed.png` })
      if (language === 'en' && width === 1440 || language === 'ko' && width === 320) await page.screenshot({ path: `${output}/${label}-screen.png`, fullPage: true })
      const replacement = await chooseRune(page, 'Perfect_Glacial_Rune')
      await replacement.click()
      await page.waitForSelector('.wb-socket.is-available')
      await page.keyboard.press('Tab')
      await slot.focus()
      check(label + ': keyboard focus remains distinct', await slot.evaluate((e) => e.matches(':focus-visible') && getComputedStyle(e).outlineColor === 'rgb(255, 255, 255)'))
      const replaced = page.waitForResponse((r) => r.url().endsWith('/augments/install'))
      await slot.press('Enter')
      check(label + ': keyboard replacement only changes requested slot', (await replaced).status() === 200 && requests === 2)
      await page.waitForFunction(() => document.activeElement?.matches('.item-slot'))
      await page.reload()
      await page.waitForSelector('.wb-socket.is-filled img')
      check(label + ': reload preserves replaced icon', await slot.locator('img').getAttribute('src') === '/assets/augments/Perfect_Glacial_Rune.webp')
    } catch (error) {
      console.log(label, await page.evaluate(() => ({ active: document.activeElement?.outerHTML, centralDisabled: document.querySelector('.item-slot')?.disabled, notice: document.querySelector('.workbench-feedback-region')?.textContent })))
      await page.screenshot({ path: `${output}/failure.png`, fullPage: true })
      throw error
    } finally { release(); await context.close() }
  }
  const touch = await browser.newContext({ viewport: { width: 320, height: 844 }, isMobile: true, hasTouch: true })
  await touch.addInitScript(({ key, value }) => { if (!localStorage.getItem(key)) localStorage.setItem(key, value) }, { key: historyKey, value: seed })
  const page = await touch.newPage()
  await page.goto(origin)
  await page.waitForSelector('.wb-sockets')
  const rune = await chooseRune(page)
  await rune.tap()
  await page.waitForSelector('.wb-socket.is-available')
  check('touch selection highlights sockets', await page.locator('.wb-socket.is-available').count() === 3)
  const response = page.waitForResponse((r) => r.url().endsWith('/augments/install'))
  await page.locator('.wb-socket').first().tap()
  check('touch installs once', (await response).status() === 200)
  await page.waitForSelector('.wb-socket.is-filled img')
  await page.screenshot({ path: `${output}/touch-320.png`, fullPage: true })
  await touch.close()
  check('no browser exceptions', errors.length === 0)
  fs.writeFileSync(`${output}/checks.json`, JSON.stringify({ origin, checks, geometries, errors, passed: true }, null, 2) + '\n')
  console.log(JSON.stringify({ passed: checks.length, output }))
} catch (error) {
  if (diagnosticPage && !diagnosticPage.isClosed()) {
    console.log(await diagnosticPage.evaluate(() => ({body:document.body.innerText.slice(0,3000), selected:[...document.querySelectorAll('[aria-pressed="true"]')].map(e=>e.getAttribute('aria-label')), central:document.querySelector('.item-slot')?.className})))
    await diagnosticPage.screenshot({path: `${output}/failure.png`, fullPage:true})
  }
  throw error
} finally { await browser.close() }
