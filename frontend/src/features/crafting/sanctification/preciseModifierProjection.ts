import { modifierMagnitudeRatio } from '../modifierMagnitudes'
import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { catalystProjection, catalystTypes } from '../catalystQuality'
import { jewelEffect } from '../basicJewel'

/** Display composition from original integers. Never feed projected decimals to catalystProjection. */
export function preciseModifierProjection(
  state: ConcreteItem,
  definition: Definition,
  modifier: ConcreteItem['explicits'][number],
  layer: 'implicits' | 'explicits' = 'explicits',
) {
  if (!Object.values(modifier.values).every(Number.isSafeInteger))
    throw new Error('Original integer source rolls are required')
  // Reuse reviewed eligibility; deliberately discard its rounded display values.
  const eligibility = catalystProjection(
    definition,
    modifier.values,
    state.catalystQuality,
    state,
  )
  const scales =
    eligibility.status === 'SCALED_INTEGER' ||
    eligibility.status === 'JEWEL_EFFECT_AND_QUALITY_PROVISIONAL'
  const quality =
    scales &&
    state.catalystQuality &&
    catalystTypes[state.catalystQuality.type].tags.some((tag) =>
      definition.tags?.includes(tag),
    )
      ? state.catalystQuality.amount
      : 0
  const effect =
    scales && /^(ruby|emerald|sapphire|diamond):/.test(definition.id)
      ? jewelEffect(state, definition.affixType)
      : 0
  const factors = state.sanctification?.[layer][modifier.modifierId] ?? {}
  const magnitude = modifierMagnitudeRatio(state, definition, layer)
  const ratios: Record<string, { numerator: bigint; denominator: bigint }> = {}
  const values: Record<string, number> = {}
  for (const [id, source] of Object.entries(modifier.values)) {
    const numerator =
      BigInt(source) *
      BigInt(100 + quality) *
      BigInt(100 + effect) *
      BigInt(factors[id] ?? 100) *
      magnitude.numerator
    const denominator = 1_000_000n * magnitude.denominator
    ratios[id] = { numerator, denominator }
    values[id] = Number(numerator) / Number(denominator)
  }
  return { values, ratios, qualityStatus: eligibility.status }
}
