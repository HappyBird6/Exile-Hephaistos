import { gameTerm, matchesGameName } from '../../../shared/i18n/i18n'
import type { Locale } from '../../../shared/i18n/i18n'
import descriptions from '../materialTooltips.json'
import { augmentFeedbackLines } from '../augmentFeedback'

/** Match names as before, plus the description lines actually shown in feedback. */
export function matchesMaterial(
  resource: { id: string; name: string },
  query: string,
  locale: Locale,
) {
  if (matchesGameName(resource.id, resource.name, query, locale)) return true
  const translated = gameTerm(resource.id, locale).data
  const catalog = descriptions as Record<string, { lines: string[] }>
  const lines =
    augmentFeedbackLines(resource.id, locale) ??
    (translated?.lines.length
      ? translated.lines
      : (catalog[resource.id]?.lines ?? []))
  const needle = query.trim().toLocaleLowerCase(locale)
  return lines.some(
    (line) =>
      !line.startsWith('Stack Size:') &&
      line.toLocaleLowerCase(locale).includes(needle),
  )
}
