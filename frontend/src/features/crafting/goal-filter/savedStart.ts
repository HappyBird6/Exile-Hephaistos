import type { ConcreteItem } from '../workbenchApi'
import type { CreateRequest } from '../path-tree'
import { assertShape } from '../path-tree/validation'
import { hasImplicitGoal, readSupport } from '../method-search/supportStorage'
import { qualityShape } from '../catalystQuality'
export type SavedStart = {
  base: string
  text: string
  item: ConcreteItem | null
  rulesetIdentity: string | null
  abstractQuality40: boolean
  editing: boolean
  started: boolean
  levelEdited: boolean
  searchRequest: CreateRequest | null
}
export function loadStart() {
  return readSupport<SavedStart>('start', (raw): raw is SavedStart => {
    try {
      const v = raw as SavedStart
      if (
        !v ||
        typeof v.base !== 'string' ||
        typeof v.text !== 'string' ||
        v.text.length > 16384 ||
        !(
          v.rulesetIdentity === null || typeof v.rulesetIdentity === 'string'
        ) ||
        !['abstractQuality40', 'editing', 'started', 'levelEdited'].every(
          (k) => typeof v[k as keyof SavedStart] === 'boolean',
        )
      )
        return false
      if (v.item !== null) {
        if (!qualityShape(v.item.catalystQuality)) return false
        const modifier = (m: ConcreteItem['explicits'][number]) => ({
          ...m,
          fractured: m.fractured ?? false,
        })
        assertShape('item', {
          ...v.item,
          implicits: v.item.implicits.map(modifier),
          explicits: v.item.explicits.map(modifier),
          augmentSockets: v.item.augmentSockets ?? null,
          catalystQuality: v.item.catalystQuality ?? null,
        })
      }
      if (v.searchRequest !== null)
        assertShape('createRequest', v.searchRequest)
      return true
    } catch {
      return false
    }
  })
}
export function restoredStart() {
  const saved = loadStart()
  if (!saved) return {}
  return saved.searchRequest && hasImplicitGoal(saved.searchRequest.goal)
    ? { ...saved, searchRequest: null, editing: true }
    : saved
}
