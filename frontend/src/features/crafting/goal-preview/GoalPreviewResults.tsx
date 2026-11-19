import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { loadModifierDefinitions } from '../../../shared/i18n/localeData'
import { uiText, useI18n, type Locale } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import type { Definition } from '../craftingApi'
import { localizedCatalystQuality } from '../localizedCrafting'
import {
  localizedModifierName,
  localizedModifierText,
} from '../localizedModifiers'
import {
  createItemPresentation,
  type ItemPresentation,
} from '../path-tree/presentation'
import type { Item } from '../path-tree/types'
import type { CatalystQuality } from '../catalystQuality'
import { augmentName } from '../socketAugments'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { goalFilterMessages } from '../goal-filter/i18n'
import { setupMessages } from '../goal-filter/setupMessages'
import { hasActiveStatFilters } from '../goal-filter/useGoalFilter'
import { qualityGoalMessages } from '../goal-filter/qualityGoalMessages'
import { readGoalFilterResponse } from '../goal-filter/api'
import { presentGoalStat } from '../goal-filter/statPresentation'
import type { GoalPreviewInput } from '../goal-filter/CraftStart'
import type { Range } from '../goal-filter/types'
import { weighted } from '../goal-filter/types'
import { previewResultMessages } from './previewResultMessages'
import {
  acquisitionSourceLabel,
  acquisitionSourceLabels,
  type AcquisitionSource,
} from './acquisitionSources'
import './previewResults.css'

type PreviewItem = Item & {
  catalystQuality: CatalystQuality | null
  installedAugments?: { augmentId: string }[]
}
type Example = {
  item: PreviewItem
  tierVariants: PreviewItem[]
  sources: Record<string, AcquisitionSource[]>
}
type Preview = {
  cards: Example[]
  modifiers: Record<string, Definition>
  resolvedCountRanges?: Record<string, Range>
  reasonCode?: string | null
}

function validCard(
  card: Example,
  definitions: Preview['modifiers'],
  context: GoalPreviewInput['context'],
) {
  if (!Array.isArray(card.tierVariants) || !card.tierVariants.length)
    return false
  const { sources } = card
  return (
    Object.values(sources).every(
      (entries) => entries.length && entries.every(acquisitionSourceLabel),
    ) &&
    [card.item, ...card.tierVariants].every(
      (item) =>
        item.snapshotId === context.snapshotId &&
        item.baseItemId === context.baseItemId &&
        item.itemLevel === context.itemLevel &&
        [...item.implicits, ...item.explicits].every(
          ({ modifierId }) =>
            definitions[modifierId] && sources[modifierId]?.length,
        ),
    )
  )
}

function bounds(range: Range, locale: Locale, unit = '') {
  const n = (value: number) =>
    value.toLocaleString(locale, { maximumFractionDigits: 20 }) +
    (unit === 'percent' ? '%' : '')
  if (range.min === null && range.max === null)
    return previewResultMessages[locale].presence
  if (range.min === null) return '≤ ' + n(range.max!)
  if (range.max === null) return '≥ ' + n(range.min)
  return range.min === range.max
    ? n(range.min)
    : n(range.min) + ' – ' + n(range.max)
}

function rarityLabel(rarity: string, locale: Locale) {
  return uiText(rarity.charAt(0) + rarity.slice(1).toLowerCase(), locale)
}

function matchingTierLabels(
  example: Example,
  definitions: Record<string, Definition>,
  locale: Locale,
) {
  const variants = example.tierVariants
  if (variants.length < 2) return []
  const commonIds = new Set(
    example.item.explicits
      .map((m) => m.modifierId)
      .filter((id) =>
        variants.every((variant) =>
          variant.explicits.some((m) => m.modifierId === id),
        ),
      ),
  )
  return variants.map((variant) =>
    variant.explicits
      .filter((m) => !commonIds.has(m.modifierId))
      .map((m) => {
        const definition = definitions[m.modifierId]!
        const name = localizedModifierName(definition, locale)
        const text = localizedModifierText(definition, m.values, locale)
        return [definition.tier ? 'T' + definition.tier : null, name, text]
          .filter(Boolean)
          .join(' · ')
      })
      .join(' / '),
  )
}

