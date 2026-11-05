import assert from 'node:assert/strict'
import fs from 'node:fs'
import { chromium } from '../frontend/node_modules/.socket-qa/node_modules/playwright/index.mjs'

// Dedicated browser context and synthetic public items only; no personal storage.
const ui = process.env.SOCKET_QA_UI ?? 'http://127.0.0.1:19011'
const api = process.env.SOCKET_QA_ORIGIN ?? 'http://127.0.0.1:19010'
const output = 'docs/verification/socket-reference-20261105'
fs.mkdirSync(output, { recursive: true })
const checks = []
const check = (label, value) => { assert(value, label); checks.push(label) }
const catalog = JSON.parse(fs.readFileSync('frontend/src/features/crafting/augments.json'))
const initialResponse = await fetch(api + '/api/v1/crafting/workbench/initial?base=stocky&itemLevel=82')
assert.equal(initialResponse.status, 200)
const initial = await initialResponse.json()
const root = { snapshotId: initial.state.snapshotId, baseItemId: initial.state.baseItemId, itemLevel: 82, rarity: initial.state.rarity, implicits: initial.state.implicits, explicits: [], conditions: [], augmentSockets: 2, equipmentQuality: initial.equipmentQuality }
async function install(state, id, slot = 0) {
  const response = await fetch(api + '/api/v1/crafting/workbench/augments/install', {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Crafting-Ruleset': initial.rulesetIdentity },
    body: JSON.stringify({ state, slotIndex: slot, augmentId: id }),
  })
  return { status: response.status, body: await response.json() }
}
for (const entry of catalog) {
  const result = await install(root, entry.id)
  check('install policy: ' + entry.id, result.status === (entry.selectionEnabled && entry.effects.Gloves ? 200 : 422))
}
const legacy = { ...root, installedAugments: [{ slotIndex: 0, augmentId: 'Desert_Rune' }] }
const upgraded = await install(legacy, 'Perfect_Glacial_Rune', 1)
check('existing lower-tier socket preserved', upgraded.status === 200 && upgraded.body.state.installedAugments[0].augmentId === 'Desert_Rune')
const browser = await chromium.launch({ headless: true })
const page = await browser.newPage({ viewport: { width: 1600, height: 1000 } })
try {
  const errors = []
  page.on('pageerror', (error) => errors.push(error.message))
  await page.goto(ui)
  await page.waitForSelector('.item-slot')
  await page.locator('.locale-trigger').click()
  await page.getByRole('menuitemradio', { name: 'English', exact: true }).click()
  await page.locator('.workbench-item-controls button').first().click()
  await page.locator('.wb-base-picker__search').fill('Tasalian Greaves')
  await page.locator('.wb-base-picker__option').first().click()
  const baseResponse = page.waitForResponse((r) => r.url().includes('/workbench/initial?') && r.status() === 200)
  await page.locator('.wb-base-picker__footer button').click()
  await baseResponse
  await page.waitForSelector('.bench-item-card')
  await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  for (let count = 1; count <= 2; count++) {
    await page.locator('[data-material-tooltip="Artificers_Orb"]').first().click()
    const response = page.waitForResponse((r) => r.url().endsWith('/workbench/apply'))
    await page.locator('.item-slot').click()
    const result = await (await response).json()
    check('socket count ' + count, result.state.augmentSockets === count)
    await page.waitForFunction((n) => document.querySelectorAll('.wb-socket').length === n, count)
    await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  }
  await page.locator('#tab-Augments').click()
  check('34 selectable Augments', await page.locator('#material-list button').count() === 34)
  check('lower Desert tiers hidden', await page.getByRole('button', { name: 'Desert Rune', exact: true }).count() === 0)
  const search = page.locator('.workbench-stash__search')
  await search.fill('Greater Desert Rune')
  check('lower tier search empty', await page.locator('#material-list button').count() === 0)
  await search.fill('Perfect Desert Rune')
  const rune = page.getByRole('button', { name: 'Perfect Desert Rune', exact: true })
  await rune.click()
  await page.locator('.item-placement').screenshot({ path: output + '/empty-item.png' })
  const response = page.waitForResponse((r) => r.url().endsWith('/augments/install'))
  await page.locator('.wb-socket').first().click()
  check('UI Perfect install', (await response).status() === 200)
  await page.waitForSelector('.wb-socket.is-filled img')
  await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  await search.fill('Perfect Glacial Rune')
  await page.getByRole('button', { name: 'Perfect Glacial Rune', exact: true }).click()
  const replacement = page.waitForResponse((r) => r.url().endsWith('/augments/install'))
  await page.locator('.wb-socket').first().press('Enter')
  check('UI replacement', (await replacement).status() === 200)
  await page.waitForSelector('.wb-socket img[alt="Perfect Glacial Rune"]')
  await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  await page.waitForFunction(() => document.activeElement?.matches('.item-slot'))
  check('keyboard replacement restores central item focus', true)
  await page.reload()
  await page.waitForSelector('.wb-socket img[alt="Perfect Glacial Rune"]')
  check('refresh preserves installed item', true)
  await page.locator('#tab-Augments').click()
  await page.locator('.workbench-stash__search').fill('Perfect Desert Rune')
  await page.getByRole('button', { name: 'Perfect Desert Rune', exact: true }).click()
  const geometry = await page.locator('.item-placement').evaluate((placement) => {
    const rect = placement.getBoundingClientRect()
    return [...placement.querySelectorAll('.wb-socket')].map((el) => { const r = el.getBoundingClientRect(); return { leftOfItem: r.right < rect.left, x: r.x, y: r.y, width: r.width, height: r.height } })
  })
  check('desktop vertical left sockets', geometry.every((s) => s.leftOfItem) && geometry[0].x === geometry[1].x && geometry[0].y < geometry[1].y)
  await page.screenshot({ path: output + '/desktop.png', fullPage: true })
  await page.locator('.wb-sockets').screenshot({ path: output + '/socket-detail.png' })
  const pixels = await page.evaluate(async () => {
    const image = new Image(); image.src = '/assets/augments/socket-frame.png'; await image.decode()
    const canvas = document.createElement('canvas'); canvas.width = image.width; canvas.height = image.height
    const ctx = canvas.getContext('2d'); ctx.drawImage(image, 0, 0)
    return { center: [...ctx.getImageData(image.width / 2, image.height / 2, 1, 1).data], corner: [...ctx.getImageData(0, 0, 1, 1).data] }
  })
  check('asset includes black center and transparent outside', pixels.center[3] >= 250 && Math.max(...pixels.center.slice(0, 3)) < 20 && pixels.corner[3] === 0)
  await page.locator('.locale-trigger').click()
  await page.getByRole('menuitemradio', { name: '한국어', exact: true }).click()
  await page.getByRole('button', { name: '완벽한 사막 룬', exact: true }).focus()
  const feedback = page.locator('.workbench-feedback-region')
  const text = await feedback.innerText()
  check('Korean grouped feedback', text.includes('무도 무기:') && text.includes('마법봉 또는 지팡이:') && text.includes('방어구:') && !text.includes('Gloves:'))
  await page.screenshot({ path: output + '/korean-feedback.png', fullPage: true })
  await page.setViewportSize({ width: 390, height: 844 })
  await page.locator('.wb-sockets').scrollIntoViewIfNeeded()
  const mobile = await page.locator('.item-placement').evaluate((el) => { const item = el.getBoundingClientRect(); return [...el.querySelectorAll('.wb-socket')].every((s) => { const r = s.getBoundingClientRect(); return r.right < item.left && r.left >= 0 }) })
  check('mobile sockets remain left and visible', mobile)
  check('mobile sockets avoid currency buttons', await page.evaluate(() => {
    const currencies = [...document.querySelectorAll('.currency-slot')].map((e) => e.getBoundingClientRect())
    return [...document.querySelectorAll('.wb-socket')].every((e) => { const s = e.getBoundingClientRect(); return currencies.every((c) => s.right <= c.left || s.left >= c.right || s.bottom <= c.top || s.top >= c.bottom) })
  }))
  check('mobile no horizontal overflow', await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
  await page.screenshot({ path: output + '/mobile.png', fullPage: true })
  await page.setViewportSize({ width: 1600, height: 1000 })
  await page.locator('.workbench-item-controls button').first().click()
  await page.locator('.wb-base-picker__filters select').nth(1).selectOption('Body Armours')
  await page.locator('.wb-base-picker__option').first().click()
  const bodyResponse = page.waitForResponse((r) => r.url().includes('/workbench/initial?') && r.status() === 200)
  await page.locator('.wb-base-picker__footer button').click()
  await bodyResponse
  await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  await page.locator('#tab-Currency').click()
  for (let count = 1; count <= 3; count++) {
    await page.locator('[data-material-tooltip="Artificers_Orb"]').first().click()
    const response = page.waitForResponse((r) => r.url().endsWith('/workbench/apply'))
    await page.locator('.item-slot').click()
    check('Body Armour socket count ' + count, (await (await response).json()).state.augmentSockets === count)
    await page.waitForFunction((n) => document.querySelectorAll('.wb-socket').length === n, count)
    await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
  }
  await page.setViewportSize({ width: 390, height: 844 })
  check('three mobile sockets fit beside the central item', await page.locator('.item-placement').evaluate((el) => {
    const item = el.getBoundingClientRect()
    return [...el.querySelectorAll('.wb-socket')].every((s) => { const r = s.getBoundingClientRect(); return r.right < item.left && r.top >= item.top && r.bottom <= item.bottom })
  }))
  await page.screenshot({ path: output + '/mobile-three-sockets.png', fullPage: true })
  check('no browser errors', errors.length === 0)
  fs.writeFileSync(output + '/checks.json', JSON.stringify({ ui, api, checks, geometry, pixels, errors, passed: true }, null, 2) + '\n')
  console.log(JSON.stringify({ passed: checks.length, output }))
} catch (error) {
  console.log((await page.locator('body').innerText()).slice(0, 5000))
  await page.screenshot({ path: output + '/failure.png', fullPage: true })
  throw error
} finally { await browser.close() }
