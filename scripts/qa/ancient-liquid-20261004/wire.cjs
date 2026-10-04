const fs=require('fs'),assert=require('assert/strict');
function edit(p,f){const s=fs.readFileSync(p,'utf8'),n=f(s);assert.notEqual(n,s,p);fs.writeFileSync(p,n)}
const be='backend/src/main/java/com/poe2craft/',fe='frontend/src/features/crafting/';
const bases=['time-lost-ruby','time-lost-emerald','time-lost-sapphire','time-lost-diamond'];
const catalogs=Object.fromEntries(bases.map(b=>[b,JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/'+b+'/catalog.json'))]));
const ids=Object.fromEntries(bases.map(b=>[b,catalogs[b].base.id]));
const target=JSON.parse(fs.readFileSync(fe+'basicJewelLiquidTargets.json'));
const actions=Object.keys(target['time-lost-sapphire']);assert.equal(actions.length,13);
edit(be+'item/BasicJewel.java',s=>s.replace('  public static int extra(',`  public static boolean timeLost(String base) {
    return Set.of(${Object.values(ids).map(id=>'"'+id+'"').join(',')}).contains(base);
  }

  public static boolean supportedCrafting(String base) {
    return supported(base) || timeLost(base);
  }

  public static int extra(`));
for(const f of ['item/ItemStateValidator.java','crafting/domain/CraftingEngine.java','crafting/domain/ModifierPoolResolver.java'])edit(be+f,s=>s.replaceAll('BasicJewel.supported(','BasicJewel.supportedCrafting('));
edit(be+'crafting/domain/WorkbenchCurrency.java',s=>s.replace('  POTENT_LIQUID_MELANCHOLY',actions.map(a=>'  '+a+'(null, 0),').join('\n')+'\n  POTENT_LIQUID_MELANCHOLY'));
edit(be+'crafting/domain/WorkbenchSimulator.java',s=>{
 s=s.replaceAll('BasicJewel.supported(','BasicJewel.supportedCrafting(');
 // Catalyst eligibility remains Basic only; source Time-Lost tags omit jewel_catalyst.
 s=s.replace('if (!BasicJewel.supportedCrafting(state.baseItemId()))\n          return blocked(action, "Refined catalysts require the supported Sapphire Jewel.");','if (!BasicJewel.supported(state.baseItemId()))\n          return blocked(action, "Refined catalysts require a supported Basic Jewel with jewel_catalyst source tag.");');
 s=s.replace('    if (action.isLiquid() && BasicJewel.supportedCrafting(catalog.base().id()))','    if (action.isLiquid())');
 s=s.replace('return "basic-jewel-potent-v3"','return BasicJewel.timeLost(catalog.base().id()) ? "time-lost-ancient-v1" : "basic-jewel-potent-v3"');
 s=s.replace('No sourced Liquid outcome for this Basic Jewel base.','No sourced Liquid outcome for this Jewel category and base.');
 s=s.replace('This operation remains outside the reviewed Sapphire basic currency and Liquid scope.','This operation remains outside the reviewed Jewel basic currency and Liquid scope.');
 s=s.replace('eligible Basic Jewel ordinary modifier','eligible per-base Jewel ordinary modifier');
 return s;
});
edit(be+'item/infrastructure/ItemCatalogLoader.java',s=>s.replace('Set.of("ruby", "emerald", "diamond")','Set.of("ruby", "emerald", "diamond", '+bases.map(b=>'"'+b+'"').join(', ')+')'));
edit(be+'crafting/application/WorkbenchService.java',s=>{
 const marker='    var extras = new ItemCatalog[] {ruby, emerald, diamond};';
 const pos=s.indexOf(marker);assert(pos>=0);
 const start=s.lastIndexOf('  public WorkbenchService(',pos);
 const signature=s.slice(start,pos).replace('      ItemCatalog diamond) {','      ItemCatalog diamond, ItemCatalog... timeLost) {');
 const delegation=s.slice(start,pos)+`    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, belt, helmet, ring, sapphire, ruby, emerald, diamond, new ItemCatalog[0]);
  }

`;
 s=s.slice(0,start)+delegation+signature+s.slice(pos);
 s=s.replace('    this.sapphire = sapphire;',`    for (var c : timeLost) {
      String name = "time-lost-" + c.base().name().substring("Time-Lost ".length()).toLowerCase(java.util.Locale.ROOT);
      basicJewels.put(name, c);
      basicJewelSimulators.put(c.base().id(), new WorkbenchSimulator(c, new CraftingEngine(c)));
    }
    this.sapphire = sapphire;`);
 return s;
});
edit(be+'bootstrap/CraftingConfiguration.java',s=>s.replace('ItemCatalogLoader.loadBasicJewel("diamond"));','ItemCatalogLoader.loadBasicJewel("diamond"), '+bases.map(b=>'ItemCatalogLoader.loadBasicJewel("'+b+'")').join(', ')+');'));
edit(fe+'basicJewel.ts',s=>s.replace('export function isBasicJewel',`export const timeLostJewelBases = ${JSON.stringify(ids,null,2)} as const
export const workbenchJewelBases = { ...basicJewelBases, ...timeLostJewelBases } as const
export function isWorkbenchJewel(base: string): boolean {
  return Object.values(workbenchJewelBases).some(id=>id===base)
}
export function isBasicJewel`).replace('!isBasicJewel(state.baseItemId)','!isWorkbenchJewel(state.baseItemId)'));
edit(fe+'workbenchHistory.ts',s=>s.replaceAll('isBasicJewel','isWorkbenchJewel'));
edit(fe+'workbenchApi.ts',s=>{
 s=s.replaceAll('isBasicJewel','isWorkbenchJewel').replace('import { isWorkbenchJewel,','import { isBasicJewel, workbenchJewelBases, isWorkbenchJewel,');
 s=s.replace("action.startsWith('REFINED_') !== isWorkbenchJewel(state.baseItemId)","action.startsWith('REFINED_') !== isBasicJewel(state.baseItemId)");
 s=s.replace("export type WorkbenchAction =",'export type WorkbenchAction =\n'+actions.map(a=>"  | '"+a+"'").join('\n'));
 s=s.replace('export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {','export const workbenchCurrencyActions: Record<string, WorkbenchAction> = {\n'+actions.map(a=>{const id=a.split('_').map(p=>p[0]+p.slice(1).toLowerCase()).join('_');return "  "+id+": '"+a+"',"}).join('\n'));
 const first="  POTENT_LIQUID_MELANCHOLY: 'Potent Liquid Melancholy',";
 s=s.replace(first,actions.map(a=>"  "+a+": '"+a.split('_').map(p=>p[0]+p.slice(1).toLowerCase()).join(' ')+"',").join('\n')+'\n'+first);
 const start=s.indexOf('  const baseName = Object.keys(liquidTargets).find('),end=s.indexOf('  const replacementTargets',start);assert(start>=0&&end>start);
 s=s.slice(0,start)+`  const baseName = Object.keys(workbenchJewelBases).find(b => (workbenchJewelBases as Record<string,string>)[b] === state.baseItemId)
`+s.slice(end);
 s=s.replace("            'Metadata/Items/Belts/FourBelt1',",Object.values(ids).map(id=>"            '"+id+"',").join('\n')+"\n            'Metadata/Items/Belts/FourBelt1',");
 return s;
});
edit(fe+'craftingApi.ts',s=>s.replace("    | 'ruby'",bases.map(b=>"    | '"+b+"'").join('\n')+"\n    | 'ruby'").replace("(['ruby', 'emerald', 'diamond'].includes(base)","(['ruby', 'emerald', 'diamond', "+bases.map(b=>"'"+b+"'").join(', ')+"].includes(base)").replace("                  ruby: 'Metadata/Items/Jewels/JewelStr',",Object.entries(ids).map(([b,id])=>"                  '"+b+"': '"+id+"',").join('\n')+"\n                  ruby: 'Metadata/Items/Jewels/JewelStr',"));
edit(fe+'CraftingPage.tsx',s=>{
 s=s.replace("import { basicJewelBases, isBasicJewel } from './basicJewel'","import { basicJewelBases, workbenchJewelBases, isWorkbenchJewel } from './basicJewel'");
 s=s.replaceAll('isBasicJewel','isWorkbenchJewel');
 // Session dispatch and rarity editor use all reviewed Jewels; quality controls remain Basic.
 s=s.replaceAll('Object.keys(basicJewelBases)','Object.keys(workbenchJewelBases)').replaceAll('Object.values(basicJewelBases)','Object.values(workbenchJewelBases)').replaceAll('keyof typeof basicJewelBases','keyof typeof workbenchJewelBases').replaceAll('basicJewelBases[b as','workbenchJewelBases[b as');
 for(const v of ['catalogBase','base'])s=s.replaceAll('Object.hasOwn(basicJewelBases, '+v+')','Object.hasOwn(workbenchJewelBases, '+v+')');
 const rarityAt=s.indexOf('id="sapphire-rarity"'),outerAt=s.lastIndexOf('Object.hasOwn(basicJewelBases, baseChoice)',rarityAt);assert(outerAt>=0);s=s.slice(0,outerAt)+s.slice(outerAt).replace('Object.hasOwn(basicJewelBases, baseChoice)','Object.hasOwn(workbenchJewelBases, baseChoice)');
 s=s.replace("  ruby: 'Ruby',",Object.keys(ids).map(b=>"  '"+b+"': 'Time-Lost "+b.split('-').pop().replace(/^./,c=>c.toUpperCase())+"',").join('\n')+"\n  ruby: 'Ruby',");
 s=s.replaceAll("    | 'ruby'",bases.map(b=>"    | '"+b+"'").join('\n')+"\n    | 'ruby'");
 s=s.replace('                    <option value="ruby">',bases.map(b=>`                    <option value="${b}">{name('${b.replaceAll('-','_').replace(/(^|_)([a-z])/g,(m,p,c)=>p+c.toUpperCase())}', '${catalogs[b].base.name}')}</option>`).join('\n')+'\n                    <option value="ruby">');
 return s;
});
const rf='backend/src/main/resources/crafting/registry-v2.json',registry=JSON.parse(fs.readFileSync(rf));
for(const r of registry.entries){const a=r.id.toUpperCase(),supported=bases.filter(b=>target[b][a]);if(supported.length){r.action=a;r.effectStatus='IMPLEMENTED';r.serviceScope='ACTIVE';r.supportedBases=supported;r.solarStatus='NOT_APPLICABLE';r.ruleSource='https://poe2db.tw/us/'+r.id;r.ruleVerifiedAt='2026-10-04';r.reason='Exact Time-Lost base Crafted outcomes; conditional radius stats only. One Crafted and disclosed uniform legal removal/outcome model.'}else if(r.supportedBases?.includes('sapphire')&& !r.id.startsWith('Refined_')&&r.category!=='LIQUID_EMOTION')r.supportedBases=[...new Set([...r.supportedBases,...bases])]}
fs.writeFileSync(rf,JSON.stringify(registry,null,2)+'\n');
