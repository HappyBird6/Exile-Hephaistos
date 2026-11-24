import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { AppProviders } from '../../../app/AppProviders'
import { setLocale } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { rulesetHeader } from '../rulesetIdentity'
import { AcquisitionRouteResults } from './AcquisitionRouteResults'
import {
  loadAcquisitionRoutes,
  type AcquisitionResult,
} from './acquisitionRouteApi'

afterEach(() => vi.unstubAllGlobals())

const request = requestFixture()
const definitions: Initial['modifiers'] = {
  life: {
    id: 'life',
    name: 'Life',
    text: '+20 to maximum Life',
    tier: 1,
    affixType: 'PREFIX',
    familyIds: [],
  },
  cold: {
    id: 'cold',
    name: 'Cold',
    text: '+12% to Cold Resistance',
    tier: 1,
    affixType: 'SUFFIX',
    familyIds: [],
  },
}

function serve(result: AcquisitionResult) {
  const fetch = vi.fn().mockImplementation(
    async () =>
      new Response(JSON.stringify(result), {
        headers: {
          [rulesetHeader]: request.start.provenance.rulesetIdentity,
        },
      }),
  )
  vi.stubGlobal('fetch', fetch)
  return fetch
}
function show() {
  return render(
    <AcquisitionRouteResults
      request={request}
      definitions={definitions}
      baseName="Solar Amulet"
      active
    />,
    { wrapper: AppProviders },
  )
}

it('requests the current item and goal, and displays the selected order, preparation, gains and losses', async () => {
  await setLocale('en')
  const step = { action: 'EXALTED', omens: [], acquired: [], lost: [] }
  const fetch = serve({
    cards: [
      {
        options: [
          { number: 1, modifierIds: ['life'] },
          { number: 2, modifierIds: ['cold'] },
        ],
      },
    ],
    routes: [
      {
        cardIndex: 0,
        order: [1, 2],
        probability: 0.27,
        steps: [
          {
            ...step,
            action: 'PERFECT_ESSENCE_ENHANCEMENT',
            omens: ['Omen_of_Sinistral_Crystallisation'],
            acquired: [1],
          },
          { ...step, action: 'Preserved_Collarbone', acquired: [2] },
        ],
      },
      {
        cardIndex: 0,
        order: [2, 1],
        probability: null,
        steps: [
          step,
          { ...step, acquired: [2], lost: [1] },
          { ...step, acquired: [1] },
        ],
      },
    ],
    complete: false,
  })
  show()
  const list = await screen.findByRole('list', { name: 'Crafting methods' })
  const [path, options] = fetch.mock.calls[0]!
  expect(path).toBe('/api/v1/crafting/support/goal-filters/test-routes')
  expect(options.method).toBe('POST')
  expect(JSON.parse(options.body)).toEqual({
    item: request.start.item,
    goal: request.goal,
  })
  expect(options.signal).toBeInstanceOf(AbortSignal)
  const [first, second] = within(list).getAllByRole('button')
  expect(first).toHaveAccessibleName(/1 → 2.*27%/)
  expect(first).toHaveAttribute('aria-pressed', 'true')
  const detail = screen.getByRole('region', {
    name: 'Modifier acquisition order',
  })
  expect(within(detail).getAllByRole('listitem')[0]).toHaveTextContent(
    'Perfect Essence of Enhancement + Omen of Sinistral Crystallisation',
  )
  expect(within(detail).getAllByRole('listitem')[1]).toHaveTextContent(
    'Preserved Collarbone',
  )
  expect(second).toHaveAccessibleName(/2 → 1.*Not yet determined/)
  fireEvent.click(second!)
  expect(within(detail).getByText('+20 to maximum Life')).toBeInTheDocument()
  const steps = within(detail).getAllByRole('listitem')
  expect(steps).toHaveLength(3)
  expect(steps[0]).toHaveTextContent('Exalted Orb')
  expect(steps[0]).not.toHaveTextContent('Added')
  expect(steps[1]).toHaveTextContent('Removed 1 · Added 2')
  expect(second).toHaveAttribute('aria-pressed', 'true')
})

it('shows already-complete and incomplete empty results distinctly', async () => {
  await setLocale('en')
  serve({
    cards: [{ options: [] }],
    routes: [{ cardIndex: 0, steps: [], order: [], probability: 1 }],
    complete: true,
  })
  const first = show()
  expect(
    await screen.findByRole('button', { name: /Goal already reached.*100%/ }),
  ).toBeInTheDocument()
  first.unmount()
  serve({ cards: [], routes: [], complete: false })
  show()
  expect(await screen.findByText('Not fully calculated')).toBeInTheDocument()
  expect(
    screen.queryByText('No matching crafting route found.'),
  ).not.toBeInTheDocument()
})

it('labels both socket replacement choices with the localized material and one-based socket number', async () => {
  await setLocale('ko')
  serve({
    cards: [{ options: [{ number: 1, modifierIds: ['life'] }] }],
    routes: [0, 1].map((socketIndex) => ({
      cardIndex: 0,
      order: [1],
      probability: null,
      steps: [
        {
          action: 'Astrids_Creativity',
          socketIndex,
          omens: [],
          acquired: [],
          lost: [],
        },
        { action: 'EXALTED', omens: [], acquired: [1], lost: [] },
      ],
    })),
    complete: true,
  })
  show()
  const routes = await screen.findByRole('list', { name: '제작 방법' })
  const [first, second] = within(routes).getAllByRole('button')
  expect(first).toHaveTextContent('아스트리드의 창의성 (소켓 1)')
  expect(second).toHaveTextContent('아스트리드의 창의성 (소켓 2)')
  fireEvent.click(second!)
  const details = screen.getByRole('region', { name: '옵션 획득 순서' })
  const steps = within(details).getAllByRole('listitem')
  expect(steps[0]).toHaveTextContent('아스트리드의 창의성 (소켓 2)')
  expect(steps[1]).not.toHaveTextContent('소켓')
})

it('rejects a response from another ruleset', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () => new Response('{}', { headers: { [rulesetHeader]: 'other' } }),
    ),
  )
  await expect(
    loadAcquisitionRoutes(request, new AbortController().signal),
  ).rejects.toThrow()
})
