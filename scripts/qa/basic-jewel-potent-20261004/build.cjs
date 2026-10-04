const fs = require('fs'), crypto = require('crypto'), assert = require('assert/strict');
const {parse, root} = require('./sources.cjs');
const clean = s => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&nbsp;/g, ' ').trim();
const slug = s => s.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'');
const digest = s => crypto.createHash('sha256').update(s).digest('hex');
function spans(s) {const out=[],rx=/<span\b[^>]*>|<\/span>/g;let start,depth=0;for(const m of s.matchAll(rx)){if(start!==undefined){if(m[0]==='</span>'){if(--depth===0){out.push({start,end:m.index+7,value:clean(s.slice(start,m.index+7))});start=undefined}}else depth++}else if(/class=['"]mod-value['"]/.test(m[0])){start=m.index;depth=1}}return out;}
function view(r) {const values=spans(r.str);let s=r.str;for(const [i,m] of [...values.entries()].reverse())s=s.slice(0,m.start)+'{v'+i+'}'+s.slice(m.end);return {name:clean(r.Name),template:clean(s),values:values.map(m=>m.value)};}
const identity = r => JSON.stringify([r.ModGenerationTypeID,[...r.ModFamilyList].sort(),r.Level,[...r.fossil_no].sort(),[...r.spawn_no].sort(),spans(r.str).map(m=>m.value)]);
const templatesFile='frontend/src/shared/i18n/modifierTemplates.json', bindings=JSON.parse(fs.readFileSync(templatesFile));
const original=JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/sapphire/catalog.json'));
const baseTags={ruby:['strjewel'],emerald:['dexjewel'],sapphire:['intjewel'],diamond:['strjewel','dexjewel','intjewel']};
const counts={ruby:50,emerald:74,diamond:160,sapphire:58}, allDefinitions={};
for(const base of Object.keys(counts)) {
  const name=base[0].toUpperCase()+base.slice(1), data=parse(fs.readFileSync(root+base+'-en.html','utf8'));
  assert.equal(data.normal.length,counts[base]);
  const rows=[...data.normal,...data.liquid], modifiers=rows.map((r,i)=>{
    const crafted=i>=data.normal.length, side=r.ModGenerationTypeID==='1'?'PREFIX':'SUFFIX';assert(['1','2'].includes(r.ModGenerationTypeID));
    if(!crafted)assert(r.spawn_no.some(t=>baseTags[base].includes(t)));
    const id=base+':'+(crafted?'crafted:'+r.Code:side.toLowerCase()+':'+slug(r.Name));
    const prior=base==='sapphire'?original.modifiers.find(d=>d.id===id):null;
    const nums=spans(r.str).map(m=>m.value);let stats=nums.map((v,j)=>{const range=v.match(/^[+]?\((-?\d+)[—–-](-?\d+)\)$/),fixed=v.match(/^[+]?(-?\d+)$/);assert(range||fixed,id+' '+v);return {id:nums.length===1?'display_source_value':'display_source_value_'+j,min:Number(range?range[1]:fixed[1]),max:Number(range?range[2]:fixed[1])};});
    const conditional=stats.length===0;if(conditional)stats=[{id:'display_condition_present',min:1,max:1}];
    const meta=crafted&&r.Code.startsWith('CraftedJewel');
    return {id,name:crafted?clean(r.Name)+' (Crafted)':r.Name,layer:'EXPLICIT',affixType:side,familyIds:r.ModFamilyList,requiredItemLevel:Number(r.Level),weight:crafted?0:1,tier:1,text:clean(r.str),stats:prior?prior.stats:stats,tags:[...r.fossil_no,...(crafted?['crafted']:[]),...(conditional||meta?['unscalable']:[])],sourceUrl:crafted?'https://poe2db.tw/us/'+r.Name.match(/href="([^"]+)"/)[1]:r.hover};
  });
  assert.equal(new Set(modifiers.map(d=>d.id)).size,modifiers.length);
  const raw=JSON.stringify({sourceUrl:'https://poe2db.tw/us/'+name,baseitem:data.baseitem,normal:data.normal,liquid:data.liquid},null,2)+'\n';
  const details=JSON.stringify({sourceUrl:'https://poe2db.tw/us/'+name,eligibleSection:'normal',excludedSections:['corrupted','desecrated'],weightPolicy:'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS',slots:{magic:[1,1],rare:[2,2],source:'https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/Classes/Item.lua'},statPolicy:'Published integer display-unit bindings; display_source_value keys are internal, not game stat IDs. Conditional display_condition_present=1 is a presence marker, not a simulated combat effect.',craftedPolicy:'At most one Crafted. Zero ordinary weight. Uniform eligible removal then uniform valid sourced outcomes. Contempt occupies an ordinary slot and expands the opposite side; cap-loss preserves existing overflow. Ferocity and quality combine multiplicatively from original values with one central rounding, a provisional display assumption. Melancholy displays its external socketed-jewel condition only.',normalPolicy:'User-approved Normal simulator starting state; enabled_rarity remains magic,rare,unique.'},null,2)+'\n';
  const p=modifiers.filter(d=>d.affixType==='PREFIX'),s=modifiers.filter(d=>d.affixType==='SUFFIX');
  const catalog={metadata:{...original.metadata,snapshotId:'poe2db-'+base+'-basic-potent-20261004-'+digest(raw).slice(0,12),retrievedAt:new Date().toISOString(),sourceUrl:'https://poe2db.tw/us/'+name,rawSha256:digest(raw),detailsSha256:digest(details),prefixCount:p.length,suffixCount:s.length,prefixWeight:p.filter(d=>d.weight>0).length,suffixWeight:s.filter(d=>d.weight>0).length},base:{...original.base,id:data.baseitem.ItemType,name,sourceUrl:'https://poe2db.tw/us/'+name},modifiers};
  const dir='backend/src/main/resources/catalog/'+base+'/';fs.mkdirSync(dir,{recursive:true});fs.writeFileSync(dir+'base.raw.json',raw);fs.writeFileSync(dir+'details.raw.json',details);fs.writeFileSync(dir+'catalog.json',JSON.stringify(catalog,null,2)+'\n');
  fs.writeFileSync('frontend/src/shared/test/'+base+'-catalog.json',JSON.stringify(catalog,null,2)+'\n');
  for(const d of modifiers)allDefinitions[d.id]={baseItemId:catalog.base.id,affixType:d.affixType,familyIds:d.familyIds,stats:d.stats,crafted:d.tags.includes('crafted')};
  for(const locale of ['en','ko','zh-CN','zh-TW','ja','es']) {
    const localized=parse(fs.readFileSync(root+base+'-'+locale+'.html','utf8'));
    for(const [i,d] of modifiers.entries()) {
      const en=rows[i],crafted=d.tags.includes('crafted');
      let matches=(crafted?(localized.liquid??[]):localized.normal).filter(r=>crafted?r.Code===en.Code:identity(r)===identity(en));
      if(crafted&&matches.length===0)matches=localized.normal.filter(r=>identity(r)===identity(en));
      if(crafted&&matches.length===0&&locale==='es'&&en.Code==='JewelLifeonKill') {
        const diamond=parse(fs.readFileSync(root+'diamond-es.html','utf8'));
        matches=diamond.normal.filter(r=>r.ModFamilyList.join()===en.ModFamilyList.join()&&r.ModGenerationTypeID===en.ModGenerationTypeID&&spans(r.str).map(m=>m.value).join()===spans(en.str).map(m=>m.value).join());
      }
      // Spanish omits Liquid tables. Preserve source-backed explicit translation for Potent.
      if(matches.length===0&&locale==='es'&&crafted&&en.Code.startsWith('CraftedJewel')) {
        const es={CraftedJewelMaximumChaosResistance: '{v0}% a la resistencia máxima al caos',CraftedJewelSuffixEffect:'{v0}% de aumento del efecto de los sufijos',CraftedJewelPrefixEffect:'{v0}% de aumento del efecto de los prefijos',CraftedJewelAdditionalSuffixAllowed:'Permite {v0} modificador de sufijo',CraftedJewelAdditionalPrefixAllowed:'Permite {v0} modificador de prefijo',CraftedJewelDebilitateOnHitWhileEmeraldSapphireSocketed:'Debilita a los enemigos al golpear mientras tengas una Emerald y una Sapphire engarzadas en tu árbol',CraftedJewelExposureOnHitWhileRubyEmeraldSocketed:'Inflige exposición elemental al golpear mientras tengas una Ruby y una Emerald engarzadas en tu árbol',CraftedJewelBlindOnHitWhileRubySapphireSocketed:'Ciega a los enemigos al golpear mientras tengas una Ruby y una Sapphire engarzadas en tu árbol'};
        assert(es[en.Code],en.Code); matches=[{...en,Name:clean(en.Name),str:en.str}]; matches[0].translated=es[en.Code];
      }
      if(!crafted&&matches.length>1) {
        const peers=data.normal.filter(r=>identity(r)===identity(en));
        assert.equal(matches.length,peers.length,'Duplicate signature count differs');
        matches=[matches[peers.indexOf(en)]];
      }
      assert.equal(matches.length,1,base+' '+locale+' '+d.id);
      const baseline=view(en),localizedView=view(matches[0]),key='basic-jewel-'+digest(d.id).slice(0,16);
      bindings.definitions[d.id]={stats:d.stats,englishText:d.text,values:baseline.values,template:key,sourceCode:en.Code??null};
      bindings.templates[locale][key]={name:localizedView.name,template:matches[0].translated??localizedView.template};
    }
  }
  console.log(base,{normal:data.normal.length,liquid:data.liquid.length,snapshot:catalog.metadata.snapshotId});
}
fs.writeFileSync(templatesFile,JSON.stringify(bindings,null,2)+'\n');
fs.writeFileSync('frontend/src/features/crafting/basicJewelDefinitions.json',JSON.stringify(allDefinitions,null,2)+'\n');
fs.writeFileSync('frontend/src/features/crafting/sapphireDefinitions.json',JSON.stringify(Object.fromEntries(Object.entries(allDefinitions).filter(([id])=>id.startsWith('sapphire:')).map(([id,{baseItemId,...d}])=>[id,d])),null,2)+'\n');
