import { expect, it } from 'vitest'
import { logCalendar, retainedDate } from './logCalendar'
it('uses server retention and timezone and clamps dates that expire overnight', () => {
  const policy = { previousDays: 3, timeZone: 'Asia/Seoul', pageSize: 25 }
  const next = logCalendar(policy, new Date('2026-11-11T15:00:00Z'))
  expect(next).toEqual({
    today: '2026-11-12',
    minimum: '2026-11-09',
    hour: '00',
  })
  expect(retainedDate('2026-11-08', '2026-11-11', next)).toBe('2026-11-09')
  expect(retainedDate('2026-11-11', '2026-11-11', next)).toBe('2026-11-12')
  expect(retainedDate('2026-11-10', '2026-11-11', next)).toBe('2026-11-10')
})
