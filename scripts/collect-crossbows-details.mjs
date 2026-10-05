import fs from 'node:fs'
import assert from 'node:assert/strict'
import crypto from 'node:crypto'
import { cleanSource as clean } from './armour-source.mjs'
const root = 'docs/evidence/crossbows-source-bundle-2026-10-05'
const data = JSON.parse(fs.readFileSync(`${root}/Crossbows.us.json`, 'utf8')).data
const known = new Map()
const signature = r => JSON.stringify([clean(r.Name), r.Level, r.ModGenerationTypeID, r.ModFamilyList, clean(r.str)])
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
fs.mkdirSync(`${root}/details`, { recursive: true })
for (const [kind, rows] of Object.entries(data).filter(([k]) => ['normal', 'essence', 'perfect_essence'].includes(k))) {
  for (let i = 0; i < rows.length; i++) {
    const path = `${root}/details/${kind}-${i}.json`
    if (fs.existsSync(path)) continue
    const row = rows[i]
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
    if (!response.ok && row.hover) {
      for (const locale of ['kr', 'jp', 'cn', 'tw', 'sp']) {
        const local = JSON.parse(fs.readFileSync(`${root}/Crossbows.${locale}.json`, 'utf8')).data[kind][i]
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
    if (!row.Code || !/^(Strength|Dexterity|Intelligence)\d+$|^EssenceAbyss/.test(row.Code)) assert.equal(detailText, expectedText, 'Public detail must match exact class text')
    const stats = [...html.matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map(m => {
      const n = clean(m[2]).match(/-?\d+(?:\.\d+)?/g).map(Number)
      return { id: clean(m[1]).replaceAll(' ', '_'), min: n[0], max: n[1], locality: clean(m[3]) }
    })
    const spawn = [...(html.match(/<tr><th>Spawn Tags<td>([^]*?)(?=<tr>|<\/table>)/)?.[1] ?? '').matchAll(/class=['"]badge bg-primary['"]>([^<]+): (\d+)<\/span>/g)].map(m => ({ tag: m[1], weight: +m[2] }))
    assert(stats.length && spawn.length, `${kind}/${i} stats and spawn`)
    fs.writeFileSync(path, JSON.stringify({ row, url, textLocale, detailText, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), html, fields, code, stats, spawn }, null, 2) + '\n', { flag: 'wx' })
  }
  console.log(`${kind}: ${rows.length}`)
}
