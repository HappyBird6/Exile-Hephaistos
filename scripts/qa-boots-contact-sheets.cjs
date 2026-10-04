const fs=require('node:fs'),{chromium}=require('/qa/node_modules/playwright')
;(async()=>{
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:1260,height:1000}})
 const keys=['tasalian','drakeskin','sekhema','blacksteel-boots','faithful','daggerfoot'],locales=['en','ko','ja','zh-CN','zh-TW','es']
 for(const key of keys)for(const width of [1440,390]) {
  const cells=locales.map(l=>`<section><h2>${key} / ${l} / ${width}</h2><img src="data:image/png;base64,${fs.readFileSync(`/evidence/cards/${key}-${l}-${width}.png`).toString('base64')}"></section>`).join('')
  await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,420px)}section{padding:8px}img{max-width:400px}h2{font-size:16px}</style>'+cells)
  await page.screenshot({path:`/evidence/contact-card-${key}-${width}.png`,fullPage:true})
 }
 for(const key of keys) {
  const cells=locales.flatMap(l=>[1440,390].map(width=>`<section><h2>${key} / ${l} / ${width}</h2><img src="data:image/png;base64,${fs.readFileSync(`/evidence/${key}-${l}-${width}.png`).toString('base64')}"></section>`)).join('')
  await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(4,315px)}section{padding:8px}img{max-width:299px}h2{font-size:14px}</style>'+cells)
  await page.screenshot({path:`/evidence/contact-full-${key}.png`,fullPage:true})
 }
 const remaining=fs.readdirSync('/evidence').filter(f=>f.endsWith('.png')&&(f.startsWith('old-film-')||f.endsWith('-orange-preview.png')))
 for(let start=0;start<remaining.length;start+=6) {
  const cells=remaining.slice(start,start+6).map(f=>`<section><h2>${f}</h2><img src="data:image/png;base64,${fs.readFileSync('/evidence/'+f).toString('base64')}"></section>`).join('')
  await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,420px)}section{padding:8px}img{max-width:404px}h2{font-size:14px}</style>'+cells)
  await page.screenshot({path:`/evidence/contact-remaining-${start}.png`,fullPage:true})
 }
 await browser.close()
})().catch(e=>{console.error(e);process.exit(1)})
