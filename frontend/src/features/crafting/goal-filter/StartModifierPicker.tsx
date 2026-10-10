import { useEffect, useId, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Definition } from '../craftingApi'
import { localizedModifierText } from '../localizedModifiers'
import { groupStartModifiers } from './modifierGroups'
import { modifierPickerMessages } from './modifierPickerMessages'
import { PickerPopover } from './PickerPopover'
import { PickerSearchInput } from './PickerSearchInput'
import { craftStartMessages } from './craftStartMessages'

export function StartModifierPicker({
  definitions,
  onAdd,
  placeholder,
  disabled = false,
}: {
  definitions: Definition[]
  onAdd: (id: string) => void
  placeholder?: string
  disabled?: boolean
}) {
  const { locale } = useI18n()
  const t = modifierPickerMessages[locale]
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const list = useRef<HTMLUListElement>(null)
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)
  const groups = groupStartModifiers(definitions).map((group) => ({
    ...group,
    label: `${localizedModifierText(group.tiers[0]!, undefined, locale)} · ${group.tiers[0]!.affixType === 'PREFIX' ? t.prefix : t.suffix}`,
  }))
  const needle = query.normalize('NFC').toLocaleLowerCase(locale).trim()
  const visible = groups.filter((group) =>
    [group.label, ...group.tiers.flatMap((m) => [m.name, m.text])].some(
      (text) =>
        text.normalize('NFC').toLocaleLowerCase(locale).includes(needle),
    ),
  )
  const activeIndex = Math.min(active, Math.max(0, visible.length - 1))
  useEffect(() => {
    if (open)
      list.current?.children[activeIndex]?.scrollIntoView?.({
        block: 'nearest',
      })
  }, [open, activeIndex, query])
  function choose(index: number) {
    const group = visible[index]
    if (!group) return
    if (disabled) return
    onAdd(group.tiers[0]!.id)
    input.current?.focus()
    setOpen(false)
    setQuery('')
  }
  return (
    <div className="start-modifier-picker">
      <PickerSearchInput
        label={craftStartMessages[locale].add}
        id={`${id}-input`}
        inputRef={input}
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={open ? `${id}-list` : undefined}
        aria-activedescendant={
          open && visible.length ? `${id}-option-${activeIndex}` : undefined
        }
        placeholder={placeholder ?? `+ ${craftStartMessages[locale].add}`}
        disabled={disabled}
        value={query}
        onClick={() => {
          if (open) return
          setOpen(true)
          setQuery('')
          setActive(0)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onChange={(event) => {
          setQuery(event.target.value)
          setOpen(true)
          setActive(0)
        }}
        onKeyDown={(event) => {
          if (event.nativeEvent.isComposing || event.keyCode === 229) return
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault()
            if (!open) {
              setOpen(true)
              setQuery('')
              setActive(event.key === 'ArrowUp' ? groups.length - 1 : 0)
            } else
              setActive(
                (activeIndex +
                  (event.key === 'ArrowDown' ? 1 : -1) +
                  visible.length) %
                  Math.max(1, visible.length),
              )
          } else if (event.key === 'Enter') {
            event.preventDefault()
            if (open) choose(activeIndex)
            else setOpen(true)
          } else if (event.key === 'Escape') {
            event.preventDefault()
            event.stopPropagation()
            setOpen(false)
            setQuery('')
          }
        }}
      />
      {open && (
        <PickerPopover anchor={input}>
          <ul id={`${id}-list`} ref={list} role="listbox" aria-label={t.group}>
            {visible.map((group, index) => (
              <li
                id={`${id}-option-${index}`}
                key={group.id}
                role="option"
                aria-selected={index === activeIndex}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => choose(index)}
              >
                {group.label}
              </li>
            ))}
          </ul>
          {!visible.length && (
            <p role="status">{groups.length ? t.noMatch : t.empty}</p>
          )}
        </PickerPopover>
      )}
    </div>
  )
}
