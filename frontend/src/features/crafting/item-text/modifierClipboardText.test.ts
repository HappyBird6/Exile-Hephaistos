import { expect, it } from 'vitest'
import catalog from '../../../shared/i18n/modifierTemplates.json'
import type { Definition } from '../craftingApi'
import {
  modifierClipboardText,
  recognizedClipboardSentence,
} from './modifierClipboardText'
const definitions = Object.fromEntries(
  Object.entries(catalog.definitions).map(([id, binding]) => [
    id,
    {
      id,
      name: catalog.templates.en[
        binding.template as keyof typeof catalog.templates.en
      ].name,
      text: binding.englishText,
      stats: binding.stats,
      tier: 1,
      affixType: 'PREFIX',
      familyIds: ['clipboard-fixture'],
    },
  ]),
) as Record<string, Definition>

it('exports permyriad rolls as the game leech sentence, not source-unit diagnostics', () => {
  const definition = definitions['stocky-mitts:suffix:of-the-arid']!
  expect(
    modifierClipboardText(definition, {
      base_mana_leech_from_physical_attack_damage_permyriad: 630,
    }),
  ).toBe('Leech 6.3(6-6.9)% of Physical Attack Damage as Mana')
})
it('retains enough per-minute precision to recover the original integer instead of rounding the screen value', () => {
  const definition = definitions['amulet:suffix:of-convalescence']!
  expect(
    modifierClipboardText(definition, {
      base_life_regeneration_rate_per_minute: 791,
    }),
  ).toContain('13.183333333333334')
})
it('covers all reviewed definitions at minimum, midpoint and maximum without private stat syntax or bare unresolved placeholders', () => {
  expect(Object.keys(definitions)).toHaveLength(3406)
  for (const definition of Object.values(definitions)) {
    for (const point of [0, 0.5, 1]) {
      const values = Object.fromEntries(
        definition.stats!.map((stat) => [
          stat.id,
          stat.min + Math.floor((stat.max - stat.min) * point),
        ]),
      )
      const original = JSON.stringify({ definition, values })
      const text = modifierClipboardText(definition, values)
      expect(text, definition.id).not.toMatch(
        /source units|\{v\d+\}|local weapon implicit hidden|local display grants/,
      )
      expect(JSON.stringify({ definition, values })).toBe(original)
    }
  }
})
it('recognizes game effect sentences while keeping unknown wording outside the observation preview', () => {
  expect(
    recognizedClipboardSentence(
      'Leech 6.61% of Physical Attack Damage as Mana',
      definitions,
    ),
  ).toBe(true)
  expect(
    recognizedClipboardSentence('Unknown future value 630', definitions),
  ).toBe(false)
})
