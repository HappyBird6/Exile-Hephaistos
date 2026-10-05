import fs from 'node:fs'
import assert from 'node:assert/strict'
import {cleanSource as clean} from './armour-source.mjs'
const root='docs/evidence/quarterstaves-spears-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const bases=read('backend/src/main/resources/catalog/top-bases.json')
const terms=read('frontend/src/shared/i18n/gameTerms.json')
for(const key of ['grand-spear','flying-spear','akoyan-spear']) {
 const b=bases[key]
 b.skillLines={}
 for(const [l,r]of Object.entries({en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'})) {
  const html=fs.readFileSync(`${root}/${b.slug}.${r}.html`,'utf8')
  const popup=html.match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)[0]
  const line=clean(popup.match(/<div[^>]*>[^]*?class="grantsSkill">([^]*?)<\/div>/)?.[1]??'')
  assert(line&&line.includes(' '),`${key}/${l}: source granted skill`)
  b.skillLines[l]=[line]
  terms[l][b.slug].lines=[...b.sourceProperties[l],line]
 }
}
for(const p of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json'])fs.writeFileSync(p,JSON.stringify(bases,null,2)+'\n')
fs.writeFileSync('frontend/src/shared/i18n/gameTerms.json',JSON.stringify(terms,null,2)+'\n')
