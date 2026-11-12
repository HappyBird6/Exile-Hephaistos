import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useAccount, accountScope } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { sessionTitle } from '../crafting/session-library/sessionTitle'
import { SessionDetail } from '../crafting/session-detail/SessionDetail'
import { communityText } from './messages'
import { CommunityShell } from './CommunityShell'
import { boardRequest } from './api'
import type { Options, Post, Summary } from './api'
export function CommunityCompose() {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale)
  const [selected, setSelected] = useState(
    new URLSearchParams(location.search).get('session') ?? '',
  )
  const [title, setTitle] = useState(''),
    [body, setBody] = useState(''),
    [password, setPassword] = useState(''),
    [version, setVersion] = useState(''),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const [requestId] = useState(() => crypto.randomUUID())
  const library = useQuery({
    queryKey: ['community', 'local-sessions', accountScope()],
    queryFn: async () => {
      const repo = new IndexedFilmRepository(
        localStorage,
        indexedDB,
        accountScope(),
      )
      try {
        return await repo.load()
      } finally {
        await repo.close()
      }
    },
  })
  const options = useQuery({
    queryKey: ['community', 'options'],
    queryFn: () => boardRequest<Options>('options'),
  })
  const film =
    library.data?.films.find((f) => f.id === selected) ?? library.data?.films[0]
  const linked = useQuery({
    queryKey: ['community', 'mine', accountScope(), film?.id],
    enabled: !!film,
    queryFn: () =>
      boardRequest<Summary[]>('mine?source=' + encodeURIComponent(film!.id)),
  })
  return (
    <CommunityShell>
      <h1>{t.newPost}</h1>
      <div className="community-compose">
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            if (!film) return
            setBusy(true)
            setError(false)
            try {
              const post = await boardRequest<Post>('posts', {
                method: 'POST',
                body: JSON.stringify({
                  id: requestId,
                  sourceId: film.id,
                  gameVersion: version || options.data?.currentVersion,
                  title: auth?.user ? title : '',
                  body: auth?.user ? body : '',
                  film,
                  password: auth?.user ? null : password,
                  revision: 0,
                }),
              })
              location.href = '/community/post/' + post.id
            } catch {
              setError(true)
            } finally {
              setBusy(false)
            }
          }}
        >
          <label>
            {t.selectSession}
            <select
              value={film?.id ?? ''}
              onChange={(e) => setSelected(e.target.value)}
            >
              {library.data?.films.map((f) => (
                <option key={f.id} value={f.id}>
                  {sessionTitle(f, locale)}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t.version}
            <input
              required
              maxLength={32}
              pattern="[0-9][A-Za-z0-9.\-]*"
              list="game-versions"
              value={version || options.data?.currentVersion || ''}
              onChange={(e) => setVersion(e.target.value)}
            />
            <datalist id="game-versions">
              {options.data?.versions.map((v) => (
                <option key={v} value={v} />
              ))}
            </datalist>
          </label>
          {auth?.user ? (
            <>
              <label>
                {t.title}
                <input
                  maxLength={160}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                />
              </label>
              <label>
                {t.body}
                <textarea
                  maxLength={options.data?.limits.bodyLength}
                  rows={6}
                  value={body}
                  onChange={(e) => setBody(e.target.value)}
                />
              </label>
            </>
          ) : (
            <label>
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
          {linked.data?.map((p) => (
            <a href={'/community/post/' + p.id} key={p.id}>
              {t.viewPost}
            </a>
          ))}
          {error && <p role="alert">{t.error}</p>}
          <button disabled={busy || !film || !options.data}>{t.publish}</button>
          {!film && !library.isPending && <p>{t.empty}</p>}
        </form>
        {film && (
          <SessionDetail
            film={film}
            cursor={film.frames.length - 1}
            className="community-preview"
          />
        )}
      </div>
    </CommunityShell>
  )
}
