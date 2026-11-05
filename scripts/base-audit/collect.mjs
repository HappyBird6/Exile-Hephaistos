import fs from '../evidence-files.cjs'
import { createHash } from 'node:crypto'
import assert from 'node:assert/strict'
import { cleanSource as clean } from '../armour-source.mjs'
const root = 'evidence:base-audit'
const plan = JSON.parse(fs.readFileSync(`${root}/plan.json`))
const slugs = process.argv.slice(2).length ? process.argv.slice(2) : plan.collect
const jobs = slugs.flatMap((slug) => ['us','kr','jp','cn','tw','sp'].map((locale) => ({ slug, locale })))
const failures = []
let index = 0
await Promise.all(Array.from({ length: 5 }, async () => {
  while (index < jobs.length) {
    const { slug, locale } = jobs[index++]
    const target = `${root}/${slug}.${locale}.json`
    if (fs.existsSync(target)) continue
    try {
      const url = `https://poe2db.tw/${locale}/${slug}`
      const response = await fetch(url, { signal: AbortSignal.timeout(30000) })
      assert(response.ok, `HTTP ${response.status}`)
      const html = await response.text()
      const sections = html.split(/(?=<div class="newItemPopup NormalPopup)/).slice(1)
      const variants = sections.map((section) => {
        const popup = section.match(/^<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
        const fields = Object.fromEntries([...section.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map((match) => [clean(match[1]), clean(match[2])]))
        return { name: clean(popup?.match(/<span class="lc">([^]*?)<\/span>/)?.[1] ?? ''), card: clean(popup ?? ''), requirements: clean(popup?.match(/<div class="requirements">([^]*?)<\/div>/)?.[1] ?? ''), fields }
      }).filter((base) => base.fields.Type?.startsWith('Metadata/Items/'))
      assert(variants.length, 'No extracted base identities')
      const proofs = [...html.matchAll(/<h5 class="card-header">((?:(?!<\/h5>)[^])*?)<\/h5>\s*<table[^]*?<\/table>/g)].filter((match) => match[0].includes('<tr><th>Family')).map((match) => ({ text: clean(match[1]), family: clean(match[0].match(/<tr><th>Family<td>([^]*?)(?=<tr>|<\/table>)/)[1]), tags: [...(match[0].match(/<tr><th>Craft Tags<td>([^]*?)(?=<\/table>)/)?.[1] ?? '').matchAll(/<span class='badge bg-primary'>([^]*?)<\/span>/g)].map((tag) => clean(tag[1]).toLowerCase()), stats: [...match[0].matchAll(/<li>([^<]*?) <span class="badge bg-primary">([^]*?)<\/span> <span class="badge bg-secondary">([^]*?)<\/span><\/li>/g)].map((stat) => { const bounds = clean(stat[2]).match(/-?\d+/g).map(Number); return { id: clean(stat[1]).replaceAll(' ', '_'), min: bounds[0], max: bounds[1], locality: clean(stat[3]) } }), html: match[0] }))
      const image = /<meta property="og:image" content="([^"]+)"/.exec(html)?.[1]
      const value = { url, retrievedAt: new Date().toISOString(), sha256: createHash('sha256').update(html).digest('hex'), variants, proofs, image }
      fs.writeFileSync(target, JSON.stringify(value, null, 2) + '\n')
      console.log(`${slug}/${locale}: ${variants.length} variants, ${proofs.length} implicit proofs`)
    } catch (error) { failures.push({ slug, locale, error: String(error) }); console.log(`FAILED ${slug}/${locale}: ${String(error)}`) }
  }
}))
fs.writeFileSync(`${root}/collection-errors.json`, JSON.stringify(failures, null, 2) + '\n')
if (failures.length) process.exitCode = 1
