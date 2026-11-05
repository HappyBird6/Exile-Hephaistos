import { Fragment, useSyncExternalStore } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Film } from '../workbenchHistory'
import type { WorkbenchResource } from '../stash/stashTypes'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import { NotationResources } from '../session-notation/SessionNotation'
import { HistoryCard } from './HistoryCard'
import { historyMessages } from './messages'

function subscribe(listener: () => void) {
  window.addEventListener('resize', listener)
  return () => window.removeEventListener('resize', listener)
}
function flowPosition(index: number, columns: number) {
  const row = Math.floor(index / columns)
  return {
    row: row * 2 + 1,
    column:
      (row % 2 ? columns - (index % columns) - 1 : index % columns) * 2 + 1,
  }
}
export function SessionFlow({
  film,
  resources,
}: {
  film: Film
  resources: readonly WorkbenchResource[]
}) {
  const { locale, name } = useI18n()
  const text = historyMessages[locale]
  const columns = useSyncExternalStore(subscribe, () =>
    window.innerWidth <= 640 ? 1 : window.innerWidth <= 1100 ? 2 : 4,
  )
  let steps: ReturnType<typeof extractSessionNotation>['steps'] = []
  try {
    steps = extractSessionNotation(film, film.frames.length - 1).steps
  } catch {
    /* Preserve every state even when its receipt cannot be read. */
  }
  return (
    <ol
      className="session-flow-grid"
      style={{
        gridTemplateColumns: Array.from({ length: columns * 2 - 1 }, (_, i) =>
          i % 2 ? '112px' : 'minmax(0,1fr)',
        ).join(' '),
      }}
    >
      {film.frames.map((frame, index) => {
        const position = flowPosition(index, columns)
        const previous = index ? flowPosition(index - 1, columns) : position
        const down = previous.row !== position.row
        const reverse = position.column < previous.column
        const step = steps.find((s) => s.frameIndex === index)
        const currency = step
          ? name(
              step.currencyId,
              resources.find((r) => r.id === step.currencyId)?.name ??
                step.currencyId,
            )
          : text.observation
        const label = step
          ? `${currency}${step.repeated ? ` × ${step.uses}` : ''}`
          : text.observation
        return (
          <Fragment key={index}>
            {index > 0 && (
              <li
                className={`session-flow-edge${down ? ' session-flow-edge--down' : ''}`}
                data-flow-edge
                data-from={index - 1}
                data-to={index}
                aria-label={label}
                style={{
                  gridRow: down ? position.row - 1 : position.row,
                  gridColumn: down
                    ? position.column
                    : Math.min(previous.column, position.column) + 1,
                }}
              >
                <svg
                  className="session-flow-arrow"
                  aria-hidden="true"
                  viewBox={down ? '0 0 20 100' : '0 0 100 20'}
                  preserveAspectRatio="none"
                >
                  <path
                    d={
                      down
                        ? 'M10 0 V94 M4 86 L10 96 L16 86'
                        : reverse
                          ? 'M100 10 H6 M14 4 L4 10 L14 16'
                          : 'M0 10 H94 M86 4 L96 10 L86 16'
                    }
                  />
                </svg>
                <div
                  className="session-flow-label"
                  title={
                    step?.consumedOmenIds
                      .map((id) =>
                        name(
                          id,
                          resources.find((r) => r.id === id)?.name ?? id,
                        ),
                      )
                      .join(' + ') || undefined
                  }
                >
                  {step && (
                    <NotationResources step={step} resources={resources} />
                  )}
                  <span>{label}</span>
                </div>
              </li>
            )}
            <li
              className="session-flow-state"
              data-flow-state
              data-index={index}
              data-flow-row={position.row}
              style={{ gridRow: position.row, gridColumn: position.column }}
            >
              <HistoryCard film={film} frame={frame} />
            </li>
          </Fragment>
        )
      })}
    </ol>
  )
}
