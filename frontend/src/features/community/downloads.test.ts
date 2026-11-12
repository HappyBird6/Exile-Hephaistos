import { beforeEach, expect, it, vi } from 'vitest'
import { recordDownload, flushDownloads } from './downloads'
const send = vi.hoisted(() => vi.fn())
const scope = vi.hoisted(() => ({ value: null as string | null }))
vi.mock('./api', () => ({ boardRequest: send }))
vi.mock('../account/auth', () => ({ accountScope: () => scope.value }))
beforeEach(() => {
  scope.value = null
  localStorage.clear()
  send.mockReset()
})
it('queues only one unified download per post and retries without losing a successful local action', async () => {
  send.mockRejectedValue(Error('offline'))
  await expect(recordDownload('post', 'import')).resolves.toBeUndefined()
  await recordDownload('post', 'favorite')
  const pending = JSON.parse(
    localStorage.getItem('hephaistos.community.downloads:guest')!,
  )
  expect(pending).toHaveLength(1)
  send.mockResolvedValue({ downloads: 1 })
  const counted = vi.fn()
  await flushDownloads(counted)
  expect(counted).toHaveBeenCalledWith('post', { downloads: 1 })
  expect(
    JSON.parse(localStorage.getItem('hephaistos.community.downloads:guest')!),
  ).toEqual([])
})

it('settles the original queue when the account changes during an in-flight count', async () => {
  let finish!: () => void
  send.mockImplementation(
    () =>
      new Promise<void>((resolve) => {
        finish = resolve
      }),
  )
  const pending = recordDownload('post', 'import')
  scope.value = 'other'
  const otherKey = 'hephaistos.community.downloads:other'
  const otherQueue = JSON.stringify([{ id: 'post', action: 'favorite' }])
  localStorage.setItem(otherKey, otherQueue)
  finish()
  await pending
  expect(localStorage.getItem(otherKey)).toBe(otherQueue)
  expect(
    JSON.parse(localStorage.getItem('hephaistos.community.downloads:guest')!),
  ).toEqual([])
})
