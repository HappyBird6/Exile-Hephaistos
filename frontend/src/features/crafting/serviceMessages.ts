import { getLocale, translate, uiText } from '../../shared/i18n/i18n'
import type { Locale, MessageKey } from '../../shared/i18n/i18n'
import messages from '../../shared/i18n/messages.json'

// API schemas remain canonical. These exact messages are the versioned adapter
// for older endpoints which have no message code. Unknown text remains original.
const serverCodes = new Map(
  Object.entries(messages.en)
    .filter(([code]) => code.startsWith('server.'))
    .map(([code, text]) => [text, code as MessageKey]),
)
const subjects = new Map(
  Object.entries(messages.en)
    .filter(([code]) => code.startsWith('error.subject.'))
    .map(([code, text]) => [text, code as MessageKey]),
)
export function serviceText(
  text: string,
  locale: Locale = getLocale(),
): string {
  const blocked = text.match(
    /^Craft blocked by rule: (.*) Your item is unchanged; active omens are preserved\.$/,
  )
  if (blocked) {
    const original = blocked[1]!
    const reason = serviceText(original, locale)
    return translate(
      'craft.blocked',
      {
        reason:
          locale !== 'en' && reason === original
            ? `${translate('translation.source_text', {}, locale)}: ${original}`
            : reason,
      },
      locale,
    )
  }
  const line = text.match(/^Line (\d+): (.*)$/)
  if (line)
    return `${translate('ui.line', {}, locale)} ${line[1]}: ${serviceText(line[2]!, locale)}`
  const code = serverCodes.get(text)
  if (code) return translate(code, {}, locale)
  const known = uiText(text, locale)
  if (known !== text) return known
  const match = text.match(/^Could not (verify|load) (.+?)\.(.*)$/)
  const subject = match && subjects.get(match[2]!)
  if (match && subject) {
    const key =
      match[1] === 'load'
        ? 'error.load'
        : match[3]?.includes('Your item is unchanged')
          ? 'error.verify_item'
          : match[3]?.includes('Your inputs are preserved')
            ? 'error.verify_inputs'
            : 'error.verify'
    return translate(key, { subject: translate(subject, {}, locale) }, locale)
  }
  return text
}
