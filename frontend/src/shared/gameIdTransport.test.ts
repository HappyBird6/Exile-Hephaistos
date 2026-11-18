import { afterEach, describe, expect, it, vi } from 'vitest'
import fixture from '../../../docs/contracts/identifier-transport-v1/fixture.json'
import {
  craftingFetch,
  gameIdHeader,
  gameIdIdentity,
  transformGameIds,
} from './gameIdTransport'

afterEach(() => vi.unstubAllGlobals())
describe('identifier transport', () => {
  it('shares exact mappings with Java and preserves free text and nested keys', () => {
    expect(transformGameIds(fixture.plain, true)).toEqual(fixture.compact)
    expect(transformGameIds(fixture.compact, false)).toEqual(fixture.plain)
    const conflicting = structuredClone(fixture.plain)
    Object.assign(
      conflicting.state.explicits[0]!.values,
      fixture.compact.state.explicits[0]!.values,
    )
    expect(() => transformGameIds(conflicting, true)).toThrow('Conflicting')
  })
  it('negotiates before compressing requests and decodes responses for existing consumers', async () => {
    const fetch = vi.fn().mockImplementation(() =>
      Promise.resolve(
        new Response(JSON.stringify(fixture.compact), {
          headers: {
            [gameIdHeader]: gameIdIdentity,
            'X-Crafting-Ruleset': 'transport-test',
          },
        }),
      ),
    )
    vi.stubGlobal('fetch', fetch)
    const response = await craftingFetch('/api/v1/crafting/workbench/initial')
    expect(await response.json()).toEqual(fixture.plain)
    await craftingFetch('/api/v1/crafting/workbench/apply', {
      method: 'POST',
      headers: { 'X-Crafting-Ruleset': 'transport-test' },
      body: JSON.stringify(fixture.plain),
    })
    const init = fetch.mock.calls[1]![1]
    expect(JSON.parse(init.body)).toEqual(fixture.compact)
    expect(init.headers.get('X-Crafting-Ids-Body')).toBe('compact')
  })
  it('uses plain bodies with a server that does not acknowledge this wire revision', async () => {
    const fetch = vi
      .fn()
      .mockImplementation(() =>
        Promise.resolve(
          new Response(JSON.stringify(fixture.plain), {
            headers: { 'X-Crafting-Ruleset': 'old-server' },
          }),
        ),
      )
    vi.stubGlobal('fetch', fetch)
    const response = await craftingFetch('/api/v1/crafting/workbench/initial')
    expect(await response.json()).toEqual(fixture.plain)
    const body = JSON.stringify(fixture.plain)
    await craftingFetch('/api/v1/crafting/workbench/apply', {
      method: 'POST',
      headers: { 'X-Crafting-Ruleset': 'old-server' },
      body,
    })
    expect(fetch.mock.calls[1]![1].body).toBe(body)
    expect(fetch.mock.calls[1]![1].headers.has('X-Crafting-Ids-Body')).toBe(
      false,
    )
  })
  it('keeps signed repeat bodies byte-for-byte unchanged', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response('{}'))
    vi.stubGlobal('fetch', fetch)
    const init = { method: 'POST', body: JSON.stringify(fixture.plain) }
    await craftingFetch('/api/v1/crafting/workbench/chaos-repeat', init)
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/crafting/workbench/chaos-repeat',
      init,
    )
  })
})
