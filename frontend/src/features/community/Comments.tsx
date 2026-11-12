import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { boardRequest, type Options } from './api'
type Comment = {
  id: string
  authorId: string
  author: string
  body: string
  revision: number
  createdAt: string
  updatedAt: string
}
type Page = {
  comments: Comment[]
  total: number
  page: number
  pageSize: number
}
export function Comments({ post }: { post: string }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const options = useQuery({
    queryKey: ['community', 'options'],
    queryFn: () => boardRequest<Options>('options'),
  })
  const [page, setPage] = useState(1),
    [body, setBody] = useState(''),
    [editing, setEditing] = useState<Comment | null>(null),
    [id, setId] = useState(() => crypto.randomUUID()),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const key = ['community', 'comments', post]
  const query = useQuery({
    queryKey: [...key, page],
    queryFn: ({ signal }) =>
      boardRequest<Page>('posts/' + post + '/comments?page=' + page, {
        signal,
      }),
  })
  return (
    <section className="community-comments">
      <h2>
        {t.comments} {query.data?.total ?? ''}
      </h2>
      {auth?.user ? (
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            setBusy(true)
            setError(false)
            try {
              await boardRequest(
                'posts/' +
                  post +
                  '/comments' +
                  (editing ? '/' + editing.id : ''),
                {
                  method: editing ? 'PUT' : 'POST',
                  body: JSON.stringify({
                    id: editing?.id ?? id,
                    body,
                    revision: editing?.revision ?? 0,
                  }),
                },
              )
              setBody('')
              setEditing(null)
              setId(crypto.randomUUID())
              setPage(1)
              await client.invalidateQueries({ queryKey: key })
            } catch {
              setError(true)
            } finally {
              setBusy(false)
            }
          }}
        >
          <label>
            {t.comment}
            <textarea
              aria-label={t.comment}
              required
              maxLength={options.data?.limits.commentLength}
              disabled={busy}
              rows={3}
              value={body}
              onChange={(e) => {
                setBody(e.target.value)
                setId(crypto.randomUUID())
              }}
            />
          </label>
          <div className="community-actions">
            <button disabled={busy || !body.trim()}>
              {editing ? t.save : t.send}
            </button>
            {editing && (
              <button
                type="button"
                disabled={busy}
                onClick={() => {
                  setEditing(null)
                  setBody('')
                }}
              >
                {t.cancel}
              </button>
            )}
          </div>
        </form>
      ) : (
        <p>{t.loginRequired}</p>
      )}
      {(error || query.isError) && <p role="alert">{t.error}</p>}
      {query.data?.comments.map((c) => (
        <article key={c.id} className="community-comment">
          <header>
            <a href={'/community?author=' + c.authorId}>{c.author}</a>
            <time>{new Date(c.createdAt).toLocaleString(locale)}</time>
          </header>
          <p className="community-body">{c.body}</p>
          {c.authorId === auth?.user?.id && (
            <div className="community-actions">
              <button
                disabled={busy}
                onClick={() => {
                  setEditing(c)
                  setBody(c.body)
                }}
              >
                {t.edit}
              </button>
              <button
                disabled={busy}
                onClick={async () => {
                  if (!confirm(t.confirmDelete)) return
                  setBusy(true)
                  try {
                    await boardRequest(
                      'posts/' + post + '/comments/' + c.id + '/delete',
                      {
                        method: 'POST',
                        body: JSON.stringify({ revision: c.revision }),
                      },
                    )
                    await client.invalidateQueries({ queryKey: key })
                  } catch {
                    setError(true)
                  } finally {
                    setBusy(false)
                  }
                }}
              >
                {t.delete}
              </button>
            </div>
          )}
        </article>
      ))}
      <nav className="community-actions">
        <button disabled={page === 1} onClick={() => setPage(page - 1)}>
          {t.previous}
        </button>
        <span>{page}</span>
        <button
          disabled={
            !query.data || page * query.data.pageSize >= query.data.total
          }
          onClick={() => setPage(page + 1)}
        >
          {t.next}
        </button>
      </nav>
    </section>
  )
}
