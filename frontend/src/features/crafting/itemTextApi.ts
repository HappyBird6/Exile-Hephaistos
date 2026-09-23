import type { Item, Modifier, TextLine, RawField } from './itemModels'
export const maxItemTextBytes = 16 * 1024
export class ItemTextApiError extends Error {
  constructor(readonly code: string) {
    super(code)
  }
}
function invalid(): never {
  throw new ItemTextApiError('INVALID_API_RESPONSE')
}
function object(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value))
    return invalid()
  return value as Record<string, unknown>
}
function string(value: unknown): string {
  return typeof value === 'string' ? value : invalid()
}
function integer(value: unknown, min = 0): number {
  return typeof value === 'number' &&
    Number.isSafeInteger(value) &&
    value >= min &&
    value <= 2147483647
    ? value
    : invalid()
}
function array<T>(value: unknown, parse: (entry: unknown) => T): T[] {
  return Array.isArray(value) ? value.map(parse) : invalid()
}
function line(value: unknown): TextLine {
  const data = object(value)
  const kind = data.kind
  if (kind !== 'CONTENT' && kind !== 'SEPARATOR' && kind !== 'BLANK')
    return invalid()
  return {
    number: integer(data.number, 1),
    section: integer(data.section),
    raw: string(data.raw),
    kind,
  }
}
function field(value: unknown): RawField {
  const data = object(value)
  return {
    key: string(data.key),
    value: string(data.value),
    source: line(data.source),
  }
}
function modifier(value: unknown): Modifier {
  const data = object(value)
  const type = data.type
  if (
    type !== 'IMPLICIT' &&
    type !== 'ENCHANT' &&
    type !== 'RUNE' &&
    type !== 'DESECRATED' &&
    type !== 'FRACTURED' &&
    type !== 'CRAFTED' &&
    type !== 'MUTATED' &&
    type !== 'EXPLICIT'
  )
    return invalid()
  if (data.affix !== null && data.affix !== 'PREFIX' && data.affix !== 'SUFFIX')
    return invalid()
  return {
    text: string(data.text),
    type,
    source: line(data.source),
    metadata: data.metadata === null ? null : line(data.metadata),
    affix: data.affix,
    tier: data.tier === null ? null : integer(data.tier),
    affixName: data.affixName === null ? null : string(data.affixName),
  }
}
function parsedItem(value: unknown): Item {
  const data = object(value)
  const text = object(data.text)
  const locale = data.locale
  const rarity = data.rarity
  if (locale !== 'en') return invalid()
  if (
    rarity !== 'NORMAL' &&
    rarity !== 'MAGIC' &&
    rarity !== 'RARE' &&
    rarity !== 'UNIQUE' &&
    rarity !== 'UNKNOWN'
  )
    return invalid()
  return {
    text: {
      originalText: string(text.originalText),
      lines: array(text.lines, line),
    },
    locale,
    itemClass: string(data.itemClass),
    rarity,
    rarityText: string(data.rarityText),
    nameLines: array(data.nameLines, line),
    displayName: string(data.displayName),
    displayBase: data.displayBase === null ? null : string(data.displayBase),
    itemLevel: data.itemLevel === null ? null : integer(data.itemLevel),
    properties: array(data.properties, field),
    requirements: array(data.requirements, field),
    markedModifiers: array(data.markedModifiers, line),
    modifiers: array(data.modifiers, modifier),
    flags: array(data.flags, line),
    unparsedLines: array(data.unparsedLines, line),
    warnings: array(data.warnings, (value) => {
      const warning = object(value)
      return {
        code: string(warning.code),
        lineNumber: integer(warning.lineNumber),
      }
    }),
  }
}

export async function parseItemText(
  text: string,
  signal: AbortSignal,
): Promise<Item> {
  const response = await fetch('/api/v1/items/parse', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ text }),
    credentials: 'same-origin',
    cache: 'no-store',
    redirect: 'error',
    signal,
  })
  if (!response.ok) {
    let code = 'REQUEST_FAILED'
    try {
      const body = object(await response.json())
      if (typeof body.code === 'string') code = body.code
    } catch {
      /* Never render raw server/proxy error messages. */
    }
    throw new ItemTextApiError(code)
  }
  const result = parsedItem(await response.json())
  if (result.text.originalText !== text) return invalid()
  if (signal.aborted) throw new DOMException('Aborted', 'AbortError')
  return result
}
