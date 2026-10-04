import fs from 'node:fs'
import { createHash } from 'node:crypto'
const root=process.argv[2]
const file='frontend/src/shared/i18n/gameTerms.json',terms=JSON.parse(fs.readFileSync(file,'utf8'))
const evidence={pages:[],ownTranslations:[]}
const ownNames={Refined_Necrotic_Catalyst:'Catalizador necrótico refinado',Liquid_Verisium:'Verisium líquido',Yaomacs_Orb_of_Sacrifice:'Orbe de sacrificio de Yaomac',Kopecs_Orb_of_Sacrifice:'Orbe de sacrificio de Kopec',Kamasas_Orb_of_Sacrifice:'Orbe de sacrificio de Kamasa',Yuguls_Orb_of_Sacrifice:'Orbe de sacrificio de Yugul'}
for(const id of ['Refined_Necrotic_Catalyst','Necrotic_Catalyst','Liquid_Verisium','Yaomacs_Orb_of_Sacrifice','Kopecs_Orb_of_Sacrifice','Kamasas_Orb_of_Sacrifice','Yuguls_Orb_of_Sacrifice']) {
 const html=fs.readFileSync(root+'/es-'+id+'.html','utf8')
 const clean=s=>s.replace(/<br\s*\/?>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&amp;/g,'&').trim()
 const lines=[...html.matchAll(/<div class="(?:explicitMod|descrText)">(.*?)<\/div>/gs)].map(m=>clean(m[1])).filter(Boolean)
 const expected=terms.en[id].itemKey
 if(!html.includes(expected))throw Error('Spanish source identity missing '+id)
 const name=ownNames[id]??terms.es[id].name
 terms.es[id]={name,lines,itemKey:expected,sourceUrl:'https://poe2db.tw/sp/'+id,...(ownNames[id]?{nameProvenance:'OWN_TRANSLATION_SOURCE_NAME_UNAVAILABLE'}:{}),...(id==='Liquid_Verisium'?{linesLanguage:'en'}:{})}
 evidence.pages.push({url:'https://poe2db.tw/sp/'+id,sha256:createHash('sha256').update(html).digest('hex'),descriptionProvenance:'POE2DB_LOCALE_PAGE'})
 if(ownNames[id])evidence.ownTranslations.push({id,locale:'es',field:'name',text:name,reason:'Category omits the localized name and direct locale page title/name contains canonical slug; stable asset identity verified against English. Liquid Verisium description remains original English and is labeled as such.'})
}
fs.writeFileSync(file,JSON.stringify(terms,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-spanish-gaps-2026-10-04.json',JSON.stringify(evidence,null,2)+'\n')
console.log(JSON.stringify(evidence))
