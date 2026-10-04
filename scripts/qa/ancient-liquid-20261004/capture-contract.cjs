const fs=require('fs');
const root='../ancient-liquid-20261004/';
const api=JSON.parse(fs.readFileSync(root+'api-results.json'));
const seen=new Set(),captures=[];
for(const c of api.captures) {
 const k=JSON.stringify([c.base,c.action,c.result.applied,c.before.rarity,c.before.explicits.length,c.result.events.map(e=>[e.kind,e.modifierId]),c.result.assumptions.filter(a=>['uniform-liquid-outcomes-v1','jewel-cap-loss-preserve-v1'].includes(a.id)).map(a=>[a.id,a.n])]);
 if(seen.has(k))continue;seen.add(k);captures.push(c);
}
fs.writeFileSync(root+'frontend-check/src/shared/test/ancient-liquid-responses.json',JSON.stringify({initials:api.initials,captures},null,2)+'\n');
fs.writeFileSync(root+'frontend-check/src/features/crafting/AncientLiquidContract.test.ts',`import { afterEach, expect, it, vi } from 'vitest'
import actual from '../../shared/test/ancient-liquid-responses.json'
import { jsonResponse } from '../../shared/test/craftingFixtures'
import { applyCurrency } from './workbenchApi'
import { verifiedHistoryState, verifiedFrameEvidence } from './workbenchHistory'
import type { ConcreteItem, WorkbenchAction, AppliedItem } from './workbenchApi'
import type { Definition, Initial } from './craftingApi'

afterEach(() => vi.unstubAllGlobals())
it.each(actual.captures.map((capture,index)=>({...capture,index})))('accepts actual $base response $index ($action)',async({base,before,action,activeOmens,result})=>{
  const initial=(actual.initials as unknown as Record<string,Initial>)[base]!
  vi.stubGlobal('fetch',vi.fn().mockResolvedValue(jsonResponse(result)))
  await expect(applyCurrency(before as ConcreteItem,action as WorkbenchAction,initial.modifiers as Record<string,Definition>,new AbortController().signal,activeOmens)).resolves.toEqual(result)
  expect(verifiedFrameEvidence({state:result.state as ConcreteItem,action,evidence:result as unknown as AppliedItem},initial)).toEqual(result)
  expect(verifiedHistoryState(result.state as ConcreteItem,initial)).toBe(true)
})
`);
fs.writeFileSync(root+'frontend-final.sh','#!/bin/sh\nset -eu\ncd /evidence/frontend-check\nnpx prettier --write src/features/crafting/AncientLiquidContract.test.ts src/shared/test/ancient-liquid-responses.json > /evidence/frontend-contract-format.log 2>&1\nnpm run lint > /evidence/frontend-final.log 2>&1\nnpm run typecheck >> /evidence/frontend-final.log 2>&1\nnpm run format:check >> /evidence/frontend-final.log 2>&1\nnpm run test -- --run >> /evidence/frontend-final.log 2>&1\nnpm run build >> /evidence/frontend-final.log 2>&1\n');
console.log('Captured contract cases:',captures.length);
