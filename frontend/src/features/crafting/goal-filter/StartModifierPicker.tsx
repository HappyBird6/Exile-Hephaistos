import { useEffect, useId, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Definition } from '../craftingApi'
import { localizedModifierText } from '../localizedModifiers'
import { groupStartModifiers } from './modifierGroups'
import { modifierPickerMessages } from './modifierPickerMessages'

export function StartModifierPicker({
  definitions,
  onAdd,
}: {
  definitions: Definition[]
  onAdd: (id: string) => void
}) {
  const { locale } = useI18n()
  const t = modifierPickerMessages[locale]
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const tier = useRef<HTMLSelectElement>(null)
  const list = useRef<HTMLUListElement>(null)
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState('')
  const [active, setActive] = useState(0)
  const groups = groupStartModifiers(definitions).map((group) => ({
    ...group,
    label: `${localizedModifierText(group.tiers[0]!, undefined, locale)} · ${group.tiers[0]!.affixType === 'PREFIX' ? t.prefix : t.suffix}`,
  }))
  const selected = groups.find((group) => group.id === selectedId)
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
    setSelectedId(group.id)
    setOpen(false)
    setQuery('')
    queueMicrotask(() => tier.current?.focus())
  }
  return (
    <div className="start-modifier-picker">
      <label htmlFor={`${id}-input`}>{t.group}</label>
      <input
        id={`${id}-input`}
        ref={input}
        role="combobox"
        autoComplete="off"
        aria-autocomplete="list"
        aria-expanded={open}
        aria-controls={open ? `${id}-list` : undefined}
        aria-activedescendant={
          open && visible.length ? `${id}-option-${activeIndex}` : undefined
        }
        placeholder={t.search}
        disabled={!groups.length}
        value={open ? query : (selected?.label ?? '')}
        onClick={() => {
          if (open) return
          setOpen(true)
          setQuery('')
          setActive(0)
        }}
        onFocus={(event) => event.currentTarget.select()}
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
          } else if (event.key === 'Enter' && open) {
            event.preventDefault()
            choose(activeIndex)
          } else if (event.key === 'Escape') {
            event.preventDefault()
            event.stopPropagation()
            setOpen(false)
            setQuery('')
          }
        }}
      />
      {open && (
        <>
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
          {!visible.length && <p role="status">{t.noMatch}</p>}
        </>
      )}
      <label htmlFor={`${id}-tier`}>{t.tier}</label>
      <select
        id={`${id}-tier`}
        ref={tier}
        value=""
        disabled={!selected}
        onChange={(event) => {
          if (!selected?.tiers.some((m) => m.id === event.target.value)) return
          onAdd(event.target.value)
          setSelectedId('')
          setQuery('')
          setOpen(false)
          input.current?.focus()
        }}
      >
        <option value="">{t.chooseTier}</option>
        {selected?.tiers.map((m) => (
          <option key={m.id} value={m.id}>
            T{m.tier} · {localizedModifierText(m, undefined, locale)}
          </option>
        ))}
      </select>
      {!groups.length && <p role="status">{t.empty}</p>}
    </div>
  )
}
