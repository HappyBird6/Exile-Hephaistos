import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import {
  initialFixture,
  jsonResponse,
} from '../../../shared/test/craftingFixtures'
import { SessionContinuation } from './SessionContinuation'
import { concreteInitial } from '../workbenchApi'
import { emptyFilms, startFilm } from '../workbenchHistory'

afterEach(() => vi.unstubAllGlobals())
const root = concreteInitial(initialFixture)
const film = startFilm(emptyFilms(), root, 'old', 'previous').films[0]!
function show(response: unknown) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(response)))
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const onContinue = vi.fn()
  const page = render(
    <QueryClientProvider client={client}>
      <SessionContinuation
        initial={initialFixture}
        film={film}
        frame={film.frames[0]!}
        disabled={false}
        onContinue={onContinue}
      />
    </QueryClientProvider>,
  )
  return { onContinue, client, page }
}
it('validates before offering continuation and starts only after an explicit click', async () => {
  const result = {
    compatible: true,
    sameRules: false,
    compatibilityId: 'key',
    state: root,
    abyss: null,
    reasons: [],
  }
  const { onContinue, client, page } = show(result)
  expect(
    screen.getByText('Checking this checkpoint against current rules…'),
  ).toBeVisible()
  const button = await screen.findByRole('button', {
    name: 'Continue in a new session',
  })
  expect(onContinue).not.toHaveBeenCalled()
  fireEvent.click(button)
  expect(onContinue).toHaveBeenCalledWith(result)
  page.unmount()
  client.clear()
})
it('preserves incompatible sessions without offering a continuation button', async () => {
  const { onContinue, client, page } = show({
    compatible: false,
    sameRules: false,
    state: null,
    abyss: null,
    compatibilityId: null,
    reasons: ['UNKNOWN_MODIFIER'],
  })
  await screen.findByText(
    'This item cannot continue under current rules. The original session is preserved.',
  )
  expect(
    screen.queryByRole('button', { name: 'Continue in a new session' }),
  ).toBeNull()
  expect(onContinue).not.toHaveBeenCalled()
  expect(
    screen.getByText('A saved modifier is not present in the current catalog.'),
  ).toBeVisible()
  page.unmount()
  client.clear()
})

it('discards late validation after switching the selected checkpoint', async () => {
  let finish: ((response: Response) => void) | undefined
  const fetch = vi
    .fn()
    .mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finish = resolve
        }),
    )
    .mockResolvedValue(
      jsonResponse({
        compatible: false,
        sameRules: false,
        state: null,
        abyss: null,
        compatibilityId: null,
        reasons: ['INVALID_STATE'],
      }),
    )
  vi.stubGlobal('fetch', fetch)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  const onContinue = vi.fn()
  const props = { initial: initialFixture, film, disabled: false, onContinue }
  const page = render(
    <QueryClientProvider client={client}>
      <SessionContinuation {...props} frame={film.frames[0]!} />
    </QueryClientProvider>,
  )
  page.rerender(
    <QueryClientProvider client={client}>
      <SessionContinuation
        {...props}
        frame={{ ...film.frames[0]!, state: { ...root, itemLevel: 1 } }}
      />
    </QueryClientProvider>,
  )
  await screen.findByText(
    'This item cannot continue under current rules. The original session is preserved.',
  )
  await act(async () => {
    finish!(
      jsonResponse({
        compatible: true,
        sameRules: false,
        compatibilityId: 'key',
        state: root,
        abyss: null,
        reasons: [],
      }),
    )
  })
  expect(
    screen.queryByRole('button', { name: 'Continue in a new session' }),
  ).toBeNull()
  expect(onContinue).not.toHaveBeenCalled()
  page.unmount()
  client.clear()
})
