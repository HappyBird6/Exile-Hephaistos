import { currencyNames } from './currencyNames'
import { skipToken, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { ItemCard } from './ItemCard'
import { toItemCard } from './itemCardData'
import { MaterialTooltip } from './MaterialTooltip'
import { useItemTextImport } from './useItemTextImport'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import {
  materials,
  materialTabs,
  specialEssences,
  essenceRows,
} from './materials'
import type { Material, MaterialTab } from './materials'
import { useItemDraft } from './draft'
import { CraftSupport } from './CraftSupport'
import { CraftingExplorer } from './CraftingExplorer'
import { loadInitial } from './craftingApi'
import {
  applyCurrency,
  concreteInitial,
  rolledText,
  workbenchCurrencyActions,
  workbenchActionNames,
  workbenchOmens,
  mapSolarText,
} from './workbenchApi'
import type { AppliedItem, MappingResult } from './workbenchApi'
import type { Action } from './craftingApi'
import './crafting.css'

function tooltipEvents(id: string) {
  return { 'data-material-tooltip': id }
}

const workspaceTabs = ['workbench', 'support', 'explorer'] as const
const workspaceNames = {
  workbench: 'Crafting Workbench',
  support: 'Craft Support',
  explorer: 'State explorer',
}

type Selection = { id: string; name: string; image: string }

export function CraftingPage() {
  const client = useQueryClient()
  const draft = useItemDraft()
  const [view, setView] = useState<(typeof workspaceTabs)[number]>('workbench')
  const imported = useItemTextImport()
  const dialog = useRef<HTMLDialogElement>(null)
  const [searches, setSearches] = useState<
    Partial<Record<MaterialTab, string>>
  >({})
  const [tooltipsEnabled, setTooltipsEnabled] = useState(true)
  const [activeTab, setActiveTab] = useState<MaterialTab>('Currency')
  const [selected, setSelected] = useState<Selection | null>(null)
  const [held, setHeld] = useState<Material | null>(null)
  const [favorites, setFavorites] = useState<(Material | null)[]>(
    Array(15).fill(null),
  )
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null)
  const [inputMode, setInputMode] = useState<'base' | 'text'>('base')
  const [inputOpen, setInputOpen] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const [applying, setApplying] = useState<AbortController | null>(null)
  const applyingRequest = useRef<AbortController | null>(null)
  const initial = useQuery({
    queryKey: ['crafting', 'initial', draft.baseItemLevel],
    queryFn: ({ signal }) => loadInitial(draft.baseItemLevel, signal),
    staleTime: 60_000,
    retry: false,
  })
  const workbenchKey = ['crafting', 'workbench', draft.baseRevision] as const
  const workbench = useQuery<AppliedItem>({
    queryKey: workbenchKey,
    queryFn: skipToken,
  })
  const mappingKey = ['crafting', 'mapped', draft.baseRevision] as const
  const mapping = useQuery<MappingResult>({
    queryKey: mappingKey,
    queryFn: skipToken,
  })
  const canCraft = draft.source === 'base' || mapping.data?.mapped === true
  useEffect(
    () => () => {
      applyingRequest.current?.abort()
      applyingRequest.current = null
    },
    [draft.baseRevision, draft.source],
  )
  const [baseLevel, setBaseLevel] = useState('82')
  const [previewRequest, setPreviewRequest] = useState<{
    count: number
    action: Action | null
  }>({ count: 0, action: null })
  const inputToggle = useRef<HTMLButtonElement>(null)
  const inputClose = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (inputOpen) {
      dialog.current?.showModal()
      inputClose.current?.focus()
    } else dialog.current?.close()
  }, [inputOpen])
  useEffect(() => {
    const cancel = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelected(null)
        setHeld(null)
        setPointer(null)
        if (inputOpen)
          dialog.current?.dispatchEvent(
            new Event('cancel', { cancelable: true }),
          )
        if (inputOpen) inputToggle.current?.focus()
        setAnnouncement('Currency selection cleared.')
      }
    }
    const hide = () => setPointer(null)
    window.addEventListener('keydown', cancel)
    window.addEventListener('blur', hide)
    return () => {
      window.removeEventListener('keydown', cancel)
      window.removeEventListener('blur', hide)
    }
  }, [inputOpen])

  function choose(resource: Selection) {
    setSelected(resource)
    setHeld(null)
    setAnnouncement(`${resource.name} selected · Click the central item.`)
  }
  async function apply() {
    if (applyingRequest.current) return
    if (!selected) {
      setAnnouncement('Select a currency from the stash first.')
      return
    }
    if (!canCraft) {
      setAnnouncement(
        'The item has not changed. Pasted items are display-only. Select the Solar Amulet base to explore probabilities.',
      )
      return
    }
    if (workbenchOmens.some((omen) => omen.id === selected.id)) {
      const active = draft.activeOmens.includes(selected.id)
      draft.setActiveOmens(
        active
          ? draft.activeOmens.filter((id) => id !== selected.id)
          : [...draft.activeOmens, selected.id],
      )
      setAnnouncement(
        `${selected.name} ${active ? 'deactivated' : 'activated. It is consumed only by a successful matching craft'}.`,
      )
      setSelected(null)
      setHeld(null)
      setPointer(null)
      return
    }
    const action = workbenchCurrencyActions[selected.id]
    if (!action) {
      setAnnouncement(
        'The item has not changed. This material has no verified crafting rule in the current Workbench.',
      )
      return
    }
    if (!initial.data) {
      setAnnouncement(
        'The item has not changed. Wait for the crafting catalog or retry loading it.',
      )
      return
    }
    const controller = new AbortController()
    applyingRequest.current = controller
    setApplying(controller)
    const revision = draft.baseRevision
    const state =
      workbench.data?.state ??
      mapping.data?.state ??
      concreteInitial(initial.data)
    try {
      const result = await applyCurrency(
        state,
        action,
        initial.data.modifiers,
        controller.signal,
        draft.activeOmens,
      )
      if (
        controller.signal.aborted ||
        useItemDraft.getState().baseRevision !== revision ||
        useItemDraft.getState().source !== draft.source
      )
        return
      if (result.applied) {
        client.setQueryData(workbenchKey, result)
        draft.setActiveOmens(result.remainingOmens)
        setAnnouncement(
          `${workbenchActionNames[action]} applied. Current item updated.${result.consumedOmens.length ? ` Consumed: ${result.consumedOmens.map((id) => id.replaceAll('_', ' ')).join(', ')}.` : ''}${result.assumptions.length ? ' Uniform probability assumptions were used; see the roll assumptions.' : ''}`,
        )
        setSelected(null)
        setHeld(null)
        setPointer(null)
      } else
        setAnnouncement(
          `Craft blocked by rule: ${result.reason} Your item is unchanged; active omens are preserved.`,
        )
    } catch (error) {
      if (!controller.signal.aborted)
        setAnnouncement(
          error instanceof Error
            ? error.message
            : 'Could not apply currency. Your item is unchanged.',
        )
    } finally {
      if (applyingRequest.current === controller) {
        applyingRequest.current = null
        setApplying(null)
      }
    }
  }
  async function importText() {
    if (await imported.submit(draft.text)) closeInput()
    setSelected(null)
    setHeld(null)
  }
  async function enableCrafting() {
    if (applyingRequest.current || !imported.data || !initial.data) return
    const controller = new AbortController()
    const revision = draft.baseRevision
    applyingRequest.current = controller
    setApplying(controller)
    try {
      const result = await mapSolarText(
        imported.data.text.originalText,
        controller.signal,
        initial.data.modifiers,
      )
      if (
        controller.signal.aborted ||
        useItemDraft.getState().baseRevision !== revision
      )
        return
      if (
        result.mapped &&
        (result.state?.snapshotId !== initial.data.metadata.snapshotId ||
          result.state?.baseItemId !== initial.data.state.baseItemId)
      )
        throw new Error(
          'The item uses another catalog snapshot or base. Please reload the catalog.',
        )
      client.setQueryData(mappingKey, result)
      setAnnouncement(
        result.mapped
          ? 'Solar Amulet catalog mapping verified. Crafting is enabled; original text remains in Edit item.'
          : 'Crafting mapping is blocked. The original text and unresolved lines are preserved.',
      )
    } catch (error) {
      if (!controller.signal.aborted)
        setAnnouncement(
          error instanceof Error ? error.message : 'Could not map this item.',
        )
    } finally {
      if (applyingRequest.current === controller) {
        applyingRequest.current = null
        setApplying(null)
      }
    }
  }
  function closeInput() {
    imported.invalidate()
    setInputOpen(false)
    inputToggle.current?.focus()
  }
  function placeFavorite(index: number) {
    if (!held) {
      const resource = favorites[index]
      if (resource) choose(resource)
      return
    }
    setFavorites((current) =>
      current.map((resource, position) =>
        position === index ? held : resource,
      ),
    )
    setAnnouncement(`${held.name} registered in favorite slot ${index + 1}.`)
    setHeld(null)
  }

  const item = draft.source === 'text' ? imported.data : undefined
  const displayedName =
    draft.source === 'base'
      ? 'Solar Amulet'
      : (item?.displayName ?? 'Pasted item')
  const search = searches[activeTab] ?? ''
  const currentMaterials = materials.filter(
    (m) =>
      m.category === activeTab &&
      m.name.toLowerCase().includes(search.trim().toLowerCase()),
  )
  function materialButton(material: Material) {
    return (
      <button
        type="button"
        key={material.id}
        className={`material-entry ${held?.id === material.id ? 'is-held' : ''} ${selected?.id === material.id ? 'is-selected' : ''}`}
        aria-label={material.name}
        aria-pressed={held?.id === material.id || selected?.id === material.id}
        {...tooltipEvents(material.id)}
        onContextMenu={(event) => {
          event.preventDefault()
          setPointer({ x: event.clientX, y: event.clientY })
          choose(material)
        }}
        onClick={(event) => {
          if (event.detail === 0) setPointer(null)
          setHeld(material)
          setSelected(null)
          setAnnouncement(
            `${material.name} picked up. Click a favorite slot to register; Escape to cancel.`,
          )
        }}
      >
        <span className="material-entry__image">
          <CurrencyImage key={material.id} {...material} />
        </span>
        <span>{material.name}</span>
      </button>
    )
  }
  const matchingEssenceRows = essenceRows(search)
  const cursor = held ?? selected
  const concrete = workbench.data?.state ?? mapping.data?.state
  const card =
    item && !concrete
      ? toItemCard(item)
      : concrete && initial.data
        ? {
            rarity: concrete.rarity,
            name: item?.displayName ?? 'Solar Amulet',
            base: 'Solar Amulet',
            itemClass: 'Amulet',
            itemLevel: concrete.itemLevel,
            properties: [],
            requirements: [],
            flags: [],
            modifiers: [...concrete.implicits, ...concrete.explicits].map(
              (m, i) => {
                const d = initial.data!.modifiers[m.modifierId]!
                return {
                  id: m.modifierId,
                  text: rolledText(d, m.values),
                  kind:
                    i < concrete.implicits.length
                      ? ('implicit' as const)
                      : ('explicit' as const),
                  affixLabel:
                    d.affixType === 'NONE'
                      ? undefined
                      : `${d.affixType === 'PREFIX' ? 'P' : 'S'}${d.tier}`,
                  detail: `Source range: ${d.text}\n${Object.entries(m.values)
                    .map(([id, value]) => `${id} = ${value}`)
                    .join('\n')}`,
                }
              },
            ),
          }
        : null

  return (
    <main
      className="craft-page"
      onPointerMove={(event) => {
        if (event.pointerType !== 'touch')
          setPointer({ x: event.clientX, y: event.clientY })
      }}
      onPointerLeave={() => setPointer(null)}
    >
      <header className="craft-header">
        <a className="craft-brand" href="/" aria-label="Exile Hephaistos home">
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>PATH OF EXILE 2 · CRAFTING WORKBENCH</small>
          </span>
        </a>
        <a className="admin-link" href="/admin">
          Admin
        </a>
      </header>
      <nav className="workspace-nav" aria-label="Main navigation">
        <div role="tablist" aria-label="Crafting workspace">
          {workspaceTabs.map((tab, index) => (
            <button
              key={tab}
              type="button"
              role="tab"
              id={`workspace-${tab}`}
              aria-selected={view === tab}
              aria-controls={`panel-${tab}`}
              tabIndex={view === tab ? 0 : -1}
              onClick={() => {
                setView(tab)
                setHeld(null)
                setSelected(null)
                setPointer(null)
              }}
              onKeyDown={(event) => {
                if (
                  !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(
                    event.key,
                  )
                )
                  return
                event.preventDefault()
                const next =
                  event.key === 'Home'
                    ? 'workbench'
                    : event.key === 'End'
                      ? 'explorer'
                      : workspaceTabs[
                          (index +
                            (event.key === 'ArrowRight' ? 1 : -1) +
                            workspaceTabs.length) %
                            workspaceTabs.length
                        ]!
                setView(next)
                setHeld(null)
                setSelected(null)
                setPointer(null)
                document.getElementById(`workspace-${next}`)?.focus()
              }}
            >
              <span aria-hidden="true">0{index + 1}</span>
              {workspaceNames[tab]}
            </button>
          ))}
        </div>
      </nav>
      <div className="craft-title">
        <div>
          <p className="craft-kicker">THE CRAFTING BENCH</p>
          <h1>
            {view === 'workbench' ? 'Crafting workbench' : workspaceNames[view]}
          </h1>
          <p>
            Apply currency in the Workbench or inspect possibilities in State
            explorer.
          </p>
        </div>
        <span className="preview-badge">
          <i />
          Solar Amulet · Base modifiers
        </span>
      </div>
      <div
        className="workbench-layout"
        id="panel-workbench"
        role="tabpanel"
        aria-labelledby="workspace-workbench"
        hidden={view !== 'workbench'}
        onContextMenu={(event) => event.preventDefault()}
      >
        <div className="stash-panel">
          <div className="panel-heading stash-heading">
            <div
              role="tablist"
              aria-label="Material stash tabs"
              className="material-tabs"
            >
              {materialTabs.map((tab, index) => (
                <button
                  key={tab.id}
                  type="button"
                  role="tab"
                  id={`tab-${tab.id}`}
                  aria-selected={activeTab === tab.id}
                  aria-controls="material-list"
                  tabIndex={activeTab === tab.id ? 0 : -1}
                  onClick={() => setActiveTab(tab.id)}
                  onKeyDown={(event) => {
                    const next =
                      event.key === 'ArrowRight'
                        ? (index + 1) % materialTabs.length
                        : event.key === 'ArrowLeft'
                          ? (index + materialTabs.length - 1) %
                            materialTabs.length
                          : event.key === 'Home'
                            ? 0
                            : event.key === 'End'
                              ? materialTabs.length - 1
                              : null
                    if (next === null) return
                    event.preventDefault()
                    const target = materialTabs[next]!
                    setActiveTab(target.id)
                    document.getElementById(`tab-${target.id}`)?.focus()
                  }}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <label className="tooltip-toggle">
              <input
                type="checkbox"
                checked={tooltipsEnabled}
                onChange={(event) => setTooltipsEnabled(event.target.checked)}
              />
              Show tooltips
            </label>
          </div>
          <div className="stash-board">
            <div className="stash-canvas" aria-label="Currency stash">
              <div
                id="material-list"
                role="tabpanel"
                aria-labelledby={`tab-${activeTab}`}
                className={
                  activeTab === 'Currency'
                    ? 'currency-catalog'
                    : 'material-panel'
                }
              >
                {activeTab === 'Currency' ? (
                  currencies.map((currency) => {
                    const name = currencyNames[currency.id]
                    return (
                      <button
                        key={currency.id}
                        type="button"
                        className={`currency-slot ${selected?.id === currency.id ? 'is-selected' : ''}`}
                        style={{
                          left: `${currency.x / 9.35}%`,
                          top: `${currency.y / 5.5}%`,
                        }}
                        aria-label={name}
                        aria-pressed={selected?.id === currency.id}
                        {...tooltipEvents(currency.id)}
                        onContextMenu={(event) => {
                          event.preventDefault()
                          setPointer({ x: event.clientX, y: event.clientY })
                          choose({ ...currency, name })
                        }}
                        onClick={(event) => {
                          if (event.detail === 0) setPointer(null)
                          choose({ ...currency, name })
                        }}
                      >
                        <CurrencyImage {...currency} name={name} />
                        {currency.id.startsWith('Greater') && (
                          <span className="currency-tier" aria-hidden="true">
                            II
                          </span>
                        )}
                        {currency.id.startsWith('Perfect') && (
                          <span className="currency-tier" aria-hidden="true">
                            III
                          </span>
                        )}
                      </button>
                    )
                  })
                ) : (
                  <>
                    <input
                      type="search"
                      className="material-search"
                      aria-label={`Search ${activeTab.replaceAll('_', ' ')}`}
                      placeholder="Search…"
                      value={search}
                      onChange={(event) =>
                        setSearches((old) => ({
                          ...old,
                          [activeTab]: event.target.value,
                        }))
                      }
                    />
                    <div
                      className={`material-catalog ${activeTab === 'Essence' ? 'essence-catalog' : ''}`}
                    >
                      {activeTab === 'Essence'
                        ? matchingEssenceRows.map((row, i) => (
                            <div className="essence-row" key={i}>
                              {row.map((m, j) =>
                                m ? materialButton(m) : <span key={j} />,
                              )}
                            </div>
                          ))
                        : currentMaterials.map(materialButton)}
                      {(activeTab === 'Essence'
                        ? matchingEssenceRows.length === 0
                        : currentMaterials.length === 0) && (
                        <p className="material-no-results">
                          No materials found
                        </p>
                      )}
                    </div>
                  </>
                )}
              </div>
              {activeTab === 'Essence' && (
                <div
                  className="special-essences"
                  role="group"
                  aria-label="Special essences"
                >
                  {specialEssences.map(materialButton)}
                </div>
              )}
              <div className="item-placement">
                <span className="placement-label">Current item</span>
                <button
                  className={`item-slot ${selected ? 'is-ready' : ''}`}
                  type="button"
                  aria-label="Use selected currency on the central item"
                  disabled={
                    applying !== null && applyingRequest.current !== null
                  }
                  onClick={apply}
                >
                  {draft.source === 'base' ? (
                    <img
                      src="/assets/currency/solar-amulet.webp"
                      alt="Solar Amulet"
                      draggable="false"
                    />
                  ) : (
                    <span className="text-item-symbol" aria-hidden="true">
                      ≡
                    </span>
                  )}
                  <span>{displayedName}</span>
                </button>
                <button
                  type="button"
                  ref={inputToggle}
                  className="item-input-toggle"
                  aria-expanded={inputOpen}
                  aria-controls="item-input-panel"
                  onClick={() => setInputOpen((open) => !open)}
                >
                  Edit item
                </button>
              </div>
              <div
                role="group"
                aria-label="Shared material favorites"
                className={`favorite-slots ${held ? 'is-placing' : ''}`}
              >
                {favorites.map((resource, index) => (
                  <button
                    type="button"
                    key={index}
                    className={`currency-slot favorite-slot ${resource && selected?.id === resource.id ? 'is-selected' : ''}`}
                    style={{
                      left: `${(652 + (index % 3) * 90) / 9.35}%`,
                      top: `${(40 + Math.floor(index / 3) * 100) / 5.5}%`,
                    }}
                    aria-label={`Favorite slot ${index + 1}: ${resource?.name ?? 'empty'}`}
                    aria-pressed={Boolean(
                      resource && selected?.id === resource.id,
                    )}
                    {...(resource ? tooltipEvents(resource.id) : {})}
                    onClick={() => placeFavorite(index)}
                    onContextMenu={(event) => {
                      event.preventDefault()
                      if (resource) {
                        setPointer({ x: event.clientX, y: event.clientY })
                        choose(resource)
                      }
                    }}
                  >
                    {resource ? (
                      <CurrencyImage key={resource.id} {...resource} />
                    ) : (
                      <span className="favorite-empty" aria-hidden="true">
                        +
                      </span>
                    )}
                  </button>
                ))}
              </div>
            </div>
            <div className="bench-lower">
              <div className="bench-item-card">
                <details className="workbench-omen-panel">
                  <summary>Active omens ({draft.activeOmens.length})</summary>
                  <fieldset
                    className="workbench-omens"
                    disabled={
                      !canCraft ||
                      (applying !== null && applyingRequest.current !== null)
                    }
                  >
                    <legend>Active omens</legend>
                    <p>
                      One matching omen per craft. A matching omen is consumed
                      after a successful application; unrelated omens stay
                      active. Greater/Perfect interactions are awaiting
                      verification.
                    </p>
                    {workbenchOmens.map((omen) => (
                      <label key={omen.id}>
                        <input
                          type="checkbox"
                          checked={draft.activeOmens.includes(omen.id)}
                          onChange={(event) =>
                            draft.setActiveOmens(
                              event.target.checked
                                ? [...draft.activeOmens, omen.id]
                                : draft.activeOmens.filter(
                                    (id) => id !== omen.id,
                                  ),
                            )
                          }
                        />
                        <span>
                          {omen.id.replaceAll('_', ' ')}
                          <small>
                            {workbenchActionNames[omen.trigger]}: {omen.effect}
                          </small>
                        </span>
                      </label>
                    ))}
                  </fieldset>
                </details>
                {card ? (
                  <ItemCard item={card} />
                ) : (
                  <ItemCard
                    item={{
                      rarity: 'NORMAL',
                      name: 'Solar Amulet',
                      base: 'Solar Amulet',
                      itemClass: 'Amulet',
                      itemLevel: draft.baseItemLevel,
                      properties: [],
                      requirements: [],
                      modifiers: [
                        {
                          id: 'spirit',
                          text: '+15 to Spirit',
                          kind: 'implicit',
                        },
                      ],
                      flags: [],
                    }}
                  />
                )}
                <p>
                  {canCraft
                    ? 'Select a currency, then click the central item to craft. The result stays in this Workbench.'
                    : 'Pasted items are display-only. Select the Solar Amulet base to explore probabilities.'}
                </p>
                {draft.source === 'text' && !mapping.data?.mapped && (
                  <button
                    type="button"
                    onClick={() => void enableCrafting()}
                    disabled={applyingRequest.current !== null || !initial.data}
                  >
                    Enable Solar crafting
                  </button>
                )}
                {mapping.data && (
                  <p>
                    {mapping.data.mapped
                      ? 'Catalog mapping verified. Original clipboard text is retained in Edit item.'
                      : 'Catalog mapping blocked:'}
                  </p>
                )}
                {mapping.data?.issues.map((issue, index) => (
                  <p key={index}>
                    Line {issue.lineNumber || 'item'}: {issue.message}
                  </p>
                ))}
                {initial.isPending && <p>Loading crafting catalog...</p>}
                {initial.isError && (
                  <p role="alert">
                    Could not load crafting catalog.{' '}
                    <button
                      type="button"
                      onClick={() => void initial.refetch()}
                    >
                      Retry catalog
                    </button>
                  </p>
                )}
                {applying && applyingRequest.current && <p>Updating item...</p>}
                {announcement && (
                  <p className="workbench-feedback">{announcement}</p>
                )}
                {workbench.data && (
                  <details className="workbench-assumptions">
                    <summary>Last craft and roll assumptions</summary>
                    <p>
                      {workbenchActionNames[workbench.data.action]} applied.{' '}
                      {workbench.data.events.some(
                        (event) => event.kind === 'ADD',
                      )
                        ? 'New modifier selected using PoE2DB published modifier weights.'
                        : workbench.data.action === 'DIVINE'
                          ? 'Numeric values rerolled within their current source ranges.'
                          : 'Modifier removal uses the uniform assumption listed below.'}
                    </p>
                    <p>
                      {workbench.data.ruleVersion} /{' '}
                      {workbench.data.ledgerVersion}
                    </p>
                    {workbench.data.consumedOmens.length > 0 && (
                      <p>
                        Consumed omens:{' '}
                        {workbench.data.consumedOmens
                          .map((id) => id.replaceAll('_', ' '))
                          .join(', ')}
                      </p>
                    )}
                    {workbench.data.assumptions.length ? (
                      workbench.data.assumptions.map((a, i) => (
                        <p key={`${a.id}-${i}`}>
                          Uniform assumption: {a.candidateUnit}, N = {a.n}, each
                          candidate = 1/{a.n}.{' '}
                          {a.min !== null
                            ? `Source range ${a.min} to ${a.max}. `
                            : ''}
                          {a.reason}{' '}
                          <a
                            href={a.sourceUrl}
                            target="_blank"
                            rel="noreferrer"
                          >
                            Source
                          </a>
                        </p>
                      ))
                    ) : (
                      <p>No uniform fallback was needed for this craft.</p>
                    )}
                  </details>
                )}
              </div>
            </div>
          </div>
          <dialog
            ref={dialog}
            onCancel={(event) => {
              event.preventDefault()
              closeInput()
            }}
            onClick={(event) => {
              if (event.target === event.currentTarget) closeInput()
            }}
            id="item-input-panel"
            className="item-input-panel detail-body"
            aria-label="Starting item"
            aria-busy={imported.pending}
          >
            <div className="item-input-content">
              <button
                ref={inputClose}
                type="button"
                className="input-close"
                aria-label="Close item input"
                onClick={closeInput}
              >
                ×
              </button>
              <div className="input-heading">
                <h3>Starting item</h3>
                <span>01</span>
              </div>
              <div
                className="input-tabs"
                role="group"
                aria-label="Item input method"
              >
                <button
                  type="button"
                  aria-pressed={inputMode === 'base'}
                  onClick={() => {
                    imported.invalidate()
                    setInputMode('base')
                  }}
                >
                  Select base
                </button>
                <button
                  type="button"
                  aria-pressed={inputMode === 'text'}
                  onClick={() => setInputMode('text')}
                >
                  Item text
                </button>
              </div>
              {inputMode === 'base' ? (
                <div className="base-form">
                  <label htmlFor="base-select">Amulet base</label>
                  <select id="base-select" defaultValue="solar">
                    <option value="solar">Solar Amulet</option>
                  </select>
                  <label htmlFor="base-level">Item level</label>
                  <input
                    id="base-level"
                    type="number"
                    min="1"
                    max="100"
                    step="1"
                    value={baseLevel}
                    onChange={(e) => setBaseLevel(e.target.value)}
                  />
                  <button
                    type="button"
                    className="primary-action"
                    disabled={
                      !Number.isInteger(Number(baseLevel)) ||
                      Number(baseLevel) < 1 ||
                      Number(baseLevel) > 100
                    }
                    onClick={() => {
                      imported.invalidate()
                      draft.setBase(Number(baseLevel))
                      setPreviewRequest({ count: 0, action: null })
                      setSelected(null)
                      closeInput()
                    }}
                  >
                    Place base <span aria-hidden="true">↗</span>
                  </button>
                </div>
              ) : (
                <div className="text-form">
                  <label htmlFor="item-text">
                    Item text copied from the game
                  </label>
                  <textarea
                    id="item-text"
                    value={draft.text}
                    onChange={(event) => {
                      imported.invalidate()
                      draft.setText(event.target.value)
                    }}
                    placeholder={
                      'Hover over an item and press Ctrl+C\nPaste the copied text here.'
                    }
                    aria-invalid={Boolean(imported.error)}
                    aria-describedby={
                      imported.error ? 'import-error' : undefined
                    }
                  />
                  {imported.error && (
                    <p id="import-error" role="alert">
                      {imported.error}
                    </p>
                  )}
                  <button
                    type="button"
                    className="primary-action"
                    onClick={importText}
                    disabled={imported.pending}
                  >
                    {imported.pending ? 'Analyzing item…' : 'Analyze item'}{' '}
                    <span aria-hidden="true">↗</span>
                  </button>
                </div>
              )}
            </div>
          </dialog>
        </div>
      </div>
      <div
        id="panel-explorer"
        role="tabpanel"
        aria-labelledby="workspace-explorer"
        hidden={view !== 'explorer'}
      >
        <div className="explorer-toolbar">
          <label>
            Starting item level
            <input
              type="number"
              min="1"
              max="100"
              value={baseLevel}
              onChange={(event) => setBaseLevel(event.target.value)}
            />
          </label>
          <button
            type="button"
            disabled={
              !Number.isInteger(Number(baseLevel)) ||
              Number(baseLevel) < 1 ||
              Number(baseLevel) > 100
            }
            onClick={() => {
              draft.setBase(Number(baseLevel))
              setPreviewRequest({ count: 0, action: null })
            }}
          >
            Start new Solar Amulet
          </button>
        </div>
        {draft.source === 'base' ? (
          <CraftingExplorer
            key={draft.baseRevision}
            level={draft.baseItemLevel}
            revision={draft.baseRevision}
            requestCount={previewRequest.count}
            requestedAction={previewRequest.action}
          />
        ) : (
          <p>
            Pasted items are display-only. Start a Solar Amulet to explore
            probabilities.
          </p>
        )}
      </div>
      <div
        id="panel-support"
        role="tabpanel"
        aria-labelledby="workspace-support"
        hidden={view !== 'support'}
      >
        <CraftSupport active={view === 'support'} />
      </div>
      <span className="sr-only" role="status">
        {imported.pending ? 'Analyzing item…' : announcement}
      </span>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span> Your personal
          crafting workbench
        </span>
        <a
          href="https://poe2db.tw/us/Currency"
          target="_blank"
          rel="noreferrer"
        >
          Currency images · PoE2DB ↗
        </a>
      </footer>
      {tooltipsEnabled && !selected && <MaterialTooltip />}
      {cursor && pointer && (
        <span
          className="currency-cursor"
          aria-hidden="true"
          style={{ left: pointer.x + 14, top: pointer.y + 14 }}
        >
          <CurrencyImage key={cursor.id} {...cursor} />
        </span>
      )}
    </main>
  )
}
