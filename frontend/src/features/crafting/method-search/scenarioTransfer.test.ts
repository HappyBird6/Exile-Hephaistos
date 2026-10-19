import { expect, it } from 'vitest'
import { emptyTransfer, transferReducer as reduce } from './scenarioTransfer'
it('never overwrites pasted code with a late calculation or a new automatic result', () => {
  let s = reduce(emptyTransfer, { type: 'begin', owner: 'old' })
  s = reduce(s, { type: 'edit', text: 'user pasted' })
  s = reduce(s, { type: 'begin', owner: 'new' })
  s = reduce(s, {
    type: 'ready',
    owner: 'old',
    code: 'stale',
    status: 'COMPLETE',
  })
  expect(s.generated).toBeNull()
  expect(s.text).toBe('user pasted')
  s = reduce(s, {
    type: 'ready',
    owner: 'new',
    code: 'fresh',
    status: 'PARTIAL',
  })
  expect(s.text).toBe('user pasted')
  expect(s.generated).toBe('fresh')
  s = reduce(s, { type: 'generated' })
  expect(s.text).toBe('fresh')
  expect(s.status).toBe('PARTIAL')
})
it('clears old generated results on new inputs and ignores stale cleanup or completion', () => {
  let s = reduce(emptyTransfer, { type: 'begin', owner: 'a' })
  s = reduce(s, {
    type: 'ready',
    owner: 'a',
    code: 'a-code',
    status: 'COMPLETE',
  })
  s = reduce(s, { type: 'begin', owner: 'b' })
  expect(s.text).toBe('')
  expect(s.pending).toBe(true)
  s = reduce(s, { type: 'clear', owner: 'a' })
  expect(s.owner).toBe('b')
  s = reduce(s, { type: 'clear', owner: 'b' })
  expect(s.text).toBe('')
  s = reduce(s, {
    type: 'ready',
    owner: 'b',
    code: 'late-code',
    status: 'COMPLETE',
  })
  expect(s.text).toBe('')
})
