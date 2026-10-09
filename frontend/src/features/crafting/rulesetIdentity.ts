export const rulesetHeader = 'X-Crafting-Ruleset'

export function isRulesetIdentity(value: unknown): value is string {
  return typeof value === 'string' && value.trim().length > 0
}

export function rulesetHeaders(identity: string | undefined) {
  if (!isRulesetIdentity(identity))
    throw new Error(translate('ruleset.request_missing'))
  return { 'Content-Type': 'application/json', [rulesetHeader]: identity }
}

export function verifyRulesetResponse(response: Response, identity: string) {
  if (response.headers.get(rulesetHeader) !== identity)
    throw new Error(translate('ruleset.request_changed'))
}
import { translate } from '../../shared/i18n/i18n'
