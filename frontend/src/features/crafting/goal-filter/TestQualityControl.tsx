import { catalystItemIds } from '../localizedCrafting'
import type { CatalystQuality } from '../catalystQuality'
import type { Locale } from '../../../shared/i18n/i18n'
import { catalystOptionLabel, testGoalMessages } from './testGoalMessages'
import type { TestQualityEditor } from './testRouteGoal'

export function TestQualityControl({
  quality,
  allowed,
  locale,
}: {
  quality: TestQualityEditor
  allowed: boolean
  locale: Locale
}) {
  const copy = testGoalMessages(locale)
  const enabled = allowed && quality.choice !== null
  return (
    <div className="test-quality-control" data-enabled={enabled}>
      {enabled && (
        <select
          aria-label={copy.catalyst}
          value={quality.choice ?? ''}
          onChange={(event) => quality.change(event.target.value)}
        >
          <option value="">{copy.anyCatalyst}</option>
          {(Object.keys(catalystItemIds) as CatalystQuality['type'][]).map(
            (type) => (
              <option key={type} value={catalystItemIds[type]}>
                {catalystOptionLabel(type, locale)}
              </option>
            ),
          )}
        </select>
      )}
      <button
        type="button"
        disabled={!allowed}
        aria-pressed={enabled}
        onClick={() => quality.change(enabled ? null : '')}
      >
        {copy.quality40}
      </button>
    </div>
  )
}
