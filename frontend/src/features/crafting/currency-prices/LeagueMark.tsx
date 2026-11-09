import { useState } from 'react'
import { useCurrencyMarket } from './context'

const artwork: Record<
  string,
  { image: string; announcement: string; viewBox: string }
> = {
  'Forbidden Rites': {
    image: '/assets/leagues/forbidden-rites-official.png',
    announcement:
      'https://store.steampowered.com/news/app/2694490/view/1842212951308718',
    viewBox: '730 145 440 100',
  },
}
/** Show vetted official artwork only when it matches the current price league. */
export function LeagueMark() {
  const market = useCurrencyMarket()
  const league = market?.data?.snapshot?.league
  const [failed, setFailed] = useState<string | null>(null)
  const source = league ? artwork[league] : undefined
  if (!league || !source || failed === source.image) return null
  return (
    <a
      className="header-league-mark"
      href={source.announcement}
      target="_blank"
      rel="noreferrer"
      aria-label={`${league} · Grinding Gear Games`}
      title={`${league} · Grinding Gear Games`}
    >
      <svg viewBox={source.viewBox} role="img" aria-label={league}>
        <image
          href={source.image}
          width="1920"
          height="1080"
          onError={() => setFailed(source.image)}
        />
      </svg>
    </a>
  )
}
