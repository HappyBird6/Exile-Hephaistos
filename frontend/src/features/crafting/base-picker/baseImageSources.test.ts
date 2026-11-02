import { readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { resolve } from 'node:path'
import { expect, it } from 'vitest'
import sources from './baseImageSources.json'
import { baseImageSource } from './baseImageSources'
import { workbenchBaseChoices } from './baseChoices'

it('renders every selectable identity from verified local art including the user-provided Fire Quiver image', () => {
  const missing = workbenchBaseChoices('en').filter(
    (choice) => !baseImageSource(choice.baseItemId),
  )
  expect(missing.map((base) => base.name).sort()).toEqual(
    ['Distorted Amulet', "Falconer's Jacket", 'Flowing Raiment'].sort(),
  )
  for (const base of missing)
    expect(
      (sources as Record<string, { cacheBlocked?: string }>)[base.baseItemId]
        ?.cacheBlocked,
    ).toContain('HTTP 403')
  expect(baseImageSource('Metadata/Items/Quivers/FourQuiver2')).toContain(
    'c8809e1438f9f231be94.webp',
  )
  for (const row of Object.values(sources) as {
    file?: string
    sha256?: string
  }[]) {
    if (!row.file) continue
    const bytes = readFileSync(
      resolve(
        process.cwd(),
        'src/features/crafting/base-picker/images',
        row.file,
      ),
    )
    expect(bytes.toString('ascii', 0, 4)).toBe('RIFF')
    expect(bytes.toString('ascii', 8, 12)).toBe('WEBP')
    expect(createHash('sha256').update(bytes).digest('hex')).toBe(row.sha256)
  }
})
