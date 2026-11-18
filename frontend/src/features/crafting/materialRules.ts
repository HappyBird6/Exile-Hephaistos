import { materialRows } from './catalogData'
import basePolicies from './basePolicies.json'
import type { Definition } from './craftingApi'
import type { RollAssumption } from './workbenchApi'
import { equivalent } from './sanctification/equivalence'

type MaterialCatalog = (typeof materialRows)[string]
type MaterialDetail = MaterialCatalog['details'][string]
const legacyBases: Record<string, { id: string }> = basePolicies.legacy

export function materialCatalogFor(base: string) {
  return materialRows[legacyBases[base]?.id ?? base]
}

/** 제작 재료 전용 옵션은 초기 카탈로그의 수치와 출처도 생성된 정의와 대조한다. */
export function materialTargetMatches(
  definition: Definition | undefined,
  expected: MaterialDetail | undefined,
) {
  if (!definition) return false
  if (!expected) return true
  return (
    equivalent(definition.stats, expected.stats) &&
    (definition.sourceUrl == null
      ? !definition.id.startsWith('equipment:')
      : definition.sourceUrl === expected.sourceUrl)
  )
}

/** 단일 수치의 후보 개수·범위·출처를 검증한다. 복합 수치는 coupledModelsMatch에서 검증한다. */
export function materialRollMatches(
  definition: Definition,
  assumptions: RollAssumption[],
  catalog: MaterialCatalog,
) {
  const source =
    catalog.details[definition.id]?.sourceUrl ?? definition.sourceUrl
  if (!source || definition.stats?.length !== 1) return true
  const range = definition.stats[0]!
  const models = assumptions.filter(
    (a) =>
      a.id === 'assumed-source-integer-roll-v1' ||
      a.id === 'uniform-integer-roll-v1',
  )
  if (range.min === range.max) return models.length === 0
  const model = models[0]
  return (
    models.length === 1 &&
    !!model &&
    model.id ===
      (catalog.coupled
        ? 'assumed-source-integer-roll-v1'
        : 'uniform-integer-roll-v1') &&
    model.candidateUnit === range.id &&
    model.min === range.min &&
    model.max === range.max &&
    model.n === range.max - range.min + 1 &&
    model.candidates.length === 0 &&
    model.sourceUrl === source &&
    (!catalog.coupled || model.reason.includes('UNVERIFIED'))
  )
}
