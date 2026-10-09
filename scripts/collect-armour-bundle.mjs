import fs from 'node:fs'
import assert from 'node:assert/strict'
import { collectArmourSource, sourceLocales, sourceProperties } from './armour-source.mjs'

// Evidence collection never changes an active catalog or historical base identity.
const root = process.argv[2]
assert(root, 'Pass a new owned evidence directory')
assert(!fs.existsSync(root), 'Preserve existing evidence; use a new directory')
fs.mkdirSync(root, { recursive: true })
const locales = sourceLocales
const archetypes = ['str', 'dex', 'int', 'str_dex', 'str_int', 'dex_int']
const roster = {
  Body_Armours: ['Soldier_Cuirass', 'Slipstrike_Vest', 'Vile_Robe', 'Death_Mail', 'Wolfskin_Mantle', 'Primal_Markings'],
  Helmets: ['Imperial_Greathelm', 'Freebooter_Cap', 'Ancestral_Tiara', 'Gladiatorial_Helm', 'Cryptic_Crown', 'Grinning_Mask'],
  Gloves: ['Massive_Mitts', 'Polished_Bracers', 'Sirenscale_Gloves', 'Blacksteel_Gauntlets', 'Tethering_Bands', 'War_Wraps'],
  Boots: ['Tasalian_Greaves', 'Drakeskin_Boots', 'Sekhema_Sandals', 'Blacksteel_Sabatons', 'Faithful_Leggings', 'Daggerfoot_Shoes'],
}
const alternatives = {
  Gloves_str_int: ['Adherent_Cuffs'],
  Gloves_dex_int: ['Secured_Wraps'],
  Boots_str_int: ['Apostle_Leggings', 'Warlock_Leggings', 'Cryptic_Leggings'],
}
const save = (file, value) => fs.writeFileSync(`${root}/${file}`, JSON.stringify(value, null, 2) + '\n')
const properties = sourceProperties
const bundle = [], failures = []
for (const [slot, slugs] of Object.entries(roster)) for (let i = 0; i < slugs.length; i++) {
  const page = `${slot}_${archetypes[i]}`
  const inventory = JSON.parse(fs.readFileSync(`docs/evidence/top-base-inventory-2026-10-04/${page}.json`))
  const candidates = [slugs[i], ...(alternatives[page] ?? [])]
  const normalCards = inventory.cards.filter(c => !/^Runeforged_|^Runemastered_/.test(c.slug))
  const topProperties = Object.fromEntries(['Armour', 'Evasion Rating', 'Energy Shield'].map(label => [label, Math.max(...normalCards.map(c => properties(c.card)[label] ?? 0))]))
  for (const slug of candidates) {
    const entry = { page, slug, inventorySource: inventory.url, inventorySha256: inventory.sha256, topProperties, locales: {}, issues: [] }
    try {
      const card = normalCards.find(c => c.slug === slug)
      assert(card, `${slug}: absent from reviewed normal base inventory`)
      entry.inventoryCard = card.card
      entry.properties = properties(card.card)
      entry.highestDefence = Object.entries(topProperties).every(([label, value]) => value === 0 || entry.properties[label] === value)
      for (const locale of locales) {
        const record = await collectArmourSource(slug, locale)
        save(`${slug}.${locale}.json`, record)
        entry.locales[locale] = record
      }
      const source = entry.locales.us
      entry.id = source.fields.Type
      entry.sourceTags = source.fields.Tags.split(', ')
      entry.maximumQuality = Number(source.fields['Quality.max_quality'])
      assert.equal(entry.maximumQuality, 20)
      const actual = properties(source.card)
      for (const [label, value] of Object.entries(entry.properties)) assert.equal(actual[label], value, `${slug}: live properties differ from inventory`)
      entry.properties = actual
      entry.implicitSource = Object.fromEntries(Object.entries(source.fields).filter(([key]) => /implicit|mod(s|ifier)/i.test(key)))
      const requires = source.requirements
      entry.requiredLevel = Number(requires.match(/Level (\d+)/)?.[1] ?? 0)
      entry.attributes = Object.fromEntries(['Str', 'Dex', 'Int'].flatMap(a => {
        const match = requires.match(new RegExp(`(\\d+) ${a}`))
        return match ? [[a, Number(match[1])]] : []
      }))
      entry.reviewStatus = 'BASE_SOURCE_VERIFIED_MODIFIER_POOL_PENDING'
    } catch (error) {
      entry.issues.push(error.message)
      failures.push({ slug, error: error.message })
      entry.reviewStatus = 'INCOMPLETE_SOURCE'
    }
    bundle.push(entry)
    save('bundle.json', bundle)
    console.log(slug, entry.reviewStatus, JSON.stringify(entry.properties))
  }
}
save('failures.json', failures)
assert.equal(failures.length, 0, 'Source failures are preserved; do not import incomplete bases')
