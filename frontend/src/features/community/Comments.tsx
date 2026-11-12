import { AuthorLink } from './AuthorLink'
import { useRef, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAccount } from '../account/auth'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import { boardRequest, type Comment } from './api'
import { communityQueries } from './queries'
export function Comments({ post }: { post: string }) {
  const auth = useAccount(),
    { locale } = useI18n(),
    t = communityText(locale),
    client = useQueryClient()
  const options = useQuery({
    ...communityQueries.options(),
    enabled: !!auth?.user,
  })
  const [page, setPage] = useState(1),
    [body, setBody] = useState(''),
    [editing, setEditing] = useState<Comment | null>(null),
    [id, setId] = useState(() => crypto.randomUUID()),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(false)
  const input = useRef<HTMLTextAreaElement>(null)
  const key = ['community', 'comments', post]
  const query = useQuery(communityQueries.comments(post, page))
  return (
    <section className="community-comments" aria-label={t.comments}>
      {(error || query.isError) && <p role="alert">{t.error}</p>}
      <div className="community-comment-list">
        {query.data?.comments.map((c) => (
          <article key={c.id} className="community-comment">
            <AuthorLink
              className="community-comment-author"
              id={c.authorId}
              name={c.author}
            />
            <p className="community-body">{c.body}</p>
            <div className="community-comment-meta">
              <time dateTime={c.createdAt}>
                {new Date(c.createdAt).toLocaleString(locale)}
              </time>
              {c.authorId === auth?.user?.id && (
                <div className="community-actions">
                  <button
                    disabled={busy}
                    onClick={() => {
                      setEditing(c)
                      setBody(c.body)
                      input.current?.focus()
                    }}
                  >
                    {t.edit}
                  </button>
                  <button
                    disabled={busy}
                    onClick={async () => {
                      if (!confirm(t.confirmDelete)) return
                      setBusy(true)
                      setError(false)
                      try {
                        await boardRequest(
                          'posts/' + post + '/comments/' + c.id + '/delete',
                          {
                            method: 'POST',
                            body: JSON.stringify({ revision: c.revision }),
                          },
                        )
                        if (editing?.id === c.id) {
                          setEditing(null)
                          setBody('')
                        }
                        // Deleting the last row of a later page must not strand an empty page.
                        if (page > 1 && query.data?.comments.length === 1)
                          setPage(page - 1)
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
            </div>
          </article>
        ))}
      </div>
      {query.data && query.data.total > query.data.pageSize && (
        <nav
          className="wb-history-pagination community-comment-pagination"
          aria-label={t.comments}
        >
          <button disabled={page === 1} onClick={() => setPage(page - 1)}>
            {t.previous}
          </button>
          <span aria-current="page">{page}</span>
          <button
            disabled={page * query.data.pageSize >= query.data.total}
            onClick={() => setPage(page + 1)}
          >
            {t.next}
          </button>
        </nav>
      )}
      {auth?.user ? (
        <form
          className="community-comment-compose"
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
          <span
            className="community-comment-author"
            title={auth.user.displayName}
          >
            {auth.user.displayName}
          </span>
          <label>
            {t.comment}
            <textarea
              ref={input}
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
        <p className="community-comment-login">{t.loginRequired}</p>
      )}
    </section>
  )
}
