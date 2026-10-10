import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act } from '@testing-library/react'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { craftStartMessages } from './craftStartMessages'
import { CraftStart } from './CraftStart'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { concreteInitial } from '../workbenchApi'
import { parseItemText } from '../itemTextApi'
import { mapSolarText } from '../workbenchApi'
import type { Item } from '../itemModels'

vi.mock('./ConnectedGoalFilter', () => ({
  ConnectedGoalFilter: () => (
    <label>
      Target draft
      <input defaultValue="preserved" />
    </label>
  ),
}))
vi.mock('../itemTextApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../itemTextApi')>()),
  parseItemText: vi.fn(),
}))
vi.mock('../workbenchApi', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../workbenchApi')>()),
  mapSolarText: vi.fn(),
}))
const solar = {
  ...initialFixture,
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Amulets/FourAmulet9',
  },
}
const ring = {
  ...initialFixture,
  modifiers: {
    ...initialFixture.modifiers,
    'iron-ring:implicit:added-physical-damage-to-attacks': {
      id: 'iron-ring:implicit:added-physical-damage-to-attacks',
      name: 'Physical damage',
      text: 'Adds 1 to 4 Physical Damage to Attacks',
      tier: 0,
      affixType: 'NONE' as const,
      layer: 'IMPLICIT',
      familyIds: ['PhysicalDamage'],
      stats: [
        { id: 'attack_minimum_added_physical_damage', min: 1, max: 1 },
        { id: 'attack_maximum_added_physical_damage', min: 4, max: 4 },
      ],
    },
  },
  state: {
    ...initialFixture.state,
    baseItemId: 'Metadata/Items/Rings/FourRing1',
    implicits: [
      {
        modifierId: 'iron-ring:implicit:added-physical-damage-to-attacks',
        values: {
          attack_minimum_added_physical_damage: 1,
          attack_maximum_added_physical_damage: 4,
        },
      },
    ],
  },
}
function parsed(base = 'Solar Amulet', itemClass = 'Amulets'): Item {
  return {
    text: { originalText: base, lines: [] },
    locale: 'en',
    itemClass,
    rarity: 'NORMAL',
    rarityText: 'Normal',
    nameLines: [],
    displayName: base,
    displayBase: null,
    itemLevel: 82,
    properties: [],
    requirements: [],
    markedModifiers: [],
    modifiers: [],
    flags: [],
    unparsedLines: [],
    warnings: [],
  }
}
beforeEach(() => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes('/registry'))
        return jsonResponse({
          workbenchBases: { solar: {}, ring: {} },
          entries: [],
        })
      return jsonResponse(url.includes('base=ring') ? ring : solar)
    }),
  )
  vi.mocked(parseItemText).mockResolvedValue(parsed())
  vi.mocked(mapSolarText).mockResolvedValue({
    mapped: true,
    state: concreteInitial(solar),
    issues: [],
  })
})
afterEach(() => {
  vi.unstubAllGlobals()
  vi.clearAllMocks()
})
async function setup() {
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <CraftStart active>
        <p>Family controls preserved</p>
      </CraftStart>
    </QueryClientProvider>,
  )
  await waitFor(() =>
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeEnabled(),
  )
}
function chooseLifeGroup() {
  const picker = screen.getByRole('combobox', { name: 'Add modifier' })
  fireEvent.change(picker, { target: { value: 'maximum Life' } })
  fireEvent.keyDown(picker, { key: 'Enter' })
}
describe('Craft Support starting screen', () => {
  it('keeps the picker mounted from the first view and places import first', async () => {
    await setup()
    const editor = screen.getByRole('region', { name: 'Starting item editor' })
    expect(within(editor).getAllByRole('button')[0]).toHaveTextContent(
      'Import item text',
    )
    expect(
      screen.queryByRole('button', { name: 'Add modifier' }),
    ).not.toBeInTheDocument()
    const picker = screen.getByRole('combobox', { name: 'Add modifier' })
    expect(picker).toHaveAttribute(
      'placeholder',
      '+ Add modifier · Prefix ≤0 / Suffix ≤0',
    )
    fireEvent.change(screen.getByLabelText('Starting rarity'), {
      target: { value: 'RARE' },
    })
    const rows = editor.querySelector('.craft-start-modifier-rows')
    for (let i = 0; i < 3; i++) {
      fireEvent.click(picker)
      expect(editor.querySelector('.picker-popover')).toHaveStyle({
        position: 'fixed',
      })
      fireEvent.keyDown(picker, { key: 'Escape' })
      expect(screen.getByRole('combobox', { name: 'Add modifier' })).toBe(
        picker,
      )
      expect(editor.querySelector('.craft-start-modifier-rows')).toBe(rows)
    }
    chooseLifeGroup()
    expect(editor.querySelector('.craft-start-rolls')?.children).toHaveLength(4)
    fireEvent.click(
      screen.getByRole('button', { name: 'Remove starting modifier 1' }),
    )
    expect(editor.querySelector('.craft-start-modifier-rows')).toBe(rows)
  })
  it('selects the highest eligible tier at the edited item level and preserves conflicting rows for correction', async () => {
    const high = { ...solar.modifiers.p!, requiredItemLevel: 80 }
    const low = {
      ...high,
      id: 'low',
      tier: 2,
      requiredItemLevel: 1,
      text: '+(5–15) to maximum Life',
      stats: [{ id: 'life', min: 5, max: 15 }],
    }
    vi.mocked(fetch).mockImplementation(async (input) =>
      String(input).includes('/registry')
        ? jsonResponse({ workbenchBases: { solar: {} }, entries: [] })
        : jsonResponse({
            ...solar,
            modifiers: { ...solar.modifiers, p: high, low },
          }),
    )
    await setup()
    fireEvent.change(screen.getByLabelText('Starting rarity'), {
      target: { value: 'MAGIC' },
    })
    const level = screen.getByRole('spinbutton', { name: 'Item level' })
    fireEvent.change(level, { target: { value: '79' } })
    chooseLifeGroup()
    let tier = screen.getByRole('combobox', { name: /Starting modifier tier:/ })
    expect(tier).toHaveValue('low')
    expect(
      within(tier).queryByRole('option', { name: 'T1' }),
    ).not.toBeInTheDocument()
    fireEvent.change(level, { target: { value: '80' } })
    fireEvent.change(tier, { target: { value: 'p' } })
    expect(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
    ).toHaveValue(10)
    fireEvent.change(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
      { target: { value: '18' } },
    )
    fireEvent.change(level, { target: { value: '79' } })
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Change its tier or restore the level',
    )
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    tier = screen.getByRole('combobox', { name: /Starting modifier tier:/ })
    expect(tier).toHaveValue('p')
    expect(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
    ).toHaveValue(18)
    fireEvent.change(tier, { target: { value: 'low' } })
    expect(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
    ).toHaveValue(15)
    expect(screen.getByRole('button', { name: 'Start Crafting' })).toBeEnabled()
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    fireEvent.click(screen.getByRole('button', { name: /Edit settings/ }))
    expect(level).toHaveValue(79)
  })
  it('requires an explicit rarity choice, blocks conflicting downgrades and keeps Magic through editing and restart', async () => {
    await setup()
    const rarity = screen.getByRole('combobox', { name: 'Starting rarity' })
    expect(rarity).toHaveValue('NORMAL')
    expect(screen.getByRole('combobox', { name: 'Add modifier' })).toBeEnabled()
    fireEvent.click(screen.getByRole('combobox', { name: 'Add modifier' }))
    expect(
      screen.queryByRole('option', { name: /maximum Life/ }),
    ).not.toBeInTheDocument()
    fireEvent.keyDown(screen.getByRole('combobox', { name: 'Add modifier' }), {
      key: 'Escape',
    })
    fireEvent.change(rarity, { target: { value: 'MAGIC' } })
    chooseLifeGroup()
    expect(rarity).toHaveValue('MAGIC')
    expect(
      within(rarity).getByRole('option', { name: 'Normal' }),
    ).toBeDisabled()
    fireEvent.change(rarity, { target: { value: 'NORMAL' } })
    expect(rarity).toHaveValue('MAGIC')
    const preview = screen.getByRole('region', {
      name: 'Starting item preview',
    })
    expect(preview).toHaveTextContent('+10 to maximum Life')
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    expect(
      (screen.getByLabelText('Starting item text') as HTMLTextAreaElement)
        .value,
    ).toContain('Rarity: Magic')
    fireEvent.click(screen.getByRole('button', { name: 'Check item text' }))
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Check item text' }),
      ).toBeEnabled(),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(rarity).toHaveValue('MAGIC')
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    expect(
      screen.getByRole('region', { name: 'Crafting tree preview' }),
    ).toHaveTextContent('+10 to maximum Life')
    fireEvent.click(screen.getByRole('button', { name: /Edit settings/ }))
    expect(rarity).toHaveValue('MAGIC')
    fireEvent.click(
      screen.getByRole('button', { name: 'Remove starting modifier 1' }),
    )
    fireEvent.change(rarity, { target: { value: 'NORMAL' } })
    expect(rarity).toHaveValue('NORMAL')
  })
  it('synchronizes rarity from verified imported and manually edited text', async () => {
    await setup()
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    const text = screen.getByLabelText('Starting item text')
    const magic = { ...concreteInitial(solar), rarity: 'MAGIC' as const }
    vi.mocked(mapSolarText).mockResolvedValueOnce({
      mapped: true,
      state: magic,
      issues: [],
    })
    fireEvent.paste(text, {
      clipboardData: { getData: () => 'Imported magic text' },
    })
    await waitFor(() =>
      expect(
        screen.getByRole('combobox', { name: 'Starting rarity' }),
      ).toHaveValue('MAGIC'),
    )
    fireEvent.change(text, { target: { value: 'Manually edited rare text' } })
    expect(
      screen.getByRole('combobox', { name: 'Starting rarity' }),
    ).toBeDisabled()
    vi.mocked(mapSolarText).mockResolvedValueOnce({
      mapped: true,
      state: { ...magic, rarity: 'RARE' },
      issues: [],
    })
    fireEvent.click(screen.getByRole('button', { name: 'Check item text' }))
    await waitFor(() =>
      expect(
        screen.getByRole('combobox', { name: 'Starting rarity' }),
      ).toHaveValue('RARE'),
    )
  })
  it('cancels modal parsing on Escape, retains text, restores focus and discards the late result', async () => {
    await setup()
    const opener = screen.getByRole('button', { name: 'Import item text' })
    fireEvent.click(opener)
    let finish!: (value: Item) => void
    let signal: AbortSignal | undefined
    vi.mocked(parseItemText).mockImplementationOnce((_text, requestSignal) => {
      signal = requestSignal
      return new Promise((resolve) => {
        finish = resolve
      })
    })
    fireEvent.paste(screen.getByLabelText('Starting item text'), {
      clipboardData: { getData: () => 'unfinished original' },
    })
    fireEvent(
      screen.getByRole('dialog'),
      new Event('cancel', { cancelable: true }),
    )
    expect(signal?.aborted).toBe(true)
    expect(opener).toHaveFocus()
    await act(async () => finish(parsed()))
    expect(screen.getByLabelText('Starting item text')).toHaveValue(
      'unfinished original',
    )
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    expect(mapSolarText).not.toHaveBeenCalled()
    fireEvent.click(opener)
    expect(screen.getByRole('dialog')).toBeVisible()
  })
  it.each(locales)(
    'keeps setup controls accessible in %s and focuses the result without motion',
    async (locale) => {
      await setup()
      act(() => setLocale(locale))
      const copy = craftStartMessages[locale]
      expect(screen.getByRole('button', { name: copy.import })).toBeVisible()
      expect(
        screen.getByRole('combobox', { name: copy.typeLabel }),
      ).toBeVisible()
      vi.stubGlobal('matchMedia', () => ({ matches: true }))
      const animate = vi.fn()
      const previous = HTMLElement.prototype.animate
      HTMLElement.prototype.animate = animate
      try {
        fireEvent.click(screen.getByRole('button', { name: copy.start }))
        expect(screen.getByRole('region', { name: copy.tree })).toHaveFocus()
        expect(animate).not.toHaveBeenCalled()
        fireEvent.click(
          screen.getByRole('button', { name: new RegExp(copy.edit) }),
        )
        await act(async () => {})
        expect(screen.getByRole('button', { name: copy.start })).toHaveFocus()
      } finally {
        HTMLElement.prototype.animate = previous
      }
    },
  )
  it('synchronizes base text and starts an empty tree without mapping or probability calls', async () => {
    await setup()
    fireEvent.change(screen.getByLabelText('Starting equipment type'), {
      target: { value: 'Rings' },
    })
    expect(screen.getByLabelText('Starting item base')).toHaveValue('ring')
    expect(
      (screen.getByLabelText('Starting item text') as HTMLTextAreaElement)
        .value,
    ).toContain('Iron Ring')
    fireEvent.change(screen.getByLabelText('Target draft'), {
      target: { value: 'Spirit target' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    expect(
      screen.getByRole('region', { name: 'Crafting tree preview' }),
    ).toHaveTextContent('Iron Ring')
    expect(screen.getByText('No crafting paths yet.')).toBeVisible()
    expect(screen.getByLabelText('Starting item text')).not.toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: /Edit settings/ }))
    expect(screen.getByLabelText('Target draft')).toHaveValue('Spirit target')
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    expect(
      screen.getAllByRole('region', { name: 'Crafting tree preview' }),
    ).toHaveLength(1)
    expect(mapSolarText).not.toHaveBeenCalled()
    expect(
      vi
        .mocked(fetch)
        .mock.calls.map((c) => String(c[0]))
        .some((u) => /recommend|explore|evaluate/.test(u)),
    ).toBe(false)
  })
  it('auto-selects verified pasted base and rejects unknown or unsupported pasted items', async () => {
    await setup()
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    const text = screen.getByLabelText('Starting item text')
    fireEvent.paste(text, {
      clipboardData: { getData: () => 'valid Solar copy' },
    })
    await waitFor(() => expect(mapSolarText).toHaveBeenCalled())
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    )
    expect(text).toHaveValue('valid Solar copy')
    vi.mocked(parseItemText).mockResolvedValueOnce(parsed('Unknown Amulet'))
    fireEvent.paste(text, {
      clipboardData: { getData: () => 'unknown original' },
    })
    await screen.findByText(/This item base is not supported/)
    expect(screen.getByLabelText('Starting item base')).toHaveValue('')
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    expect(text).toHaveValue('unknown original')
    vi.mocked(parseItemText).mockResolvedValueOnce(parsed('Iron Ring', 'Rings'))
    fireEvent.paste(text, { clipboardData: { getData: () => 'copied ring' } })
    await screen.findByText(/Only Solar Amulet text/)
    expect(screen.getByLabelText('Starting item base')).toHaveValue('ring')
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
  })
  it('keeps a late validation response from replacing a newly selected base', async () => {
    await setup()
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    let resolve!: (item: Item) => void
    vi.mocked(parseItemText).mockImplementationOnce(
      () =>
        new Promise((r) => {
          resolve = r
        }),
    )
    fireEvent.paste(screen.getByLabelText('Starting item text'), {
      clipboardData: { getData: () => 'old pasted text' },
    })
    fireEvent.change(screen.getByLabelText('Starting equipment type'), {
      target: { value: 'Rings' },
    })
    resolve(parsed())
    await waitFor(() =>
      expect(screen.getByLabelText('Starting item base')).toHaveValue('ring'),
    )
    expect(
      (screen.getByLabelText('Starting item text') as HTMLTextAreaElement)
        .value,
    ).toContain('Iron Ring')
  })
  it('creates a non-Solar equipment modifier and retains it through start and edit', async () => {
    await setup()
    fireEvent.change(screen.getByLabelText('Starting equipment type'), {
      target: { value: 'Rings' },
    })
    fireEvent.change(
      screen.getByRole('combobox', { name: 'Starting rarity' }),
      { target: { value: 'MAGIC' } },
    )
    chooseLifeGroup()
    expect(
      screen.getByRole('region', { name: 'Starting item preview' }),
    ).toHaveTextContent('+10 to maximum Life')
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    vi.mocked(parseItemText).mockResolvedValueOnce(parsed('Iron Ring', 'Rings'))
    fireEvent.click(screen.getByRole('button', { name: 'Check item text' }))
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    )
    expect(mapSolarText).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(
      screen.getByRole('region', { name: 'Starting item preview' }),
    ).toHaveTextContent('+10 to maximum Life')
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    expect(
      screen.getByRole('region', { name: 'Crafting tree preview' }),
    ).toHaveTextContent('Iron Ring')
    fireEvent.click(screen.getByRole('button', { name: /Edit settings/ }))
    expect(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
    ).toHaveValue(10)
  })
  it('accepts the exact Amulet alias for verified pasted Solar text', async () => {
    await setup()
    fireEvent.click(screen.getByRole('button', { name: 'Import item text' }))
    vi.mocked(parseItemText).mockResolvedValueOnce(
      parsed('Solar Amulet', 'Amulet'),
    )
    fireEvent.paste(screen.getByLabelText('Starting item text'), {
      clipboardData: { getData: () => 'singular class copy' },
    })
    await waitFor(() => expect(mapSolarText).toHaveBeenCalled())
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start Crafting' }),
      ).toBeEnabled(),
    )
    expect(screen.getByLabelText('Starting item base')).toHaveValue('solar')
  })
  it('adds and removes catalog modifiers, blocks invalid rolls and preserves the started root', async () => {
    await setup()
    fireEvent.change(
      screen.getByRole('combobox', { name: 'Starting rarity' }),
      { target: { value: 'RARE' } },
    )
    chooseLifeGroup()
    const preview = screen.getByRole('region', {
      name: 'Starting item preview',
    })
    expect(preview).toHaveTextContent('+10 to maximum Life')
    fireEvent.change(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
      {
        target: { value: '21' },
      },
    )
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    fireEvent.change(
      screen.getByRole('spinbutton', { name: /Value.*maximum Life/ }),
      {
        target: { value: '18' },
      },
    )
    expect(preview).toHaveTextContent('+18 to maximum Life')
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    fireEvent.click(screen.getByRole('button', { name: /Edit settings/ }))
    fireEvent.click(
      screen.getByRole('button', { name: 'Remove starting modifier 1' }),
    )
    expect(
      within(
        screen.getByRole('region', { name: 'Crafting tree preview' }),
      ).getByText('+18 to maximum Life'),
    ).toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: 'Start Crafting' }))
    expect(
      screen.getByRole('region', { name: 'Crafting tree preview' }),
    ).not.toHaveTextContent('maximum Life')
  })
})
