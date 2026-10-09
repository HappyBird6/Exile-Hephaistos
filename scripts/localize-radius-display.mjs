import fs from 'node:fs'
const file='frontend/src/shared/i18n/modifierTemplates.json',data=JSON.parse(fs.readFileSync(file,'utf8'))
const text={en:'Base radius (source value): [1000]',ko:'기본 반경 (출처 값): [1000]','zh-CN':'基础半径（来源值）：[1000]','zh-TW':'基礎半徑（來源值）：[1000]',ja:'基本半径（出典の値）：[1000]',es:'Radio base (valor de la fuente): [1000]'}
const ids=Object.keys(data.definitions).filter(id=>id.startsWith('time-lost-')&&id.endsWith(':implicit:base-radius'))
if(ids.length!==4)throw Error('Expected exact four Time-Lost implicits')
for(const id of ids) {
 const binding=data.definitions[id]
 if(binding.englishText!=='local jewel effect base radius [1000]'||binding.stats.length!==1||binding.stats[0].min!==1000||binding.stats[0].max!==1000)throw Error('Changed radius source')
 for(const locale of Object.keys(text))data.templates[locale][binding.template].template=text[locale]
}
fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-radius-display-2026-10-04.json',JSON.stringify({ids,text,provenance:'OWN_DISPLAY_LABEL_FROM_CANONICAL_STAT_ID',policy:'Raw [1000] is preserved. No distance unit, Medium/Large label, additive radius, or tree effect is inferred.'},null,2)+'\n')