function PreviewCard({
  example,
  definitions,
  present,
  locale,
}: {
  example: Example
  definitions: Record<string, Definition>
  present: ItemPresentation
  locale: Locale
}) {
  const [selected, setSelected] = useState(0)
  const { tierVariants, sources } = example
  const item = tierVariants[selected] ?? example.item
  const copy = previewResultMessages[locale]
  const card = present(item, locale)
  const quality = item.catalystQuality
  const tierLabels = useMemo(
    () => matchingTierLabels(example, definitions, locale),
    [example, definitions, locale],
  )
  return (
    <ItemCard
      baseItemId={item.baseItemId}
      item={{
        ...card,
        properties: [
          { id: 'rarity', text: rarityLabel(item.rarity, locale) },
          ...(quality
            ? [
                {
                  id: 'quality',
                  text: localizedCatalystQuality(
                    quality,
                    item.baseItemId,
                    locale,
                  ),
                },
              ]
            : []),
          ...(item.installedAugments ?? []).map((a, i) => ({
            id: 'socket-' + i,
            text: augmentName(a.augmentId),
          })),
        ],
        modifiers: card.modifiers.map((line) => ({
          ...line,
          sourceLabel:
            copy.sources +
            ' · ' +
            acquisitionSourceLabels(sources[line.id]!).join(', '),
        })),
      }}
    >
      {tierVariants.length > 1 && (
        <label className="goal-preview-tiers">
          {copy.tiers} ({tierVariants.length.toLocaleString(locale)})
          <select
            value={selected}
            onChange={(e) => setSelected(Number(e.target.value))}
          >
            {tierLabels.map((label, index) => (
              <option key={index} value={index}>
                {label}
              </option>
            ))}
          </select>
        </label>
      )}
    </ItemCard>
  )
}

export function GoalPreviewResults({
  base,
  initial,
  context,
  catalog,
  goal,
}: GoalPreviewInput) {
  const { locale, name } = useI18n()
  const copy = previewResultMessages[locale],
    labels = goalFilterMessages[locale]
  const { rulesetIdentity } = initial
  const preview = useQuery({
    queryKey: ['goalPreview', rulesetIdentity, context, goal],
    queryFn: async ({ signal }) => {
      const response = await craftingFetch(
        '/api/v1/crafting/support/goal-filters/preview',
        {
          method: 'POST',
          signal,
          headers: rulesetHeaders(rulesetIdentity),
          body: JSON.stringify({ context, goal }),
        },
      )
      if (response.ok) verifyRulesetResponse(response, rulesetIdentity)
      const result = await readGoalFilterResponse<Preview>(response)
      if (
        !Array.isArray(result.cards) ||
        !result.modifiers ||
        !result.cards.every((card) =>
          validCard(card, result.modifiers, context),
        )
      )
        throw new Error('Invalid goal preview')
      await loadModifierDefinitions(Object.keys(result.modifiers))
      signal.throwIfAborted()
      return result
    },
    retry: false,
    staleTime: Infinity,
  })
  const presentationCatalog = {
    ...catalog,
    sourceModifiers: catalog.sourceModifiers ?? initial.modifiers,
  }
  const present = createItemPresentation(
    { name: base[2], itemClass: base[1] },
    preview.data?.modifiers ?? {},
  )
  return (
    <section className="goal-preview-results" aria-label={copy.conditions}>
      <h2>{copy.conditions}</h2>
      <p>
        {name(context.baseItemId, base[2])} · {labels.level} {context.itemLevel}{' '}
        ·{' '}
        {goal.general.rarities
          .map((rarity) => rarityLabel(rarity, locale))
          .join(' / ')}
        {goal.general.quality
          ? ` · ${qualityGoalMessages[locale].label} ${goal.general.quality}%`
          : ''}
      </p>
      {goal.groups.filter(hasActiveStatFilters).map((group) => {
        const range =
          group.type === 'COUNT'
            ? preview.data?.resolvedCountRanges?.[group.id]
            : group.range
        return (
          <div key={group.id}>
            <strong>{setupMessages[locale].groups[group.type]}</strong>
            {' · '}
            {labels.operations[group.type]}
            {range && (range.min !== null || range.max !== null) && (
              <>
                {' '}
                {group.type === 'COUNT' ? labels.count : labels.score}{' '}
                {bounds(range, locale)}
              </>
            )}
            <ul>
              {group.entries
                .filter((entry) => !entry.disabled)
                .map((entry) => {
                  const stat = catalog.stats.find(
                    (s) => s.statId === entry.statId,
                  )
                  return (
                    <li key={entry.id}>
                      {stat
                        ? presentGoalStat(stat, presentationCatalog, locale)
                            .label
                        : entry.statId}
                      {' · '}
                      {bounds(entry.range, locale, entry.unit)}
                      {weighted(group.type) && (
                        <>
                          {' '}
                          · {labels.weight} {entry.weight ?? 1}
                        </>
                      )}
                    </li>
                  )
                })}
            </ul>
          </div>
        )
      })}
      <h2>{copy.examples}</h2>
      {preview.isPending && <p role="status">{copy.loading}</p>}
      {preview.isError && (
        <p role="alert">
          {labels.error}{' '}
          <button type="button" onClick={() => void preview.refetch()}>
            {copy.retry}
          </button>
        </p>
      )}
      {preview.data?.cards.length === 0 && (
        <p role="status">
          {preview.data.reasonCode === 'QUALITY_PROJECTION_NOT_IMPLEMENTED'
            ? copy.qualityProjection
            : copy.empty}
        </p>
      )}
      <div className="goal-preview-cards">
        {preview.data?.cards.map((example) => (
          <PreviewCard
            key={JSON.stringify(example.item)}
            example={example}
            definitions={preview.data.modifiers}
            locale={locale}
            present={present}
          />
        ))}
      </div>
    </section>
  )
}
