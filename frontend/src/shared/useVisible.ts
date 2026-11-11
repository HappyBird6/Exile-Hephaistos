import { useEffect, useRef, useState } from 'react'

/** Pause background reference work outside the viewport; resume with the latest state on return. */
export function useVisible<T extends HTMLElement>() {
  const ref = useRef<T>(null)
  const [visible, setVisible] = useState(
    typeof IntersectionObserver === 'undefined',
  )
  useEffect(() => {
    if (!ref.current || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      ([entry]) => setVisible(!!entry?.isIntersecting),
      { rootMargin: '200px' },
    )
    observer.observe(ref.current)
    return () => observer.disconnect()
  }, [])
  return { ref, visible }
}
