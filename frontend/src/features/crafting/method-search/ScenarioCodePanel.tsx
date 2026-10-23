import { isRepeatable } from './methodObservation'
import { ScenarioTransferContext } from './scenarioTransfer'
import type { ReactNode } from 'react'
import { useContext, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { PathTreeProps } from '../path-tree/PathTree'
import type { AutomaticView } from './automaticApi'
import type { Selection } from './supportStorage'
import { saveSupport } from './supportStorage'
import { canonical } from '../path-tree/validation'
import { ItemCard } from '../ItemCard'
import { presentAutomaticItem } from './automaticTierLaws'
import { AutomaticMethodSearch } from './AutomaticMethodSearch'
import {
  decodeScenario,
  encodeScenario,
  scenarioLocales,
  SCENARIO_CODE_LIMIT,
} from './scenarioCode'
import type { Scenario } from './scenarioCode'
import { scenarioMessages } from './scenarioMessages'

const anonymousId = '00000000-0000-4000-8000-000000000000'
function download(code: string) {
  const url = URL.createObjectURL(
      new Blob([code], { type: 'text/plain;charset=utf-8' }),
    ),
    link = document.createElement('a')
  link.href = url
  link.download = 'scenario.exile'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
export function ScenarioExport({
  request,
  view,
  selected,
  cycles,
  route,
  presentItem,
  abstractQuality40,
  abstractQuality,
  baseKey,
  frozen,
  goalLabels,
}: {
  request: PathTreeProps['request']
  view: AutomaticView
  selected: number
  cycles: number
  route: Selection[]
  presentItem: PathTreeProps['presentItem']
  abstractQuality40: boolean
  abstractQuality?: number
  baseKey: string
  frozen?: Scenario | undefined
  goalLabels?: Scenario['goalLabels']
}) {
  const { locale } = useI18n(),
    copy = scenarioMessages[locale]
  const [code, setCode] = useState(''),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState('')
  const text = useRef<HTMLTextAreaElement>(null)
  const transfer = useContext(ScenarioTransferContext)
  const buildSnapshot = (): Scenario => ({
    format: 'exile-scenario',
    version: 1,
    createdAt: frozen?.createdAt ?? new Date().toISOString(),
    baseKey,
    request: structuredClone({ ...request, clientRequestId: anonymousId }),
    abstractQuality40,
    ...(abstractQuality !== undefined ? { abstractQuality } : {}),
    result: structuredClone({ ...view, id: anonymousId }),
    selected,
    cycles,
    route: structuredClone(route),
    ...((frozen?.goalLabels ?? goalLabels)
      ? { goalLabels: frozen?.goalLabels ?? goalLabels }
      : {}),
    cards:
      frozen?.cards ??
      (Object.fromEntries(
        view.nodes.map((n) => [
          String(n.id),
          Object.fromEntries(
            scenarioLocales.map((language) => [
              language,
              presentAutomaticItem(n, language, presentItem, view.nodes),
            ]),
          ),
        ]),
      ) as Scenario['cards']),
  })
  const exportCode = async () => {
    setBusy(true)
    setMessage('')
    try {
      const snapshot = buildSnapshot()
      const next = await encodeScenario(snapshot)
      setCode(next)
      try {
        await navigator.clipboard.writeText(next)
        setMessage(copy.copied)
      } catch {
        setMessage(copy.manual)
      }
    } catch {
      setMessage(copy.invalid)
    } finally {
      setBusy(false)
    }
  }
  const begin = transfer?.begin,
    ready = transfer?.ready,
    clear = transfer?.clear,
    failed = transfer?.failed
  const source = JSON.stringify([
    view.id,
    view.revision,
    request.clientRequestId,
    selected,
    cycles,
    route,
  ])
  useLayoutEffect(() => {
    if (!begin || !ready || !clear || !failed || frozen) return
    if (!['COMPLETE', 'PARTIAL'].includes(view.status)) return
    let alive = true
    begin(source)
    const timer = setTimeout(() => {
      void encodeScenario(buildSnapshot())
        .then((value) => {
          if (alive)
            ready(source, value, view.status + ' · ' + new Date().toISOString())
        })
        .catch(() => {
          if (alive) failed(source)
        })
    }, 350)
    return () => {
      alive = false
      clearTimeout(timer)
      clear(source)
    }
    // Immutable revision and selection identify the snapshot; callbacks are stable.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [source, view.status, begin, ready, clear, failed, frozen])
  if (transfer) return null
  return (
    <section className="scenario-export" aria-label={copy.title}>
      <button
        disabled={busy || !['COMPLETE', 'PARTIAL'].includes(view.status)}
        onClick={() => void exportCode()}
      >
        {busy ? copy.busy : copy.export}
      </button>
      {message && <p role="status">{message}</p>}
      {code && (
        <>
          <p>{copy.warning}</p>
          <p>{code.length.toLocaleString(locale)}</p>
          <textarea
            ref={text}
            aria-label={copy.title}
            value={code}
            readOnly
            onFocus={(e) => e.currentTarget.select()}
            rows={2}
          />
          <button
            onClick={() => {
              text.current?.focus()
              text.current?.select()
            }}
          >
            {copy.manual}
          </button>
          <button onClick={() => download(code)}>{copy.download}</button>
        </>
      )}
    </section>
  )
}
export function ScenarioImport({
  onOpenChanged,
  onCopy,
  policyControl,
}: {
  onOpenChanged: (open: boolean) => void
  onCopy: () => void
  policyControl?: ReactNode
}) {
  const { locale } = useI18n(),
    copy = scenarioMessages[locale]
  const transfer = useContext(ScenarioTransferContext)
  const [localText, setLocalText] = useState(''),
    [preview, setPreview] = useState<Scenario | null>(null),
    [opened, setOpened] = useState<Scenario | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState('')
  const text = transfer?.state.text ?? localText
  const setText = transfer?.edit ?? setLocalText
  const [notice, setNotice] = useState('')
  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(text)
      setNotice(copy.copied)
    } catch {
      setNotice(copy.manual)
    }
  }
  const generation = useRef(0)
  const [snapshotId, setSnapshotId] = useState(0)
  useEffect(() => {
    let alive = true
    const restoring = generation.current
    let cached: string | null = null
    try {
      cached = sessionStorage.getItem('hephaistos.scenario.readonly.v1')
    } catch {
      /* optional convenience only */
    }
    if (cached)
      void decodeScenario(cached)
        .then((value) => {
          if (alive && generation.current === restoring) {
            setSnapshotId((id) => id + 1)
            setText(cached!)
            setOpened(value)
            onOpenChanged(true)
          }
        })
        .catch(() => {
          if (alive && generation.current === restoring) {
            setError(copy.invalid)
            onOpenChanged(false)
          }
        })
    return () => {
      alive = false
    }
    // Restore only the explicitly opened code, independently of the live editor.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  const parse = async () => {
    const turn = ++generation.current
    transfer?.edit(text)
    setBusy(true)
    setError('')
    setPreview(null)
    try {
      const parsed = await decodeScenario(text)
      if (turn === generation.current) setPreview(parsed)
    } catch {
      if (turn === generation.current) setError(copy.invalid)
    } finally {
      if (turn === generation.current) setBusy(false)
    }
  }
  const edit = () => {
    if (!opened) return
    const start = opened.request.start
    const keys = ['hephaistos.support.v1:start', 'hephaistos.support.v1:goal']
    let backup: (string | null)[]
    try {
      backup = keys.map((key) => localStorage.getItem(key))
    } catch {
      setError(copy.invalid)
      return
    }
    const saved = saveSupport('start', {
      base: opened.baseKey,
      text: 'Imported scenario',
      item: start.item,
      rulesetIdentity: start.provenance.rulesetIdentity,
      abstractQuality40: opened.abstractQuality40,
      abstractQuality:
        opened.abstractQuality ?? (opened.abstractQuality40 ? 40 : 0),
      editing: true,
      started: false,
      levelEdited: false,
      searchRequest: null,
    })
    if (!saved || !saveSupport('goal', opened.request.goal)) {
      keys.forEach((key, index) => {
        try {
          const previous = backup[index]
          if (previous === null) localStorage.removeItem(key)
          else if (previous !== undefined) localStorage.setItem(key, previous)
        } catch {
          /* Keep the editor mounted if storage is unavailable. */
        }
      })
      setError(copy.invalid)
      return
    }
    try {
      sessionStorage.removeItem('hephaistos.scenario.readonly.v1')
    } catch {
      /* optional */
    }
    setOpened(null)
    onOpenChanged(false)
    onCopy()
  }
  return (
    <section className="scenario-import" aria-label={copy.import}>
      <div className="scenario-code-entry">
        <label>
          {copy.title}
          <textarea
            value={text}
            maxLength={SCENARIO_CODE_LIMIT + 1}
            rows={4}
            onChange={(e) => {
              generation.current++
              setBusy(false)
              setPreview(null)
              setText(e.target.value)
            }}
          />
        </label>
        <button
          disabled={
            busy ||
            !text ||
            Boolean(transfer?.state.pending && !transfer.state.dirty)
          }
          onClick={() => void copyCode()}
        >
          {copy.export}
        </button>
        {transfer?.state.generated && transfer.state.dirty && (
          <button
            onClick={() => {
              generation.current++
              setPreview(null)
              setBusy(false)
              setNotice('')
              transfer.useGenerated()
            }}
          >
            {copy.current}
          </button>
        )}
        <button disabled={busy || !text} onClick={() => void parse()}>
          {busy ? copy.busy : copy.preview}
        </button>
        <div className="scenario-policy-control">{policyControl}</div>
        <div className="scenario-code-status" role="status">
          {transfer?.state.pending
            ? copy.busy
            : transfer?.state.dirty
              ? copy.pasted
              : transfer?.state.status.startsWith('COMPLETE')
                ? copy.complete
                : transfer?.state.status.startsWith('PARTIAL')
                  ? copy.partial
                  : ''}
          {notice && <p>{notice}</p>}
          {transfer?.state.failed && <p>{copy.invalid}</p>}
        </div>
        <details className="scenario-code-help">
          <summary>{copy.details}</summary>
          {transfer?.state.status && <p>{transfer.state.status}</p>}
          <p>{copy.warning}</p>
          <p>{copy.manual}</p>
          <button disabled={!text} onClick={() => download(text)}>
            {copy.download}
          </button>
          <label>
            {copy.file}
            <input
              type="file"
              accept=".exile,.txt,text/plain"
              disabled={busy}
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (!file) return
                if (file.size > SCENARIO_CODE_LIMIT) {
                  setError(copy.invalid)
                  return
                }
                const turn = ++generation.current
                setPreview(null)
                setBusy(true)
                void file
                  .text()
                  .then((code) => {
                    if (turn === generation.current) setText(code)
                  })
                  .catch(() => {
                    if (turn === generation.current) setError(copy.invalid)
                  })
                  .finally(() => {
                    if (turn === generation.current) setBusy(false)
                  })
              }}
            />
          </label>
        </details>
        {preview && (
          <div className="scenario-preview">
            <h3>{copy.start}</h3>
            <ItemCard item={preview.cards['0']![locale]} />
            <h3>{copy.goal}</h3>
            <GoalSummary scenario={preview} />
            <p>
              {preview.result.status === 'COMPLETE'
                ? copy.complete
                : copy.partial}{' '}
              · {copy.states}: {preview.result.nodes.length} · {copy.methods}:{' '}
              {preview.result.methods.length}
            </p>
            {preview.result.methods.some(isRepeatable) && (
              <p>
                {copy.repeats}: {preview.cycles}
              </p>
            )}
            <button
              onClick={() => {
                try {
                  sessionStorage.setItem(
                    'hephaistos.scenario.readonly.v1',
                    text.trim(),
                  )
                } catch {
                  /* The code itself remains portable without browser storage. */
                }
                setSnapshotId((id) => id + 1)
                setOpened(preview)
                onOpenChanged(true)
                setPreview(null)
              }}
            >
              {copy.open}
            </button>
          </div>
        )}
      </div>
      {error && <p role="alert">{error}</p>}
      {opened && (
        <section className="scenario-readonly">
          <p role="status">{copy.readonly}</p>
          <button
            onClick={() => {
              try {
                sessionStorage.removeItem('hephaistos.scenario.readonly.v1')
              } catch {
                /* optional */
              }
              setOpened(null)
              onOpenChanged(false)
            }}
          >
            {copy.close}
          </button>
          <details>
            <summary>{copy.edit}</summary>
            <p>{copy.editWarning}</p>
            <button onClick={edit}>{copy.edit}</button>
          </details>
          <h3>{copy.goal}</h3>
          <GoalSummary scenario={opened} />
          <AutomaticMethodSearch
            key={snapshotId}
            snapshot={opened}
            scenarioBaseKey={opened.baseKey}
            request={opened.request}
            inputGeneration={opened.createdAt}
            abstractQuality40={opened.abstractQuality40}
            abstractQuality={
              opened.abstractQuality ?? (opened.abstractQuality40 ? 40 : 0)
            }
            presentItem={(item, language) => {
              const node =
                opened.result.nodes.find(
                  (n) => canonical(n.item) === canonical(item),
                ) ?? opened.result.nodes[0]!
              return opened.cards[String(node.id)]![language]
            }}
            onRecalculate={() => {}}
          />
        </section>
      )}
    </section>
  )
}
function GoalSummary({ scenario }: { scenario: Scenario }) {
  const { locale } = useI18n(),
    copy = scenarioMessages[locale],
    goal = scenario.request.goal
  return (
    <>
      <ul>
        {goal.groups.map((group) => (
          <li key={group.id}>
            {group.type}
            {group.disabled ? ' (−)' : ''}
            <ul>
              {group.entries.map((entry) => (
                <li key={entry.id}>
                  {scenario.goalLabels?.[locale]?.[entry.statId] ??
                    entry.statId.split(':').at(-1)?.replaceAll('_', ' ')}
                  : {entry.range.min ?? '−∞'} … {entry.range.max ?? '∞'}
                  {entry.disabled ? ' (off)' : ''}
                </li>
              ))}
            </ul>
          </li>
        ))}
      </ul>
      <details>
        <summary>{copy.details}</summary>
        <pre>
          {JSON.stringify(
            {
              createdAt: scenario.createdAt,
              goal,
              provenance: scenario.request.start.provenance,
              resultVersion: scenario.result.version,
            },
            null,
            2,
          )}
        </pre>
      </details>
    </>
  )
}
