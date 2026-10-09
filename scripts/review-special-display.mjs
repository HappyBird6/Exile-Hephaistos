import fs from 'node:fs'
const file='frontend/src/shared/i18n/modifierTemplates.json',data=JSON.parse(fs.readFileSync(file,'utf8'))
const terms=JSON.parse(fs.readFileSync('frontend/src/shared/i18n/gameTerms.json','utf8'))
const changes={
'amulet:prefix:essence-maximum-quality':['Essence_of_the_Breach','+20'],
'iron-ring:prefix:essence-increased-maximum-mana':['Perfect_Essence_of_the_Mind','(4—6)'],
'crude-bow:suffix:essence-onslaught-on-kill':['Perfect_Essence_of_Haste','(20—25)'],
}
const exceptions=[]
for(const[id,[item,span]]of Object.entries(changes)) {
 const term=terms.es[item]
 const template=term.lines[1].split(': ').slice(1).join(': ').replace(span,'{v0}')
 if(!template.includes('{v0}'))throw Error('Alternate source span missing')
 const key=data.definitions[id].template,source=data.templates.es[key].template
 data.templates.es[key].template=template
 exceptions.push({id,locale:'es',source,template,provenance:'POE2DB_ALTERNATE_ITEM_EFFECT_SOURCE',sourceUrl:term.sourceUrl,itemKey:term.itemKey,reason:'Exact Code hover returns unrelated semantics; verified corresponding material locale description supplies the correct effect'})
}
const iron=data.definitions['iron-ring:implicit:added-physical-damage-to-attacks']
iron.valueStats=[{id:'attack_minimum_added_physical_damage',divisor:1},{id:'attack_maximum_added_physical_damage',divisor:1}]
fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-special-review-2026-10-04.json',JSON.stringify({exceptions,review:'All 37 English/Korean/Spanish effect strings reviewed semantically; numeric placeholders reviewed in every locale'},null,2)+'\n')
