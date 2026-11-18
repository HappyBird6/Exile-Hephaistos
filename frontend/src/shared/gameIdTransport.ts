import bundle from './contracts/gameIds.json'

const domains: Record<string, Record<string, string>> = bundle.domains
const forward = Object.fromEntries(
  Object.entries(domains).map(([domain, entries]) => [
    domain,
    Object.fromEntries(
      Object.entries(entries).map(([id, source]) => [source, id]),
    ),
  ]),
)
export const gameIdIdentity = bundle.identity + '-fields2'
export const gameIdHeader = 'X-Crafting-Ids'
const bodyHeader = 'X-Crafting-Ids-Body'
let negotiatedRuleset: string | null = null

const object = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value)
const modifierCandidateModels = new Set([
  'poe2db-published-candidates-v1',
  'boots-uniform-candidates-v1',
  'body-uniform-candidates-v1',
  'helmets-uniform-candidates-v1',
  'gloves-uniform-candidates-v1',
  'sapphire-uniform-candidates-v1',
  'uniform-removal-v1',
  'user-coupled-ratio-half-up-v1',
  'uniform-liquid-outcomes-v1',
  'uniform-essence-choice-v1',
])
function domain(
  field: string,
  outer: string,
  parent: Record<string, unknown> | null,
): string {
  switch (field) {
    case 'candidates':
      return outer === 'assumptions' &&
        typeof parent?.id === 'string' &&
        modifierCandidateModels.has(parent.id)
        ? 'modifiers'
        : ''
    case 'candidateUnit':
      return outer === 'assumptions' &&
        (parent?.id === 'uniform-integer-roll-v1' ||
          parent?.id === 'assumed-source-integer-roll-v1')
        ? 'stats'
        : ''
    case 'baseItemId':
      return 'bases'
    case 'modifierId':
    case 'implicitModifierId':
    case 'modifierIds':
      return 'modifiers'
    case 'sourceStatId':
    case 'sourceStatIds':
    case 'statIds':
    case 'stat':
      return 'stats'
    case 'familyIds':
      return 'families'
    case 'tags':
      return 'tags'
    case 'action':
    case 'baseAction':
    case 'baseMaterialActions':
      return 'actions'
    case 'augmentId':
      return 'augments'
    case 'itemClass':
      return 'itemClasses'
    case 'id':
      return parent && 'affixType' in parent && 'stats' in parent
        ? 'modifiers'
        : parent && 'rarePrefixes' in parent
          ? 'bases'
          : parent && 'min' in parent && 'max' in parent
            ? 'stats'
            : ''
    default:
      return ''
  }
}
/** 도메인별 명시적 식별자 필드만 바꾼다. 자유 입력·텍스트·모르는 값은 그대로 보존한다. */
export function transformGameIds(input: unknown, compact: boolean): unknown {
  const mappings = compact ? forward : domains
  const convert = (kind: string, value: string): string => {
    const entries = mappings[kind]
    return entries && Object.hasOwn(entries, value)
      ? (entries[value] ?? value)
      : value
  }
  function visit(
    value: unknown,
    field: string,
    outer: string,
    parent: Record<string, unknown> | null,
  ): unknown {
    if (typeof value === 'string')
      return convert(domain(field, outer, parent), value)
    if (Array.isArray(value))
      return value.map((child) => visit(child, field, outer, parent))
    if (!object(value)) return value
    const keyDomain =
      field === 'values'
        ? 'stats'
        : field === 'modifiers'
          ? 'modifiers'
          : outer === 'sanctification' &&
              (field === 'implicits' || field === 'explicits')
            ? 'modifiers'
            : (outer === 'implicits' || outer === 'explicits') &&
                !('modifierId' in value)
              ? 'stats'
              : ''
    const result: Record<string, unknown> = Object.create(null)
    for (const [key, child] of Object.entries(value)) {
      const target = convert(keyDomain, key)
      if (Object.hasOwn(result, target))
        throw new Error('Conflicting identifier keys')
      result[target] = visit(child, key, field, value)
    }
    return result
  }
  return visit(input, '', '', null)
}

/** 기존 fetch의 취소·상태 코드를 유지한다. 서버가 같은 사전을 확인한 다음 요청부터 본문을 압축한다. */
export async function craftingFetch(
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> {
  const url =
    typeof input === 'string'
      ? input
      : input instanceof URL
        ? input.pathname
        : input.url
  const path = new URL(url, globalThis.location?.origin ?? 'http://localhost')
    .pathname
  if (!/^\/api\/v[12]\/crafting\//.test(path) || path.includes('/chaos-repeat'))
    return globalThis.fetch(input, init)
  const headers = new Headers(
    init?.headers ?? (input instanceof Request ? input.headers : undefined),
  )
  headers.set(gameIdHeader, gameIdIdentity)
  const currentRuleset = headers.get('X-Crafting-Ruleset')
  const compress =
    currentRuleset !== null &&
    negotiatedRuleset === currentRuleset &&
    typeof init?.body === 'string'
  if (compress) headers.set(bodyHeader, 'compact')
  const response = await globalThis.fetch(input, {
    ...init,
    headers,
    ...(compress
      ? {
          body: JSON.stringify(
            transformGameIds(JSON.parse(init!.body as string), true),
          ),
        }
      : {}),
  })
  const identity = response.headers.get(gameIdHeader)
  if (identity === null) {
    negotiatedRuleset = null
    if (compress)
      throw new Error(
        'Identifier transport changed; reload the crafting catalog',
      )
    return response
  }
  if (identity !== gameIdIdentity)
    throw new Error('Identifier mappings differ; reload the crafting catalog')
  negotiatedRuleset = response.headers.get('X-Crafting-Ruleset')
  return decodedResponse(response)
}

function decodedResponse(response: Response): Response {
  return new Proxy(response, {
    get(target, key) {
      if (key === 'json')
        return async () => transformGameIds(await target.json(), false)
      if (key === 'text')
        return async () =>
          JSON.stringify(transformGameIds(await target.json(), false))
      if (key === 'clone') return () => decodedResponse(target.clone())
      const value = Reflect.get(target, key, target)
      return typeof value === 'function' ? value.bind(target) : value
    },
  })
}
