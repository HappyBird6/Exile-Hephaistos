const labels = new WeakMap<Response, string>()
let sequence = 0

export function traceLabel(response: Response) {
  return labels.get(response) ?? '[API]'
}

export function inheritApiTrace(original: Response, decoded: Response) {
  const label = labels.get(original)
  if (label) labels.set(decoded, label)
  return decoded
}

export function installApiTrace() {
  const fetch = window.fetch.bind(window)
  window.fetch = async (input, init) => {
    const request = input instanceof Request ? input : undefined
    const url = new URL(request?.url ?? String(input), window.location.href)
    if (
      url.origin !== window.location.origin ||
      !url.pathname.startsWith('/api/')
    )
      return fetch(input, init)
    const method = init?.method ?? request?.method ?? 'GET'
    const label = `[API #${++sequence}] ${method} ${url.pathname}`
    const started = performance.now()
    console.info(label, '요청')
    try {
      const response = await fetch(input, init)
      labels.set(response, label)
      const elapsed = Math.round(performance.now() - started)
      const log = response.ok ? console.info : console.warn
      log(label, `HTTP ${response.status} · ${elapsed} ms`)
      return response
    } catch (error) {
      console.warn(
        label,
        error instanceof Error ? error.name : 'FetchError',
        `${Math.round(performance.now() - started)} ms`,
      )
      throw error
    }
  }
}
