import { useState } from 'react'
import { useI18n } from '../../shared/i18n/context'
export function CurrencyImage({
  image,
  name,
}: {
  image: string
  name: string
}) {
  const [failed, setFailed] = useState(false)
  const { t } = useI18n()
  return failed ? (
    <span className="currency-missing" title={`${name} · ${t('missingImage')}`}>
      {t('missingImage')}
    </span>
  ) : (
    <img src={image} alt="" draggable="false" onError={() => setFailed(true)} />
  )
}
