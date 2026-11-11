import { useEffect, useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useI18n } from '../../shared/i18n/i18n'
import { LocaleSelector } from '../../shared/i18n/LocaleSelector'
import { adminMessages } from './messages'
import './admin.css'
import { OperationsLogs, type AdminSection } from './OperationsLogs'
import { logMessages } from './logMessages'
type Dashboard = {
  logPolicy: import('./logCalendar').LogPolicy
  observedAt: string
  uptimeSeconds: number
  requests: number
  serverErrors: number
  rejectedRequests: number
  meanLatencyMs: number
  recentMaxLatencyMs: number
  heapUsedBytes: number
  heapMaxBytes: number
  processors: number
  cpuPercent: number | null
  database: string
  redis: string
  databaseConnections: number | null
  priceUpdatedAt: string | null
  priceStale: boolean
  browserEvents: Record<string, number>
  traffic: { at: string; requests: number; errors: number }[]
  alerts: string[]
}
// Additional tools register a section here; authentication and the surrounding shell stay shared.
const sections = [
  { id: 'dashboard', icon: '▦' },
  { id: 'requests', icon: '↗' },
  { id: 'errors', icon: '!' },
  { id: 'limited', icon: '◷' },
  { id: 'archives', icon: '↓' },
] as const
export function AdminPage() {
  const { locale } = useI18n(),
    text = adminMessages[locale],
    client = useQueryClient()
  const [section, setSection] = useState<AdminSection>('dashboard')
  const sectionNames = {
    dashboard: text.title,
    requests: text.requests,
    errors: text.errors,
    limited: text.rejected,
    archives: logMessages[locale].archives,
  }
  const [password, setPassword] = useState(''),
    [credential, setCredential] = useState(''),
    [epoch, setEpoch] = useState(0)
  const data = useQuery({
    queryKey: ['admin-dashboard', epoch],
    enabled: !!credential,
    retry: false,
    refetchInterval: credential ? 30000 : false,
    refetchIntervalInBackground: false,
    gcTime: 0,
    queryFn: async ({ signal }) => {
      const response = await fetch('/api/admin/dashboard', {
        signal,
        cache: 'no-store',
        headers: { Authorization: credential },
      })
      if (!response.ok)
        throw Error(
          response.status === 401 || response.status === 429
            ? text.wrong
            : response.status === 404
              ? text.disabled
              : text.failed,
        )
      return (await response.json()) as Dashboard
    },
  })
  const dashboard = credential ? data.data : undefined
  const number = (v: number) => v.toLocaleString(locale),
    date = (v: string | null) =>
      v ? new Date(v).toLocaleString(locale) : text.unknown
  const names: Record<string, string> = {
    DATABASE: text.database,
    REDIS: text.redis,
    PRICES: text.prices,
    MEMORY: text.memory,
    SERVER_ERRORS: text.errors,
  }
  useEffect(() => {
    const clearDashboard = () => {
      void client.cancelQueries({
        predicate: (query) => String(query.queryKey[0]).startsWith('admin-'),
      })
      client.removeQueries({
        predicate: (query) => String(query.queryKey[0]).startsWith('admin-'),
      })
    }
    const leavePage = () => {
      setCredential('')
      setPassword('')
      setEpoch((value) => value + 1)
      clearDashboard()
    }
    // Back/forward cache can restore this component without remounting it.
    const restorePage = (event: PageTransitionEvent) => {
      if (event.persisted) leavePage()
    }
    window.addEventListener('pagehide', leavePage)
    window.addEventListener('pageshow', restorePage)
    return () => {
      window.removeEventListener('pagehide', leavePage)
      window.removeEventListener('pageshow', restorePage)
      clearDashboard()
    }
  }, [client])

  return (
    <main className="admin-shell">
      <aside className="admin-sidebar">
        <a href="/">EXILE HEPHAISTOS</a>
        <small>{text.management}</small>
        <nav aria-label={text.navigation}>
          {sections.map((entry) => (
            <button
              key={entry.id}
              aria-current={section === entry.id ? 'page' : undefined}
              onClick={() => setSection(entry.id)}
            >
              <span aria-hidden="true">{entry.icon}</span>{' '}
              {sectionNames[entry.id]}
            </button>
          ))}
        </nav>
      </aside>
      <section className="admin-main">
        <header>
          <div>
            <p>{text.operations}</p>
            <h1>{sectionNames[section]}</h1>
          </div>
          <div className="admin-header-actions">
            <LocaleSelector />
          </div>
        </header>
        {!dashboard ? (
          <form
            className="admin-login"
            onSubmit={(event) => {
              event.preventDefault()
              setCredential(
                'Basic ' +
                  btoa(
                    Array.from(
                      new TextEncoder().encode('admin:' + password),
                      (byte) => String.fromCharCode(byte),
                    ).join(''),
                  ),
              )
              setEpoch((x) => x + 1)
              setPassword('')
            }}
          >
            <h2>{text.login}</h2>
            <label>
              {text.password}
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            <button disabled={data.isFetching}>{text.login}</button>
            {data.error && <p role="alert">{data.error.message}</p>}
          </form>
        ) : (
          <>
            <div className="admin-status">
              <span>● {text.connected}</span>
              <span>
                {text.updated} {date(dashboard.observedAt)}
              </span>
              <button
                disabled={data.isFetching}
                onClick={() => void data.refetch()}
              >
                {text.refresh}
              </button>
            </div>
            {data.error && <p role="alert">{data.error.message}</p>}
            <OperationsLogs
              policy={dashboard.logPolicy}
              credential={credential}
              epoch={epoch}
              section={section}
              onSection={setSection}
            />
            {section === 'dashboard' && (
              <>
                <section
                  className={
                    'admin-alerts ' +
                    (dashboard.alerts.length ? 'has-alerts' : '')
                  }
                  aria-label={text.alerts}
                >
                  <strong>
                    {dashboard.alerts.length ? text.alerts : text.allClear}
                  </strong>
                  {dashboard.alerts.map((code) => (
                    <span key={code}>{names[code] ?? code}</span>
                  ))}
                </section>
                <div className="admin-grid">
                  {[
                    [
                      text.memory,
                      number(Math.round(dashboard.heapUsedBytes / 1048576)) +
                        ' / ' +
                        number(Math.round(dashboard.heapMaxBytes / 1048576)) +
                        ' MB',
                    ],
                    [
                      text.cpu,
                      dashboard.cpuPercent === null
                        ? text.unknown
                        : number(Math.round(dashboard.cpuPercent)) + '%',
                    ],
                    [
                      text.connections,
                      dashboard.databaseConnections === null
                        ? text.unknown
                        : number(dashboard.databaseConnections),
                    ],
                    [
                      text.uptime,
                      number(Math.floor(dashboard.uptimeSeconds / 60)) +
                        ' ' +
                        text.minute,
                    ],
                  ].map(([label, value]) => (
                    <article className="admin-card" key={label}>
                      <h2>{label}</h2>
                      <strong>{value}</strong>
                    </article>
                  ))}
                </div>
                <div className="admin-panels">
                  <section className="admin-card">
                    <h2>{text.operations}</h2>
                    <dl>
                      {[
                        [
                          text.database,
                          dashboard.database === 'UP'
                            ? text.healthy
                            : text.attention,
                        ],
                        [
                          text.redis,
                          dashboard.redis === 'UP'
                            ? text.healthy
                            : text.attention,
                        ],
                        [
                          text.prices,
                          dashboard.priceStale ? text.stale : text.healthy,
                        ],
                        [text.priceUpdated, date(dashboard.priceUpdatedAt)],
                      ].map(([label, value]) => (
                        <div key={label}>
                          <dt>{label}</dt>
                          <dd>{value}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                </div>
              </>
            )}
          </>
        )}
      </section>
    </main>
  )
}
