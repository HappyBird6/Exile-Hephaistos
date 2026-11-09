import { useEffect, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { currencies } from '../currencies'
import type { Film } from '../workbenchHistory'
import { useCurrencyMarket } from './context'
import { currencyQuote, craftingCost } from './cost'
import { priceMessages } from './messages'
import './currency-prices.css'

const images = new Map(currencies.map((c) => [c.id, c.image]))
export function CoinAmount({
  value,
  kind,
  prefix = '',
  iconOnly = false,
}: {
  value: number | null
  kind: 'Divine_Orb' | 'Exalted_Orb'
  prefix?: string
  iconOnly?: boolean
}) {
  const { locale, name } = useI18n()
  const amount =
    value === null
      ? '—'
      : new Intl.NumberFormat(locale, {
          maximumFractionDigits:
            value > 0 && value < (kind === 'Exalted_Orb' ? 0.01 : 0.0001)
              ? 2
              : kind === 'Exalted_Orb'
                ? 2
                : 4,
          notation:
            value > 0 && value < (kind === 'Exalted_Orb' ? 0.01 : 0.0001)
              ? 'scientific'
              : 'standard',
        }).format(value)
  return (
    <span
      className="coin-amount"
      aria-label={`${prefix}${amount} ${name(kind, kind === 'Divine_Orb' ? 'Divine Orb' : 'Exalted Orb')}`}
    >
      <img src={images.get(kind)} alt="" draggable={false} />
      {!iconOnly && (
        <span aria-hidden="true">
          {prefix}
          {amount}
        </span>
      )}
    </span>
  )
}
export function CurrencyTicker() {
  const market = useCurrencyMarket()
  const { locale, name } = useI18n()
  const text = priceMessages[locale]
  const [now, setNow] = useState(Date.now)
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [])
  const view = market?.data,
    snapshot = view?.snapshot
  const time = (value: string) =>
    new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    }).format(new Date(value))
  const allowed = view ? Date.parse(view.nextRefreshAllowedAt) <= now : true
  const details = [
    snapshot?.league,
    snapshot
      ? `${text.updated}: ${new Intl.DateTimeFormat(locale, { dateStyle: 'medium', timeStyle: 'medium' }).format(new Date(snapshot.updatedAt))}`
      : text.unavailable,
    view?.stale ? text.stale : '',
    !allowed && view
      ? `${text.waiting}: ${time(view.nextRefreshAllowedAt)}`
      : '',
    text.source,
  ]
    .filter(Boolean)
    .join('\n')
  return (
    <section
      className={`currency-ticker${view?.stale || market?.error ? ' currency-ticker--stale' : ''}`}
      aria-label={text.market}
      title={details}
    >
      <div className="currency-ticker__display">
        <a
          href="https://poe.ninja/poe2/economy"
          target="_blank"
          rel="noreferrer"
          aria-label={`1 ${name('Divine_Orb', 'Divine Orb')} ~ ${snapshot ? new Intl.NumberFormat(locale, { maximumFractionDigits: 2 }).format(snapshot.exaltedPerDivine) : '—'} ${name('Exalted_Orb', 'Exalted Orb')} · ${snapshot?.league ?? ''} · ${text.source}`}
        >
          <CoinAmount kind="Divine_Orb" value={1} iconOnly />
          <span className="currency-ticker__separator" aria-hidden="true">
            ~
          </span>
          <CoinAmount
            kind="Exalted_Orb"
            value={snapshot?.exaltedPerDivine ?? null}
          />
        </a>
        <small>
          <svg viewBox="0 0 20 20" aria-hidden="true">
            <circle cx="10" cy="10" r="7.2" />
            <path d="M10 5.5V10l3 1.7" />
          </svg>
          {snapshot ? (
            <time dateTime={snapshot.updatedAt}>
              {time(snapshot.updatedAt)}
            </time>
          ) : market?.pending ? (
            '···'
          ) : (
            text.unavailable
          )}
        </small>
      </div>
      <button
        type="button"
        className="currency-ticker__refresh"
        aria-label={text.refresh}
        disabled={!market || market.refreshing}
        onClick={() => market?.refresh()}
      >
        <svg
          aria-hidden="true"
          viewBox="0 0 20 20"
          className={market?.refreshing ? 'currency-ticker__spinner' : ''}
        >
          <path d="M16.5 8a6.7 6.7 0 1 0-.7 5.3M16.5 3.5V8H12" />
        </svg>
      </button>
    </section>
  )
}
export function CurrencyPriceFooter({ materialId }: { materialId: string }) {
  const market = useCurrencyMarket()
  const { locale } = useI18n()
  if (!market) return null
  const quote = currencyQuote(market.data?.snapshot, materialId)
  return (
    <footer
      className="currency-price-footer"
      aria-label={priceMessages[locale].market}
    >
      {quote ? (
        <>
          <CoinAmount kind="Exalted_Orb" value={quote.exalted} />
          <span aria-hidden="true">·</span>
          <CoinAmount kind="Divine_Orb" value={quote.divine} />
        </>
      ) : (
        <span>{priceMessages[locale].unknown}</span>
      )}
    </footer>
  )
}
export function CraftingCostLine({
  film,
  cursor,
}: {
  film: Film | undefined
  cursor: number
}) {
  const market = useCurrencyMarket()
  const { locale, name } = useI18n()
  const text = priceMessages[locale]
  const cost = craftingCost(film, cursor, market?.data?.snapshot)
  const priced = market?.data?.snapshot && (!cost.incomplete || cost.divine > 0)
  const details = cost.incomplete
    ? `${text.incomplete}${cost.unknown.length ? ': ' + cost.unknown.map((id) => name(id, id.replaceAll('_', ' '))).join(', ') : ''}`
    : text.spent
  return (
    <div
      className="crafting-cost-line"
      role="group"
      aria-label={text.spent}
      title={details}
    >
      <CoinAmount
        kind="Exalted_Orb"
        value={priced ? cost.exalted : null}
        prefix={priced && cost.incomplete ? '≥ ' : ''}
      />
      <span aria-hidden="true">·</span>
      <CoinAmount
        kind="Divine_Orb"
        value={priced ? cost.divine : null}
        prefix={priced && cost.incomplete ? '≥ ' : ''}
      />
      {cost.incomplete && (
        <span className="crafting-cost-line__partial" aria-label={details}>
          *
        </span>
      )}
    </div>
  )
}
