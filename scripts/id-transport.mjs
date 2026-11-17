// Build transport-only dictionaries from already allocated IDs; never allocate or rewrite IDs here.
import assert from 'node:assert/strict'
import crypto from 'node:crypto'

const included = new Set([
  'bases', 'modifiers', 'stats', 'families', 'tags', 'actions', 'augments', 'itemClasses', 'pseudos',
])
export function idTransport(mappings) {
  const domains = {}
  for (const mapping of mappings.filter(row => included.has(row.domain)).sort((a, b) => a.domain < b.domain ? -1 : 1)) {
    const entries = {}
    for (const row of [...mapping.entries].sort((a, b) => a.id < b.id ? -1 : 1)) {
      assert(!Object.hasOwn(entries, row.id), `Duplicate transport ID: ${row.id}`)
      assert(!/[\0\r\n]/.test(row.sourceKey), 'Invalid transport source key')
      entries[row.id] = row.sourceKey // Retired IDs remain decodable for historical input.
    }
    domains[mapping.domain] = entries
  }
  assert.equal(Object.keys(domains).length, included.size, 'Transport mapping domain missing')
  const canonical = Object.entries(domains).flatMap(([domain, entries]) =>
    Object.entries(entries).map(([id, source]) => `${domain}\0${id}\0${source}\n`)).join('')
  return { version: 1, identity: 'game-ids-v1-' + crypto.createHash('sha256').update(canonical).digest('hex'), domains }
}
