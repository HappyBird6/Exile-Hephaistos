import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it } from 'vitest'
import example from '../../../../../backend/src/main/resources/crafting/goalfilter/production-example.json'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { localizedModifierText } from '../localizedModifiers'
import { GoalFilterPanel } from './GoalFilterPanel'
import { StartModifierPicker } from './StartModifierPicker'
import { createGoalFilterEditor, emptyGoal } from './editor'
import { goalFilterMessages } from './i18n'
import type { Catalog, GoalFilterAdapter } from './types'

it('preserves an explicit Rare-only target selection in the compact editor', async () => {
  const catalog = structuredClone(example.catalog) as Catalog
  const goal = emptyGoal(catalog.context, catalog.catalogVersion)
  goal.general.rarities = ['RARE']
  const editor = createGoalFilterEditor(goal)
  const adapter: GoalFilterAdapter = {
    id: 'compact-rarity',
    catalog: async () => catalog,
    validate: async () => ({
      version: 1,
      valid: true,
      issues: [],
      capabilities: { evaluation: 'SUPPORTED', probability: 'SUPPORTED' },
    }),
  }
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <GoalFilterPanel
        adapter={adapter}
        context={catalog.context}
        editor={editor}
        bases={[]}
        language="en"
        compact
      />
    </QueryClientProvider>,
  )
  const rare = await screen.findByRole('checkbox', { name: 'RARE' })
  expect(rare).toBeChecked()
  expect(screen.getByRole('checkbox', { name: 'MAGIC' })).not.toBeChecked()
  fireEvent.click(screen.getByRole('checkbox', { name: 'MAGIC' }))
  expect(editor.getState().goal.general.rarities).toEqual(['RARE', 'MAGIC'])
  fireEvent.click(screen.getByRole('checkbox', { name: 'MAGIC' }))
  expect(editor.getState().goal.general.rarities).toEqual(['RARE'])
})

it('omits implicit candidates while preserving and warning about an existing implicit condition', async () => {
  const catalog = structuredClone(example.catalog) as Catalog
  const implicit = {
    ...catalog.stats[0]!,
    statId: 'hephaistos:v1:implicit:test',
    kind: 'IMPLICIT' as const,
    label: 'Fixed only test',
  }
  catalog.stats.push(implicit)
  const goal = emptyGoal(catalog.context, catalog.catalogVersion)
  goal.groups[0]!.entries.push({
    id: 'old-implicit',
    statId: implicit.statId,
    unit: implicit.unit,
    range: { min: 1, max: null },
    weight: null,
    disabled: false,
  })
  const editor = createGoalFilterEditor(goal)
  const adapter: GoalFilterAdapter = {
    id: 'implicit-picker',
    catalog: async () => catalog,
    validate: async () => ({
      version: 1,
      valid: true,
      issues: [],
      capabilities: { evaluation: 'SUPPORTED', probability: 'SUPPORTED' },
    }),
  }
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <GoalFilterPanel
        adapter={adapter}
        context={catalog.context}
        editor={editor}
        bases={[]}
        language="en"
        compact
      />
    </QueryClientProvider>,
  )
  expect(
    await screen.findByText(/Existing implicit target conditions/),
  ).toBeInTheDocument()
  fireEvent.change(screen.getByRole('combobox', { name: 'Search stats' }), {
    target: { value: 'Fixed only test' },
  })
  expect(
    screen.queryByRole('option', { name: /Fixed only test/ }),
  ).not.toBeInTheDocument()
  expect(editor.getState().goal.groups[0]!.entries[0]!.statId).toBe(
    implicit.statId,
  )
})

for (const locale of locales) {
  it(`renders the same life effect in both actual picker components: ${locale}`, async () => {
    setLocale(locale)
    const catalog = example.catalog as Catalog
    const definition = catalog.sourceModifiers!['amulet:prefix:athlete-s']!
    const description = localizedModifierText(definition, undefined, locale)
    const editor = createGoalFilterEditor(
      emptyGoal(catalog.context, catalog.catalogVersion),
    )
    const adapter: GoalFilterAdapter = {
      id: `catalog-parity-${locale}`,
      catalog: async () => catalog,
      validate: async () => ({
        version: 1,
        valid: true,
        issues: [],
        capabilities: { evaluation: 'SUPPORTED', probability: 'UNSUPPORTED' },
      }),
    }
    render(
      <QueryClientProvider
        client={
          new QueryClient({ defaultOptions: { queries: { retry: false } } })
        }
      >
        <div data-testid="starting">
          <StartModifierPicker definitions={[definition]} onAdd={() => {}} />
        </div>
        <GoalFilterPanel
          adapter={adapter}
          context={catalog.context}
          editor={editor}
          bases={[]}
          language={locale}
          compact
        />
      </QueryClientProvider>,
    )
    const starting = within(screen.getByTestId('starting')).getByRole(
      'combobox',
    )
    fireEvent.focus(starting)
    expect(
      within(screen.getByRole('listbox')).getByRole('option'),
    ).toHaveTextContent(description)
    fireEvent.blur(starting)
    const target = screen.getByRole('combobox', {
      name: goalFilterMessages[locale].search,
    })
    fireEvent.change(target, { target: { value: description } })
    const option = await within(await screen.findByRole('listbox')).findByRole(
      'option',
    )
    expect(option).not.toHaveTextContent('base_maximum_life')
    expect(option).not.toHaveAttribute('aria-disabled', 'true')
    fireEvent.click(option)
    expect(editor.getState().goal.groups[0]!.entries[0]!.statId).toBe(
      'hephaistos:v1:explicit:base_maximum_life',
    )
    setLocale('en')
  })
}
