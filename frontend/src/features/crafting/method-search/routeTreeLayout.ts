import type {
  AcquisitionRouteTree,
  RouteTreeNode,
} from './acquisitionRouteTree'

export const routeCardWidth = 276
const columnGap = 32
const rowGap = 64
const padding = 24

export type RouteTreeBox = {
  node: RouteTreeNode
  x: number
  y: number
  height: number
}

// Each leaf owns a column. Its ancestors sit between their outermost children.
// Only the children reachable from the projected root belong to the displayed tree.
export function layoutRouteTree(
  tree: AcquisitionRouteTree,
  heights: ReadonlyMap<number, number>,
) {
  const rows: RouteTreeBox[][] = []
  const byId = new Map<number, RouteTreeBox>()
  let leaves = 0
  function position(node: RouteTreeNode): RouteTreeBox {
    const row = (rows[node.depth] ??= [])
    const box: RouteTreeBox = {
      node,
      x: 0,
      y: 0,
      height:
        heights.get(node.id) ??
        172 +
          30 *
            (node.state.explicits.length +
              (node.state.desecrated?.length ?? 0)),
    }
    row.push(box)
    byId.set(node.id, box)
    if (node.children.length === 0) {
      box.x = padding + leaves * (routeCardWidth + columnGap)
      leaves++
    } else {
      for (const id of node.children) position(tree.nodes[id]!)
      const first = byId.get(node.children[0]!)!
      const last = byId.get(node.children[node.children.length - 1]!)!
      box.x = (first.x + last.x) / 2
    }
    return box
  }
  if (tree.nodes[0]) position(tree.nodes[0])
  let y = padding
  for (const row of rows) {
    let height = 0
    for (const box of row) {
      box.y = y
      height = Math.max(height, box.height)
    }
    y += height + rowGap
  }
  return {
    rows,
    byId,
    width: Math.max(
      1,
      2 * padding + leaves * (routeCardWidth + columnGap) - columnGap,
    ),
    height: Math.max(1, y - rowGap + padding),
  }
}
