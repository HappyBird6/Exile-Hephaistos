const fs=require('fs'),crypto=require('crypto'),assert=require('assert/strict');
const evidence='E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004/';
const clean=s=>s.replace(/<[^>]+>/g,'').replace(/&amp;/g,'&').replace(/&nbsp;/g,' ').trim();
function parse(s){const start=s.indexOf('new ModsView(')+13;let depth=0,q=false,e=false;for(let i=start;i<s.length;i++){const c=s[i];if(q){if(e)e=false;else if(c==='\\')e=true;else if(c==='"')q=false}else if(c==='"')q=true;else if(c==='{')depth++;else if(c==='}'&&--depth===0)return JSON.parse(s.slice(start,i+1))}throw Error('Missing source JSON')}
function spans(s){const matches=[],rx=/<span\b[^>]*>|<\/span>/g;let start,depth=0;for(const m of s.matchAll(rx)){if(start!==undefined){if(m[0]==='</span>'){if(--depth===0){matches.push({start,end:m.index+7,value:clean(s.slice(start,m.index+7))});start=undefined}}else depth++}else if(/class=['"]mod-value['"]/.test(m[0])){start=m.index;depth=1}}return matches}
const identity=r=>JSON.stringify([r.ModGenerationTypeID,[...r.ModFamilyList].sort(),r.Level,[...r.fossil_no].sort(),[...r.spawn_no].sort(),spans(r.str).map(m=>m.value)]);
function display(r){const nums=spans(r.str);let html=r.str;for(const [i,m]of [...nums.entries()].reverse())html=html.slice(0,m.start)+'{v'+i+'}'+html.slice(m.end);return {name:clean(r.Name),template:clean(html),values:nums.map(m=>m.value)}}
(async()=>{
const file='frontend/src/shared/i18n/modifierTemplates.json',bindings=JSON.parse(fs.readFileSync(file));
const catalog=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/sapphire/catalog.json'));
const english=parse(fs.readFileSync(evidence+'sapphire.html','utf8'));
for(const [locale,prefix]of Object.entries({en:'us',ko:'kr','zh-CN':'cn','zh-TW':'tw',ja:'jp',es:'sp'})){
 let html;if(locale==='en')html=fs.readFileSync(evidence+'sapphire.html','utf8');else if(fs.existsSync(evidence+'sapphire-'+locale+'.html'))html=fs.readFileSync(evidence+'sapphire-'+locale+'.html','utf8');else{let response=await fetch('https://poe2db.tw/'+prefix+'/Sapphire');assert(response.ok);html=await response.text();fs.writeFileSync(evidence+'sapphire-'+locale+'.html',html)}
 const source=parse(html);
 for(const definition of catalog.modifiers){
  const crafted=definition.tags.includes('crafted'),englishRows=crafted?english.liquid:english.normal;
  const en=englishRows.find(r=>crafted?'sapphire:crafted:'+r.Code===definition.id:r.hover===definition.sourceUrl);
  assert(en,definition.id); const localizedRows=crafted?(source.liquid??source.normal):source.normal;
  assert(localizedRows,locale+' missing normal/liquid source; keys '+Object.keys(source));
  const candidates=localizedRows.filter(r=>crafted&&source.liquid?r.Code===en.Code:identity(r)===identity(en));
  assert.equal(candidates.length,1,locale+' '+definition.id);
  const view=display(candidates[0]),baseline=display(en),key='sapphire-source-'+crypto.createHash('sha256').update(definition.id).digest('hex').slice(0,16);
  bindings.definitions[definition.id]={stats:definition.stats,englishText:definition.text,values:baseline.values,template:key,sourceCode:en.Code??null};
  bindings.templates[locale][key]={name:view.name,template:view.template};
 }
}
fs.writeFileSync(file,JSON.stringify(bindings,null,2)+'\n');
const mf='frontend/src/shared/i18n/messages.json',messages=JSON.parse(fs.readFileSync(mf));
const scopes={en:'Sapphire: 58 ordinary candidates, Magic 1P/1S and Rare 2P/2S. Basic currencies, refined catalysts and 10 Basic Liquids are supported. Candidate odds use an explicit uniform simulator model, not verified game weights. One Crafted modifier; remove it before another Liquid. Potent and Ancient remain unsupported.',ko:'Sapphire: 일반 후보 58개, Magic 1P/1S·Rare 2P/2S. 기본 화폐·refined catalyst·Basic Liquid 10종을 지원합니다. 후보 확률은 실제 게임 weight가 아닌 명시적 균등 simulator 모델입니다. Crafted는 하나만 허용하며 다음 Liquid 전에 제거해야 합니다. Potent·Ancient는 미지원입니다.','zh-CN':'Sapphire：58种普通词缀，Magic 1P/1S、Rare 2P/2S。支持基础通货、精炼催化剂及10种Basic Liquid。候选概率采用明确的均匀模拟模型，并非已验证游戏权重。只能有一个Crafted词缀，使用另一Liquid前需移除。Potent和Ancient尚未支持。','zh-TW':'Sapphire：58種一般詞綴，Magic 1P/1S、Rare 2P/2S。支援基本通貨、精煉催化劑及10種Basic Liquid。候選機率採明確的均勻模擬模型，並非已驗證遊戲權重。只能有一個Crafted詞綴，使用另一Liquid前需移除。Potent和Ancient尚未支援。',ja:'Sapphire：通常候補58種、Magic 1P/1S・Rare 2P/2S。基本通貨、精製Catalyst、Basic Liquid 10種に対応。候補確率は明示された均等シミュレータモデルで、確認済みゲームweightではありません。Craftedは1個のみで、次のLiquid使用前に除去してください。Potent・Ancientは未対応です。',es:'Sapphire: 58 candidatos ordinarios, Magic 1P/1S y Rare 2P/2S. Admite monedas básicas, catalizadores refinados y 10 Basic Liquids. Las probabilidades usan un modelo uniforme explícito del simulador, no pesos del juego verificados. Solo un modificador Crafted; elimínalo antes de otro Liquid. Potent y Ancient siguen sin soporte.'};
for(const [locale,text]of Object.entries(scopes))messages[locale]['notice.sapphire_scope']=text;
fs.writeFileSync(mf,JSON.stringify(messages,null,2)+'\n');console.log('Six locales: 68 sourced Sapphire bindings each');
})().catch(e=>{console.error(e);process.exit(1)});
