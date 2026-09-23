import { useI18n } from '../../shared/i18n/context'
import { locales } from '../../shared/i18n/messages'
import type { MessageKey } from '../../shared/i18n/messages'
import type { ParsedItemText, TextLine } from './itemTextApi'

const warningKeys: Record<string, MessageKey> = {
  UNSUPPORTED_RARITY: 'warningUnsupportedRarity',
  INVALID_ITEM_LEVEL: 'warningInvalidLevel',
  DUPLICATE_ITEM_LEVEL: 'warningDuplicateLevel',
  MISSING_ITEM_LEVEL: 'warningMissingLevel',
  UNRESOLVED_BASE: 'warningUnresolvedBase',
  UNPARSED_LINES: 'warningUnparsed',
  CATALOG_VALIDATION_REQUIRED: 'warningCatalog',
}

export function ParsedItemDetails({ item }: { item: ParsedItemText }) {
  const { t } = useI18n()
  const groups: { title: MessageKey; lines: TextLine[] }[] = [
    {
      title: 'properties',
      lines: item.properties.map((field) => field.source),
    },
    {
      title: 'requirements',
      lines: item.requirements.map((field) => field.source),
    },
    { title: 'markedModifiers', lines: item.markedModifiers },
    { title: 'flags', lines: item.flags },
    { title: 'unparsed', lines: item.unparsedLines },
  ]
  return (
    <div className="parsed-item">
      <p className="detail-note">{t('catalogDraft')}</p>
      <dl>
        <div>
          <dt>{t('itemClass')}</dt>
          <dd>{item.itemClass}</dd>
        </div>
        <div>
          <dt>{t('rarity')}</dt>
          <dd>{item.rarityText}</dd>
        </div>
        <div>
          <dt>{t('base')}</dt>
          <dd>{item.displayBase ?? t('unknown')}</dd>
        </div>
        <div>
          <dt>{t('itemLevel')}</dt>
          <dd>{item.itemLevel ?? t('unknown')}</dd>
        </div>
        <div>
          <dt>{t('sourceLanguage')}</dt>
          <dd>{locales[item.locale].label}</dd>
        </div>
      </dl>
      {groups
        .filter((group) => group.lines.length > 0)
        .map((group) => (
          <div className="parsed-group" key={group.title}>
            <h3>{t(group.title)}</h3>
            <ul>
              {group.lines.map((line) => (
                <li key={line.number}>
                  <span className="source-line">
                    {t('lineNumber', { line: line.number })}
                  </span>{' '}
                  {line.raw}
                </li>
              ))}
            </ul>
          </div>
        ))}
      {item.warnings.length > 0 && (
        <div className="parsed-group item-warnings">
          <h3>{t('warnings')}</h3>
          <ul>
            {item.warnings.map((warning, index) => (
              <li key={`${warning.code}-${index}`}>
                {warning.lineNumber > 0 &&
                  `${t('lineNumber', { line: warning.lineNumber })}: `}
                {t(
                  Object.hasOwn(warningKeys, warning.code)
                    ? warningKeys[warning.code]!
                    : 'warningUnknown',
                  { code: warning.code },
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      <details>
        <summary>{t('originalText')}</summary>
        <pre className="item-raw">{item.text.originalText}</pre>
      </details>
    </div>
  )
}
