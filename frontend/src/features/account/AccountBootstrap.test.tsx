import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import { AccountBootstrap } from './AccountBootstrap'
const mocks = vi.hoisted(() => ({
  load: vi.fn(),
  sync: vi.fn(),
  merge: vi.fn(),
  decide: vi.fn(),
}))
vi.mock('./auth', () => ({
  loadAccount: mocks.load,
  useAccount: () => ({
    enabled: true,
    user: { id: 'account', displayName: 'user' },
    csrf: 'token',
  }),
  AccountError: class extends Error {
    status = 401
  },
}))
vi.mock('./librarySync', () => ({
  syncLibrary: mocks.sync,
  browserMergeStatus: mocks.merge,
  decideBrowserMerge: mocks.decide,
}))
beforeEach(() => {
  vi.clearAllMocks()
  mocks.sync.mockResolvedValue(false)
  mocks.merge.mockResolvedValue({ needed: true })
  mocks.decide.mockResolvedValue(undefined)
})
it('resolves identity before mounting storage consumers and asks before importing', async () => {
  let resolve!: (value: unknown) => void
  mocks.load.mockImplementation(
    () =>
      new Promise((r) => {
        resolve = r
      }),
  )
  render(
    <AccountBootstrap>
      <p>Workspace</p>
    </AccountBootstrap>,
  )
  expect(screen.queryByText('Workspace')).toBeNull()
  expect(mocks.decide).not.toHaveBeenCalled()
  resolve({ enabled: true, user: { id: 'account', displayName: 'user' } })
  expect(await screen.findByText('Workspace')).toBeVisible()
  expect(
    screen.getByRole('button', { name: 'Add to my account' }),
  ).toBeVisible()
  fireEvent.click(screen.getByRole('button', { name: 'Skip' }))
  await waitFor(() =>
    expect(mocks.decide).toHaveBeenCalledWith('account', false),
  )
  expect(screen.queryByRole('button', { name: 'Add to my account' })).toBeNull()
})
