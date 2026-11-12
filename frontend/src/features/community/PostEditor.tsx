import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount, accountScope } from '../account/auth'
import { IndexedFilmRepository } from '../crafting/session-library/IndexedFilmRepository'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { boardRequest } from './api'
import type { Post } from './api'
export function PostEditor({ post }: { post: Post }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const [open, setOpen] = useState(false),
    [body, setBody] = useState(post.body),
    [title, setTitle] = useState(post.title),
    [password, setPassword] = useState(''),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const own = post.authorId === null || post.authorId === auth?.user?.id
  const source = useQuery({
    queryKey: ['community', 'source', accountScope(), post.sourceId],
    enabled: open && own,
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
  if (!open)
    return (
      <button onClick={() => setOpen(true)}>
        {t.edit} / {t.update}
      </button>
    )
  return (
    <form
      className="community-editor"
      onSubmit={async (e) => {
        e.preventDefault()
        setBusy(true)
        setError(false)
        try {
          await boardRequest('posts/' + post.id, {
            method: 'PUT',
            body: JSON.stringify({
              id: post.id,
              sourceId: post.sourceId,
              gameVersion: post.gameVersion,
              title: post.authorId ? title : '',
              body: post.authorId ? body : '',
              film: source.data ?? post.film,
              password: post.authorId ? null : password,
              revision: post.revision,
            }),
          })
          setOpen(false)
          await client.invalidateQueries({ queryKey: ['community'] })
        } catch {
          setError(true)
        } finally {
          setBusy(false)
        }
      }}
    >
      <p>{t.updateHint}</p>
      {!source.isPending && !source.data && <p>{t.sourceMissing}</p>}
      {post.authorId ? (
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
              maxLength={5000}
              rows={5}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          </label>
        </>
      ) : (
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
        <button disabled={busy || source.isFetching}>{t.update}</button>
        <button
          type="button"
          disabled={busy}
          onClick={async () => {
            if (!window.confirm(t.confirmDelete)) return
            setBusy(true)
            try {
              await boardRequest('posts/' + post.id + '/delete', {
                method: 'POST',
                body: JSON.stringify({
                  password: post.authorId ? null : password,
                  revision: post.revision,
                }),
              })
              location.href = '/community'
            } catch {
              setError(true)
              setBusy(false)
            }
          }}
        >
          {t.delete}
        </button>
        <button
          type="button"
          disabled={busy}
          onClick={() => {
            setOpen(false)
            setPassword('')
          }}
        >
          {t.cancel}
        </button>
      </div>
    </form>
  )
}
