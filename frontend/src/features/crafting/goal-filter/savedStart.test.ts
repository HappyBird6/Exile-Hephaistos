import { expect, it } from 'vitest'
import { saveSupport } from '../method-search/supportStorage'
import { loadStart, restoredStart } from './savedStart'
import { baseIdForKey } from '../baseRegistry'
import { requestFixture } from '../path-tree/fixtures.test-support'
it('restores editor items with omitted optional defaults and keeps the immutable request', () => {
  const request = requestFixture()
  const {
    augmentSockets: _sockets,
    catalystQuality: _quality,
    ...item
  } = request.start.item
  void _sockets
  void _quality
  const data = {
    base: 'solar',
    text: 'Solar Amulet',
    item,
    rulesetIdentity: request.start.provenance.rulesetIdentity,
    abstractQuality40: false,
    editing: false,
    started: true,
    levelEdited: false,
    searchRequest: request,
  }
  saveSupport('start', data)
  expect(loadStart()).toEqual(data)
  expect(restoredStart()).toEqual({ ...data, abstractQuality: 0 })
  request.goal.groups[0]!.entries[0]!.statId =
    'hephaistos:v1:pseudo:total_cold_resistance'
  saveSupport('start', data)
  expect(restoredStart()).toMatchObject({
    editing: true,
    searchRequest: request,
    item,
  })
  expect(loadStart()).toEqual(data)
  request.goal.groups[0]!.entries[0]!.statId =
    'hephaistos:v1:implicit:base_spirit_from_equipment'
  saveSupport('start', data)
  expect(restoredStart()).toMatchObject({
    editing: true,
    searchRequest: null,
    item,
  })
  expect(loadStart()?.searchRequest?.goal).toEqual(request.goal)
})

it('retains the exact fractured modifier through saved start and restoration', () => {
  const request = requestFixture()
  const item = {
    ...request.start.item,
    rarity: 'RARE' as const,
    explicits: [{ modifierId: 'p', values: { life: 17 }, fractured: true }],
  }
  saveSupport('start', {
    base: 'solar',
    text: '+17 to maximum Life (fractured)',
    item,
    rulesetIdentity: request.start.provenance.rulesetIdentity,
    abstractQuality40: false,
    editing: true,
    started: false,
    levelEdited: false,
    searchRequest: null,
  })
  expect(loadStart()?.item).toEqual(item)
  expect(restoredStart()).toMatchObject({ item })
})

it('restores the chosen quality currency for test sessions only and never reuses a quality request for another base or group operation', () => {
  const request = requestFixture()
  request.start.item.baseItemId = baseIdForKey('solar')!
  request.goal.general.baseItemId = request.start.item.baseItemId
  request.goal.general.quality = 40
  request.goal.groups[0]!.disabled = true
  request.goal.groups[0]!.entries = []
  request.qualityCatalyst = 'Flesh_Catalyst'
  const saved = {
    base: 'solar',
    text: 'Solar Amulet',
    item: request.start.item,
    rulesetIdentity: request.start.provenance.rulesetIdentity,
    abstractQuality40: false,
    editing: false,
    started: true,
    levelEdited: false,
    searchRequest: request,
  }
  saveSupport('support-test-start', saved)
  expect(restoredStart('support-test-start')).toMatchObject({
    testQualityChoice: 'Flesh_Catalyst',
    searchRequest: request,
  })
  saveSupport('support-test-start', {
    ...saved,
    item: { ...saved.item, baseItemId: 'other-base' },
  })
  expect(restoredStart('support-test-start')).toMatchObject({
    testQualityChoice: null,
    searchRequest: null,
    editing: true,
  })
  saveSupport('support-test-start', { ...saved, base: 'ring' })
  expect(restoredStart('support-test-start')).toMatchObject({
    testQualityChoice: null,
    searchRequest: null,
    editing: true,
  })
  request.goal.groups[0]!.disabled = false
  request.goal.groups[0]!.type = 'COUNT'
  request.goal.groups[0]!.range = { min: 1, max: null }
  saveSupport('support-test-start', saved)
  expect(restoredStart('support-test-start')).toMatchObject({
    testQualityChoice: null,
    searchRequest: null,
    editing: true,
  })
  expect(
    loadStart('support-test-start')?.searchRequest?.goal.groups[0]?.type,
  ).toBe('COUNT')
  request.qualityCatalyst = 'Refined_Flesh_Catalyst'
  saveSupport('support-test-start', saved)
  expect(loadStart('support-test-start')).toBeNull()
})
