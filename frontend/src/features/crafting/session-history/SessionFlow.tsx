import {
  sessionNotationMessages,
  workLabel,
} from '../session-notation/messages'
import type { ReactNode } from 'react'
import { CraftingCostLine } from '../currency-prices/CurrencyPrices'
import { sessionTitle } from '../session-library/sessionTitle'
import { abyssWorkbenchMessages } from '../abyss-workbench/messages'
import { Fragment, useLayoutEffect, useRef, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Film } from '../workbenchHistory'
import type { WorkbenchResource } from '../stash/stashTypes'
import { extractSessionNotation } from '../session-notation/extractSessionNotation'
import { NotationResources } from '../session-notation/SessionNotation'
import { HistoryCard } from './HistoryCard'
import { historyMessages } from './messages'

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
  columnCount,
}: {
  film: Film
  resources: readonly WorkbenchResource[]
  columnCount?: 1 | 2 | 4
}) {
  const { locale, name } = useI18n()
  const text = historyMessages[locale]
  let steps: ReturnType<typeof extractSessionNotation>['steps'] = []
  try {
    steps = extractSessionNotation(film, film.frames.length - 1).steps
  } catch {
    /* Preserve every state even when its receipt cannot be read. */
  }
  const byFrame = new Map<number, typeof steps>()
  for (const step of steps) {
    const group = byFrame.get(step.frameIndex)
    if (group) group.push(step)
    else byFrame.set(step.frameIndex, [step])
  }
  const frames = film.frames.map((frame, index) => {
    const group = byFrame.get(index) ?? []
    const labels = group.map((step) => {
      if (step.work) return workLabel(step.work, locale)
      const currency =
        step.kind === 'reveal'
          ? `${abyssWorkbenchMessages[locale].desecration} ${abyssWorkbenchMessages[locale].reveal}`
          : name(
              step.currencyId,
              resources.find((r) => r.id === step.currencyId)?.name ??
                step.currencyId,
            )
      return (
        currency +
        (step.expectedUses
          ? ' ' +
            sessionNotationMessages[locale].expectedUses(
              step.expectedUses.toLocaleString(locale, {
                maximumFractionDigits: 2,
              }),
            )
          : step.maxUses
            ? ` ×1–${step.maxUses}`
            : step.repeated
              ? ` × ${step.uses}`
              : '')
      )
    })
    const label = labels.join(' → ') || text.observation
    return {
      label,
      card: <HistoryCard film={film} frame={frame} />,
      resources: (
        <div
          className="session-flow-resources"
          title={
            group
              .flatMap((step) => step.consumedOmenIds)
              .map((id) =>
                name(id, resources.find((r) => r.id === id)?.name ?? id),
              )
              .join(' + ') || undefined
          }
        >
          {group
            .filter((step) => !step.work)
            .map((step) => (
              <span key={step.number}>
                <NotationResources step={step} resources={resources} />
              </span>
            ))}
        </div>
      ),
    }
  })
  return (
    <SessionFlowGrid
      frames={frames}
      columnCount={columnCount}
      summary={
        <>
          <strong>{sessionTitle(film, locale)}</strong>
          <CraftingCostLine film={film} cursor={film.frames.length - 1} />
        </>
      }
    />
  )
}

export function SessionFlowGrid({
  frames,
  summary,
  columnCount,
}: {
  frames: { card: ReactNode; label: string; resources?: ReactNode }[]
  summary: ReactNode
  columnCount?: 1 | 2 | 4 | undefined
}) {
  const grid = useRef<HTMLOListElement>(null)
  const [width, setWidth] = useState(window.innerWidth)
  useLayoutEffect(() => {
    const element = grid.current
    if (!element) return
    const resize = () =>
      setWidth(element.getBoundingClientRect().width || window.innerWidth)
    resize()
    const observer =
      typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(resize)
    observer?.observe(element)
    window.addEventListener('resize', resize)
    return () => {
      observer?.disconnect()
      window.removeEventListener('resize', resize)
    }
  }, [])
  const columns = columnCount ?? (width <= 600 ? 1 : width <= 1100 ? 2 : 4)
  return (
    <ol
      ref={grid}
      className="session-flow-grid"
      style={{
        gridTemplateColumns: Array.from({ length: columns * 2 - 1 }, (_, i) =>
          i % 2 ? '112px' : 'minmax(0,1fr)',
        ).join(' '),
      }}
    >
      <li
        className={`session-flow-summary${columns === 1 ? ' session-flow-summary--one-column' : ''}${frames.length <= columns ? ' session-flow-summary--single-row' : ''}`}
        aria-hidden="true"
        style={{
          gridRow: 2,
          gridColumn: 1,
        }}
      >
        <div className="session-flow-summary__content">{summary}</div>
      </li>
      {frames.map((frame, index) => {
        const position = flowPosition(index, columns)
        const previous = index ? flowPosition(index - 1, columns) : position
        const down = previous.row !== position.row
        const reverse = position.column < previous.column
        return (
          <Fragment key={index}>
            {index > 0 && (
              <li
                className={`session-flow-edge${down ? ' session-flow-edge--down' : ''}`}
                data-flow-edge
                data-from={index - 1}
                data-to={index}
                aria-label={frame.label}
                style={{
                  gridRow: down ? position.row - 1 : position.row,
                  gridColumn: down
                    ? position.column
                    : Math.min(previous.column, position.column) + 1,
                }}
              >
                {frame.resources}
                <svg
                  className="session-flow-arrow"
                  aria-hidden="true"
                  viewBox={down ? '0 0 20 40' : '0 0 100 20'}
                  preserveAspectRatio="none"
                >
                  <path
                    d={
                      down
                        ? 'M10 0 V34 M4 26 L10 36 L16 26'
                        : reverse
                          ? 'M100 10 H6 M14 4 L4 10 L14 16'
                          : 'M0 10 H94 M86 4 L96 10 L86 16'
                    }
                  />
                </svg>
                <span className="session-flow-label">{frame.label}</span>
              </li>
            )}
            <li
              className="session-flow-state"
              data-flow-state
              data-index={index}
              data-flow-row={position.row}
              style={{ gridRow: position.row, gridColumn: position.column }}
            >
              {frame.card}
            </li>
          </Fragment>
        )
      })}
    </ol>
  )
}
