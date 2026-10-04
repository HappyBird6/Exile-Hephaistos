import {
  useI18n,
  matchesGameName,
  uiText,
  translate,
} from '../../shared/i18n/i18n'
import { LocaleSelector } from '../../shared/i18n/LocaleSelector'
import { localizedAction, catalystItemIds } from './localizedCrafting'
import { localizedModifierText } from './localizedModifiers'
import { inServiceScope } from './serviceScope'
import { maximumQuality } from './qualityLimit'
import { catalystTypes, catalystProjection } from './catalystQuality'
import type { CatalystQuality } from './catalystQuality'
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
  workbenchOmens,
  legacyOmenIds,
  compatibleOmenPair,
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
const baseSlugs = {
  solar: 'Solar_Amulet',
  stocky: 'Stocky_Mitts',
  bow: 'Crude_Bow',
  wand: 'Attuned_Wand',
  body: 'Rusted_Cuirass',
  sceptre: 'Rattling_Sceptre',
  belt: 'Rawhide_Belt',
  ring: 'Iron_Ring',
  helmet: 'Rusted_Greathelm',
} as const

type Selection = { id: string; name: string; image: string }

function filmId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function CraftingPage() {
  const { t, name } = useI18n()
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
        error: t('notice.history_load'),
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
                      'Metadata/Items/Rings/FourRing1'
                    ? 'ring'
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
                : catalogBase === 'ring'
                  ? 'Iron Ring'
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
        error = t('notice.history_save')
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
  const [showLegacyOmens, setShowLegacyOmens] = useState(false)
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
      | 'helmet'
      | 'ring' = 'solar',
    catalystQuality: CatalystQuality | null = null,
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
      const root = {
        ...concreteInitial(data),
        ...(catalystQuality ? { catalystQuality } : {}),
      }
      if (!verifiedHistoryState(root, data))
        throw new Error('Unsupported starting quality')
      draft.setBase(level, base)
      saveFilms(
        startFilm(filmState.history, root, filmId()),
        useItemDraft.getState().baseRevision,
      )
      setSelected(null)
      setHeld(null)
      setAnnouncement('')
    } catch {
      setAnnouncement(t('notice.load_base'))
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
        : state.baseItemId === 'Metadata/Items/Rings/FourRing1'
          ? 'ring'
          : state.baseItemId === 'Metadata/Items/Armours/Helmets/FourHelmetStr1'
            ? 'helmet'
            : state.baseItemId === 'Metadata/Items/Belts/FourBelt1'
              ? 'belt'
              : state.baseItemId ===
                  'Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1'
                ? 'sceptre'
                : state.baseItemId ===
                    'Metadata/Items/Armours/BodyArmours/FourBodyStr1'
                  ? 'body'
                  : state.baseItemId ===
                      'Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3'
                    ? 'wand'
                    : state.baseItemId ===
                        'Metadata/Items/Weapons/TwoHandWeapons/Bows/FourBow1'
                      ? 'bow'
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
        setAnnouncement(t('notice.saved_session'))
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
      setAnnouncement(t('notice.session_catalog'))
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
  const [qualityType, setQualityType] = useState<'' | CatalystQuality['type']>(
    '',
  )
  const [qualityAmount, setQualityAmount] = useState('0')
  const [baseChoice, setBaseChoice] = useState<
    | 'solar'
    | 'stocky'
    | 'bow'
    | 'wand'
    | 'body'
    | 'sceptre'
    | 'belt'
    | 'helmet'
    | 'ring'
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
        setAnnouncement(translate('notice.selection_cleared'))
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
    setAnnouncement(
      t('material.selected', { name: name(resource.id, resource.name) }),
    )
  }
  function toggleOmen(id: string) {
    if (applyingRequest.current) return
    const omen = workbenchOmens.find((entry) => entry.id === id)
    if (!omen) {
      setAnnouncement(t('notice.omen_rule'))
      return
    }
    const active = draft.activeOmens.includes(id)
    const conflict = workbenchOmens.find(
      (entry) =>
        entry.id !== id &&
        entry.trigger === omen.trigger &&
        !compatibleOmenPair(entry.id, id) &&
        draft.activeOmens.includes(entry.id),
    )
    if (!active && conflict) {
      setAnnouncement(
        t('omen.conflict', {
          name: name(conflict.id, conflict.id.replaceAll('_', ' ')),
        }),
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
      t(active ? 'omen.deactivate' : 'omen.activate', {
        name: name(id, id.replaceAll('_', ' ')),
      }),
    )
  }
  async function apply(repeat = false) {
    if (applyingRequest.current || placementPending.current) return
    if (!selected) {
      setAnnouncement(t('notice.select_currency'))
      return
    }
    if (!canCraft) {
      setAnnouncement(
        'The item has not changed. Pasted items are display-only. Select the Solar Amulet base to explore probabilities.',
      )
      return
    }
    if (workbenchOmens.some((omen) => omen.id === selected.id)) {
      setAnnouncement(t('notice.omen_favorite'))
      return
    }
    const action = workbenchCurrencyActions[selected.id]
    if (!action) {
      setAnnouncement(t('notice.no_rule'))
      return
    }
    if (!initial.data) {
      setAnnouncement(t('notice.wait_catalog'))
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
          [
            t('craft.applied', { name: localizedAction(action) }),
            result.consumedOmens.length
              ? t('craft.consumed', {
                  names: result.consumedOmens
                    .map((id) => name(id, id.replaceAll('_', ' ')))
                    .join(', '),
                })
              : '',
            result.assumptions.length ? t('craft.uniform') : '',
          ]
            .filter(Boolean)
            .join(' '),
        )
        if (!repeat) {
          setSelected(null)
          setHeld(null)
          setPointer(null)
        }
      } else
        setAnnouncement(t('craft.blocked', { reason: result.reason ?? '' }))
    } catch (error) {
      if (!controller.signal.aborted)
        setAnnouncement(
          error instanceof Error ? error.message : t('notice.apply_error'),
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
    setAnnouncement(
      t('material.registered', {
        name: name(held.id, held.name),
        index: index + 1,
      }),
    )
    setHeld(null)
  }

  const item = draft.source === 'text' ? imported.data : undefined
  const displayedName =
    draft.source === 'base'
      ? name(baseSlugs[catalogBase], baseName)
      : (item?.displayName ?? 'Pasted item')
  const search = searches[activeTab] ?? ''
  const currentMaterials = materials.filter(
    (m) =>
      inServiceScope(m.id) &&
      m.category === activeTab &&
      (showLegacyOmens || !legacyOmenIds.includes(m.id)) &&
      matchesGameName(m.id, m.name, search),
  )
  function materialButton(material: Material) {
    return (
      <button
        type="button"
        key={material.id}
        className={`material-entry ${held?.id === material.id ? 'is-held' : ''} ${selected?.id === material.id ? 'is-selected' : ''}`}
        aria-label={name(material.id, material.name)}
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
            t('material.picked', { name: name(material.id, material.name) }),
          )
        }}
      >
        <span className="material-entry__image">
          <CurrencyImage key={material.id} {...material} />
        </span>
        <span>
          {name(material.id, material.name)}
          {legacyOmenIds.includes(material.id) ? t('legacy.suffix') : ''}
        </span>
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
                          : catalogBase === 'ring'
                            ? 'Rings'
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
              ...(concrete.catalystQuality
                ? [
                    {
                      id: 'catalyst-quality',
                      text: `Quality (${catalystTypes[concrete.catalystQuality.type].name}): +${concrete.catalystQuality.amount}%`,
                    },
                  ]
                : []),
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
                const projection = catalystProjection(
                  d,
                  m.values,
                  concrete.catalystQuality,
                )
                const displayText =
                  projection.status === 'SCALED_INTEGER' &&
                  d.id === 'iron-ring:implicit:added-physical-damage-to-attacks'
                    ? `Adds ${projection.values.attack_minimum_added_physical_damage} to ${projection.values.attack_maximum_added_physical_damage} Physical Damage to Attacks`
                    : localizedModifierText(d, projection.values)
                return {
                  id: m.modifierId,
                  text: `${'fractured' in m && m.fractured ? '[Fractured] ' : ''}${altHeld ? localizedModifierText(d) : displayText}`,
                  detail: concrete.catalystQuality
                    ? `Original roll: ${rolledText(d, m.values)}\nQuality display: ${projection.status}. Secondary source model; game engine precision is not guaranteed.`
                    : undefined,
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
        <a
          className="craft-brand"
          href="/"
          aria-label={t('ui.exile_hephaistos_home')}
        >
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>{t('ui.path_of_exile_2_crafting_workbench')}</small>
          </span>
        </a>
        <a className="admin-link" href="/admin">
          {t('ui.admin')}
        </a>
        <LocaleSelector />
      </header>
      <nav className="workspace-nav" aria-label={t('ui.main_navigation')}>
        <div role="tablist" aria-label={t('ui.crafting_workspace')}>
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
              {t(`workspace.${tab}`)}
            </button>
          ))}
        </div>
      </nav>
      <div className="craft-title">
        <div>
          <p className="craft-kicker">{t('ui.the_crafting_bench')}</p>
          <h1>
            {view === 'workbench'
              ? t('ui.crafting_workbench')
              : t(`workspace.${view}`)}
          </h1>
        </div>
        <span className="preview-badge">
          <i />
          {name(baseSlugs[catalogBase], baseName)} · {t('ui.base')}
        </span>
      </div>
      <aside className="locale-disclosure">
        <p>{t('probability.disclaimer')}</p>
        <small>{t('translation.coverage')}</small>
      </aside>
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
              aria-label={t('ui.material_stash_tabs')}
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
                  {t(`stash.${tab.id}`)}
                </button>
              ))}
            </div>
            <label className="tooltip-toggle">
              <input
                type="checkbox"
                checked={tooltipsEnabled}
                onChange={(event) => setTooltipsEnabled(event.target.checked)}
              />
              {t('ui.show_tooltips')}
            </label>
          </div>
          <div className="stash-board">
            <div className="stash-viewport">
              <div className="stash-canvas" aria-label={t('ui.currency_stash')}>
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
                    currencies
                      .filter((currency) => inServiceScope(currency.id))
                      .map((currency) => {
                        const currencyName = name(
                          currency.id,
                          currencyNames[currency.id],
                        )
                        return (
                          <button
                            key={currency.id}
                            type="button"
                            className={`currency-slot ${selected?.id === currency.id ? 'is-selected' : ''}`}
                            style={{
                              left: `${currency.x / 9.35}%`,
                              top: `${currency.y / 5.5}%`,
                            }}
                            aria-label={currencyName}
                            aria-pressed={selected?.id === currency.id}
                            {...tooltipEvents(currency.id)}
                            onContextMenu={(event) => {
                              event.preventDefault()
                              setPointer({ x: event.clientX, y: event.clientY })
                              choose({
                                ...currency,
                                name: currencyNames[currency.id],
                              })
                            }}
                            onClick={(event) => {
                              if (event.detail === 0) setPointer(null)
                              choose({
                                ...currency,
                                name: currencyNames[currency.id],
                              })
                            }}
                          >
                            <CurrencyImage {...currency} name={currencyName} />
                            {currency.id.startsWith('Greater') && (
                              <span
                                className="currency-tier"
                                aria-hidden="true"
                              >
                                II
                              </span>
                            )}
                            {currency.id.startsWith('Perfect') && (
                              <span
                                className="currency-tier"
                                aria-hidden="true"
                              >
                                III
                              </span>
                            )}
                          </button>
                        )
                      })
                  ) : (
                    <>
                      {activeTab === 'Omen' && (
                        <div className="legacy-omen-info">
                          <label>
                            <input
                              type="checkbox"
                              checked={showLegacyOmens}
                              onChange={(event) =>
                                setShowLegacyOmens(event.target.checked)
                              }
                            />{' '}
                            {t('ui.show_legacy_omens')}
                          </label>
                          {showLegacyOmens && (
                            <p>
                              Homogenising: drops disabled in 0.4; existing
                              items work. Alchemy, Coronation and Greater
                              Annulment: no longer obtainable in 0.3. Legacy
                              effects are modeled on ordinary currency; current
                              acquisition is not asserted. Ordinary currency
                              only. Greater Exaltation uses pre-craft tags for
                              both additions. Other same-trigger combinations
                              and catalyst quality are unsupported. Failed
                              requests preserve resources here; actual game
                              failure consumption is unverified.{' '}
                              <a
                                href="https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff"
                                target="_blank"
                                rel="noreferrer"
                              >
                                {t('ui.official_homogenising_source')}
                              </a>{' '}
                              <a
                                href="https://www.pathofexile.com/forum/view-thread/3826682"
                                target="_blank"
                                rel="noreferrer"
                              >
                                {t('ui.official_0_3_availability')}
                              </a>
                            </p>
                          )}
                        </div>
                      )}
                      <input
                        type="search"
                        className="material-search"
                        aria-label={t('material.search', {
                          category: t(`stash.${activeTab}`),
                        })}
                        placeholder={t('ui.search')}
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
                            {t('ui.no_materials_found')}
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
                    aria-label={t('ui.special_essences')}
                  >
                    {specialEssences.map(materialButton)}
                  </div>
                )}
                <div className="item-placement">
                  <span className="placement-label">
                    {t('ui.current_item')}
                  </span>
                  <button
                    className={`item-slot ${selected ? 'is-ready' : ''}`}
                    type="button"
                    aria-label={t(
                      'ui.use_selected_currency_on_the_central_item',
                    )}
                    disabled={applying !== null}
                    onClick={(event) => void apply(event.shiftKey)}
                  >
                    {draft.source === 'base' && catalogBase === 'solar' ? (
                      <img
                        src="/assets/currency/solar-amulet.webp"
                        alt={name('Solar_Amulet', 'Solar Amulet')}
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
                    {t('ui.edit_item')}
                  </button>
                </div>
                <div
                  role="group"
                  aria-label={t('ui.shared_material_favorites')}
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
                      aria-label={t('material.favorite', {
                        index: index + 1,
                        name: resource
                          ? name(resource.id, resource.name)
                          : t('material.empty'),
                      })}
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
                    aria-label={t('ui.previous_crafting_step')}
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
                      ? t('film.step', {
                          step: filmState.history.cursor,
                          total: film.frames.length - 1,
                        })
                      : t('ui.new_craft')}
                  </span>
                  <button
                    type="button"
                    aria-label={t('ui.next_crafting_step')}
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
                    {t('ui.crafting_session')}
                    <select
                      aria-label={t('ui.crafting_session')}
                      value={film?.id ?? ''}
                      disabled={applying !== null}
                      onChange={(event) => void restoreFilm(event.target.value)}
                    >
                      <option value="" disabled>
                        {t('ui.new_craft')}
                      </option>
                      {filmState.history.films.map((entry, index) => (
                        <option key={entry.id} value={entry.id}>
                          {t('film.session', { index: index + 1 })} ·{' '}
                          {t('crafts', { count: entry.frames.length - 1 })}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                {filmState.error && (
                  <p role="alert">{uiText(filmState.error)}</p>
                )}
                {storedFrame && initial.data && !restoredValid && (
                  <p role="alert">{t('notice.saved_catalog')}</p>
                )}
                {card ? (
                  <ItemCard
                    item={card}
                    {...(concrete
                      ? { baseItemId: baseSlugs[catalogBase] }
                      : {})}
                  />
                ) : (
                  <ItemCard
                    baseItemId={baseSlugs[catalogBase]}
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
                                    : catalogBase === 'ring'
                                      ? 'Rings'
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
                    {t('notice.maximum_quality')}
                  </p>
                )}
                {(selected?.id === 'Essence_of_the_Abyss' ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':essence-abyssal-mark'),
                  )) && (
                  <p className="workbench-feedback">{t('notice.mark')}</p>
                )}
                {((selected?.id === 'Essence_of_Horror' &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':essence-socketed-augment-effect'),
                  )) && (
                  <p className="workbench-feedback">
                    {t('notice.augment_effect')}
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
                    {t('notice.glove_recoup')}
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
                    {t('notice.glove_alloy_area')}
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
                    {t('notice.glove_alloy_ward')}
                  </p>
                )}
                {((selected?.id === 'Prismatic_Alloy' &&
                  concrete?.baseItemId ===
                    'Metadata/Items/Armours/Gloves/FourGlovesStr1') ||
                  concrete?.explicits.some((m) =>
                    m.modifierId.endsWith(':alloy-elemental-penetration'),
                  )) && (
                  <p className="workbench-feedback">
                    {t('notice.glove_alloy_penetration')}
                  </p>
                )}
                {!canCraft && <p>{t('notice.pasted_display')}</p>}
                {draft.source === 'text' && !mapping.data?.mapped && (
                  <button
                    type="button"
                    onClick={() => void enableCrafting()}
                    disabled={applying !== null || !initial.data}
                  >
                    {t('ui.enable_solar_crafting')}
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
                    {' '}
                    {t('ui.line')} {issue.lineNumber || 'item'}: {issue.message}
                  </p>
                ))}
                {initial.isPending && <p>{t('ui.loading_crafting_catalog')}</p>}
                {initial.isError && (
                  <p role="alert">
                    {' '}
                    {t('ui.catalog_load_error')}{' '}
                    <button
                      type="button"
                      onClick={() => void initial.refetch()}
                    >
                      {t('ui.retry_catalog')}
                    </button>
                  </p>
                )}
                {applying && <p>{t('ui.updating_item')}</p>}
                {announcement && (
                  <p className="workbench-feedback">{uiText(announcement)}</p>
                )}
                {evidenceInvalid && (
                  <p role="alert">{t('notice.saved_evidence')}</p>
                )}
                {catalogBase === 'ring' && (
                  <p className="workbench-feedback">{t('notice.ring_scope')}</p>
                )}
                {concrete?.catalystQuality && (
                  <p className="workbench-feedback">
                    Existing catalyst quality is retained. Matching reviewed
                    integer stats use a separate quality multiplier and
                    truncation. Original rolls remain in modifier details; other
                    stats stay unscaled. Currency interactions and catalyst
                    application are not verified, so crafting is blocked.{' '}
                    <a
                      href="https://www.poe2wiki.net/wiki/Quality"
                      target="_blank"
                      rel="noreferrer"
                    >
                      {t('ui.quality_source')}
                    </a>
                  </p>
                )}
                {catalogBase === 'helmet' && (
                  <p className="workbench-feedback">
                    {t('notice.helmet_scope')}
                  </p>
                )}
                {catalogBase === 'belt' && (
                  <p className="workbench-feedback">{t('notice.belt_scope')}</p>
                )}
                {catalogBase === 'sceptre' && (
                  <p className="workbench-feedback">
                    {t('notice.sceptre_scope')}
                  </p>
                )}
                {catalogBase === 'body' && (
                  <p className="workbench-feedback">{t('notice.body_scope')}</p>
                )}
                {catalogBase === 'wand' && (
                  <p className="workbench-feedback">{t('notice.wand_scope')}</p>
                )}
                {catalogBase === 'bow' && (
                  <p className="workbench-feedback">{t('notice.bow_scope')}</p>
                )}
                {catalogBase === 'stocky' && (
                  <p className="workbench-feedback">
                    {t('notice.glove_scope')}
                  </p>
                )}
                {craftEvidence && (
                  <details className="workbench-assumptions">
                    <summary>{t('ui.last_craft_and_roll_assumptions')}</summary>
                    <p>
                      {localizedAction(craftEvidence.action)} {t('ui.applied')}{' '}
                      {uiText(craftProbabilityEvidence(craftEvidence).text)}
                    </p>
                    {craftProbabilityEvidence(craftEvidence).weighted && (
                      <p>
                        <a
                          href={modifierWeightSources[catalogBase]}
                          target="_blank"
                          rel="noreferrer"
                        >
                          {t('ui.poe2db_modifier_table')}
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
                        {' '}
                        {t('ui.consumed_omens')}{' '}
                        {craftEvidence.consumedOmens
                          .map((id) => name(id, id.replaceAll('_', ' ')))
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
                          : {a.candidateUnit}, N = {a.n}
                          {t('ui.candidate_uniform')} {a.n}.{' '}
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
                            {t('ui.source')}
                          </a>
                        </p>
                      ))
                    ) : (
                      <p>{t('notice.no_uniform_fallback')}</p>
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
            aria-label={t('ui.starting_item')}
            aria-busy={imported.pending}
          >
            <div className="item-input-content">
              <button
                ref={inputClose}
                type="button"
                className="input-close"
                aria-label={t('ui.close_item_input')}
                onClick={closeInput}
              >
                ×
              </button>
              <div className="input-heading">
                <h3>{t('ui.starting_item')}</h3>
                <span>01</span>
              </div>
              <div
                className="input-tabs"
                role="group"
                aria-label={t('ui.item_input_method')}
              >
                <button
                  type="button"
                  aria-pressed={inputMode === 'base'}
                  onClick={() => {
                    imported.invalidate()
                    setInputMode('base')
                  }}
                >
                  {t('ui.select_base')}
                </button>
                <button
                  type="button"
                  aria-pressed={inputMode === 'text'}
                  onClick={() => setInputMode('text')}
                >
                  {t('ui.item_text')}
                </button>
              </div>
              {inputMode === 'base' ? (
                <div className="base-form">
                  <label htmlFor="base-select">{t('ui.equipment_base')}</label>
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
                          | 'helmet'
                          | 'ring',
                      )
                    }
                  >
                    <option value="solar">
                      {name('Solar_Amulet', 'Solar Amulet')}
                    </option>
                    <option value="stocky">
                      {name('Stocky_Mitts', 'Stocky Mitts')}
                    </option>
                    <option value="bow">
                      {name('Crude_Bow', 'Crude Bow')}
                    </option>
                    <option value="wand">
                      {name('Attuned_Wand', 'Attuned Wand')}
                    </option>
                    <option value="body">
                      {name('Rusted_Cuirass', 'Rusted Cuirass')}
                    </option>
                    <option value="sceptre">
                      {name('Rattling_Sceptre', 'Rattling Sceptre')}
                    </option>
                    <option value="belt">
                      {name('Rawhide_Belt', 'Rawhide Belt')}
                    </option>
                    <option value="helmet">
                      {name('Rusted_Greathelm', 'Rusted Greathelm')}
                    </option>
                    <option value="ring">
                      {name('Iron_Ring', 'Iron Ring')}
                    </option>
                  </select>
                  <label htmlFor="base-level">{t('ui.item_level')}</label>
                  <input
                    id="base-level"
                    type="number"
                    min="1"
                    max="100"
                    step="1"
                    value={baseLevel}
                    onChange={(e) => setBaseLevel(e.target.value)}
                  />
                  {(baseChoice === 'solar' || baseChoice === 'ring') && (
                    <>
                      <label htmlFor="starting-quality-type">
                        {t('ui.existing_catalyst_quality')}
                      </label>
                      <select
                        id="starting-quality-type"
                        value={qualityType}
                        onChange={(e) =>
                          setQualityType(
                            e.target.value as '' | CatalystQuality['type'],
                          )
                        }
                      >
                        <option value="">
                          {t('ui.no_typed_quality_supplied')}
                        </option>
                        {Object.entries(catalystTypes).map(([type, info]) => (
                          <option key={type} value={type}>
                            {name(
                              catalystItemIds[type as CatalystQuality['type']],
                              `${info.name} Catalyst`,
                            )}
                          </option>
                        ))}
                      </select>
                      {qualityType !== '' && (
                        <>
                          <label htmlFor="starting-quality-amount">
                            {t('ui.current_quality')}
                          </label>
                          <input
                            id="starting-quality-amount"
                            type="number"
                            min="0"
                            max="20"
                            step="1"
                            value={qualityAmount}
                            onChange={(e) => setQualityAmount(e.target.value)}
                          />
                          <p>{t('notice.existing_quality')}</p>
                        </>
                      )}
                    </>
                  )}
                  <button
                    type="button"
                    className="primary-action"
                    disabled={
                      !Number.isInteger(Number(baseLevel)) ||
                      Number(baseLevel) < 1 ||
                      Number(baseLevel) > 100 ||
                      ((baseChoice === 'solar' || baseChoice === 'ring') &&
                        qualityType !== '' &&
                        (qualityAmount.trim() === '' ||
                          !Number.isInteger(Number(qualityAmount)) ||
                          Number(qualityAmount) < 0 ||
                          Number(qualityAmount) > 20))
                    }
                    onClick={() => {
                      imported.invalidate()
                      void startBase(
                        Number(baseLevel),
                        baseChoice,
                        (baseChoice === 'solar' || baseChoice === 'ring') &&
                          qualityType !== ''
                          ? { type: qualityType, amount: Number(qualityAmount) }
                          : null,
                      )
                      setPreviewRequest({ count: 0, action: null })
                      setSelected(null)
                      closeInput()
                    }}
                  >
                    {t('ui.place_base')}
                    <span aria-hidden="true">↗</span>
                  </button>
                </div>
              ) : (
                <div className="text-form">
                  <label htmlFor="item-text">
                    {t('ui.item_text_copied_from_the_game')}
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
                    {imported.pending
                      ? t('ui.analyzing_item')
                      : t('ui.analyze_item')}{' '}
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
            {t('ui.starting_item_level')}
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
            {t('ui.start_new_solar_amulet', {
              name: name('Solar_Amulet', 'Solar Amulet'),
            })}
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
          <p>{t('notice.pasted_explorer')}</p>
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
            {t('notice.support_base', {
              name: name(baseSlugs[catalogBase], baseName),
            })}
          </p>
        )}
      </div>
      <span className="sr-only" role="status">
        {imported.pending ? 'Analyzing item…' : announcement}
      </span>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span>
          {t('ui.your_personal_crafting_workbench')}
        </span>
        <a
          href="https://poe2db.tw/us/Currency"
          target="_blank"
          rel="noreferrer"
        >
          {t('ui.currency_images_poe2db')}
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
