import fs from 'node:fs'
import assert from 'node:assert/strict'
const initials = JSON.parse(fs.readFileSync('/qa/api-attempt-3/api-initials.json'))
const captures = []
for (const [key, initial] of Object.entries(initials).filter(([k]) => k.endsWith('-belt'))) {
  const before = { ...initial.state, explicits: [], augmentSockets: null, catalystQuality: null }; delete before.modifierIds
  const response = await fetch('http://app:8080/api/v1/crafting/workbench/apply', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ state: before, action: 'TRANSMUTATION', activeOmens: [] }) })
  assert.equal(response.status, 200)
  const result = await response.json(); assert(result.applied && result.qualityLimit === null)
  const ids = new Set([...before.implicits, ...result.state.explicits].map(m => m.modifierId))
  captures.push({ key, before, result, definitions: Object.fromEntries([...ids].map(id => [id, initial.modifiers[id]])) })
}
fs.writeFileSync('/qa/reviewed-belts-contract.json', JSON.stringify({ captures }, null, 2) + '\n', { flag: 'wx' })
