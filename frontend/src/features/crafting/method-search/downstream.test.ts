import { expect, it } from 'vitest'
import { downstream } from './downstream'
it('follows branches, merges and cycles without including unrelated incoming paths', () => {
  const edges = [
    ['parent', 'root', 'a'],
    ['sibling', 'root', 'b'],
    ['left', 'a', 'c'],
    ['right', 'a', 'd'],
    ['merge', 'c', 'd'],
    ['unrelated', 'b', 'd'],
    ['cycle', 'd', 'a'],
    ['self', 'd', 'd'],
  ].map(([id, from, to]) => ({ id: id!, from: from!, to: to! }))
  const result = downstream('a', edges)
  expect([...result.nodes].sort()).toEqual(['a', 'c', 'd'])
  expect([...result.links].sort()).toEqual([
    'cycle',
    'left',
    'merge',
    'right',
    'self',
  ])
  expect(downstream(null, edges).nodes.size).toBe(0)
  expect(downstream('leaf', edges).links.size).toBe(0)
})
