import { useState } from 'react'
export function CurrencyImage({
  image,
  name,
}: {
  image: string
  name: string
}) {
  const [failed, setFailed] = useState(false)
  return failed ? (
    <span className="currency-missing" title={`${name} · Image unavailable`}>
      Image unavailable
    </span>
  ) : (
    <img src={image} alt="" draggable="false" onError={() => setFailed(true)} />
  )
}
