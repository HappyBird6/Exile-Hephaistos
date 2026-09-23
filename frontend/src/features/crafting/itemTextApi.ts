// Manual DTO mirror of docs/openapi-item.yaml, shared by API validation and UI.
export interface TextLine {
  number: number
  section: number
  raw: string
  kind: 'CONTENT' | 'SEPARATOR' | 'BLANK'
}
export interface RawField {
  key: string
  value: string
  source: TextLine
}
export interface ParsedItemText {
  text: { originalText: string; lines: TextLine[] }
  locale: 'en' | 'ko'
  itemClass: string
  rarity: 'NORMAL' | 'MAGIC' | 'RARE' | 'UNIQUE' | 'UNKNOWN'
  rarityText: string
  nameLines: TextLine[]
  displayName: string
  displayBase: string | null
  itemLevel: number | null
  properties: RawField[]
  requirements: RawField[]
  markedModifiers: TextLine[]
  flags: TextLine[]
  unparsedLines: TextLine[]
  warnings: { code: string; lineNumber: number }[]
}

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
function parsedItem(value: unknown): ParsedItemText {
  const data = object(value)
  const text = object(data.text)
  const locale = data.locale
  const rarity = data.rarity
  if (locale !== 'en' && locale !== 'ko') return invalid()
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
): Promise<ParsedItemText> {
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
