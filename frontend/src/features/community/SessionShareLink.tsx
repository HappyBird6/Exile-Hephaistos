import { useQuery } from '@tanstack/react-query'
import { useI18n } from '../../shared/i18n/i18n'
import { accountScope } from '../account/auth'
import { communityText } from './messages'
import { boardRequest } from './api'
import type { Summary } from './api'
export function SessionShareLink({ id }: { id: string }) {
  const { locale } = useI18n(),
    t = communityText(locale)
  const linked = useQuery({
    queryKey: ['community', 'mine', accountScope(), id],
    queryFn: () =>
      boardRequest<Summary[]>('mine?source=' + encodeURIComponent(id)),
    staleTime: 30000,
    retry: false,
  })
  return linked.data?.length ? (
    <a href={'/community/post/' + linked.data[0]!.id}>{t.viewPost}</a>
  ) : (
    <a href={'/community/new?session=' + encodeURIComponent(id)}>{t.newPost}</a>
  )
}
