import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { act, render } from '@testing-library/react'
import { expect, it } from 'vitest'
import inputs from './goldens/inputs.json'
import { projectExplicitOptionGroups } from './projectExplicitOptionGroups'
import type { ExplicitOptionProjection } from './types'
import type { ConcreteItem } from '../../workbenchApi'
import type { Definition } from '../../craftingApi'
import { catalystProjection } from '../../catalystQuality'
import { preciseModifierProjection } from '../../sanctification/preciseModifierProjection'
import { localizedModifierText } from '../../localizedModifiers'
import { ItemCard } from '../../ItemCard'
import type { ItemCardLine } from '../../itemCardData'
import { locales, setLocale } from '../../../../shared/i18n/i18n'
import type { Locale } from '../../../../shared/i18n/i18n'

const definitions = inputs.definitions as unknown as Record<string, Definition>
const cases = inputs.cases as unknown as {
  id: string
  scope: string
  oracle: boolean
  state: ConcreteItem
}[]
const goldenDirectory = resolve(
  'src/features/crafting/chaos-repeat/display/goldens',
)
const update = process.env.UPDATE_CHAOS_DISPLAY_GOLDENS === '1'

function freeze(value: unknown) {
  if (value && typeof value === 'object' && !Object.isFrozen(value)) {
    Object.freeze(value)
    for (const child of Object.values(value)) freeze(child)
  }
}

/** Independent Page(non-Alt) -> actual ItemCard oracle. No projector output is
 * supplied to the renderer; exclude labels/detail/placeholder DOM afterwards. */
function cardTexts(state: ConcreteItem, locale: Locale) {
  act(() => setLocale(locale))
  const modifiers: ItemCardLine[] = [
    ...state.implicits,
    ...state.explicits,
  ].map((modifier, index) => {
    const definition = definitions[modifier.modifierId]!
    const projection = catalystProjection(
      definition,
      modifier.values,
      state.catalystQuality,
      state,
    )
    const values = state.conditions.includes('SANCTIFIED')
      ? preciseModifierProjection(
          state,
          definition,
          modifier,
          index < state.implicits.length ? 'implicits' : 'explicits',
        ).values
      : projection.values
    return {
      id: modifier.modifierId,
      text: localizedModifierText(definition, values, locale),
      fractured: 'fractured' in modifier && Boolean(modifier.fractured),
      kind:
        index < state.implicits.length
          ? 'implicit'
          : state.desecrated?.some(
                (slot) => slot.modifierId === modifier.modifierId,
              )
            ? 'desecrated'
            : definition.tags?.includes('crafted')
              ? 'crafted'
              : 'explicit',
      affixLabel:
        definition.affixType === 'NONE'
          ? undefined
          : `${definition.affixType === 'PREFIX' ? 'P' : 'S'}${definition.tier}`,
      detail: state.catalystQuality
        ? 'EXCLUDED original roll and projection detail'
        : undefined,
    }
  })
  modifiers.push({
    id: 'source-skill',
    text: 'EXCLUDED base skill',
    kind: 'implicit',
  })
  modifiers.sort((a, b) => {
    const rank = (line: ItemCardLine) =>
      line.fractured
        ? -1
        : line.kind === 'implicit'
          ? 0
          : line.affixLabel?.startsWith('P')
            ? 1
            : 2
    return rank(a) - rank(b)
  })
  for (const slot of state.desecrated ?? []) {
    if (slot.modifierId === null)
      modifiers.push({
        id: slot.id,
        text: 'EXCLUDED hidden placeholder',
        kind: 'desecrated',
        unrevealedDesecrated: true,
        affixLabel: slot.side === 'PREFIX' ? 'P' : 'S',
      })
  }
  const mounted = render(
    <ItemCard
      item={{
        rarity: state.rarity,
        name: 'EXCLUDED item name',
        base: 'EXCLUDED base name',
        itemClass: 'EXCLUDED class',
        itemLevel: state.itemLevel,
        properties: [{ id: 'augment', text: 'EXCLUDED augment effect' }],
        requirements: [{ id: 'requirements', text: 'EXCLUDED requirements' }],
        flags: [{ id: 'flag', text: 'EXCLUDED flag' }],
        modifiers,
      }}
    />,
  )
  const texts = [
    ...mounted.container.querySelectorAll<HTMLElement>(
      '.item-card__line--explicit, .item-card__line--crafted, .item-card__line--desecrated',
    ),
  ]
    .filter((row) => !row.querySelector('.abyss-unrevealed-rune'))
    .map((row) => {
      const sentence = (row.querySelector('summary') ?? row).cloneNode(
        true,
      ) as HTMLElement
      sentence
        .querySelectorAll('.item-card__affix, .sr-only')
        .forEach((label) => label.remove())
      return sentence.textContent ?? ''
    })
  expect(
    mounted.container.querySelectorAll('.abyss-unrevealed-rune'),
  ).toHaveLength(
    (state.desecrated ?? []).filter((slot) => slot.modifierId === null).length,
  )
  mounted.unmount()
  return texts
}

