import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { concreteInitial } from '../workbenchApi'
import type { PoolReference } from '../workbenchReferenceApi'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it } from 'vitest'
import { setLocale, locales } from '../../../shared/i18n/i18n'
import { initialFixture } from '../../../shared/test/craftingFixtures'
import type { Definition, Initial } from '../craftingApi'
import { ModifierPoolPanel } from './ModifierPoolPanel'
import { modifierPoolMessages } from './modifierPoolMessages'

const definition = (
  id: string,
  values: Partial<Definition> = {},
): Definition => ({
  id,
  name: id,
  text: '+(1—10) to maximum Life',
  tier: 2,
  affixType: 'PREFIX',
  layer: 'EXPLICIT',
  familyIds: ['life'],
  requiredItemLevel: 1,
  weight: 100,
  stats: [{ id: 'life', min: 1, max: 10 }],
  ...values,
})
const catalog: Initial = {
  ...initialFixture,
  metadata: {
    ...initialFixture.metadata,
    sourceUrl: 'https://poe2db.tw/us/Amulets#ModifiersCalc',
    weightPolicy: 'POE2DB_AS_PUBLISHED',
  },
  modifiers: {
    low: definition('low'),
    high: definition('high', {
      tier: 1,
      requiredItemLevel: 80,
      weight: 20,
      text: '+(11—20) to maximum Life',
      stats: [{ id: 'life', min: 11, max: 20 }],
    }),
    locked: definition('locked', { tier: 0, requiredItemLevel: 90, weight: 5 }),
    defence: definition('defence', {
      weight: 80,
      text: '+(1—20) to maximum Energy Shield',
      familyIds: ['energy'],
      stats: [{ id: 'energy', min: 1, max: 20 }],
    }),
    suffix: definition('suffix', {
      affixType: 'SUFFIX',
      weight: 40,
      text: '+(1—10) to Strength',
      familyIds: ['strength'],
      stats: [{ id: 'strength', min: 1, max: 10 }],
    }),
  },
}
function show(
  data: Initial | undefined = catalog,
  loading = false,
  level = 82,
) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  if (data) {
    const reference: PoolReference = {
      entries: Object.values(data.modifiers)
        .filter((d) => d.affixType !== 'NONE')
        .map((d) => ({
          modifierId: d.id,
          kind: 'ordinary',
          eligible: d.id !== 'locked',
          reasons: d.id === 'locked' ? ['ITEM_LEVEL'] : [],
          actions: [],
        })),
    }
    client.setQueryData(
      [
        'crafting',
        'modifier-pools',
        data.rulesetIdentity,
        concreteInitial(data),
        'dynamic',
      ],
      reference,
    )
  }
  return render(
    <ModifierPoolPanel
      catalog={data}
      baseName="Solar Amulet"
      itemLevel={level}
      loading={loading}
    />,
    {
      wrapper: ({ children }) => (
        <QueryClientProvider client={client}>{children}</QueryClientProvider>
      ),
    },
  )
}
it('keeps each affix denominator intact during search and shows tier probabilities against that same pool', () => {
  show()
  const prefix = screen.getByRole('region', { name: 'Prefixes' })
  const suffix = screen.getByRole('region', { name: 'Suffixes' })
  expect(within(prefix).getByText('Total weight 200')).toBeVisible()
  expect(within(suffix).getByText('Total weight 40')).toBeVisible()
  const life = [...prefix.querySelectorAll('details')].find((d) =>
    d.textContent?.includes('maximum Life'),
  )!
  expect(within(life).getByText('60.00%')).toBeVisible()
  fireEvent.click(life.querySelector('summary')!)
  expect(within(life).getByRole('table')).toBeVisible()
  expect(within(life).getByText('10.00%')).toBeVisible()
  expect(within(life).getByText('50.00%')).toBeVisible()
  expect(within(life).getByText('locked')).toBeVisible()
  expect(
    within(life).getByLabelText('Unavailable: Item level is too low'),
  ).toBeVisible()
  expect(within(life).getByText('0.00%')).toBeVisible()
  expect(
    within(suffix).getByText('100.00%', { selector: '.modifier-pool__chance' }),
  ).toBeVisible()
  fireEvent.change(screen.getByRole('searchbox'), {
    target: { value: 'maximum Life' },
  })
  expect(prefix.querySelectorAll('details')).toHaveLength(1)
  expect(within(prefix).getByText('60.00%')).toBeVisible()
  expect(within(prefix).getByText('Total weight 200')).toBeVisible()
  expect(within(suffix).getByRole('status')).toHaveTextContent(
    'No matching modifiers.',
  )
})
it.each([
  'USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS',
  'UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES',
  undefined,
])(
  'hides all weights, percentages and probability bars for %s without removing modifiers',
  (policy) => {
    const metadata = { ...catalog.metadata }
    delete metadata.weightPolicy
    if (policy) metadata.weightPolicy = policy
    const { container } = show({ ...catalog, metadata })
    expect(
      screen.getByText(modifierPoolMessages.en.unpublishedNote, {
        exact: false,
      }),
    ).toBeVisible()
    expect(container.querySelectorAll('details')).toHaveLength(3)
    expect(container.querySelector('.modifier-pool__bar')).toBeNull()
    expect(container.textContent).not.toContain('%')
    const group = container.querySelector('details')!
    fireEvent.click(group.querySelector('summary')!)
    const rows = within(group).getAllByRole('row')
    for (const row of rows.slice(1)) {
      const cells = within(row).getAllByRole('cell')
      expect(cells.at(-1)).toHaveTextContent('Not published')
      expect(cells.at(-2)).toHaveTextContent('Not published')
    }
  },
)
it.each(locales)(
  'provides translated controls, headings and formatted probabilities in %s',
  (locale) => {
    setLocale(locale)
    show()
    const messages = modifierPoolMessages[locale]
    expect(screen.getByRole('region', { name: messages.title })).toBeVisible()
    expect(
      screen.getByRole('searchbox', { name: messages.search }),
    ).toBeEnabled()
    expect(screen.getByRole('region', { name: messages.prefix })).toBeVisible()
    expect(screen.getByRole('region', { name: messages.suffix })).toBeVisible()
    const percent = new Intl.NumberFormat(locale, {
      style: 'percent',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(0.6)
    expect(screen.getByText(percent.replace(/\s/g, ' '))).toBeVisible()
  },
)
it('clears prior option data on loading/error and keeps unavailable and empty pools distinct', () => {
  const { rerender, unmount } = show()
  rerender(
    <ModifierPoolPanel
      catalog={undefined}
      baseName="New base"
      itemLevel={1}
      loading
    />,
  )
  expect(screen.getByRole('status')).toHaveTextContent('Loading modifiers…')
  expect(screen.queryByText('60.00%')).toBeNull()
  rerender(
    <ModifierPoolPanel
      catalog={undefined}
      baseName="New base"
      itemLevel={1}
      loading={false}
    />,
  )
  expect(screen.getByRole('status')).toHaveTextContent(
    'Modifier data is unavailable',
  )
  unmount()
  show({ ...catalog, modifiers: {} })
  expect(screen.getAllByRole('status')).toHaveLength(6)
  expect(screen.getAllByRole('status')[0]).toHaveTextContent(
    'No modifiers at this item level.',
  )
})
it('does not link to an unrecognized source host', () => {
  show({
    ...catalog,
    metadata: {
      ...catalog.metadata,
      sourceUrl: 'https://example.com/poe2db.tw/',
    },
  })
  expect(screen.queryByRole('link')).toBeNull()
})
