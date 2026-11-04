import templates from '../../../../shared/i18n/modifierTemplates.json'
import type { Locale } from '../../../../shared/i18n/i18n'
import type { Definition } from '../../craftingApi'
import { catalystProjection } from '../../catalystQuality'
import { preciseModifierProjection } from '../../sanctification/preciseModifierProjection'
import { localizedModifierText } from '../../localizedModifiers'
import { formatAttributeText } from '../../numeric-display/attributeNumbers'
import { rolledText } from '../../workbenchApi'
import type { ConcreteItem } from '../../workbenchApi'
import type {
  DisplayIssueCode,
  ExplicitOptionGroup,
  ExplicitOptionProjection,
} from './types'

type Binding = {
  englishText: string
  stats: NonNullable<Definition['stats']>
  template: string
  valueStats?: { id: string; divisor: number }[]
}
const bindings: Record<string, Binding> = templates.definitions
const translations: Record<
  Locale,
  Record<string, unknown>
> = templates.templates

/** Mirror only the existing private binding identity check to diagnose fallback.
 * Text itself always comes from the existing production display functions. */
function bindingFor(definition: Definition) {
  const binding = bindings[definition.id]
  return binding &&
    binding.englishText === definition.text &&
    binding.stats.length === (definition.stats ?? []).length &&
    binding.stats.every((stat, index) => {
      const actual = definition.stats?.[index]
      return (
        actual?.id === stat.id &&
        actual.min === stat.min &&
        actual.max === stat.max
      )
    })
    ? binding
    : undefined
}

export function projectExplicitOptionGroups(
  state: ConcreteItem,
  definitions: Readonly<Record<string, Definition>>,
  locale: Locale,
): ExplicitOptionProjection {
  const result: ExplicitOptionProjection = { locale, groups: [], issues: [] }
  state.explicits.forEach((modifier, explicitIndex) => {
    const modifierId = modifier.modifierId
    const issue = (code: DisplayIssueCode) =>
      result.issues.push({ code, modifierId, explicitIndex })
    const definition = definitions[modifierId]
    if (!definition) {
      issue('MISSING_DEFINITION')
      return
    }
    if (definition.id !== modifierId) {
      issue('DEFINITION_ID_MISMATCH')
      return
    }
    if (
      definition.affixType !== 'PREFIX' &&
      definition.affixType !== 'SUFFIX'
    ) {
      issue('EXPLICIT_WITHOUT_AFFIX')
      return
    }
    if (
      (definition.stats ?? []).some(
        (stat) => !Number.isFinite(modifier.values[stat.id]),
      ) ||
      Object.values(modifier.values).some((value) => !Number.isFinite(value))
    ) {
      issue('MISSING_OR_NONFINITE_STAT_VALUE')
      return
    }
    if (!Object.values(modifier.values).every(Number.isSafeInteger))
      issue('NONINTEGER_SOURCE_ROLL')
    try {
      const projection = catalystProjection(
        definition,
        modifier.values,
        state.catalystQuality,
        state,
      )
      const values = state.conditions.includes('SANCTIFIED')
        ? preciseModifierProjection(state, definition, modifier, 'explicits')
            .values
        : projection.values
      if (
        projection.status === 'UNREVIEWED_NUMERIC_SEMANTICS' ||
        projection.status === 'JEWEL_EFFECT_AND_QUALITY_PROVISIONAL'
      )
        issue(projection.status)
      const binding = bindingFor(definition)
      if (!binding) issue('UNVERIFIED_TEMPLATE_BINDING')
      if (!binding || !translations[locale][binding.template]) {
        issue(
          binding &&
            definition.id.startsWith('reveal:') &&
            translations.en[binding.template]
            ? 'ENGLISH_REVEAL_TEMPLATE_FALLBACK'
            : 'ENGLISH_TEXT_FALLBACK',
        )
      }
      const raw = rolledText(definition, values)
      const sourceUnits = `${definition.name}: ${Object.entries(values)
        .map(
          ([id, value]) =>
            `${id.replaceAll('_', ' ')} = ${value} (source units)`,
        )
        .join(', ')}`
      const translated =
        binding &&
        (translations[locale][binding.template] ??
          (definition.id.startsWith('reveal:')
            ? translations.en[binding.template]
            : undefined))
      const converted =
        translated &&
        binding &&
        (binding.valueStats
          ? binding.valueStats.every((stat) => Number.isFinite(values[stat.id]))
          : /^(ruby|emerald|sapphire|diamond):/.test(definition.id))
      const rawFallback = raw === sourceUnits && !converted
      if (
        translated &&
        binding &&
        !binding.valueStats &&
        /^(ruby|emerald|sapphire|diamond):/.test(definition.id) &&
        Object.values(values).some((value) => !Number.isSafeInteger(value))
      )
        issue('UNRESOLVED_ROLL_TEXT')
      if (rawFallback) issue('SOURCE_UNIT_FALLBACK')
      // Page's ordinary (non-Alt) text, followed by ItemCard.Lines' final pass.
      // The second pass deliberately retains existing locale edge cases.
      const text = formatAttributeText(
        localizedModifierText(definition, values, locale),
        locale,
      )
      if (/\{v\d+\}/.test(text)) issue('UNRESOLVED_TEMPLATE_VALUE')
      result.groups.push({
        explicitIndex,
        modifierId,
        affixType: definition.affixType,
        fractured: Boolean(modifier.fractured),
        text,
        lines: text.split('\n'),
        textKind: rawFallback ? 'source-units' : 'option-sentence',
      })
    } catch {
      issue('DISPLAY_PROJECTION_UNAVAILABLE')
    }
  })
  const rank = (group: ExplicitOptionGroup) =>
    group.fractured ? -1 : group.affixType === 'PREFIX' ? 1 : 2
  result.groups.sort((a, b) => rank(a) - rank(b))
  for (const slot of state.desecrated ?? []) {
    if (slot.modifierId === null)
      result.issues.push({
        code: 'UNREVEALED_DESECRATED_SLOT',
        slotId: slot.id,
      })
  }
  return result
}
