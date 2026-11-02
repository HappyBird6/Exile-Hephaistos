import { readFileSync } from 'node:fs'
import { useRef } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import {
  baseAffixCapacity,
  baseRarityAllowed,
  baseStartingRarity,
  baseTexts,
  clipboardBase,
  newBaseSelectionAllowed,
  selectableBaseKeys,
} from '../baseRegistry'
import { topBase } from '../topBases'
import { concreteInitial } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import type { Definition, Initial } from '../craftingApi'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import {
  LocalFilmRepository,
  currentFrame,
  emptyFilms,
  historyStorageKey,
  startFilm,
  verifiedHistoryState,
} from '../workbenchHistory'
import { basePreview, workbenchBaseChoices } from './baseChoices'
import { WorkbenchBasePicker } from './WorkbenchBasePicker'
import { startBases } from '../goal-filter/startItem'

function initial(key: string): Initial {
  const base = topBase(key)!
  const catalog = JSON.parse(
    readFileSync(
      `../backend/src/main/resources/catalog/${base.pool}/catalog.json`,
      'utf8',
    ),
  )
  return {
    ruleVersion: 'fixture',
    rulesetIdentity: 'fixture-ruleset',
    metadata: catalog.metadata,
    id: 'fixture',
    actions: [],
    baseRules: catalog.base,
    state: {
      snapshotId: catalog.metadata.snapshotId,
      baseItemId: base.id,
      itemLevel: 82,
      rarity: baseStartingRarity(key),
      conditions: [],
      modifierIds: [],
      implicits: [
        {
          modifierId: base.implicitModifierId!,
          values: Object.fromEntries(
            base.implicitStats!.map((stat) => [stat.id, stat.max]),
          ),
        },
      ],
    },
    modifiers: Object.fromEntries(
      catalog.modifiers.map((definition: Definition) => [
        definition.id,
        definition,
      ]),
    ),
  }
}

it('offers all requested missing accessories and preserves excluded identities only for existing/imported records', () => {
  for (const key of [
    'jade-amulet',
    'lapis-amulet',
    'gold-amulet',
    'twisted-amulet',
    'distorted-amulet',
    'absent-amulet',
    'portent-amulet',
    'lament-amulet',
    'dusk-amulet',
    'penumbra-amulet',
    'gloam-amulet',
    'corona-amulet',
    'tenebrous-amulet',
    'lazuli-ring',
    'sapphire-ring',
    'topaz-ring',
    'emerald-ring',
    'gold-ring',
    'unset-ring',
  ])
    expect(selectableBaseKeys).toContain(key)
  for (const key of ['kinetic', 'mnemonic', 'vitalic', 'two-stone-fire-cold']) {
    expect(newBaseSelectionAllowed(key)).toBe(false)
    expect(selectableBaseKeys).not.toContain(key)
    expect(startBases.map(([key]) => key)).not.toContain(key)
    expect(
      clipboardBase(
        baseTexts[key as keyof typeof baseTexts].split('\n').at(-1)!,
        'Rings',
      ),
    ).toBe(key)
  }
})

it.each([
  'dusk-amulet',
  'gloam-amulet',
  'penumbra-amulet',
  'tenebrous-amulet',
  'corona-amulet',
  'sinew-belt',
  'invoking-belt',
  'forking-belt',
])(
  'initializes and validates %s as Rare without erasing incompatible saved bytes',
  (key) => {
    const base = topBase(key)!
    expect(baseStartingRarity(key)).toBe('RARE')
    expect(baseTexts[key as keyof typeof baseTexts]).toContain('Rarity: Rare')
    expect(
      basePreview(
        workbenchBaseChoices('en').find((choice) => choice.key === key)!,
        82,
        'en',
      ).rarity,
    ).toBe('RARE')
    const selected = initial(key)
    const root = concreteInitial(selected)
    expect(verifiedHistoryState(root, selected)).toBe(true)
    for (const rarity of ['NORMAL', 'MAGIC'] as const) {
      expect(baseRarityAllowed(base.id, rarity)).toBe(false)
      const invalid = { ...root, rarity }
      expect(supportsConcreteStateShape(invalid)).toBe(false)
      const saved = startFilm(
        emptyFilms(),
        invalid,
        'old',
        'fixture-ruleset',
        'exact original',
      )
      const repository = new LocalFilmRepository(localStorage)
      repository.save(saved)
      const before = localStorage.getItem(historyStorageKey)
      expect(currentFrame(repository.load())!.state).toEqual(invalid)
      expect(verifiedHistoryState(invalid, selected)).toBe(false)
      expect(localStorage.getItem(historyStorageKey)).toBe(before)
    }
  },
)

