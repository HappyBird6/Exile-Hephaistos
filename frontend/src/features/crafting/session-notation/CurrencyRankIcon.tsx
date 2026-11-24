import type { CurrencyRank } from '../custom-exalt/messages'

/** Small geometric Roman numerals stay legible at currency-icon size. */
export function CurrencyRankIcon({ rank }: { rank: CurrencyRank }) {
  const columns = { I: [8], II: [5, 11], III: [2, 8, 14] }[rank]
  return (
    <svg
      aria-hidden="true"
      width="16"
      height="14"
      viewBox="0 0 16 14"
      fill="currentColor"
    >
      {columns.map((x) => (
        <path
          key={x}
          transform={`translate(${x} 0)`}
          d="M-2 2h4v1l-1 .5v7l1 .5v1h-4v-1l1-.5v-7L-2 3z"
        />
      ))}
    </svg>
  )
}
