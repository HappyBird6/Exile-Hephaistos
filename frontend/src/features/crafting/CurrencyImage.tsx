import { useState } from 'react'
import { useI18n } from '../../shared/i18n/i18n'
export function CurrencyImage({
  image,
  name,
}: {
  image: string
  name: string
}) {
  const { t } = useI18n()
  const [failed, setFailed] = useState(false)
  return failed ? (
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
