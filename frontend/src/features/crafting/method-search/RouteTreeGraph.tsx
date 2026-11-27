import {
  memo,
  useCallback,
  useId,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
  type ReactNode,
  type SetStateAction,
} from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type {
  AcquisitionRouteTree,
  RouteTreeNode,
} from './acquisitionRouteTree'
import { graphNavigationMessages } from './graphNavigationMessages'
import { graphStateMessages } from './graphStateMessages'
import { MeasuredGraphBox } from './MeasuredGraphBox'
import {
  layoutRouteTree,
  routeCardWidth,
  type RouteTreeBox,
} from './routeTreeLayout'
import { routeTreeMessages } from './routeTreeMessages'
import { useGraphPan } from './useGraphPan'
import { useGraphWheelZoom } from './useGraphWheelZoom'
import './route-tree-graph.css'

type Content = {
  renderItem: (node: RouteTreeNode) => ReactNode
  chanceText: (node: RouteTreeNode) => string
  costText: (node: RouteTreeNode) => string
  actionText: (node: RouteTreeNode) => string
}

type Props = Content & {
  tree: AcquisitionRouteTree
  selectedId: number
  onSelect: (id: number) => void
  renderDetail: (node: RouteTreeNode | null) => ReactNode
}

const StateCard = memo(function StateCard({
  box,
  selected,
  onSelect,
  onMeasure,
  renderItem,
  chanceText,
  costText,
  actionText,
}: Content & {
  box: RouteTreeBox
  selected: boolean
  onSelect: (id: number) => void
  onMeasure: (id: number, height: number) => void
}) {
  const { locale } = useI18n()
  const copy = routeTreeMessages[locale]
  const { node } = box
  const measure = useCallback(
    (height: number) => onMeasure(node.id, height),
    [onMeasure, node.id],
  )
  const action = actionText(node)
  return (
    <foreignObject
      x={box.x}
      y={box.y}
      width={routeCardWidth}
      height={box.height}
      className="route-tree__box"
    >
      <MeasuredGraphBox onSize={measure}>
        <article
          className="route-tree__card"
          data-selected={selected}
          onClick={() => onSelect(node.id)}
        >
          <button
            type="button"
            className="route-tree__action"
            aria-label={graphStateMessages[locale].select + ' · ' + action}
            data-route-tree-id={node.id}
            aria-pressed={selected}
            tabIndex={selected ? 0 : -1}
          >
            <span>{action}</span>
            {node.terminals.length > 0 && (
              <span
                className="route-tree__terminal"
                aria-label={copy.complete}
                title={copy.complete}
              >
                ✓
              </span>
            )}
          </button>
          {renderItem(node)}
          <dl className="route-tree__metrics">
            <div>
              <dt>{copy.chance}</dt>
              <dd>{chanceText(node)}</dd>
            </div>
            <div>
              <dt>{copy.cost}</dt>
              <dd>{costText(node)}</dd>
            </div>
          </dl>
        </article>
      </MeasuredGraphBox>
    </foreignObject>
  )
})

const initialViewport = { left: 0, top: 0, width: 760, height: 520 }

