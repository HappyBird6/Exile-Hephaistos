import { AppLink } from '../../shared/navigation/AppLink'
import { authorHref } from './listNavigation'
export function AuthorLink({
  id,
  name,
  className = '',
}: {
  id: string
  name: string
  className?: string
}) {
  return (
    <AppLink
      className={'community-author ' + className}
      href={authorHref(id, location.pathname, location.search)}
      title={name}
    >
      <svg viewBox="0 0 20 20" aria-hidden="true">
        <circle cx="10" cy="6" r="3" />
        <path d="M4 17v-2a6 6 0 0 1 12 0v2" />
      </svg>
      <span>{name}</span>
    </AppLink>
  )
}
