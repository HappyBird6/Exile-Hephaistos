import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'
import { createStore } from 'zustand/vanilla'
import { useStore } from 'zustand'
import { loadInitial } from '../craftingApi'
import type { Initial } from '../craftingApi'
import { concreteInitial, mapSolarText } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import { parseItemText, maxItemTextBytes } from '../itemTextApi'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'
import { toItemCard } from '../itemCardData'
import { ConnectedGoalFilter } from './ConnectedGoalFilter'
import { StartModifierPicker } from './StartModifierPicker'
import { craftStartMessages } from './craftStartMessages'
import { startInputMessages, startInputIssue } from './startInputMessages'
import { localizedModifierText } from '../localizedModifiers'
import {
  startBases,
  startCard,
  startText,
  eligibleStartModifiers,
  startClassMatches,
  startItemIssues,
} from './startItem'
import './craft-start.css'
import { useI18n, uiText } from '../../../shared/i18n/i18n'

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
  rootItem: ConcreteItem | null
  rootDefinitions: Initial['modifiers'] | null
  checking: boolean
}
export function CraftStart({
  active,
  activeOmens = [],
  maxMillis = 2000,
}: {
  active: boolean
  children?: ReactNode
  activeOmens?: string[]
  maxMillis?: number
}) {
  const { t, locale, name } = useI18n()
  const copy = craftStartMessages[locale]
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
      rootItem: null,
      rootDefinitions: null,
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
  const importButton = useRef<HTMLButtonElement>(null)
  const dialog = useRef<HTMLDialogElement>(null)
  const preview = useRef<HTMLDivElement>(null)
  const tree = useRef<HTMLElement>(null)
  const origin = useRef<DOMRect | null>(null)
  const [run, setRun] = useState(0)
  const [importOpen, setImportOpen] = useState(false)
  function closeImport() {
    cancel()
    setImportOpen(false)
    dialog.current?.close()
    importButton.current?.focus()
  }
  useLayoutEffect(() => {
    if (!run || !tree.current) return
    const reduced = window.matchMedia?.(
      '(prefers-reduced-motion: reduce)',
    ).matches
    const target = tree.current.querySelector<HTMLElement>('.craft-start-root')
    if (target && origin.current && !reduced) {
      const rect = target.getBoundingClientRect()
      target.animate?.(
        [
          {
            transform:
              'translate(' +
              (origin.current.x - rect.x) +
              'px, ' +
              (origin.current.y - rect.y) +
              'px)',
            opacity: 0.65,
          },
          { transform: 'translate(0, 0)', opacity: 1 },
        ],
        { duration: 320, easing: 'ease-out' },
      )
    }
    tree.current.focus({ preventScroll: true })
    tree.current.scrollIntoView?.({
      block: 'start',
      behavior: reduced ? 'instant' : 'smooth',
    })
  }, [run])
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
  function displayCard(
    card: ItemCardData,
    item: ConcreteItem | null,
    definitions: Initial['modifiers'] | undefined | null,
  ) {
    return {
      ...card,
      itemClass:
        card.itemClass === 'Amulets'
          ? uiText('Amulet', locale)
          : card.itemClass,
      modifiers: card.modifiers.map((line) => {
        const modifier =
          item &&
          [...item.implicits, ...item.explicits].find(
            (m) => m.modifierId === line.id,
          )
        const definition = definitions?.[line.id]
        return modifier && definition
          ? {
              ...line,
              text: localizedModifierText(definition, modifier.values, locale),
            }
          : line
      }),
    }
  }
  return (
    <section className="craft-start" aria-label={copy.setup}>
      <section className="craft-start-target">
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
          <span>
            {draft.editing ? '▾' : '▸'} {copy.setup}
          </span>
          <span>{draft.editing ? copy.hide : copy.edit}</span>
        </button>
        <div id="craft-start-settings" hidden={!draft.editing}>
          {draft.item && !currentRules && (
            <p role="alert">{t('ruleset.request_changed')}</p>
          )}
          {inventory.isError && (
            <p role="alert">
              {copy.loadError}{' '}
              <button onClick={() => void inventory.refetch()}>
                {copy.retry}
              </button>
            </p>
          )}
          {inventory.isLoading && <p role="status">{copy.loading}</p>}
          <div className="craft-start-workspace">
            <section className="craft-start-editor" aria-label={copy.editor}>
              <h3>{copy.create}</h3>
              <div className="craft-start-base-selects">
                <label>
                  {copy.type}
                  <select
                    aria-label={copy.typeLabel}
                    value={selected?.base[1] ?? ''}
                    onChange={(e) => {
                      const entry = inventory.data?.find(
                        (b) => b.base[1] === e.target.value,
                      )
                      if (entry) selectBase(entry.base[0])
                    }}
                  >
                    {!selected && <option value="">{copy.selectType}</option>}
                    {[...new Set(inventory.data?.map((e) => e.base[1]))].map(
                      (type) => (
                        <option key={type} value={type}>
                          {uiText(type === 'Amulets' ? 'Amulet' : type, locale)}
                        </option>
                      ),
                    )}
                  </select>
                </label>
                <label>
                  {copy.base}
                  <select
                    aria-label={copy.baseLabel}
                    value={draft.base}
                    onChange={(e) => selectBase(e.target.value)}
                  >
                    {!selected && <option value="">{copy.unverified}</option>}
                    {inventory.data
                      ?.filter(
                        (e) => !selected || e.base[1] === selected.base[1],
                      )
                      .map((e) => (
                        <option key={e.base[0]} value={e.base[0]}>
                          {name(e.initial.state.baseItemId, e.base[2])}
                        </option>
                      ))}
                  </select>
                </label>
              </div>
              <button
                ref={importButton}
                onClick={() => {
                  setImportOpen(true)
                  dialog.current?.showModal()
                }}
              >
                {copy.import}
              </button>
              <dialog
                ref={dialog}
                aria-label={copy.import}
                onKeyDown={(event) => {
                  if (event.key === 'Escape') event.stopPropagation()
                }}
                onCancel={(event) => {
                  event.preventDefault()
                  closeImport()
                }}
              >
                <label>
                  {copy.text}
                  <textarea
                    aria-label={copy.textLabel}
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
                    {checking ? copy.checking : copy.check}
                  </button>
                  <button onClick={closeImport}>{copy.close}</button>
                </div>
                {draft.issue && (
                  <p role="alert">{startInputIssue(draft.issue, locale)}</p>
                )}
              </dialog>
              <div className="craft-start-actions">
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
                  {copy.add}
                </button>
              </div>
              {draft.issue && !importOpen && (
                <p role="alert">{startInputIssue(draft.issue, locale)}</p>
              )}
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
                    <StartModifierPicker
                      key={`${draft.base}:${draft.rulesetIdentity}`}
                      definitions={availableModifiers}
                      onAdd={addModifier}
                    />
                    <small>{copy.minimum}</small>
                    {draft.item.explicits.map((m, index) => (
                      <div key={m.modifierId} className="craft-start-rolls">
                        <strong>
                          {selected.initial.modifiers[m.modifierId] &&
                            localizedModifierText(
                              selected.initial.modifiers[m.modifierId]!,
                              undefined,
                              locale,
                            )}
                        </strong>
                        {selected.initial.modifiers[m.modifierId]?.stats?.map(
                          (stat, statIndex) => (
                            <label key={stat.id}>
                              {startInputMessages[locale].value} {index + 1}.
                              {statIndex + 1}
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
                          aria-label={`${copy.removeLabel} ${index + 1}`}
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
                          {copy.remove}
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
                  origin.current =
                    preview.current?.getBoundingClientRect() ?? null
                  update({
                    started: true,
                    editing: false,
                    root: draft.card,
                    rootItem: draft.item,
                    rootDefinitions: selected?.initial.modifiers ?? null,
                  })
                  setRun((value) => value + 1)
                }}
              >
                {copy.start}
              </button>
            </section>
            <section className="craft-start-preview" aria-label={copy.preview}>
              <h3>{copy.create}</h3>
              {draft.card ? (
                <div ref={preview}>
                  <ItemCard
                    item={displayCard(
                      draft.card,
                      draft.item,
                      selected?.initial.modifiers,
                    )}
                    {...(draft.item
                      ? { baseItemId: draft.item.baseItemId }
                      : {})}
                  />
                </div>
              ) : (
                <p>{copy.previewEmpty}</p>
              )}
            </section>
            <section className="craft-start-stats" aria-label={copy.target}>
              <h3>{copy.target}</h3>
              {context && (
                <ConnectedGoalFilter
                  rulesetIdentity={draft.rulesetIdentity ?? undefined}
                  item={currentRules ? draft.item : null}
                  context={context}
                  language={locale}
                  activeOmens={activeOmens}
                  maxMillis={maxMillis}
                  compact
                />
              )}
            </section>
          </div>
        </div>
      </section>
      {draft.started && draft.root && (
        <section
          className="craft-start-tree"
          ref={tree}
          tabIndex={-1}
          aria-label={copy.tree}
        >
          <div className="craft-start-root">
            <span>{copy.create}</span>
            <ItemCard
              item={displayCard(
                draft.root,
                draft.rootItem,
                draft.rootDefinitions,
              )}
              {...(draft.rootItem
                ? { baseItemId: draft.rootItem.baseItemId }
                : {})}
            />
          </div>
          <div className="craft-start-tree-empty" role="status">
            {copy.empty}
          </div>
        </section>
      )}
    </section>
  )
}
