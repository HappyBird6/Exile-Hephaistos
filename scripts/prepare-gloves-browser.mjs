import fs from 'node:fs'
let s = fs.readFileSync('/source/scripts/qa-top-bases-browser.cjs', 'utf8')
s = s.replaceAll('19281', '19381')
  .replaceAll("['soldier', 'imperial']", "['massive', 'sirenscale', 'adherent']")
  .replaceAll('19 base selector choices', '22 base selector choices').replaceAll('count() === 19', 'count() === 22')
  .replace("for (const legacy of ['body', 'helmet'])", "for (const legacy of ['stocky', 'body', 'helmet'])")
  .replace("legacy === 'body' ? 'soldier' : 'imperial'", "'massive'")
  .replace("text.includes(String(bases[k].armour))", "(bases[k].armour === 0 || text.includes(String(bases[k].armour))) && (bases[k].energyShield === 0 || text.includes(String(bases[k].energyShield)))")
  .replace("values: { [d.stats[0].id]: d.stats[0].min }", "values: { [d.stats[0].id]: d.stats[0].min }, fractured: true")
  .replace("const normal = await page.locator('.bench-item-card').innerText()", "check(k + ' fractured rendered', await page.locator('.bench-item-card [data-fractured], .bench-item-card .item-card__line--fractured').count() === 1)\n    const normal = await page.locator('.bench-item-card').innerText()")
s = s.replace('d.stats.length === 1 && d.stats[0].min !== d.stats[0].max', "(k === 'massive' ? d.stats.length === 1 : d.stats.length === 2 && d.id.startsWith(bases[k].pool + ':')) && d.stats[0].min !== d.stats[0].max")
  .replace('values: { [d.stats[0].id]: d.stats[0].min }, fractured: true', 'values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true')
  .replace("check(k + ' fractured rendered'", "check(k + ' no invented implicit lines', await page.locator('.bench-item-card .item-card__line--implicit').count() === 0)\n    check(k + ' fractured rendered'")
s = s.replace("await page.locator('#tab-Currency').click()", "await page.locator(id.includes('Essence') ? '#tab-Essence' : '#tab-Currency').click()")
  .replace('await button.click()', "await button.click({ button: id.includes('Essence') ? 'right' : 'left' })")
s = s.replace("  for (const legacy of ['stocky', 'body', 'helmet']) {", `  for (const k of ['massive', 'sirenscale', 'adherent']) {
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
    await seed(rare, 'abyss-' + k)
    const abyss = await use('Essence_of_the_Abyss')
    check(k + ' browser accepts sourced Gloves Abyss targets', abyss.applied)
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
    await omen.click()
    const favorite = page.locator('.favorite-slot').first()
    await favorite.click()
    await favorite.click({ button: 'right' })
    await page.waitForSelector('.bench-item-card .item-card__line--removal-candidate')
    check(k + ' local orange preview', await page.locator('.item-card__line--removal-candidate').evaluate(el => getComputedStyle(el).color) === 'rgb(255, 166, 77)')
    check(k + ' local preview preserves film', await raw() === saved)
    await page.screenshot({ path: '/evidence/' + k + '-orange-preview.png', fullPage: true })
    await favorite.click({ button: 'right' })
  }
  for (const legacy of ['stocky', 'body', 'helmet']) {`)
fs.writeFileSync('/source/scripts/qa-gloves-browser.cjs', s)
