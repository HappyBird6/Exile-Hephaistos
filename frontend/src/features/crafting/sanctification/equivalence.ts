/** Compare wire values independently of JSON object-key order. */
export function equivalent(a: unknown, b: unknown): boolean {
  if (a === b) return true
  if (Array.isArray(a) || Array.isArray(b))
    return (
      Array.isArray(a) &&
      Array.isArray(b) &&
      a.length === b.length &&
      a.every((v, i) => equivalent(v, b[i]))
    )
  if (!a || !b || typeof a !== 'object' || typeof b !== 'object') return false
  const x = a as Record<string, unknown>,
    y = b as Record<string, unknown>
  return (
    Object.keys(x).length === Object.keys(y).length &&
    Object.entries(x).every(
      ([key, value]) => Object.hasOwn(y, key) && equivalent(value, y[key]),
    )
  )
}

/** Legacy records may omit a false fractured flag; both encode the same original roll. */
export function sameRolls(
  a: readonly {
    modifierId: string
    values: Record<string, number>
    fractured?: boolean
    crafted?: boolean
  }[],
  b: readonly {
    modifierId: string
    values: Record<string, number>
    fractured?: boolean
    crafted?: boolean
  }[],
): boolean {
  return (
    a.length === b.length &&
    a.every(
      (m, index) =>
        m.modifierId === b[index]?.modifierId &&
        Boolean(m.fractured) === Boolean(b[index]?.fractured) &&
        Boolean(m.crafted) === Boolean(b[index]?.crafted) &&
        equivalent(m.values, b[index]?.values),
    )
  )
}
