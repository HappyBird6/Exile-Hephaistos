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
