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
  const groups: { title: string; lines: TextLine[] }[] = [
    {
      title: 'Raw properties',
      lines: item.properties.map((field) => field.source),
    },
    {
      title: 'Raw requirements',
      lines: item.requirements.map((field) => field.source),
    },
    {
      title: 'Modifier source text',
      lines: item.modifiers.map((mod) => mod.source),
    },
    { title: 'Raw flags', lines: item.flags },
    { title: 'Unresolved lines', lines: item.unparsedLines },
  ]
  return (
    <div className="parsed-item">
      <p className="detail-note">
        This is a draft read from text. The base and modifiers have not been
        verified against a catalog and cannot be used for crafting calculations.
      </p>

      {item.warnings.length > 0 && (
        <div className="parsed-group item-warnings">
          <h3>Review needed</h3>
          <ul>
            {item.warnings.map((warning, index) => (
              <li key={`${warning.code}-${index}`}>
                {warning.lineNumber > 0 && `Line ${warning.lineNumber}: `}
                {Object.hasOwn(warningMessages, warning.code)
                  ? warningMessages[warning.code]
                  : `Review required: ${warning.code}`}
              </li>
            ))}
          </ul>
        </div>
      )}
      <details className="parsed-evidence">
        <summary>Parsing details and source text</summary>
        <dl>
          <div>
            <dt>Base</dt>
            <dd>{item.displayBase ?? 'Unknown'}</dd>
          </div>
          <div>
            <dt>Rarity</dt>
            <dd>{item.rarityText}</dd>
          </div>
          <div>
            <dt>Text language</dt>
            <dd>English</dd>
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
                    <span className="source-line">{`Line ${line.number}`}</span>{' '}
                    {line.raw}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        <details>
          <summary>View full original text</summary>
          <pre className="item-raw">{item.text.originalText}</pre>
        </details>
      </details>
    </div>
  )
}
