import fs from 'node:fs'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/maces-source-bundle-2026-10-05'
for (const page of ['One_Hand_Maces','Two_Hand_Maces']) {
  const s=JSON.parse(fs.readFileSync(`${root}/${page}.us.json`))
  console.log(page,s.data.baseitem.name,s.data.normal.length)
  console.log(s.cards)
  console.log(s.data.perfect_essence.map(r=>[clean(r.Name),r.Code,clean(r.str)]))
  console.log(s.data.normal.filter(r=>!['1','2'].includes(r.ModGenerationTypeID)))
}
for(const slug of ['Fortified_Hammer','Strife_Pick','Akoyan_Club','Ruination_Maul','Fanatic_Greathammer','Tawhoan_Greatclub']) {
  const html=fs.readFileSync(`${root}/${slug}.us.html`,'utf8')
  console.log(slug,[...html.matchAll(/<h5 class="card-header">[^]*?<\/h5>\s*<table[^]*?<\/table>/g)].filter(m=>m[0].includes('<tr><th>Family')).map(m=>clean(m[0])))
}
