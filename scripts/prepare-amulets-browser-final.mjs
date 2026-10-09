import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
let s=fs.readFileSync(`${root}/qa-browser-3.cjs`,'utf8')
s=s.replace('d.stats.length > 1 && d.stats.some(s=>s.min!==s.max)',"d.familyIds.includes('IncreasedLife') && d.stats.some(s=>s.min!==s.max)")
s=s.replace("check(k+':'+l+' compound original source ranges retained', Object.keys(state.explicits[0].values).length>1)","check(k+':'+l+' original source roll retained', state.explicits[0].values[d.stats[0].id]===d.stats[0].min)")
s=s.replace("  check('zero browser errors'",`  const compoundSource=initials['kinetic']
  const compoundDef=Object.values(compoundSource.modifiers).find(d=>d.weight>0 && d.requiredItemLevel<=82 && d.stats.length>1 && d.stats.some(s=>s.min!==s.max))
  check('source-backed compound definition exists',Boolean(compoundDef))
  const compoundState={...concrete(compoundSource),rarity:'RARE',explicits:[{modifierId:compoundDef.id,values:Object.fromEntries(compoundDef.stats.map(s=>[s.id,s.min])),fractured:false}]}
  await seed(compoundState,'source-compound-ring')
  for(const l of locales){
    await locale(l)
    const before=await raw(),normal=await page.locator('.bench-item-card').innerText()
    await page.keyboard.down('Alt')
    await page.waitForFunction(t=>document.querySelector('.bench-item-card').innerText!==t,normal)
    check('compound '+l+' source ranges',await page.locator('.bench-item-card').innerText()!==normal)
    await page.screenshot({path:'/evidence/compound-'+l+'.png',fullPage:true})
    await page.keyboard.up('Alt')
    check('compound '+l+' original rolls and films preserved',await raw()===before && await page.locator('.bench-item-card').innerText()===normal)
  }
  check('zero browser errors'`)
assert(!fs.existsSync(`${root}/qa-browser-4.cjs`))
fs.writeFileSync(`${root}/qa-browser-4.cjs`,s)
const out=`${root}/browser-attempt-3`
fs.mkdirSync(out)
for(const f of ['api-initials.json','baseline-api-initials.json'])fs.copyFileSync(`${root}/${f}`,`${out}/${f}`)
