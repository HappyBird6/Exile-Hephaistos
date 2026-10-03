import { maximumQuality } from './qualityLimit'
import { currencyNames } from './currencyNames'
import {
  craftProbabilityEvidence,
  modifierWeightSources,
} from './craftProbabilityEvidence'
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
import {
  LocalFilmRepository,
  emptyFilms,
  currentFilm,
  currentFrame,
  startFilm,
  recordCraft,
  viewFrame,
  verifiedHistoryState,
  verifiedFrameEvidence,
  upgradeCompatibleFilms,
} from './workbenchHistory'
import type { Films } from './workbenchHistory'
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

function filmId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function CraftingPage() {
  const client = useQueryClient()
  const draft = useItemDraft()
  const [filmState, setFilmState] = useState(() => {
    try {
      return {
        history: new LocalFilmRepository(window.localStorage).load(),
        revision: draft.baseRevision,
        error: '',
        blockedSaving: false,
      }
    } catch {
      return {
        history: emptyFilms(),
        revision: draft.baseRevision,
        error:
          'Saved history could not be loaded. Existing storage is preserved; new crafts remain available in this page only.',
        blockedSaving: true,
      }
    }
  })
  const storedFrame =
    filmState.revision === draft.baseRevision
      ? currentFrame(filmState.history)
      : undefined
  const storedLevel = storedFrame?.state?.itemLevel
  const catalogBase =
    draft.source === 'text'
      ? 'solar'
      : storedFrame?.state.baseItemId ===
          'Metadata/Items/Armours/Gloves/FourGlovesStr1'
        ? 'stocky'
        : storedFrame?.state.baseItemId ===
            'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
          ? 'bow'
          : storedFrame?.state.baseItemId ===
              'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
            ? 'wand'
            : storedFrame?.state.baseItemId ===
                'Metadata/Items/Armours/BodyArmours/FourBodyStr1'
              ? 'body'
              : storedFrame?.state.baseItemId ===
                  'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
                ? 'sceptre'
                : storedFrame?.state.baseItemId ===
                    'Metadata/Items/Belts/FourBelt1'
                  ? 'belt'
                  : storedFrame?.state.baseItemId ===
                      'Metadata/Items/Armours/Helmets/FourHelmetStr1'
                    ? 'helmet'
                    : storedFrame
                      ? 'solar'
                      : draft.base
  const baseName =
    catalogBase === 'stocky'
      ? 'Stocky Mitts'
      : catalogBase === 'bow'
        ? 'Crude Bow'
        : catalogBase === 'wand'
          ? 'Attuned Wand'
          : catalogBase === 'body'
            ? 'Rusted Cuirass'
            : catalogBase === 'sceptre'
              ? 'Rattling Sceptre'
              : catalogBase === 'belt'
                ? 'Rawhide Belt'
                : catalogBase === 'helmet'
                  ? 'Rusted Greathelm'
                  : 'Solar Amulet'
  const catalogLevel =
    Number.isInteger(storedLevel) && storedLevel! >= 1 && storedLevel! <= 100
      ? storedLevel!
      : draft.baseItemLevel
  function saveFilms(history: Films, revision = draft.baseRevision) {
    let error = filmState.error
    if (!filmState.blockedSaving) {
      try {
        new LocalFilmRepository(window.localStorage).save(history)
        error = ''
      } catch {
        error =
          'History could not be saved. Current films remain available until this page closes.'
      }
    }
    setFilmState({ ...filmState, history, revision, error })
  }
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
  const [altHeld, setAltHeld] = useState(false)
  const [pendingApplication, setApplying] = useState<{
    controller: AbortController
    revision: number
  } | null>(null)
  const applying =
    pendingApplication?.revision === draft.baseRevision
      ? pendingApplication.controller
      : null
  const applyingRequest = useRef<AbortController | null>(null)
  const placementRequest = useRef(0)
  const placementPending = useRef(false)
  const initial = useQuery({
    queryKey: ['crafting', 'initial', catalogBase, catalogLevel],
    queryFn: ({ signal }) => loadInitial(catalogLevel, signal, catalogBase),
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
  const restoredValid =
    storedFrame && initial.data
      ? verifiedHistoryState(storedFrame.state, initial.data)
      : false
  const restoredState =
    restoredValid && initial.data && storedFrame
      ? { ...storedFrame.state, snapshotId: initial.data.metadata.snapshotId }
      : undefined
  const canCraft =
    (draft.source === 'base' || mapping.data?.mapped === true) &&
    (!storedFrame || restoredValid)
  const film =
    filmState.revision === draft.baseRevision
      ? currentFilm(filmState.history)
      : undefined
  async function startBase(
    level: number,
    base:
      | 'solar'
      | 'stocky'
      | 'bow'
      | 'wand'
      | 'body'
      | 'sceptre'
      | 'belt'
      | 'helmet' = 'solar',
  ) {
    const request = ++placementRequest.current
    const revision = useItemDraft.getState().baseRevision
    placementPending.current = true
    applyingRequest.current?.abort()
    applyingRequest.current = null
    setApplying(null)
    try {
      const data = await client.fetchQuery({
        queryKey: ['crafting', 'initial', base, level],
        queryFn: ({ signal }) => loadInitial(level, signal, base),
        staleTime: 60_000,
      })
      if (
        request !== placementRequest.current ||
        revision !== useItemDraft.getState().baseRevision
      )
        return
      draft.setBase(level, base)
      saveFilms(
        startFilm(filmState.history, concreteInitial(data), filmId()),
        useItemDraft.getState().baseRevision,
      )
      setSelected(null)
      setHeld(null)
      setAnnouncement('')
    } catch {
      setAnnouncement(
        'Could not load the selected base. Your item and saved films are preserved.',
      )
    } finally {
      if (request === placementRequest.current) placementPending.current = false
    }
  }
  async function restoreFilm(id: string) {
    if (applyingRequest.current) return
    const selectedFilm = filmState.history.films.find(
      (entry) => entry.id === id,
    )
    if (!selectedFilm) return
    const request = ++placementRequest.current
    const revision = useItemDraft.getState().baseRevision
    placementPending.current = true
    const state = selectedFilm.frames.at(-1)!.state
    const base =
      state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
        ? 'stocky'
        : 'solar'
    try {
      const data = await client.fetchQuery({
        queryKey: ['crafting', 'initial', base, state.itemLevel],
        queryFn: ({ signal }) => loadInitial(state.itemLevel, signal, base),
        staleTime: 60_000,
      })
      if (
        request !== placementRequest.current ||
        revision !== useItemDraft.getState().baseRevision
      )
        return
      if (!verifiedHistoryState(state, data)) {
        setAnnouncement(
          'This saved session does not match the current catalog. It has been preserved.',
        )
        return
      }
      draft.setBase(state.itemLevel, base)
      saveFilms(
        {
          ...filmState.history,
          active: selectedFilm.id,
          cursor: selectedFilm.frames.length - 1,
        },
        useItemDraft.getState().baseRevision,
      )
      setSelected(null)
      setHeld(null)
      setAnnouncement('')
    } catch {
      setAnnouncement(
        'Could not load this saved session catalog. Saved films are preserved.',
      )
    } finally {
      if (request === placementRequest.current) placementPending.current = false
    }
  }
  function browse(cursor: number) {
    if (applyingRequest.current || placementPending.current) return
    saveFilms(viewFrame(filmState.history, cursor))
    setSelected(null)
    setHeld(null)
    setPointer(null)
    setAnnouncement('')
  }
  useEffect(
    () => () => {
      applyingRequest.current?.abort()
      applyingRequest.current = null
      placementRequest.current++
      placementPending.current = false
    },
    [draft.baseRevision, draft.source],
  )
  const [baseLevel, setBaseLevel] = useState('82')
  const [baseChoice, setBaseChoice] = useState<
    'solar' | 'stocky' | 'bow' | 'wand' | 'body' | 'sceptre' | 'belt' | 'helmet'
  >('solar')
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
      if (event.key === 'Alt') setAltHeld(true)
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
    const release = (event: KeyboardEvent) => {
      if (event.key === 'Alt') setAltHeld(false)
    }
    const hide = () => {
      setPointer(null)
      setAltHeld(false)
    }
    window.addEventListener('keydown', cancel)
    window.addEventListener('keyup', release)
    window.addEventListener('blur', hide)
    return () => {
      window.removeEventListener('keydown', cancel)
      window.removeEventListener('keyup', release)
      window.removeEventListener('blur', hide)
    }
  }, [inputOpen])

  function choose(resource: Selection) {
    setSelected(resource)
    setHeld(null)
    setAnnouncement(`${resource.name} selected · Click the central item.`)
  }
  function toggleOmen(id: string) {
    if (applyingRequest.current) return
    const omen = workbenchOmens.find((entry) => entry.id === id)
    if (!omen) {
      setAnnouncement('This omen has no verified Workbench rule yet.')
      return
    }
    const active = draft.activeOmens.includes(id)
    const conflict = workbenchOmens.find(
      (entry) =>
        entry.id !== id &&
        entry.trigger === omen.trigger &&
        draft.activeOmens.includes(entry.id),
    )
    if (!active && conflict) {
      setAnnouncement(
        `Deactivate ${conflict.id.replaceAll('_', ' ')} first. This combination has not been verified.`,
      )
      return
    }
    draft.setActiveOmens(
      active
        ? draft.activeOmens.filter((entry) => entry !== id)
        : [...draft.activeOmens, id],
    )
    setSelected(null)
    setHeld(null)
    setPointer(null)
    setAnnouncement(
      `${id.replaceAll('_', ' ')} ${active ? 'deactivated' : 'activated'}.`,
    )
  }
  async function apply(repeat = false) {
    if (applyingRequest.current || placementPending.current) return
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
      setAnnouncement(
        'Move this omen to a favorite slot, then right-click the favorite to activate it.',
      )
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
    setApplying({ controller, revision: draft.baseRevision })
    const revision = draft.baseRevision
    const state =
      restoredState ??
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
        const history =
          filmState.revision === revision
            ? filmState.history
            : { ...filmState.history, active: null, cursor: 0 }
        saveFilms(
          recordCraft(
            upgradeCompatibleFilms(history, initial.data),
            state,
            result,
            filmId(),
          ),
          revision,
        )
        draft.setActiveOmens(result.remainingOmens)
        setAnnouncement(
          `${workbenchActionNames[action]} applied. Current item updated.${result.consumedOmens.length ? ` Consumed: ${result.consumedOmens.map((id) => id.replaceAll('_', ' ')).join(', ')}.` : ''}${result.assumptions.length ? ' Uniform probability assumptions were used; see the roll assumptions.' : ''}`,
        )
        if (!repeat) {
          setSelected(null)
          setHeld(null)
          setPointer(null)
        }
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
    if (await imported.submit(draft.text)) {
      saveFilms(
        { ...filmState.history, active: null, cursor: 0 },
        useItemDraft.getState().baseRevision,
      )
      closeInput()
    }
    setSelected(null)
    setHeld(null)
  }
  async function enableCrafting() {
    if (applyingRequest.current || !imported.data || !initial.data) return
    const controller = new AbortController()
    const revision = draft.baseRevision
    applyingRequest.current = controller
    setApplying({ controller, revision: draft.baseRevision })
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
      if (result.mapped && result.state)
        saveFilms(
          startFilm(filmState.history, result.state, filmId()),
          revision,
        )
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
      else {
        setSelected(null)
        setPointer(null)
      }
      return
    }
    const replaced = favorites[index]
    if (
      replaced &&
      !favorites.some(
        (entry, position) => position !== index && entry?.id === replaced.id,
      )
    )
      draft.setActiveOmens(draft.activeOmens.filter((id) => id !== replaced.id))
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
    draft.source === 'base' ? baseName : (item?.displayName ?? 'Pasted item')
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
  const concrete = restoredState ?? workbench.data?.state ?? mapping.data?.state
  const craftEvidence =
    storedFrame && initial.data
      ? verifiedFrameEvidence(storedFrame, initial.data)
      : workbench.data &&
          JSON.stringify(concrete) === JSON.stringify(workbench.data.state)
        ? workbench.data
        : undefined
  const evidenceInvalid =
    storedFrame?.evidence !== undefined && restoredValid && !craftEvidence
  const qualityMaximum =
    concrete && initial.data
      ? maximumQuality(concrete, initial.data.modifiers)
      : null
  const card =
    item && !concrete
      ? toItemCard(item)
      : concrete && initial.data
        ? {
            rarity: concrete.rarity,
            name: item?.displayName ?? baseName,
            base: baseName,
            itemClass:
              catalogBase === 'stocky'
                ? 'Gloves'
                : catalogBase === 'bow'
                  ? 'Bows'
                  : catalogBase === 'wand'
                    ? 'Wands'
                    : catalogBase === 'body'
                      ? 'Body Armours'
                      : catalogBase === 'sceptre'
                        ? 'Sceptres'
                        : catalogBase === 'belt'
                          ? 'Belts'
                          : catalogBase === 'helmet'
                            ? 'Helmets'
                            : 'Amulet',
            itemLevel: concrete.itemLevel,
            properties: [
              ...(catalogBase === 'helmet'
                ? [
                    {
                      id: 'base-armour',
                      text: 'Base Armour: 29 (not computed)',
                    },
                  ]
                : []),
              ...(catalogBase === 'belt'
                ? [
                    {
                      id: 'belt-implicit',
                      text: 'Flask life recovery: unknown (20–30% base range)',
                    },
                    {
                      id: 'belt-charm',
                      text: 'Charm slots: unknown (not modeled)',
                    },
                  ]
                : []),
              ...(catalogBase === 'sceptre'
                ? [
                    {
                      id: 'base-spirit',
                      text: 'Base Spirit: 100 (not computed)',
                    },
                    {
                      id: 'innate-skill',
                      text: 'Grants Skill: Skeletal Warrior (not simulated)',
                    },
                  ]
                : []),
              ...(catalogBase === 'body'
                ? [
                    {
                      id: 'base-armour',
                      text: 'Base Armour: 45 (not computed)',
                    },
                  ]
                : []),
              ...(catalogBase === 'wand'
                ? [
                    {
                      id: 'innate-skill',
                      text: 'Grants Skill: Mana Drain (not simulated)',
                    },
                  ]
                : []),
              ...(qualityMaximum === null
                ? []
                : [
                    {
                      id: 'maximum-quality',
                      text: `Maximum Quality: ${qualityMaximum}%`,
                    },
                  ]),
              ...(catalogBase === 'stocky'
                ? [
                    {
                      id: 'augment-sockets',
                      text:
                        concrete.augmentSockets == null
                          ? 'Augment Sockets: unknown'
                          : `Augment Sockets: ${concrete.augmentSockets} / 1`,
                    },
                  ]
                : []),
            ],
            requirements: [],
            flags: [],
            modifiers: [...concrete.implicits, ...concrete.explicits].map(
              (m, i) => {
                const d = initial.data!.modifiers[m.modifierId]!
                return {
                  id: m.modifierId,
                  text: `${'fractured' in m && m.fractured ? '[Fractured] ' : ''}${altHeld ? d.text : rolledText(d, m.values)}`,
                  kind:
                    i < concrete.implicits.length
                      ? ('implicit' as const)
                      : ('explicit' as const),
                  affixLabel:
                    d.affixType === 'NONE'
                      ? undefined
                      : `${d.affixType === 'PREFIX' ? 'P' : 'S'}${d.tier}`,
                }
              },
            ),
          }
        : null

  return (
    <main
      className="craft-page"
      onClick={(event) => {
        const target = event.target as HTMLElement
        if (!target.closest('button, input, label, dialog, a')) {
          setSelected(null)
          setHeld(null)
          setPointer(null)
        }
      }}
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
        </div>
        <span className="preview-badge">
          <i />
          {baseName} · Base modifiers
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
            <div className="stash-viewport">
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
                    disabled={applying !== null}
                    onClick={(event) => void apply(event.shiftKey)}
                  >
                    {draft.source === 'base' && catalogBase === 'solar' ? (
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
                      className={`currency-slot favorite-slot ${resource && selected?.id === resource.id ? 'is-selected' : ''} ${resource && draft.activeOmens.includes(resource.id) ? 'is-active-omen' : ''}`}
                      style={{
                        left: `${(652 + (index % 3) * 90) / 9.35}%`,
                        top: `${(40 + Math.floor(index / 3) * 100) / 5.5}%`,
                      }}
                      aria-label={`Favorite slot ${index + 1}: ${resource?.name ?? 'empty'}`}
                      aria-pressed={Boolean(
                        resource &&
                        (selected?.id === resource.id ||
                          draft.activeOmens.includes(resource.id)),
                      )}
                      {...(resource ? tooltipEvents(resource.id) : {})}
                      onClick={() => placeFavorite(index)}
                      onContextMenu={(event) => {
                        event.preventDefault()
                        if (resource) {
                          if (resource.category === 'Omen') {
                            toggleOmen(resource.id)
                            return
                          }
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
            </div>
            <div className="bench-lower">
              <div className="bench-item-card">
                <div className="workbench-film-controls">
                  <button
                    type="button"
                    aria-label="Previous crafting step"
                    disabled={
                      !film ||
                      filmState.history.cursor === 0 ||
                      applying !== null
                    }
                    onClick={() => browse(filmState.history.cursor - 1)}
                  >
                    ‹
                  </button>
                  <span>
                    {film
                      ? `Step ${filmState.history.cursor} / ${film.frames.length - 1}`
                      : 'New craft'}
                  </span>
                  <button
                    type="button"
                    aria-label="Next crafting step"
                    disabled={
                      !film ||
                      filmState.history.cursor === film.frames.length - 1 ||
                      applying !== null
                    }
                    onClick={() => browse(filmState.history.cursor + 1)}
                  >
                    ›
                  </button>
                </div>
                {filmState.history.films.length > 0 && (
                  <label className="workbench-film-select">
                    Crafting session
                    <select
                      aria-label="Crafting session"
                      value={film?.id ?? ''}
                      disabled={applying !== null}
                      onChange={(event) => void restoreFilm(event.target.value)}
                    >
                      <option value="" disabled>
                        New craft
                      </option>
                      {filmState.history.films.map((entry, index) => (
                        <option key={entry.id} value={entry.id}>
                          Session {index + 1} · {entry.frames.length - 1} crafts
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                {filmState.error && <p role="alert">{filmState.error}</p>}
                {storedFrame && initial.data && !restoredValid && (
                  <p role="alert">
                    Saved step does not match the current catalog. Start a new
                    equipment base; saved films are preserved.
                  </p>
                )}
                {card ? (
                  <ItemCard item={card} />
                ) : (
                  <ItemCard
                    item={{
                      rarity: 'NORMAL',
                      name: baseName,
                      base: baseName,
                      itemClass:
                        catalogBase === 'stocky'
                          ? 'Gloves'
                          : catalogBase === 'bow'
                            ? 'Bows'
                            : catalogBase === 'wand'
                              ? 'Wands'
                              : catalogBase === 'body'
                                ? 'Body Armours'
                                : catalogBase === 'sceptre'
                                  ? 'Sceptres'
                                  : catalogBase === 'belt'
                                    ? 'Belts'
                                    : catalogBase === 'helmet'
                                      ? 'Helmets'
                                      : 'Amulet',
                      itemLevel: draft.baseItemLevel,
                      properties: [],
                      requirements: [],
                      modifiers:
                        catalogBase !== 'solar'
                          ? []
                          : [
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
                {(selected?.id === 'Essence_of_the_Breach' ||
                  concrete?.explicits.some(
                    (m) =>
                      m.modifierId === 'amulet:prefix:essence-maximum-quality',
                  )) && (
                  <p className="workbench-feedback">
                    Maximum Quality modifier supported. Applying Catalyst
                    quality is not supported yet.
                  </p>
                )}
                {(selected?.id === 'Essence_of_the_Abyss' ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':essence-abyssal-mark'),
                  )) && (
                  <p className="workbench-feedback">
                    Mark modifier supported. Desecration and revealing its
                    result are not supported yet.
                  </p>
                )}
                {((selected?.id === 'Essence_of_Horror' &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':essence-socketed-augment-effect'),
                  )) && (
                  <p className="workbench-feedback">
                    60% Socketed Augment Item effect modifier supported.
                    Socketing and Rune/Soul Core effect calculations are not
                    supported yet.
                  </p>
                )}
                {(([
                  'Perfect_Essence_of_Grounding',
                  'Perfect_Essence_of_Opulence',
                ].includes(selected?.id ?? '') &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some(
                    (m) =>
                      m.modifierId.endsWith(':essence-lightning-recoup') ||
                      m.modifierId.endsWith(':essence-gold-quantity'),
                  )) && (
                  <p className="workbench-feedback">
                    Supported gloves: item level 72+. Lower item-level use is
                    not verified. Modifier assignment only; Recoup recovery and
                    Gold drop totals are not calculated.
                  </p>
                )}
                {(([
                  'Expansive_Alloy',
                  'Cyclonic_Alloy',
                  'Mystic_Alloy',
                ].includes(selected?.id ?? '') &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    [
                      ':alloy-remnant-pickup-range',
                      ':alloy-damaging-ailment-duration',
                      ':alloy-attack-area-of-effect',
                    ].some((id) => m.modifierId.endsWith(id)),
                  )) && (
                  <p className="workbench-feedback">
                    Supported gloves: Expansive item level 25+; Cyclonic and
                    Mystic 45+. Lower item-level use is not verified. Modifier
                    assignment only; Remnant collection, ailment duration and
                    attack area are not calculated. Crystallisation omens do not
                    apply to Alloys.
                  </p>
                )}
                {(([
                  'Adaptive_Alloy',
                  'Swift_Alloy',
                  'Sovereign_Alloy',
                ].includes(selected?.id ?? '') &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    [
                      ':alloy-attack-speed-missing-ward',
                      ':alloy-cast-speed',
                      ':alloy-local-runic-ward',
                    ].some((id) => m.modifierId.endsWith(id)),
                  )) && (
                  <p className="workbench-feedback">
                    Supported gloves: Adaptive and Sovereign item level 25+;
                    Swift 45+. Lower item-level use is not verified. Modifier
                    assignment only; missing-Ward conditions, attack/cast speed
                    and Local Ward totals are not calculated. Crystallisation
                    omens do not apply to Alloys.
                  </p>
                )}
                {((selected?.id === 'Prismatic_Alloy' &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':alloy-elemental-penetration'),
                  )) && (
                  <p className="workbench-feedback">
                    Supported gloves: item level 45+. Lower item-level use is
                    not verified. Penetration modifier only; enemy resistances
                    and damage are not calculated. Crystallisation omens do not
                    apply to Alloys.
                  </p>
                )}
                {!canCraft && (
                  <p>
                    Pasted items are display-only until catalog mapping is
                    verified.
                  </p>
                )}
                {draft.source === 'text' && !mapping.data?.mapped && (
                  <button
                    type="button"
                    onClick={() => void enableCrafting()}
                    disabled={applying !== null || !initial.data}
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
                {applying && <p>Updating item...</p>}
                {announcement && (
                  <p className="workbench-feedback">{announcement}</p>
                )}
                {evidenceInvalid && (
                  <p role="alert">
                    Saved craft evidence could not be verified. Item history is
                    preserved.
                  </p>
                )}
                {catalogBase === 'helmet' && (
                  <p className="workbench-feedback">
                    Armour is a base fact; computed Armour, applied quality,
                    sockets and pasted mapping are unsupported. Blessed has no
                    implicit target.
                  </p>
                )}
                {catalogBase === 'belt' && (
                  <p className="workbench-feedback">
                    Explicit affix crafting only. Variable flask implicit and
                    Charm slots are unknown; Divine, Blessed, applied quality,
                    sockets and pasted mapping are unsupported. Quality maximum
                    is unknown.
                  </p>
                )}
                {catalogBase === 'sceptre' && (
                  <p className="workbench-feedback">
                    Spirit and Skeletal Warrior are base facts; their totals,
                    applied quality, sockets and pasted mapping are unsupported.
                    Item-level gates use source modifier levels; lower-level
                    game behavior is unverified.
                  </p>
                )}
                {catalogBase === 'body' && (
                  <p className="workbench-feedback">
                    Computed Armour, movement speed, applied quality, sockets
                    and pasted Body Armour mapping are not simulated. Numeric
                    rolls use unverified source-unit/shared-ratio models.
                    Item-level gates use source modifier levels; lower-level
                    game behavior is unverified. Delirium Notable outcomes are
                    not supported.
                  </p>
                )}
                {catalogBase === 'wand' && (
                  <p className="workbench-feedback">
                    Innate Mana Drain, combat totals, applied quality, sockets
                    and pasted Wand mapping are not simulated. Numeric rolls use
                    unverified source-unit/shared-ratio models. Item-level gates
                    use source modifier levels; lower-level game behavior is
                    unverified.
                  </p>
                )}
                {catalogBase === 'bow' && (
                  <p className="workbench-feedback">
                    Numeric rolls use unverified source-unit and shared-ratio
                    models. Computed weapon damage, quality, sockets and pasted
                    bow mapping are not supported. Item-level gates use source
                    modifier levels; lower-level game behavior is unverified.
                  </p>
                )}
                {catalogBase === 'stocky' && (
                  <p className="workbench-feedback">
                    Numeric rolls use unverified source-unit and shared-ratio
                    models. Base Armour: 15; computed Armour, applied quality,
                    socketed Augment effects and pasted glove mapping are not
                    supported.
                  </p>
                )}
                {craftEvidence && (
                  <details className="workbench-assumptions">
                    <summary>Last craft and roll assumptions</summary>
                    <p>
                      {workbenchActionNames[craftEvidence.action]} applied.{' '}
                      {craftProbabilityEvidence(craftEvidence).text}
                    </p>
                    {craftProbabilityEvidence(craftEvidence).weighted && (
                      <p>
                        <a
                          href={modifierWeightSources[catalogBase]}
                          target="_blank"
                          rel="noreferrer"
                        >
                          PoE2DB modifier table
                        </a>
                        : published DropChance values supply model weights;
                        ordered Spawn Tags establish eligibility only. The
                        difference between these source fields remains
                        unresolved.
                      </p>
                    )}
                    <p>
                      {craftEvidence.ruleVersion} /{' '}
                      {craftEvidence.ledgerVersion}
                    </p>
                    {craftEvidence.consumedOmens.length > 0 && (
                      <p>
                        Consumed omens:{' '}
                        {craftEvidence.consumedOmens
                          .map((id) => id.replaceAll('_', ' '))
                          .join(', ')}
                      </p>
                    )}
                    {craftEvidence.assumptions.length ? (
                      craftEvidence.assumptions.map((a, i) => (
                        <p key={`${a.id}-${i}`}>
                          {a.id === 'user-coupled-ratio-half-up-v1'
                            ? 'Unverified coupled roll model'
                            : a.id === 'assumed-source-integer-roll-v1'
                              ? 'Unverified source-unit roll model'
                              : 'Uniform assumption'}
                          : {a.candidateUnit}, N = {a.n}, each candidate = 1/
                          {a.n}.{' '}
                          {a.id === 'user-coupled-ratio-half-up-v1' &&
                          a.min !== null
                            ? `Assumed ratio ticks ${a.min} to ${a.max}. `
                            : ''}
                          {a.reason}{' '}
                          {a.ratioTick != null
                            ? `Sampled ratio ${a.ratioTick}/10000. `
                            : ''}
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
                  <label htmlFor="base-select">Equipment base</label>
                  <select
                    id="base-select"
                    value={baseChoice}
                    onChange={(e) =>
                      setBaseChoice(
                        e.target.value as
                          | 'solar'
                          | 'stocky'
                          | 'bow'
                          | 'wand'
                          | 'body'
                          | 'sceptre'
                          | 'belt'
                          | 'helmet',
                      )
                    }
                  >
                    <option value="solar">Solar Amulet</option>
                    <option value="stocky">Stocky Mitts</option>
                    <option value="bow">Crude Bow</option>
                    <option value="wand">Attuned Wand</option>
                    <option value="body">Rusted Cuirass</option>
                    <option value="sceptre">Rattling Sceptre</option>
                    <option value="belt">Rawhide Belt</option>
                    <option value="helmet">Rusted Greathelm</option>
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
                      void startBase(Number(baseLevel), baseChoice)
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
              void startBase(Number(baseLevel))
              setPreviewRequest({ count: 0, action: null })
            }}
          >
            Start new Solar Amulet
          </button>
        </div>
        {draft.source === 'base' && catalogBase === 'solar' ? (
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
        {catalogBase === 'solar' ? (
          <CraftSupport active={view === 'support'} />
        ) : (
          <p>
            {baseName} is supported in Workbench only. Select Solar Amulet to
            use Craft Support.
          </p>
        )}
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
