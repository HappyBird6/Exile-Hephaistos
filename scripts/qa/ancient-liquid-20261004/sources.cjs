const fs = require('fs'), assert = require('assert/strict');
const {parse} = require('../basic-jewel-potent-20261004/sources.cjs');
const root = '../ancient-liquid-20261004/';
module.exports = {parse, root};
if (require.main === module) (async () => {
  fs.mkdirSync(root, {recursive:true});
  for(const name of ['Time-Lost_Ruby','Time-Lost_Emerald','Time-Lost_Sapphire','Time-Lost_Diamond','Liquid_Emotions']) {
    for(const [locale,prefix] of Object.entries({en:'us',ko:'kr','zh-CN':'cn','zh-TW':'tw',ja:'jp',es:'sp'})) {
      const file=root+name.toLowerCase()+'-'+locale+'.html';
      if(!fs.existsSync(file)) { const r=await fetch('https://poe2db.tw/'+prefix+'/'+name); assert(r.ok, name+' '+r.status); fs.writeFileSync(file,await r.text()); }
      if(locale==='en' && name!=='Liquid_Emotions') {const data=parse(fs.readFileSync(file,'utf8'));fs.writeFileSync(root+name.toLowerCase()+'-mods.json',JSON.stringify(data,null,2));console.log(name,data.baseitem,data.normal.length,data.liquid);}
    }
  }
})().catch(e=>{console.error(e);process.exit(1)});
