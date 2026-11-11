import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useI18n } from '../../shared/i18n/i18n'
import { adminMessages } from './messages'
import { logMessages } from './logMessages'
export type AdminSection =
  'dashboard' | 'requests' | 'errors' | 'limited' | 'archives'
import { logCalendar, retainedDate, type LogPolicy } from './logCalendar'
type Minute = {
  minute: string
  requests: number
  errors: number
  limited: number
  average_ms: number
}
type Ranking = { value: string; requests: number }
type Row = {
  id: string
  occurred_at: string
  method: string
  path: string
  route: string
  status: number
  duration_ms: number
  client_ip: string
  visitor_id: string | null
  user_agent: string | null
  error_type: string | null
}
type Logs = {
  summary: Record<string, number>
  minutes: Minute[]
  routes: Ranking[]
  ips: Ranking[]
  visitors: Ranking[]
  rows: Row[]
  page: number
  pageSize: number
  logging: { queued: number; dropped: number; storage: string; archive: string }
}
type Archive = {
  rows: { id: string; filename: string; row_count: number; bytes: number }[]
  totals: { files: number; bytes: number; rows: number }
  pageSize: number
  freeBytes: number
}
const emptyFilters = { ip: '', visitor: '', route: '', status: '' }
export function OperationsLogs({
  policy,
  credential,
  epoch,
  section,
  onSection,
}: {
  policy: LogPolicy
  credential: string
  epoch: number
  section: AdminSection
  onSection: (section: AdminSection) => void
}) {
  const { locale } = useI18n(),
    t = logMessages[locale],
    a = adminMessages[locale]
  const [calendar, setCalendar] = useState(() => logCalendar(policy)),
    [date, setDate] = useState(() => logCalendar(policy).today),
    [page, setPage] = useState(1),
    [draft, setDraft] = useState(emptyFilters),
    [filters, setFilters] = useState(emptyFilters),
    [downloadError, setDownloadError] = useState('')
  useEffect(() => {
    const timer = window.setInterval(() => {
      const next = logCalendar(policy)
      setCalendar((previous) => {
        if (
          previous.today !== next.today ||
          previous.minimum !== next.minimum
        ) {
          setDate((selected) => retainedDate(selected, previous.today, next))
          setPage(1)
        }
        return previous.today === next.today &&
          previous.hour === next.hour &&
          previous.minimum === next.minimum
          ? previous
          : next
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [policy.previousDays, policy.timeZone, policy])
  const [previousSection, setPreviousSection] = useState(section)
  if (previousSection !== section) {
    setPreviousSection(section)
    setPage(1)
    setDraft(emptyFilters)
    setFilters(emptyFilters)
  }
  const { today, minimum } = calendar
  const get = async <T,>(path: string, signal: AbortSignal): Promise<T> => {
    const response = await fetch(path, {
      signal,
      cache: 'no-store',
      headers: { Authorization: credential },
    })
    if (!response.ok) throw Error(a.failed)
    return response.json() as Promise<T>
  }
  const query = useQuery({
    queryKey: ['admin-requests', epoch, date, section, filters, page],
    enabled: section !== 'archives',
    retry: false,
    gcTime: 0,
    refetchInterval: 30000,
    refetchIntervalInBackground: false,
    queryFn: ({ signal }) =>
      get<Logs>(
        '/api/admin/requests?' +
          new URLSearchParams({
            date,
            kind: section === 'dashboard' ? 'requests' : section,
            page: String(page),
            ...Object.fromEntries(
              Object.entries(filters).filter(([, value]) => value),
            ),
          }),
        signal,
      ),
  })
  const archives = useQuery({
    queryKey: ['admin-archives', epoch, page],
    enabled: section === 'archives',
    retry: false,
    gcTime: 0,
    queryFn: ({ signal }) =>
      get<Archive>('/api/admin/archives?page=' + page, signal),
  })
  const n = (value: number) => value.toLocaleString(locale),
    bytes = (value: number) =>
      value < 0 ? t.unavailable : n(Math.round(value / 1024)) + ' KB'
  const pagination = (total: number, size: number) => (
    <div className="admin-pagination">
      <button disabled={page <= 1} onClick={() => setPage((p) => p - 1)}>
        {t.previous}
      </button>
      <span>
        {page} / {Math.max(1, Math.ceil(total / size))}
      </span>
      <button
        disabled={page * size >= total}
        onClick={() => setPage((p) => p + 1)}
      >
        {t.next}
      </button>
    </div>
  )
  if (section === 'archives')
    return (
      <section className="admin-log-panel">
        <p>{t.archived}</p>
        {archives.error && <p role="alert">{archives.error.message}</p>}
        {downloadError && <p role="alert">{downloadError}</p>}
        {archives.data && (
          <>
            <div className="admin-grid">
              {[
                [t.files, n(archives.data.totals.files)],
                [t.bytes, bytes(archives.data.totals.bytes)],
                [t.free, bytes(archives.data.freeBytes)],
              ].map(([label, value]) => (
                <article className="admin-card" key={label}>
                  <h2>{label}</h2>
                  <strong>{value}</strong>
                </article>
              ))}
            </div>
            {pagination(archives.data.totals.files, archives.data.pageSize)}
            <ul className="admin-archive-list">
              {archives.data.rows.map((row) => (
                <li key={row.id}>
                  <div>
                    <strong>{row.filename}</strong>
                    <small>
                      {t.records}: {n(row.row_count)} · {bytes(row.bytes)}
                    </small>
                  </div>
                  <button
                    onClick={() => {
                      setDownloadError('')
                      void fetch('/api/admin/archives/' + row.id, {
                        headers: { Authorization: credential },
                        cache: 'no-store',
                      })
                        .then(async (response) => {
                          if (!response.ok) throw Error(a.failed)
                          const url = URL.createObjectURL(await response.blob())
                          const link = document.createElement('a')
                          link.href = url
                          link.download = row.filename
                          link.click()
                          setTimeout(() => URL.revokeObjectURL(url), 1000)
                        })
                        .catch(() => setDownloadError(a.failed))
                    }}
                  >
                    {t.download}
                  </button>
                </li>
              ))}
            </ul>
            {!archives.data.rows.length && <p>{t.empty}</p>}
          </>
        )}
      </section>
    )
  const data = query.data
  const pick = (key: keyof typeof filters, value: string) => {
    setFilters((f) => ({ ...f, [key]: value }))
    setDraft((f) => ({ ...f, [key]: value }))
    setPage(1)
  }
  const points = Array.from({ length: 1440 }, (_, i) => {
      const minute =
        String(Math.floor(i / 60)).padStart(2, '0') +
        ':' +
        String(i % 60).padStart(2, '0')
      return (
        data?.minutes.find((p) => p.minute === minute) ?? {
          minute,
          requests: 0,
          errors: 0,
          limited: 0,
          average_ms: 0,
        }
      )
    }),
    max = Math.max(1, ...points.map((p) => p.requests))
  return (
    <section className="admin-log-panel">
      <div className="admin-log-toolbar">
        <label>
          {t.date}
          <input
            aria-label={t.date}
            type="date"
            min={minimum}
            max={today}
            value={date}
            onChange={(e) => {
              setDate(e.target.value || today)
              setPage(1)
            }}
          />
        </label>
        <button
          disabled={query.isFetching}
          onClick={() => void query.refetch()}
        >
          {a.refresh}
        </button>
      </div>
      <p className="admin-note">
        {t.retention.replace('{days}', String(policy.previousDays))}
      </p>
      {query.error && <p role="alert">{query.error.message}</p>}
      {data && (
        <>
          {section === 'dashboard' && (
            <>
              <div className="admin-grid">
                {(
                  [
                    { key: 'requests', label: a.requests, target: 'requests' },
                    { key: 'errors', label: a.errors, target: 'errors' },
                    { key: 'limited', label: a.rejected, target: 'limited' },
                    { key: 'visitors', label: t.visitors },
                    { key: 'views', label: t.views },
                    { key: 'active_visitors', label: t.active },
                    { key: 'p95_ms', label: t.p95 },
                  ] as const
                ).map((card) => (
                  <article className="admin-card" key={card.key}>
                    <h2>{card.label}</h2>
                    {'target' in card ? (
                      <button
                        className="admin-metric"
                        onClick={() => onSection(card.target)}
                      >
                        {n(data.summary[card.key] ?? 0)} ↗
                      </button>
                    ) : (
                      <strong>
                        {n(data.summary[card.key] ?? 0)}
                        {card.key === 'p95_ms' ? ' ms' : ''}
                      </strong>
                    )}
                  </article>
                ))}
              </div>
              <p className="admin-note">{t.anonymous}</p>
              <section className="admin-card admin-traffic">
                <div className="admin-log-toolbar">
                  <h2>{t.minute}</h2>
                </div>
                <svg viewBox="0 0 760 210" role="img" aria-label={t.minute}>
                  {[0, 0.5, 1].map((r) => (
                    <g key={r}>
                      <line
                        x1="44"
                        x2="755"
                        y1={170 - r * 140}
                        y2={170 - r * 140}
                        stroke="#ffffff20"
                      />
                      <text x="38" y={174 - r * 140} textAnchor="end">
                        {n(Math.round(max * r))}
                      </text>
                    </g>
                  ))}
                  {points.map((p, i) => {
                    const x = 48 + i * (702 / 1440),
                      scale = 140 / max
                    return (
                      <g key={p.minute}>
                        <title>
                          {date} {p.minute} KST · {a.requests}: {p.requests} ·{' '}
                          {a.errors}: {p.errors} · {a.rejected}: {p.limited} ·{' '}
                          {t.latency}: {p.average_ms} ms
                        </title>
                        <rect
                          x={x}
                          y={170 - p.requests * scale}
                          width={702 / 1440}
                          height={p.requests * scale}
                          fill="#c9b977"
                        />
                        <rect
                          x={x}
                          y={170 - p.errors * scale}
                          width={702 / 1440}
                          height={p.errors * scale}
                          fill="#d97b77"
                        />
                        <rect
                          x={x}
                          y={170 - (p.errors + p.limited) * scale}
                          width={702 / 1440}
                          height={p.limited * scale}
                          fill="#8ea8d8"
                        />
                        {i % 240 === 0 && (
                          <text x={x} y="190">
                            {p.minute}
                          </text>
                        )}
                      </g>
                    )
                  })}
                </svg>
                <div className="admin-chart-legend">
                  <span>{a.requests}</span>
                  <span>{a.errors}</span>
                  <span>{a.rejected}</span>
                </div>
              </section>
            </>
          )}
          {(section === 'dashboard' ||
            data.logging.dropped > 0 ||
            data.logging.storage !== 'OK' ||
            data.logging.archive !== 'OK') && (
            <p
              className={
                data.logging.dropped ||
                data.logging.storage !== 'OK' ||
                data.logging.archive !== 'OK'
                  ? 'admin-log-warning'
                  : 'admin-note'
              }
            >
              {t.queue}: {n(data.logging.queued)} · {t.dropped}:{' '}
              {n(data.logging.dropped)} · {t.logStorage}:{' '}
              {data.logging.storage === 'OK' ? a.healthy : a.attention} ·{' '}
              {t.archiveStorage}:{' '}
              {data.logging.archive === 'OK' ? a.healthy : a.attention}
            </p>
          )}

          {section !== 'dashboard' && (
            <>
              <p className="admin-log-summary">
                {a.requests}: {n(data.summary.requests ?? 0)} · {a.errors}:{' '}
                {n(data.summary.errors ?? 0)} · {a.rejected}:{' '}
                {n(data.summary.limited ?? 0)} · {t.p95}:{' '}
                {n(data.summary.p95_ms ?? 0)} ms
              </p>
              <form
                className="admin-log-filters"
                onSubmit={(e) => {
                  e.preventDefault()
                  setFilters(draft)
                  setPage(1)
                }}
              >
                {(['ip', 'visitor', 'route', 'status'] as const).map((key) => (
                  <label key={key}>
                    {t[key]}
                    <input
                      value={draft[key]}
                      onChange={(e) =>
                        setDraft((d) => ({ ...d, [key]: e.target.value }))
                      }
                      maxLength={
                        key === 'route'
                          ? 256
                          : key === 'ip'
                            ? 64
                            : key === 'visitor'
                              ? 36
                              : 3
                      }
                      inputMode={key === 'status' ? 'numeric' : undefined}
                    />
                  </label>
                ))}
                <button>{t.apply}</button>
                <button
                  type="button"
                  onClick={() => {
                    setDraft(emptyFilters)
                    setFilters(emptyFilters)
                    setPage(1)
                  }}
                >
                  {t.clear}
                </button>
              </form>
              <p>{t.filterHint}</p>
              <div className="admin-panels admin-log-rankings">
                {(
                  [
                    { key: 'route', title: t.topApis, rows: data.routes },
                    { key: 'ip', title: t.topIps, rows: data.ips },
                    {
                      key: 'visitor',
                      title: t.topVisitors,
                      rows: data.visitors,
                    },
                  ] as const
                ).map((group) => (
                  <section className="admin-card" key={group.key}>
                    <h2>{group.title}</h2>
                    <ol className="admin-rankings">
                      {group.rows.slice(0, 3).map((row) => (
                        <li key={row.value}>
                          <button onClick={() => pick(group.key, row.value)}>
                            <span>{row.value}</span>
                            <b>{n(row.requests)}</b>
                          </button>
                        </li>
                      ))}
                    </ol>
                  </section>
                ))}
              </div>
              {pagination(data.summary.requests ?? 0, data.pageSize)}
              <div className="admin-table-scroll">
                <table className="admin-log-table">
                  <thead>
                    <tr>
                      {[
                        t.time,
                        t.method,
                        t.source,
                        t.status,
                        t.latency,
                        t.ip,
                        t.details,
                      ].map((label) => (
                        <th key={label}>{label}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {data.rows.map((row) => (
                      <tr key={row.id}>
                        <td>
                          {new Date(row.occurred_at).toLocaleTimeString(
                            locale,
                            { timeZone: policy.timeZone, hour12: false },
                          )}
                        </td>
                        <td>{row.method}</td>
                        <td>{row.path}</td>
                        <td>
                          <span
                            className={
                              'admin-http-status status-' +
                              Math.floor(row.status / 100)
                            }
                          >
                            {row.status}
                          </span>
                        </td>
                        <td>{n(row.duration_ms)} ms</td>
                        <td>
                          <button onClick={() => pick('ip', row.client_ip)}>
                            {row.client_ip}
                          </button>
                        </td>
                        <td>
                          <details>
                            <summary>{t.details}</summary>
                            <dl>
                              {[
                                [t.route, row.route],
                                [t.visitor, row.visitor_id],
                                [t.agent, row.user_agent],
                                [t.errorType, row.error_type],
                              ].map(([label, value]) => (
                                <div key={label}>
                                  <dt>{label}</dt>
                                  <dd>{value || '—'}</dd>
                                </div>
                              ))}
                            </dl>
                          </details>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {!data.rows.length && <p>{t.empty}</p>}
              </div>
            </>
          )}
        </>
      )}
    </section>
  )
}
