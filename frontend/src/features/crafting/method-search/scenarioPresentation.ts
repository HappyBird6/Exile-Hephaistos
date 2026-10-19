import type { Definition } from '../craftingApi'
import type { GoalFilter } from '../goal-filter/types'
import { localizedModifierValueLabel } from '../localizedModifiers'
import { startInputMessages } from '../goal-filter/startInputMessages'
import { scenarioLocales } from './scenarioCode'
import type { Locale } from '../../../shared/i18n/i18n'
export function scenarioGoalLabels(
  definitions: Record<string, Definition>,
  goal: GoalFilter,
) {
  const byStat = new Map<string, Definition>()
  for (const definition of Object.values(definitions))
    for (const stat of definition.stats ?? []) {
      const key = `hephaistos:v1:${definition.layer?.toLowerCase()}:${stat.id}`
      if (!byStat.has(key)) byStat.set(key, definition)
    }
  return Object.fromEntries(
    scenarioLocales.map((locale) => [
      locale,
      Object.fromEntries(
        goal.groups.flatMap((group) =>
          group.entries.map((entry) => {
            const definition = byStat.get(entry.statId),
              statId = entry.statId.replace(
                /^hephaistos:v1:(explicit|implicit):/,
                '',
              )
            return [
              entry.statId,
              definition
                ? localizedModifierValueLabel(
                    definition,
                    statId,
                    startInputMessages[locale].value,
                    locale,
                  )
                : statId.replaceAll('_', ' '),
            ]
          }),
        ),
      ),
    ]),
  ) as Record<Locale, Record<string, string>>
}
