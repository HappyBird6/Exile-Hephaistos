import fs from 'node:fs'
import assert from 'node:assert/strict'
const root='E:/WORK/Exile-Hephaistos/codex/amulets-qa-20261004'
let s=fs.readFileSync(`${root}/qa-browser-2.cjs`,'utf8')
s=s.replace("    check(k+' browser crossclass Sorcery rejects atomically',!wrong.applied && await raw()===wrongBefore)","    check(k+' browser crossclass Sorcery rejects atomically',!wrong.applied && await raw()===wrongBefore)\n    await page.screenshot({path:'/evidence/'+k+'-crossclass-rejection.png',fullPage:true})")
s=s.replace("    const d=Object.values(initials[k].modifiers).find",`    const matched=({stellar:'Adaptive_Catalyst',amber:'Adaptive_Catalyst',bloodstone:'Flesh_Catalyst',lunar:'Carapace_Catalyst',azure:'Neural_Catalyst',crimson:'Flesh_Catalyst',pearlescent:'Eshs_Catalyst'})[k]
    await use(matched)
    const repeatedMatch=await use(matched)
    check(k+' actual matched Catalyst repeat',repeatedMatch.applied && repeatedMatch.state.catalystQuality.amount===20)
    for(const l of locales){
      await locale(l)
      check(k+':'+l+' actual repeat leaves canonical implicit',JSON.stringify(active(await history()).frames.at(-1).state.implicits)===JSON.stringify(concrete(initials[k]).implicits))
      await page.screenshot({path:'/evidence/'+k+'-'+l+'-catalyst-repeat.png',fullPage:true})
    }
    await locale('en')
    const d=Object.values(initials[k].modifiers).find`)
s=s.replace("    await page.screenshot({path:'/evidence/'+k+'-quality-overflow.png',fullPage:true})",`    for(const l of locales){
      await locale(l)
      check(k+':'+l+' overflow preserves quality40',active(await history()).frames.at(-1).state.catalystQuality.amount===40)
      await page.screenshot({path:'/evidence/'+k+'-'+l+'-quality-overflow.png',fullPage:true})
    }
    await locale('en')`)
assert(!fs.existsSync(`${root}/qa-browser-3.cjs`),'Preserve prior probe')
fs.writeFileSync(`${root}/qa-browser-3.cjs`,s)
let c=fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/rings-qa-20261004/contact-sheets.cjs','utf8')
c=c.replace('["kinetic","vitalic","mnemonic","pearl","amethyst","prismatic","ruby-ring","two-stone-fire-cold"]',JSON.stringify(['stellar','amber','bloodstone','lunar','azure','crimson','pearlescent']))
c=c.replace("f.endsWith('-quality-overflow.png')","f.endsWith('-quality-overflow.png')||f.endsWith('-catalyst-repeat.png')||f.endsWith('-crossclass-rejection.png')")
assert(!fs.existsSync(`${root}/contact-sheets.cjs`),'Preserve previous contact script');fs.writeFileSync(`${root}/contact-sheets.cjs`,c)
