import { useI18n } from '../../shared/i18n/i18n'
import { serviceText } from './serviceMessages'
export function ServiceMessage({ text }: { text: string }) {
  const { locale, t } = useI18n()
  const display = serviceText(text, locale)
  if (!text) return null
  if (locale !== 'en' && display === text)
    return (
      <span>
        <small>{t('translation.source_text')}: </small>
        <span lang="en">{text}</span>
      </span>
    )
  return <span>{display}</span>
}
