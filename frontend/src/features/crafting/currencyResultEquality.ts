import type { ConcreteItem } from './workbenchApi'

export const sameValues = (
  a: Record<string, number> | undefined,
  b: Record<string, number> | undefined,
) =>
  Boolean(a && b) &&
  Object.keys(a!).length === Object.keys(b!).length &&
  Object.entries(a!).every(
    ([key, value]) => Object.hasOwn(b!, key) && b![key] === value,
  )

export const sameModifiers = (
  a: ConcreteItem['explicits'],
  b: ConcreteItem['explicits'],
) =>
  Array.isArray(a) &&
  Array.isArray(b) &&
  a.length === b.length &&
  a.every(
    (m, i) =>
      m.modifierId === b[i]?.modifierId &&
      Boolean(m.fractured) === Boolean(b[i]?.fractured) &&
      Boolean(m.crafted) === Boolean(b[i]?.crafted) &&
      sameValues(m.values, b[i]?.values),
  )
