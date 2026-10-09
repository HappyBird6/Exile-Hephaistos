const fs=require('fs'),{chromium}=require('/qa/node_modules/playwright');
(async()=>{
 const browser=await chromium.launch({headless:true}),page=await browser.newPage({viewport:{width:1260,height:1000}});
 for(const size of ['1920x1080','1440x900','1440x1100','1440x1400','1024x768','390x844']){
  const cells=['en','ko','zh-CN','zh-TW','ja','es'].map(loc=>'<section><h2>'+loc+' / '+size+'</h2><img src="data:image/png;base64,'+fs.readFileSync('/evidence/after-'+loc+'-'+size+'.png').toString('base64')+'"></section>').join('');
  await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,420px)}section{padding:8px}img{max-width:400px}h2{font-size:18px}</style>'+cells);await page.screenshot({path:'/evidence/contact-'+size+'.png',fullPage:true});
 }
 await page.setViewportSize({width:720,height:900});
 const menus=['en','ko','zh-CN','zh-TW','ja','es'].map(loc=>'<section><h2>'+loc+'</h2><div><img src="data:image/png;base64,'+fs.readFileSync('/evidence/popover-'+loc+'-1440.png').toString('base64')+'"></div></section>').join('');
 await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,240px)}section{padding:8px}section div{position:relative;width:224px;height:370px;overflow:hidden}img{position:absolute;right:-48px;top:0;width:1440px}h2{font-size:18px}</style>'+menus);await page.screenshot({path:'/evidence/contact-menus.png',fullPage:true});
 await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
