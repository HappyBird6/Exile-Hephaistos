import type { AutomaticView } from './automaticApi'
export function validateSearchResources(
  value: unknown,
  states: number,
  methods: number,
): asserts value is NonNullable<AutomaticView['resources']> {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Invalid resource accounting')
  const r = value as Record<string, unknown>
  const numbers = [
    'computationStates',
    'stateLimit',
    'methodExecutions',
    'methodLimit',
    'concreteStates',
    'concreteMethods',
    'retainedRecords',
    'retainedLimit',
    'primitiveOutcomeBudget',
  ]
  if (
    Object.keys(r).some((k) => !numbers.includes(k) && k !== 'blockers') ||
    numbers.some((k) => !Number.isSafeInteger(r[k]) || (r[k] as number) < 0) ||
    r.concreteStates !== states ||
    r.concreteMethods !== methods ||
    (r.computationStates as number) > (r.stateLimit as number) ||
    (r.methodExecutions as number) > (r.methodLimit as number) ||
    (r.stateLimit as number) < 1 ||
    (r.stateLimit as number) > 128 ||
    (r.methodLimit as number) < 1 ||
    (r.methodLimit as number) > 256 ||
    (r.retainedLimit as number) < 1 ||
    (r.retainedLimit as number) > 50000 ||
    (r.primitiveOutcomeBudget as number) < 1 ||
    (r.primitiveOutcomeBudget as number) > 10000 ||
    (r.retainedRecords as number) < states + methods ||
    !Array.isArray(r.blockers) ||
    r.blockers.length > 4 ||
    r.blockers.some(
      (b) =>
        ![
          'STATES',
          'METHODS',
          'RETAINED_RECORDS',
          'PRIMITIVE_OUTCOMES',
        ].includes(b),
    )
  )
    throw new Error('Invalid resource accounting')
}
