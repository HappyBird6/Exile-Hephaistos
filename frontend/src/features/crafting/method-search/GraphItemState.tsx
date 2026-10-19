import { useLayoutEffect, useRef } from 'react'
import { ItemCard } from '../ItemCard'
import type { ItemCardData } from '../itemCardData'
import { useI18n, uiText } from '../../../shared/i18n/i18n'
import { graphStateChanges } from './graphStateChanges'
import { graphStateMessages } from './graphStateMessages'
export function GraphItemState({
  card,
  previous,
  badge,
  selected,
  select,
  onSize,
}: {
  card: ItemCardData
  previous?: ItemCardData | undefined
  badge: string
  selected: boolean
  select: () => void
  onSize: (height: number) => void
}) {
  const ref = useRef<HTMLDivElement>(null),
    { locale } = useI18n(),
    copy = graphStateMessages[locale],
    changes = graphStateChanges(card, previous)
  useLayoutEffect(() => {
    const el = ref.current
    if (!el) return
    const measure = () => {
      const h = Math.ceil(el.getBoundingClientRect().height)
      if (h > 0) onSize(h)
    }
    measure()
    if (typeof ResizeObserver === 'undefined') return
    const observer = new ResizeObserver(measure)
    observer.observe(el)
    return () => observer.disconnect()
  }, [onSize])
  return (
    <div
      ref={ref}
      className={'graph-item-state' + (selected ? ' is-selected' : '')}
    >
      <div className="graph-item-badges">
        <span>
          {uiText(card.rarity.charAt(0) + card.rarity.slice(1).toLowerCase())}
        </span>
        <span>{badge}</span>
      </div>
      <ItemCard item={card} modifierChanges={changes.marks} />
      {Object.values(changes.marks).includes('added') && (
        <span className="graph-added">+ {copy.added} </span>
      )}
      {Object.values(changes.marks).includes('changed') && (
        <span className="graph-changed">~ {copy.changed} </span>
      )}
      {changes.removed.length > 0 && (
        <div className="graph-removed">
          − {copy.removed}
          <ul>
            {changes.removed.map((line) => (
              <li key={line.id}>{line.text}</li>
            ))}
          </ul>
        </div>
      )}
      <button onClick={select} aria-pressed={selected}>
        {copy.select}
      </button>
    </div>
  )
}
