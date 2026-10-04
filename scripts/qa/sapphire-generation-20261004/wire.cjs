const fs=require('fs');
const liquids={DILUTED_LIQUID_IRE:['Diluted_Liquid_Ire','JewelEnergyShield'],DILUTED_LIQUID_GUILT:['Diluted_Liquid_Guilt','JewelColdDamage'],DILUTED_LIQUID_GREED:['Diluted_Liquid_Greed','JewelChaosDamage'],LIQUID_PARANOIA:['Liquid_Paranoia','JewelCastSpeed'],LIQUID_ENVY:['Liquid_Envy','JewelSpellDamage'],LIQUID_DISGUST:['Liquid_Disgust','JewelManaonKill'],LIQUID_DESPAIR:['Liquid_Despair','JewelSpellCriticalChance'],CONCENTRATED_LIQUID_FEAR:['Concentrated_Liquid_Fear','JewelSpellCriticalDamage'],CONCENTRATED_LIQUID_SUFFERING:['Concentrated_Liquid_Suffering','JewelAreaofEffect'],CONCENTRATED_LIQUID_ISOLATION:['Concentrated_Liquid_Isolation','JewelMaximumColdResistance']};
function edit(p,f){const s=fs.readFileSync(p,'utf8'),next=f(s);if(next===s)throw Error('No edit: '+p);fs.writeFileSync(p,next)}
edit('frontend/src/features/crafting/workbenchApi.ts',s=>s
 .replace('export type WorkbenchAction =','export type WorkbenchAction =\n'+Object.keys(liquids).map(a=>"  | '"+a+"'").join('\n'))
 .replace('> = {\n  PERFECT_ESSENCE_MIND:', '> = {\n'+Object.entries(liquids).map(([a,[,code]])=>`  ${a}: ['sapphire:crafted:${code}'],`).join('\n')+'\n  PERFECT_ESSENCE_MIND:')
 .replace('export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {','export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {\n'+Object.entries(liquids).map(([a,[id]])=>`  ${id}: '${a}',`).join('\n'))
 .replace('export const workbenchActionNames: Record<WorkbenchAction, string> = {','export const workbenchActionNames: Record<WorkbenchAction, string> = {\n'+Object.entries(liquids).map(([a,[id]])=>`  ${a}: '${id.replaceAll('_',' ')}',`).join('\n'))
 .replace("!['RUNIC_ALLOY'", "!['RUNIC_ALLOY'")
 .replace("    next.rarity === 'NORMAL' ? 0 : next.rarity === 'MAGIC' ? 1 : 3", "    next.rarity === 'NORMAL' ? 0 : next.rarity === 'MAGIC' ? 1 : next.baseItemId === sapphireBase ? 2 : 3")
 .replace("        o.trigger === 'ESSENCE_HYSTERIA' &&", "        !action.includes('LIQUID_') &&\n        o.trigger === 'ESSENCE_HYSTERIA' &&"));
edit('frontend/src/features/crafting/workbenchHistory.ts',s=>s.replace("state.rarity === 'NORMAL' ? 0 : state.rarity === 'MAGIC' ? 1 : 3", "state.rarity === 'NORMAL' ? 0 : state.rarity === 'MAGIC' ? 1 : state.baseItemId === sapphireBase ? 2 : 3"));
edit('frontend/src/features/crafting/CraftingPage.tsx',s=>s.replaceAll("'MAGIC' | 'RARE'", "'NORMAL' | 'MAGIC' | 'RARE'").replace('<option value="MAGIC">{t(\'ui.magic\')}</option>', '<option value="NORMAL">{t(\'ui.normal\')}</option>\n                        <option value="MAGIC">{t(\'ui.magic\')}</option>').replace('checked={sapphireAffix}', "checked={sapphireAffix && sapphireRarity !== 'NORMAL'}\n                          disabled={sapphireRarity === 'NORMAL'}").replace('sapphireAffix ? Number(sapphireRoll) : null', "sapphireAffix && sapphireRarity !== 'NORMAL' ? Number(sapphireRoll) : null"));
const simulator='backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java';
edit(simulator,s=>s.replaceAll('long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();','recordSapphireSelection(candidates, assumptions);\n        long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();').replace('  private ModifierInstance roll(',`  private void recordSapphireSelection(List<ModifierDefinition> candidates, List<Assumption> assumptions) {
    if (catalog.base().id().equals(SapphireJewel.BASE_ID))
      assumptions.add(new Assumption("sapphire-uniform-candidates-v1", "eligible Sapphire ordinary modifier", candidates.size(), candidates.stream().map(ModifierDefinition::id).toList(), null, null, "https://poe2db.tw/us/Sapphire", "USER-APPROVED SIMULATOR MODEL: equal 1/N among the eligible normal-section candidates after family, side and level restrictions. Actual game spawn weights are unavailable; DropChance=1 is not a verified weight."));
  }

  private ModifierInstance roll(`));
console.log('Wired ten Liquid actions and Sapphire generation state.');
