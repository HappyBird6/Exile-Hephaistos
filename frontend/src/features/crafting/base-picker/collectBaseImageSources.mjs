// Reads existing committed source evidence; does not fetch or invent image identities.
/* global process, fetch, AbortSignal, console */
import { readFileSync, readdirSync, writeFileSync, existsSync } from 'node:fs'
import { resolve, relative, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'
const destination = dirname(fileURLToPath(import.meta.url))
const root = resolve(destination, '../../../../..')
const top = JSON.parse(
  readFileSync(resolve(destination, '../topBases.json'), 'utf8'),
)
const policies = JSON.parse(
  readFileSync(resolve(destination, '../basePolicies.json'), 'utf8'),
)
const evidence = resolve(root, 'docs/evidence')
const files = readdirSync(evidence, { recursive: true }).map((file) =>
  resolve(evidence, file),
)
const rows = {}
const manifest = resolve(destination, 'baseImageSources.json')
const previous = existsSync(manifest)
  ? JSON.parse(readFileSync(manifest, 'utf8'))
  : {}
const missing = []
for (const [key, base] of Object.entries({ ...policies.legacy, ...top })) {
  const slug = base.slug.replaceAll(' ', '_')
  const slugs = [
    slug,
    (base.name ?? base.initialText.split('\n').at(-1)).replaceAll(' ', '_'),
  ]
  const candidates = files.filter((path) =>
    slugs.some(
      (name) =>
        path.endsWith(`/${name}.us.html`) ||
        path.endsWith(`\\${name}.us.html`) ||
        path.endsWith(`/${name}.us.json`) ||
        path.endsWith(`\\${name}.us.json`) ||
        path.endsWith(`\\${name}.json`),
    ),
  )
  let found
  for (const path of candidates) {
    const bytes = readFileSync(path)
    const raw = bytes.toString('utf8')
    let html = raw
    let icon
    if (path.endsWith('.json')) {
      const value = JSON.parse(raw)
      html = value.html ?? ''
      icon = value.fields?.Icon ?? value.data?.Icon
    }
    const og =
      /<meta property="og:image" content="(https:\/\/cdn\.poe2db\.tw\/image\/[^"<>]+)"/.exec(
        html,
      )?.[1]
    if (!og && !icon) continue
    found = {
      image: og ?? `https://cdn.poe2db.tw/image/${icon}.webp`,
      source: relative(root, path).replaceAll('\\', '/'),
      sourceSha256: createHash('sha256').update(bytes).digest('hex'),
    }
    break
  }
  if (key === 'solar')
    found = {
      image: '/assets/currency/solar-amulet.webp',
      source: 'frontend/public/assets/currency/sources.json',
      sourceSha256: createHash('sha256')
        .update(
          readFileSync(
            resolve(root, 'frontend/public/assets/currency/sources.json'),
          ),
        )
        .digest('hex'),
    }
  if (!found) found = previous[base.id]
  if (!found && process.argv.includes('--fetch-missing')) {
    const url = `https://poe2db.tw/us/${slug}`
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(20000) })
      if (!response.ok) throw new Error(String(response.status))
      const html = await response.text()
      const image =
        /<meta property="og:image" content="(https:\/\/cdn\.poe2db\.tw\/image\/[^"<>]+)"/.exec(
          html,
        )?.[1]
      if (!image) throw new Error('No source image')
      found = {
        image,
        source: url,
        sourceSha256: createHash('sha256').update(html).digest('hex'),
        retrievedAt: new Date().toISOString(),
      }
    } catch (error) {
      console.log(`${key}: ${String(error)}`)
    }
  }
  if (found)
    rows[base.id] =
      previous[base.id]?.image === found.image
        ? { ...previous[base.id], ...found }
        : found
  else missing.push(key)
}
writeFileSync(
  resolve(destination, 'baseImageSources.json'),
  JSON.stringify(rows, null, 2) + '\n',
)
console.log(JSON.stringify({ sourced: Object.keys(rows).length, missing }))
