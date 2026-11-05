import fs from './evidence-files.cjs'
import assert from 'node:assert/strict'
const root='evidence:staves-talismans-source'
const roster=JSON.parse(fs.readFileSync(`${root}/roster-review.json`)),bases=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/top-bases.json'))
const identities=roster.skills.map(skill=>{
 const field=name=>skill.fields.find(([key])=>key===name)?.[1]
 const base=Object.values(bases).find(b=>b.id===skill.base)
 assert(base);assert(field('Id'));assert(field('ItemType'));assert(field('ActiveSkillsCode'))
 return {baseId:skill.base,baseKey:base.key,baseRequiredCharacterLevel:base.requiredLevel,name:skill.name,sourceSkillId:field('Id'),sourceItemType:field('ItemType'),activeSkillsCode:field('ActiveSkillsCode'),skillTypes:field('Type'),sourceUrl:skill.url,sourceSha256:skill.sha256,sourceLocaleLines:base.skillLines,displayOnly:true,skillLevelModel:'No fixed granted level is stated in the base popup; level scaling and combat are not simulated'}
})
assert.equal(identities.length,6)
fs.writeFileSync(`${root}/skill-identities.json`,JSON.stringify({passed:true,identities},null,2)+'\n',{flag:'wx'})
console.log(identities.map(s=>[s.baseKey,s.sourceSkillId]))
