import { expect, it } from 'vitest'
import { saveSupport } from '../method-search/supportStorage'
import { loadStart, restoredStart } from './savedStart'
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
