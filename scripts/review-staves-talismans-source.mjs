import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const report={retrievedAt:new Date().toISOString(),classes:[],bases:[],skills:[],availability:'Current PoE2DB ordinary class listing plus source normal/magic/rare enable flags; no market drop-rate inference',combatSimulation:false}
for(const page of ['Staves','Talismans']) {
 const source=read(`${root}/${page}.us.json`)
 report.classes.push({page,classId:source.data.baseitem.name,ordinary:source.data.normal.length,essence:source.data.essence.length,perfectEssence:source.data.perfect_essence.length,sourceUrl:source.url,sha256:source.sha256,roster:source.cards})
}
for(const slug of ['Permafrost_Staff','Reflecting_Staff','Dark_Staff','Ravenous_Staff','Perching_Staff','Sanctified_Staff','Maji_Talisman','Fungal_Talisman','Jade_Talisman']) {
 const source=read(`${root}/${slug}.us.json`),b=source.variants[0],html=fs.readFileSync(`${root}/${slug}.us.html`,'utf8')
 assert.equal(crypto.createHash('sha256').update(html).digest('hex'),source.sha256)
 assert(read(`${root}/${b.fields.Class}.us.json`).cards.some(c=>c.slug===slug))
 assert.equal(b.fields['Mods.enable_rarity'],'normal, magic, rare, unique')
 report.bases.push({slug,id:b.fields.Type,sourceUrl:source.url,sha256:source.sha256,requiredLevel:+b.fields.DropLevel,requirements:b.requirements,tags:b.fields.Tags,properties:b.properties,rationale:b.fields.Class==='Talismans'?'Highest-tier selected Str/Int family: Maji rage, Fungal speed, Jade damage/implicit sidegrade':'Highest required-level and distinct granted-skill selected families; Sanctified retains Consecrate family'})
 if(b.fields.Class==='Staves') {
  const match=html.match(/Grants Skill: <a[^>]*href="([^"]+)"[^>]*>([^<]+)<\/a>/)
  assert(match)
  const url=new URL(match[1],source.url).href,path=`${root}/skill-${slug}.us.html`
  if(!fs.existsSync(path)) { const response=await fetch(url);assert(response.ok);fs.writeFileSync(path,await response.text(),{flag:'wx'}) }
  const skillHtml=fs.readFileSync(path,'utf8')
  report.skills.push({base:b.fields.Type,name:clean(match[2]),url,sha256:crypto.createHash('sha256').update(skillHtml).digest('hex'),fields:[...skillHtml.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])]),displayOnly:true})
 }
}
fs.writeFileSync(`${root}/roster-review.json`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log(report.classes.map(c=>[c.page,c.ordinary,c.essence,c.perfectEssence]))
