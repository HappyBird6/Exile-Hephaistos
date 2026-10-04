import { useI18n, uiText } from '../../shared/i18n/i18n'
import type { Item, TextLine } from './itemModels'

const warningMessages: Record<string, string> = {
  MISSING_ITEM_CLASS: 'The item class is missing and requires catalog lookup.',
  UNSUPPORTED_RARITY: 'This rarity is not supported.',
  INVALID_ITEM_LEVEL: 'The item level could not be read.',
  DUPLICATE_ITEM_LEVEL: 'Multiple item levels were found.',
  MISSING_ITEM_LEVEL: 'The item level is missing.',
  UNRESOLVED_BASE: 'The base name could not be resolved.',
  UNPARSED_LINES: 'Unresolved lines have been preserved unchanged.',
  CATALOG_VALIDATION_REQUIRED:
    'Verification against a validated catalog is required.',
}

export function ParsedItemDetails({ item }: { item: Item }) {
  const { t } = useI18n()
  const groups: { title: string; lines: TextLine[] }[] = [
    {
      title: t('parser.raw_properties'),
      lines: item.properties.map((field) => field.source),
    },
    {
      title: t('parser.raw_requirements'),
      lines: item.requirements.map((field) => field.source),
    },
    {
      title: t('parser.modifier_source'),
      lines: item.modifiers.map((mod) => mod.source),
    },
    { title: t('parser.raw_flags'), lines: item.flags },
    { title: t('parser.unresolved'), lines: item.unparsedLines },
  ]
  return (
    <div className="parsed-item">
      <p className="detail-note">{t('notice.parsed_draft')}</p>

      {item.warnings.length > 0 && (
        <div className="parsed-group item-warnings">
          <h3>{t('ui.review_needed')}</h3>
          <ul>
            {item.warnings.map((warning, index) => (
              <li key={`${warning.code}-${index}`}>
                {warning.lineNumber > 0 && `Line ${warning.lineNumber}: `}
                {Object.hasOwn(warningMessages, warning.code)
                  ? uiText(warningMessages[warning.code]!)
                  : `Review required: ${warning.code}`}
              </li>
            ))}
          </ul>
        </div>
      )}
      <details className="parsed-evidence">
        <summary>{t('ui.parsing_details_and_source_text')}</summary>
        <dl>
          <div>
            <dt>{t('ui.base')}</dt>
            <dd>{item.displayBase ?? t('ui.unknown')}</dd>
          </div>
          <div>
            <dt>{t('ui.rarity')}</dt>
            <dd>{item.rarityText}</dd>
          </div>
          <div>
            <dt>{t('ui.text_language')}</dt>
            <dd>{t('ui.english')}</dd>
          </div>
        </dl>
        {groups
          .filter((group) => group.lines.length > 0)
          .map((group) => (
            <div className="parsed-group" key={group.title}>
              <h3>{group.title}</h3>
              <ul>
                {group.lines.map((line) => (
                  <li key={line.number}>
                    <span className="source-line">
                      {t('parser.line', { index: line.number })}
                    </span>{' '}
                    {line.raw}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        <details>
          <summary>{t('ui.view_full_original_text')}</summary>
          <pre className="item-raw">{item.text.originalText}</pre>
        </details>
      </details>
    </div>
  )
}
