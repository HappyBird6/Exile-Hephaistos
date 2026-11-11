import type { Film } from '../workbenchHistory'
import { HistoryCard } from '../session-history/HistoryCard'
import {
  useBaseFavorites,
  removeBaseFavorite,
} from '../base-favorites/repository'
import { baseFavoriteMessages } from '../base-favorites/messages'
import { BaseAffixEditor } from './BaseAffixEditor'
import type { BaseAffixEditorProps } from './BaseAffixEditor'
import { startingPreview } from './baseAffixSetup'
import { useId, useState } from 'react'
import type { ReactNode } from 'react'
import { useI18n, uiText } from '../../../shared/i18n/i18n'
import type { WorkbenchBaseKey } from '../baseRegistry'
import { BasePreviewPanel } from './BasePreviewPanel'
import { BaseImage } from './BaseImage'
import { LeftShelfOverlay } from './LeftShelfOverlay'
import type { ShelfOverlayProps } from './LeftShelfOverlay'
import { basePreview, workbenchBaseChoices } from './baseChoices'
import type { BaseCategory } from './baseChoices'
import { basePickerMessages } from './basePickerMessages'
import './base-picker.css'

export type WorkbenchBasePickerProps = Omit<ShelfOverlayProps, 'onClose'> & {
  open: boolean
  onOpenChange: (open: boolean) => void
  value: WorkbenchBaseKey
  onChange: (base: WorkbenchBaseKey) => void
  level: string
  onLevelChange: (level: string) => void
  onPlace: () => void
  onSaveFavorite?: (base: WorkbenchBaseKey, withAffixes?: boolean) => void
  onPlaceFavorite?: (film: Film) => void
  disabled?: boolean
  /** Starting rarity and affixes are staged until the base is placed. */
  setup?: BaseAffixEditorProps | undefined
  extraControls?: ReactNode
  valid?: boolean
  selectedRarity?: 'NORMAL' | 'MAGIC' | 'RARE'
  imageForBase?: (key: WorkbenchBaseKey) => string | undefined
}

export function WorkbenchBasePicker(props: WorkbenchBasePickerProps) {
  if (!props.open) return null
  return <BasePickerContent {...props} />
}

