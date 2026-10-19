import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { webcrypto } from 'node:crypto'
import { CompressionStream, DecompressionStream } from 'node:stream/web'
import { Blob } from 'node:buffer'
import { ScenarioExport, ScenarioImport } from './ScenarioCodePanel'
import { encodeScenario, scenarioLocales } from './scenarioCode'
import type { Scenario } from './scenarioCode'
import { requestFixture } from '../path-tree/fixtures.test-support'
import { createItemPresentation } from '../path-tree/presentation'
import { setLocale } from '../../../shared/i18n/i18n'
const present = createItemPresentation(
  { name: 'Solar Amulet', itemClass: 'Amulet' },
  {},
)
function fixture(): Scenario {
  const request = requestFixture(),
    item = request.start.item
  return {
    format: 'exile-scenario',
    version: 1,
    createdAt: '2026-10-19T00:00:00.000Z',
    baseKey: 'solar',
    request,
    abstractQuality40: false,
    result: {
      version: 'automatic-method-job-v1',
      id: '00000000-0000-4000-8000-000000000000',
      revision: 1,
      generation: 1,
      status: 'COMPLETE',
      reason: 'SUPPORTED_PLANS_COMPLETE',
      complete: true,
      operations: 1,
      retainedStates: 1,
      nodes: [
        {
          id: 0,
          depth: 0,
          quality40: false,
          item,
          progress: { goal: 'MATCH', entries: [] },
          discoveryFinished: true,
          directFinished: true,
        },
      ],
      methods: [],
      recommendations: [],
    },
    selected: 0,
    cycles: 300,
    route: [],
    cards: {
      '0': Object.fromEntries(
        scenarioLocales.map((locale) => [locale, present(item, locale)]),
      ) as Scenario['cards'][string],
    },
  }
}
beforeEach(() => {
  setLocale('en')
  sessionStorage.clear()
  localStorage.clear()
  vi.stubGlobal('crypto', webcrypto)
  vi.stubGlobal('CompressionStream', CompressionStream)
  vi.stubGlobal('DecompressionStream', DecompressionStream)
  vi.stubGlobal('Blob', Blob)
})
it('previews before opening, restores without API calls, and preserves existing localStorage', async () => {
  const scenario = fixture()
  scenario.request.goal.catalogVersion = 'historical-version'
  const code = await encodeScenario(scenario)
  localStorage.setItem('existing-user-work', 'keep exactly')
  const fetcher = vi.fn(() => Promise.reject(Error('no server')))
  vi.stubGlobal('fetch', fetcher)
  const changed = vi.fn(),
    copy = vi.fn()
  const mounted = render(
    <ScenarioImport onOpenChanged={changed} onCopy={copy} />,
  )
  fireEvent.change(screen.getByRole('textbox', { name: 'Scenario code' }), {
    target: { value: code },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Validate and preview' }))
  const open = await screen.findByRole('button', {
    name: 'Open read-only result',
  })
  expect(changed).not.toHaveBeenCalled()
  expect(localStorage.getItem('existing-user-work')).toBe('keep exactly')
  fireEvent.click(open)
  expect(
    await screen.findByText(/Saved result — read only/),
  ).toBeInTheDocument()
  expect(changed).toHaveBeenCalledWith(true)
  expect(
    screen.queryByRole('button', { name: 'Recalculate' }),
  ).not.toBeInTheDocument()
  expect(fetcher).not.toHaveBeenCalled()
  expect(localStorage.getItem('existing-user-work')).toBe('keep exactly')
  const second = structuredClone(scenario)
  second.result.revision = 2
  second.result.nodes.push({
    ...structuredClone(second.result.nodes[0]!),
    id: 1,
  })
  second.result.retainedStates = 2
  second.selected = 1
  second.cards['1'] = Object.fromEntries(
    scenarioLocales.map((language) => [
      language,
      { ...second.cards['0']![language], name: 'Second snapshot selected' },
    ]),
  ) as Scenario['cards'][string]
  fireEvent.change(screen.getByRole('textbox', { name: 'Scenario code' }), {
    target: { value: await encodeScenario(second) },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Validate and preview' }))
  fireEvent.click(
    await screen.findByRole('button', { name: 'Open read-only result' }),
  )
  await waitFor(() =>
    expect(
      document.querySelector('.scenario-readonly .automatic-inspector'),
    ).toHaveTextContent('Second snapshot selected'),
  )
  expect(fetcher).not.toHaveBeenCalled()
  mounted.unmount()
  render(<ScenarioImport onOpenChanged={changed} onCopy={copy} />)
  expect(
    await screen.findByText(/Saved result — read only/),
  ).toBeInTheDocument()
  expect(fetcher).not.toHaveBeenCalled()
  expect(copy).not.toHaveBeenCalled()
})
it('keeps a manually copyable code when clipboard permission fails', async () => {
  const scenario = fixture()
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value: { writeText: vi.fn(() => Promise.reject(Error('denied'))) },
  })
  render(
    <ScenarioExport
      request={scenario.request}
      view={scenario.result}
      selected={0}
      cycles={300}
      route={[]}
      presentItem={present}
      abstractQuality40={false}
      baseKey="solar"
    />,
  )
  fireEvent.click(screen.getByRole('button', { name: 'Copy scenario code' }))
  await waitFor(() =>
    expect(
      (
        screen.getByRole('textbox', {
          name: 'Scenario code',
        }) as HTMLTextAreaElement
      ).value,
    ).toMatch(/^EHSC[12]\./),
  )
  expect(
    screen.getAllByText(/Select and copy the code manually/).length,
  ).toBeGreaterThan(0)
})
it('does not execute pasted content or show a result for invalid data', async () => {
  const copied = vi.fn()
  render(<ScenarioImport onOpenChanged={vi.fn()} onCopy={copied} />)
  fireEvent.change(screen.getByRole('textbox', { name: 'Scenario code' }), {
    target: { value: '<script>globalThis.executed=true</script>' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Validate and preview' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Cannot load')
  expect(
    screen.queryByRole('button', { name: 'Open read-only result' }),
  ).not.toBeInTheDocument()
  expect(copied).not.toHaveBeenCalled()
})
