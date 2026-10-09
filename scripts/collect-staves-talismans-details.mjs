import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/staves-talismans-source-bundle-2026-10-05'
fs.mkdirSync(`${root}/details`, { recursive: true })
for (const page of ['Staves', 'Talismans']) {
const data = JSON.parse(fs.readFileSync(`${root}/${page}.us.json`, 'utf8')).data
const known = new Map()
const signature = r => JSON.stringify([clean(r.Name), r.Level, r.ModGenerationTypeID, r.ModFamilyList, clean(r.str)])
const fresh = new Map()
for(const folder of fs.readdirSync(`${root}/details`).filter(p=>fs.statSync(`${root}/details/${p}`).isDirectory()))for(const file of fs.readdirSync(`${root}/details/${folder}`)) {
 const proof=JSON.parse(fs.readFileSync(`${root}/details/${folder}/${file}`,'utf8'))
 if(proof.row.hover)fresh.set(proof.row.hover,proof)
 if(proof.row.Code)fresh.set(`code:${proof.row.Code}`,proof)
}
function index(value) {
  if (Array.isArray(value)) { for (const x of value) index(x); return }
  if (!value || typeof value !== 'object') return
  if (value.row?.hover && value.code && value.url) { known.set(value.row.hover, value); known.set(signature(value.row), value) }
  for (const x of Object.values(value)) if (typeof x === 'object') index(x)
}
for (const dir of fs.readdirSync('backend/src/main/resources/catalog')) {
  const p = `backend/src/main/resources/catalog/${dir}`
  if (!fs.statSync(p).isDirectory()) continue
  for (const file of fs.readdirSync(p).filter(f => f.endsWith('.raw.json'))) index(JSON.parse(fs.readFileSync(`${p}/${file}`, 'utf8')))
}
fs.mkdirSync(`${root}/details/${page}`, { recursive: true })
for (const [kind, rows] of Object.entries(data).filter(([k]) => ['normal', 'essence', 'perfect_essence'].includes(k))) {
  for (let i = 0; i < rows.length; i++) {
    const path = `${root}/details/${page}/${kind}-${i}.json`
    if (fs.existsSync(path)) continue
    const row = rows[i]
    const shared=fresh.get(row.hover??`code:${row.Code}`)
    if(shared && shared.row.Level===row.Level && shared.row.ModGenerationTypeID===row.ModGenerationTypeID && JSON.stringify(shared.row.ModFamilyList)===JSON.stringify(row.ModFamilyList) && (row.Code===shared.row.Code || clean(row.str)===clean(shared.row.str))) {
      fs.writeFileSync(path,JSON.stringify({...shared,row,sharedFreshProof:true},null,2)+'\n',{flag:'wx'})
      continue
    }
    const previous = known.get(row.hover) ?? known.get(signature(row))
    let url = row.hover ?? `https://poe2db.tw/us/hover?s=${encodeURIComponent('Data\\Mods/' + row.Code)}`
    let response = await fetch(url)
    if (!response.ok && previous) { url = previous.url; response = await fetch(url) }
    if (!response.ok && kind === 'normal') {
      const codes = [...new Set(data.essence.filter(r => r.ModFamilyList.join(',') === row.ModFamilyList.join(',')).map(r => r.Code.replace(/\d+$/, '')))]
      for (const stem of codes) {
        const peers = rows.filter(r => r.ModFamilyList.join(',') === row.ModFamilyList.join(',') && r.ModGenerationTypeID === row.ModGenerationTypeID).sort((a,b) => +a.Level - +b.Level)
        const candidate = `${stem}${peers.findIndex(r => r.hover === row.hover) + 1}`
        const candidateUrl = `https://poe2db.tw/us/hover?s=${encodeURIComponent('Data\\Mods/' + candidate)}`
        const candidateResponse = await fetch(candidateUrl)
        if (!candidateResponse.ok) continue
        const candidateHtml = await candidateResponse.text()
        const header = clean(candidateHtml.match(/<h5 class="card-header">([^]*?)<\/h5>/)?.[1] ?? '')
        if (header !== clean(row.str)) continue
        url = candidateUrl
        response = new Response(candidateHtml)
        break
      }
    }
    let textLocale = 'us'
    let expectedText = clean(row.str)
    if (url === previous?.url && previous.textLocale && previous.textLocale !== 'us') {
      textLocale = previous.textLocale
      const localized = JSON.parse(fs.readFileSync(`${root}/${page}.${textLocale}.json`, 'utf8')).data[kind][i]
      assert.equal(localized.Level, row.Level)
      assert.deepEqual(localized.ModFamilyList, row.ModFamilyList)
      expectedText = clean(localized.str)
    }
    if (!response.ok && row.hover) {
      for (const locale of ['kr', 'jp', 'cn', 'tw', 'sp']) {
        const local = JSON.parse(fs.readFileSync(`${root}/${page}.${locale}.json`, 'utf8')).data[kind][i]
        assert.equal(local.Level, row.Level)
        assert.deepEqual(local.ModFamilyList, row.ModFamilyList)
        const localUrl = new URL(local.hover, `https://poe2db.tw/${locale}/hover`).href
        const localResponse = await fetch(localUrl)
        if (!localResponse.ok) continue
        url = localUrl
        response = localResponse
        textLocale = locale
        expectedText = clean(local.str)
        break
      }
    }
    assert(response.ok, `${url}: ${response.status}`)
    const html = await response.text()
    const code = row.Code ?? (url.includes('/hover?') ? new URL(url).searchParams.get('s').split('/').at(-1) : previous?.code) ?? html.match(/Data(?:%5C|\\|\/|%2F)Mods(?:%2F|\/|\\)([^'"&<>]+)/i)?.[1]
      ?? html.match(/<tr><th>Code<td>([^<\n]+)/)?.[1]
    const fields = Object.fromEntries([...html.matchAll(/<tr><th>([^<]+)<td>([^]*?)(?=<tr>|<\/table>)/g)].map(m => [clean(m[1]), clean(m[2])]))
    const detailText = clean(html.match(/<h5 class="card-header">([^]*?)<\/h5>/)?.[1] ?? '')
    const archetypeTarget = kind === 'essence' && data.normal.some(r => r.Level === row.Level && r.ModGenerationTypeID === row.ModGenerationTypeID && r.ModFamilyList.join(',') === row.ModFamilyList.join(',') && clean(r.str) === detailText)
    const genericDefence = kind === 'essence' && /^LocalIncreased(?:(?:PhysicalDamageReductionRating|EvasionRating|EnergyShield)Percent|ArmourAndEvasion|ArmourAndEnergyShield|EvasionAndEnergyShield)\d+_?$/.test(row.Code) && /increased Armour, Evasion and Energy Shield$/.test(expectedText)
    if(genericDefence) assert.deepEqual(detailText.match(/-?\d+/g),expectedText.match(/-?\d+/g),'Generic defence Essence retains exact numeric spans; ordered spawn determines applicability')
    if (!row.Code || !/^(Strength|Dexterity|Intelligence)\d+$|^EssenceAbyss/.test(row.Code)) assert(detailText === expectedText || archetypeTarget || genericDefence, `${page}/${kind}/${i}/${row.Code}: actual=${detailText}; expected=${expectedText}; public detail must match a reviewed target`)
    const stats = [...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => {
      const n = clean(m[2]).match(/-?\d+(?:\.\d+)?/g).map(Number)
      return { id: clean(m[1]).replaceAll(' ', '_'), min: n[0], max: n[1], locality: clean(m[3]) }
    })
    const spawn = [...(html.match(/<tr><th>Spawn Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1] ?? '').matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
    assert(stats.length && spawn.length, `${kind}/${i} stats and spawn`)
    fs.writeFileSync(path, JSON.stringify({ row, url, textLocale, detailText, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), html, fields, code, stats, spawn }, null, 2) + '\n', { flag: 'wx' })
    const saved=JSON.parse(fs.readFileSync(path,'utf8'))
    if(row.hover)fresh.set(row.hover,saved)
    if(row.Code)fresh.set(`code:${row.Code}`,saved)
  }
  console.log(`${kind}: ${rows.length}`)
}

}