it('uses sourced asymmetric prefix/suffix limits in history verification', () => {
  const selected = initial('dusk-amulet')
  const root = concreteInitial(selected)
  expect(baseAffixCapacity(root.baseItemId, 'RARE')).toEqual({
    prefixes: 4,
    suffixes: 2,
  })
  const families = new Set<string>()
  const choose = (side: string, count: number) =>
    Object.values(selected.modifiers)
      .filter((definition) => {
        if (
          definition.affixType !== side ||
          definition.requiredItemLevel! > 82 ||
          definition.familyIds.some((id) => families.has(id))
        )
          return false
        definition.familyIds.forEach((id) => families.add(id))
        return true
      })
      .slice(0, count)
      .map((definition) => ({
        modifierId: definition.id,
        values: Object.fromEntries(
          definition.stats!.map((stat) => [stat.id, stat.max]),
        ),
      }))
  const prefixes = choose('PREFIX', 5),
    suffixes = choose('SUFFIX', 3)
  const valid: ConcreteItem = {
    ...root,
    explicits: [...prefixes.slice(0, 4), ...suffixes.slice(0, 2)],
  }
  expect(verifiedHistoryState(valid, selected)).toBe(true)
  expect(verifiedHistoryState({ ...root, explicits: prefixes }, selected)).toBe(
    false,
  )
  expect(
    verifiedHistoryState(
      { ...root, explicits: [...prefixes.slice(0, 3), ...suffixes] },
      selected,
    ),
  ).toBe(false)
  expect(baseAffixCapacity(topBase('penumbra-amulet')!.id, 'RARE')).toEqual({
    prefixes: 5,
    suffixes: 1,
  })
})

it('keeps each armour implicit identity distinct and resolves same-name armour only from exact implicit text', () => {
  const variants = workbenchBaseChoices('en').filter(
    (choice) => choice.name === 'Shrouded Mail',
  )
  expect(variants).toHaveLength(3)
  expect(new Set(variants.map((choice) => choice.baseItemId)).size).toBe(3)
  expect(new Set(variants.map((choice) => choice.variantLabel)).size).toBe(3)
  expect(clipboardBase('Shrouded Mail', 'Body Armours')).toBeUndefined()
  for (const element of ['Fire', 'Cold', 'Lightning'])
    expect(
      clipboardBase(
        'Shrouded Mail',
        'Body Armours',
        `+23% to ${element} Resistance (implicit)`,
      ),
    ).toBe(`shrouded-mail-${element.toLowerCase()}`)
  expect(
    clipboardBase(
      'Shrouded Mail',
      'Body Armours',
      '+23% to Fire Resistance (implicit)\n+23% to Cold Resistance (implicit)',
    ),
  ).toBeUndefined()
})

it('uses one source-valid temporary skill example with an explicit policy label in all six locales', () => {
  for (const key of ['lament-amulet', 'portent-amulet', 'absent-amulet'])
    for (const locale of ['en', 'ko', 'ja', 'zh-CN', 'zh-TW', 'es'] as const) {
      const base = topBase(key)!
      expect(base.skillLines![locale]).toHaveLength(1)
      expect(base.sourceProperties![locale]![0]).toContain(
        base.skillLines![locale]![0],
      )
      expect(base.temporaryBaseOption?.kind).toBe(
        'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE',
      )
      expect(base.skillLines![locale]![0]!.length).toBeLessThan(110)
    }
})

it('reserves enough card and image space for sparse subcategories and long variant names without changing the global ItemCard', () => {
  const css =
    readFileSync(
      'src/features/crafting/base-picker/left-shelf-overlay.css',
      'utf8',
    ) +
    readFileSync('src/features/crafting/base-picker/base-picker.css', 'utf8')
  function Picker() {
    const anchor = useRef<HTMLButtonElement>(null)
    return (
      <>
        <style>{css}</style>
        <button ref={anchor}>Open</button>
        <WorkbenchBasePicker
          id="audit-picker"
          anchor={anchor}
          open
          onOpenChange={vi.fn()}
          value="solar"
          onChange={vi.fn()}
          level="82"
          onLevelChange={vi.fn()}
          onPlace={vi.fn()}
        />
      </>
    )
  }
  render(<Picker />)
  fireEvent.change(screen.getByRole('searchbox'), {
    target: { value: 'Shrouded Mail' },
  })
  const cards = screen.getAllByRole('button', { name: /Shrouded Mail/ })
  expect(cards).toHaveLength(3)
  for (const card of cards) {
    const art = card.querySelector('.wb-base-picker__art')!
    expect(getComputedStyle(card).minHeight).toBe('56px')
    expect(getComputedStyle(art).width).toBe('40px')
    expect(getComputedStyle(art).height).toBe('40px')
    expect(getComputedStyle(art).flexShrink).toBe('0')
    expect(card.querySelector('.wb-base-picker__label')).toHaveTextContent(
      'Resistance',
    )
  }
  fireEvent.change(screen.getByRole('searchbox'), {
    target: { value: 'Dusk Amulet' },
  })
  expect(screen.getAllByRole('button', { name: /Dusk Amulet/ })).toHaveLength(1)
  expect(
    getComputedStyle(screen.getByRole('button', { name: /Dusk Amulet/ }))
      .minHeight,
  ).toBe('56px')
})
