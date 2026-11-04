import type { Locale } from '../../../../shared/i18n/i18n'

export type ExplicitOptionGroup = {
  explicitIndex: number
  modifierId: string
  affixType: 'PREFIX' | 'SUFFIX'
  fractured: boolean
  text: string
  /** Actual LF boundaries only; no visual wrapping or matching-unit decision. */
  lines: string[]
  textKind: 'option-sentence' | 'source-units'
}

export type DisplayIssueCode =
  | 'MISSING_DEFINITION'
  | 'DEFINITION_ID_MISMATCH'
  | 'EXPLICIT_WITHOUT_AFFIX'
  | 'MISSING_OR_NONFINITE_STAT_VALUE'
  | 'NONINTEGER_SOURCE_ROLL'
  | 'UNVERIFIED_TEMPLATE_BINDING'
  | 'ENGLISH_TEXT_FALLBACK'
  | 'ENGLISH_REVEAL_TEMPLATE_FALLBACK'
  | 'SOURCE_UNIT_FALLBACK'
  | 'UNRESOLVED_TEMPLATE_VALUE'
  | 'UNRESOLVED_ROLL_TEXT'
  | 'UNREVIEWED_NUMERIC_SEMANTICS'
  | 'JEWEL_EFFECT_AND_QUALITY_PROVISIONAL'
  | 'DISPLAY_PROJECTION_UNAVAILABLE'
  | 'UNREVEALED_DESECRATED_SLOT'

export type DisplayIssue = {
  code: DisplayIssueCode
  modifierId?: string
  explicitIndex?: number
  slotId?: string
}

/** Projection diagnostics, not an item validator or a repeat eligibility decision. */
export type ExplicitOptionProjection = {
  locale: Locale
  groups: ExplicitOptionGroup[]
  issues: DisplayIssue[]
}
