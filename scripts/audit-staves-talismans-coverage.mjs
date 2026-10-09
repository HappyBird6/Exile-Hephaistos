import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='docs/evidence/staves-talismans-source-bundle-2026-10-05'
const read=p=>JSON.parse(fs.readFileSync(p,'utf8'))
const bases=read('backend/src/main/resources/catalog/top-bases.json'),policies=read('backend/src/main/resources/catalog/base-policies.json')
const classes=[]
for(const [page,family,count]of [['Staves','staves',6],['Talismans','talismans',3]]) {
 const source=read(`${root}/${page}.us.json`)
 const ordinary=source.cards.filter(c=>! /Runeforged|Runemastered/.test(c.slug)).map(c=>({...c,requiredLevel:+(c.card.match(/Level (\d+)/)?.[1]??0)})).sort((a,b)=>b.requiredLevel-a.requiredLevel)
 const selected=Object.values(bases).filter(b=>b.family===family)
 assert.deepEqual(selected.map(b=>b.slug).sort(),ordinary.slice(0,count).map(c=>c.slug).sort(),'Exact selected highest-requirement roster')
 classes.push({page,sourceUrl:source.url,sourceSha256:source.sha256,listedOrdinaryBases:ordinary.length,selected: selected.map(b=>({key:b.key,id:b.id,requiredLevel:b.requiredLevel,skill:b.skillLines?.en??[],implicit:b.implicitStats})),unselected:ordinary.filter(c=>!selected.some(b=>b.slug===c.slug)).map(c=>({slug:c.slug,requiredLevel:c.requiredLevel})),rationale:'Highest required character level ordinary roster; captures selected distinct skill/profile sidegrades, does not imply best damage or all skill families'})
}
const knownReleasedEquipmentClasses=['Wands','Sceptres','Bows','Crossbows','One Hand Maces','Two Hand Maces','Quarterstaves','Spears','Staves','Talismans','Shields','Bucklers','Foci','Quivers','Gloves','Boots','Body Armours','Helmets','Rings','Amulets','Belts']
const represented=new Set(Object.values(policies.families).map(p=>p.itemClass))
const releasedRepresentativeClassGaps=knownReleasedEquipmentClasses.filter(c=>!represented.has(c))
assert.deepEqual(releasedRepresentativeClassGaps,[])
const report={passed:true,totalBases:Object.keys(bases).length+Object.keys(policies.legacy).length,selectedRosterComplete:true,classes,knownReleasedEquipmentClasses,releasedRepresentativeClassGaps,releaseUnverifiedCandidateClasses:['Claws','Daggers','One Hand Swords','Two Hand Swords','One Hand Axes','Two Hand Axes','Flails','Traps'],classReleaseEvidence:['https://www.pathofexile.com/forum/view-thread/3932540','https://www.pathofexile.com/forum/view-forum/2212'],outsideRequestedWorkbenchScope:['Life Flasks','Mana Flasks','Charms','Unique item mechanics','Runeforged/Runemastered variants','Combat','Shapeshift execution','Passive tree'],boundedUnselectedSidegrades:['Crossbows Stout/Dedalian/Cumbrous','Unselected lower-requirement Staff granted-skill families','Unselected Talisman profiles'],unchangedRestrictions:['Stocky empty socket0/1 execution only','deferred50','Support/Explorer Solar only'],availabilityLimits:{ordinaryEvidence:'Current PoE2DB ordinary class listing plus normal/magic/rare flags; released class confirmed by public GGG patch notes',gggTradeApi:'HTTP403; exact unnamed trade entry and drop source remain independently unverified'},wholeGameComplete:false}
fs.writeFileSync(`${root}/coverage-audit.json`,JSON.stringify(report,null,2)+'\n',{flag:'wx'})
console.log('Selected highest-requirement roster complete; bounded class audit',report.totalBases,classes.map(c=>[c.page,c.listedOrdinaryBases,c.unselected.length]))
