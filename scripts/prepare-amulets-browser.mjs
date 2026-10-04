import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
let s=fs.readFileSync(`${root}/qa-browser.cjs`,'utf8')
s=s.replace("rarity: 'RARE', explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }", "rarity: 'RARE', catalystQuality:{type:({stellar:'ADAPTIVE',amber:'ADAPTIVE',bloodstone:'FLESH',lunar:'CARAPACE',azure:'NEURAL',crimson:'FLESH',pearlescent:'ESH'})[k],amount:20}, explicits: [{ modifierId: d.id, values: Object.fromEntries(d.stats.map(s => [s.id, s.min])), fractured: true }")
s=s.replace("      const saved = await raw()",`      const implicit = bases[k].implicitStats[0]
      const projected = Math.round(implicit.max*1.2)/(k==='crimson'?60:1)
      const formatted = new Intl.NumberFormat(l,{maximumFractionDigits:20}).format(projected)
      check(k+':'+l+' exact quality implicit unit', (await page.locator('.bench-item-card .item-card__line--implicit').innerText()).includes(formatted))
      check(k+':'+l+' compound original source ranges retained', Object.keys(state.explicits[0].values).length>1)
      const saved = await raw()`)
s=s.replace("    await seed(rare, 'abyss-' + k)",`    await seed(rare,'wrongclass-'+k)
    const wrongBefore=await raw(),wrong=await use('Perfect_Essence_of_Sorcery')
    check(k+' browser crossclass Sorcery rejects atomically',!wrong.applied && await raw()===wrongBefore)
    await seed(rare, 'abyss-' + k)`)
s=s.replace("      check(k+' browser accepts Catalyst '+id, q.applied && q.state.catalystQuality.amount===20)",`      check(k+' browser accepts Catalyst '+id, q.applied && q.state.catalystQuality.amount===20)
      const repeated=await use(id)
      check(k+' Catalyst repeat cap20 '+id,repeated.applied && repeated.state.catalystQuality.amount===20)`)
assert(!fs.existsSync(`${root}/qa-browser-2.cjs`),'Preserve prior probes')
fs.writeFileSync(`${root}/qa-browser-2.cjs`,s)
