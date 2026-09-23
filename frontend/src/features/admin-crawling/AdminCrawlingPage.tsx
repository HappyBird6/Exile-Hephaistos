import { useI18n } from '../../shared/i18n/context'
import { LanguageSelector } from '../../shared/i18n/LanguageSelector'
import type { MessageKey, Locale, Translate } from '../../shared/i18n/messages'
import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  AdminApiError,
  adminApi,
  errorMessage,
  type AdminSession,
  type CrawlSettings,
  type CrawlTarget,
  type RunStatus,
} from './api'
import './admin.css'

const sessionKey = ['admin', 'session']
const settingsKey = ['admin', 'crawl-settings']
const runsKey = ['admin', 'crawl-runs']
const labels: Record<RunStatus, MessageKey> = {
  QUEUED: 'runQueued',
  RUNNING: 'runRunning',
  RAW_CAPTURED: 'runCaptured',
  FAILED: 'runFailed',
}

export function AdminCrawlingPage() {
  const { t } = useI18n()
  const client = useQueryClient()
  const session = useQuery({
    queryKey: sessionKey,
    queryFn: ({ signal }) => adminApi.session(signal),
    retry: false,
    staleTime: 0,
  })
  const authenticated = session.data?.authenticated
  useEffect(() => {
    if (authenticated === false) {
      void client.cancelQueries({ queryKey: settingsKey }).then(() => {
        client.removeQueries({ queryKey: settingsKey })
      })
      void client.cancelQueries({ queryKey: runsKey }).then(() => {
        client.removeQueries({ queryKey: runsKey })
      })
    }
  }, [authenticated, client])

  return (
    <main className="admin-page">
      <LanguageSelector />
      <nav aria-label={t('pageNavigation')}>
        <a href="/">{t('craftingPage')}</a>
      </nav>
      <p className="eyebrow">{t('adminEyebrow')}</p>
      <h1>{t('crawlAdmin')}</h1>
      <p className="intro">{t('adminIntro')}</p>
      <button
        type="button"
        disabled={session.isFetching}
        onClick={() => void session.refetch()}
      >
        {t('recheckSession')}
      </button>
      {session.isPending && <p role="status">{t('checkingSession')}</p>}
      {session.isError && (
        <div role="alert">
          <p>{errorMessage(session.error, t)}</p>
          <button type="button" onClick={() => void session.refetch()}>
            {t('retry')}
          </button>
        </div>
      )}
      {session.data && !session.data.configured && (
        <section aria-labelledby="admin-disabled-heading">
          <h2 id="admin-disabled-heading">{t('adminNotConfigured')}</h2>
          <p>{t('adminSetupHelp')}</p>
        </section>
      )}
      {session.data?.configured &&
        (session.data.authenticated ? (
          <CrawlingDashboard session={session.data} />
        ) : (
          <AdminLogin session={session.data} />
        ))}
    </main>
  )
}

function AdminLogin({ session }: { session: AdminSession }) {
  const { t } = useI18n()
  const client = useQueryClient()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const login = useMutation({
    mutationFn: () => adminApi.login(session, username, password),
    onSuccess: async () => {
      setPassword('')
      client.removeQueries({ queryKey: settingsKey })
      client.removeQueries({ queryKey: runsKey })
      await client.invalidateQueries({ queryKey: sessionKey })
    },
    onError: () => {
      setPassword('')
      void client.invalidateQueries({ queryKey: sessionKey })
    },
  })

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    login.mutate()
  }

  return (
    <section aria-labelledby="admin-login-heading">
      <h2 id="admin-login-heading">{t('adminLogin')}</h2>
      <form onSubmit={submit} className="admin-login">
        <label htmlFor="admin-username">{t('username')}</label>
        <input
          id="admin-username"
          autoComplete="username"
          required
          maxLength={100}
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          disabled={login.isPending}
        />
        <label htmlFor="admin-password">{t('password')}</label>
        <input
          id="admin-password"
          type="password"
          autoComplete="current-password"
          required
          maxLength={200}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={login.isPending}
        />
        <button disabled={login.isPending} type="submit">
          {login.isPending ? t('loggingIn') : t('login')}
        </button>
        {login.error && <p role="alert">{errorMessage(login.error, t)}</p>}
      </form>
    </section>
  )
}

