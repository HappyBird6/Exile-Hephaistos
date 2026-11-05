import {
  augments,
  augmentName,
  augmentEffect,
  socketText,
  installAugment,
} from './socketAugments'
import { createPortal } from 'react-dom'
import { defenceDisplay } from './defenceDisplay'
import { preciseModifierProjection } from './sanctification/preciseModifierProjection'
import {
  applyBone,
  isBone,
  boneText,
  putrefactionText,
  boneConsumedOmens,
  boneRemainingOmens,
} from './desecrationCurrency'
import {
  applyAbyssCurrency,
  abyssFromConcrete,
} from './abyss-workbench/applyAbyssCurrency'
import { abyssCurrencyReason } from './abyss-workbench/currencyMessages'
import { validAbyssResponse } from './abyssResponse'
import { AbyssPanel } from './AbyssPanel'
import { readSupport, saveSupport } from './method-search/supportStorage'
import {
  baseSlugs,
  baseClass,
  baseStartingRarity,
  sourcePropertyUnscaled,
  clipboardBase,
  supersededBases,
} from './baseRegistry'
import type { WorkbenchBaseKey } from './baseRegistry'
import { basePropertyLines, topBase, topBaseKey } from './topBases'
import { WorkbenchBasePicker } from './base-picker/WorkbenchBasePicker'
import { SessionLibrary } from './session-library/SessionLibrary'
import { SessionLibraryButtons } from './session-library/SessionLibraryButtons'
import { ItemSlotImage } from './item-slot/ItemSlotImage'
import { ServiceMessage } from './ServiceMessage'
import { serviceText } from './serviceMessages'
import { getLocale, useI18n, uiText, translate } from '../../shared/i18n/i18n'
import { LocaleSelector } from '../../shared/i18n/LocaleSelector'
import { localizedAction, catalystItemIds } from './localizedCrafting'
import { localizedModifierText } from './localizedModifiers'
import { inServiceScope } from './serviceScope'
import { catalystTypes, catalystProjection } from './catalystQuality'
import type { CatalystQuality } from './catalystQuality'
import { currencyNames } from './currencyNames'
import { skipToken, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from 'react'
import { ItemCard } from './ItemCard'
import { lastCurrencyAffectedOptions } from './lastCurrencyAffectedOptions'
import { whittlingCandidates } from './omenRemovalCandidates'
import { toItemCard } from './itemCardData'
import {
  FeedbackTooltipProvider,
  FeedbackTooltipOutlet,
} from './feedback-tooltip/FeedbackTooltipProvider'
import { MaterialTooltip } from './MaterialTooltip'
import {
  ChaosRepeatControls,
  chaosRepeatCurrencyIds,
  chaosRepeatMaximumUsesLimit,
  chaosRepeatControlMessages,
} from './chaos-repeat/controls'
import type {
  ChaosRepeatControlErrors,
  ChaosRepeatCurrencyId,
} from './chaos-repeat/controls'
import './chaos-repeat/integration/chaos-repeat-integration.css'
import { chaosRepeatResultMessage } from './chaos-repeat/integration/messages'
import { applyChaosRepeat } from './chaos-repeat/api'
import type { ChaosRepeatRequest } from './chaos-repeat/api'
import { SessionNotation } from './session-notation/SessionNotation'
import { useItemTextImport } from './useItemTextImport'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import { abyssWorkbenchMessages } from './abyss-workbench/messages'
import { excludedWorkbenchBoneIds } from './abyss-workbench/policy'
import { WorkbenchStash } from './stash/WorkbenchStash'
import { visibilityPolicy } from './stash/visibilityPolicy'
import { additionalStashCurrencies } from './materials'
import { WorkbenchFavorites } from './stash/WorkbenchFavorites'
import type { WorkbenchResource } from './stash/stashTypes'
import { materials, materialTabs } from './materials'
import type { MaterialTab } from './materials'
import { useItemDraft } from './draft'
import { CraftSupport } from './CraftSupport'
import { loadInitial } from './craftingApi'
import {
  applyCurrency,
  concreteInitial,
  workbenchCurrencyActions,
  workbenchOmens,
  compatibleOmenPair,
  mapSolarText,
} from './workbenchApi'
import type { AppliedItem, MappingResult } from './workbenchApi'
import {
  LocalFilmRepository,
  emptyFilms,
  currentFilm,
  currentFrame,
  startFilm,
  recordCraft,
  recordChaosRepeat,
  recordAbyss,
  storedAbyssItem,
  viewFrame,
  verifiedHistoryState,
  verifiedFrameEvidence,
  filmRulesetStatus,
  updateFilmMetadata,
  deleteFilm,
  undoFilmDeletion,
} from './workbenchHistory'
import type { Films, FilmDeletion } from './workbenchHistory'
import {
  basicJewelBases,
  isWorkbenchJewel,
  workbenchJewelBases,
} from './basicJewel'
import { sapphireCastSpeed } from './sapphireJewel'
import './crafting.css'
import './compact-viewport/compactViewport.css'

function tooltipEvents(id: string) {
  return { 'data-material-tooltip': id }
}

const workspaceTabs = ['workbench', 'support'] as const

type Selection = { id: string; name: string; image: string }

function filmId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function CraftingPage() {
  const { t, name, locale } = useI18n()
  const [boneCounts, setBoneCounts] = useState([0, 0])
  const client = useQueryClient()
  const draft = useItemDraft()
  const imported = useItemTextImport()
  const importedBase = imported.data
    ? clipboardBase(
        imported.data.displayBase ?? imported.data.displayName,
        imported.data.itemClass,
        imported.data.text.originalText,
      )
    : undefined
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
      ? (importedBase ?? 'solar')
      : (topBaseKey(storedFrame?.state.baseItemId) ??
        (Object.values(workbenchJewelBases).some(
          (id) => id === storedFrame?.state.baseItemId,
        )
          ? (Object.keys(workbenchJewelBases).find(
              (b) =>
                workbenchJewelBases[b as keyof typeof workbenchJewelBases] ===
                storedFrame?.state.baseItemId,
            ) as keyof typeof workbenchJewelBases)
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
                            : draft.base))
  const baseName =
    topBase(catalogBase)?.name ??
    (Object.hasOwn(workbenchJewelBases, catalogBase)
      ? baseSlugs[catalogBase]
      : catalogBase === 'stocky'
        ? 'Stocky Mitts'
        : catalogBase === 'bow' || topBase(catalogBase)?.family === 'bows'
          ? 'Crude Bow'
          : catalogBase === 'wand' || topBase(catalogBase)?.family === 'wands'
            ? 'Attuned Wand'
            : catalogBase === 'body' ||
                catalogBase === 'soldier' ||
                topBase(catalogBase)?.family === 'body'
              ? 'Rusted Cuirass'
              : catalogBase === 'sceptre' ||
                  topBase(catalogBase)?.family === 'sceptres'
                ? 'Rattling Sceptre'
                : catalogBase === 'belt'
                  ? 'Rawhide Belt'
                  : catalogBase === 'ring'
                    ? 'Iron Ring'
                    : catalogBase === 'helmet' || catalogBase === 'imperial'
                      ? 'Rusted Greathelm'
                      : 'Solar Amulet')
  const catalogLevel =
    Number.isInteger(storedLevel) && storedLevel! >= 1 && storedLevel! <= 100
      ? storedLevel!
      : draft.source === 'text'
        ? (imported.data?.itemLevel ?? draft.baseItemLevel)
        : draft.baseItemLevel
  function saveFilms(
    history: Films,
    revision = draft.baseRevision,
    requirePersistence = false,
  ) {
    let error = filmState.error
    if (!filmState.blockedSaving) {
      try {
        new LocalFilmRepository(window.localStorage).save(history)
        error = ''
      } catch {
        error = t('notice.history_save')
      }
    }
    if (error && requirePersistence) {
      setFilmState({ ...filmState, error })
      return false
    }
    setFilmState({ ...filmState, history, revision, error })
    return true
  }
  const [view, setView] = useState<(typeof workspaceTabs)[number]>(
    () =>
      readSupport('workspace', (v): v is (typeof workspaceTabs)[number] =>
        workspaceTabs.includes(v as (typeof workspaceTabs)[number]),
      ) ?? 'workbench',
  )
  useEffect(() => {
    saveSupport('workspace', view)
  }, [view])
  const dialog = useRef<HTMLDialogElement>(null)
  const [activeTab, setActiveTab] = useState<MaterialTab>('Currency')
  const excludedResourceIds: readonly string[] = excludedWorkbenchBoneIds
  const fixedCurrencies = currencies.filter(
    (currency) =>
      inServiceScope(currency.id) &&
      !isBone(currency.id) &&
      currency.id !== 'Artificers_Orb' &&
      !excludedResourceIds.includes(currency.id),
  )
  const stashResources: WorkbenchResource[] = [
    ...currencies.map((currency) => ({
      ...currency,
      name: currencyNames[currency.id],
      category: 'Currency' as const,
    })),
    ...materials,
    ...additionalStashCurrencies,
  ]
  const [selected, setSelected] = useState<Selection | null>(null)
  const [held, setHeld] = useState<WorkbenchResource | null>(null)
  const [repeatEnabled, setRepeatEnabled] = useState(false)
  const [repeatPattern, setRepeatPattern] = useState('')
  const [repeatErrors, setRepeatErrors] = useState<ChaosRepeatControlErrors>({})
  const [repeatHintRequest, setRepeatHintRequest] = useState<{
    materialId: ChaosRepeatCurrencyId
    context: string
  } | null>(null)
  const [abyssBusy, setAbyssBusy] = useState(false)
  const abyssBusyRef = useRef(false)
  const handleAbyssBusy = useCallback((value: boolean) => {
    abyssBusyRef.current = value
    setAbyssBusy(value)
  }, [])
  const [feedbackAnchor, setFeedbackAnchor] = useState<HTMLElement | null>(null)
  function rememberFeedbackAnchor(event: { target: EventTarget | null }) {
    if (mutationLocked()) return
    const anchor =
      event.target instanceof Element
        ? event.target.closest<HTMLElement>('[data-material-tooltip]')
        : null
    if (anchor) setFeedbackAnchor(anchor)
  }
  const [favorites, setFavorites] = useState<(WorkbenchResource | null)[]>(
    Array(10).fill(null),
  )
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null)
  const [leftMenu, setLeftMenu] = useState<
    'base' | 'history' | 'favorites' | null
  >(null)
  const baseAnchor = useRef<HTMLButtonElement>(null)
  const basePlacementRegion = useRef<HTMLDivElement>(null)
  const historyAnchor = useRef<HTMLButtonElement>(null)
  const favoritesAnchor = useRef<HTMLButtonElement>(null)
  const currencyShelf = useRef<HTMLDivElement>(null)
  const [lastDeletion, setLastDeletion] = useState<FilmDeletion | null>(null)
  const [inputOpen, setInputOpen] = useState(false)
  const inputOpener = useRef<HTMLElement | null>(null)
  const [announcement, setAnnouncement] = useState('')
  const [failedApplication, setFailedApplication] = useState<{
    revision: number
    message: string
  } | null>(null)
  const applicationFailure =
    failedApplication?.revision === draft.baseRevision
      ? failedApplication.message
      : ''
  const [repeatNotice, setRepeatNotice] = useState<{
    revision: number
    film: string | null
    cursor: number
    kind: 'match' | 'limit'
    uses: number
  } | null>(null)
  const repeatSuccess =
    repeatNotice?.revision === draft.baseRevision &&
    repeatNotice.film === filmState.history.active &&
    repeatNotice.cursor === filmState.history.cursor
      ? chaosRepeatResultMessage(locale, repeatNotice.kind, repeatNotice.uses)
      : ''
  const [altHeld, setAltHeld] = useState(false)
  const [stashSpace, setStashSpace] = useState(0)
  useEffect(() => {
    const page = document.querySelector('.craft-page')
    if (!page) return
    const measure = () => {
      const spare =
        window.innerHeight -
        page.getBoundingClientRect().bottom -
        window.scrollY +
        stashSpace
      setStashSpace(Math.max(0, Math.min(24, Math.floor(spare))))
    }
    const observer =
      typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(measure)
    observer?.observe(page)
    window.addEventListener('resize', measure)
    measure()
    return () => {
      observer?.disconnect()
      window.removeEventListener('resize', measure)
    }
  }, [stashSpace, view])
  const [pendingApplication, setApplying] = useState<{
    controller: AbortController
    revision: number
    repeat?: boolean
  } | null>(null)
  const applying =
    pendingApplication?.revision === draft.baseRevision
      ? pendingApplication.controller
      : null
  const repeatRunning = applying !== null && pendingApplication?.repeat === true
  const applyingRequest = useRef<AbortController | null>(null)
  const placementRequest = useRef(0)
  const placementPending = useRef(false)
  const inputRequestGeneration = useRef(0)
  const inputFilm = useRef(filmState)
  useLayoutEffect(() => {
    inputFilm.current = filmState
  }, [filmState])
  useEffect(
    () => () => {
      inputRequestGeneration.current++
    },
    [],
  )
  const workbenchBusy = applying !== null || abyssBusy || imported.pending
  const inputBusy = applying !== null || abyssBusy
  const inputMutationLocked = useCallback(
    () =>
      Boolean(
        applyingRequest.current ||
        placementPending.current ||
        abyssBusyRef.current,
      ),
    [],
  )
  const mutationLocked = useCallback(
    () =>
      Boolean(
        applyingRequest.current ||
        placementPending.current ||
        abyssBusyRef.current ||
        imported.pending,
      ),
    [imported.pending],
  )
  const canMutateAbyss = useCallback(
    () =>
      !applyingRequest.current &&
      !placementPending.current &&
      !imported.pending,
    [imported.pending],
  )
  const initialKey = [
    'crafting',
    'initial',
    catalogBase,
    catalogLevel,
    'manual',
  ] as const
  const initial = useQuery({
    queryKey: initialKey,
    queryFn: ({ signal }) =>
      loadInitial(catalogLevel, signal, catalogBase, true),
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
  const activeFilm =
    filmState.revision === draft.baseRevision
      ? currentFilm(filmState.history)
      : undefined
  const rulesetStatus =
    activeFilm && initial.data
      ? filmRulesetStatus(activeFilm, initial.data.rulesetIdentity)
      : undefined
  const restoredValid =
    storedFrame && initial.data
      ? rulesetStatus === 'current' &&
        verifiedHistoryState(storedFrame.state, initial.data) &&
        (!storedFrame.abyss ||
          (validAbyssResponse(
            'validate',
            storedFrame.abyss,
            storedFrame.state,
            initial.data,
          ) &&
            JSON.stringify(storedAbyssItem(storedFrame.abyss)) ===
              JSON.stringify(storedFrame.state)))
      : false
  const restoredState =
    restoredValid && initial.data && storedFrame ? storedFrame.state : undefined
  const canCraft =
    (draft.source === 'base' || mapping.data?.mapped === true) &&
    (!storedFrame || restoredValid)
  const film =
    filmState.revision === draft.baseRevision
      ? currentFilm(filmState.history)
      : undefined
  const repeatContext = JSON.stringify([
    draft.baseRevision,
    draft.source,
    filmState.revision,
    filmState.history,
    restoredState ?? workbench.data?.state ?? mapping.data?.state,
    draft.activeOmens,
    locale,
    initial.data?.rulesetIdentity,
  ])
  const latestRepeatContext = useRef(repeatContext)
  const appliedRepeatRequests = useRef(new Set<string>())
  useLayoutEffect(() => {
    latestRepeatContext.current = repeatContext
  }, [repeatContext])
  async function startBase(
    level: number,
    base: WorkbenchBaseKey = 'solar',
    catalystQuality: CatalystQuality | null = null,
    sapphireRarity: 'NORMAL' | 'MAGIC' | 'RARE' = 'MAGIC',
    castSpeed: number | null = null,
  ) {
    if (mutationLocked()) return
    const request = ++placementRequest.current
    const revision = useItemDraft.getState().baseRevision
    placementPending.current = true
    applyingRequest.current?.abort()
    applyingRequest.current = null
    setApplying(null)
    try {
      const data = await client.fetchQuery({
        queryKey: ['crafting', 'initial', base, level, 'manual'],
        queryFn: ({ signal }) => loadInitial(level, signal, base, true),
        staleTime: 60_000,
      })
      if (
        request !== placementRequest.current ||
        revision !== useItemDraft.getState().baseRevision
      )
        return
      const root = {
        ...concreteInitial(data),
        ...(Object.hasOwn(workbenchJewelBases, base)
          ? {
              rarity: sapphireRarity,
              explicits:
                castSpeed === null || base !== 'sapphire'
                  ? []
                  : [
                      {
                        modifierId: sapphireCastSpeed,
                        values: { display_cast_speed_percent: castSpeed },
                      },
                    ],
            }
          : {}),
        ...(catalystQuality ? { catalystQuality } : {}),
      }
      if (!verifiedHistoryState(root, data))
        throw new Error('Unsupported starting quality')
      draft.setBase(level, base)
      saveFilms(
        startFilm(filmState.history, root, filmId(), data.rulesetIdentity),
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
  function restoreFilm(
    id: string,
    sourceHistory = filmState.history,
    cursor?: number,
  ) {
    if (mutationLocked()) return
    const selectedFilm = sourceHistory.films.find((entry) => entry.id === id)
    if (!selectedFilm) return
    const nextCursor = cursor ?? selectedFilm.frames.length - 1
    const state = selectedFilm.frames[nextCursor]!.state
    const base =
      topBaseKey(state.baseItemId) ??
      (isWorkbenchJewel(state.baseItemId)
        ? (Object.keys(workbenchJewelBases).find(
            (b) =>
              workbenchJewelBases[b as keyof typeof workbenchJewelBases] ===
              state.baseItemId,
          ) as keyof typeof workbenchJewelBases)
        : state.baseItemId === 'Metadata/Items/Armours/Gloves/FourGlovesStr1'
          ? 'stocky'
          : state.baseItemId === 'Metadata/Items/Rings/FourRing1'
            ? 'ring'
            : state.baseItemId ===
                'Metadata/Items/Armours/Helmets/FourHelmetStr1'
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
                        : 'solar')
    // Reading a historical record must not depend on availability of its catalog or engine.
    draft.setBase(state.itemLevel, base)
    saveFilms(
      {
        ...sourceHistory,
        active: selectedFilm.id,
        cursor: nextCursor,
      },
      useItemDraft.getState().baseRevision,
    )
    setSelected(null)
    setHeld(null)
    setAnnouncement('')
  }
  function commitLibraryHistory(next: Films) {
    if (mutationLocked() || filmState.blockedSaving) return false
    if (!saveFilms(next, draft.baseRevision, true)) return false
    const activeChanged =
      next.active !== filmState.history.active ||
      next.cursor !== filmState.history.cursor
    if (activeChanged && next.active)
      restoreFilm(next.active, next, next.cursor)
    else if (activeChanged) {
      draft.setBase(catalogLevel, catalogBase)
      setFilmState({
        ...filmState,
        history: next,
        revision: useItemDraft.getState().baseRevision,
        error: '',
      })
      setSelected(null)
      setHeld(null)
      setPointer(null)
    }
    return true
  }
  function deleteSession(id: string) {
    const result = deleteFilm(filmState.history, id)
    if (result && commitLibraryHistory(result.history))
      setLastDeletion(result.deletion)
  }
  function undoSessionDeletion() {
    if (
      lastDeletion &&
      commitLibraryHistory(undoFilmDeletion(filmState.history, lastDeletion))
    )
      setLastDeletion(null)
  }
  function browse(cursor: number) {
    if (mutationLocked()) return
    saveFilms(viewFrame(filmState.history, cursor))
    setFailedApplication(null)
    setRepeatNotice(null)
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
  const [sapphireRarity, setSapphireRarity] = useState<
    'NORMAL' | 'MAGIC' | 'RARE'
  >('MAGIC')
  const [sapphireAffix, setSapphireAffix] = useState(false)
  const [sapphireRoll, setSapphireRoll] = useState('2')
  const [baseChoice, setBaseChoice] = useState<WorkbenchBaseKey>('solar')
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
      if (
        inputOpen && event.key === 'Escape'
          ? inputMutationLocked()
          : mutationLocked()
      )
        return
      if (event.key === 'Alt') setAltHeld(true)
      if (event.key === 'Escape') {
        setFailedApplication(null)
        setRepeatNotice(null)
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
  }, [inputOpen, mutationLocked, inputMutationLocked])

  function choose(resource: Selection) {
    if (mutationLocked()) return
    if (
      excludedResourceIds.includes(resource.id) ||
      !visibilityPolicy(resource)
    )
      return
    setFailedApplication(null)
    setRepeatNotice(null)
    setSelected(resource)
    setHeld(null)
    setAnnouncement(
      t('material.selected', { name: name(resource.id, resource.name) }),
    )
  }
  function toggleOmen(id: string) {
    if (mutationLocked()) return
    if (
      !draft.activeOmens.includes(id) &&
      !visibilityPolicy({ id, category: 'Omen' })
    )
      return
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
      const oppositeSides =
        (id.includes('Sinistral') && conflict.id.includes('Dextral')) ||
        (id.includes('Dextral') && conflict.id.includes('Sinistral'))
      setAnnouncement(
        `${oppositeSides ? t('omen.opposite_sides') + ' ' : ''}${t('omen.conflict', { name: name(conflict.id, conflict.id.replaceAll('_', ' ')) })}`,
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
  async function apply(repeat = false, socketSlot?: number) {
    if (mutationLocked()) return
    setFailedApplication(null)
    setRepeatNotice(null)
    const failApply = (message: string) => {
      setFailedApplication({ revision: draft.baseRevision, message })
      setAnnouncement(serviceText(message))
    }
    if (!selected) {
      failApply(t('notice.select_currency'))
      return
    }
    if (!canCraft) {
      if (rulesetStatus && rulesetStatus !== 'current') {
        failApply(t(`ruleset.${rulesetStatus}`))
        return
      }
      failApply(
        'The item has not changed. Pasted items are display-only. Select the Solar Amulet base to explore probabilities.',
      )
      return
    }
    if (workbenchOmens.some((omen) => omen.id === selected.id)) {
      failApply(t('notice.omen_favorite'))
      return
    }
    const isAugment = augments.some((a) => a.id === selected.id)
    const action = workbenchCurrencyActions[selected.id]
    if (!action && !isBone(selected.id) && !isAugment) {
      failApply(t('notice.no_rule'))
      return
    }
    if (!initial.data) {
      failApply(t('notice.wait_catalog'))
      return
    }
    const repeatingChaos =
      repeatEnabled &&
      (action === 'CHAOS' ||
        action === 'GREATER_CHAOS' ||
        action === 'PERFECT_CHAOS')
    const controller = new AbortController()
    applyingRequest.current = controller
    setApplying({
      controller,
      revision: draft.baseRevision,
      repeat: repeatingChaos,
    })
    const revision = draft.baseRevision
    const state =
      restoredState ??
      workbench.data?.state ??
      mapping.data?.state ??
      concreteInitial(initial.data)
    const repeatContextCurrent = () =>
      !controller.signal.aborted &&
      applyingRequest.current === controller &&
      useItemDraft.getState().baseRevision === revision &&
      useItemDraft.getState().source === draft.source &&
      JSON.stringify(useItemDraft.getState().activeOmens) ===
        JSON.stringify(draft.activeOmens) &&
      getLocale() === locale &&
      client.getQueryData(initialKey) === initial.data &&
      latestRepeatContext.current === repeatContext &&
      (restoredState !== undefined ||
        JSON.stringify(
          client.getQueryData<AppliedItem>(workbenchKey)?.state ??
            client.getQueryData<MappingResult>(mappingKey)?.state ??
            concreteInitial(initial.data!),
        ) === JSON.stringify(state))
    try {
      if (repeatingChaos) {
        setRepeatErrors({})
        const abyssRepeat = Boolean(
          state.desecrated?.length || storedFrame?.abyss,
        )
        const dormant = abyssRepeat ? boneConsumedOmens(draft.activeOmens) : []
        const request: ChaosRepeatRequest = {
          requestId: crypto.randomUUID(),
          state: abyssRepeat
            ? {
                kind: 'ABYSS',
                abyss:
                  storedFrame?.abyss ?? abyssFromConcrete(state, initial.data),
              }
            : { kind: 'CONCRETE', item: state },
          action,
          activeOmens: abyssRepeat
            ? boneRemainingOmens(draft.activeOmens)
            : [...draft.activeOmens],
          pattern: repeatPattern,
          locale,
          maximumUses: chaosRepeatMaximumUsesLimit,
        }
        const history =
          filmState.revision === revision
            ? filmState.history
            : { ...filmState.history, active: null, cursor: 0 }
        const result = await applyChaosRepeat(
          request,
          initial.data,
          controller.signal,
        )
        if (
          !repeatContextCurrent() ||
          result.requestId !== request.requestId ||
          appliedRepeatRequests.current.has(request.requestId)
        )
          return
        if (result.kind === 'completed' || result.kind === 'partial') {
          let next: Films
          try {
            next = recordChaosRepeat(
              history,
              result.receipt,
              filmId(),
              initial.data,
            )
            if (next === history) throw new Error('Unverified receipt')
          } catch {
            const message = chaosRepeatResultMessage(locale, 'invalid')
            setRepeatErrors({ request: message })
            failApply(message)
            return
          }
          appliedRepeatRequests.current.add(request.requestId)
          client.setQueryData(['crafting', 'chaos-repeat', revision], result)
          saveFilms(next, revision)
          draft.setActiveOmens([...result.remainingOmens, ...dormant])
          const message = chaosRepeatResultMessage(
            locale,
            result.kind === 'partial'
              ? 'partial'
              : result.termination === 'MATCH'
                ? 'match'
                : 'limit',
            result.actualUses,
          )
          if (result.kind === 'partial') {
            setRepeatErrors({ request: message })
            failApply(message)
          } else {
            setRepeatNotice({
              revision,
              film: next.active,
              cursor: next.cursor,
              kind: result.termination === 'MATCH' ? 'match' : 'limit',
              uses: result.actualUses,
            })
            setAnnouncement(message)
          }
          if (!repeat) {
            setSelected(null)
            setHeld(null)
            setPointer(null)
          }
        } else {
          const kind =
            result.kind === 'no-use'
              ? 'noUse'
              : result.kind === 'rejected'
                ? 'rejected'
                : result.kind === 'uncertain'
                  ? 'uncertain'
                  : 'invalid'
          const message = chaosRepeatResultMessage(locale, kind)
          setRepeatErrors({ request: message })
          failApply(message)
        }
        return
      }
      if (isBone(selected.id)) {
        const next = await applyBone(
          state,
          selected.id,
          '',
          '',
          draft.activeOmens,
          initial.data,
          controller.signal,
          boneText[locale][6],
          draft.activeOmens.includes('Omen_of_Putrefaction')
            ? [
                ...Array(boneCounts[0]).fill('PREFIX'),
                ...Array(boneCounts[1]).fill('SUFFIX'),
              ]
            : undefined,
        )
        if (
          controller.signal.aborted ||
          useItemDraft.getState().baseRevision !== revision
        )
          return
        const history =
          filmState.revision === revision
            ? filmState.history
            : { ...filmState.history, active: null, cursor: 0 }
        saveFilms(
          recordAbyss(
            history,
            state,
            next,
            selected.id,
            filmId(),
            initial.data.rulesetIdentity,
            boneConsumedOmens(draft.activeOmens),
          ),
          revision,
        )
        draft.setActiveOmens(boneRemainingOmens(draft.activeOmens))
        setAnnouncement(
          [
            t('craft.applied', { name: name(selected.id, selected.name) }),
            boneConsumedOmens(draft.activeOmens).length
              ? t('craft.consumed', {
                  names: boneConsumedOmens(draft.activeOmens)
                    .map((id) => name(id, id.replaceAll('_', ' ')))
                    .join(', '),
                })
              : '',
          ]
            .filter(Boolean)
            .join(' '),
        )
        setBoneCounts([0, 0])
        if (!repeat) {
          setSelected(null)
          setHeld(null)
          setPointer(null)
        }
        return
      }
      if (isAugment) {
        const slot =
          socketSlot ??
          Array.from({ length: state.augmentSockets ?? 0 }, (_, i) => i).find(
            (i) =>
              !(state.installedAugments ?? []).some((a) => a.slotIndex === i),
          )
        if (slot === undefined) throw new Error(socketText().unsupported)
        const result = await installAugment(
          state,
          slot,
          selected.id,
          initial.data.rulesetIdentity,
          controller.signal,
        )
        if (!repeatContextCurrent()) return
        result.remainingOmens = [...draft.activeOmens]
        client.setQueryData(workbenchKey, result)
        const history =
          filmState.revision === revision
            ? filmState.history
            : { ...filmState.history, active: null, cursor: 0 }
        saveFilms(recordCraft(history, state, result, filmId()), revision)
        setAnnouncement(augmentName(selected.id))
        setSelected(null)
        setHeld(null)
        setPointer(null)
        requestAnimationFrame(() => {
          if (
            useItemDraft.getState().baseRevision === revision &&
            document.activeElement === document.body
          )
            document.querySelector<HTMLButtonElement>('.item-slot')?.focus()
        })
        return
      }
      if (!action) throw new Error(t('notice.no_rule'))
      if (state.desecrated?.length || storedFrame?.abyss) {
        const source =
          storedFrame?.abyss ?? abyssFromConcrete(state, initial.data)
        const result = await applyAbyssCurrency(
          source,
          action,
          draft.activeOmens,
          initial.data,
          controller.signal,
          t('notice.apply_error'),
        )
        if (
          controller.signal.aborted ||
          useItemDraft.getState().baseRevision !== revision ||
          useItemDraft.getState().source !== draft.source
        )
          return
        if (!result.applied) {
          failApply(abyssCurrencyReason(result.reason, locale))
          return
        }
        const history =
          filmState.revision === revision
            ? filmState.history
            : { ...filmState.history, active: null, cursor: 0 }
        saveFilms(
          recordAbyss(
            history,
            state,
            result.state,
            action,
            filmId(),
            initial.data.rulesetIdentity,
            result.consumedOmens,
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
          ]
            .filter(Boolean)
            .join(' '),
        )
        if (!repeat) {
          setSelected(null)
          setHeld(null)
          setPointer(null)
        }
        return
      }
      const result = await applyCurrency(
        state,
        action,
        initial.data.modifiers,
        controller.signal,
        draft.activeOmens,
        initial.data.rulesetIdentity,
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
        saveFilms(recordCraft(history, state, result, filmId()), revision)
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
        failApply(
          translate('craft.blocked', { reason: result.reason ?? '' }, 'en'),
        )
    } catch (error) {
      if (
        !controller.signal.aborted &&
        (!repeatingChaos || repeatContextCurrent())
      )
        failApply(
          repeatingChaos
            ? chaosRepeatResultMessage(locale, 'uncertain')
            : error instanceof Error
              ? error.message
              : t('notice.apply_error'),
        )
    } finally {
      if (applyingRequest.current === controller) {
        applyingRequest.current = null
        setApplying(null)
      }
    }
  }
  async function importText() {
    if (mutationLocked()) return
    const generation = ++inputRequestGeneration.current
    const original = useItemDraft.getState()
    const originalLocale = getLocale()
    const history = filmState.history
    const filmRevision = filmState.revision
    const accepted = await imported.submit(original.text)
    const current = useItemDraft.getState()
    const sameItemContext = accepted
      ? current.baseRevision === original.baseRevision + 1 &&
        current.source === 'text' &&
        current.currentText.text === original.text
      : current.baseRevision === original.baseRevision &&
        current.source === original.source &&
        current.currentText === original.currentText
    // The hook rejects stale server data; the Page must also discard its own
    // cancelled continuation before it can clear a newer currency selection.
    if (
      generation !== inputRequestGeneration.current ||
      inputMutationLocked() ||
      !sameItemContext ||
      current.text !== original.text ||
      getLocale() !== originalLocale ||
      inputFilm.current.history !== history ||
      inputFilm.current.revision !== filmRevision
    )
      return
    if (accepted) {
      saveFilms({ ...history, active: null, cursor: 0 }, current.baseRevision)
      closeInput()
    }
    setSelected(null)
    setHeld(null)
  }
  async function enableCrafting() {
    if (mutationLocked() || !imported.data || !importedBase || !initial.data)
      return
    const controller = new AbortController()
    const revision = draft.baseRevision
    applyingRequest.current = controller
    setApplying({ controller, revision: draft.baseRevision })
    try {
      const result = await mapSolarText(
        imported.data.text.originalText,
        controller.signal,
        initial.data.modifiers,
        initial.data.rulesetIdentity,
      )
      if (
        controller.signal.aborted ||
        useItemDraft.getState().baseRevision !== revision
      )
        return
      if (
        result.mapped &&
        (result.state?.snapshotId !== initial.data.metadata.snapshotId ||
          result.state?.baseItemId !== initial.data.state.baseItemId ||
          !result.state ||
          !verifiedHistoryState(result.state, initial.data))
      )
        throw new Error(
          'The item uses another catalog snapshot or base. Please reload the catalog.',
        )
      client.setQueryData(mappingKey, result)
      if (result.mapped && result.state)
        saveFilms(
          startFilm(
            filmState.history,
            result.state,
            filmId(),
            initial.data.rulesetIdentity,
            imported.data.text.originalText,
          ),
          revision,
        )
      setAnnouncement(
        result.mapped
          ? t(
              result.state?.observations?.augmentEffects.length
                ? 'notice.resolve_augments'
                : 'notice.import_ready',
            )
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
  function invalidateInput() {
    inputRequestGeneration.current++
    imported.invalidate()
  }
  function closeInput() {
    if (inputMutationLocked()) return
    invalidateInput()
    setInputOpen(false)
    ;(inputOpener.current ?? inputToggle.current)?.focus()
  }
  function placeFavorite(
    index: number,
    resourceToPlace: WorkbenchResource | null = held,
  ) {
    if (mutationLocked()) return
    if (
      resourceToPlace &&
      (excludedResourceIds.includes(resourceToPlace.id) ||
        !visibilityPolicy(resourceToPlace))
    )
      return
    if (!resourceToPlace) {
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
        position === index ? resourceToPlace : resource,
      ),
    )
    setAnnouncement(
      t('material.registered', {
        name: name(resourceToPlace.id, resourceToPlace.name),
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
  const cursor = held ?? selected
  const concrete = storedFrame
    ? restoredState
    : (workbench.data?.state ?? mapping.data?.state)
  const craftEvidence =
    storedFrame && initial.data
      ? verifiedFrameEvidence(storedFrame, initial.data)
      : workbench.data &&
          JSON.stringify(concrete) === JSON.stringify(workbench.data.state)
        ? workbench.data
        : undefined
  const evidenceInvalid =
    storedFrame?.evidence !== undefined && restoredValid && !craftEvidence
  const slotBaseItemId =
    concrete?.baseItemId ??
    (draft.source === 'base' || importedBase
      ? initial.data?.state.baseItemId
      : undefined)
  const removalCandidates =
    concrete && initial.data
      ? whittlingCandidates(concrete, initial.data.modifiers, draft.activeOmens)
      : []
  const lastAffected =
    restoredValid && initial.data
      ? lastCurrencyAffectedOptions(
          film,
          filmState.history.cursor,
          initial.data,
        )
      : new Set<string>()
  const defences =
    concrete && initial.data && !concrete.installedAugments?.length
      ? defenceDisplay(concrete, initial.data.modifiers, locale)
      : { properties: [], replacedSourceIndexes: new Set<number>() }
  const card =
    item && !concrete
      ? toItemCard(item)
      : concrete && initial.data
        ? {
            rarity: concrete.rarity,
            name: item?.displayName ?? baseName,
            base: baseName,
            itemClass: baseClass(catalogBase),
            itemLevel: concrete.itemLevel,
            properties: [
              ...basePropertyLines(catalogBase, locale)
                .filter(
                  (text, index) =>
                    !defences.replacedSourceIndexes.has(index) &&
                    !topBase(catalogBase)?.skillLines?.[locale].includes(text),
                )
                .map((text, i) => ({
                  id: 'source-property-' + i,
                  text: sourcePropertyUnscaled(catalogBase)
                    ? t('base.weapon_property', { property: text })
                    : text,
                })),
              ...defences.properties,
              ...(concrete.augmentSockets != null
                ? [{ id: 'socket-policy', text: socketText().policy }]
                : []),
              ...(concrete.installedAugments?.length
                ? [
                    { id: 'socket-effect-note', text: socketText().effects },
                    ...concrete.installedAugments.map((a) => ({
                      id: 'socket-effect-' + a.slotIndex,
                      text:
                        augmentName(a.augmentId) +
                        ': ' +
                        augmentEffect(a.augmentId, concrete.baseItemId),
                    })),
                  ]
                : []),
              ...(catalogBase === 'belt'
                ? [
                    {
                      id: 'belt-implicit',
                      text: t('base.flask'),
                    },
                    {
                      id: 'belt-charm',
                      text: t('base.charms'),
                    },
                  ]
                : []),
              ...(catalogBase === 'sceptre'
                ? [
                    {
                      id: 'base-spirit',
                      text: t('base.spirit'),
                    },
                    {
                      id: 'innate-skill',
                      text: t('base.skeletal_warrior'),
                    },
                  ]
                : []),
              ...(catalogBase === 'wand'
                ? [
                    {
                      id: 'innate-skill',
                      text: t('base.mana_drain'),
                    },
                  ]
                : []),
              ...(concrete.equipmentQuality == null
                ? []
                : [
                    {
                      id: 'equipment-quality',
                      text: t('quality.equipment', {
                        value: concrete.equipmentQuality,
                      }),
                    },
                  ]),
              ...(concrete.catalystQuality
                ? [
                    {
                      id: 'catalyst-quality',
                      text: t('quality.current', {
                        type:
                          locale === 'en'
                            ? catalystTypes[concrete.catalystQuality.type].name
                            : name(
                                (Object.hasOwn(basicJewelBases, catalogBase)
                                  ? 'Refined_'
                                  : '') +
                                  catalystItemIds[
                                    concrete.catalystQuality.type
                                  ],
                                catalystTypes[concrete.catalystQuality.type]
                                  .name,
                              ),
                        value: concrete.catalystQuality.amount,
                      }),
                    },
                  ]
                : []),
              ...(catalogBase === 'stocky'
                ? [
                    {
                      id: 'augment-sockets',
                      text:
                        concrete.augmentSockets == null
                          ? t('base.sockets_unknown')
                          : t('base.sockets', {
                              value: concrete.augmentSockets,
                            }),
                    },
                  ]
                : []),
            ],
            requirements: topBase(catalogBase)
              ? [
                  {
                    id: 'base-requirements',
                    text: topBase(catalogBase)!.requirements[locale],
                  },
                ]
              : [],
            flags: concrete.conditions.includes('SANCTIFIED')
              ? [{ id: 'sanctified', text: 'SANCTIFIED' }]
              : [],
            modifiers: [...concrete.implicits, ...concrete.explicits]
              .map((m, i) => {
                const d = initial.data!.modifiers[m.modifierId]!
                const projection = catalystProjection(
                  d,
                  m.values,
                  concrete.catalystQuality,
                  concrete,
                )
                const displayedValues = concrete.conditions.includes(
                  'SANCTIFIED',
                )
                  ? preciseModifierProjection(
                      concrete,
                      d,
                      m,
                      i < concrete.implicits.length ? 'implicits' : 'explicits',
                    ).values
                  : projection.values
                const displayText = localizedModifierText(d, displayedValues)
                return {
                  id: m.modifierId,
                  lastCurrencyAffected: lastAffected.has(m.modifierId),
                  removalCandidate:
                    i >= concrete.implicits.length &&
                    removalCandidates.includes(m.modifierId),
                  text: altHeld ? localizedModifierText(d) : displayText,
                  fractured: 'fractured' in m && Boolean(m.fractured),
                  detail:
                    concrete.catalystQuality ||
                    projection.status === 'JEWEL_EFFECT_AND_QUALITY_PROVISIONAL'
                      ? `${t('quality.original_roll', { roll: localizedModifierText(d, m.values) })}\n${t('quality.display_model', { status: projection.status })}`
                      : undefined,
                  kind:
                    i < concrete.implicits.length
                      ? ('implicit' as const)
                      : concrete.desecrated?.some(
                            (slot) => slot.modifierId === m.modifierId,
                          )
                        ? ('desecrated' as const)
                        : d.tags?.includes('crafted')
                          ? ('crafted' as const)
                          : ('explicit' as const),
                  affixLabel:
                    d.affixType === 'NONE'
                      ? undefined
                      : `${d.affixType === 'PREFIX' ? 'P' : 'S'}${Object.hasOwn(workbenchJewelBases, catalogBase) || d.tier === 0 ? '' : d.tier}`,
                }
              })
              .concat(
                (topBase(catalogBase)?.skillLines?.[locale] ?? []).map(
                  (text, i) => ({
                    id: 'source-skill-' + i,
                    lastCurrencyAffected: false,
                    removalCandidate: false,
                    text,
                    fractured: false,
                    detail: undefined,
                    kind: 'implicit' as const,
                    affixLabel: undefined,
                  }),
                ),
              )
              .sort((a, b) => {
                const rank = (line: typeof a) =>
                  !altHeld && line.fractured
                    ? -1
                    : line.kind === 'implicit'
                      ? 0
                      : line.affixLabel?.startsWith('P')
                        ? 1
                        : 2
                return rank(a) - rank(b)
              }),
          }
        : null

  const feedbackContextKey = JSON.stringify([
    view,
    locale,
    draft.baseRevision,
    filmState.history.active,
    filmState.history.cursor,
    activeTab,
    draft.activeOmens,
    inputOpen,
  ])
  const repeatHint =
    repeatHintRequest?.context === feedbackContextKey &&
    repeatHintRequest.materialId === cursor?.id
      ? {
          materialId: repeatHintRequest.materialId,
          text: chaosRepeatControlMessages[locale].hint,
        }
      : null
  const page = (
    <main
      className={`craft-page${view === 'workbench' ? ' craft-page--workbench' : ''}`}
      onClickCapture={rememberFeedbackAnchor}
      onContextMenuCapture={rememberFeedbackAnchor}
      onDragStartCapture={rememberFeedbackAnchor}
      onClick={(event) => {
        if (mutationLocked()) return
        const target = event.target as HTMLElement
        if (
          !target.closest(
            'button, input, label, dialog, a, .workbench-feedback-region, .chaos-repeat-controls',
          )
        ) {
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
                disabled={workbenchBusy}
                onClick={() => {
                  if (mutationLocked()) return
                  setView(tab)
                  setHeld(null)
                  setSelected(null)
                  setPointer(null)
                }}
                onKeyDown={(event) => {
                  if (mutationLocked()) return
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
                        ? 'support'
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
                {t(`workspace.${tab}`)}
              </button>
            ))}
          </div>
          <a className="workspace-admin" href="/admin">
            {t('ui.admin')}
          </a>
        </nav>
        <fieldset
          className="chaos-repeat-language-lock"
          disabled={workbenchBusy}
          onClickCapture={(event) => {
            if (mutationLocked()) {
              event.preventDefault()
              event.stopPropagation()
            }
          }}
          onKeyDownCapture={(event) => {
            if (mutationLocked()) {
              event.preventDefault()
              event.stopPropagation()
            }
          }}
        >
          <LocaleSelector />
        </fieldset>
      </header>

      <div className="craft-title">
        <div>
          <p className="craft-kicker">{t('ui.the_crafting_bench')}</p>
          <h1>
            {view === 'workbench'
              ? t('ui.crafting_workbench')
              : t(`workspace.${view}`)}
          </h1>
        </div>
      </div>
      <div
        className="workbench-layout"
        id="panel-workbench"
        role="tabpanel"
        aria-labelledby="workspace-workbench"
        hidden={view !== 'workbench'}
        onContextMenu={(event) => event.preventDefault()}
      >
        <div className="stash-panel" style={{ marginBottom: stashSpace }}>
          <div className="stash-board">
            <div className="stash-viewport">
              <div className="stash-canvas" aria-label={t('ui.currency_stash')}>
                <div className="stash-canvas__content">
                  <div className="stash-canvas__upper" ref={currencyShelf}>
                    <div className="currency-catalog">
                      {fixedCurrencies.map((currency) => {
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
                            disabled={workbenchBusy}
                            onContextMenu={(event) => {
                              event.preventDefault()
                              if (mutationLocked()) return
                              setPointer({ x: event.clientX, y: event.clientY })
                              choose({
                                ...currency,
                                name: currencyNames[currency.id],
                              })
                            }}
                            onClick={(event) => {
                              if (mutationLocked()) return
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
                      })}
                    </div>
                    <div className="item-placement">
                      <button
                        className={`item-slot ${selected ? 'is-ready' : ''}`}
                        type="button"
                        aria-label={t(
                          'ui.use_selected_currency_on_the_central_item',
                        )}
                        disabled={workbenchBusy}
                        onClick={(event) => void apply(event.shiftKey)}
                      >
                        <ItemSlotImage
                          baseItemId={slotBaseItemId ?? ''}
                          label={
                            slotBaseItemId
                              ? name(baseSlugs[catalogBase], baseName)
                              : displayedName
                          }
                        />
                        <span>{displayedName}</span>
                      </button>
                      {concrete?.augmentSockets != null &&
                        concrete.augmentSockets > 0 && (
                          <div
                            className="wb-sockets"
                            aria-label={socketText().tab}
                          >
                            {Array.from(
                              { length: concrete.augmentSockets },
                              (_, slot) => {
                                const installed =
                                  concrete.installedAugments?.find(
                                    (a) => a.slotIndex === slot,
                                  )
                                return (
                                  <button
                                    key={slot}
                                    type="button"
                                    className={
                                      'wb-socket ' +
                                      (installed ? 'is-filled' : '')
                                    }
                                    disabled={
                                      workbenchBusy ||
                                      !selected ||
                                      !augments.some(
                                        (a) => a.id === selected.id,
                                      )
                                    }
                                    title={
                                      socketText().policy +
                                      ' ' +
                                      (installed
                                        ? augmentName(installed.augmentId) +
                                          ': ' +
                                          augmentEffect(
                                            installed.augmentId,
                                            concrete.baseItemId,
                                          )
                                        : socketText().empty)
                                    }
                                    aria-label={
                                      socketText().socket +
                                      ' ' +
                                      (slot + 1) +
                                      ': ' +
                                      (installed
                                        ? augmentName(installed.augmentId)
                                        : socketText().empty)
                                    }
                                    onClick={() => void apply(false, slot)}
                                  >
                                    {installed ? (
                                      <img
                                        src={
                                          augments.find(
                                            (a) => a.id === installed.augmentId,
                                          )?.image ??
                                          '/assets/augments/rune.svg'
                                        }
                                        alt={augmentName(installed.augmentId)}
                                      />
                                    ) : (
                                      <span aria-hidden="true">○</span>
                                    )}
                                  </button>
                                )
                              },
                            )}
                          </div>
                        )}
                    </div>
                  </div>
                  <div className="chaos-repeat-integration">
                    <ChaosRepeatControls
                      enabled={repeatEnabled}
                      pattern={repeatPattern}
                      running={repeatRunning}
                      disabled={workbenchBusy}
                      errors={repeatErrors}
                      currencyId={
                        chaosRepeatCurrencyIds.includes(
                          cursor?.id as ChaosRepeatCurrencyId,
                        )
                          ? (cursor!.id as ChaosRepeatCurrencyId)
                          : 'Chaos_Orb'
                      }
                      onEnabledChange={(value) => {
                        if (!mutationLocked()) {
                          setRepeatEnabled(value)
                          setRepeatErrors({})
                        }
                      }}
                      onPatternChange={(value) => {
                        if (!mutationLocked()) {
                          setRepeatPattern(value)
                          setRepeatErrors({})
                        }
                      }}
                      onHintInteraction={(materialId) =>
                        setRepeatHintRequest((current) =>
                          current?.materialId === materialId &&
                          current.context === feedbackContextKey
                            ? current
                            : { materialId, context: feedbackContextKey },
                        )
                      }
                    />
                  </div>
                  <div className="workbench-item-controls">
                    <button
                      type="button"
                      className="item-input-toggle"
                      disabled={workbenchBusy}
                      ref={baseAnchor}
                      aria-expanded={leftMenu === 'base'}
                      aria-controls="workbench-base-picker"
                      onClick={() => {
                        if (mutationLocked()) return
                        setBaseChoice(
                          supersededBases[catalogBase] ?? catalogBase,
                        )
                        setLeftMenu(leftMenu === 'base' ? null : 'base')
                      }}
                    >
                      {t('ui.select_base')}
                    </button>
                    <button
                      type="button"
                      ref={inputToggle}
                      className="item-input-toggle"
                      disabled={workbenchBusy}
                      aria-expanded={inputOpen}
                      aria-controls="item-input-panel"
                      onClick={(event) => {
                        if (mutationLocked()) return
                        inputOpener.current = event.currentTarget
                        setInputOpen((open) => !open)
                      }}
                    >
                      {t('ui.edit_item')}
                    </button>
                    <button
                      type="button"
                      className="item-input-toggle"
                      disabled={workbenchBusy}
                      onClick={() => {
                        if (mutationLocked()) return
                        invalidateInput()
                        void startBase(catalogLevel, catalogBase)
                      }}
                    >
                      {t('ui.new_craft')}
                    </button>
                  </div>
                  <div className="workbench-stash-column">
                    <WorkbenchStash
                      tabs={
                        <>
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
                                disabled={workbenchBusy}
                                onClick={() => {
                                  if (!mutationLocked()) setActiveTab(tab.id)
                                }}
                                onKeyDown={(event) => {
                                  if (mutationLocked()) return
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
                                  document
                                    .getElementById(`tab-${target.id}`)
                                    ?.focus()
                                }}
                              >
                                {tab.id === 'Augments'
                                  ? socketText().tab
                                  : t(`stash.${tab.id}`)}
                              </button>
                            ))}
                          </div>
                        </>
                      }
                      fixedCurrencyIds={fixedCurrencies.map(
                        (currency) => currency.id,
                      )}
                      resources={stashResources}
                      excludedIds={excludedResourceIds}
                      activeTab={activeTab}
                      selectedId={selected?.id ?? null}
                      activeOmenIds={draft.activeOmens}
                      held={held}
                      disabled={workbenchBusy}
                      onActivateOmen={(resource) => toggleOmen(resource.id)}
                      onSelect={(resource, pointer) => {
                        if (mutationLocked()) return
                        setPointer(pointer ?? null)
                        choose(resource)
                      }}
                      onPickUp={(resource, pointer) => {
                        if (mutationLocked()) return
                        setFailedApplication(null)
                        setRepeatNotice(null)
                        setPointer(pointer ?? null)
                        setHeld(resource)
                        setSelected(null)
                        setAnnouncement(
                          t('material.picked', {
                            name: name(resource.id, resource.name),
                          }),
                        )
                      }}
                    />
                    <div className="workbench-feedback-region wb-scrollbar">
                      {(applicationFailure || repeatSuccess) && (
                        <div
                          className="workbench-feedback-panel wb-scrollbar"
                          tabIndex={0}
                          role={applicationFailure ? 'alert' : 'status'}
                          aria-label={t('ui.crafting_notes')}
                        >
                          <p className="workbench-feedback">
                            {applicationFailure
                              ? serviceText(applicationFailure)
                              : repeatSuccess}
                          </p>
                        </div>
                      )}
                      <FeedbackTooltipOutlet />
                    </div>
                  </div>
                  <WorkbenchFavorites
                    excludedIds={excludedResourceIds}
                    selectedId={selected?.id ?? null}
                    activeOmenIds={draft.activeOmens}
                    favorites={favorites}
                    held={held}
                    disabled={workbenchBusy}
                    onFavoriteActivate={(_, resource, pointer) => {
                      if (mutationLocked()) return
                      setPointer(pointer ?? null)
                      if (resource) choose(resource)
                      else setSelected(null)
                    }}
                    onActivateOmen={(resource) => toggleOmen(resource.id)}
                    onFavoritePlace={placeFavorite}
                  />
                </div>
              </div>
              <div className="workbench-currency-controls">
                {selected &&
                  isBone(selected.id) &&
                  draft.activeOmens.includes('Omen_of_Putrefaction') && (
                    <fieldset disabled={workbenchBusy}>
                      <legend>{name(selected.id, selected.name)}</legend>
                      {draft.activeOmens.includes('Omen_of_Putrefaction') ? (
                        <div>
                          {(['PREFIX', 'SUFFIX'] as const).map(
                            (side, index) => (
                              <label key={side}>
                                {putrefactionText[locale][index + 1]}{' '}
                                <input
                                  type="number"
                                  min={0}
                                  max={3}
                                  step={1}
                                  value={boneCounts[index]}
                                  onChange={(event) => {
                                    if (mutationLocked()) return
                                    const value = event.target.valueAsNumber
                                    if (
                                      Number.isInteger(value) &&
                                      value >= 0 &&
                                      value <= 3
                                    )
                                      setBoneCounts((current) =>
                                        current.map((n, i) =>
                                          i === index ? value : n,
                                        ),
                                      )
                                  }}
                                />
                              </label>
                            ),
                          )}
                        </div>
                      ) : null}
                    </fieldset>
                  )}
                {draft.source === 'text' && !mapping.data?.mapped && (
                  <button
                    type="button"
                    onClick={() => void enableCrafting()}
                    disabled={workbenchBusy || !initial.data || !importedBase}
                  >
                    {t('ui.enable_solar_crafting')}
                  </button>
                )}
              </div>
              <div className="workbench-input-status">
                {mapping.data?.issues.map((issue, index) => (
                  <p key={index}>
                    {' '}
                    {t('ui.line')} {issue.lineNumber || 'item'}:{' '}
                    <ServiceMessage text={issue.message} />
                  </p>
                ))}
                {initial.isError && (
                  <p role="alert">
                    {' '}
                    {t('ui.catalog_load_error')}{' '}
                    <button
                      type="button"
                      disabled={workbenchBusy}
                      onClick={() => {
                        if (!mutationLocked()) void initial.refetch()
                      }}
                    >
                      {t('ui.retry_catalog')}
                    </button>
                  </p>
                )}
                {evidenceInvalid && (
                  <p role="alert">{t('notice.saved_evidence')}</p>
                )}
              </div>
            </div>
            <div className="bench-lower" ref={basePlacementRegion}>
              <SessionLibraryButtons
                libraryId="workbench-session-library"
                historyAnchor={historyAnchor}
                favoritesAnchor={favoritesAnchor}
                disabled={workbenchBusy}
                openView={
                  leftMenu === 'history' || leftMenu === 'favorites'
                    ? leftMenu
                    : null
                }
                onOpenChange={(menu) => {
                  if (!mutationLocked()) setLeftMenu(menu)
                }}
              >
                <div className="workbench-film-controls">
                  <button
                    type="button"
                    aria-label={t('ui.previous_crafting_step')}
                    disabled={
                      !film || filmState.history.cursor === 0 || workbenchBusy
                    }
                    onClick={() => browse(filmState.history.cursor - 1)}
                  >
                    <span
                      className="workbench-film-controls__arrow"
                      aria-hidden="true"
                    >
                      ‹
                    </span>
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
                      workbenchBusy
                    }
                    onClick={() => browse(filmState.history.cursor + 1)}
                  >
                    <span
                      className="workbench-film-controls__arrow"
                      aria-hidden="true"
                    >
                      ›
                    </span>
                  </button>
                </div>
              </SessionLibraryButtons>
              <div className="bench-item-card">
                {!!concrete?.observations?.augmentEffects.length && (
                  <p>{t('notice.resolve_augments')}</p>
                )}
                {film?.sourceDocument !== undefined && (
                  <details>
                    <summary>{t('film.original_paste')}</summary>
                    <pre
                      style={{
                        whiteSpace: 'pre-wrap',
                        overflowWrap: 'anywhere',
                      }}
                    >
                      {film.sourceDocument}
                    </pre>
                  </details>
                )}
                {filmState.error && (
                  <p role="alert">{uiText(filmState.error)}</p>
                )}
                {storedFrame &&
                  rulesetStatus &&
                  rulesetStatus !== 'current' && (
                    <section aria-label={t('ruleset.saved_record')}>
                      <p role="alert">{t(`ruleset.${rulesetStatus}`)}</p>
                    </section>
                  )}
                {storedFrame &&
                  rulesetStatus === 'current' &&
                  initial.data &&
                  !restoredValid && (
                    <p role="alert">{t('notice.saved_catalog')}</p>
                  )}
                {storedFrame && !restoredValid && (
                  <details>
                    <summary>{t('ruleset.saved_record')}</summary>
                    <pre>{JSON.stringify(storedFrame, null, 2)}</pre>
                  </details>
                )}
                {concrete?.rarity === 'RARE' &&
                  initial.data &&
                  restoredValid && (
                    <AbyssPanel
                      key={JSON.stringify([concrete, storedFrame?.abyss])}
                      source={concrete}
                      localScenario={false}
                      allowAdd={false}
                      directSelection
                      embedded={!!concrete.desecrated?.length}
                      initial={initial.data}
                      {...(storedFrame?.abyss
                        ? { restoredState: storedFrame.abyss }
                        : {})}
                      externalBusy={applying !== null || imported.pending}
                      canMutate={canMutateAbyss}
                      onBusyChange={handleAbyssBusy}
                      onTransition={(next, transition) => {
                        if (transition.operation === 'restore') return
                        if (
                          useItemDraft.getState().baseRevision !==
                            draft.baseRevision ||
                          applyingRequest.current
                        )
                          return
                        saveFilms(
                          recordAbyss(
                            filmState.history,
                            concrete,
                            next,
                            transition.operation === 'apply'
                              ? (transition.action ?? 'ABYSS_APPLY')
                              : `ABYSS_${transition.operation.toUpperCase()}`,
                            filmId(),
                            initial.data!.rulesetIdentity,
                          ),
                          draft.baseRevision,
                        )
                      }}
                    />
                  )}
                {storedFrame && !restoredValid ? null : card ? (
                  <ItemCard
                    item={{
                      ...card,
                      modifiers: [
                        ...card.modifiers,
                        ...(concrete?.desecrated ?? [])
                          .filter(
                            (slot) =>
                              slot.modifierId === null &&
                              !card.modifiers.some(
                                (line) => line.id === slot.id,
                              ),
                          )
                          .map((slot) => ({
                            id: slot.id,
                            lastCurrencyAffected: lastAffected.has(slot.id),
                            text: abyssWorkbenchMessages[locale].hidden,
                            kind: 'desecrated' as const,
                            unrevealedDesecrated: true,
                            affixLabel: slot.side === 'PREFIX' ? 'P' : 'S',
                          })),
                      ],
                    }}
                    showOriginalOrder={altHeld}
                    {...(concrete
                      ? { baseItemId: baseSlugs[catalogBase] }
                      : {})}
                  />
                ) : (
                  <ItemCard
                    baseItemId={baseSlugs[catalogBase]}
                    item={{
                      rarity: baseStartingRarity(catalogBase),
                      name: baseName,
                      base: baseName,
                      itemClass: baseClass(catalogBase),
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
              </div>
              <SessionNotation
                film={film}
                cursor={filmState.history.cursor}
                resources={stashResources}
                onNavigate={browse}
                disabled={workbenchBusy}
              />
            </div>
          </div>
          <WorkbenchBasePicker
            id="workbench-base-picker"
            anchor={baseAnchor}
            placementRegion={basePlacementRegion}
            shelf={currencyShelf}
            open={leftMenu === 'base'}
            onOpenChange={(open) => {
              if (!mutationLocked()) setLeftMenu(open ? 'base' : null)
            }}
            value={baseChoice}
            onChange={(value) => {
              if (!mutationLocked()) setBaseChoice(value)
            }}
            level={baseLevel}
            onLevelChange={(value) => {
              if (!mutationLocked()) setBaseLevel(value)
            }}
            disabled={workbenchBusy}
            valid={
              !(
                baseChoice === 'sapphire' &&
                sapphireAffix &&
                sapphireRarity !== 'NORMAL' &&
                (sapphireRoll.trim() === '' ||
                  !Number.isInteger(Number(sapphireRoll)) ||
                  Number(sapphireRoll) < 2 ||
                  Number(sapphireRoll) > 4)
              )
            }
            extraControls={
              <>
                {Object.hasOwn(workbenchJewelBases, baseChoice) && (
                  <>
                    <p>
                      {t(
                        catalogBase.startsWith('time-lost-')
                          ? 'notice.time_lost_scope'
                          : 'notice.sapphire_scope',
                      )}
                    </p>
                    <label htmlFor="sapphire-rarity">{t('ui.rarity')}</label>
                    <select
                      id="sapphire-rarity"
                      value={sapphireRarity}
                      disabled={workbenchBusy}
                      onChange={(e) => {
                        if (!mutationLocked())
                          setSapphireRarity(
                            e.target.value as 'NORMAL' | 'MAGIC' | 'RARE',
                          )
                      }}
                    >
                      <option value="NORMAL">{t('ui.normal')}</option>
                      <option value="MAGIC">{t('ui.magic')}</option>
                      <option value="RARE">{t('ui.rare')}</option>
                    </select>
                    <label hidden={baseChoice !== 'sapphire'}>
                      <input
                        type="checkbox"
                        checked={
                          baseChoice === 'sapphire' &&
                          sapphireAffix &&
                          sapphireRarity !== 'NORMAL'
                        }
                        disabled={workbenchBusy || sapphireRarity === 'NORMAL'}
                        onChange={(e) => {
                          if (!mutationLocked())
                            setSapphireAffix(e.target.checked)
                        }}
                      />
                      {t('ui.sapphire_existing_suffix')}
                    </label>
                    {baseChoice === 'sapphire' &&
                      sapphireAffix &&
                      sapphireRarity !== 'NORMAL' && (
                        <>
                          <label htmlFor="sapphire-roll">
                            {t('ui.sapphire_cast_speed')}
                          </label>
                          <input
                            id="sapphire-roll"
                            type="number"
                            min="2"
                            max="4"
                            step="1"
                            value={sapphireRoll}
                            disabled={workbenchBusy}
                            onChange={(e) => {
                              if (!mutationLocked())
                                setSapphireRoll(e.target.value)
                            }}
                          />
                        </>
                      )}
                  </>
                )}
              </>
            }
            onPlace={() => {
              if (mutationLocked()) return
              invalidateInput()
              void startBase(
                Number(baseLevel),
                baseChoice,
                null,
                sapphireRarity,
                baseChoice === 'sapphire' &&
                  sapphireAffix &&
                  sapphireRarity !== 'NORMAL'
                  ? Number(sapphireRoll)
                  : null,
              )
              setSelected(null)
              setLeftMenu(null)
            }}
          />
          <SessionLibrary
            id="workbench-session-library"
            anchor={leftMenu === 'favorites' ? favoritesAnchor : historyAnchor}
            placementRegion={basePlacementRegion}
            shelf={currencyShelf}
            open={leftMenu === 'history' || leftMenu === 'favorites'}
            favoritesOnly={leftMenu === 'favorites'}
            onOpenChange={(open) => {
              if (!mutationLocked() && !open) setLeftMenu(null)
            }}
            history={filmState.history}
            disabled={workbenchBusy || filmState.blockedSaving}
            onSelect={(id) => {
              if (mutationLocked()) return
              restoreFilm(id)
              setLeftMenu(null)
            }}
            onRename={(id, title) =>
              commitLibraryHistory(
                updateFilmMetadata(filmState.history, id, { title }),
              )
            }
            onFavoriteChange={(id, favorite) =>
              commitLibraryHistory(
                updateFilmMetadata(filmState.history, id, { favorite }),
              )
            }
            onDelete={deleteSession}
            {...(lastDeletion
              ? {
                  undo: {
                    title: lastDeletion.film.title || t('ui.crafting_session'),
                    onUndo: undoSessionDeletion,
                  },
                }
              : {})}
          />
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
                disabled={inputBusy}
                onClick={closeInput}
              >
                ×
              </button>
              <div className="input-heading">
                <h3>{t('ui.starting_item')}</h3>
                <span>01</span>
              </div>
              <div className="text-form">
                <label htmlFor="item-text">
                  {t('ui.item_text_copied_from_the_game')}
                </label>
                <textarea
                  id="item-text"
                  value={draft.text}
                  disabled={inputBusy}
                  onChange={(event) => {
                    if (inputMutationLocked()) return
                    invalidateInput()
                    draft.setText(event.target.value)
                  }}
                  placeholder={
                    'Hover over an item and press Ctrl+C\nPaste the copied text here.'
                  }
                  aria-invalid={Boolean(imported.error)}
                  aria-describedby={imported.error ? 'import-error' : undefined}
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
                  disabled={workbenchBusy}
                >
                  {imported.pending
                    ? t('ui.analyzing_item')
                    : t('ui.analyze_item')}{' '}
                  <span aria-hidden="true">↗</span>
                </button>
              </div>
            </div>
          </dialog>
        </div>
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
        {view !== 'support' && (
          <aside className="locale-disclosure">
            <p>{t('probability.disclaimer')}</p>
            <small>{t('translation.coverage')}</small>
          </aside>
        )}
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
      <MaterialTooltip />
      {cursor &&
        pointer &&
        createPortal(
          <span
            className="currency-cursor"
            aria-hidden="true"
            style={{ left: pointer.x + 14, top: pointer.y + 14 }}
          >
            <CurrencyImage key={cursor.id} {...cursor} />
          </span>,
          document.body,
        )}
    </main>
  )
  return (
    <FeedbackTooltipProvider
      enabled={view === 'workbench'}
      priorityMaterialId={cursor?.id ?? null}
      priorityAnchor={feedbackAnchor}
      contextKey={feedbackContextKey}
      priorityMaterialHint={repeatHint}
      interactionLocked={workbenchBusy}
      isInteractionLocked={mutationLocked}
    >
      {page}
    </FeedbackTooltipProvider>
  )
}
