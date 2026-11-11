import { sanctificationFactor } from './sanctification'
import { roundQualityRatio } from '../qualityRoundingPolicy'
import { modifierMagnitudeRatio } from '../modifierMagnitudes'
import type { Definition } from '../craftingApi'
import type { ConcreteItem } from '../workbenchApi'
import { catalystProjection, catalystTypes } from '../catalystQuality'
import { jewelEffect } from '../basicJewel'

/** Sanctification rounds first, then typed quality rounds; later effects keep their existing ratios. */
export function preciseModifierProjection(
  state: ConcreteItem,
  definition: Definition,
  modifier: ConcreteItem['explicits'][number],
  layer: 'implicits' | 'explicits' = 'explicits',
) {
  if (!Object.values(modifier.values).every(Number.isSafeInteger))
    throw new Error('Original integer source rolls are required')
  // Reuse reviewed eligibility without modifying the canonical rolls.
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
  const magnitude = modifierMagnitudeRatio(state, definition, layer)
  const ratios: Record<string, { numerator: bigint; denominator: bigint }> = {}
  const values: Record<string, number> = {}
  for (const [id, source] of Object.entries(modifier.values)) {
    const sanctified = roundQualityRatio(
      BigInt(source) * BigInt(sanctificationFactor(state, modifier, id, layer)),
      100n,
    )
    const qualityValue = roundQualityRatio(
      BigInt(sanctified) * BigInt(100 + quality),
      100n,
    )
    const numerator =
      BigInt(qualityValue) * BigInt(100 + effect) * magnitude.numerator
    const denominator = 100n * magnitude.denominator
    const displayed = roundQualityRatio(numerator, denominator)
    ratios[id] = { numerator: BigInt(displayed), denominator: 1n }
    values[id] = displayed
  }
  return { values, ratios, qualityStatus: eligibility.status }
}
