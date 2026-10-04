const fs=require('fs'),crypto=require('crypto'),assert=require('assert/strict');
const {parse,root}=require('./sources.cjs');
const clean=s=>s.replace(/<br\s*\/?>/gi,'\n').replace(/<[^>]+>/g,'').replace(/&amp;/g,'&').replace(/&nbsp;/g,' ').trim();
const digest=s=>crypto.createHash('sha256').update(s).digest('hex');
const slug=s=>s.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
function spans(s){const out=[],rx=/<span\b[^>]*>|<\/span>/g;let start,depth=0;for(const m of s.matchAll(rx)){if(start!==undefined){if(m[0]==='</span>'){if(--depth===0){out.push({start,end:m.index+7,value:clean(s.slice(start,m.index+7))});start=undefined}}else depth++}else if(/class=['"]mod-value['"]/.test(m[0])){start=m.index;depth=1}}return out;}
function view(r){const values=spans(r.str);let s=r.str;for(const [i,m]of [...values.entries()].reverse())s=s.slice(0,m.start)+'{v'+i+'}'+s.slice(m.end);return {name:clean(r.Name),template:clean(s),values:values.map(m=>m.value)};}
const identity=r=>JSON.stringify([r.ModGenerationTypeID,[...r.ModFamilyList].sort(),r.Level,[...r.fossil_no].sort(),[...r.spawn_no].sort(),spans(r.str).map(m=>m.value)]);
const file='frontend/src/shared/i18n/modifierTemplates.json',bindings=JSON.parse(fs.readFileSync(file));
const definitions=JSON.parse(fs.readFileSync('frontend/src/features/crafting/basicJewelDefinitions.json'));
const targets=JSON.parse(fs.readFileSync('frontend/src/features/crafting/basicJewelLiquidTargets.json'));
const original=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/sapphire/catalog.json'));
const counts={ruby:53,emerald:77,sapphire:60,diamond:160};
const esSpecial={
 CraftedJewelRadiusExtraLargeSize:'Mejora el radio a muy grande\nlocal jewel effect base radius [500]',
 CraftedJewelAdditionalSuffixAllowed:'Permite {v0} modificador de sufijo',
 CraftedJewelAdditionalPrefixAllowed:'Permite {v0} modificador de prefijo',
 JewelRadiusIncLightningColdToFire:'Los aumentos y las reducciones del daño de frío y de rayo dentro del radio se transforman para aplicarse al daño de fuego',
 JewelRadiusIncColdFreToLightning:'Los aumentos y las reducciones del daño de frío y de fuego dentro del radio se transforman para aplicarse al daño de rayo',
 JewelRadiusIncLightningFireToCold:'Los aumentos y las reducciones del daño de fuego y de rayo dentro del radio se transforman para aplicarse al daño de frío',
};
for(const [color,count]of Object.entries(counts)){
 const base='time-lost-'+color,name='Time-Lost_'+color[0].toUpperCase()+color.slice(1),data=parse(fs.readFileSync(root+name.toLowerCase()+'-en.html','utf8'));
 assert.equal(data.normal.length,count);assert.equal(data.liquid.length,color==='diamond'?4:14);
 const rows=[...data.normal,...data.liquid],modifiers=rows.map((r,i)=>{
  const crafted=i>=count,side=r.ModGenerationTypeID==='1'?'PREFIX':'SUFFIX';assert(['1','2'].includes(r.ModGenerationTypeID));
  if(!crafted)assert(r.spawn_no.some(t=>t==='radius_jewel'||t==={ruby:'str',emerald:'dex',sapphire:'int'}[color]+'_radius_jewel'||color==='diamond'&&t.endsWith('_radius_jewel')));
  const duplicate=!crafted&&data.normal.filter(n=>n.Name===r.Name&&n.ModGenerationTypeID===r.ModGenerationTypeID).length>1;
  const id=base+':'+(crafted?'crafted:'+r.Code:side.toLowerCase()+':'+slug(r.Name)+(duplicate?'-'+digest(identity(r)).slice(0,8):''));
  const nums=spans(r.str).map(m=>m.value);let stats=nums.map((v,j)=>{const range=v.match(/^[+]?\((-?\d+)[—–-](-?\d+)\)$/),fixed=v.match(/^[+]?(-?\d+)$/);assert(range||fixed,id+' '+v);return {id:nums.length===1?'display_source_value':'display_source_value_'+j,min:Number(range?range[1]:fixed[1]),max:Number(range?range[2]:fixed[1])}});
  const radius=r.str.match(/local jewel effect base radius \[(\d+)\]/);if(radius)stats=[{id:'local_jewel_effect_base_radius',min:Number(radius[1]),max:Number(radius[1])}];
  if(!stats.length)stats=[{id:'display_condition_present',min:1,max:1}];
  return {id,name:clean(r.Name)+(crafted?' (Crafted)':''),layer:'EXPLICIT',affixType:side,familyIds:r.ModFamilyList,requiredItemLevel:Number(r.Level),weight:crafted?0:1,tier:1,text:clean(r.str),stats,tags:[...r.fossil_no,...(crafted?['crafted']:[]),...(radius||!nums.length||r.Code?.startsWith('CraftedJewelAdditional')?['unscalable']:[]),'passive-radius-condition'],sourceUrl:crafted?'https://poe2db.tw/us/'+r.Name.match(/href="([^"]+)"/)[1]:r.hover};
 });
 assert.equal(new Set(modifiers.map(d=>d.id)).size,modifiers.length);
 const html=fs.readFileSync(root+name.toLowerCase()+'-en.html','utf8');
 assert(html.includes('<div class="implicitMod"><span class="secondary">local jewel effect base radius [1000]</span></div>'));
 const implicit={id:base+':implicit:base-radius',name:'Base radius',layer:'IMPLICIT',affixType:'NONE',familyIds:['JewelEffectBaseRadius'],requiredItemLevel:1,weight:0,tier:0,text:'local jewel effect base radius [1000]',stats:[{id:'local_jewel_effect_base_radius',min:1000,max:1000}],tags:['unscalable','passive-radius-condition'],sourceUrl:'https://poe2db.tw/us/'+name};
 const raw=JSON.stringify({sourceUrl:'https://poe2db.tw/us/'+name,baseitem:data.baseitem,baseTags:html.match(/<tr><td>Tags<\/td><td>([^<]+)<\/td><\/tr>/)[1].split(', '),implicitHtml:html.match(/<div class="implicitMod">.*?<\/div>/)[0],normal:data.normal,liquid:data.liquid},null,2)+'\n';
 const details=JSON.stringify({sourceUrl:'https://poe2db.tw/us/'+name,eligibleSection:'normal',excludedSections:['corrupted','desecrated'],weightPolicy:'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS',slots:{magic:[1,1],rare:[2,2]},statPolicy:'Published display-unit bounds. Radius [150/300/500] is a fixed source value, not scaled. Presence marker 1 is not a combat stat. Small/Notable scope is conditional text; passive tree is not simulated.',craftedPolicy:'One Crafted occupying ordinary slot. Uniform legal removals and valid sourced outcomes. Ancient Ferocity grants Notable resistance, Ancient Melancholy sets Very Large radius, Contempt expands opposite affix cap. Existing overflow preserved after cap loss.',catalystPolicy:'No jewel_catalyst tag in Time-Lost base source; Refined and ordinary catalysts excluded.',normalPolicy:'User-approved Normal simulator starting state; game drop rarity not inferred.'},null,2)+'\n';
 const p=modifiers.filter(d=>d.affixType==='PREFIX'),s=modifiers.filter(d=>d.affixType==='SUFFIX');
 const catalog={metadata:{...original.metadata,snapshotId:'poe2db-'+base+'-ancient-20261004-'+digest(raw).slice(0,12),retrievedAt:new Date().toISOString(),sourceUrl:'https://poe2db.tw/us/'+name,rawSha256:digest(raw),detailsSha256:digest(details),prefixCount:p.length,suffixCount:s.length,prefixWeight:p.filter(d=>d.weight>0).length,suffixWeight:s.filter(d=>d.weight>0).length},base:{...original.base,id:data.baseitem.ItemType,name:name.replaceAll('_',' '),sourceUrl:'https://poe2db.tw/us/'+name,implicitModifierId:implicit.id},modifiers:[implicit,...modifiers]};
 const dir='backend/src/main/resources/catalog/'+base+'/';fs.mkdirSync(dir,{recursive:true});for(const [f,s]of Object.entries({'base.raw.json':raw,'details.raw.json':details,'catalog.json':JSON.stringify(catalog,null,2)+'\n'}))fs.writeFileSync(dir+f,s);
 fs.writeFileSync('frontend/src/shared/test/'+base+'-catalog.json',JSON.stringify(catalog,null,2)+'\n');
 targets[base]={};for(const d of modifiers){definitions[d.id]={baseItemId:catalog.base.id,affixType:d.affixType,familyIds:d.familyIds,stats:d.stats,crafted:d.tags.includes('crafted')};if(d.tags.includes('crafted'))(targets[base][d.sourceUrl.split('/').pop().toUpperCase()]??=[]).push(d.id)}
 for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']){
  const implicitKey='time-lost-jewel-'+digest(implicit.id).slice(0,16);
  bindings.definitions[implicit.id]={stats:implicit.stats,englishText:implicit.text,values:[],template:implicitKey,sourceCode:null};
  bindings.templates[locale][implicitKey]={name:implicit.name,template:implicit.text};
  const local=parse(fs.readFileSync(root+name.toLowerCase()+'-'+locale+'.html','utf8'));
  for(const [i,d]of modifiers.entries()){
   const en=rows[i],crafted=i>=count;let matches=(crafted?(local.liquid??[]):local.normal).filter(r=>crafted?r.Code===en.Code:identity(r)===identity(en));let translated;
   if(crafted&&matches.length===0)matches=local.normal.filter(r=>identity(r)===identity(en));
   if(matches.length===0&&locale==='es'&&crafted){
    translated=esSpecial[en.Code];
    if(!translated&&en.Code.startsWith('CraftedJewelRadius')&&en.Code.endsWith('Resistance'))translated='Las habilidades pasivas notables dentro del radio también otorgan {v0}% de resistencia '+({ruby:'al fuego',emerald:'al rayo',sapphire:'al frío',diamond:'al caos'}[color]);
    if(!translated&&en.Code==='JewelRadiusLifeonKill')translated='Las habilidades pasivas notables dentro del radio también otorgan: Recuperas un {v0}% de la vida máxima al matar';
    assert(translated,'Missing documented Spanish translation '+en.Code);matches=[en];
   }
   if(!crafted&&matches.length>1){const peers=data.normal.filter(r=>identity(r)===identity(en));assert.equal(matches.length,peers.length);matches=[matches[peers.indexOf(en)]]}
   assert.equal(matches.length,1,base+' '+locale+' '+d.id);
   const baseline=view(en),v=view(matches[0]),key='time-lost-jewel-'+digest(d.id).slice(0,16);
   bindings.definitions[d.id]={stats:d.stats,englishText:d.text,values:baseline.values,template:key,sourceCode:en.Code??null};bindings.templates[locale][key]={name:v.name,template:translated??v.template};
  }
 }
 console.log(base,count,data.liquid.length,catalog.metadata.snapshotId);
}
fs.writeFileSync(file,JSON.stringify(bindings,null,2)+'\n');fs.writeFileSync('frontend/src/features/crafting/basicJewelDefinitions.json',JSON.stringify(definitions,null,2)+'\n');fs.writeFileSync('frontend/src/features/crafting/basicJewelLiquidTargets.json',JSON.stringify(targets,null,2)+'\n');
