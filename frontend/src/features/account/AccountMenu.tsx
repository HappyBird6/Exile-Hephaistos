import { useEffect, useRef, useState } from 'react'
import { authenticate, loadAccount, logout, useAccount } from './auth'
import { useI18n } from '../../shared/i18n/i18n'
import { accountMessages } from './messages'
import './account.css'
export function AccountMenu() {
  const auth = useAccount()
  const { locale } = useI18n()
  const text = accountMessages[locale]
  const [open, setOpen] = useState(false)
  const [register, setRegister] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(false)
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    if (open) dialog.current?.showModal()
    else dialog.current?.close()
  }, [open])
  useEffect(() => {
    if (!auth) void loadAccount().catch(() => setError(true))
  }, [auth])
  const changed = () => {
    localStorage.setItem('hephaistos.account.changed', crypto.randomUUID())
    location.reload()
  }
  if (auth?.enabled === false) return null
  return (
    <div className="account-menu">
      {auth?.user ? (
        <>
          <span>{auth.user.displayName}</span>
          <button
            disabled={busy}
            onClick={async () => {
              setBusy(true)
              try {
                await logout()
                changed()
              } catch {
                setError(true)
                setBusy(false)
              }
            }}
          >
            {text.logout}
          </button>
        </>
      ) : (
        <button onClick={() => setOpen(true)}>{text.login}</button>
      )}
      {error && !open && <span role="alert">{text.unavailable}</span>}
      <dialog
        ref={dialog}
        className="account-dialog"
        onCancel={() => {
          setOpen(false)
          setPassword('')
        }}
      >
        <form
          onSubmit={async (e) => {
            e.preventDefault()
            setBusy(true)
            setError(false)
            try {
              await authenticate(username, password, register)
              setPassword('')
              changed()
            } catch {
              setError(true)
            } finally {
              setBusy(false)
            }
          }}
        >
          <h2>{register ? text.register : text.login}</h2>
          <label>
            {text.username}
            <input
              required
              autoComplete="username"
              pattern="[A-Za-z0-9_]{3,32}"
              maxLength={32}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>
          <label>
            {text.password}
            <input
              required
              type="password"
              autoComplete={register ? 'new-password' : 'current-password'}
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          <p>{text.hint}</p>
          {error && <p role="alert">{text.error}</p>}
          <button disabled={busy} type="submit">
            {register ? text.register : text.login}
          </button>
          <button
            disabled={busy}
            type="button"
            onClick={() => {
              setRegister(!register)
              setError(false)
            }}
          >
            {register ? text.login : text.register}
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={() => {
              setOpen(false)
              setPassword('')
              setError(false)
            }}
          >
            {text.close}
          </button>
        </form>
      </dialog>
    </div>
  )
}
