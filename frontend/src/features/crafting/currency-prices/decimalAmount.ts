// API quotes and expected counts are decimal numbers. Accumulate their decimal
// digits exactly so a floating-point carry cannot overcharge a ceiling boundary.
function parts(value: number) {
  const [mantissa, exponent = '0'] = String(value).split('e')
  const [whole, decimal = ''] = mantissa!.split('.')
  return {
    units: BigInt(whole! + decimal),
    places: decimal.length - Number(exponent),
  }
}
export function decimalFraction(value: number) {
  const { units, places } = parts(value)
  return places < 0
    ? { numerator: units * 10n ** BigInt(-places), denominator: 1n }
    : { numerator: units, denominator: 10n ** BigInt(places) }
}
export function costAccumulator() {
  let units = 0n,
    places = 0
  return {
    add(price: number, count: number) {
      const a = parts(price),
        b = parts(count)
      const productPlaces = a.places + b.places
      const next = Math.max(places, productPlaces)
      units =
        units * 10n ** BigInt(next - places) +
        a.units * b.units * 10n ** BigInt(next - productPlaces)
      places = next
    },
    total: () => Number(units + 'e-' + places),
  }
}
