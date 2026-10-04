import fs from 'node:fs'
import crypto from 'node:crypto'
import assert from 'node:assert/strict'
const root = 'docs/evidence/top-base-bundle-2026-10-04'
fs.mkdirSync(root, { recursive: true })
const clean = (s) => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').trim()
const digest = (s) => crypto.createHash('sha256').update(s).digest('hex')
const candidates = [['Soldier_Cuirass', 'body', 'rusted-cuirass'], ['Imperial_Greathelm', 'helmet', 'rusted-greathelm']]
const bundle = []
for (const [slug, family, pool] of candidates) {
  const sources = await Promise.all(['us', 'kr', 'jp', 'cn', 'tw', 'sp'].map(async (locale) => {
    const url = `https://poe2db.tw/${locale}/${slug}`
    const response = await fetch(url)
    assert(response.ok)
    const html = await response.text()
    const popup = html.match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
    assert(popup, url)
    const name = clean(popup.match(/<span class="lc">([^]*?)<\/span>/)[1])
    const fields = Object.fromEntries([...html.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map((m) => [clean(m[1]), clean(m[2])]))
    const record = { url, retrievedAt: new Date().toISOString(), sha256: digest(html), name, card: clean(popup), requirements: clean(popup.match(/<div class="requirements">([^]*?)<\/div>/)?.[1] ?? ''), fields }
    fs.writeFileSync(`${root}/${slug}.${locale}.json`, JSON.stringify(record, null, 2) + '\n')
    return [locale, record]
  }))
  const records = Object.fromEntries(sources)
  const source = records.us
  const tags = source.fields.Tags.split(', ')
  const details = JSON.parse(fs.readFileSync(`backend/src/main/resources/catalog/${pool}/details.raw.json`))
  const eligible = details.map((d) => {
    const first = d.parsed.spawn.find((s) => [...tags, 'default'].includes(s.tag))
    assert(first && first.weight > 0, `${slug}: ${d.code}`)
    return d.code
  })
  const armour = Number(source.card.match(/Armour: (\d+)/)[1])
  const requiredLevel = Number(source.card.match(/Level (\d+)/)[1])
  const strength = Number(source.card.match(/(\d+) Str/)[1])
  assert.equal(source.fields['Quality.max_quality'], '20')
  bundle.push({ key: slug === 'Soldier_Cuirass' ? 'soldier' : 'imperial', family, pool, slug, id: source.fields.Type, name: source.name, sourceUrl: source.url, sourceSha256: source.sha256, sourceTags: tags, armour, requiredLevel, strength, maximumQuality: 20, eligibleCodes: eligible, locales: Object.fromEntries(sources.map(([l, r]) => [l, r.name])) })
}
fs.writeFileSync(`${root}/bundle.json`, JSON.stringify(bundle, null, 2) + '\n')
console.log(bundle.map(({ eligibleCodes, ...b }) => ({ ...b, eligibleCount: eligibleCodes.length })))
