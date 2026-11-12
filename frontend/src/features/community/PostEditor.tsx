import { navigate } from '../../shared/navigation/navigation'
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount, accountScope } from '../account/auth'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { revisionText } from './revisionMessages'
import { publicFilm, exceedsShareLimit } from './sharedSnapshot'
import { boardRequest, type Options, type Post } from './api'
export function PostEditor({ post }: { post: Post }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    r = revisionText(locale),
    client = useQueryClient()
  const [mode, setMode] = useState<'text' | 'session' | 'delete' | null>(null)
  const [body, setBody] = useState(post.body),
    [title, setTitle] = useState(post.title),
    [password, setPassword] = useState(''),
    [version, setVersion] = useState(post.gameVersion),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const own = post.authorId === null || post.authorId === auth?.user?.id
  const options = useQuery({
    queryKey: ['community', 'options'],
    queryFn: () => boardRequest<Options>('options'),
  })
  const source = useQuery({
    queryKey: ['community', 'source', accountScope(), post.sourceId],
    enabled: mode === 'session' && own,
    queryFn: async () => {
      const repo = new IndexedFilmRepository(
        localStorage,
        indexedDB,
        accountScope(),
      )
      try {
        return (
          (await repo.load()).films.find((f) => f.id === post.sourceId) ?? null
        )
      } finally {
        await repo.close()
      }
    },
  })
  if (!own) return null
  const oversized =
    !!source.data &&
    !!options.data &&
    exceedsShareLimit(source.data, options.data.limits)
  if (!mode)
    return (
      <div className="community-actions community-owner-actions">
        {post.authorId && (
          <button
            onClick={() => {
              setTitle(post.title)
              setBody(post.body)
              setError(false)
              setMode('text')
            }}
          >
            {r.editText}
          </button>
        )}
        <button
          onClick={() => {
            setError(false)
            setMode('session')
          }}
        >
          {r.updateSession}
        </button>
        <button
          className="site-button--quiet"
          onClick={() => {
            setError(false)
            setMode('delete')
          }}
        >
          {t.delete}
        </button>
      </div>
    )
  return (
    <form
      className="community-editor community-form site-panel"
      onSubmit={async (e) => {
        e.preventDefault()
        if (mode === 'session' && (!source.data || oversized)) return
        if (mode === 'delete' && !window.confirm(t.confirmDelete)) return
        setBusy(true)
        setError(false)
        try {
          if (mode === 'text')
            await boardRequest('posts/' + post.id + '/text', {
              method: 'PUT',
              body: JSON.stringify({ title, body, revision: post.revision }),
            })
          else if (mode === 'session')
            await boardRequest('posts/' + post.id, {
              method: 'PUT',
              body: JSON.stringify({
                id: post.id,
                sourceId: post.sourceId,
                gameVersion: version,
                title: post.title,
                body: post.body,
                film: publicFilm(source.data!),
                password: post.authorId ? null : password,
                revision: post.revision,
              }),
            })
          else {
            await boardRequest('posts/' + post.id + '/delete', {
              method: 'POST',
              body: JSON.stringify({
                password: post.authorId ? null : password,
                revision: post.revision,
              }),
            })
            navigate('/community')
            return
          }
          setMode(null)
          setPassword('')
          await client.invalidateQueries({ queryKey: ['community'] })
        } catch {
          setError(true)
        } finally {
          setBusy(false)
        }
      }}
    >
      <h2>
        {mode === 'text'
          ? r.editText
          : mode === 'session'
            ? r.updateSession
            : t.delete}
      </h2>
      {mode === 'text' ? (
        <>
          <p className="community-muted">{r.textHint}</p>
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
              aria-label={t.body}
              maxLength={options.data?.limits.bodyLength}
              rows={5}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          </label>
        </>
      ) : mode === 'session' ? (
        <>
          <p className="community-muted">{r.updateHint}</p>
          {!source.isPending && !source.data && (
            <p role="alert">{t.sourceMissing}</p>
          )}
          {oversized && <p role="alert">{r.sizeLimit}</p>}
          <label>
            {t.version}
            <input
              required
              maxLength={32}
              pattern="[0-9][A-Za-z0-9.\-]*"
              value={version}
              onChange={(e) => setVersion(e.target.value)}
            />
          </label>
        </>
      ) : null}
      {!post.authorId && (
        <label>
          {t.password}
          <input
            type="password"
            required
            minLength={8}
            autoComplete="off"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
      )}
      {error && <p role="alert">{t.error}</p>}
      <div className="community-actions">
        <button
          className="site-button--primary"
          disabled={
            busy ||
            (mode === 'session' &&
              (source.isFetching || !source.data || oversized || !options.data))
          }
        >
          {mode === 'delete'
            ? t.delete
            : mode === 'text'
              ? t.save
              : r.updateSession}
        </button>
        <button
          type="button"
          disabled={busy}
          onClick={() => {
            setMode(null)
            setPassword('')
          }}
        >
          {t.cancel}
        </button>
      </div>
    </form>
  )
}
