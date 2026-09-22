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
const labels: Record<RunStatus, string> = {
  QUEUED: '실행 대기',
  RUNNING: '수집 중',
  RAW_CAPTURED: '원본 수집 완료',
  FAILED: '수집 실패',
}

export function AdminCrawlingPage() {
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
      <nav aria-label="페이지 이동">
        <a href="/">제작 페이지</a>
      </nav>
      <p className="eyebrow">Exile Hephaistos · 관리자</p>
      <h1>크롤링 관리</h1>
      <p className="intro">수집 대상을 설정하고 원본 HTML 수집을 실행합니다.</p>
      <button
        type="button"
        disabled={session.isFetching}
        onClick={() => void session.refetch()}
      >
        세션 다시 확인
      </button>
      {session.isPending && <p role="status">관리자 세션 확인 중…</p>}
      {session.isError && (
        <div role="alert">
          <p>{errorMessage(session.error)}</p>
          <button type="button" onClick={() => void session.refetch()}>
            다시 확인
          </button>
        </div>
      )}
      {session.data && !session.data.configured && (
        <section aria-labelledby="admin-disabled-heading">
          <h2 id="admin-disabled-heading">관리자 계정 미설정</h2>
          <p>
            서버에 관리자 계정을 설정하면 대상 편집과 수집 실행을 사용할 수
            있습니다.
          </p>
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
      <h2 id="admin-login-heading">관리자 로그인</h2>
      <form onSubmit={submit} className="admin-login">
        <label htmlFor="admin-username">아이디</label>
        <input
          id="admin-username"
          autoComplete="username"
          required
          maxLength={100}
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          disabled={login.isPending}
        />
        <label htmlFor="admin-password">비밀번호</label>
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
          {login.isPending ? '로그인 중…' : '로그인'}
        </button>
        {login.error && <p role="alert">{errorMessage(login.error)}</p>}
      </form>
    </section>
  )
}

function CrawlingDashboard({ session }: { session: AdminSession }) {
  const client = useQueryClient()
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState('')
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
      setNotice(
        '수집 요청을 접수했습니다. 아래 실행 이력에서 상태를 확인하세요.',
      )
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
        <span>관리자 로그인됨</span>
        <button
          type="button"
          disabled={logout.isPending}
          onClick={() => logout.mutate()}
        >
          로그아웃
        </button>
      </div>
      {logout.error && <p role="alert">{errorMessage(logout.error)}</p>}
      <section aria-labelledby="crawl-targets-heading">
        <h2 id="crawl-targets-heading">수집 대상 설정</h2>
        <p>
          서버에 저장한 대상은 다음 수집부터 적용됩니다. 실행 중인 작업의 대상은
          바뀌지 않습니다.
        </p>
        {settings.isPending && <p role="status">설정 불러오는 중…</p>}
        {settings.error && <p role="alert">{errorMessage(settings.error)}</p>}
        {settings.data && (
          <TargetEditor
            key={`${settings.data.version}-${reloadRevision}`}
            settings={settings.data}
            session={session}
            onDirty={setDirty}
            onSaved={() => {
              setDirty(false)
              setNotice('대상 설정을 서버에 저장했습니다.')
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
              setNotice('서버 설정을 다시 불러왔습니다.')
            }
          }}
        >
          서버 설정 다시 불러오기{dirty ? ' (입력 내용 취소)' : ''}
        </button>
      </section>
      <section aria-labelledby="crawl-execution-heading">
        <h2 id="crawl-execution-heading">원본 수집 실행</h2>
        <p>
          활성화된 저장 대상에 요청을 보내고, HTML 원본과 출처를 서버에
          보관합니다.
        </p>
        {!settings.data?.runnerEnabled && settings.data && (
          <p>서버의 수집 실행기가 비활성화되어 있습니다.</p>
        )}
        {dirty && <p>수집 전에 변경한 대상 설정을 저장해 주세요.</p>}
        {!hasTargets && settings.data && (
          <p>수집하려면 대상을 한 개 이상 활성화해 저장해 주세요.</p>
        )}
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
            ? '수집 진행 중'
            : start.isPending
              ? '실행 요청 중…'
              : '크롤링 시작'}
        </button>
        {start.error && <p role="alert">{errorMessage(start.error)}</p>}
        <p className="admin-note">
          현재는 원본 수집만 지원합니다. 데이터 정제·변경 비교·최신화는 이후
          단계에서 제공합니다.
        </p>
      </section>
      <p role="status" className="admin-notice">
        {notice}
      </p>
      <section aria-labelledby="crawl-history-heading">
        <div className="admin-toolbar">
          <h2 id="crawl-history-heading">최근 실행 이력</h2>
          <button
            type="button"
            disabled={runs.isFetching}
            onClick={() => void runs.refetch()}
          >
            이력 새로고침
          </button>
        </div>
        {runs.isPending && <p role="status">실행 이력 불러오는 중…</p>}
        {runs.error && <p role="alert">{errorMessage(runs.error)}</p>}
        {runs.data?.length === 0 && <p>아직 실행한 수집 작업이 없습니다.</p>}
        {runs.data && runs.data.length > 0 && (
          <div className="admin-table-wrap">
            <table>
              <caption className="sr-only">저장된 크롤링 실행 결과</caption>
              <thead>
                <tr>
                  <th scope="col">요청 시각</th>
                  <th scope="col">대상</th>
                  <th scope="col">상태</th>
                  <th scope="col">저장된 응답</th>
                </tr>
              </thead>
              <tbody>
                {runs.data.map((run) => (
                  <tr key={run.id}>
                    <td>
                      <time dateTime={run.createdAt}>
                        {formatTime(run.createdAt)}
                      </time>
                    </td>
                    <td>
                      {run.targets.map((target) => target.name).join(', ')}
                    </td>
                    <td>
                      {labels[run.status]}
                      {run.errorCode && (
                        <p className="admin-note">오류 코드: {run.errorCode}</p>
                      )}
                    </td>
                    <td>
                      {run.sourceCount === null ? '—' : `${run.sourceCount}개`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="admin-note">
          저장된 응답 수에는 접근 정책 확인 페이지가 포함될 수 있습니다. 원본
          수집 완료는 게임 데이터 최신화를 뜻하지 않습니다.
        </p>
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
  const client = useQueryClient()
  const [draft, setDraft] = useState(settings.targets)
  const [validation, setValidation] = useState('')
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
    setValidation('')
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
      setValidation(
        '이름과 poe2db 공개 페이지 주소를 확인해 주세요. 예: https://poe2db.tw/us/Currency',
      )
      return
    }
    if (new Set(targets.map((target) => target.url)).size !== targets.length) {
      setValidation('같은 주소를 중복으로 등록할 수 없습니다.')
      return
    }
    save.mutate(targets)
  }

  return (
    <form onSubmit={submit}>
      <p className="admin-note">
        poe2db의 us·kr 공개 페이지 주소를 최대 {settings.limits.maxTargets}개
        등록할 수 있습니다.
      </p>
      {draft.length === 0 && (
        <p>등록된 대상이 없습니다. 수집할 페이지를 추가해 주세요.</p>
      )}
      <fieldset disabled={save.isPending} className="target-list">
        <legend className="sr-only">수집 대상 목록</legend>
        {draft.map((target, index) => (
          <fieldset key={target.id} className="target-row">
            <legend>대상 {index + 1}</legend>
            <label htmlFor={`name-${target.id}`}>이름</label>
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
            <label htmlFor={`url-${target.id}`}>페이지 주소</label>
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
                수집 활성화
              </label>
              <button
                type="button"
                aria-label={`대상 ${index + 1} 삭제`}
                onClick={() =>
                  change(draft.filter((entry) => entry.id !== target.id))
                }
              >
                삭제
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
            대상 추가
          </button>
          <button type="submit">
            {save.isPending ? '저장 중…' : '대상 설정 저장'}
          </button>
        </div>
      </fieldset>
      {validation && <p role="alert">{validation}</p>}
      {save.error && <p role="alert">{errorMessage(save.error)}</p>}
    </form>
  )
}

function formatTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? '시각 확인 불가'
    : date.toLocaleString('ko-KR')
}