function BasePickerContent({
  id,
  anchor,
  canvas,
  onOpenChange,
  value,
  onChange,
  level,
  onLevelChange,
  onPlace,
  onSaveFavorite,
  onPlaceFavorite,
  disabled = false,
  extraControls,
  valid = true,
  selectedRarity,
  imageForBase,
  setup,
}: WorkbenchBasePickerProps) {
  const { locale, t } = useI18n()
  const messages = basePickerMessages[locale]
  const favoriteText = baseFavoriteMessages[locale]
  const favorites = useBaseFavorites()
  const [favoriteMode, setFavoriteMode] = useState(false)
  const [savedSelection, setSavedSelection] = useState<Film | null>(null)
  const [favoriteError, setFavoriteError] = useState(false)
  const [hoveredSaved, setHoveredSaved] = useState<Film | null>(null)
  const fieldId = useId()
  const [category, setCategory] = useState<BaseCategory | 'all'>('all')
  const [subtype, setSubtype] = useState('all')
  const [search, setSearch] = useState('')
  const [hovered, setHovered] = useState<WorkbenchBaseKey | null>(null)
  const [focused, setFocused] = useState<WorkbenchBaseKey | null>(null)
  function restoreSelectedPreview() {
    setHoveredSaved(null)
    setHovered(null)
    setFocused(null)
  }
  const choices = workbenchBaseChoices(locale)
  const subtypes = [
    ...new Set(
      choices
        .filter((choice) => category === 'all' || choice.category === category)
        .map((choice) => choice.itemClass),
    ),
  ]
  const needle = search.trim().toLocaleLowerCase(locale)
  const visible = choices.filter(
    (choice) =>
      (category === 'all' || choice.category === category) &&
      (subtype === 'all' || choice.itemClass === subtype) &&
      [choice.name, choice.itemClass, choice.slug].some((text) =>
        text.toLocaleLowerCase(locale).includes(needle),
      ),
  )
  const preview = choices.find(
    (choice) => choice.key === (hovered ?? focused ?? value),
  )
  const favoritePreview = hoveredSaved ?? savedSelection
  const numericLevel = Number(level)
  const levelValid =
    level.trim() !== '' &&
    Number.isInteger(numericLevel) &&
    numericLevel >= 1 &&
    numericLevel <= 100
  let previewItem = preview
    ? basePreview(preview, levelValid ? numericLevel : null, locale)
    : null
  if (previewItem && preview?.key === value && selectedRarity)
    previewItem.rarity = selectedRarity
  if (preview && preview.key === value && setup)
    previewItem = startingPreview(
      preview,
      setup.initial,
      setup.item,
      setup.affixes,
      locale,
    )
  return (
    <LeftShelfOverlay
      id={id}
      anchor={anchor}
      {...(canvas ? { canvas } : {})}
      onClose={() => onOpenChange(false)}
      title={t('ui.select_base')}
      closeLabel={t('ui.close_item_input')}
      bodyClassName="wb-base-picker-body"
    >
      <div className="wb-base-picker">
        <div className="wb-base-picker__controls wb-scrollbar">
          <button
            type="button"
            className="wb-base-picker__favorites"
            aria-pressed={favoriteMode}
            onClick={() => {
              setFavoriteMode(!favoriteMode)
              setSavedSelection(null)
              restoreSelectedPreview()
            }}
          >
            ⭐ {favoriteText.list} ({favorites.entries.length})
          </button>
          {(favorites.error || favoriteError) && (
            <p role="alert">{favoriteText.error}</p>
          )}
          <div className="wb-base-picker__filters">
            <select
              aria-label={messages.category}
              id={`${fieldId}-category`}
              value={category}
              disabled={disabled}
              onChange={(event) => {
                if (disabled) return
                setCategory(event.target.value as BaseCategory | 'all')
                setSubtype('all')
                setHovered(null)
                setFocused(null)
              }}
            >
              <option value="all">{messages.all}</option>
              {(['weapons', 'armour', 'accessories', 'jewels'] as const).map(
                (key) => (
                  <option key={key} value={key}>
                    {messages[key]}
                  </option>
                ),
              )}
            </select>
            <select
              aria-label={messages.subtype}
              id={`${fieldId}-subtype`}
              value={subtype}
              disabled={disabled}
              onChange={(event) => {
                if (disabled) return
                setSubtype(event.target.value)
                setHovered(null)
                setFocused(null)
              }}
            >
              <option value="all">{messages.all}</option>
              {subtypes.map((key) => (
                <option key={key} value={key}>
                  {uiText(key, locale)}
                </option>
              ))}
            </select>
          </div>
          <input
            className="wb-base-picker__search"
            type="search"
            aria-label={t('ui.search')}
            placeholder={t('ui.search')}
            value={search}
            disabled={disabled}
            onChange={(event) => {
              if (disabled) return
              setSearch(event.target.value)
              setHovered(null)
              setFocused(null)
            }}
          />
          <div className="wb-base-picker__footer">
            <label
              className="wb-base-picker__level"
              htmlFor={`${fieldId}-level`}
            >
              {t('ui.item_level')}
              <input
                id={`${fieldId}-level`}
                type="number"
                min="1"
                max="100"
                step="1"
                value={
                  savedSelection
                    ? String(savedSelection.frames[0]!.state.itemLevel)
                    : level
                }
                disabled={disabled || !!savedSelection}
                onChange={(event) => {
                  if (!disabled) onLevelChange(event.target.value)
                }}
                aria-invalid={!levelValid}
                aria-describedby={
                  !levelValid ? `${fieldId}-level-error` : undefined
                }
              />
            </label>
            {!savedSelection && extraControls}
            <button
              type="button"
              disabled={
                disabled ||
                (favoriteMode ? !savedSelection : !valid || !levelValid)
              }
              onClick={() =>
                savedSelection ? onPlaceFavorite?.(savedSelection) : onPlace()
              }
            >
              {t('ui.select_base')}
            </button>
          </div>
          {!levelValid && (
            <p id={`${fieldId}-level-error`} role="alert">
              {t('ui.item_level_must_be_an_integer_from_1_to_100')}
            </p>
          )}
        </div>
        <div
          className="wb-base-picker__options wb-scrollbar"
          role="group"
          aria-label={t('ui.equipment_base')}
          onMouseLeave={restoreSelectedPreview}
          onBlur={(event) => {
            if (
              !event.currentTarget.contains(event.relatedTarget) &&
              !(
                event.relatedTarget instanceof Element &&
                event.relatedTarget.closest('.wb-base-picker__preview')?.id ===
                  `${fieldId}-preview`
              )
            )
              setFocused(null)
          }}
        >
          {(favoriteMode
            ? favorites.entries.flatMap((film) => {
                const choice = visible.find(
                  (c) => c.baseItemId === film.frames[0]?.state.baseItemId,
                )
                return choice ? [{ ...choice, favorite: film }] : []
              })
            : visible.map((choice) => ({
                ...choice,
                favorite: undefined as Film | undefined,
              }))
          ).map((choice) => (
            <div
              className="wb-base-picker__row"
              key={choice.favorite?.id ?? choice.key}
            >
              <button
                type="button"
                className="wb-base-picker__option"
                key={choice.key}
                aria-pressed={
                  choice.favorite
                    ? savedSelection?.id === choice.favorite.id
                    : !savedSelection && choice.key === value
                }
                aria-describedby={
                  preview?.key === choice.key ? `${fieldId}-preview` : undefined
                }
                disabled={disabled}
                onClick={() => {
                  setSavedSelection(choice.favorite ?? null)
                  if (!choice.favorite) onChange(choice.key)
                }}
                onMouseEnter={() => {
                  setHovered(choice.key)
                  setHoveredSaved(choice.favorite ?? null)
                }}
                onMouseLeave={restoreSelectedPreview}
                onPointerDown={(event) => {
                  if (event.pointerType === 'touch') setHovered(choice.key)
                }}
                onFocus={() => {
                  setFocused(choice.key)
                  setHoveredSaved(choice.favorite ?? null)
                }}
              >
                <span className="wb-base-picker__art" aria-hidden="true">
                  <BaseImage
                    baseItemId={choice.baseItemId}
                    label={choice.name}
                    image={imageForBase?.(choice.key)}
                  />
                </span>
                <span className="wb-base-picker__label">
                  {choice.name}
                  <small>
                    {uiText(choice.itemClass, locale)}
                    {choice.variantLabel ? ` · ${choice.variantLabel}` : ''}
                  </small>
                </span>
              </button>
              <button
                type="button"
                className="wb-base-picker__star"
                disabled={disabled}
                aria-label={
                  choice.favorite
                    ? favoriteText.remove
                    : `${favoriteText.add}: ${choice.name}`
                }
                onClick={() => {
                  try {
                    if (choice.favorite) {
                      removeBaseFavorite(choice.favorite.id)
                      if (savedSelection?.id === choice.favorite.id)
                        setSavedSelection(null)
                    } else onSaveFavorite?.(choice.key)
                  } catch {
                    setFavoriteError(true)
                  }
                }}
              >
                ⭐
              </button>
            </div>
          ))}
          {(!visible.length || (favoriteMode && !favorites.entries.length)) && (
            <p>{messages.empty}</p>
          )}
        </div>
        {favoritePreview ? (
          <div className="wb-base-picker__preview wb-scrollbar">
            <HistoryCard
              film={favoritePreview}
              frame={favoritePreview.frames[0]!}
            />
          </div>
        ) : (
          preview &&
          previewItem && (
            <BasePreviewPanel
              id={`${fieldId}-preview`}
              label={messages.preview}
              item={previewItem}
              slug={preview.slug}
              onMouseLeave={restoreSelectedPreview}
              onBlur={(event) => {
                if (
                  !event.currentTarget.contains(event.relatedTarget) &&
                  !(
                    event.relatedTarget instanceof Element &&
                    event.relatedTarget.closest('.wb-base-picker__options')
                  )
                )
                  setFocused(null)
              }}
            >
              {setup && preview.key === value && (
                <>
                  <BaseAffixEditor
                    key={`${value}:${level}:${selectedRarity}`}
                    {...setup}
                  />
                  <button
                    type="button"
                    className="wb-base-picker__save-configured"
                    disabled={disabled || !valid || !levelValid}
                    onClick={() => onSaveFavorite?.(value, true)}
                  >
                    {favoriteText.add}
                  </button>
                </>
              )}
            </BasePreviewPanel>
          )
        )}
      </div>
    </LeftShelfOverlay>
  )
}