const outputs: {
  inputId: string
  locale: Locale
  result: ExplicitOptionProjection
}[] = []
for (const input of cases) {
  for (const locale of locales) {
    it(`${input.id}: ${locale} matches the non-Alt ItemCard display and preserves source`, () => {
      const state = structuredClone(input.state)
      const original = structuredClone(state)
      freeze(state)
      freeze(definitions)
      // Intentionally choose a different ambient locale: helper must use its argument.
      act(() => setLocale(locale === 'en' ? 'es' : 'en'))
      const result = projectExplicitOptionGroups(state, definitions, locale)
      expect(state).toEqual(original)
      if (input.oracle)
        expect(result.groups.map((group) => group.text)).toEqual(
          cardTexts(state, locale),
        )
      for (const group of result.groups) {
        expect(group.lines).toEqual(group.text.split('\n'))
        expect(group.text).not.toContain('EXCLUDED')
      }
      outputs.push({ inputId: input.id, locale, result })
      if (!update) {
        const golden = JSON.parse(
          readFileSync(resolve(goldenDirectory, 'expected.json'), 'utf8'),
        ) as { cases: typeof outputs }
        expect(result).toEqual(
          golden.cases.find(
            (row) => row.inputId === input.id && row.locale === locale,
          )?.result,
        )
      }
    })
  }
}

it('exposes diagnostics without treating an affix name or source units as an option sentence', () => {
  const result = projectExplicitOptionGroups(
    cases.find((input) => input.id === 'unbound-source-units')!.state,
    definitions,
    'ko',
  )
  expect(result.groups[0]?.textKind).toBe('source-units')
  expect(result.issues.map((issue) => issue.code)).toEqual([
    'UNVERIFIED_TEMPLATE_BINDING',
    'ENGLISH_TEXT_FALLBACK',
    'SOURCE_UNIT_FALLBACK',
  ])
  const hidden = projectExplicitOptionGroups(
    cases.find((input) => input.id === 'revealed-and-hidden')!.state,
    definitions,
    'ko',
  )
  expect(hidden.groups).toHaveLength(1)
  expect(hidden.issues.map((issue) => issue.code)).toContain(
    'UNREVEALED_DESECRATED_SLOT',
  )
  expect(hidden.issues.map((issue) => issue.code)).not.toContain(
    'ENGLISH_REVEAL_TEMPLATE_FALLBACK',
  )
  const numeric = structuredClone(
    cases.find((input) => input.id === 'synthetic-negative-tie')!.state,
  )
  numeric.conditions = ['SANCTIFIED']
  const failed = projectExplicitOptionGroups(numeric, definitions, 'en')
  expect(failed.groups).toHaveLength(0)
  expect(failed.issues.map((issue) => issue.code)).toEqual([
    'NONINTEGER_SOURCE_ROLL',
    'DISPLAY_PROJECTION_UNAVAILABLE',
  ])
  numeric.explicits[0]!.values = { 'spell_damage_+%': Number.NaN }
  expect(
    projectExplicitOptionGroups(numeric, definitions, 'en').issues[0]?.code,
  ).toBe('MISSING_OR_NONFINITE_STAT_VALUE')
  const source = cases.find(
    (input) => input.id === 'synthetic-positive-tie',
  )!.state
  expect(
    projectExplicitOptionGroups(
      source,
      {
        ...definitions,
        'amulet:prefix:adept-s': definitions['amulet:prefix:athlete-s']!,
      },
      'en',
    ).issues[0]?.code,
  ).toBe('DEFINITION_ID_MISMATCH')
})

