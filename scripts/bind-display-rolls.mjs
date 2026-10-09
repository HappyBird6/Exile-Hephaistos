// Reviewed display order. Source stat IDs/bounds and English source spans must agree.
import fs from 'node:fs'
import assert from 'node:assert/strict'
const file='frontend/src/shared/i18n/modifierTemplates.json'
const data=JSON.parse(fs.readFileSync(file,'utf8'))
const mappings=[]
for(const [id,b]of Object.entries(data.definitions)) {
  if(b.stats.length<2 || !b.stats.some(s=>s.min!==s.max) || /^(ruby|emerald|sapphire|diamond|time-lost-)/.test(id))continue
  const stats=b.stats
  let order
  if(stats.every(s=>/_(minimum|maximum)_.*damage/.test(s.id))) order=[stats.find(s=>s.id.includes('_minimum_')),stats.find(s=>s.id.includes('_maximum_'))]
  else if(/increased Armour/.test(b.englishText)) {
    const percent=stats.find(s=>s.id==='local_physical_damage_reduction_rating_+%'),other=stats.find(s=>s.id!=='local_physical_damage_reduction_rating_+%')
    order=b.englishText.startsWith('+')?[other,percent]:[percent,other]
  }
  else if(/increased Physical Damage/.test(b.englishText))order=[stats.find(s=>s.id==='local_physical_damage_+%'),stats.find(s=>s.id==='local_accuracy_rating')]
  else if(/increased Spell Damage/.test(b.englishText))order=[stats.find(s=>s.id==='spell_damage_+%'),stats.find(s=>s.id==='base_maximum_mana')]
  else if(/increased Spirit/.test(b.englishText))order=[stats.find(s=>s.id==='local_spirit_+%'),stats.find(s=>s.id==='base_maximum_mana')]
  else if(/Light Radius/.test(b.englishText)) {
    const light=stats.find(s=>s.id==='light_radius_+%'), other=stats.find(s=>s.id!=='light_radius_+%')
    order=b.englishText.indexOf('Light Radius')<b.englishText.search(/Accuracy|Mana Regeneration/)?[light,other]:[other,light]
  }
  assert(order?.every(Boolean)&&order.length===b.values.length,'Unreviewed display order: '+id)
  order.forEach((s,i)=>{
    const nums=b.values[i].match(/-?\d+(?:\.\d+)?/g).map(Number)
    assert.deepEqual(nums.length===1?[nums[0],nums[0]]:nums,[s.min,s.max],id+':'+s.id)
  })
  b.valueStats=order.map(s=>({id:s.id,divisor:1}))
  mappings.push({id,sourceCode:b.sourceCode,values:b.values,valueStats:b.valueStats})
}
assert.equal(mappings.length,228)
fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-compound-rolls-2026-10-04.json',JSON.stringify({method:'Reviewed English stat semantics/order, canonical stat identity and exact numeric bounds; no inferred conversion',count:mappings.length,mappings},null,2)+'\n')
