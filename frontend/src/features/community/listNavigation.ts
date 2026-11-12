/** URL-only preview/return context must never fragment the list's API cache. */
export function listParams(search: string, currentVersion?: string) {
  const params = new URLSearchParams(search)
  if (!params.has('version') && currentVersion)
    params.set('version', currentVersion)
  return params
}
export function listQuery(params: URLSearchParams) {
  const query = new URLSearchParams()
  for (const key of [
    'author',
    'base',
    'itemClass',
    'page',
    'sort',
    'version',
  ]) {
    const value = params.get(key)
    if (value) query.set(key, value)
  }
  return query.toString()
}
export function authorReturn(search: string) {
  const value = new URLSearchParams(search).get('authorFrom')
  // Only board routes; never navigate to a user-provided external address.
  return value &&
    /^\/community(?:\?|\/post\/[a-zA-Z0-9-]+(?:\?|$)|$)/.test(value)
    ? value
    : '/community'
}
export function authorHref(id: string, pathname: string, search: string) {
  const current = new URLSearchParams(search)
  const from = current.has('author') ? authorReturn(search) : pathname + search
  return '/community?' + new URLSearchParams({ author: id, authorFrom: from })
}
