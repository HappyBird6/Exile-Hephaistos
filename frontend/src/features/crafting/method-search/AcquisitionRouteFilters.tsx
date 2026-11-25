import './method-search.css'
import { useI18n } from '../../../shared/i18n/i18n'
import { supportProbabilityText } from '../supportProbability'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'

type Props = {
  lower: number
  cap: number
  lucky: boolean
  lowerStops: number[]
  costStops: number[]
  setLower: (value: number) => void
  setCap: (value: number) => void
  setLucky: (checked: boolean) => void
}

export function AcquisitionRouteFilters({
  lower,
  cap,
  lucky,
  lowerStops,
  costStops,
  setLower,
  setCap,
  setLucky,
}: Props) {
  const { locale } = useI18n()
  const text = acquisitionRouteMessages[locale]
  const chance = supportProbabilityText({ probability: lower }, locale)
  const price = Number.isFinite(cap)
    ? new Intl.NumberFormat(locale, { maximumSignificantDigits: 6 }).format(
        cap,
      ) + ' ex'
    : text.unlimited
  return (
    <div className="acquisition-route-filters">
      <label>
        <span>
          {text.minimumChance} · {chance}
        </span>
        <input
          type="range"
          min={0}
          max={lowerStops.length - 1}
          step={1}
          value={lowerStops.indexOf(lower)}
          aria-label={text.minimumChance}
          aria-valuetext={chance}
          onChange={(event) =>
            setLower(lowerStops[Number(event.target.value)]!)
          }
        />
      </label>
      <label>
        <span>
          {text.maximumPrice} · {price}
        </span>
        <input
          type="range"
          min={0}
          max={costStops.length - 1}
          step={1}
          value={costStops.indexOf(cap)}
          aria-label={text.maximumPrice}
          aria-valuetext={price}
          onChange={(event) => setCap(costStops[Number(event.target.value)]!)}
        />
      </label>
      <label>
        <input
          type="checkbox"
          checked={lucky}
          onChange={(event) => setLucky(event.target.checked)}
        />
        {text.showLucky}
      </label>
    </div>
  )
}
