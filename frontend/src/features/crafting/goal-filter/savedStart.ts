import type { ConcreteItem } from '../workbenchApi'
import type { CreateRequest } from '../path-tree'
import { assertShape } from '../path-tree/validation'
import { hasImplicitGoal, readSupport } from '../method-search/supportStorage'
import { hasActiveTotalGoal } from './types'
import { qualityShape } from '../catalystQuality'
import { baseIdForKey } from '../baseRegistry'
import { isFlatRouteGoal, isOrdinaryCatalyst } from './testRouteGoal'
export type SavedStart = {
  testQualityChoice?: string | null
  base: string
  text: string
  item: ConcreteItem | null
  rulesetIdentity: string | null
  abstractQuality40: boolean
  abstractQuality?: number
  editing: boolean
  started: boolean
  levelEdited: boolean
  searchRequest: CreateRequest | null
}
export function loadStart(key = 'start') {
  return readSupport<SavedStart>(key, (raw): raw is SavedStart => {
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
      if (
        v.abstractQuality !== undefined &&
        (!Number.isInteger(v.abstractQuality) ||
          v.abstractQuality < 0 ||
          v.abstractQuality > 40)
      )
        return false
      if (
        key === 'support-test-start' &&
        v.testQualityChoice != null &&
        v.testQualityChoice !== '' &&
        !isOrdinaryCatalyst(v.testQualityChoice)
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
      if (v.searchRequest !== null) {
        if (key === 'support-test-start') {
          const { qualityCatalyst, ...request } = v.searchRequest
          if (
            qualityCatalyst !== undefined &&
            (!isOrdinaryCatalyst(qualityCatalyst) ||
              request.goal.general.quality !== 40)
          )
            return false
          assertShape('createRequest', request)
        } else assertShape('createRequest', v.searchRequest)
      }
      return true
    } catch {
      return false
    }
  })
}
export function restoredStart(key = 'start') {
  const stored = loadStart(key)
  const saved = stored
    ? {
        ...stored,
        ...(key === 'support-test-start'
          ? {
              testQualityChoice:
                stored.testQualityChoice !== undefined
                  ? stored.testQualityChoice
                  : stored.searchRequest?.goal.general.quality === 40
                    ? (stored.searchRequest.qualityCatalyst ?? '')
                    : null,
            }
          : {}),
        abstractQuality:
          stored.abstractQuality ?? (stored.abstractQuality40 ? 40 : 0),
      }
    : null
  if (!saved) return {}
  if (
    key === 'support-test-start' &&
    saved.searchRequest &&
    (!isFlatRouteGoal(saved.searchRequest.goal) ||
      saved.searchRequest.start.item.baseItemId !== saved.item?.baseItemId ||
      saved.searchRequest.goal.general.baseItemId !== saved.item?.baseItemId ||
      (saved.searchRequest.goal.general.quality === 40 &&
        baseIdForKey(saved.base) !== saved.item?.baseItemId))
  )
    return {
      ...saved,
      searchRequest: null,
      editing: true,
      testQualityChoice: null,
    }
  if (saved.searchRequest && hasActiveTotalGoal(saved.searchRequest.goal))
    return { ...saved, editing: true }
  return saved.searchRequest && hasImplicitGoal(saved.searchRequest.goal)
    ? { ...saved, searchRequest: null, editing: true }
    : saved
}
