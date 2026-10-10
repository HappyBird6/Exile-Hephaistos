import type { InputHTMLAttributes, RefObject } from 'react'
import './picker.css'

/** Shared visual and accessibility contract; each picker retains its own data. */
export function PickerSearchInput({
  label,
  inputRef,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & {
  label: string
  inputRef: RefObject<HTMLInputElement | null>
}) {
  return (
    <label className="craft-picker-label">
      <span>{label}</span>
      <input
        type="search"
        {...props}
        ref={inputRef}
        className="craft-picker-input"
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
      />
    </label>
  )
}
