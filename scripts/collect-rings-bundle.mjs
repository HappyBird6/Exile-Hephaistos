import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
import { collectArmourSource, cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/rings-source-bundle-2026-10-04'
fs.mkdirSync(root, { recursive: true })
const locales = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const write = (p,v) => fs.writeFileSync(p, JSON.stringify(v,null,2)+'\n')
for (const slug of ['Kinetic_Ring','Vitalic_Ring','Mnemonic_Ring','Pearl_Ring','Amethyst_Ring','Prismatic_Ring','Ruby_Ring','Two-Stone_Ring']) {
  for (const locale of Object.values(locales)) {
    const path = `${root}/${slug}.${locale}.json`
    if (fs.existsSync(path)) continue
    if (slug !== 'Two-Stone_Ring') write(path, await collectArmourSource(slug,locale))
    else {
      const url = `https://poe2db.tw/${locale}/${slug}`, r = await fetch(url)
      assert(r.ok); const html = await r.text()
      // The shared page contains three identities; select the first Fire/Cold section explicitly.
      const section = html.slice(0,html.indexOf('Metadata/Items/Rings/FourRing13b'))
      const popup = section.match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
      assert(popup)
      const fields = Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])]))
      assert.equal(fields.Type, 'Metadata/Items/Rings/FourRing13a', 'Fire/Cold identity must be sourced, never assigned')
      write(path,{url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),name:clean(popup.match(/<span class="lc">([^]*?)<\/span>/)[1]),card:clean(popup),requirements:clean(popup.match(/<div class="requirements">([^]*?)<\/div>/)?.[1]??''),fields})
    }
  }
  const path = `${root}/${slug}.implicit.json`
  if (!fs.existsSync(path) || !JSON.parse(fs.readFileSync(path,'utf8')).family) {
    const url=`https://poe2db.tw/us/${slug}`, r=await fetch(url); assert(r.ok)
    let html=await r.text(); const sha256=crypto.createHash('sha256').update(html).digest('hex')
    if(slug==='Two-Stone_Ring') html=html.slice(0,html.indexOf('Metadata/Items/Rings/FourRing13b'))
    const stats=[...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m=>{const n=clean(m[2]).match(/-?\d+/g).map(Number);return {id:clean(m[1]).replace(/ /g,'_'),min:n[0],max:n[1],locality:clean(m[3])}})
    assert(stats.length)
    const fields=Object.fromEntries([...html.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m=>[clean(m[1]),clean(m[2])]))
    const family=clean(html.match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)?.[1]??'')
    const tagSection=html.match(/<tr><th>Craft Tags<td>([^]*?)<\/table>/)?.[1]??''
    const craftTags=[...tagSection.matchAll(/<span class='badge bg-primary'>([^]*?)<\/span>/g)].map(m=>clean(m[1]).toLowerCase())
    const implicitText=clean(html.match(/<h5 class="card-header">([^]*?)<\/h5>/g)?.find(s=>!s.includes(' /7'))??'')
    assert(family && craftTags.length)
    write(path,{url,retrievedAt:new Date().toISOString(),sha256,stats,family,craftTags,implicitText,html})
  }
}
for(const [locale,source] of Object.entries(locales)) {
  const path=`${root}/Rings.${locale}.json`; if(fs.existsSync(path))continue
  const url=`https://poe2db.tw/${source}/Rings`,r=await fetch(url);assert(r.ok);const html=await r.text()
  const line=html.split('\n').find(l=>l.includes('new ModsView('));assert(line)
  const all=JSON.parse(line.slice(line.indexOf('new ModsView(')+13,line.lastIndexOf(');')))
  write(path,{url,retrievedAt:new Date().toISOString(),sha256:crypto.createHash('sha256').update(html).digest('hex'),data:{normal:all.normal,essence:all.essence,perfect_essence:all.perfect_essence}})
}
console.log('Collected eight exact Ring identities, implicit proofs and six class pools')
