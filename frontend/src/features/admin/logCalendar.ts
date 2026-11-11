export type LogPolicy = {
  previousDays: number
  timeZone: string
  pageSize: number
}
export function logCalendar(policy: LogPolicy, now = new Date()) {
  const today = new Intl.DateTimeFormat('en-CA', {
    timeZone: policy.timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)
  const minimum = new Date(today + 'T00:00:00Z')
  minimum.setUTCDate(minimum.getUTCDate() - policy.previousDays)
  const hour = new Intl.DateTimeFormat('en-GB', {
    timeZone: policy.timeZone,
    hour: '2-digit',
    hourCycle: 'h23',
  }).format(now)
  return { today, minimum: minimum.toISOString().slice(0, 10), hour }
}
export function retainedDate(
  selected: string,
  previousToday: string,
  calendar: ReturnType<typeof logCalendar>,
) {
  return selected === previousToday
    ? calendar.today
    : selected < calendar.minimum
      ? calendar.minimum
      : selected > calendar.today
        ? calendar.today
        : selected
}
