import { fireEvent, render, screen, within } from '@testing-library/react'
import { useState } from 'react'
import { expect, it } from 'vitest'
import { requestFixture } from '../path-tree/fixtures.test-support'
import type { ConcreteItem } from '../workbenchApi'
import {
  buildAcquisitionRouteTree,
  projectAcquisitionRouteTree,
} from './acquisitionRouteTree'
import { RouteTreeGraph } from './RouteTreeGraph'
import { layoutRouteTree, routeCardWidth } from './routeTreeLayout'

const start = requestFixture().start.item as ConcreteItem
function makeTree(count: number) {
  return buildAcquisitionRouteTree(
    start,
    Array.from({ length: count }, (_, index) => {
      const step = {
        action: 'EXALTED',
        omens: [],
        acquired: [index + 1],
        lost: [],
        reachChance: { probability: 0.25 },
      }
      const route = {
        cardIndex: 0,
        order: [index + 1],
        steps: [step],
        stateIndices: [0],
        probability: 0.25,
      }
      return {
        index,
        route,
        result: {
          complete: true,
          cards: [{ options: [] }],
          states: [start],
          routes: [route],
        },
      }
    }),
  )
}

function Fixture({
  tree,
  initialSelection = 0,
}: {
  tree: ReturnType<typeof makeTree>
  initialSelection?: number
}) {
  const [selected, setSelected] = useState(initialSelection)
  return (
    <RouteTreeGraph
      tree={tree}
      selectedId={selected}
      onSelect={setSelected}
      renderItem={(n) => <p>Item {n.id}</p>}
      renderDetail={(n) => <p>History {n?.id}</p>}
      actionText={(n) => (n.id ? 'Roll ' + n.id : 'Start')}
      chanceText={(n) => (n.id ? '25%' : '100%')}
      costText={(n) => (n.id ? '3 ex' : '0 ex')}
    />
  )
}

it('places children below the measured parent and centres the shared starting state', () => {
  const tree = makeTree(3)
  const layout = layoutRouteTree(tree, new Map([[0, 600]]))
  const [root] = layout.rows[0]!,
    children = layout.rows[1]!
  expect(root!.x).toBe((children[0]!.x + children[2]!.x) / 2)
  expect(children[0]!.y).toBeGreaterThan(root!.y + root!.height)
  expect(children[0]!.x + routeCardWidth).toBeLessThan(children[1]!.x)
  const subset = layoutRouteTree(
    projectAcquisitionRouteTree(tree, new Set([1])),
    new Map(),
  )
  expect([...subset.byId.keys()]).toEqual([0, 2])
})

it('selects external history by clicking a card or navigating between siblings with the keyboard', () => {
  const view = render(<Fixture tree={makeTree(2)} />)
  const detail = screen.getByRole('complementary')
  expect(view.container.querySelector('svg')?.contains(detail)).toBe(false)
  expect(within(detail).getByText('History 0')).toBeTruthy()
  const root = screen.getByRole('button', { name: /Start/ })
  expect(within(root.closest('article')!).getByText('100%')).toBeTruthy()
  expect(within(root.closest('article')!).getByText('0 ex')).toBeTruthy()
  fireEvent.click(screen.getByText('Item 1'))
  expect(within(detail).getByText('History 1')).toBeTruthy()
  fireEvent.keyDown(screen.getByRole('button', { name: /Roll 1/ }), {
    key: 'ArrowRight',
  })
  expect(within(detail).getByText('History 2')).toBeTruthy()
  expect(screen.getByRole('button', { name: /Roll 2/ })).toHaveFocus()
})

it('keeps the detail panel available without highlighting a different item while its route is pending', () => {
  render(<Fixture tree={makeTree(1)} initialSelection={-1} />)
  expect(screen.getByRole('button', { name: /Start/ })).toHaveAttribute(
    'aria-pressed',
    'false',
  )
  expect(
    within(screen.getByRole('complementary')).getByText('History'),
  ).toBeTruthy()
  fireEvent.keyDown(screen.getByRole('region'), { key: 'Home' })
  expect(screen.getByRole('complementary')).toHaveTextContent('History 0')
})

it('mounts only visible item cards and fits a wide tree without mounting hundreds of hidden cards', () => {
  const view = render(<Fixture tree={makeTree(240)} />)
  const before = view.container.querySelectorAll('.route-tree__card').length
  expect(before).toBeGreaterThan(0)
  expect(before).toBeLessThan(15)
  const full = Number(
    view.container.querySelector('svg')!.getAttribute('width'),
  )
  fireEvent.click(screen.getByRole('button', { name: /fit graph/i }))
  expect(
    Number(view.container.querySelector('svg')!.getAttribute('width')),
  ).toBeLessThan(full)
  expect(
    view.container.querySelectorAll('.route-tree__card').length,
  ).toBeLessThan(15)
  expect(view.container.querySelector('.route-tree__silhouettes')).toBeTruthy()
  expect(screen.getByRole('complementary')).toHaveTextContent('History 0')
})
