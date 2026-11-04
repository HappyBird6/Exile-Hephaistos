import { useId, useState } from 'react'
import type { ReactNode } from 'react'
import { useI18n, uiText } from '../../../shared/i18n/i18n'
import type { WorkbenchBaseKey } from '../baseRegistry'
import { BasePreviewTooltip } from './BasePreviewTooltip'
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
  disabled?: boolean
  /** Existing jewel rarity/observed suffix controls; never catalyst controls. */
  extraControls?: ReactNode
  valid?: boolean
  imageForBase?: (key: WorkbenchBaseKey) => string | undefined
}

export function WorkbenchBasePicker(props: WorkbenchBasePickerProps) {
  if (!props.open) return null
  return <BasePickerContent {...props} />
}

function BasePickerContent({
  id,
  anchor,
  placementRegion,
  shelf,
  onOpenChange,
  value,
  onChange,
  level,
  onLevelChange,
  onPlace,
  disabled = false,
  extraControls,
  valid = true,
  imageForBase,
}: WorkbenchBasePickerProps) {
  const { locale, t } = useI18n()
  const messages = basePickerMessages[locale]
  const fieldId = useId()
  const [category, setCategory] = useState<BaseCategory | 'all'>('all')
  const [subtype, setSubtype] = useState('all')
  const [search, setSearch] = useState('')
  const [hovered, setHovered] = useState<{
    key: WorkbenchBaseKey
    anchor: HTMLElement
  } | null>(null)
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
  const preview = choices.find((choice) => choice.key === hovered?.key)
  const numericLevel = Number(level)
  const levelValid =
    level.trim() !== '' &&
    Number.isInteger(numericLevel) &&
    numericLevel >= 1 &&
    numericLevel <= 100
  return (
    <LeftShelfOverlay
      id={id}
      anchor={anchor}
      {...(placementRegion ? { placementRegion } : {})}
      {...(shelf ? { shelf } : {})}
      onClose={() => onOpenChange(false)}
      title={t('ui.select_base')}
      closeLabel={t('ui.close_item_input')}
      bodyClassName="wb-base-picker-body"
    >
      <div className="wb-base-picker">
        <div className="wb-base-picker__controls wb-scrollbar">
          <div className="wb-base-picker__filters">
            <select
              aria-label={messages.category}
              id={`${fieldId}-category`}
              value={category}
              onChange={(event) => {
                setCategory(event.target.value as BaseCategory | 'all')
                setSubtype('all')
                setHovered(null)
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
              onChange={(event) => {
                setSubtype(event.target.value)
                setHovered(null)
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
            onChange={(event) => {
              setSearch(event.target.value)
              setHovered(null)
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
                value={level}
                onChange={(event) => onLevelChange(event.target.value)}
                aria-invalid={!levelValid}
                aria-describedby={
                  !levelValid ? `${fieldId}-level-error` : undefined
                }
              />
            </label>
            {extraControls}
            <button
              type="button"
              disabled={disabled || !valid || !levelValid}
              onClick={onPlace}
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
          onMouseLeave={(event) => {
            const next = event.relatedTarget
            if (
              !(
                next instanceof Element &&
                next.closest('.wb-base-picker__preview')?.id ===
                  `${fieldId}-preview`
              ) &&
              !event.currentTarget.contains(document.activeElement)
            )
              setHovered(null)
          }}
          onBlur={(event) => {
            if (
              !event.currentTarget.contains(event.relatedTarget) &&
              !(
                event.relatedTarget instanceof Element &&
                event.relatedTarget.closest('.wb-base-picker__preview')?.id ===
                  `${fieldId}-preview`
              )
            )
              setHovered(null)
          }}
        >
          {visible.map((choice) => (
            <button
              type="button"
              className="wb-base-picker__option"
              key={choice.key}
              aria-pressed={choice.key === value}
              aria-describedby={
                hovered?.key === choice.key ? `${fieldId}-preview` : undefined
              }
              disabled={disabled}
              onClick={() => onChange(choice.key)}
              onMouseEnter={(event) =>
                setHovered({ key: choice.key, anchor: event.currentTarget })
              }
              onPointerDown={(event) => {
                if (event.pointerType === 'touch')
                  setHovered({ key: choice.key, anchor: event.currentTarget })
              }}
              onFocus={(event) =>
                setHovered({ key: choice.key, anchor: event.currentTarget })
              }
              onKeyDown={(event) => {
                if (event.key === 'Escape' && hovered) {
                  event.stopPropagation()
                  setHovered(null)
                }
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
          ))}
          {!visible.length && <p>{messages.empty}</p>}
        </div>
        {preview && hovered && (
          <BasePreviewTooltip
            onDismiss={() => setHovered(null)}
            id={`${fieldId}-preview`}
            label={messages.preview}
            anchor={hovered.anchor}
            item={basePreview(
              preview,
              levelValid ? numericLevel : null,
              locale,
            )}
            slug={preview.slug}
          />
        )}
      </div>
    </LeftShelfOverlay>
  )
}
