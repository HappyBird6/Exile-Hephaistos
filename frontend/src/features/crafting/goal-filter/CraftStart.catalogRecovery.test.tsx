import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CraftStart } from './CraftStart'
import { emptyGoal } from './editor'
import { saveSupport } from '../method-search/supportStorage'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { concreteInitial } from '../workbenchApi'
import { AutomaticMethodSearch } from '../method-search/AutomaticMethodSearch'
import type { PathTreeProps } from '../path-tree'

vi.mock('../method-search/AutomaticMethodSearch', () => ({
  AutomaticMethodSearch: vi.fn(({ request }: PathTreeProps) => (
    <output>{request.start.provenance.rulesetIdentity}</output>
  )),
}))
it.each(['valid', 'missing-base', 'invalid-item'])(
  'refreshes catalog safely after reload: %s',
  async (mode) => {
    const initial = {
      ...initialFixture,
      rulesetIdentity: 'current-rules',
      state: {
        ...initialFixture.state,
        baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
      },
    }
    const context = {
      snapshotId: initial.state.snapshotId,
      baseItemId: initial.state.baseItemId,
      itemLevel: 82,
    }
    const oldGoal = emptyGoal(context, 'old-catalog')
    oldGoal.general.quality = 20
    saveSupport('goal', oldGoal)
    const originalItem = concreteInitial(initial)
    if (mode === 'invalid-item') originalItem.snapshotId = 'obsolete-snapshot'
    saveSupport('start', {
      base: mode === 'missing-base' ? 'removed-base' : 'solar',
      text: 'old starting text',
      item: originalItem,
      rulesetIdentity: 'old-rules',
      abstractQuality40: false,
      abstractQuality: 0,
      editing: true,
      started: false,
      levelEdited: false,
      searchRequest: null,
    })
    saveSupport('run', {
      request: 'old-request',
      job: '11111111-1111-4111-8111-111111111111',
      selected: 0,
      cycles: 100,
      route: [],
    })
    const startRecord = JSON.parse(
      localStorage.getItem('hephaistos.support.v1:start')!,
    ).data
    const record = localStorage.getItem('hephaistos.support.v1:run')
    const postIdentities: (string | null)[] = []
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input)
        let value: unknown
        if (url.includes('/registry'))
          value = { workbenchBases: { solar: {} }, entries: [] }
        else if (url.includes('/initial?')) value = initial
        else if (url.includes('/provenance?'))
          value = {
            ...requestFixture().start.provenance,
            rulesetIdentity: initial.rulesetIdentity,
          }
        else if (url.includes('/catalog?'))
          value = {
            version: 1,
            catalogVersion: 'current-catalog',
            context,
            groupTypes: [
              {
                type: 'AND',
                evaluation: 'SUPPORTED',
                probability: 'SUPPORTED',
                reasonCode: null,
              },
            ],
            stats: [],
            issues: [],
          }
        else if (url.endsWith('/validate')) {
          postIdentities.push(
            new Headers(init?.headers).get('X-Crafting-Ruleset'),
          )
          const body = JSON.parse(String(init?.body))
          expect(body.goal.catalogVersion).toBe('current-catalog')
          value = {
            version: 1,
            valid: true,
            issues: [],
            capabilities: { evaluation: 'SUPPORTED', probability: 'SUPPORTED' },
          }
        } else throw new Error('Unexpected endpoint: ' + url)
        const response = jsonResponse(value)
        response.headers.set('X-Crafting-Ruleset', initial.rulesetIdentity)
        return response
      }),
    )
    const mount = () =>
      render(
        <QueryClientProvider
          client={
            new QueryClient({ defaultOptions: { queries: { retry: false } } })
          }
        >
          <CraftStart active />
        </QueryClientProvider>,
      )
    const mounted = mount()
    let reload = await screen.findByRole('button', {
      name: 'Refresh catalog',
    })
    await waitFor(() => expect(reload).toBeEnabled())
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    fireEvent.click(reload)
    await waitFor(() => expect(reload).toBeEnabled())
    if (mode !== 'valid') {
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeDisabled()
      expect(screen.getAllByRole('alert').length).toBeGreaterThan(0)
      const kept = JSON.parse(
        localStorage.getItem('hephaistos.support.v1:start')!,
      ).data
      expect(kept.base).toBe(startRecord.base)
      expect(kept.item).toEqual(startRecord.item)
      expect(kept.text).toBe(startRecord.text)
      expect(kept.rulesetIdentity).toBe('old-rules')
      expect(localStorage.getItem('hephaistos.support.v1:run')).toBe(record)
      return
    }
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    )
    const retained = JSON.parse(
      localStorage.getItem('hephaistos.support.v1:start')!,
    ).data
    expect(retained.item).toEqual(concreteInitial(initial))
    expect(retained.text).toBe('old starting text')
    expect(retained.rulesetIdentity).toBe('current-rules')
    mounted.unmount()
    mount()
    reload = await screen.findByRole('button', { name: 'Refresh catalog' })
    await waitFor(() => expect(reload).toBeEnabled())
    fireEvent.click(reload)
    await waitFor(() => expect(reload).toBeEnabled())
    expect(localStorage.getItem('hephaistos.support.v1:run')).toBe(record)
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    await waitFor(() => expect(AutomaticMethodSearch).toHaveBeenCalled())
    const created = vi.mocked(AutomaticMethodSearch).mock.lastCall![0].request
    expect(created.start.provenance.rulesetIdentity).toBe('current-rules')
    expect(created.goal.catalogVersion).toBe('current-catalog')
    expect(created.goal.general.quality).toBeNull()
    expect(postIdentities.length).toBeGreaterThan(0)
    expect(
      postIdentities.every((identity) => identity === 'current-rules'),
    ).toBe(true)
    expect(localStorage.getItem('hephaistos.support.v1:run')).toBe(record)
  },
)
