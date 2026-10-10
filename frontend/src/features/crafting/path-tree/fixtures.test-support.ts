// Test-only shared contract reader. Never imported by the production entry point.
import fixtures from '../../../../../contracts/crafting-paths-v1/synthetic-fixtures.json'
import type { CreateRequest, GraphPage, JobSnapshot } from './types'
export function fixture<T>(id: string): T {
  const found = fixtures.examples.find((x) => x.id === id)
  if (!found) throw new Error('Missing shared fixture: ' + id)
  return structuredClone(found.value) as T
}
export const requestFixture = () => fixture<CreateRequest>('create')
export const jobFixture = () => fixture<JobSnapshot>('complete-renewal')
export const graphFixture = (id: string) => fixture<GraphPage>(id)
export { fixtures }
