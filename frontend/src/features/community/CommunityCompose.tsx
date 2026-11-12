import { AppLink } from '../../shared/navigation/AppLink'
import { navigate, useAppLocation } from '../../shared/navigation/navigation'
import { publicFilm, exceedsShareLimit } from './sharedSnapshot'
import { revisionText } from './revisionMessages'
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount, accountScope } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { sessionTitle } from '../crafting/session-library/sessionTitle'
import { sessionLibraryMessages } from '../crafting/session-library/sessionLibraryMessages'
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { historyResources } from '../crafting/session-history/resources'
import type { Film } from '../crafting/workbenchHistory'
import { communityText } from './messages'
import { CommunityShell } from './CommunityShell'
import { boardRequest } from './api'
import type { Post } from './api'
import { communityQueries, invalidatePost } from './queries'
const historyHref = '/?workbench=history'

export function CommunityCompose() {
  useAppLocation()
  const { locale } = useI18n(),
    t = communityText(locale)
  const selected = new URLSearchParams(location.search).get('session')
  const owner = accountScope()
  const library = useQuery({
    queryKey: ['community', 'local-sessions', owner],
    enabled: !!selected,
    // Read once per visit. Focus/reconnect must not replace the session being reviewed.
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    gcTime: 0,
    queryFn: async () => {
      const repo = new IndexedFilmRepository(
        localStorage,
        globalThis.indexedDB,
        owner,
      )
      try {
        return await repo.load()
      } finally {
        await repo.close()
      }
    },
  })
  const film = library.data?.films.find((f) => f.id === selected)
  return (
    <CommunityShell>
      <article className="community-post">
        <AppLink className="community-back" href={historyHref}>
          ← {sessionLibraryMessages[locale].history}
        </AppLink>
        <header className="community-detail-header">
          <h1>{t.newPost}</h1>
          {film && (
            <p className="community-compose-source">
              {sessionTitle(film, locale)}
            </p>
          )}
        </header>
        {selected && library.isPending ? (
          <p role="status">…</p>
        ) : library.isError ? (
          <p role="alert">{t.error}</p>
        ) : film ? (
          <ShareForm key={film.id} film={film} />
        ) : (
          <p role="status">{t.sourceMissing}</p>
        )}
      </article>
    </CommunityShell>
  )
}
function ShareForm({ film }: { film: Film }) {
  const client = useQueryClient(),
    auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale)
  const [cursor, setCursor] = useState(film.frames.length - 1)
  const [title, setTitle] = useState(''),
    [body, setBody] = useState(''),
    [password, setPassword] = useState(''),
    [version, setVersion] = useState<string | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const [requestId] = useState(() => crypto.randomUUID())
  const options = useQuery(communityQueries.options())
  const linked = useQuery(communityQueries.linked(film.id, accountScope()))
  const oversized =
    !!options.data && exceedsShareLimit(film, options.data.limits)
  const alreadyShared = !!linked.data?.length
  const canPublish =
    !busy &&
    !!options.data &&
    !options.isError &&
    !oversized &&
    linked.isSuccess &&
    !alreadyShared
  return (
    <form
      className="community-compose"
      onSubmit={async (e) => {
        e.preventDefault()
        if (!canPublish) return
        setBusy(true)
        setError(false)
        try {
          const post = await boardRequest<Post>('posts', {
            method: 'POST',
            body: JSON.stringify({
              id: requestId,
              sourceId: film.id,
              gameVersion: version ?? options.data!.currentVersion,
              title: auth?.user ? title : '',
              body: auth?.user ? body : '',
              film: publicFilm(film),
              password: auth?.user ? null : password,
              revision: 0,
            }),
          })
          await invalidatePost(client, post, accountScope())
          client.setQueryData(communityQueries.post(post.id).queryKey, post)
          void client.invalidateQueries({
            queryKey: communityQueries.options().queryKey,
            refetchType: 'none',
          })
          navigate('/community/post/' + post.id)
        } catch {
          setError(true)
        } finally {
          setBusy(false)
        }
      }}
    >
      <div className="community-post-content">
        <SessionDetail
          film={film}
          cursor={cursor}
          onNavigate={setCursor}
          resources={historyResources}
          showOverview={false}
          className="community-session-detail"
          disabled={busy}
        />
        <fieldset
          className="community-form community-compose-fields"
          disabled={busy || alreadyShared}
        >
          {alreadyShared ? (
            <AppLink
              className="community-existing-post"
              href={'/community/post/' + linked.data![0]!.id}
            >
              {t.viewPost}
            </AppLink>
          ) : (
            <>
              <div
                className={
                  'community-compose-metadata' +
                  (auth?.user ? ' community-compose-metadata--member' : '')
                }
              >
                {auth?.user && (
                  <label>
                    {t.title}
                    <input
                      maxLength={160}
                      value={title}
                      onChange={(e) => setTitle(e.target.value)}
                    />
                  </label>
                )}
                <label>
                  {t.version}
                  <input
                    required
                    maxLength={32}
                    pattern="[0-9][A-Za-z0-9.\-]*"
                    list="game-versions"
                    value={version ?? options.data?.currentVersion ?? ''}
                    onChange={(e) => setVersion(e.target.value)}
                  />
                </label>
                <datalist id="game-versions">
                  {options.data?.versions.map((v) => (
                    <option key={v} value={v} />
                  ))}
                </datalist>
              </div>
              {auth?.user ? (
                <label className="community-compose-body">
                  {t.body}
                  <textarea
                    maxLength={options.data?.limits.bodyLength}
                    rows={12}
                    value={body}
                    onChange={(e) => setBody(e.target.value)}
                  />
                </label>
              ) : (
                <label className="community-compose-password">
                  {t.password}
                  <input
                    required
                    type="password"
                    autoComplete="new-password"
                    minLength={8}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <small>{t.passwordHint}</small>
                </label>
              )}
            </>
          )}
          {(error || options.isError || linked.isError) && (
            <p role="alert">{t.error}</p>
          )}
          {oversized && <p role="alert">{revisionText(locale).sizeLimit}</p>}
        </fieldset>
      </div>
      <div className="community-post-actions community-actions community-compose-actions">
        {busy ? (
          <button type="button" disabled>
            {t.cancel}
          </button>
        ) : (
          <AppLink href={historyHref}>{t.cancel}</AppLink>
        )}
        {!alreadyShared && (
          <button className="site-button--primary" disabled={!canPublish}>
            {t.publish}
          </button>
        )}
      </div>
    </form>
  )
}
