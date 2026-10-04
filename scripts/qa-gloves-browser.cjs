const fs = require('node:fs'), assert = require('node:assert/strict')
const { chromium } = require('/qa/node_modules/playwright')
const terms = require('/source/frontend/src/shared/i18n/gameTerms.json')
const bases = require('/source/frontend/src/features/crafting/topBases.json')
const initials = JSON.parse(fs.readFileSync('/evidence/api-initials.json'))
const key = 'hephaistos.workbench.films.v1', checks = [], errors = [], captures = []
const check = (name, value) => { assert(value, name); checks.push(name) }
const locales = ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es']
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
  check('22 base selector choices', await page.locator('#base-select option').count() === 22)
  await page.locator('#base-select').selectOption(base)
  await page.locator('#base-level').fill('82')
  await page.locator('dialog[open] .base-form .primary-action').click()
  await page.waitForFunction(({ key, before }) => JSON.parse(localStorage.getItem(key))?.films.length > before, { key, before })
  await page.waitForSelector('.bench-item-card')
}
async function use(id, shift = false) {
  await page.locator(id.includes('Essence') ? '#tab-Essence' : '#tab-Currency').click()
  const button = page.getByRole('button', { name: terms.en[id].name, exact: true })
  await button.click({ button: id.includes('Essence') ? 'right' : 'left' })
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
;(async () => {
  browser = await chromium.launch({ headless: true })
  page = await (await browser.newContext({ viewport: { width: 1440, height: 1100 } })).newPage()
  page.on('pageerror', (e) => errors.push(e.message))
  await page.goto('http://host.docker.internal:19381', { waitUntil: 'networkidle' })
  await locale('en')
  for (const k of ['massive', 'sirenscale', 'adherent']) {
    await place(k)
    check(k + ' actual selector creates correct base', active(await history()).frames[0].state.baseItemId === bases[k].id)
    const r = await use('Orb_of_Transmutation', true)
    check(k + ' positive browser Transmutation', r.applied)
    check(k + ' Shift selection remains', await page.getByRole('button', { name: terms.en.Orb_of_Transmutation.name, exact: true }).getAttribute('aria-pressed') === 'true')
    const before = await raw(), response = page.waitForResponse((r) => r.url().endsWith('/workbench/apply'))
    await page.locator('.item-slot').click({ modifiers: ['Shift'] })
    const repeat = await (await response).json()
    await page.waitForFunction(() => !document.querySelector('.item-slot').disabled)
    check(k + ' repeat atomic refusal', !repeat.applied && await raw() === before)
    await page.keyboard.press('Escape')
    check(k + ' Escape clears selection', await page.getByRole('button', { name: terms.en.Orb_of_Transmutation.name, exact: true }).getAttribute('aria-pressed') !== 'true')
    await use('Orb_of_Augmentation')
    await use('Regal_Orb')
    const h = await history(), future = JSON.stringify(active(h).frames), oldId = h.active
    await page.locator('.workbench-film-controls button').first().click()
    await page.waitForFunction((k) => JSON.parse(localStorage.getItem(k)).cursor === 2, key)
    const regal = await use('Regal_Orb')
    check(k + ' past craft creates film', regal.applied && (await history()).films.length === h.films.length + 1)
    check(k + ' future frames preserved', JSON.stringify((await history()).films.find((f) => f.id === oldId).frames) === future)
    const saved = await raw()
    await page.reload({ waitUntil: 'networkidle' })
    check(k + ' exact reload history', await raw() === saved)
  }
  for (const k of ['massive', 'sirenscale', 'adherent']) {
    const source = initials[k], d = Object.values(source.modifiers).find((d) => d.weight > 0 && (k === 'massive' ? d.stats.length === 1 : d.stats.length === 2 && d.id.startsWith(bases[k].pool + ':')) && d.stats[0].min !== d.stats[0].max)
    const state = { ...concrete(source), rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }] }
    await seed(state, 'qa-' + k)
    check(k + ' fractured rendered', await page.locator('.bench-item-card [data-fractured], .bench-item-card .item-card__line--fractured').count() === 1)
    const normal = await page.locator('.bench-item-card').innerText()
    await page.keyboard.down('Alt')
    await page.waitForFunction((text) => document.querySelector('.bench-item-card').innerText !== text, normal)
    check(k + ' Alt shows source range', await page.locator('.bench-item-card').innerText() !== normal)
    await page.keyboard.up('Alt')
    check(k + ' Alt release restores rolled display', await page.locator('.bench-item-card').innerText() === normal)
    for (const l of locales) {
      await locale(l)
      const text = await page.locator('.bench-item-card').innerText()
      check(`${k}:${l} localized name`, text.includes(terms[l][bases[k].slug].name))
      check(`${k}:${l} sourced armour`, (bases[k].armour === 0 || text.includes(String(bases[k].armour))) && (bases[k].energyShield === 0 || text.includes(String(bases[k].energyShield))))
      check(`${k}:${l} exact required level distinct from ilvl`, text.replace(/\s+/g, ' ').includes(bases[k].requirements[l].replace(/\s+/g, ' ')) && text.includes('82'))
      check(`${k}:${l} no locale fallback`, await page.locator('.bench-item-card .translation-fallback').count() === 0)
      const saved = await raw()
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: 1100 })
        await page.locator('.bench-item-card').scrollIntoViewIfNeeded()
        await page.screenshot({ path: `/evidence/${k}-${l}-${width}.png`, fullPage: true })
        check(`${k}:${l}:${width} no document overflow`, await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1))
      }
      check(`${k}:${l} locale and viewport preserve films`, await raw() === saved)
    }
    await page.setViewportSize({ width: 1440, height: 1100 })
  }
  await locale('en')
  for (const k of ['massive', 'sirenscale', 'adherent']) {
    const source = initials[k]
    const defs = Object.values(source.modifiers)
    const d = defs.find(d => d.weight > 0 && d.familyIds.includes('FireResistance') && d.requiredItemLevel === 1)
    const magic = { ...concrete(source), rarity: 'MAGIC', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: false }] }
    for (const id of ['Lesser_Essence_of_Enhancement', 'Lesser_Essence_of_the_Infinite']) {
      await seed(magic, 'essence-' + k + '-' + id)
      const result = await use(id)
      check(k + ' browser accepts per-base Essence target ' + id, result.applied)
    }
    const life = defs.find(d => d.weight > 0 && d.familyIds.includes('IncreasedLife') && d.requiredItemLevel === 1)
    const rare = { ...magic, rarity: 'RARE', explicits: [{ modifierId: life.id, values: Object.fromEntries(life.stats.map(s => [s.id, s.min])), fractured: false }, ...magic.explicits] }
    await seed(rare, 'perfect-' + k)
    const result = await use('Perfect_Essence_of_Grounding')
    check(k + ' browser accepts class-valid Perfect Essence', result.applied)
  }
  for (const k of ['massive', 'sirenscale', 'adherent']) {
    const source = initials[k]
    const defs = Object.values(source.modifiers)
    const d = defs.find(d => d.weight > 0 && d.familyIds.includes('IncreasedLife') && d.requiredItemLevel === 1)
    const state = { ...concrete(source), rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: false }] }
    await seed(state, 'preview-' + k)
    const saved = await raw()
    await page.locator('#tab-Omen').click()
    const omen = page.getByRole('button', { name: terms.en.Omen_of_Whittling.name, exact: true })
    await omen.click({ button: 'right' })
    await page.waitForSelector('.bench-item-card .item-card__line--removal-candidate')
    check(k + ' local orange preview', await page.locator('.item-card__line--removal-candidate').evaluate(el => getComputedStyle(el).color) === 'rgb(255, 166, 77)')
    check(k + ' local preview preserves film', await raw() === saved)
    await page.screenshot({ path: '/evidence/' + k + '-orange-preview.png', fullPage: true })
    await omen.click({ button: 'right' })
  }
  for (const legacy of ['stocky', 'body', 'helmet']) {
    const state = concrete(initials[legacy])
    await seed(state, 'old-' + legacy)
    const saved = await raw()
    check(legacy + ' legacy reload retains original base ID', active(await history()).frames[0].state.baseItemId === initials[legacy].state.baseItemId)
    await place('massive')
    const h = await history()
    check(legacy + ' new selection preserves old film', JSON.stringify(h.films[0]) === JSON.stringify(JSON.parse(saved).films[0]))
    await page.locator('.workbench-film-select select').selectOption('old-' + legacy)
    await page.waitForFunction(({ key, id }) => JSON.parse(localStorage.getItem(key)).active === id, { key, id: 'old-' + legacy })
    const r = await use('Orb_of_Transmutation')
    check(legacy + ' restored original film crafts with old ID', r.applied && r.state.baseItemId === initials[legacy].state.baseItemId)
  }
  check('zero browser errors', errors.length === 0)
  fs.writeFileSync('/evidence/browser-results.json', JSON.stringify({ checks, count: checks.length, errors, captures, passed: true }, null, 2) + '\n')
  console.log(checks.length, 'browser checks passed')
})().catch(async (error) => {
  fs.writeFileSync('/evidence/browser-failure-1.json', JSON.stringify({ error: error.stack, checks, errors, captures }, null, 2))
  if (page) await page.screenshot({ path: '/evidence/browser-failure-1.png', fullPage: true })
  process.exitCode = 1
}).finally(async () => { if (browser) await browser.close() })
