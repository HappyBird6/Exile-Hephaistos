import { ClearSessions } from './ClearSessions'
import { libraryMessages } from './libraryMessages'
import { useEffect, useState } from 'react'
import type { PropsWithChildren } from 'react'
import { loadAccount, useAccount, AccountError } from './auth'
import { useI18n } from '../../shared/i18n/i18n'
import { accountMessages } from './messages'
import './account.css'

// Guests do not download session persistence/merge code before opening the workbench.
async function syncLibrary(owner: string) {
  return (await import('./librarySync')).syncLibrary(owner)
}
async function browserMergeStatus(owner: string) {
  return (await import('./librarySync')).browserMergeStatus(owner)
}
async function decideBrowserMerge(owner: string, add: boolean) {
  return (await import('./librarySync')).decideBrowserMerge(owner, add)
}

/** Resolve identity before opening storage; never open another account's cache as guest data. */
export function AccountBootstrap({ children }: PropsWithChildren) {
  const auth = useAccount(),
    { locale } = useI18n(),
    text = accountMessages[locale]
  const [counts, setCounts] = useState({ sessions: 0, favorites: 0 })
  const [ready, setReady] = useState(false),
    [merge, setMerge] = useState(false),
    [error, setError] = useState(false),
    [busy, setBusy] = useState(false),
    [refresh, setRefresh] = useState(false)
  useEffect(() => {
    let alive = true
    void (async () => {
      try {
        const next = await loadAccount()
        if (next.user) {
          await syncLibrary(next.user.id)
          const status = await browserMergeStatus(next.user.id)
          if (alive) {
            setMerge(status.needed)
            setCounts({
              sessions: status.history.films.length,
              favorites: status.bases.length,
            })
          }
        }
      } catch {
        if (alive) setError(true)
      } finally {
        if (alive) setReady(true)
      }
    })()
    return () => {
      alive = false
    }
  }, [])
  const owner = auth?.user?.id
  useEffect(() => {
    if (!ready || !owner) return
    let timer: number | undefined
    let active = true
    let failedAt = 0
    const run = async () => {
      if (document.visibilityState === 'hidden') return
      try {
        const changed = await syncLibrary(owner)
        if (active) {
          setError(false)
          if (changed) setRefresh(true)
        }
        failedAt = 0
      } catch (e) {
        failedAt = Date.now()
        if (active) setError(true)
        if (
          e instanceof AccountError &&
          (e.status === 401 || e.status === 403)
        ) {
          const next = await loadAccount().catch(() => null)
          if (next && next.user?.id !== owner) location.reload()
        }
      }
    }
    const schedule = () => {
      clearTimeout(timer)
      timer = window.setTimeout(
        () => void run(),
        Math.max(2000, failedAt + 30000 - Date.now()),
      )
    }
    const focus = () => {
      void loadAccount()
        .then((next) => {
          if (next.user?.id !== owner) location.reload()
          else schedule()
        })
        .catch(() => setError(true))
    }
    window.addEventListener('account-library-changed', schedule)
    window.addEventListener('focus', focus)
    const interval = window.setInterval(() => void run(), 30000)
    return () => {
      active = false
      clearTimeout(timer)
      clearInterval(interval)
      window.removeEventListener('account-library-changed', schedule)
      window.removeEventListener('focus', focus)
    }
  }, [ready, owner])
  useEffect(() => {
    const changed = (e: StorageEvent) => {
      if (e.key === 'hephaistos.account.changed') location.reload()
    }
    window.addEventListener('storage', changed)
    return () => window.removeEventListener('storage', changed)
  }, [])
  if (!ready)
    return (
      <div role="status" aria-busy="true">
        …
      </div>
    )
  return (
    <>
      {(merge || error || refresh) && (
        <div className="craft-page craft-page--framed account-sync-frame">
          <aside className="account-sync">
            {merge && (
              <>
                <p>{text.merge}</p>
                <p>
                  {libraryMessages(locale)
                    .count.replace('{sessions}', String(counts.sessions))
                    .replace('{favorites}', String(counts.favorites))}
                </p>
                {[true, false].map((add) => (
                  <button
                    key={String(add)}
                    disabled={busy}
                    onClick={async () => {
                      if (!auth?.user) return
                      setBusy(true)
                      try {
                        await decideBrowserMerge(auth.user.id, add)
                        setMerge(false)
                        if (add) {
                          await syncLibrary(auth.user.id)
                          location.reload()
                        }
                      } catch {
                        setError(true)
                      } finally {
                        setBusy(false)
                      }
                    }}
                  >
                    {add ? text.add : text.skip}
                  </button>
                ))}
                <ClearSessions browserOnly />
              </>
            )}
            {error && <p role="alert">{text.unavailable}</p>}
            {refresh && <p role="status">{text.sync}</p>}
            {(error || refresh) && (
              <button disabled={busy} onClick={() => location.reload()}>
                {text.retry}
              </button>
            )}
          </aside>
        </div>
      )}
      {children}
    </>
  )
}