function CrawlingDashboard({ session }: { session: AdminSession }) {
  const { t, locale } = useI18n()
  const client = useQueryClient()
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState<MessageKey | null>(null)
  const [reloadRevision, setReloadRevision] = useState(0)
  const settings = useQuery({
    queryKey: settingsKey,
    queryFn: ({ signal }) => adminApi.settings(signal),
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
  const runs = useQuery({
    queryKey: runsKey,
    queryFn: ({ signal }) => adminApi.runs(signal),
    retry: false,
    refetchInterval: 3000,
  })
  const accessError = settings.error ?? runs.error
  useEffect(() => {
    if (
      accessError instanceof AdminApiError &&
      (accessError.status === 401 || accessError.status === 403)
    ) {
      void client.invalidateQueries({ queryKey: sessionKey })
    }
  }, [accessError, client])
  const logout = useMutation({
    mutationFn: () => adminApi.logout(session),
    onSuccess: async () => {
      await client.cancelQueries({ queryKey: ['admin'] })
      client.setQueryData(sessionKey, { ...session, authenticated: false })
      await client.invalidateQueries({ queryKey: sessionKey })
    },
  })
  const start = useMutation({
    mutationFn: () => adminApi.startRun(session),
    onSuccess: async () => {
      setNotice('runAccepted')
      await client.invalidateQueries({ queryKey: runsKey })
    },
  })
  const running =
    runs.data?.some(
      (run) => run.status === 'QUEUED' || run.status === 'RUNNING',
    ) ?? false
  const hasTargets =
    settings.data?.targets.some((target) => target.enabled) ?? false

  return (
    <>
      <div className="admin-toolbar">
        <span>{t('loggedIn')}</span>
        <button
          type="button"
          disabled={logout.isPending}
          onClick={() => logout.mutate()}
        >
          {t('logout')}
        </button>
      </div>
      {logout.error && <p role="alert">{errorMessage(logout.error, t)}</p>}
      <section aria-labelledby="crawl-targets-heading">
        <h2 id="crawl-targets-heading">{t('targetsHeading')}</h2>
        <p>{t('targetsHelp')}</p>
        {settings.isPending && <p role="status">{t('loadingSettings')}</p>}
        {settings.error && (
          <p role="alert">{errorMessage(settings.error, t)}</p>
        )}
        {settings.data && (
          <TargetEditor
            key={`${settings.data.version}-${reloadRevision}`}
            settings={settings.data}
            session={session}
            onDirty={setDirty}
            onSaved={() => {
              setDirty(false)
              setNotice('settingsSaved')
            }}
          />
        )}
        <button
          type="button"
          className="secondary-button"
          disabled={settings.isFetching}
          onClick={async () => {
            const result = await settings.refetch()
            if (result.isSuccess) {
              setReloadRevision((revision) => revision + 1)
              setDirty(false)
              setNotice('settingsReloaded')
            }
          }}
        >
          {t('reloadSettings')}
          {dirty ? t('discardInput') : ''}
        </button>
      </section>
      <section aria-labelledby="crawl-execution-heading">
        <h2 id="crawl-execution-heading">{t('runHeading')}</h2>
        <p>{t('runHelp')}</p>
        {!settings.data?.runnerEnabled && settings.data && (
          <p>{t('runnerDisabled')}</p>
        )}
        {dirty && <p>{t('saveBeforeRun')}</p>}
        {!hasTargets && settings.data && <p>{t('enableTarget')}</p>}
        <button
          type="button"
          onClick={() => start.mutate()}
          disabled={
            !settings.data?.runnerEnabled ||
            !hasTargets ||
            dirty ||
            running ||
            start.isPending ||
            !runs.isSuccess
          }
        >
          {running
            ? t('running')
            : start.isPending
              ? t('requesting')
              : t('startCrawl')}
        </button>
        {start.error && <p role="alert">{errorMessage(start.error, t)}</p>}
        <p className="admin-note">{t('rawOnly')}</p>
      </section>
      <p role="status" className="admin-notice">
        {notice && t(notice)}
      </p>
      <section aria-labelledby="crawl-history-heading">
        <div className="admin-toolbar">
          <h2 id="crawl-history-heading">{t('historyHeading')}</h2>
          <button
            type="button"
            disabled={runs.isFetching}
            onClick={() => void runs.refetch()}
          >
            {t('refreshHistory')}
          </button>
        </div>
        {runs.isPending && <p role="status">{t('loadingHistory')}</p>}
        {runs.error && <p role="alert">{errorMessage(runs.error, t)}</p>}
        {runs.data?.length === 0 && <p>{t('noRuns')}</p>}
        {runs.data && runs.data.length > 0 && (
          <div className="admin-table-wrap">
            <table>
              <caption className="sr-only">{t('resultsCaption')}</caption>
              <thead>
                <tr>
                  <th scope="col">{t('requestedAt')}</th>
                  <th scope="col">{t('target')}</th>
                  <th scope="col">{t('status')}</th>
                  <th scope="col">{t('savedResponses')}</th>
                </tr>
              </thead>
              <tbody>
                {runs.data.map((run) => (
                  <tr key={run.id}>
                    <td>
                      <time dateTime={run.createdAt}>
                        {formatTime(run.createdAt, locale, t)}
                      </time>
                    </td>
                    <td>
                      {run.targets.map((target) => target.name).join(', ')}
                    </td>
                    <td>
                      {t(labels[run.status])}
                      {run.errorCode && (
                        <p className="admin-note">
                          {t('errorCode', { code: run.errorCode })}
                        </p>
                      )}
                    </td>
                    <td>
                      {run.sourceCount === null
                        ? '—'
                        : t('responseCount', { count: run.sourceCount })}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="admin-note">{t('rawCountHelp')}</p>
      </section>
    </>
  )
}

function TargetEditor({
  settings,
  session,
  onDirty,
  onSaved,
}: {
  settings: CrawlSettings
  session: AdminSession
  onDirty: (dirty: boolean) => void
  onSaved: () => void
}) {
  const { t } = useI18n()
  const client = useQueryClient()
  const [draft, setDraft] = useState(settings.targets)
  const [validation, setValidation] = useState<MessageKey | null>(null)
  const save = useMutation({
    mutationFn: (targets: CrawlTarget[]) =>
      adminApi.saveSettings(session, settings.version, targets),
    onSuccess: (result) => {
      if (client.getQueryData<AdminSession>(sessionKey)?.authenticated) {
        client.setQueryData(settingsKey, result)
        onSaved()
      }
    },
  })

  function change(targets: CrawlTarget[]) {
    setDraft(targets)
    setValidation(null)
    onDirty(true)
  }

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const targets = draft.map((target) => ({
      ...target,
      name: target.name.trim(),
      url: target.url.trim(),
    }))
    if (
      targets.some(
        (target) =>
          !target.name ||
          !/^https:\/\/poe2db\.tw\/(us|kr)\/[A-Za-z0-9_-]+$/.test(target.url),
      )
    ) {
      setValidation('invalidTarget')
      return
    }
    if (new Set(targets.map((target) => target.url)).size !== targets.length) {
      setValidation('duplicateTarget')
      return
    }
    save.mutate(targets)
  }

  return (
    <form onSubmit={submit}>
      <p className="admin-note">
        {t('targetLimit', { count: settings.limits.maxTargets })}
      </p>
      {draft.length === 0 && <p>{t('noTargets')}</p>}
      <fieldset disabled={save.isPending} className="target-list">
        <legend className="sr-only">{t('targetList')}</legend>
        {draft.map((target, index) => (
          <fieldset key={target.id} className="target-row">
            <legend>{t('targetNumber', { number: index + 1 })}</legend>
            <label htmlFor={`name-${target.id}`}>{t('name')}</label>
            <input
              id={`name-${target.id}`}
              value={target.name}
              maxLength={80}
              required
              onChange={(event) =>
                change(
                  draft.map((entry) =>
                    entry.id === target.id
                      ? { ...entry, name: event.target.value }
                      : entry,
                  ),
                )
              }
            />
            <label htmlFor={`url-${target.id}`}>{t('pageUrl')}</label>
            <input
              id={`url-${target.id}`}
              type="url"
              value={target.url}
              maxLength={1024}
              required
              spellCheck={false}
              onChange={(event) =>
                change(
                  draft.map((entry) =>
                    entry.id === target.id
                      ? { ...entry, url: event.target.value }
                      : entry,
                  ),
                )
              }
            />
            <div className="admin-toolbar">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={target.enabled}
                  onChange={(event) =>
                    change(
                      draft.map((entry) =>
                        entry.id === target.id
                          ? { ...entry, enabled: event.target.checked }
                          : entry,
                      ),
                    )
                  }
                />
                {t('enableCrawl')}
              </label>
              <button
                type="button"
                aria-label={t('deleteTarget', { number: index + 1 })}
                onClick={() =>
                  change(draft.filter((entry) => entry.id !== target.id))
                }
              >
                {t('delete')}
              </button>
            </div>
          </fieldset>
        ))}
        <div className="admin-actions">
          <button
            type="button"
            disabled={draft.length >= settings.limits.maxTargets}
            onClick={() =>
              change([
                ...draft,
                { id: crypto.randomUUID(), name: '', url: '', enabled: true },
              ])
            }
          >
            {t('addTarget')}
          </button>
          <button type="submit">
            {save.isPending ? t('saving') : t('saveTargets')}
          </button>
        </div>
      </fieldset>
      {validation && <p role="alert">{t(validation)}</p>}
      {save.error && <p role="alert">{errorMessage(save.error, t)}</p>}
    </form>
  )
}

function formatTime(value: string, locale: Locale, t: Translate) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? t('timeUnknown')
    : date.toLocaleString(locale)
}
