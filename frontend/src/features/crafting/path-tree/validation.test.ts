import { describe, expect, it } from 'vitest'
import sharedSchema from '../../../../../contracts/crafting-paths-v1/schema.json'
import generatedSchema from './schema.generated.json'
import {
  fixtures,
  fixture,
  jobFixture,
  requestFixture,
  graphFixture,
} from './fixtures.test-support'
import {
  assertShape,
  validateJob,
  validateRequest,
  percent,
  mergeGraph,
  validateCreated,
} from './validation'
import type { JobSnapshot } from './types'
import { ancestors, stateLayers } from './graph'

describe('frozen path-search contract consumer', () => {
  it('ships exactly the frozen schema, without runtime fixture fallback', () =>
    expect(generatedSchema).toEqual(sharedSchema))
  for (const example of fixtures.examples) {
    it(example.id, () => {
      const verify = () => {
        assertShape(
          example.definition as keyof typeof sharedSchema.definitions,
          example.value,
        )
        if (example.definition === 'jobSnapshot')
          validateJob(example.value as JobSnapshot)
      }
      if (example.id.includes('negative')) expect(verify).toThrow()
      else expect(verify).not.toThrow()
    })
  }
  it('formats enormous exact fractions without rounding up to success', () => {
    const d = 10n ** 500n
    expect(percent({ numerator: String(d - 1n), denominator: String(d) })).toBe(
      '≈99.99999%',
    )
    expect(percent({ numerator: '1', denominator: String(d) })).toBe(
      '<0.00001%',
    )
    expect(percent({ numerator: '1', denominator: '1' })).toBe('100%')
  })
  it('rejects unsafe item rolls and out-of-range observation strings', () => {
    const request = requestFixture()
    request.start.item.explicits[0]!.values['synthetic:value'] =
      Number.MAX_SAFE_INTEGER + 1
    expect(() => validateRequest(request)).toThrow()
    const other = requestFixture()
    other.observations = ['9223372036854775808']
    expect(() => validateRequest(other)).toThrow()
  })
  it('rejects client or provenance mismatches', () => {
    const job = jobFixture()
    job.clientRequestId = 'other'
    expect(() => validateCreated(job, requestFixture())).toThrow()
    const changed = jobFixture()
    changed.provenance.transition.ruleVersion = 'new'
    expect(() => validateCreated(changed, requestFixture())).toThrow()
  })
  it('deduplicates immutable pages and rejects conflicts/stale revisions', () => {
    const first = graphFixture('graph-page1'),
      second = graphFixture('graph-page2')
    const merged = mergeGraph(first, second)
    expect(mergeGraph(merged, second)).toEqual(merged)
    const changed = structuredClone(second)
    changed.revision++
    expect(() => mergeGraph(first, changed)).toThrow()
    const bad = structuredClone(first)
    bad.nodes[0]!.item.itemLevel--
    expect(() => mergeGraph(first, bad)).toThrow()
  })
  it('uses finite state layers for loops and traverses only actual recovery ancestors', () => {
    const graph = jobFixture().graph
    expect(stateLayers(graph, 'synthetic:p1').flat()).toEqual(['s0', 's1'])
    expect(ancestors(graph, 'x0')).toEqual(['s0'])
    expect(ancestors(graph, 'missing')).toEqual([])
    const parent = fixture<JobSnapshot>('recovery-parent')
    expect(parent.graph.nodes.length).toBeGreaterThan(0)
  })
})
