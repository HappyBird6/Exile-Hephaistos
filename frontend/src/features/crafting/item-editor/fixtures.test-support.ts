import type { Item } from '../itemModels'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'

export function parsedTextFixture(text: string): Item {
  let section = 0
  const lines = text.split(/\r\n|\r|\n/).map((raw, index) => {
    const kind = /^-{8,}$/.test(raw)
      ? ('SEPARATOR' as const)
      : raw.trim()
        ? ('CONTENT' as const)
        : ('BLANK' as const)
    const line = { number: index + 1, section, raw, kind }
    if (kind === 'SEPARATOR') section++
    return line
  })
  const itemLevel = Number(text.match(/Item Level: (\d+)/)?.[1] ?? 82)
  const rarity = text.includes('Rarity: Rare')
    ? 'RARE'
    : text.includes('Rarity: Magic')
      ? 'MAGIC'
      : 'NORMAL'
  return {
    text: { originalText: text, lines },
    locale: 'en',
    itemClass: 'Amulets',
    rarity,
    rarityText: rarity[0] + rarity.slice(1).toLowerCase(),
    nameLines: lines.filter((line) => line.raw === 'Solar Amulet'),
    displayName: 'Solar Amulet',
    displayBase: 'Solar Amulet',
    itemLevel,
    properties: [],
    requirements: [],
    modifiers: [],
    markedModifiers: [],
    flags: [],
    unparsedLines: [],
    warnings: [],
  }
}
export const editorFixtureFetch: typeof fetch = (input, init) => {
  const url = String(input)
  if (url.endsWith('/prepare-text')) {
    const { text } = JSON.parse(String(init?.body)) as { text: string }
    return Promise.resolve(
      jsonResponse({
        parsed: parsedTextFixture(text),
        mapping: {
          mapped: true,
          state: {
            ...concreteInitial(initialFixture),
            itemLevel: Number(text.match(/Item Level: (\d+)/)?.[1] ?? 82),
          },
          issues: [],
        },
      }),
    )
  }
  if (url.endsWith('/api/v1/items/parse')) {
    const { text } = JSON.parse(String(init?.body)) as { text: string }
    return Promise.resolve(jsonResponse(parsedTextFixture(text)))
  }
  if (url.endsWith('/edit-text') || url.endsWith('/map-text')) {
    const { text } = JSON.parse(String(init?.body)) as { text: string }
    return Promise.resolve(
      jsonResponse({
        mapped: true,
        state: {
          ...concreteInitial(initialFixture),
          itemLevel: Number(text.match(/Item Level: (\d+)/)?.[1] ?? 82),
        },
        issues: [],
      }),
    )
  }
  return fixtureFetch(input, init)
}

export function preparedResponse(
  text: string,
  mapping: unknown,
  parsed: unknown = parsedTextFixture(text),
) {
  return jsonResponse({ parsed, mapping })
}
