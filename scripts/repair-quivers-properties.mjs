import fs from 'node:fs'
import assert from 'node:assert/strict'
import {cleanSource as clean} from './armour-source.mjs'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const root='docs/evidence/quivers-source-bundle-2026-10-05'
const bases=read('frontend/src/features/crafting/topBases.json'),terms=read('frontend/src/shared/i18n/gameTerms.json'),registry=read('backend/src/main/resources/crafting/registry-v2.json')
for(const b of Object.values(bases).filter(b=>b.family==='quivers')) {
 for(const [l,r]of Object.entries({en:'us',ko:'kr',ja:'jp','zh-CN':'cn','zh-TW':'tw',es:'sp'})) {
  const html=fs.readFileSync(`${root}/${b.slug}.${r}.html`,'utf8')
  const popup=html.match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)[0]
  const description=clean(popup.match(/<div class="default fst-italic">([^]*?)<\/div>/)[1])
  assert(description)
  b.sourceProperties[l]=[description]
  terms[l][b.slug].lines=[description]
 }
 registry.workbenchBases[b.key].sourceProperties=b.sourceProperties.en
}
for(const p of ['frontend/src/features/crafting/topBases.json','backend/src/main/resources/catalog/top-bases.json'])fs.writeFileSync(p,JSON.stringify(bases,null,2)+'\n')
fs.writeFileSync('frontend/src/shared/i18n/gameTerms.json',JSON.stringify(terms,null,2)+'\n')
fs.writeFileSync('backend/src/main/resources/crafting/registry-v2.json',JSON.stringify(registry,null,2)+'\n')
