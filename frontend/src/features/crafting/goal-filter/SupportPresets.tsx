import { useCallback, useEffect, useState } from 'react'
import { accountScope, useAccount } from '../../account/auth'
import type { Locale } from '../../../shared/i18n/i18n'
import {
  UnavailablePresetRows,
  listPresets,
  loadPreset,
  savePreset,
  type PresetStart,
  type SupportPreset,
} from './supportPresetStorage'
import { presetMessages } from './presetMessages'
import type { GoalFilter } from './types'

type Props = {
  locale: Locale
  start: PresetStart | null
  goal: GoalFilter
  onLoad: (preset: SupportPreset) => Promise<void>
}

export function SupportPresets(props: Props) {
  const auth = useAccount()
  const owner = auth?.user?.id ?? null
  return (
    <PresetControls
      key={owner === null ? 'guest' : 'account:' + owner}
      {...props}
      owner={owner}
      identityReady={auth !== null}
    />
  )
}

function PresetControls({
  owner,
  locale,
  identityReady,
  start,
  goal,
  onLoad,
}: Props & { owner: string | null; identityReady: boolean }) {
  const copy = presetMessages[locale]
  const [name, setName] = useState('')
  const [selected, choose] = useState('')
  const [notice, setNotice] = useState<
    'saved' | 'loaded' | 'duplicate' | 'error' | null
  >(null)
  const [rows, setRows] = useState<string[]>([])
  const [loading, setLoading] = useState(false)
  const read = useCallback(() => {
    try {
      return listPresets(owner)
    } catch {
      return null
    }
  }, [owner])
  const [entries, setEntries] = useState(read)
  useEffect(() => {
    const refresh = () => setEntries(read())
    window.addEventListener('storage', refresh)
    return () => window.removeEventListener('storage', refresh)
  }, [read])
  const options = (guest: boolean) =>
    entries
      ?.filter((entry) => entry.guest === guest)
      .map((entry) => (
        <option key={entry.key} value={entry.key}>
          {entry.name}
        </option>
      ))
  return (
    <fieldset className="support-presets" disabled={!identityReady}>
      <legend>{copy.title}</legend>
      <div>
        <input
          aria-label={copy.name}
          placeholder={copy.name}
          value={name}
          onChange={(event) => {
            setName(event.target.value)
            setNotice(null)
          }}
        />
        <button
          type="button"
          disabled={!start || !name.trim() || !entries || loading}
          onClick={() => {
            if (!start || owner !== accountScope()) return
            setRows([])
            try {
              const key = savePreset(owner, name, { start, goal })
              if (!key) {
                setNotice('duplicate')
                return
              }
              setEntries(read())
              choose(key)
              setName('')
              setNotice('saved')
            } catch {
              setNotice('error')
            }
          }}
        >
          {copy.save}
        </button>
      </div>
      <div>
        <select
          aria-label={copy.choose}
          value={selected}
          onChange={(e) => {
            choose(e.target.value)
            setNotice(null)
          }}
        >
          <option value="">{copy.choose}</option>
          {owner !== null ? (
            <>
              <optgroup label={copy.account}>{options(false)}</optgroup>
              <optgroup label={copy.guest}>{options(true)}</optgroup>
            </>
          ) : (
            options(true)
          )}
        </select>
        <button
          type="button"
          disabled={
            loading || !entries?.some((entry) => entry.key === selected)
          }
          onClick={async () => {
            if (owner !== accountScope()) return
            setLoading(true)
            try {
              await onLoad(loadPreset(owner, selected))
              setNotice('loaded')
            } catch (error) {
              setRows(error instanceof UnavailablePresetRows ? error.rows : [])
              setNotice('error')
            } finally {
              setLoading(false)
            }
          }}
        >
          {copy.load}
        </button>
      </div>
      {(!entries || notice) && (
        <p
          role={
            !entries || notice === 'error' || notice === 'duplicate'
              ? 'alert'
              : 'status'
          }
        >
          {notice === 'error' && rows.length
            ? copy.incompatible + ' ' + rows.join(', ')
            : copy[entries && notice ? notice : 'error']}
        </p>
      )}
    </fieldset>
  )
}