it('keeps fractured suffix/prefix source order ahead of ordinary prefixes and preserves compound LF groups', () => {
  const ordered = projectExplicitOptionGroups(
    cases.find((input) => input.id === 'protected-order-divisor')!.state,
    definitions,
    'en',
  )
  expect(ordered.groups.map((group) => group.explicitIndex)).toEqual([0, 2, 1])
  expect(ordered.groups.map((group) => group.fractured)).toEqual([
    true,
    true,
    false,
  ])
  const compound = projectExplicitOptionGroups(
    cases.find((input) => input.id === 'compound-template-newline')!.state,
    definitions,
    'en',
  )
  expect(compound.groups[0]?.lines).toEqual([
    '11% increased Armour',
    '+9 to maximum Life',
  ])
})

it('preserves the raw negative-zero sign even though display renders zero', () => {
  const state = cases.find(
    (input) => input.id === 'synthetic-negative-zero',
  )!.state
  expect(Object.is(state.explicits[0]!.values['spell_damage_+%'], -0)).toBe(
    true,
  )
  expect(
    projectExplicitOptionGroups(state, definitions, 'en').groups[0]?.text,
  ).not.toContain('-0')
  expect(Object.is(state.explicits[0]!.values['spell_damage_+%'], -0)).toBe(
    true,
  )
})

it('records or verifies the machine-readable output and exact display source hashes', () => {
  const files = [
    ...new Set([
      ...Object.values(inputs.definitionOrigins).filter(
        (path) => path.startsWith('backend/') || path.startsWith('frontend/'),
      ),
      'frontend/src/features/crafting/CraftingPage.tsx',
      'frontend/src/features/crafting/ItemCard.tsx',
      'frontend/src/features/crafting/localizedModifiers.ts',
      'frontend/src/features/crafting/catalystQuality.ts',
      'frontend/src/features/crafting/item-text/modifierClipboardText.ts',
      'frontend/src/features/crafting/qualityRoundingPolicy.ts',
      'frontend/src/features/crafting/basicJewel.ts',
      'frontend/src/features/crafting/sanctification/preciseModifierProjection.ts',
      'frontend/src/features/crafting/numeric-display/attributeNumbers.ts',
      'frontend/src/shared/i18n/modifierTemplates.json',
      'frontend/src/shared/i18n/i18n.ts',
      'frontend/package-lock.json',
      'frontend/src/features/crafting/chaos-repeat/display/goldens/inputs.json',
      'frontend/src/features/crafting/chaos-repeat/display/projectExplicitOptionGroups.ts',
      'frontend/src/features/crafting/chaos-repeat/display/types.ts',
    ]),
  ].sort()
  const hashes = Object.fromEntries(
    files.map((file) => [
      file,
      createHash('sha256')
        .update(readFileSync(resolve('..', file)))
        .digest('hex'),
    ]),
  )
  expect(outputs).toHaveLength(cases.length * locales.length)
  if (update) {
    writeFileSync(
      resolve(goldenDirectory, 'expected.json'),
      JSON.stringify({ schemaVersion: 1, cases: outputs }, null, 2) + '\n',
    )
    writeFileSync(
      resolve(goldenDirectory, 'sources.json'),
      JSON.stringify(
        {
          schemaVersion: 1,
          baselineCommit: '3f4495e664d8112011cc5dcd9f17330446167a97',
          runtime: {
            node: process.versions.node,
            icu: process.versions.icu,
            cldr: process.versions.cldr,
            unicode: process.versions.unicode,
          },
          files: hashes,
        },
        null,
        2,
      ) + '\n',
    )
  } else {
    const manifest = JSON.parse(
      readFileSync(resolve(goldenDirectory, 'sources.json'), 'utf8'),
    ) as { files: typeof hashes }
    expect(hashes).toEqual(manifest.files)
  }
})
