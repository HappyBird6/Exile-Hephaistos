import fs from './evidence-files.cjs'
const root='evidence:quarterstaves-spears-source'
for(const name of ['Aegis_Quarterstaff','Bolting_Quarterstaff','Dreaming_Quarterstaff','Grand_Spear','Flying_Spear','Akoyan_Spear']) {
 const html=fs.readFileSync(`${root}/${name}.us.html`,'utf8')
 console.log(name,[...html.matchAll(/<h5 class="card-header">[^]*?<\/table>/g)].filter(m=>m[0].includes('<tr><th>Family')).map(m=>m[0]))
 console.log(html.match(/Grants Skill[^]*?<\/div>/)?.[0])
}
