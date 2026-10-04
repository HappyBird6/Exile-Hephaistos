import { useI18n, formatNumber } from '../../shared/i18n/i18n'
import { isTradePrice } from './itemCardData'
import type { ItemCardData, ItemCardLine } from './itemCardData'
import './item-card.css'

function Lines({
  lines,
  flags = false,
}: {
  lines: readonly ItemCardLine[]
  flags?: boolean
}) {
  const { t } = useI18n()
  const visible = lines.filter((line) => !isTradePrice(line.text))
  if (!visible.length) return null
  return (
    <div className="item-card__section">
      {visible.map((line) => (
        <div
          key={line.id}
          className={`item-card__line item-card__line--${flags && /^(Corrupted|Twice Corrupted)$/.test(line.text.trim()) ? 'corrupted' : (line.kind ?? 'property')}${line.removalCandidate ? ' item-card__line--removal-candidate' : ''}`}
          data-removal-candidate={line.removalCandidate ? line.id : undefined}
          data-fractured={
            line.fractured || line.kind === 'fractured' ? true : undefined
          }
          title={line.removalCandidate ? t('omen.candidate_title') : undefined}
        >
          {line.affixLabel && (
            <span className="item-card__affix">{line.affixLabel}</span>
          )}
          {line.detail ? (
            <details className="item-card__modifier-detail">
              <summary>
                {(line.fractured || line.kind === 'fractured') && (
                  <span className="sr-only">{t('ui.fractured')}: </span>
                )}
                {line.text}
              </summary>
              <span>{line.detail}</span>
            </details>
          ) : (
            <>
              {(line.fractured || line.kind === 'fractured') && (
                <span className="sr-only">{t('ui.fractured')}: </span>
              )}
              {line.text}
            </>
          )}
        </div>
      ))}
    </div>
  )
}

// Rendering has no item state: replacing props updates all fields together.
export function ItemCard({
  item,
  baseItemId,
  showOriginalOrder = false,
}: {
  item: ItemCardData
  baseItemId?: string
  showOriginalOrder?: boolean
}) {
  const { t, name } = useI18n()
  const implicit = item.modifiers.filter((line) => line.kind === 'implicit')
  const other = item.modifiers.filter((line) => line.kind !== 'implicit')
  if (!showOriginalOrder)
    other.sort(
      (a, b) =>
        Number(Boolean(b.fractured || b.kind === 'fractured')) -
        Number(Boolean(a.fractured || a.kind === 'fractured')),
    )
  const doubleHeader =
    (item.rarity === 'RARE' || item.rarity === 'UNIQUE') &&
    item.base &&
    item.base !== item.name
  return (
    <article
      className={`item-card item-card--${item.rarity.toLowerCase()}`}
      aria-label={t('ui.item_card')}
    >
      <header className="item-card__header">
        <h2>
          {baseItemId && item.name === item.base
            ? name(baseItemId, item.name)
            : item.name}
        </h2>
        {doubleHeader && (
          <div>{baseItemId ? name(baseItemId, item.base!) : item.base}</div>
        )}
      </header>
      <div className="item-card__content">
        <div className="item-card__class">
          {item.itemClass || t('ui.unknown')}
        </div>
        <Lines lines={item.properties} />
        <div className="item-card__section">
          <div>
            {t('ui.item_level_colon')}{' '}
            <span className="item-card__value">
              {item.itemLevel === null
                ? t('ui.unknown')
                : formatNumber(item.itemLevel)}
            </span>
          </div>
          {item.requirements
            .filter((line) => !isTradePrice(line.text))
            .map((line) => (
              <div key={line.id}>{line.text}</div>
            ))}
        </div>
        <Lines lines={implicit} />
        <Lines lines={other} />
        <Lines lines={item.flags} flags />
      </div>
    </article>
  )
}
