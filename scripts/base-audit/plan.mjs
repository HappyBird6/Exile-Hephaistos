import fs from '../evidence-files.cjs'
const root = 'evidence:base-audit'
fs.mkdirSync(root, { recursive: true })
const current = JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/top-bases.json'))
const inventory = JSON.parse(fs.readFileSync('evidence:top-base-inventory/inventory.json'))
const selected = []
const armourAudit = []
for (const page of inventory.filter((entry) => /^(Body_Armours|Helmets|Gloves|Boots)_/.test(entry.page))) {
  const groups = new Map()
  for (const base of page.cards) {
    if (/^Rune(forged|mastered)_/.test(base.slug)) continue
    const propertyStart = base.card.search(/(?:Armour|Evasion Rating|Energy Shield|Base Movement Speed):/)
    const tail = base.card.slice(propertyStart < 0 ? 0 : propertyStart)
      .replace(/(?:Armour|Evasion Rating|Energy Shield|Base Movement Speed): -?[\d.]+/g, '')
      .replace(/Requires:  Level \d+(?:, \d+ (?:Str|Dex|Int))*/g, '').trim()
    const level = +(base.card.match(/Requires:  Level (\d+)/)?.[1] ?? 0)
    const defense = [...base.card.matchAll(/(?:Armour|Evasion Rating|Energy Shield): ([\d.]+)/g)].reduce((n, match) => n + +match[1], 0)
    const row = { page: page.page, slug: base.slug, implicit: tail, level, defense, card: base.card }
    const previous = groups.get(tail)
    if (!previous || defense > previous.defense || defense === previous.defense && level > previous.level) groups.set(tail, row)
  }
  for (const row of groups.values()) {
    row.registered = Object.values(current).some((base) => base.slug === row.slug)
    armourAudit.push(row)
    if (!row.registered && row.implicit) selected.push(row.slug)
  }
}
const requested = ['Jade_Amulet','Lapis_Amulet','Gold_Amulet','Twisted_Amulet','Distorted_Amulet','Absent_Amulet','Portent_Amulet','Lament_Amulet','Dusk_Amulet','Penumbra_Amulet','Gloam_Amulet','Corona_Amulet','Tenebrous_Amulet','Lazuli_Ring','Sapphire_Ring','Topaz_Ring','Emerald_Ring','Gold_Ring','Unset_Ring']
const accessoryAudit = inventory.filter((entry) => ['Rings','Amulets','Belts'].includes(entry.page)).map((page) => ({ page: page.page, rows: page.cards.map((base) => ({ ...base, registered: Object.values(current).some((item) => item.slug === base.slug), requested: requested.includes(base.slug) })) }))
const plan = { requested, armourAudit, accessoryAudit, collect: [...new Set([...requested, ...selected])] }
fs.writeFileSync(`${root}/plan.json`, JSON.stringify(plan, null, 2) + '\n')
console.log(JSON.stringify({ extraArmour: armourAudit.filter((row) => !row.registered), totalCollect: plan.collect.length }, null, 2))
