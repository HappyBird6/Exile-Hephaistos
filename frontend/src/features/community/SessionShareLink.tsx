import { AppLink } from '../../shared/navigation/AppLink'
import { useQuery } from '@tanstack/react-query'
import { useI18n } from '../../shared/i18n/i18n'
import { accountScope } from '../account/auth'
import { communityText } from './messages'
import { communityQueries } from './queries'
export function SessionShareLink({ id }: { id: string }) {
  const { locale } = useI18n(),
    t = communityText(locale)
  const linked = useQuery(communityQueries.linked(id, accountScope()))
  return linked.data?.length ? (
    <AppLink href={'/community/post/' + linked.data[0]!.id}>
      {t.viewPost}
    </AppLink>
  ) : (
    <AppLink href={'/community/new?session=' + encodeURIComponent(id)}>
      {t.newPost}
    </AppLink>
  )
}
