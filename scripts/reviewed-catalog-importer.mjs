import assert from 'node:assert/strict'
const numberPattern = /[+]?(?:\(-?\d+(?:\.\d+)?[—–]-?\d+(?:\.\d+)?\)|-?\d+(?:\.\d+)?)/g
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b)
export function orderedSpawnEligible(detail,tags) {const first=detail.spawn.find(s=>tags.includes(s.tag));assert(first,`${detail.url}: ordered spawn missing`);return first.weight>0}
export function createDisplayBinder(display,localeKeys,namespace) {
return function bind(d,texts,code) {
  if (display.definitions[d.id]) {
    assert.equal(display.definitions[d.id].englishText,d.text)
    assert.deepEqual(display.definitions[d.id].stats,d.stats)
    for (const l of localeKeys) {
      const b=display.definitions[d.id]
      assert.equal(display.templates[l][b.template].template.replace(/\{v(\d+)\}/g,(_,n)=>b.values[+n]).replaceAll('\n',''),texts[l].replaceAll('\n',''))
    }
    return
  }
  const template = `${namespace}.${d.id}`
  if (d.layer==='IMPLICIT' && d.stats.every(s=>s.min===s.max)) {
    display.definitions[d.id]={stats:d.stats,englishText:d.text,values:[],template,sourceCode:code}
    for(const [l,text]of Object.entries(texts))display.templates[l][template]={name:d.name,template:text}
    return
  }
  const values = [...d.text.matchAll(numberPattern)].map(m=>m[0])
  const available = [...d.stats]
  const valueStats = values.map(value=>{
    const n=value.match(/-?\d+(?:\.\d+)?/g).map(Number)
    const range=n.length===1?[n[0],n[0]]:n
    const candidates=available.flatMap(s=>[1,-1,100,-100,60,-60].filter(divisor=>equal([s.min/divisor,s.max/divisor].sort((a,b)=>a-b),range)).map(divisor=>({id:s.id,divisor})))
    assert.equal(candidates.length,1,`${d.id}: source span must have one unambiguous stat binding: ${value}`)
    const chosen=candidates[0]
    available.splice(available.findIndex(s=>s.id===chosen.id),1)
    return chosen
  })
  assert(available.every(s=>s.min===s.max),`${d.id}: undisplayed variable stat needs a reviewed binding`)
  display.definitions[d.id]={stats:d.stats,englishText:d.text,values,template,sourceCode:code??null,valueStats}
  for (const [l,text] of Object.entries(texts)) {
    const localValues=[...text.matchAll(numberPattern)].map(m=>m[0])
    assert.deepEqual(localValues,values,`${d.id}/${l}: exact numeric spans`)
    let i=0
    const translated=text.replace(numberPattern,()=>`{v${i++}}`)
    display.templates[l][template]={name:d.name,template:translated}
    assert.equal(translated.replace(/\{v(\d+)\}/g,(_,n)=>values[+n]),text)
  }
}
}
