import { describe, it, expect } from 'vitest'
import { twoSteps } from '../session-notation/fixtures.test-support'
import { appendDocument, decodeDocument, documentFor } from './transfer'
import { emptyFilms } from '../workbenchHistory'

describe('portable session documents', () => {
  it('preserves legacy identities and refuses an append over the existing storage budget', async () => {
    const { history } = await twoSteps()
    const legacy = structuredClone(history.films[0]!)
    delete legacy.rulesetIdentity
    legacy.frames.forEach((frame) => {
      delete frame.rulesetIdentity
      if (frame.evidence)
        delete (frame.evidence as Partial<typeof frame.evidence>)
          .rulesetIdentity
    })
    expect(
      appendDocument(
        emptyFilms(),
        decodeDocument(JSON.stringify(documentFor(legacy))),
      ).history.films[0],
    ).toEqual(legacy)
    Object.assign(history.films[0]!, { future: 'x'.repeat(990_000) })
    const before = JSON.stringify(history)
    expect(() =>
      appendDocument(
        history,
        documentFor({ ...legacy, id: 'additional', title: 'x'.repeat(20_000) }),
      ),
    ).toThrow()
    expect(JSON.stringify(history)).toBe(before)
  })
  it('round trips exact original text, unknown fields, frames and receipts', async () => {
    const { history } = await twoSteps()
    const film = { ...history.films[0]!, future: { opaque: ['retain', null] } }
    const decoded = decodeDocument(JSON.stringify(documentFor(film)))
    expect(decoded.film).toEqual(film)
    const added = appendDocument(emptyFilms(), decoded)
    expect(added.history.films).toEqual([film])
    expect(added.history.active).toBeNull()
    expect(appendDocument(added.history, decoded).history).toBe(added.history)
  })
  it('adds collisions without replacing an existing session or selection', async () => {
    const { history } = await twoSteps()
    const original = structuredClone(history)
    const document = documentFor({ ...history.films[0]!, title: 'different' })
    const added = appendDocument(history, document, () => 'new-id')
    expect(added.id).toBe('new-id')
    expect(added.history.films[0]).toEqual(original.films[0])
    expect(added.history.active).toBe(original.active)
    expect(history).toEqual(original)
    expect(appendDocument(added.history, document).history).toBe(added.history)
  })
  it('rejects future schema, unsafe keys, malformed frames, overlarge and deep files', async () => {
    const { history } = await twoSteps()
    const d = documentFor(history.films[0]!)
    expect(() => decodeDocument(JSON.stringify({ ...d, version: 2 }))).toThrow()
    expect(() => decodeDocument('{"__proto__":{}}')).toThrow()
    expect(() =>
      decodeDocument(
        JSON.stringify({ ...d, film: { ...d.film, frames: [null] } }),
      ),
    ).toThrow()
    expect(() => decodeDocument(' '.repeat(2000001))).toThrow()
    expect(() =>
      decodeDocument('['.repeat(34) + '0' + ']'.repeat(34)),
    ).toThrow()
  })
})
