import type { MessageKey } from '../../../shared/i18n/i18n'
import { clipboardBase } from '../baseRegistry'
import type { WorkbenchBaseKey } from '../baseRegistry'
import type { Item } from '../itemModels'
import { recognizedClipboardSentence } from '../item-text/modifierClipboardText'
import type { Initial } from '../craftingApi'
import {
  ItemTextApiError,
  maxItemTextBytes,
  parseItemText,
} from '../itemTextApi'
import { mapSolarText } from '../workbenchApi'
import type { ConcreteItem } from '../workbenchApi'
import { verifiedHistoryState } from '../workbenchHistory'

export type ItemTextProblem = {
  lineNumber?: number | undefined
  text?: string | undefined
  code?: MessageKey
  message?: string
}

export type ValidatedItemText =
  | { ok: true; state: ConcreteItem; catalog: Initial; base: WorkbenchBaseKey }
  | { ok: true; observed: Item; catalog: Initial; base: WorkbenchBaseKey }
  | { ok: false; problems: ItemTextProblem[] }

/** Validation has no draft, history or displayed-item side effects. */
export async function validateItemText({
  text,
  signal,
  before,
  catalogFor,
}: {
  text: string
  signal: AbortSignal
  before?: ConcreteItem
  catalogFor: (base: WorkbenchBaseKey, level: number) => Promise<Initial>
}): Promise<ValidatedItemText> {
  if (!text.trim()) return { ok: false, problems: [{ code: 'input.empty' }] }
  if (new TextEncoder().encode(text).length > maxItemTextBytes)
    return { ok: false, problems: [{ code: 'input.too_large' }] }
  try {
    const parsed = await parseItemText(text, signal)
    const base = clipboardBase(
      parsed.displayBase ?? parsed.displayName,
      parsed.itemClass,
      text,
    )
    if (!base)
      return {
        ok: false,
        problems: [
          {
            code: 'server.clipboard_base',
            text: parsed.displayBase ?? parsed.displayName,
          },
        ],
      }
    if (
      parsed.itemLevel === null ||
      parsed.itemLevel < 1 ||
      parsed.itemLevel > 100
    )
      return {
        ok: false,
        problems: [
          {
            code: 'server.mapping_level',
            text: parsed.properties.find((field) => field.key === 'Item Level')
              ?.source.raw,
          },
        ],
      }
    const catalog = await catalogFor(base, parsed.itemLevel)
    signal.throwIfAborted()
    const sameBaseBefore =
      before?.baseItemId === catalog.state.baseItemId ? before : undefined
    const importingSanctified =
      parsed.flags.some((line) => line.raw === 'Sanctified') &&
      !before?.conditions.includes('SANCTIFIED')
    const editableBefore = importingSanctified ? undefined : sameBaseBefore
    let mapped = await mapSolarText(
      text,
      signal,
      catalog.modifiers,
      catalog.rulesetIdentity,
      editableBefore,
    )
    // A complete replacement starts a new film; inherited editor-only metadata must not block it.
    if (
      !mapped.mapped &&
      editableBefore &&
      !editableBefore.conditions.includes('SANCTIFIED') &&
      mapped.issues.some((issue) =>
        /Socket and Rune lines|catalyst quality and special conditions|Modifier edits cannot reconstruct/.test(
          issue.message,
        ),
      )
    ) {
      mapped = await mapSolarText(
        text,
        signal,
        catalog.modifiers,
        catalog.rulesetIdentity,
      )
    }
    if (
      (!mapped.mapped || !mapped.state) &&
      parsed.modifiers.length > 0 &&
      parsed.modifiers.every(
        (modifier) =>
          modifier.type === 'RUNE' ||
          recognizedClipboardSentence(modifier.text, catalog.modifiers),
      ) &&
      parsed.unparsedLines.every(
        (line) =>
          /^\{ .*Modifier.* }$/.test(line.raw) ||
          line.raw.startsWith('Grants Skill:'),
      ) &&
      mapped.issues.every((issue) =>
        /Special item conditions|special item conditions/.test(issue.message),
      )
    )
      return { ok: true, observed: parsed, catalog, base }
    if (!mapped.mapped || !mapped.state)
      return {
        ok: false,
        problems: mapped.issues.length
          ? mapped.issues.map((issue) => ({
              lineNumber: issue.lineNumber || undefined,
              message: issue.message,
              text: parsed.text.lines.find(
                (line) => line.number === issue.lineNumber,
              )?.raw,
            }))
          : [{ code: 'error.mapping_verify' }],
      }
    if (!verifiedHistoryState(mapped.state, catalog))
      return { ok: false, problems: [{ code: 'error.mapping_verify' }] }
    return { ok: true, state: mapped.state, catalog, base }
  } catch (error) {
    if (signal.aborted) throw error
    const code: MessageKey =
      error instanceof ItemTextApiError
        ? error.code === 'INVALID_ITEM_TEXT'
          ? 'input.invalid'
          : error.code === 'ITEM_TEXT_TOO_LARGE'
            ? 'input.too_large'
            : error.code === 'MALFORMED_REQUEST'
              ? 'input.malformed'
              : 'input.connection'
        : 'input.connection'
    return { ok: false, problems: [{ code }] }
  }
}
