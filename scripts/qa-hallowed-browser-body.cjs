;(async () => {
  browser = await chromium.launch({ headless: true })
  page = await (await browser.newContext({ viewport: { width: 1440, height: 1100 } })).newPage()
  page.on('pageerror', e => errors.push(e.message))
  await page.goto('http://host.docker.internal:20081', { waitUntil: 'networkidle' })
  await locale('en')
  await place('hallowed')
  check('selector creates Hallowed', active(await history()).frames[0].state.baseItemId === bases.hallowed.id)
  check('Sceptres class', await page.locator('.bench-item-card .item-card__class').innerText() === 'Sceptres')
  const r = await use('Orb_of_Transmutation', true)
  check('positive Transmutation', r.applied)
  check('Shift selection remains', await page.getByRole('button', { name: terms.en.Orb_of_Transmutation.name, exact: true }).getAttribute('aria-pressed') === 'true')
  await seed({ ...concrete(initials.hallowed), rarity: 'MAGIC' }, 'hallowed-command')
  await locale('en')
  check('positive Command Essence', (await use('Greater_Essence_of_Command')).applied)
  const rare = { ...concrete(initials.hallowed), rarity: 'RARE', explicits: r.state.explicits }
  await seed(rare, 'hallowed-wrongclass')
  const before = await raw()
  const wrong = await use('Perfect_Essence_of_Sorcery')
  check('wrongclass source refusal', !wrong.applied && await raw() === before)
  const d = Object.values(initials.hallowed.modifiers).find(d => d.weight > 0 && d.stats.length > 1 && d.stats.some(s => s.min !== s.max))
  check('real Sceptre compound source exists', Boolean(d))
  const state = { ...concrete(initials.hallowed), rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }] }
  await seed(state, 'hallowed-compound')
  for (const l of locales) {
    await locale(l)
    let card = await page.locator('.bench-item-card').innerText()
    check(l + ' exact name', card.includes(terms[l].Hallowed_Sceptre.name))
    for (const line of bases.hallowed.sourceProperties[l]) check(l + ' source built-in property ' + line, card.includes(line))
    check(l + ' canonical requirements', card.replace(/\s+/g, ' ').includes(bases.hallowed.requirements[l].replace(/\s+/g, ' ')))
    const saved = await raw()
    for (const width of [1440, 390]) {
      await page.setViewportSize({ width, height: 1100 })
      check(l + ' viewport stays within horizontal bounds ' + width, await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
      await page.screenshot({ path: '/evidence/hallowed-' + l + '-' + width + '.png', fullPage: true })
      await page.locator('.bench-item-card').screenshot({ path: '/evidence/cards/hallowed-' + l + '-' + width + '.png' })
      card = await page.locator('.bench-item-card').innerText()
      await page.keyboard.down('Alt')
      await page.waitForFunction(t => document.querySelector('.bench-item-card').innerText !== t, card)
      check(l + ' compound Alt ranges ' + width, await page.locator('.bench-item-card').innerText() !== card)
      await page.screenshot({ path: '/evidence/hallowed-' + l + '-' + width + '-alt.png', fullPage: true })
      await page.locator('.bench-item-card').screenshot({ path: '/evidence/cards/hallowed-' + l + '-' + width + '-alt.png' })
      await page.keyboard.up('Alt')
      check(l + ' Alt preserves canonical film ' + width, await raw() === saved)
    }
  }
  await page.setViewportSize({ width: 1440, height: 1100 })
  await locale('en')
  await seed({ ...state, explicits: state.explicits.map(m => ({ ...m, fractured: false })) }, 'hallowed-preview')
  const previewBefore = await raw()
  await page.locator('#tab-Omen').click()
  await page.getByRole('button', { name: terms.en.Omen_of_Whittling.name, exact: true }).click()
  const favorite = page.locator('.favorite-slot').first()
  await favorite.click()
  await favorite.click({ button: 'right' })
  await page.waitForSelector('.bench-item-card .item-card__line--removal-candidate')
  for (const l of locales) {
    await locale(l)
    check(l + ' local orange preview', await page.locator('.item-card__line--removal-candidate').evaluate(el => getComputedStyle(el).color) === 'rgb(255, 166, 77)')
    check(l + ' preview leaves film unchanged', await raw() === previewBefore)
    await page.screenshot({ path: '/evidence/hallowed-' + l + '-orange-preview.png', fullPage: true })
  }
  await favorite.click({ button: 'right' })
  await locale('en')
  for (const [k, old] of Object.entries(oldInitials)) {
    const historical = concrete(old)
    await seed(historical, 'old-' + k)
    assert.deepEqual(active(await history()).frames[0].state, historical)
    check(k + ' complete historical film state preserved', true)
    check(k + ' historical film base preserved', active(await history()).frames[0].state.baseItemId === old.state.baseItemId)
    check(k + ' historical film displays', (await page.locator('.bench-item-card').innerText()).length > 0)
  }
  for (const k of ['wand', 'sceptre']) {
    const old = oldInitials[k]
    const definition = Object.values(old.modifiers).find(d => d.weight > 0 && d.requiredItemLevel <= 82)
    const historical = { ...concrete(old), rarity: 'RARE', explicits: [{ modifierId: definition.id, values: Object.fromEntries(definition.stats.map(s => [s.id, s.min])), fractured: true }] }
    await seed(historical, 'old-filled-' + k)
    assert.deepEqual(active(await history()).frames[0].state, historical)
    check(k + ' filled legacy film preserves original source rolls and fracture', true)
    await page.screenshot({ path: '/evidence/old-filled-' + k + '.png', fullPage: true })
  }
  const life = Object.values(initials.solar.modifiers).find(d => d.familyIds.includes('IncreasedLife') && d.requiredItemLevel === 1 && d.stats.length === 1)
  const lifeStat = life.stats[0], originalLife = lifeStat.min + 1
  check('source life rounding fixture inside range', originalLife <= lifeStat.max)
  const overflow = { ...concrete(initials.solar), rarity: 'RARE', catalystQuality: { type: 'FLESH', amount: 40 }, explicits: [{ modifierId: life.id, values: { [lifeStat.id]: originalLife }, fractured: false }] }
  await seed(overflow, 'solar-preserved-overflow')
  const repeated = await use('Flesh_Catalyst')
  check('existing Solar overflow repeat preserves quality40/cap20', repeated.applied && repeated.state.catalystQuality.amount === 40 && repeated.qualityLimit.maximumQuality === 20)
  for (const l of locales) {
    await locale(l)
    const stored = active(await history()).frames.at(-1).state
    check(l + ' original Solar quality/roll retained', stored.catalystQuality.amount === 40 && stored.explicits[0].values[lifeStat.id] === originalLife)
    check(l + ' existing HALF_UP display rounding retained', (await page.locator('.bench-item-card .item-card__line--explicit').innerText()).includes(String(Math.round(originalLife * 140 / 100))))
    await page.screenshot({ path: '/evidence/solar-overflow-' + l + '.png', fullPage: true })
  }
  await seed(state, 'hallowed-reload')
  await page.reload({ waitUntil: 'networkidle' })
  check('new Hallowed film reload preserved', active(await history()).frames[0].state.baseItemId === bases.hallowed.id)
  check('zero browser errors', errors.length === 0)
  fs.writeFileSync('/evidence/browser-results.json', JSON.stringify({ passed: true, count: checks.length, checks, errors, captures }, null, 2) + '\n')
  console.log(checks.length, 'browser checks passed')
})().catch(async error => {
  fs.writeFileSync('/evidence/browser-failure.json', JSON.stringify({ error: error.stack, checks, errors, captures }, null, 2))
  if (page) await page.screenshot({ path: '/evidence/browser-failure.png', fullPage: true })
  process.exitCode = 1
}).finally(async () => { if (browser) await browser.close() })
