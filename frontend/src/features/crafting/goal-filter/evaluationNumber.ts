/** A repeating decimal is presentation only; expose its exact source conversion. */
export function evaluationNumber(
  value: number | null,
  exact?: { numerator: string; denominator: string },
) {
  if (value === null) return '—'
  return exact ? `${exact.numerator}/${exact.denominator} ≈ ${value}` : value
}
