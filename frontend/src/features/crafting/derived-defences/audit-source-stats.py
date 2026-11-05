from pathlib import Path
import json
root=Path(__file__).resolve().parents[5]
stats={
 'local_base_physical_damage_reduction_rating':'armour',
 'local_base_evasion_rating':'evasion',
 'local_energy_shield':'energy_shield',
 'local_physical_damage_reduction_rating_+%':'armour',
 'local_evasion_rating_+%':'evasion',
 'local_energy_shield_+%':'energy_shield',
 'local_armour_and_evasion_+%':'armour',
 'local_armour_and_energy_shield_+%':'energy_shield',
 'local_evasion_and_energy_shield_+%':'evasion',
}
summary={key:dict(definitions=0,pools=set()) for key in stats}
conflicts=[]
for file in (root/'backend/src/main/resources/catalog').glob('*/catalog.json'):
    catalog=json.loads(file.read_text(encoding='utf-8'))
    for modifier in catalog['modifiers']:
        for stat in modifier['stats']:
            if stat['id'] not in stats: continue
            entry=summary[stat['id']]
            entry['definitions']+=1
            entry['pools'].add(file.parent.name)
            if stats[stat['id']] not in modifier['tags']: conflicts.append(dict(pool=file.parent.name,modifier=modifier['id'],stat=stat['id'],tags=modifier['tags']))
for entry in summary.values(): entry['pools']=sorted(entry['pools'])
frontend=json.loads((root/'frontend/src/features/crafting/topBases.json').read_text(encoding='utf-8'))
backend=json.loads((root/'backend/src/main/resources/catalog/top-bases.json').read_text(encoding='utf-8'))
report=dict(baseMirrorsAgree=frontend==backend,sourceStats=summary,missingModifierTags=conflicts,
    interpretation='Exact local stat IDs determine local defence semantics. Some reviewed modifier rows omit broad modifier tags; their numeric stat IDs are preserved and used without inventing tags.',
    formula='(base + local flat) * (1 + local increased / 100) * (1 + quality / 100)',
    rounding='No intermediate rounding. Formatting belongs to the card.',
    excludedGlobalStats=['base_physical_damage_reduction_rating','base_evasion_rating','base_maximum_energy_shield','physical_damage_reduction_rating_+%','evasion_rating_+%','maximum_energy_shield_+%','global_armour_evasion_energy_shield_+%'])
out=root/'docs/evidence/0.5.5/crafting/sanctification/sources/local-defence-stats.json'
out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
assert all(entry['definitions'] for entry in summary.values()) and frontend==backend
print('Nine local defence stat IDs audited; missing modifier tags are recorded, not invented. Base observations agree.')
