import { useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { createStore } from 'zustand/vanilla'
import { useStore } from 'zustand'
import { loadInitial } from '../craftingApi'
import { concreteInitial, mapSolarText } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import { parseItemText, maxItemTextBytes } from '../itemTextApi'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'
import { toItemCard } from '../itemCardData'
import { ConnectedGoalFilter } from './ConnectedGoalFilter'
import { BasicPaths } from '../basic-paths/BasicPaths'
import {
  startBases,
  startCard,
  startText,
  eligibleStartModifiers,
  startClassMatches,
  startItemIssues,
} from './startItem'
import './craft-start.css'
import { useI18n } from '../../../shared/i18n/i18n'

type Draft = {
  base: string
  text: string
  item: ConcreteItem | null
  rulesetIdentity: string | null
  card: ItemCardData | null
  issue: string
  modifiersOpen: boolean
  started: boolean
  editing: boolean
  root: ItemCardData | null
  checking: boolean
}
export function CraftStart({
  active,
  children,
  activeOmens = [],
  maxMillis = 2000,
}: {
  active: boolean
  children: ReactNode
  activeOmens?: string[]
  maxMillis?: number
}) {
  const { t } = useI18n()
  const [editor] = useState(() =>
    createStore<Draft>(() => ({
      base: 'solar',
      text: '',
      item: null,
      rulesetIdentity: null,
      card: null,
      issue: '',
      modifiersOpen: false,
      started: false,
      editing: true,
      root: null,
      checking: false,
    })),
  )
  const draft = useStore(editor)
  const update = (value: Partial<Draft>) => editor.setState(value)
  const request = useRef<AbortController | null>(null)
  const revision = useRef(0)
  const checking = draft.checking
  const setChecking = (checking: boolean) => editor.setState({ checking })
  const startButton = useRef<HTMLButtonElement>(null)
  const editButton = useRef<HTMLButtonElement>(null)
  const inventory = useQuery({
    queryKey: ['craftStart', 'inventory'],
    enabled: active,
    retry: false,
    staleTime: Infinity,
    queryFn: async ({ signal }) => {
      const response = await fetch('/api/v1/crafting/workbench/registry', {
        signal,
      })
      if (!response.ok) throw new Error('Base inventory unavailable.')
      const registry = (await response.json()) as {
        workbenchBases: Record<string, unknown>
        entries: { serviceScope?: string; supportedBases?: string[] }[]
      }
      const keys = new Set([
        ...Object.keys(registry.workbenchBases),
        ...registry.entries
          .filter((e) => e.serviceScope === 'ACTIVE')
          .flatMap((e) => e.supportedBases ?? []),
      ])
      return Promise.all(
        startBases
          .filter((b) => keys.has(b[0]))
          .map(async (base) => ({
            base,
            initial: await loadInitial(82, signal, base[0]),
          })),
      )
    },
  })
  const solar = inventory.data?.find((entry) => entry.base[0] === 'solar')
  const selected = inventory.data?.find((entry) => entry.base[0] === draft.base)
  const currentRules =
    !!draft.rulesetIdentity &&
    draft.rulesetIdentity === selected?.initial.rulesetIdentity
  function cancel() {
    revision.current++
    request.current?.abort()
    request.current = null
    setChecking(false)
  }
  function selectBase(key: string) {
    cancel()
    const entry = inventory.data?.find((e) => e.base[0] === key)
    if (!entry) return
    const item = concreteInitial(entry.initial)
    update({
      base: key,
      item,
      rulesetIdentity: entry.initial.rulesetIdentity,
      text: startText(entry.base, item, entry.initial.modifiers),
      card: startCard(entry.base, item, entry.initial.modifiers),
      issue: '',
      modifiersOpen: false,
    })
  }
  useEffect(() => {
    if (inventory.data && !editor.getState().text) {
      const entry = inventory.data.find((e) => e.base[0] === 'solar')
      if (entry) {
        const item = concreteInitial(entry.initial)
        editor.setState({
          item,
          rulesetIdentity: entry.initial.rulesetIdentity,
          text: startText(entry.base, item, entry.initial.modifiers),
          card: startCard(entry.base, item, entry.initial.modifiers),
        })
      }
    }
  }, [inventory.data, editor])
  useEffect(() => () => request.current?.abort(), [])
  useEffect(() => {
    if (!active) {
      revision.current++
      request.current?.abort()
      request.current = null
      editor.setState({ checking: false })
    }
  }, [active, editor])
  async function checkText(text: string) {
    const editorItem = editor.getState().item
    const editorIdentity = editor.getState().rulesetIdentity
    cancel()
    const version = revision.current
    const controller = new AbortController()
    request.current = controller
    setChecking(true)
    try {
      if (new TextEncoder().encode(text).length > maxItemTextBytes)
        throw new Error('Item text exceeds 16 KiB. Your text is preserved.')
      const parsed = await parseItemText(text, controller.signal)
      if (controller.signal.aborted || version !== revision.current) return
      const entry = inventory.data?.find(
        (e) =>
          e.base[2] === (parsed.displayBase ?? parsed.displayName) &&
          startClassMatches(e.base[1], parsed.itemClass),
      )
      update({
        card: toItemCard(parsed),
        item: null,
        rulesetIdentity: null,
        base: entry?.base[0] ?? '',
        issue: '',
      })
      if (!entry)
        throw new Error(
          'Unknown or unsupported base. Select a supported base or keep editing this text.',
        )
      if (
        editorItem &&
        editorIdentity === entry.initial.rulesetIdentity &&
        !startItemIssues(entry.initial, editorItem).length &&
        text === startText(entry.base, editorItem, entry.initial.modifiers)
      ) {
        update({
          item: editorItem,
          rulesetIdentity: editorIdentity,
          card: startCard(entry.base, editorItem, entry.initial.modifiers),
        })
      } else if (entry.base[0] !== 'solar') {
        const item = concreteInitial(entry.initial)
        if (text !== startText(entry.base, item, entry.initial.modifiers))
          throw new Error(
            'Pasted-item catalog mapping currently supports Solar Amulet only. This text is preserved for display.',
          )
        update({ item, rulesetIdentity: entry.initial.rulesetIdentity })
      } else {
        const result = await mapSolarText(
          text,
          controller.signal,
          entry.initial.modifiers,
          entry.initial.rulesetIdentity,
        )
        if (controller.signal.aborted || version !== revision.current) return
        if (
          !result.mapped ||
          !result.state ||
          result.state.baseItemId !== entry.initial.state.baseItemId ||
          result.state.snapshotId !== entry.initial.state.snapshotId
        )
          throw new Error(
            result.issues
              .map((i) => `Line ${i.lineNumber}: ${i.message}`)
              .join(' ') ||
              'The item could not be verified against the Solar catalog.',
          )
        update({
          item: result.state,
          rulesetIdentity: entry.initial.rulesetIdentity,
        })
      }
    } catch (error) {
      if (!controller.signal.aborted && version === revision.current)
        update({
          item: null,
          rulesetIdentity: null,
          issue:
            error instanceof Error
              ? error.message
              : 'Item check failed. Your text is preserved.',
        })
    } finally {
      if (request.current === controller) {
        request.current = null
        setChecking(false)
      }
    }
  }
  function editText(text: string) {
    cancel()
    update({
      text,
      item: null,
      rulesetIdentity: null,
      card: null,
      issue: 'Check the edited text before starting.',
      modifiersOpen: false,
    })
  }
  function addModifier(id: string) {
    if (!currentRules || !selected || !draft.item) return
    const definition = eligibleStartModifiers(
      selected.initial,
      draft.item,
    ).find((m) => m.id === id)
    if (!definition?.stats) return
    const item: ConcreteItem = {
      ...draft.item,
      rarity: 'RARE',
      explicits: [
        ...draft.item.explicits,
        {
          modifierId: id,
          values: Object.fromEntries(
            definition.stats.map((s) => [s.id, s.min]),
          ),
        },
      ],
    }
    update({
      item,
      card: startCard(selected.base, item, selected.initial.modifiers),
      text: startText(selected.base, item, selected.initial.modifiers),
      issue: '',
    })
  }
  const context = draft.item ?? solar?.initial.state
  const stateIssues =
    selected && draft.item ? startItemIssues(selected.initial, draft.item) : []
  const availableModifiers =
    selected && draft.item
      ? eligibleStartModifiers(selected.initial, draft.item)
      : []
  return (
    <section className="craft-start" aria-label="Crafting setup">
      <section className="craft-start-target" aria-label="Target item filters">
        <button
          className="craft-start-disclosure"
          aria-expanded={draft.editing}
          aria-controls="craft-start-settings"
          ref={editButton}
          onClick={() => {
            update({ editing: !draft.editing })
            if (!draft.editing)
              queueMicrotask(() => startButton.current?.focus())
          }}
        >
          <span>{draft.editing ? '▾' : '▸'} Target item & starting item</span>
          <span>{draft.editing ? 'Hide settings' : 'Edit settings'}</span>
        </button>
        <div id="craft-start-settings" hidden={!draft.editing}>
          {draft.item && !currentRules && (
            <p role="alert">{t('ruleset.request_changed')}</p>
          )}
          {context && (
            <ConnectedGoalFilter
              rulesetIdentity={draft.rulesetIdentity ?? undefined}
              item={currentRules ? draft.item : null}
              context={context}
              language="en"
              activeOmens={activeOmens}
              maxMillis={maxMillis}
              compact
            />
          )}
          {inventory.isError && (
            <p role="alert">
              Base inventory unavailable.{' '}
              <button onClick={() => void inventory.refetch()}>Retry</button>
            </p>
          )}
          {inventory.isLoading && <p role="status">Loading supported bases…</p>}
          <div className="craft-start-workspace">
            <section
              className="craft-start-editor"
              aria-label="Starting item editor"
            >
              <h3>Create starting item</h3>
              <div className="craft-start-base-selects">
                <label>
                  Equipment type
                  <select
                    aria-label="Starting equipment type"
                    value={selected?.base[1] ?? ''}
                    onChange={(e) => {
                      const entry = inventory.data?.find(
                        (b) => b.base[1] === e.target.value,
                      )
                      if (entry) selectBase(entry.base[0])
                    }}
                  >
                    {!selected && <option value="">Select type</option>}
                    {[...new Set(inventory.data?.map((e) => e.base[1]))].map(
                      (type) => (
                        <option key={type}>{type}</option>
                      ),
                    )}
                  </select>
                </label>
                <label>
                  Base
                  <select
                    aria-label="Starting item base"
                    value={draft.base}
                    onChange={(e) => selectBase(e.target.value)}
                  >
                    {!selected && <option value="">Unverified base</option>}
                    {inventory.data
                      ?.filter(
                        (e) => !selected || e.base[1] === selected.base[1],
                      )
                      .map((e) => (
                        <option key={e.base[0]} value={e.base[0]}>
                          {e.base[2]}
                        </option>
                      ))}
                  </select>
                </label>
              </div>
              <label>
                Item text
                <textarea
                  aria-label="Starting item text"
                  spellCheck={false}
                  value={draft.text}
                  onChange={(e) => editText(e.target.value)}
                  onPaste={(e) => {
                    const text = e.clipboardData.getData('text')
                    if (text) {
                      e.preventDefault()
                      editText(text)
                      void checkText(text)
                    }
                  }}
                />
              </label>
              <div className="craft-start-actions">
                <button
                  disabled={checking || !draft.text.trim() || !inventory.data}
                  onClick={() => void checkText(draft.text)}
                >
                  {checking ? 'Checking…' : 'Check item text'}
                </button>
                <button
                  disabled={
                    !currentRules ||
                    !draft.item ||
                    !selected ||
                    Boolean(stateIssues.length)
                  }
                  aria-expanded={draft.modifiersOpen}
                  onClick={() =>
                    update({ modifiersOpen: !draft.modifiersOpen })
                  }
                >
                  Add modifier
                </button>
              </div>
              {draft.issue && <p role="alert">{draft.issue}</p>}
              {stateIssues.map((issue) => (
                <p role="alert" key={issue}>
                  {issue}
                </p>
              ))}
              {currentRules &&
                draft.modifiersOpen &&
                draft.item &&
                selected && (
                  <div className="craft-start-modifiers">
                    <label>
                      Modifier tier
                      <select
                        aria-label="Starting modifier tier"
                        value=""
                        onChange={(e) => addModifier(e.target.value)}
                      >
                        <option value="">Select modifier…</option>
                        {availableModifiers.map((m) => (
                          <option key={m.id} value={m.id}>
                            {m.text} · T{m.tier}
                          </option>
                        ))}
                      </select>
                    </label>
                    {!availableModifiers.length && (
                      <p role="status">
                        No eligible modifiers remain at this level and the
                        current affix/Crafted capacity.
                      </p>
                    )}
                    <small>
                      Editor values start at the catalog minimum; choose your
                      intended rolls.
                    </small>
                    {draft.item.explicits.map((m, index) => (
                      <div key={m.modifierId} className="craft-start-rolls">
                        <strong>
                          {selected.initial.modifiers[m.modifierId]?.text}
                        </strong>
                        {selected.initial.modifiers[m.modifierId]?.stats?.map(
                          (stat) => (
                            <label key={stat.id}>
                              {stat.id}
                              <input
                                type="number"
                                min={stat.min}
                                max={stat.max}
                                value={m.values[stat.id]}
                                onChange={(e) => {
                                  if (!draft.item) return
                                  const value = Number(e.target.value)
                                  const valid =
                                    e.target.value !== '' &&
                                    Number.isSafeInteger(value) &&
                                    value >= stat.min &&
                                    value <= stat.max
                                  if (!valid) {
                                    update({
                                      issue: `Roll must be an integer from ${stat.min} to ${stat.max}.`,
                                    })
                                    return
                                  }
                                  const item = {
                                    ...draft.item,
                                    explicits: draft.item.explicits.map(
                                      (row, i) =>
                                        i === index
                                          ? {
                                              ...row,
                                              values: {
                                                ...row.values,
                                                [stat.id]: value,
                                              },
                                            }
                                          : row,
                                    ),
                                  }
                                  update({
                                    item,
                                    text: startText(
                                      selected.base,
                                      item,
                                      selected.initial.modifiers,
                                    ),
                                    card: startCard(
                                      selected.base,
                                      item,
                                      selected.initial.modifiers,
                                    ),
                                    issue: '',
                                  })
                                }}
                              />
                            </label>
                          ),
                        )}
                        <button
                          aria-label={`Remove starting modifier ${index + 1}`}
                          onClick={() => {
                            if (!draft.item) return
                            const item = {
                              ...draft.item,
                              explicits: draft.item.explicits.filter(
                                (_, i) => i !== index,
                              ),
                            }
                            update({
                              item,
                              text: startText(
                                selected.base,
                                item,
                                selected.initial.modifiers,
                              ),
                              card: startCard(
                                selected.base,
                                item,
                                selected.initial.modifiers,
                              ),
                              issue: '',
                            })
                          }}
                        >
                          Remove
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              <button
                className="craft-start-primary"
                ref={startButton}
                disabled={
                  !draft.item ||
                  !currentRules ||
                  !draft.card ||
                  checking ||
                  Boolean(draft.issue) ||
                  Boolean(stateIssues.length)
                }
                onClick={() => {
                  update({ started: true, editing: false, root: draft.card })
                  queueMicrotask(() => editButton.current?.focus())
                }}
              >
                Start Crafting
              </button>
            </section>
            <section
              className="craft-start-preview"
              aria-label="Starting item preview"
            >
              <h3>Starting item</h3>
              {draft.card ? (
                <ItemCard item={draft.card} />
              ) : (
                <p>Check item text to preview it.</p>
              )}
            </section>
          </div>
        </div>
      </section>
      {draft.started && draft.root && (
        <section
          className="craft-start-tree"
          aria-label="Crafting tree preview"
        >
          <div className="craft-start-root">
            <span>Starting item</span>
            <ItemCard item={draft.root} />
          </div>
          <div className="craft-start-tree-empty" role="status">
            No crafting paths yet.
          </div>
        </section>
      )}
      <BasicPaths
        active={active}
        item={draft.item}
        initial={selected?.initial}
        valid={currentRules && !checking && !draft.issue && !stateIssues.length}
        revision={JSON.stringify([
          draft.text,
          draft.started,
          draft.editing,
          draft.root,
        ])}
        activeOmens={activeOmens}
      />
      <details className="craft-start-advanced">
        <summary>Advanced family / tier comparison</summary>
        {children}
      </details>
    </section>
  )
}
