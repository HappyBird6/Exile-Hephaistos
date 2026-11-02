export interface ItemObservations {
  socketCount: number | null
  augmentEffects: string[]
  grantedSkills: { name: string; level: number | null }[]
}
export function observationsShape(
  value: unknown,
): value is ItemObservations | null | undefined {
  if (value == null) return true
  if (typeof value !== 'object' || Array.isArray(value)) return false
  const v = value as Record<string, unknown>
  return (
    Object.keys(v).length === 3 &&
    (v.socketCount === null ||
      (Number.isInteger(v.socketCount) &&
        Number(v.socketCount) >= 0 &&
        Number(v.socketCount) <= 2147483647)) &&
    Array.isArray(v.augmentEffects) &&
    v.augmentEffects.length <= 128 &&
    v.augmentEffects.every(
      (x) => typeof x === 'string' && x.trim().length > 0 && x.length <= 4096,
    ) &&
    Array.isArray(v.grantedSkills) &&
    v.grantedSkills.length <= 16 &&
    v.grantedSkills.every(
      (x) =>
        x &&
        typeof x === 'object' &&
        !Array.isArray(x) &&
        Object.keys(x).length === 2 &&
        typeof x.name === 'string' &&
        x.name.trim().length > 0 &&
        x.name.length <= 256 &&
        (x.level === null ||
          (Number.isInteger(x.level) && x.level >= 1 && x.level <= 2147483647)),
    ) &&
    new Set(v.grantedSkills.map((x) => x.name)).size === v.grantedSkills.length
  )
}
export function sameObservations(a: unknown, b: unknown): boolean {
  if (!observationsShape(a) || !observationsShape(b)) return false
  const canonical = (v: ItemObservations | null | undefined) =>
    v == null
      ? null
      : [
          v.socketCount,
          v.augmentEffects,
          v.grantedSkills.map((s) => [s.name, s.level]),
        ]
  return JSON.stringify(canonical(a)) === JSON.stringify(canonical(b))
}
