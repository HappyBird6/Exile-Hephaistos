import type { AnchorHTMLAttributes } from 'react'
import { navigate } from './navigation'
export function AppLink({
  href = '/',
  onClick,
  ...props
}: AnchorHTMLAttributes<HTMLAnchorElement>) {
  return (
    <a
      {...props}
      href={href}
      onClick={(event) => {
        onClick?.(event)
        if (
          event.defaultPrevented ||
          event.button !== 0 ||
          event.metaKey ||
          event.ctrlKey ||
          event.shiftKey ||
          event.altKey ||
          props.download ||
          (props.target && props.target !== '_self')
        )
          return
        if (new URL(href, location.href).origin !== location.origin) return
        event.preventDefault()
        navigate(href)
      }}
    />
  )
}
