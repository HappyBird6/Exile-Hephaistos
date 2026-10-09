import fs from 'node:fs'
import assert from 'node:assert/strict'
const edit=(p,f)=>{const before=fs.readFileSync(p,'utf8'),after=f(before);assert.notEqual(after,before,p);fs.writeFileSync(p,after)}
const root='frontend/src/features/crafting/'
for(const file of ['draft.ts','craftingApi.ts','CraftingPage.tsx'])edit(root+file,s=>{
 let count=0
 s=s.replace(/(?:[ \t]*\| '[a-z0-9-]+'\r?\n){60,}/g,()=>{count++;return '    | WorkbenchBaseKey\n'})
 assert(count>0,file)
 s=`import type { WorkbenchBaseKey } from './baseRegistry'\n`+s
 if(file==='draft.ts') {
  const a=s.indexOf('const baseTexts = {'),z=s.indexOf('\n}\ntype Draft',a)
  assert(a>0&&z>a)
  s=s.slice(0,a)+s.slice(z+3)
  s="import { baseTexts } from './baseRegistry'\n"+s
 }
 if(file==='CraftingPage.tsx') {
  const a=s.indexOf('const baseSlugs = {'),z=s.indexOf('\n} as const',a)
  assert(a>0&&z>a)
  s=s.slice(0,a)+s.slice(z+'\n} as const'.length)
  s="import { baseSlugs, baseClass, sourcePropertyUnscaled } from './baseRegistry'\n"+s
  s=s.replace(/itemClass: \['one-hand-maces', 'two-hand-maces'\][^]*?(?=itemLevel:)/g,'itemClass: baseClass(catalogBase),\n')
  s=s.replace(/text: \[\s*'quarterstaves',[^]*?\]\.includes\(topBase\(catalogBase\)\?\.family \?\? ''\)/,'text: sourcePropertyUnscaled(catalogBase)')
 }
 console.log(file,'unions removed',count)
 return s
})
edit(root+'topBases.ts',s=>{
 const a=s.indexOf('export const reviewedArmourKeys =')
 return s.slice(0,a)+`// Legacy entries keep their original selector position.
export const reviewedArmourKeys = (Object.keys(data) as TopBaseKey[]).filter(key => !['soldier','imperial'].includes(key))
`
})
edit(root+'workbenchApi.ts',s=>s.replace(/reviewedKey &&\s*\[\s*'gloves',[^]*?\]\.includes\(topBase\(reviewedKey\)\?\.family \?\? ''\)/,'reviewedKey'))
edit(root+'catalystQuality.ts',s=>{
 s="import { basePolicy } from './baseRegistry'\n"+s
 const a=s.indexOf('export const catalystBase ='),z=s.indexOf('\nexport function qualityShape',a)
 return s.slice(0,a)+'export const catalystBase = (base: string) => basePolicy(base)?.catalystQuality === true\n'+s.slice(z)
})
edit(root+'qualityLimit.ts',s=>{
 s=s.replace("import { topBase, topBaseKey } from './topBases'","import { basePolicy } from './baseRegistry'")
 s=s.replace("const stocky = 'Metadata/Items/Armours/Gloves/FourGlovesStr1'\n",'').replace("const solar = 'Metadata/Items/Amulets/FourAmulet9'\n",'')
 const a=s.indexOf('  if (',s.indexOf('export function maximumQuality')),z=s.indexOf('  let maximum = 20',a)
 s=s.slice(0,a)+'  if (basePolicy(state.baseItemId)?.qualityLimit !== true) return null\n'+s.slice(z)
 return s.replace(/\(state\.baseItemId !== solar &&[^]*?topBase\(topBaseKey\(state\.baseItemId\) \?\? ''\)\?\.family \?\? '',\s*\)\)/,'(basePolicy(state.baseItemId)?.maximumQualityBreach !== true)')
})
edit(root+'workbenchStateShape.ts',s=>{
 s="import { basePolicy } from './baseRegistry'\n"+s
 return s.replace("item.baseItemId !== 'Metadata/Items/Armours/Gloves/FourGlovesStr1' ||\n      ![0, 1].includes(item.augmentSockets as number)","typeof item.baseItemId !== 'string' || basePolicy(item.baseItemId)?.socketExecutionMaximum == null || !Number.isInteger(item.augmentSockets) || Number(item.augmentSockets) < 0 || Number(item.augmentSockets) > basePolicy(item.baseItemId)!.socketExecutionMaximum!")
})
// Preserve all previously displayed weapon property disclaimers.
const policy='backend/src/main/resources/catalog/base-policies.json'
const data=JSON.parse(fs.readFileSync(policy,'utf8'))
for(const family of ['bows','shields','bucklers','foci'])data.families[family].sourcePropertyUnscaled=true
for(const p of [policy,root+'basePolicies.json'])fs.writeFileSync(p,JSON.stringify(data,null,2)+'\n')
