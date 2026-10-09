import fs from 'node:fs'
import crypto from 'node:crypto'

const output = 'docs/evidence/top-base-inventory-2026-10-04'
fs.mkdirSync(output, { recursive: true })
const clean = (s) => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').trim()
const pages = ['Body_Armours_str', 'Body_Armours_dex', 'Body_Armours_int', 'Body_Armours_str_dex', 'Body_Armours_str_int', 'Body_Armours_dex_int', 'Helmets_str', 'Helmets_dex', 'Helmets_int', 'Helmets_str_dex', 'Helmets_str_int', 'Helmets_dex_int', 'Gloves_str', 'Gloves_dex', 'Gloves_int', 'Gloves_str_dex', 'Gloves_str_int', 'Gloves_dex_int', 'Boots_str', 'Boots_dex', 'Boots_int', 'Boots_str_dex', 'Boots_str_int', 'Boots_dex_int', 'Bows', 'Wands', 'Sceptres', 'Rings', 'Amulets', 'Belts']
const inventory = []
for (const page of pages) {
  const existing = `${output}/${page}.json`
  if (fs.existsSync(existing)) {
    const previous = JSON.parse(fs.readFileSync(existing))
    if (previous.cards.length) { inventory.push(previous); continue }
  }
  const url = 'https://poe2db.tw/us/' + page
  const response = await fetch(url)
  if (!response.ok) throw new Error(`${page}: ${response.status}`)
  const html = await response.text()
  const section = html.match(/<h5[^>]*>[^<]*BaseItem[^]*?(?=<footer|<h5|$)/)?.[0]
    ?? html.match(/<h5[^>]*>(?:Bow|Wand|Sceptre|Ring|Amulet|Belt) \/\d+[^]*?(?=<footer|<h5|$)/)?.[0] ?? ''
  const cards = [...(section.includes('BaseItem') ? section : html).matchAll(/<div class="flex-grow-1 ms-2"><a class="whiteitem[^]*?href="([^"]+)"[^]*?<\/a><div>([^]*?)(?=<\/div><\/div><\/div>|$)/g)].map((m) => ({ slug: m[1], card: clean(m[0]) }))
  const entry = { page, url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), cards }
  inventory.push(entry)
  fs.writeFileSync(`${output}/${page}.json`, JSON.stringify(entry, null, 2) + '\n')
  console.log(page, cards.length, cards.filter((c) => !/Runeforged|Runemastered/.test(c.card)).slice(-8))
}
fs.writeFileSync(`${output}/inventory.json`, JSON.stringify(inventory, null, 2) + '\n')
