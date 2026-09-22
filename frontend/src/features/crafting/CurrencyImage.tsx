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
    <span className="currency-missing" title={`${name} · 이미지 미확보`}>
      이미지 미확보
    </span>
  ) : (
    <img src={image} alt="" draggable="false" onError={() => setFailed(true)} />
  )
}
