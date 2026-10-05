import fs from 'node:fs'
import assert from 'node:assert/strict'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/wands-source-bundle-2026-10-05'
const roster = JSON.parse(fs.readFileSync(`${root}/applicability.json`))
const identities = []
for (const b of roster.bases) {
  const html = fs.readFileSync(`${root}/${b.name}_Wand.us.html`,'utf8')
  const match = html.match(/Grants Skill: <a[^>]*href="([^"]+)"[^>]*>([^<]+)<\/a>/)
  assert(match && clean(match[2]) === b.skill)
  const url = `https://poe2db.tw${match[1]}`
  const response = await fetch(url)
  assert(response.ok)
  const skillHtml = await response.text()
  const path = `${root}/${b.skill.replaceAll(' ','_')}.us.html`
  assert(!fs.existsSync(path),'Preserve previous evidence')
  fs.writeFileSync(path,skillHtml)
  const fields = [...skillHtml.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])])
  identities.push({base:b.id,name:b.skill,url,fields,displayOnly:true})
}
fs.writeFileSync(`${root}/skill-identities.json`,JSON.stringify(identities,null,2)+'\n',{flag:'wx'})
