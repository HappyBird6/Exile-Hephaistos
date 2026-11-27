import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import {
  fixtureFetch,
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { baseIdForKey } from '../baseRegistry'
import { loadAccount } from '../../account/auth'
import { concreteInitial } from '../workbenchApi'
import { saveSupport } from '../method-search/supportStorage'
import { CraftStart } from './CraftStart'
import { craftStartMessages } from './craftStartMessages'
import { emptyGoal } from './editor'
import { loadStart } from './savedStart'
import type { Catalog } from './types'
import {
  currentPresetGoal,
  loadPreset,
  listPresets,
  savePreset,
  type SupportPreset,
} from './supportPresetStorage'

const id = baseIdForKey('solar')!
const item = { ...concreteInitial(initialFixture), baseItemId: id }
const goal = emptyGoal(item, 'test-catalog')
goal.groups[0]!.entries.push({
  id: 'cold',
  statId: 'direct-cold',
  unit: 'percent',
  range: { min: 18, max: 32 },
  weight: null,
  disabled: true,
})
function preset(testQualityChoice: string | null = null): SupportPreset {
  return {
    start: {
      base: 'solar',
      text: 'Solar Amulet',
      levelEdited: true,
      item,
      rulesetIdentity: initialFixture.rulesetIdentity,
      testQualityChoice,
    },
    goal,
  }
}
beforeEach(() => localStorage.clear())

it.each([null, '', 'Eshs_Catalyst'])(
  'preserves the start, every goal row and the quality choice %s as a detached named set',
  (choice) => {
    const input = preset(choice)
    const key = savePreset(null, '  Solar setup  ', input)!
    expect(listPresets(null)).toEqual([
      { key, name: 'Solar setup', guest: true },
    ])
    expect(loadPreset(null, key)).toEqual(input)
    const restored = loadPreset(null, key)
    restored.goal.groups[0]!.entries[0]!.range.min = 99
    expect(savePreset(null, 'Solar setup', restored)).toBeNull()
    expect(loadPreset(null, key)).toEqual(input)
  },
)

it('lets a logged-in user load guest setups on this browser and isolates other accounts', () => {
  const guest = savePreset(null, 'Guest', preset())!
  const alice = savePreset('alice', 'Private', preset(''))!
  const bob = savePreset('bob', 'Private', preset('Eshs_Catalyst'))!
  expect(listPresets('alice').map((p) => p.key)).toEqual([guest, alice])
  expect(listPresets('bob').map((p) => p.key)).toEqual([guest, bob])
  expect(listPresets(null).map((p) => p.key)).toEqual([guest])
  expect(loadPreset('alice', guest)).toEqual(preset())
  expect(() => loadPreset('bob', alice)).toThrow()
  expect(() => loadPreset(null, bob)).toThrow()
  localStorage.setItem(alice, '{"start": {}}')
  expect(() => loadPreset('alice', alice)).toThrow()
  expect(loadPreset('alice', guest)).toEqual(preset())
})

it('loads the live starting item, disabled row values and catalyst into the test editor across bases without running a search', async () => {
  saveSupport('support-test-flat-goal', goal)
  let removeCold = false
  const fetcher = vi.fn(
    async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), 'http://localhost')
      if (url.pathname.endsWith('/registry'))
        return jsonResponse({
          workbenchBases: { solar: {}, stocky: {} },
          entries: [],
        })
      if (url.pathname.endsWith('/initial')) {
        const base =
          url.searchParams.get('base') === 'stocky' ? 'stocky' : 'solar'
        return jsonResponse({
          ...initialFixture,
          state: {
            ...initialFixture.state,
            baseItemId: baseIdForKey(base),
            implicits: base === 'solar' ? initialFixture.state.implicits : [],
          },
        })
      }
      if (url.pathname.endsWith('/goal-filters/catalog'))
        return jsonResponse({
          version: 1,
          catalogVersion: 'test-catalog',
          context: {
            baseItemId: url.searchParams.get('baseItemId'),
            snapshotId: url.searchParams.get('snapshotId'),
            itemLevel: Number(url.searchParams.get('itemLevel')),
          },
          groupTypes: [],
          issues: [],
          stats: removeCold
            ? []
            : [
                {
                  statId: 'direct-cold',
                  label: 'Cold resistance',
                  unit: 'percent',
                  kind: 'EXPLICIT',
                  support: {
                    evaluation: 'SUPPORTED',
                    probability: 'SUPPORTED',
                    reasonCode: null,
                  },
                  eligible: true,
                  eligibilityReason: null,
                  sourceStatIds: ['cold'],
                  contributions: [],
                  sourceUrls: [],
                },
              ],
        })
      return fixtureFetch(input, init)
    },
  )
  vi.stubGlobal('fetch', fetcher)
  await loadAccount()
  const results = vi.fn(() => null)
  const mount = () =>
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <CraftStart active testResults={results} />
      </QueryClientProvider>,
    )

  const first = mount()
  const favorite = await screen.findByRole('group', { name: 'Favorite setups' })
  expect(favorite.parentElement).toBe(
    document.querySelector('.craft-start-stats > footer:last-child'),
  )
  expect(favorite.closest('.goal-filter-connected')).toBeNull()
  const level = screen.getByRole('spinbutton')
  fireEvent.change(level, { target: { value: '76' } })
  const source = await screen.findByRole('group', { name: 'Cold resistance' })
  fireEvent.change(within(source).getByPlaceholderText('Min'), {
    target: { value: '23' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Quality 40' }))
  fireEvent.change(screen.getByRole('combobox', { name: 'Catalyst' }), {
    target: { value: 'Eshs_Catalyst' },
  })
  fireEvent.change(within(favorite).getByRole('textbox'), {
    target: { value: 'Cold jewellery' },
  })
  const save = within(favorite).getByRole('button', { name: 'Save favorite' })
  await waitFor(() => expect(save).toBeEnabled())
  fireEvent.click(save)
  expect(await within(favorite).findByText('Favorite saved.')).toBeVisible()
  const key = listPresets(null)[0]!.key
  expect(loadPreset(null, key)).toMatchObject({
    start: {
      base: 'solar',
      item: { itemLevel: 76 },
      testQualityChoice: 'Eshs_Catalyst',
    },
    goal: {
      groups: [{ entries: [{ disabled: true, range: { min: 23, max: 32 } }] }],
    },
  })
  const older = loadPreset(null, key)
  older.start.rulesetIdentity = 'previous-ruleset'
  localStorage.setItem(key, JSON.stringify(older))
  fireEvent.click(screen.getByRole('button', { name: 'Reset goal' }))
  fireEvent.change(
    screen.getByRole('combobox', { name: craftStartMessages.en.typeLabel }),
    { target: { value: 'Gloves' } },
  )
  await waitFor(() =>
    expect(
      screen.getByRole('combobox', { name: craftStartMessages.en.baseLabel }),
    ).toHaveValue('stocky'),
  )
  await screen.findByRole('combobox', { name: 'Select a favorite' })
  window.dispatchEvent(new Event('pagehide'))
  first.unmount()
  mount()
  fireEvent.change(
    await screen.findByRole('combobox', { name: 'Select a favorite' }),
    { target: { value: key } },
  )
  fireEvent.click(screen.getByRole('button', { name: 'Load favorite' }))
  expect(await screen.findByText('Favorite loaded.')).toBeVisible()
  expect(loadStart('support-test-start')?.rulesetIdentity).toBe(
    initialFixture.rulesetIdentity,
  )
  expect(loadPreset(null, key).start.rulesetIdentity).toBe('previous-ruleset')
  expect(
    screen.getByRole('combobox', { name: craftStartMessages.en.baseLabel }),
  ).toHaveValue('solar')
  expect(screen.getByRole('combobox', { name: 'Catalyst' })).toHaveValue(
    'Eshs_Catalyst',
  )
  expect(screen.getByRole('spinbutton')).toHaveValue(76)
  const row = within(screen.getByRole('group', { name: 'Cold resistance' }))
  expect(row.getByRole('checkbox')).not.toBeChecked()
  expect(row.getByPlaceholderText('Min')).toHaveValue('23')
  expect(row.getByPlaceholderText('Max')).toHaveValue('32')
  expect(results).not.toHaveBeenCalled()
  expect(
    fetcher.mock.calls.some(([url]) => String(url).includes('test-routes')),
  ).toBe(false)
  // A previous result cannot replace the newly loaded draft or launch the saved search again.
  fireEvent.click(row.getByRole('checkbox'))
  fireEvent.change(row.getByPlaceholderText('Min'), { target: { value: '12' } })
  const startButton = screen.getByRole('button', { name: 'Start Crafting' })
  await waitFor(() => expect(startButton).toBeEnabled())
  fireEvent.click(startButton)
  await waitFor(() => expect(results).toHaveBeenCalled())
  await waitFor(() =>
    expect(
      loadStart('support-test-start')?.searchRequest?.goal.groups[0]
        ?.entries[0],
    ).toMatchObject({ disabled: false, range: { min: 12 } }),
  )
  fireEvent.click(screen.getByRole('button', { name: 'Load favorite' }))
  await waitFor(() =>
    expect(loadStart('support-test-start')?.searchRequest).toBeNull(),
  )

  const loadedRow = within(
    screen.getByRole('group', { name: 'Cold resistance' }),
  )
  expect(loadedRow.getByRole('checkbox')).not.toBeChecked()
  expect(loadedRow.getByPlaceholderText('Min')).toHaveValue('23')
  fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '72' } })
  const raw = localStorage.getItem(key)
  removeCold = true
  fireEvent.click(screen.getByRole('button', { name: 'Load favorite' }))
  expect(
    await screen.findByText(
      /These saved goal rows are unavailable.*1 · direct-cold/,
    ),
  ).toBeVisible()
  expect(screen.getByRole('spinbutton')).toHaveValue(72)
  expect(localStorage.getItem(key)).toBe(raw)
})

