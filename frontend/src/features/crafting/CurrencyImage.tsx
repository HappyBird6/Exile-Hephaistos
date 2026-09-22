import { useState } from 'react'
const unavailable = new Set(['Greater_Jewellers_Orb', 'Hinekoras_Lock'])
export function CurrencyImage({
  id,
  image,
  name,
}: {
  id: string
  image: string
  name: string
}) {
  const [failed, setFailed] = useState(false)
  return unavailable.has(id) || failed ? (
    <span className="currency-missing" title={`${name} · 이미지 미확보`}>
      이미지 미확보
    </span>
  ) : (
    <img src={image} alt="" draggable="false" onError={() => setFailed(true)} />
  )
}
