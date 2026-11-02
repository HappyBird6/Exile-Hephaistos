import { useState } from 'react'
import { useI18n } from '../../shared/i18n/i18n'
export function CurrencyImage({
  id,
  image,
  name,
}: {
  id?: string
  image: string
  name: string
}) {
  const { t, name: localizedName } = useI18n()
  const [failed, setFailed] = useState(false)
  return !image ? (
    <span className="currency-name-fallback">
      {id ? localizedName(id, name) : name}
    </span>
  ) : failed ? (
    <span
      className="currency-missing"
      title={`${name} · ${t('ui.image_unavailable')}`}
    >
      {t('ui.image_unavailable')}
    </span>
  ) : (
    <img src={image} alt="" draggable="false" onError={() => setFailed(true)} />
  )
}
