const fs = require('fs'), crypto = require('crypto'), assert = require('assert/strict');
const root = 'backend/src/main/resources/catalog/sapphire/';
const data = JSON.parse(fs.readFileSync('E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004/mods.json'));
const old = JSON.parse(fs.readFileSync(root + 'catalog.json'));
const clean = s => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '').replace(/&amp;/g, '&').replace(/&nbsp;/g, ' ').trim();
const slug = s => s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
const liquidRows = data.liquid.filter(r => !r.Name.includes('Potent_'));
assert.equal(data.normal.length, 58); assert.equal(liquidRows.length, 10);
function definition(r, crafted = false) {
  assert(r.spawn_no.includes('intjewel'));
  assert(['1','2'].includes(r.ModGenerationTypeID));
  const side = r.ModGenerationTypeID === '1' ? 'prefix' : 'suffix';
  const text = clean(r.str), range = text.match(/\((-?\d+)—(-?\d+)\)/);
  const fixed = text.match(/[+-]?(\d+)%/);
  assert(range || fixed, text);
  const id = crafted ? 'sapphire:crafted:' + r.Code : 'sapphire:' + side + ':' + slug(r.Name);
  const prior = old.modifiers.find(d => d.id === id);
  return {id, name: crafted ? clean(r.Name) + ' (Crafted)' : r.Name, layer:'EXPLICIT', affixType:side.toUpperCase(),
    familyIds:r.ModFamilyList, requiredItemLevel:Number(r.Level), weight:crafted ? 0 : 1, tier:1, text,
    stats:prior ? prior.stats : [{id:'display_source_value', min:Number(range ? range[1] : fixed[1]), max:Number(range ? range[2] : fixed[1])}],
    tags:[...r.fossil_no, ...(crafted ? ['crafted'] : [])], sourceUrl:crafted ? 'https://poe2db.tw/us/' + r.Name.match(/href="([^"]+)"/)[1] : r.hover};
}
const modifiers = [...data.normal.map(r => definition(r)), ...liquidRows.map(r => definition(r,true))];
const raw = JSON.stringify({sourceUrl:'https://poe2db.tw/us/Sapphire', baseitem:data.baseitem, normal:data.normal, liquid:liquidRows},null,2)+'\n';
const details = JSON.stringify({sourceUrl:'https://poe2db.tw/us/Sapphire', eligibleSection:'normal', excludedSections:['corrupted','desecrated','liquid'],
  weightPolicy:'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS; DropChance=1 is not imported as a verified weight',
  slots:{magic:[1,1],rare:[2,2],source:'https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/Classes/Item.lua',status:'corroborated simulator model'},
  statPolicy:'Single published display-unit range per row; display_source_value is an internal key, not a verified game stat ID. Minion attack and cast speed uses one shared published magnitude.',
  normalPolicy:'Normal Sapphire is a user-approved reversible simulator starting state; PoE2DB enabled_rarity lists magic,rare,unique.',
  craftedPolicy:'Zero ordinary generation weight; identified persistently by sourced modifier ID and crafted tag; one per item. Every random-removal branch must permit the fixed outcome; uncertain branches refuse unchanged.'},null,2)+'\n';
const digest = s => crypto.createHash('sha256').update(s).digest('hex');
const prefixes=modifiers.filter(d=>d.affixType==='PREFIX'), suffixes=modifiers.filter(d=>d.affixType==='SUFFIX');
const catalog={metadata:{...old.metadata,snapshotId:'poe2db-sapphire-basic-liquid-20261004-'+digest(raw).slice(0,12),retrievedAt:new Date().toISOString(),weightPolicy:'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS',rawSha256:digest(raw),detailsSha256:digest(details),prefixCount:prefixes.length,suffixCount:suffixes.length,prefixWeight:23,suffixWeight:35},base:{...old.base,magicPrefixes:1,magicSuffixes:1,rarePrefixes:2,rareSuffixes:2},modifiers};
fs.writeFileSync(root+'base.raw.json',raw); fs.writeFileSync(root+'details.raw.json',details);
fs.writeFileSync(root+'catalog.json',JSON.stringify(catalog,null,2)+'\n');
fs.writeFileSync('frontend/src/shared/test/sapphire-catalog.json',JSON.stringify(catalog,null,2)+'\n');
fs.writeFileSync('frontend/src/features/crafting/sapphireDefinitions.json',JSON.stringify(Object.fromEntries(modifiers.map(d=>[d.id,{affixType:d.affixType,familyIds:d.familyIds,stats:d.stats,crafted:d.tags.includes('crafted')} ])),null,2)+'\n');
console.log({ordinary:58,crafted:10,snapshot:catalog.metadata.snapshotId});
