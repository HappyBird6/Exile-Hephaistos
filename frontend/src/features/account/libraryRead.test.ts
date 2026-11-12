import { beforeEach, expect, it, vi } from 'vitest'
import { readRemoteLibrary } from './libraryRead'
import { accountRequest, accountScope } from './auth'
import { emptyFilms, startFilm } from '../crafting/workbenchHistory'
import { concreteInitial } from '../crafting/workbenchApi'
import { initialFixture } from '../../shared/test/craftingFixtures'
const film = startFilm(
  emptyFilms(),
  concreteInitial(initialFixture),
  'a',
  'fixture',
).films[0]!

vi.mock('./auth', () => ({ accountRequest: vi.fn(), accountScope: vi.fn() }))
const refs = [{ kind: 'session' as const, id: 'a', version: 1, deleted: false }]
beforeEach(() => {
  vi.mocked(accountScope).mockReturnValue('owner')
  vi.mocked(accountRequest).mockReset()
})
it.each(
  [
    [],
    [
      {
        kind: 'session',
        id: 'wrong',
        version: 1,
        content: { ...film, id: 'wrong' },
      },
    ],
    [{ kind: 'session', id: 'a', version: 2, content: film }],
    [{ kind: 'session', id: 'a', version: 1, content: null }],
  ].map((body) => ({ body })),
)(
  'rejects incomplete or changed batches without returning partial data: %j',
  async ({ body }) => {
    vi.mocked(accountRequest).mockResolvedValue(Response.json(body))
    await expect(readRemoteLibrary('owner', 0, refs, [], '5')).rejects.toThrow()
  },
)
it.each(['0', '-1', '21', 'NaN', '1.5', ''])(
  'rejects invalid server budgets %s',
  async (size) => {
    await expect(readRemoteLibrary('owner', 0, refs, [], size)).rejects.toThrow(
      'INVALID_LIBRARY_BATCH_SIZE',
    )
    expect(accountRequest).not.toHaveBeenCalled()
  },
)
it('stops on an account switch before sending more reads', async () => {
  vi.mocked(accountScope).mockReturnValue('different')
  await expect(readRemoteLibrary('owner', 0, refs, [], '5')).rejects.toThrow(
    'ACCOUNT_CHANGED',
  )
  expect(accountRequest).not.toHaveBeenCalled()
})

it('rejects account switching after a response before requesting the next batch', async () => {
  vi.mocked(accountRequest).mockImplementation(async () => {
    vi.mocked(accountScope).mockReturnValue('different')
    return Response.json([
      { kind: 'session', id: 'a', version: 1, content: film },
    ])
  })
  await expect(
    readRemoteLibrary('owner', 0, [...refs, { ...refs[0]!, id: 'b' }], [], '1'),
  ).rejects.toThrow('ACCOUNT_CHANGED')
  expect(accountRequest).toHaveBeenCalledTimes(1)
})
