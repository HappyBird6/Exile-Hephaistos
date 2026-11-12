import { useI18n } from '../../shared/i18n/i18n'
import { AppLink } from '../../shared/navigation/AppLink'
import { communityText } from './messages'
/** Add boards here as they become available; filters belong to each board's content. */
export function CommunityBoardsNav() {
  const { locale } = useI18n(),
    t = communityText(locale)
  const boards = [
    { id: 'sessions', href: '/community', title: t.board, icon: '▤' },
  ]
  return (
    <nav className="wb-history-nav" aria-label={t.board}>
      {boards.map((board) => (
        <AppLink key={board.id} href={board.href} aria-current="page">
          <span aria-hidden="true">{board.icon}</span>
          <span>{board.title}</span>
        </AppLink>
      ))}
    </nav>
  )
}
