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
