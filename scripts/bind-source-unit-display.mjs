// Exact source endpoint correspondence; no game precision or rounding is inferred.
import fs from 'node:fs'
import assert from 'node:assert/strict'
const file='frontend/src/shared/i18n/modifierTemplates.json',data=JSON.parse(fs.readFileSync(file,'utf8'))
const divisors={base_life_regeneration_rate_per_minute:60,allies_in_presence_life_regeneration_rate_per_minute:60,base_life_leech_from_physical_attack_damage_permyriad:100,base_mana_leech_from_physical_attack_damage_permyriad:100,local_life_leech_from_physical_damage_permyriad:100,local_mana_leech_from_physical_damage_permyriad:100,local_critical_strike_chance:100,'self_bleed_duration_+%':-1,'self_poison_duration_+%':-1,'flask_charges_used_+%':-1,'charm_charges_used_+%':-1}
const mappings=[]
for(const[id,b]of Object.entries(data.definitions)) {
 if(b.valueStats||b.stats.length!==1||b.values.length!==1)continue
 const s=b.stats[0],divisor=divisors[s.id]
 if(!divisor||s.min===s.max)continue
 const display=b.values[0].match(/-?\d+(?:\.\d+)?/g)?.map(Number)
 if(display?.length!==2)continue
 const projected=[s.min/divisor,s.max/divisor].sort((a,b)=>a-b)
 assert.deepEqual(display,projected,'Source display endpoint mismatch: '+id)
 b.valueStats=[{id:s.id,divisor}]
 mappings.push({id,sourceCode:b.sourceCode,stat:s,displaySpan:b.values[0],divisor})
}
assert.equal(mappings.length,103)
fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-source-unit-display-2026-10-04.json',JSON.stringify({count:mappings.length,method:'Verified canonical stat IDs and both source display endpoints: per-minute /60; permyriad and local crit /100; explicit reduced percentage sign reversal. Formatting adds no rounding to canonical values or game precision claim.',mappings},null,2)+'\n')
