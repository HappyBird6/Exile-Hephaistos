import { expect, it } from 'vitest'
import {
  layoutDepths,
  type BundleEdge,
  type StateBundle,
} from './stateProjection'

it('places the converged goal after the essence-only intermediate and keeps distinct incoming sources', () => {
  // Original attachment: 2 -> 5 is Essence without Chaos; 5 -> 4 is Exalted;
  // 3 -> 4 is Essence retaining Chaos acquired earlier. Node 4 was first discovered at depth 2.
  const bundles = [0, 1, 2, 3, 4, 5].map((id) => ({
    id: `state:${id}`,
    depth: id ? 2 : 0,
    nodes: [],
  })) as StateBundle[]
  const edges = [
    [0, 1],
    [0, 2],
    [1, 3],
    [2, 5],
    [3, 4],
    [5, 4],
  ].map(([from, to], i) => ({
    id: `e${i}`,
    from: `state:${from}`,
    to: `state:${to}`,
    variants: [],
    preferred: [],
  })) as BundleEdge[]
  const before = JSON.stringify({ bundles, edges })
  const depth = layoutDepths(bundles, edges)
  for (const edge of edges)
    expect(depth.get(edge.to)!).toBeGreaterThan(depth.get(edge.from)!)
  expect(JSON.stringify({ bundles, edges })).toBe(before)
  expect(edges).not.toContainEqual(
    expect.objectContaining({ from: 'state:2', to: 'state:4' }),
  )
})

it('keeps cyclic or unreachable-policy intermediate states instead of removing their work', () => {
  const bundles = [0, 1, 2].map((id) => ({
    id: String(id),
    depth: id,
    nodes: [],
  }))
  const edges = [
    [0, 1],
    [1, 2],
    [2, 1],
  ].map(([from, to], i) => ({
    id: String(i),
    from: String(from),
    to: String(to),
    variants: [],
    preferred: [],
  }))
  expect(layoutDepths(bundles, edges).size).toBe(3)
})
