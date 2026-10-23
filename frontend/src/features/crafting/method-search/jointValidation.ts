import { sumBounds } from './outcomeGrouping'
import type { AutomaticNode, AutomaticView } from './automaticApi'
import { assertShape } from '../path-tree/validation'
import { validateTierLaws } from './automaticTierLaws'
import type { Bounds, RollBands } from './types'

const object = (v: unknown) => !!v && typeof v === 'object' && !Array.isArray(v)
const id = (v: unknown) =>
  typeof v === 'string' && v.length > 0 && v.length <= 16384
const index = (v: unknown) => Number.isSafeInteger(v) && Number(v) >= 0
export function validateRollBands(
  item: AutomaticNode['item'],
  rolls: RollBands,
) {
  if (!object(rolls)) throw new Error('Invalid roll law')
  for (const [key, b] of Object.entries(rolls))
    if (
      !object(b) ||
      !id(b.stat) ||
      !Number.isSafeInteger(b.min) ||
      !Number.isSafeInteger(b.max) ||
      b.min > b.max ||
      !item.explicits.some(
        (m) => m.modifierId === key && m.values[b.stat] === b.min,
      )
    )
      throw new Error('Invalid roll law')
}
export function validateJointNode(n: AutomaticNode) {
  if (n.attributes !== undefined) {
    if (!object(n.attributes)) throw new Error('Invalid attribute support')
    for (const [key, options] of Object.entries(n.attributes)) {
      if (
        !n.item.explicits.some((m) => m.modifierId === key) ||
        !Array.isArray(options) ||
        !options.length ||
        options.length > 50000
      )
        throw new Error('Invalid attribute support')
      for (const v of options)
        if (
          !object(v) ||
          !id(v.modifierId) ||
          !id(v.stat) ||
          !Number.isSafeInteger(v.min) ||
          !Number.isSafeInteger(v.max) ||
          v.min > v.max ||
          !index(v.tier) ||
          v.tier < 1 ||
          !id(v.role)
        )
          throw new Error('Invalid attribute option')
    }
  }
  if (n.members !== undefined) {
    if (!Array.isArray(n.members) || n.members.length > 50000)
      throw new Error('Invalid joint members')
    for (const m of n.members) {
      if (!object(m)) throw new Error('Invalid joint member')
      assertShape('item', m.item)
      validateTierLaws({ ...n, item: m.item, tiers: m.tiers })
      validateRollBands(m.item, m.rolls)
    }
  }
  if (n.joint !== undefined) {
    if (!Array.isArray(n.joint) || n.joint.length > 50000)
      throw new Error('Invalid joint origins')
    for (const o of n.joint)
      if (
        !object(o) ||
        !id(o.method) ||
        !index(o.source) ||
        !['GOAL', 'CHECKPOINT', 'UNRESOLVED', 'ACTIVE'].includes(o.kind) ||
        !object(o.retained) ||
        Object.entries(o.retained).some(
          ([key, value]) =>
            !n.item.explicits.some((m) => m.modifierId === key) || !id(value),
        )
      )
        throw new Error('Invalid joint origin')
  }
}
const rationalEqual = (
  a: { numerator: string; denominator: string },
  b: { numerator: string; denominator: string },
) =>
  BigInt(a.numerator) * BigInt(b.denominator) ===
  BigInt(b.numerator) * BigInt(a.denominator)
const probabilityClamp = (f: { numerator: string; denominator: string }) =>
  BigInt(f.numerator) > BigInt(f.denominator)
    ? { numerator: '1', denominator: '1' }
    : f
export function validateJointGraph(
  v: AutomaticView,
  bound: (b: Bounds) => void,
) {
  const nodes = new Map(v.nodes.map((n) => [n.id, n])),
    methods = new Map(v.methods.map((m) => [m.id, m]))
  for (const n of v.nodes) {
    validateJointNode(n)
    for (const origin of n.joint ?? []) {
      const source = nodes.get(origin.source),
        method = methods.get(origin.method)
      if (!source || !method || method.source !== source.id)
        throw new Error('Unknown joint source')
      for (const [key, value] of Object.entries(origin.retained)) {
        if (!source.item.explicits.some((m) => m.modifierId === value))
          throw new Error('Unknown retained source field')
        const from = source.attributes?.[value]?.[0]?.role,
          to = n.attributes?.[key]?.[0]?.role
        if (from && to && from !== to)
          throw new Error('Incompatible joint source projection')
      }
    }
  }
  for (const method of v.methods)
    for (const point of method.points)
      for (const o of point.outcomes) {
        if (o.conditional === undefined) continue
        if (!Array.isArray(o.conditional) || o.conditional.length > 50000)
          throw new Error('Invalid conditional law')
        const source = nodes.get(method.source)!
        for (const c of o.conditional) {
          if (
            !object(c) ||
            !Array.isArray(c.local) ||
            c.local.length > 4 ||
            !Array.isArray(c.retainedFromSource) ||
            new Set(c.retainedFromSource).size !==
              c.retainedFromSource.length ||
            c.retainedFromSource.some(
              (id) =>
                !source.item.explicits.some((m) => m.modifierId === id) ||
                c.local.some((m) => m.modifierId === id),
            )
          )
            throw new Error('Invalid conditional source projection')
          const item = { ...source.item, explicits: c.local }
          assertShape('item', item)
          validateTierLaws({ ...source, item, tiers: c.tiers })
          validateRollBands(item, c.rolls)
          bound(c.probability)
        }
        if (o.conditional.length) {
          const total = sumBounds(o.conditional.map((c) => c.probability))
          if (
            !rationalEqual(
              probabilityClamp(total.lower),
              o.probability.lower,
            ) ||
            !rationalEqual(probabilityClamp(total.upper), o.probability.upper)
          )
            throw new Error('Conditional probability mass differs from outcome')
        } else if (o.target !== null && nodes.get(o.target)?.members?.length)
          throw new Error('Missing conditional law')
      }
}
