import fs from 'node:fs'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/belts-source-bundle-2026-10-05'
for (const slug of ['Linen', 'Wide', 'Long', 'Plate', 'Ornate', 'Mail', 'Double', 'Heavy', 'Utility', 'Fine', 'Invoking', 'Sinew', 'Forking']) {
  const html = fs.readFileSync(`${root}/${slug}_Belt.us.html`, 'utf8')
  const blocks = [...html.matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].filter(m => m[0].includes('<tr><th>Family'))
  console.log(slug, JSON.stringify(blocks.map(m => ({ text: clean(m[1]), family: clean(m[0].match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)[1]), stats: [...m[0].matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => ({ id: clean(m[1]).replaceAll(' ', '_'), range: clean(m[2]), locality: clean(m[3]) })) }))))
}
const catalysts = fs.readFileSync(`${root}/Catalysts.us.html`, 'utf8')
console.log('Catalyst class rules', [...new Set([...catalysts.matchAll(/(Adds quality[^<]*|Improves[^<]*|Rings[^<]*Amulets[^<]*|ring or amulet[^<]*)/gi)].map(m => clean(m[0])))])
const pool = JSON.parse(fs.readFileSync(`${root}/Belts.us.json`, 'utf8')).data
console.log('Class pool counts', Object.fromEntries(Object.entries(pool).map(([k,v])=>[k,Array.isArray(v)?v.length:typeof v])))
