import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { navigate } from '../../../shared/navigation/navigation'
import { accountScope } from '../../account/auth'
import { communityText } from '../../community/messages'
import { useHistoryInitial } from '../session-history/useHistoryInitial'
import { sessionLibraryMessages } from '../session-library/sessionLibraryMessages'
import type { Film } from '../workbenchHistory'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'
import {
  transferAcquisitionRoute,
  type RouteDestination,
} from './transferAcquisitionRoute'

export function AcquisitionRouteActions({
  film,
  cursor,
}: {
  film: Film
  cursor: number
}) {
  const { locale } = useI18n()
  const copy = acquisitionRouteMessages[locale]
  const { initial } = useHistoryInitial(film.frames[0]!.state)
  const client = useQueryClient()
  const pending = useRef<AbortController | null>(null)
  const [busy, setBusy] = useState(false)
  const [failed, setFailed] = useState(false)
  useEffect(() => () => pending.current?.abort(), [])

  async function open(destination: RouteDestination) {
    if (pending.current) return
    const controller = new AbortController()
    pending.current = controller
    const owner = accountScope()
    setBusy(true)
    setFailed(false)
    try {
      const catalog =
        initial.data ?? (await initial.refetch({ throwOnError: true })).data
      if (!catalog) throw Error('CATALOG_UNAVAILABLE')
      const href = await transferAcquisitionRoute(
        film,
        cursor,
        destination,
        catalog,
        owner,
        controller.signal,
      )
      if (controller.signal.aborted || owner !== accountScope()) return
      client.removeQueries({
        queryKey: ['workbench-history-bootstrap'],
        type: 'inactive',
      })
      client.removeQueries({
        queryKey: ['community', 'local-sessions', owner],
        type: 'inactive',
      })
      navigate(href)
    } catch {
      if (!controller.signal.aborted) setFailed(true)
    } finally {
      pending.current = null
      if (!controller.signal.aborted) setBusy(false)
    }
  }

  return (
    <div className="wb-history-preview__actions" aria-busy={busy}>
      <button
        type="button"
        disabled={busy}
        onClick={() => void open('library')}
      >
        {copy.save}
      </button>
      <button
        type="button"
        disabled={busy}
        onClick={() => void open('simulation')}
      >
        {sessionLibraryMessages[locale].resume}
      </button>
      <button
        type="button"
        disabled={busy}
        onClick={() => void open('community')}
      >
        {communityText(locale).newPost}
      </button>
      {failed && <p role="alert">{copy.transferError}</p>}
    </div>
  )
}
