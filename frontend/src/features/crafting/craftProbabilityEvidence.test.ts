import { expect, it } from 'vitest'
import actual from '../../shared/test/wand-essence-responses.json'
import { craftProbabilityEvidence } from './craftProbabilityEvidence'
import type { AppliedItem } from './workbenchApi'

it.each(actual.captures)(
  'does not describe actual $action guaranteed targets as weighted draws',
  ({ result }) => {
    const explanation = craftProbabilityEvidence(result as AppliedItem)
    expect(explanation.weighted).toBe(false)
    expect(explanation.text).toContain('verified eligible targets')
  },
)

it.each([
  ['TRANSMUTATION', 'ADD', true, 'not verified game odds'],
  ['RUNIC_ALLOY', 'ADD', false, 'verified eligible targets'],
  ['DIVINE', 'REROLL_EXPLICIT', false, 'Numeric values rerolled'],
  ['ANNULMENT', 'REMOVE', false, 'uniform assumption'],
  ['FRACTURING', 'FRACTURE', false, 'Operation conditions'],
] as const)(
  'explains %s using its actual selection basis',
  (action, kind, weighted, text) => {
    const explanation = craftProbabilityEvidence({
      action,
      events: [{ kind, modifierId: 'p', values: {}, selectionProbability: 1 }],
    })
    expect(explanation.weighted).toBe(weighted)
    expect(explanation.text).toContain(text)
  },
)
