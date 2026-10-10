import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { MethodGraph } from './MethodGraph'
import { methodMessages } from './methodMessages'
import { jobFixture } from './fixtures.test-support'
import { createItemPresentation } from './presentation'
import { validateJob, percent } from './validation'

const zero = { numerator: '0', denominator: '1' }
function methodJob() {
  const job = jobFixture(),
    r = job.recommendations[0]!
  r.policy.actions = ['CHAOS']
  job.graph.nodes.push({ ...structuredClone(job.graph.nodes[1]!), id: 's2' })
  r.method = {
    fromStateId: 's0',
    stopCondition: 'FIRST_GOAL_OR_OBSERVATION',
    cycleUses: 1,
    proofVersion: 'synthetic:method',
    exits: [
      ...['s1', 's2'].map((stateId) => ({
        stateId,
        kind: 'HIT' as const,
        points: r.points.map((p) => ({
          attempts: p.attempts,
          probability: {
            numerator: p.lower.numerator,
            denominator: String(BigInt(p.lower.denominator) * 2n),
          },
        })),
      })),
      {
        stateId: 's0',
        kind: 'ACTIVE',
        points: r.points.map((p) => ({
          attempts: p.attempts,
          probability: p.active,
        })),
      },
    ],
    omitted: r.points.map((p) => ({
      attempts: p.attempts,
      hit: zero,
      active: zero,
    })),
  }
  return job
}
const present = createItemPresentation(
  { name: 'Amulet', itemClass: 'Amulets' },
  {},
)
beforeEach(() => setLocale('en'))
describe('method state flow', () => {
  it('draws directed branches with state-specific chances and highlights every recommended arrow', () => {
    const job = methodJob(),
      r = job.recommendations[0]!
    const { container, rerender } = render(
      <MethodGraph
        graph={job.graph}
        recommendation={r}
        attempts="100"
        recommended
        recovery={false}
        presentItem={present}
      />,
    )
    expect(
      container.querySelectorAll('path[data-method-edge][marker-end]'),
    ).toHaveLength(3)
    expect(
      container.querySelector('.method-flow--recommended'),
    ).toBeInTheDocument()
    expect(
      screen.getAllByText(percent(r.method!.exits[0]!.points[0]!.probability)),
    ).toHaveLength(2)
    const link = container.querySelector<HTMLAnchorElement>(
      '.method-flow__edge-label',
    )!
    fireEvent.click(link)
    expect(document.activeElement?.id).toBe(link.hash.slice(1))
    expect(
      container.querySelectorAll('.method-flow__actions img'),
    ).toHaveLength(3)
    rerender(
      <MethodGraph
        graph={job.graph}
        recommendation={r}
        attempts="500"
        recommended={false}
        recovery
        presentItem={present}
      />,
    )
    expect(
      container.querySelector('.method-flow--recovery'),
    ).toBeInTheDocument()
    expect(
      container.querySelector('.method-flow--recommended'),
    ).not.toBeInTheDocument()
    expect(
      screen.getByRole('region', { name: methodMessages.en.outcomes }),
    ).toHaveAttribute('tabindex', '0')
  })
  it('preserves probability mass and rejects swapping aggregate chance onto each state edge', () => {
    const job = methodJob()
    expect(() => validateJob(job)).not.toThrow()
    job.recommendations[0]!.method!.exits[0]!.points[0]!.probability =
      job.recommendations[0]!.points[0]!.lower
    expect(() => validateJob(job)).toThrow()
  })
  it('separates omitted mass, unresolved mass and identical state identities in different policies', () => {
    const job = methodJob(),
      r = job.recommendations[0]!
    const removed = r.method!.exits.shift()!
    r.method!.omitted = r.points.map((p, i) => ({
      attempts: p.attempts,
      hit: removed.points[i]!.probability,
      active: zero,
    }))
    expect(() => validateJob(job)).not.toThrow()
    const other = structuredClone(r)
    other.policy.id = 'other-policy'
    other.policy.actions = ['ANNULMENT', 'EXALTED']
    other.method!.cycleUses = 2
    // Shared ItemState IDs do not permit reusing another policy's probability values.
    expect(other.method!.exits[0]!.stateId).toBe(r.method!.exits[0]!.stateId)
    const { container } = render(
      <MethodGraph
        graph={job.graph}
        recommendation={other}
        attempts="100"
        recommended={false}
        recovery={false}
        presentItem={present}
      />,
    )
    expect(
      container.querySelectorAll('.method-flow__actions img'),
    ).toHaveLength(4)
    expect(
      screen.getByText(methodMessages.en.omitted + ' · Goal reached'),
    ).toBeInTheDocument()
  })
  it('does not draw zero-probability branches', () => {
    const job = methodJob(),
      r = job.recommendations[0]!
    r.method!.exits[0]!.points[0]!.probability = zero
    const { container } = render(
      <MethodGraph
        graph={job.graph}
        recommendation={r}
        attempts="100"
        recommended={false}
        recovery={false}
        presentItem={present}
      />,
    )
    expect(container.querySelectorAll('path[data-method-edge]')).toHaveLength(2)
  })
  for (const locale of locales)
    it(`renders method meanings in ${locale}`, () => {
      setLocale(locale)
      const job = methodJob()
      render(
        <MethodGraph
          graph={job.graph}
          recommendation={job.recommendations[0]!}
          attempts="100"
          recommended={false}
          recovery={false}
          presentItem={present}
        />,
      )
      expect(
        screen.getByText(methodMessages[locale].single),
      ).toBeInTheDocument()
    })
})
