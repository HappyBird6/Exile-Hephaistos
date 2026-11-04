import { locales } from '../../../../shared/i18n/i18n'
import type { Initial } from '../../craftingApi'
import {
  isRulesetIdentity,
  rulesetHeaders,
  verifyRulesetResponse,
} from '../../rulesetIdentity'
import { workbenchOmens } from '../../workbenchApi'
import { isChaosRepeatMetadata } from '../chaosRepeatReceipt'
import {
  chaosRepeatMaximumUsesLimit,
  defaultChaosRepeatMaximumUses,
} from '../controls/types'
import { object, validRepeatState } from './stateValidation'
import type { ChaosRepeatRequest, ChaosRepeatResult } from './types'
import { validateChaosRepeatResponse } from './validateChaosRepeatResponse'

const omenIds = new Set(workbenchOmens.map((omen) => omen.id))
const uuid =
  /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/

/** One immutable body, one request, no retry and no client-side regex execution. */
export async function applyChaosRepeat(
  request: ChaosRepeatRequest,
  initial: Initial,
  signal: AbortSignal,
): Promise<ChaosRepeatResult> {
  const requestId =
    typeof request?.requestId === 'string' ? request.requestId : ''
  let snapshot: ChaosRepeatRequest
  let body: string
  let sha256: string
  let maximumUses: number
  let catalog: Initial
  try {
    snapshot = structuredClone(request)
    catalog = structuredClone(initial)
    maximumUses =
      snapshot.maximumUses === undefined
        ? Number(defaultChaosRepeatMaximumUses)
        : snapshot.maximumUses
    if (
      !uuid.test(requestId) ||
      !isRulesetIdentity(catalog.rulesetIdentity) ||
      !isChaosRepeatMetadata({
        version: 1,
        action: snapshot.action,
        uses: maximumUses,
      }) ||
      maximumUses > chaosRepeatMaximumUsesLimit ||
      !locales.includes(snapshot.locale) ||
      typeof snapshot.pattern !== 'string' ||
      snapshot.pattern.length === 0 ||
      snapshot.pattern.length > 512 ||
      !Array.isArray(snapshot.activeOmens) ||
      !Array.from(snapshot.activeOmens).every(
        (id) => typeof id === 'string' && omenIds.has(id),
      ) ||
      new Set(snapshot.activeOmens).size !== snapshot.activeOmens.length ||
      !validRepeatState(snapshot.state, catalog)
    )
      return { kind: 'rejected', source: 'client', requestId }
    body = JSON.stringify(snapshot)
    const bytes = new TextEncoder().encode(body)
    const hash = await globalThis.crypto.subtle.digest('SHA-256', bytes)
    sha256 = Array.from(new Uint8Array(hash), (byte) =>
      byte.toString(16).padStart(2, '0'),
    ).join('')
  } catch {
    return { kind: 'rejected', source: 'client', requestId }
  }
  if (signal.aborted) return { kind: 'uncertain', requestId, reason: 'ABORTED' }
  let response: Response
  try {
    response = await fetch('/api/v1/crafting/workbench/chaos-repeat', {
      method: 'POST',
      signal,
      headers: rulesetHeaders(catalog.rulesetIdentity),
      body,
    })
  } catch {
    return {
      kind: 'uncertain',
      requestId,
      reason: signal.aborted ? 'ABORTED' : 'NETWORK',
    }
  }
  if (signal.aborted) return { kind: 'uncertain', requestId, reason: 'ABORTED' }
  try {
    verifyRulesetResponse(response, catalog.rulesetIdentity)
  } catch {
    return { kind: 'invalid-response', requestId }
  }
  if (response.status >= 400 && response.status < 500)
    return {
      kind: 'rejected',
      requestId,
      source: 'server',
      status: response.status,
    }
  let raw: unknown
  try {
    raw = await response.json()
  } catch {
    return response.ok
      ? { kind: 'invalid-response', requestId }
      : {
          kind: 'uncertain',
          requestId,
          reason: 'HTTP_ERROR',
          status: response.status,
        }
  }
  if (signal.aborted) return { kind: 'uncertain', requestId, reason: 'ABORTED' }
  if (response.status === 500) {
    if (
      !object(raw) ||
      raw.status !== 500 ||
      !Object.hasOwn(raw, 'partialOutcome')
    )
      return { kind: 'uncertain', requestId, reason: 'HTTP_ERROR', status: 500 }
    raw = raw.partialOutcome
  } else if (response.status !== 200) {
    return {
      kind: 'uncertain',
      requestId,
      reason: 'HTTP_ERROR',
      status: response.status,
    }
  }
  return (
    validateChaosRepeatResponse(raw, {
      request: snapshot,
      maximumUses,
      sha256,
      initial: catalog,
      httpStatus: response.status,
    }) ?? { kind: 'invalid-response', requestId }
  )
}
