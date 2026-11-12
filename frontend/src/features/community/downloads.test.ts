import { beforeEach, expect, it, vi } from 'vitest'
import { recordDownload, flushDownloads } from './downloads'
const send = vi.hoisted(() => vi.fn())
vi.mock('./api', () => ({ boardRequest: send }))
vi.mock('../account/auth', () => ({ accountScope: () => null }))
beforeEach(() => {
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
  await flushDownloads()
  expect(
    JSON.parse(localStorage.getItem('hephaistos.community.downloads:guest')!),
  ).toEqual([])
})