export function RouteTreeGraph({
  tree,
  selectedId,
  onSelect,
  renderDetail,
  ...content
}: Props) {
  const { locale } = useI18n()
  const copy = routeTreeMessages[locale],
    nav = graphNavigationMessages[locale]
  const host = useRef<HTMLDivElement>(null)
  const [viewport, setViewport] = useState(initialViewport)
  const [zoomMode, setZoomMode] = useState<number | 'fit'>(1)
  const [sizes, setSizes] = useState(new Map<number, number>())
  const layout = useMemo(() => layoutRouteTree(tree, sizes), [tree, sizes])
  const selected = layout.byId.get(selectedId)
  const fitZoom = Math.min(
    1,
    viewport.width / layout.width,
    viewport.height / layout.height,
  )
  const fitting = zoomMode === 'fit'
  const zoom = fitting ? fitZoom : zoomMode
  const minZoom = Math.min(0.1, fitZoom)
  const pan = useGraphPan()
  const { panning, ...panHandlers } = pan
  const wheelZoom = useCallback(
    (value: SetStateAction<number>) =>
      setZoomMode((previous) =>
        typeof value === 'number'
          ? value
          : value(previous === 'fit' ? fitZoom : previous),
      ),
    [fitZoom],
  )
  useGraphWheelZoom(host, zoom, wheelZoom, minZoom)
  const marker = useId()
  const readViewport = useCallback(() => {
    const el = host.current
    if (!el) return
    const next = {
      left: el.scrollLeft,
      top: el.scrollTop,
      width: el.clientWidth || initialViewport.width,
      height: el.clientHeight || initialViewport.height,
    }
    setViewport((prev) =>
      prev.left === next.left &&
      prev.top === next.top &&
      prev.width === next.width &&
      prev.height === next.height
        ? prev
        : next,
    )
  }, [])
  useLayoutEffect(() => {
    const el = host.current
    if (!el) return
    readViewport()
    let task = 0
    const onScroll = () => {
      if (!task)
        task = requestAnimationFrame(() => {
          task = 0
          readViewport()
        })
    }
    const observer =
      typeof ResizeObserver === 'undefined'
        ? null
        : new ResizeObserver(readViewport)
    observer?.observe(el)
    el.addEventListener('scroll', onScroll, { passive: true })
    return () => {
      observer?.disconnect()
      el.removeEventListener('scroll', onScroll)
      cancelAnimationFrame(task)
    }
  }, [readViewport])

  const onMeasure = useCallback((id: number, height: number) => {
    setSizes((previous) => {
      if (previous.get(id) === height) return previous
      return new Map(previous).set(id, height)
    })
  }, [])
  const adjustZoom = useRef<{ x: number; y: number } | null>(null)
  const changeZoom = (
    next: number,
    point?: { x: number; y: number },
    fit = false,
  ) => {
    const el = host.current
    if (!el) return
    adjustZoom.current = point ?? {
      x: Math.min(
        layout.width / 2 + el.scrollLeft / zoom,
        (el.scrollLeft + viewport.width / 2) / zoom,
      ),
      y: (el.scrollTop + viewport.height / 2) / zoom,
    }
    setZoomMode(fit ? 'fit' : Math.max(minZoom, Math.min(next, 2)))
  }
  useLayoutEffect(() => {
    const point = adjustZoom.current,
      el = host.current
    if (!point || !el) return
    adjustZoom.current = null
    el.scrollLeft = Math.max(0, point.x * zoom - viewport.width / 2)
    el.scrollTop = Math.max(0, point.y * zoom - viewport.height / 2)
    readViewport()
  }, [zoom, viewport.width, viewport.height, readViewport])

  const lastSelection = useRef<{
    tree: AcquisitionRouteTree
    id: number
  } | null>(null)
  const keyboardTarget = useRef<number | null>(null)
  useLayoutEffect(() => {
    const el = host.current
    const anchor = selected ?? layout.byId.get(0)
    if (!el || !anchor) return
    if (
      lastSelection.current?.tree !== tree ||
      lastSelection.current.id !== selectedId
    ) {
      lastSelection.current = { tree, id: selectedId }
      const x = anchor.x * zoom,
        y = anchor.y * zoom
      if (
        x < el.scrollLeft ||
        x + routeCardWidth * zoom > el.scrollLeft + viewport.width
      ) {
        el.scrollLeft = Math.max(
          0,
          x + (routeCardWidth * zoom - viewport.width) / 2,
        )
      }
      if (
        y < el.scrollTop ||
        y + anchor.height * zoom > el.scrollTop + viewport.height
      ) {
        el.scrollTop = Math.max(0, y - 16)
      }
      readViewport()
    }
    if (selected && keyboardTarget.current === selected.node.id) {
      const button = el.querySelector<HTMLButtonElement>(
        `[data-route-tree-id="${selected.node.id}"]`,
      )
      if (button) {
        keyboardTarget.current = null
        button.focus({ preventScroll: true })
      }
    }
  }, [selected, selectedId, layout, tree, zoom, viewport, readViewport])
  const navigate = (event: KeyboardEvent<HTMLDivElement>) => {
    if (
      event.altKey ||
      event.metaKey ||
      event.ctrlKey ||
      (event.target !== host.current &&
        !(event.target as Element).closest('[data-route-tree-id]'))
    )
      return
    if (!selected) {
      if (event.key === 'Home' || event.key === 'ArrowDown') {
        event.preventDefault()
        keyboardTarget.current = 0
        onSelect(0)
      }
      return
    }
    const node = selected.node
    const siblings =
      node.parentId == null ? [node.id] : tree.nodes[node.parentId]!.children
    const index = siblings.indexOf(node.id)
    const target =
      event.key === 'ArrowDown'
        ? node.children[0]
        : event.key === 'ArrowUp'
          ? node.parentId
          : event.key === 'ArrowLeft'
            ? siblings[index - 1]
            : event.key === 'ArrowRight'
              ? siblings[index + 1]
              : event.key === 'Home'
                ? 0
                : undefined
    if (target == null) return
    event.preventDefault()
    keyboardTarget.current = target
    onSelect(target)
  }

  const world = {
    left: viewport.left / zoom - 80,
    right: (viewport.left + viewport.width) / zoom + 80,
    top: viewport.top / zoom - 80,
    bottom: (viewport.top + viewport.height) / zoom + 80,
  }
  const visible: RouteTreeBox[] = []
  const lines: string[] = []
  for (const row of layout.rows) {
    if (!row.length || row[0]!.y > world.bottom) break
    for (const box of row) {
      const { x, y, height, node } = box
      if (
        x <= world.right &&
        x + routeCardWidth >= world.left &&
        y + height >= world.top
      )
        visible.push(box)
      if (!node.children.length) continue
      const first = layout.byId.get(node.children[0]!)!,
        last = layout.byId.get(node.children[node.children.length - 1]!)!
      if (
        first.y < world.top ||
        y + height > world.bottom ||
        last.x + routeCardWidth / 2 < world.left ||
        first.x + routeCardWidth / 2 > world.right
      )
        continue
      const mid = first.y - 32
      const center = x + routeCardWidth / 2
      if (center >= world.left && center <= world.right)
        lines.push(`M${center} ${y + height}V${mid}`)
      lines.push(
        `M${first.x + routeCardWidth / 2} ${mid}H${last.x + routeCardWidth / 2}`,
      )
      for (const child of node.children) {
        const target = layout.byId.get(child)!,
          to = target.x + routeCardWidth / 2
        if (to >= world.left && to <= world.right)
          lines.push(`M${to} ${mid}V${target.y}`)
      }
    }
  }
  // At a fit scale where text is physically unreadable, draw the entire overview
  // in a few paths. Selecting a silhouette opens its readable card at native size.
  const overview = zoom < 0.25 && visible.length > 120
  const silhouettes = overview
    ? visible
        .map(
          ({ x, y, height }) =>
            `M${x} ${y}h${routeCardWidth}v${height}h-${routeCardWidth}Z`,
        )
        .join(' ')
    : ''
  return (
    <div className="route-tree">
      <div className="route-tree__workspace">
        <div className="route-tree__toolbar">
          <span className="route-tree__heading">{copy.graph}</span>
          <div className="route-tree__zoom">
            <button
              type="button"
              aria-label={nav.zoomOut}
              title={nav.zoomOut}
              disabled={zoom <= minZoom}
              onClick={() => changeZoom(zoom / 1.25)}
            >
              −
            </button>
            <output>
              {new Intl.NumberFormat(locale, {
                style: 'percent',
                maximumSignificantDigits: 3,
              }).format(zoom)}
            </output>
            <button
              type="button"
              aria-label={nav.zoomIn}
              title={nav.zoomIn}
              disabled={zoom >= 2}
              onClick={() => changeZoom(zoom * 1.25)}
            >
              +
            </button>
          </div>
          <button
            type="button"
            className="route-tree__fit"
            onClick={() =>
              changeZoom(
                fitZoom,
                { x: layout.width / 2, y: layout.height / 2 },
                true,
              )
            }
          >
            {nav.fit}
          </button>
        </div>
        <div
          className={`route-tree__viewport${panning ? ' is-panning' : ''}`}
          ref={host}
          role="region"
          aria-label={copy.graph}
          tabIndex={0}
          {...panHandlers}
          onKeyDown={navigate}
        >
          <svg
            width={layout.width * zoom}
            height={layout.height * zoom}
            style={{
              marginBlockStart: fitting
                ? Math.max(0, (viewport.height - layout.height * zoom) / 2)
                : 0,
            }}
            viewBox={`0 0 ${layout.width} ${layout.height}`}
            aria-label={copy.graph}
            onClick={(event) => {
              if (!overview || event.detail === 0) return
              const rect = event.currentTarget.getBoundingClientRect()
              const x =
                  ((event.clientX - rect.left) * layout.width) / rect.width,
                y = ((event.clientY - rect.top) * layout.height) / rect.height
              const box = visible.find(
                (b) =>
                  x >= b.x &&
                  x <= b.x + routeCardWidth &&
                  y >= b.y &&
                  y <= b.y + b.height,
              )
              if (box) {
                changeZoom(1, {
                  x: box.x + routeCardWidth / 2,
                  y: box.y + box.height / 2,
                })
                onSelect(box.node.id)
              }
            }}
          >
            <path
              className="route-tree__edges"
              vectorEffect="non-scaling-stroke"
              d={lines.join(' ')}
              aria-hidden="true"
            />
            {overview ? (
              <g className="route-tree__silhouettes" aria-hidden="true">
                <path d={silhouettes} />
                {selected && (
                  <rect
                    className="is-selected"
                    x={selected.x}
                    y={selected.y}
                    width={routeCardWidth}
                    height={selected.height}
                  />
                )}
              </g>
            ) : (
              visible.map((box) => (
                <StateCard
                  key={box.node.id}
                  box={box}
                  selected={box.node.id === selected?.node.id}
                  onSelect={onSelect}
                  onMeasure={onMeasure}
                  {...content}
                />
              ))
            )}
          </svg>
        </div>
      </div>
      <aside className="route-tree__detail" aria-labelledby={marker}>
        <h3 id={marker}>{copy.detail}</h3>
        {renderDetail(selected?.node ?? null)}
      </aside>
    </div>
  )
}
