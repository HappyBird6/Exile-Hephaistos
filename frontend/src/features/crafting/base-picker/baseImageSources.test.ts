import { readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { resolve } from 'node:path'
import { expect, it } from 'vitest'
import sources from './baseImageSources.json'
import { baseImageSource } from './baseImageSources'
import { workbenchBaseChoices } from './baseChoices'

it('renders every selectable identity from local art or the explicitly denied Fire Quiver placeholder', () => {
  const missing = workbenchBaseChoices('en').filter(
    (choice) => !baseImageSource(choice.baseItemId),
  )
  expect(missing.map((choice) => choice.name)).toEqual(['Fire Quiver'])
  expect(baseImageSource('Metadata/Items/Quivers/FourQuiver2')).toBeUndefined()
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
