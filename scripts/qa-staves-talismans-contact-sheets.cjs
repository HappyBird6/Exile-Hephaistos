const fs=require('node:fs'),assert=require('node:assert/strict'),{chromium}=require('/qa/node_modules/playwright')
const bases=['permafrost-staff','reflecting-staff','dark-staff','ravenous-staff','perching-staff','sanctified-staff','maji-talisman','fungal-talisman','jade-talisman']
;(async()=>{
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:1260,height:1000}}),sheets=[]
 async function sheet(name,files) {
  const path='/evidence/'+name;assert(!fs.existsSync(path))
  await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,420px)}section{padding:8px}img{max-width:404px}h2{font-size:13px}</style>'+files.map(file=>`<section><h2>${file}</h2><img src="data:image/png;base64,${fs.readFileSync('/evidence/'+file).toString('base64')}"></section>`).join(''))
  await page.screenshot({path,fullPage:true});sheets.push({file:name,originals:files})
 }
 for(const locale of ['en','ko','ja','zh-CN','zh-TW','es'])for(const width of [1440,390])for(const kind of ['cards','full']) {
  const files=bases.map(base=>(kind==='cards'?'cards/':'')+`${base}-${locale}-${width}.png`)
  await sheet(`review-${kind}-${locale}-${width}.png`,files)
 }
 for(const prefix of ['maji-talisman-orange','solar-overflow'])await sheet(`review-${prefix}.png`,['en','ko','ja','zh-CN','zh-TW','es'].map(l=>`${prefix}-${l}.png`))
 const legacy=fs.readdirSync('/evidence/cards').filter(f=>f.startsWith('legacy-')&&f.endsWith('-en.png'))
 for(let i=0;i<legacy.length;i+=6)await sheet(`review-legacy-en-${i}.png`,legacy.slice(i,i+6).map(f=>'cards/'+f))
 fs.writeFileSync('/evidence/review-contact-sheets.json',JSON.stringify({sheets,selectedOriginals:228,legacyEnglishOriginals:legacy.length},null,2)+'\n',{flag:'wx'})
 await browser.close();console.log(sheets.length,'contact sheets generated without overwriting evidence')
})().catch(e=>{console.error(e);process.exitCode=1})
