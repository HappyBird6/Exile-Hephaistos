import { useI18n, uiText } from '../../shared/i18n/i18n'
import { AppLink } from '../../shared/navigation/AppLink'
import { communityText } from './messages'
import { revisionText } from './revisionMessages'
/** Keep board navigation separate from equipment filters so more boards can join this rail. */
export function CommunityBoardsNav({
  classes,
  selected,
  onSelect,
}: {
  classes: string[]
  selected: string
  onSelect: (itemClass: string) => void
}) {
  const { locale } = useI18n(),
    t = communityText(locale),
    r = revisionText(locale)
  return (
    <nav className="wb-history-nav community-board-nav" aria-label={t.board}>
      <AppLink href="/community" className="community-board-nav__board">
        <span aria-hidden="true">▤</span>
        <span>{t.board}</span>
      </AppLink>
      <div
        className="community-equipment wb-scrollbar"
        role="group"
        aria-label={r.itemClass}
      >
        <button
          aria-current={!selected ? 'page' : undefined}
          onClick={() => onSelect('')}
        >
          {t.all}
        </button>
        {classes.map((name) => (
          <button
            key={name}
            aria-current={name === selected ? 'page' : undefined}
            onClick={() => onSelect(name)}
          >
            {uiText(name, locale)}
          </button>
        ))}
      </div>
    </nav>
  )
}
