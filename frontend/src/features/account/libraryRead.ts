import { accountRequest, accountScope } from './auth'
import { documentKey, readDocuments } from './libraryPlan'
import type { Document } from './libraryPlan'

type Reference = Pick<Document, 'kind' | 'id' | 'version'> & {
  deleted: boolean
}

/** Reuse unchanged documents; bound cold reads by the server's advertised response budget. */
export async function readRemoteLibrary(
  owner: string,
  generation: number,
  references: Reference[],
  baseline: Document[],
  advertisedSize: string | null,
): Promise<Document[]> {
  const cached = new Map(baseline.map((d) => [documentKey(d), d]))
  const documents = new Map<string, Document>()
  const missing: Reference[] = []
  for (const ref of references) {
    const key = documentKey(ref)
    const previous = cached.get(key)
    if (ref.deleted)
      documents.set(key, {
        kind: ref.kind,
        id: ref.id,
        version: ref.version,
        content: null,
      })
    else if (previous?.version === ref.version && previous.content !== null)
      documents.set(key, previous)
    else missing.push(ref)
  }
  // Older servers omit the capability header during a rolling deployment.
  const size = advertisedSize === null ? 1 : Number(advertisedSize)
  if (!Number.isSafeInteger(size) || size < 1 || size > 20)
    throw Error('INVALID_LIBRARY_BATCH_SIZE')
  for (let offset = 0; offset < missing.length; offset += size) {
    if (accountScope() !== owner) throw Error('ACCOUNT_CHANGED')
    const batch = missing.slice(offset, offset + size)
    const response =
      advertisedSize === null
        ? await accountRequest(
            `library/${encodeURIComponent(batch[0]!.kind)}/${encodeURIComponent(batch[0]!.id)}`,
            {},
            owner,
          )
        : await accountRequest(
            'library/read-batch',
            {
              method: 'POST',
              body: JSON.stringify({
                generation,
                references: batch.map(({ kind, id, version }) => ({
                  kind,
                  id,
                  version,
                })),
              }),
            },
            owner,
          )
    const body: unknown = await response.json()
    const received = readDocuments(advertisedSize === null ? [body] : body)
    const expected = new Map(
      batch.map((ref) => [documentKey(ref), ref.version]),
    )
    if (received.length !== batch.length)
      throw Error('INVALID_LIBRARY_DOCUMENT')
    for (const document of received) {
      const key = documentKey(document)
      if (expected.get(key) !== document.version || document.content === null)
        throw Error('INVALID_LIBRARY_DOCUMENT')
      expected.delete(key)
      documents.set(key, document)
    }
    if (expected.size) throw Error('INVALID_LIBRARY_DOCUMENT')
  }
  if (accountScope() !== owner) throw Error('ACCOUNT_CHANGED')
  return references.map((ref) => documents.get(documentKey(ref))!)
}
