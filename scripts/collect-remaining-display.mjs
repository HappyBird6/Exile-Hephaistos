// Display source collection only. Canonical catalogs and films are read-only.
import fs from 'node:fs'
import path from 'node:path'
import { createHash } from 'node:crypto'

const cache = process.argv[2]
if (!cache) throw new Error('Provide a new owned source cache')
fs.mkdirSync(cache, { recursive: true })
const locales = { en: 'us', ko: 'kr', 'zh-CN': 'cn', 'zh-TW': 'tw', ja: 'jp', es: 'sp' }
const termsPath = 'frontend/src/shared/i18n/gameTerms.json'
const terms = JSON.parse(fs.readFileSync(termsPath, 'utf8'))
const tooltip = JSON.parse(fs.readFileSync('frontend/src/features/crafting/materialTooltips.json', 'utf8'))
const inventory = JSON.parse(fs.readFileSync('backend/src/main/resources/crafting/registry-v2.json','utf8')).entries.map(e=>e.id)
const evidence = { retrievedAt: new Date().toISOString(), pages: [], gaps: [], coverage: {} }
const clean = s => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '').replace(/&amp;/g, '&').replace(/&nbsp;?/g, ' ').replace(/&quot;/g, '"').replace(/&#0*39;|&apos;/g, "'").replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(+n)).trim()
async function page(locale, slug, retained) {
  const url = `https://poe2db.tw/${locales[locale]}/${slug}`
  const file = path.join(cache, `${locale}-${slug}.html`)
  let html
  if (fs.existsSync(file)) html = fs.readFileSync(file, 'utf8')
  else if (retained && fs.existsSync(retained)) { html = fs.readFileSync(retained, 'utf8'); fs.writeFileSync(file, html) }
  else { const r = await fetch(url); if (!r.ok) throw new Error(`${r.status}: ${url}`); html = await r.text(); fs.writeFileSync(file, html) }
  evidence.pages.push({ url, sha256: createHash('sha256').update(html).digest('hex'), retained: !!retained })
  return html
}
for (const locale of Object.keys(locales)) {
  for (const category of ['Liquid_Emotions', 'Currency', 'Stackable_Currency', 'Essence', 'Omen', 'Catalysts']) {
    const html = await page(locale, category, category === 'Liquid_Emotions' ? `../ancient-liquid-20261004/liquid_emotions-${locale}.html` : undefined)
    for (const row of html.split('<div class="col">')) {
      const item = row.match(/<div class="flex-grow-1 ms-2"><a[^>]+href="([^"#]+)"[^>]*>(.*?)<\/a>/s)
      if (!item || !inventory.includes(item[1])) continue
      const id = item[1], name = clean(item[2])
      const itemKey = item[2].match(/alt="([^"]+)"/)?.[1] ?? row.slice(0, item.index).match(/alt="([^"]+)"/)?.[1]
      if (!name || !itemKey || (locale !== 'en' && terms.en[id]?.itemKey !== itemKey)) continue
      const body = row.slice(item.index + item[0].length).split('<div class="flex-shrink-0">')[0]
      const lines = [...body.matchAll(/<div class="(?:explicitMod|descrText)">(.*?)<\/div>/gs)].map(m => clean(m[1])).filter(Boolean)
      terms[locale][id] = { name, lines: locale === 'en' && tooltip[id] ? tooltip[id].lines.filter(l => !l.startsWith('Stack Size:')) : lines, itemKey, sourceUrl: `https://poe2db.tw/${locales[locale]}/${id}` }
    }
  }
  for (const slug of ['Ruby', 'Emerald', 'Diamond']) {
    const html = await page(locale, slug, `../basic-jewel-potent-20261004/${slug.toLowerCase()}-${locale}.html`)
    const itemKey = html.match(/Metadata\/Items\/Jewels\/[A-Za-z]+/)?.[0]
    const name = clean(html.match(/<title>(.*?) - /s)?.[1] ?? '')
    if (!name || !itemKey || (locale !== 'en' && terms.en[slug]?.itemKey !== itemKey)) throw new Error(`Base identity: ${locale}:${slug}`)
    terms[locale][slug] = { name, lines: [], itemKey, sourceUrl: `https://poe2db.tw/${locales[locale]}/${slug}` }
  }
  evidence.coverage[locale] = { inventory: inventory.length, names: inventory.filter(id => terms[locale][id]).length, missing: inventory.filter(id => !terms[locale][id]), emptyDescriptions: inventory.filter(id => terms[locale][id] && !terms[locale][id].lines.length) }
  console.log(locale, JSON.stringify(evidence.coverage[locale]))
}
fs.writeFileSync(termsPath, JSON.stringify(terms, null, 2) + '\n')
fs.writeFileSync('docs/evidence/i18n-remaining-terms-2026-10-04.json', JSON.stringify(evidence, null, 2) + '\n')
