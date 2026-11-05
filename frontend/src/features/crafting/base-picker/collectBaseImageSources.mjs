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
const evidence = resolve(root, 'docs/evidence/0.5.5')
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
  const names = slugs.map((name) =>
    name
      .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-'),
  )
  const candidates = files.filter((file) =>
    names.some(
      (name) =>
        file.endsWith(`-${name}.en.html`) ||
        file.endsWith(`-${name}.en.json`) ||
        file.endsWith(`/${name}.en.json`),
    ),
  )
  const priorSource = previous[base.id]?.source
  if (
    priorSource?.startsWith('docs/evidence/0.5.5/') &&
    existsSync(resolve(root, priorSource))
  ) {
    candidates.unshift(resolve(root, priorSource))
  }

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
if (process.argv.includes('--check')) {
  for (const [id, row] of Object.entries(rows)) {
    if (JSON.stringify(row) !== JSON.stringify(previous[id]))
      throw new Error(`Image source index is stale: ${id}`)
  }
  if (missing.length)
    throw new Error(`Missing source identities: ${missing.join(', ')}`)
} else {
  writeFileSync(
    resolve(destination, 'baseImageSources.json'),
    JSON.stringify(rows, null, 2) + '\n',
  )
}
console.log(JSON.stringify({ sourced: Object.keys(rows).length, missing }))
