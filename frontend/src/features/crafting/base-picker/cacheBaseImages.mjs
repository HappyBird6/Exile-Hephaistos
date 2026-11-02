/* global fetch, AbortSignal, console */
// Exact source URLs only. Original art copyright: Grinding Gear Games.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createHash } from 'node:crypto'
import { Buffer } from 'node:buffer'
const directory = dirname(fileURLToPath(import.meta.url))
const manifest = resolve(directory, 'baseImageSources.json')
const sources = JSON.parse(readFileSync(manifest, 'utf8'))
const urls = [
  ...new Set(
    Object.values(sources)
      .filter((row) => !row.file && !row.cacheBlocked)
      .map((row) => row.image)
      .filter((url) => url.startsWith('https://cdn.poe2db.tw/image/')),
  ),
]
mkdirSync(resolve(directory, 'images'), { recursive: true })
const cached = new Map()
const failures = []
let index = 0
await Promise.all(
  Array.from({ length: 6 }, async () => {
    while (index < urls.length) {
      const url = urls[index++]
      try {
        const response = await fetch(url, {
          signal: AbortSignal.timeout(25000),
        })
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        const bytes = Buffer.from(await response.arrayBuffer())
        if (
          bytes.toString('ascii', 0, 4) !== 'RIFF' ||
          bytes.toString('ascii', 8, 12) !== 'WEBP'
        )
          throw new Error('Not a WEBP image')
        const sha256 = createHash('sha256').update(bytes).digest('hex')
        const file = `${sha256.slice(0, 20)}.webp`
        writeFileSync(resolve(directory, 'images', file), bytes)
        cached.set(url, { file, sha256, size: bytes.length })
      } catch (error) {
        failures.push({ url, error: String(error) })
      }
    }
  }),
)
for (const row of Object.values(sources)) {
  const image = cached.get(row.image)
  if (image) Object.assign(row, image)
  const failure = failures.find((entry) => entry.url === row.image)
  if (failure?.error.includes('HTTP 403'))
    row.cacheBlocked =
      'HTTP 403: source denied download; placeholder until supplied art is available.'
}
writeFileSync(manifest, JSON.stringify(sources, null, 2) + '\n')
console.log(
  JSON.stringify({ cached: cached.size, total: urls.length, failures }),
)
