// User-selected simulator assumption, replaceable after game precision is verified.
// Same half-away-from-zero ratio policy as the compound roll model.
export function roundQualityRatio(
  numerator: bigint,
  denominator: bigint,
): number {
  if (denominator <= 0n) throw new Error('Positive denominator required')
  const sign = numerator < 0n ? -1n : 1n
  const absolute = numerator < 0n ? -numerator : numerator
  return Number(sign * ((absolute + denominator / 2n) / denominator))
}
