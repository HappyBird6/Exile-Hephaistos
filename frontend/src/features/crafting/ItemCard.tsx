import { useI18n } from '../../shared/i18n/context'
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
  const visible = lines.filter((line) => !isTradePrice(line.text))
  if (!visible.length) return null
  return (
    <div className="item-card__section">
      {visible.map((line) => (
        <div
          key={line.id}
          className={`item-card__line item-card__line--${flags && /^(Corrupted|Twice Corrupted)$/.test(line.text.trim()) ? 'corrupted' : (line.kind ?? 'property')}`}
        >
          {line.affixLabel && (
            <span className="item-card__affix">{line.affixLabel}</span>
          )}
          {line.detail ? (
            <details className="item-card__modifier-detail">
              <summary>{line.text}</summary>
              <span>{line.detail}</span>
            </details>
          ) : (
            line.text
          )}
        </div>
      ))}
    </div>
  )
}

// Rendering has no item state: replacing props updates all fields together.
export function ItemCard({ item }: { item: ItemCardData }) {
  const { t } = useI18n()
  const implicit = item.modifiers.filter((line) => line.kind === 'implicit')
  const other = item.modifiers.filter((line) => line.kind !== 'implicit')
  const doubleHeader =
    (item.rarity === 'RARE' || item.rarity === 'UNIQUE') &&
    item.base &&
    item.base !== item.name
  return (
    <article
      className={`item-card item-card--${item.rarity.toLowerCase()}`}
      aria-label={t('itemCard')}
    >
      <header className="item-card__header">
        <h2>{item.name}</h2>
        {doubleHeader && <div>{item.base}</div>}
      </header>
      <div className="item-card__content">
        <div className="item-card__class">{item.itemClass || t('unknown')}</div>
        <Lines lines={item.properties} />
        <div className="item-card__section">
          <div>
            {t('itemLevel')}:{' '}
            <span className="item-card__value">
              {item.itemLevel ?? t('unknown')}
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
