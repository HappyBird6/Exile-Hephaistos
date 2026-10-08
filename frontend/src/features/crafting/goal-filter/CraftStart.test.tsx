import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
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
describe('Craft Support starting screen', () => {
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
    await screen.findByText(/Unknown or unsupported base/)
    expect(screen.getByLabelText('Starting item base')).toHaveValue('')
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    expect(text).toHaveValue('unknown original')
    vi.mocked(parseItemText).mockResolvedValueOnce(parsed('Iron Ring', 'Rings'))
    fireEvent.paste(text, { clipboardData: { getData: () => 'copied ring' } })
    await screen.findByText(
      /Pasted-item catalog mapping currently supports Solar/,
    )
    expect(screen.getByLabelText('Starting item base')).toHaveValue('ring')
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
  })
  it('keeps a late validation response from replacing a newly selected base', async () => {
    await setup()
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
  it('adds and removes catalog modifiers, blocks invalid rolls and preserves the started root', async () => {
    await setup()
    fireEvent.click(screen.getByRole('button', { name: 'Add modifier' }))
    fireEvent.change(screen.getByLabelText('Starting modifier tier'), {
      target: { value: 'p' },
    })
    const preview = screen.getByRole('region', {
      name: 'Starting item preview',
    })
    expect(preview).toHaveTextContent('+10 to maximum Life')
    fireEvent.change(screen.getByLabelText('life'), { target: { value: '21' } })
    expect(
      screen.getByRole('button', { name: 'Start Crafting' }),
    ).toBeDisabled()
    fireEvent.change(screen.getByLabelText('life'), { target: { value: '18' } })
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
