import { useI18n } from '../../../shared/i18n/i18n'
import { abyssWorkbenchMessages } from './messages'
import './abyss-workbench.css'

/** Displays the actual glyph pixels supplied by the user, with a localized accessible name. */
export function UnrevealedRune({ className = '' }: { className?: string }) {
  const { locale } = useI18n()
  return (
    <span
      role="img"
      aria-label={abyssWorkbenchMessages[locale].hidden}
      className={`abyss-unrevealed-rune ${className}`.trim()}
    />
  )
}
