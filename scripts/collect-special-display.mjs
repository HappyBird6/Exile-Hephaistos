import fs from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'
const cache = process.argv[2]
if (!cache) throw Error('Owned source cache required')
fs.mkdirSync(cache, { recursive: true })
const file = 'frontend/src/shared/i18n/modifierTemplates.json'
const data = JSON.parse(fs.readFileSync(file, 'utf8'))
const locales = { en:'us', ko:'kr', 'zh-CN':'cn', 'zh-TW':'tw', ja:'jp', es:'sp' }
const defs = {}
for (const dir of fs.readdirSync('backend/src/main/resources/catalog'))
  for (const f of fs.readdirSync('backend/src/main/resources/catalog/'+dir).filter(f=>f==='catalog.json'||f.endsWith('.catalog.json')))
    for (const d of JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/'+dir+'/'+f)).modifiers??[]) defs[d.id]=d
const missing = Object.values(defs).filter(d=>!data.definitions[d.id])
const evidence = { retrievedAt:new Date().toISOString(), pages:[], bindings:[], gaps:[] }
const clean = s=>s.replace(/<br\s*\/?>/gi,'\n').replace(/<[^>]*>/g,'').replace(/&amp;/g,'&').replace(/&nbsp;/g,' ').trim()
const normal = s=>s.replace(/\s+/g,'').replace(/[–—]/g,'-')
function display(html) {
  const spans=[]
  let start,depth=0
  for(const m of html.matchAll(/<span\b[^>]*>|<\/span>/g)) {
    if(start!==undefined) {
      depth+=m[0].startsWith('</')?-1:1
      if(!depth){spans.push({start,end:m.index+m[0].length,value:clean(html.slice(start,m.index+m[0].length))});start=undefined}
    } else if(/class=['"]mod-value['"]/.test(m[0])){start=m.index;depth=1}
  }
  const values=spans.map(s=>s.value)
  let text=html
  for(const [i,s]of [...spans.entries()].reverse())text=text.slice(0,s.start)+`{v${i}}`+text.slice(s.end)
  return {template:clean(text),values,text:clean(html)}
}
async function page(locale,d) {
  const code=d.sourceUrl.split('s=')[1], slug=code?decodeURIComponent(code).split('/').at(-1):d.sourceUrl.split('/').at(-1)
  const url=d.sourceUrl.replace('/us/',`/${locales[locale]}/`)
  const target=path.join(cache,`${locale}-${slug}.html`)
  let html
  if(fs.existsSync(target))html=fs.readFileSync(target,'utf8')
  else {const r=await fetch(url,{signal:AbortSignal.timeout(15000)});if(!r.ok)throw Error(`${r.status}:${url}`);html=await r.text();fs.writeFileSync(target,html)}
  evidence.pages.push({url,sha256:createHash('sha256').update(html).digest('hex')})
  let str=code?html.match(/<h5 class="card-header"><span class='item_magic'>(.*?)<\/span><\/h5>/s)?.[1]:html.match(/<div class="implicitMod">(.*?)<\/div>/s)?.[1]
  if(!str)throw Error('Missing exact modifier display: '+url)
  return { ...display(str),name:clean(html.match(/<tr><th>Name<td>([^<\n]+)/)?.[1]??d.name) }
}
for (const d of missing) {
  const rows={}
  for(const locale of Object.keys(locales)) {
    try {rows[locale]=await page(locale,d)} catch(e) {evidence.gaps.push({id:d.id,locale,reason:e.message})}
  }
  if(!rows.en || normal(rows.en.text)!==normal(d.text)) {evidence.gaps.push({id:d.id,reason:'English snapshot mismatch',actual:rows.en?.text});continue}
  const id='special.'+createHash('sha256').update(d.id).digest('hex').slice(0,16)
  data.definitions[d.id]={stats:d.stats??[],englishText:d.text,values:rows.en.values,template:id,sourceCode:d.sourceUrl}
  for(const [locale,row]of Object.entries(rows))data.templates[locale][id]={template:row.template,name:row.name}
  evidence.bindings.push(d.id)
  console.log(d.id,Object.keys(rows).length)
}
fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n')
fs.writeFileSync('docs/evidence/i18n-special-sources-2026-10-04.json',JSON.stringify(evidence,null,2)+'\n')
console.log('bindings',evidence.bindings.length,'gaps',evidence.gaps)