it('reuses compatible older goal catalogs and reports a changed or missing disabled option without changing the preset', () => {
  const sample = structuredClone(preset())
  const statId = 'hephaistos:v1:explicit:cold'
  sample.goal.groups[0]!.entries[0]!.statId = statId
  const catalog: Catalog = {
    version: 1,
    catalogVersion: 'new',
    context: {
      snapshotId: item.snapshotId,
      baseItemId: item.baseItemId,
      itemLevel: item.itemLevel,
    },
    groupTypes: [
      {
        type: 'AND',
        evaluation: 'SUPPORTED',
        probability: 'UNKNOWN',
        reasonCode: null,
      },
    ],
    issues: [],
    stats: [
      {
        statId,
        label: 'Cold resistance',
        unit: 'percent',
        kind: 'EXPLICIT',
        support: {
          evaluation: 'SUPPORTED',
          probability: 'UNKNOWN',
          reasonCode: null,
        },
        eligible: true,
        eligibilityReason: null,
        sourceStatIds: ['cold'],
        contributions: [],
        sourceUrls: [],
      },
    ],
  }
  expect(currentPresetGoal(sample, catalog, 'en')).toEqual({
    ...sample.goal,
    catalogVersion: 'new',
  })
  expect(sample.goal.catalogVersion).toBe('test-catalog')
  const changed = {
    ...catalog,
    stats: [{ ...catalog.stats[0]!, unit: 'flat' }],
  }
  expect(() => currentPresetGoal(sample, changed, 'en')).toThrow(
    expect.objectContaining({ rows: ['1 · Cold resistance'] }),
  )
  expect(() =>
    currentPresetGoal(sample, { ...catalog, stats: [] }, 'en'),
  ).toThrow(expect.objectContaining({ rows: ['1 · ' + statId] }))
})
