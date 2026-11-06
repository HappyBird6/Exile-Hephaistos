import { useId, useMemo, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import { localizedModifierText } from '../localizedModifiers'
import { modifierPool, poolProbability } from './modifierPool'
import { modifierPoolMessages } from './modifierPoolMessages'
import './modifier-pool.css'

export function ModifierPoolPanel({
  catalog,
  baseName,
  itemLevel,
  loading,
}: {
  catalog: Initial | undefined
  baseName: string
  itemLevel: number
  loading: boolean
}) {
  const { locale } = useI18n()
  const text = modifierPoolMessages[locale]
  const titleId = useId()
  const [query, setQuery] = useState('')
  const published = catalog?.metadata.weightPolicy === 'POE2DB_AS_PUBLISHED'
  const sides = useMemo(
    () =>
      catalog
        ? modifierPool(Object.values(catalog.modifiers), itemLevel).map(
            (side) => ({
              ...side,
              groups: side.groups
                .map((group) => {
                  const tierTexts = group.tiers.map((tier) =>
                    localizedModifierText(tier, undefined, locale),
                  )
                  return {
                    ...group,
                    label: tierTexts[0]!,
                    tierTexts,
                    search: [
                      ...tierTexts,
                      ...group.tiers.flatMap((tier) => [tier.name, tier.text]),
                    ]
                      .join('\n')
                      .normalize('NFC')
                      .toLocaleLowerCase(locale),
                  }
                })
                .sort((a, b) => a.label.localeCompare(b.label, locale)),
            }),
          )
        : [],
    [catalog, itemLevel, locale],
  )
  const number = useMemo(() => new Intl.NumberFormat(locale), [locale])
  const percent = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        style: 'percent',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [locale],
  )
  function probability(weight: number, total: number) {
    const value = poolProbability(weight, total)
    return value > 0 && value < 0.0001
      ? `<${percent.format(0.0001)}`
      : percent.format(value)
  }
  const needle = query.normalize('NFC').trim().toLocaleLowerCase(locale)
  const source = catalog?.metadata.sourceUrl
  const sourceUrl =
    source && /^https:\/\/poe2db\.tw\//.test(source) ? source : null
  return (
    <section className="modifier-pool" aria-labelledby={titleId}>
      <header className="modifier-pool__header">
        <div>
          <h2 id={titleId}>{text.title}</h2>
          <div className="modifier-pool__context">
            <span>{baseName}</span>
            <span className="modifier-pool__level">
              {text.itemLevel} {number.format(itemLevel)}
            </span>
          </div>
        </div>
        <div className="modifier-pool__tools">
          <input
            className="modifier-pool__search"
            type="search"
            aria-label={text.search}
            placeholder={text.search}
            value={query}
            disabled={!catalog || loading}
            onChange={(event) => setQuery(event.target.value)}
          />
          {sourceUrl && (
            <a
              className="modifier-pool__source"
              href={sourceUrl}
              target="_blank"
              rel="noreferrer"
            >
              {text.source} <span aria-hidden="true">↗</span>
            </a>
          )}
        </div>
      </header>
      {loading || !catalog ? (
        <p className="modifier-pool__status" role="status">
          {loading ? text.loading : text.unavailable}
        </p>
      ) : (
        <>
          <p className="modifier-pool__note">
            {published ? text.basis : text.unpublishedNote}
          </p>
          <div className="modifier-pool__columns">
            {sides.map((side) => {
              const heading =
                side.affix === 'PREFIX' ? text.prefix : text.suffix
              const visible = side.groups.filter((group) =>
                group.search.includes(needle),
              )
              return (
                <section
                  key={side.affix}
                  className={`modifier-pool__column${side.affix === 'SUFFIX' ? ' modifier-pool__column--suffix' : ''}`}
                  aria-label={heading}
                >
                  <header className="modifier-pool__column-header">
                    <h3>
                      {heading}{' '}
                      <span className="modifier-pool__count">
                        {number.format(visible.length)}
                        {needle &&
                          ` / ${number.format(side.groups.length)}`}{' '}
                        {text.groups}
                      </span>
                    </h3>
                    <span className="modifier-pool__total">
                      {text.totalWeight}{' '}
                      {published
                        ? number.format(side.totalWeight)
                        : text.unpublished}
                    </span>
                  </header>
                  <div className="modifier-pool__labels" aria-hidden="true">
                    <span>{text.modifier}</span>
                    <span>{text.weight}</span>
                    <span>{text.probability}</span>
                  </div>
                  {!visible.length && (
                    <p className="modifier-pool__status" role="status">
                      {side.groups.length ? text.noMatch : text.empty}
                    </p>
                  )}
                  {visible.map((group) => {
                    const label = group.label
                    return (
                      <details className="modifier-pool__group" key={group.id}>
                        <summary>
                          <span className="modifier-pool__effect">
                            {label}
                            <small>
                              {number.format(group.tiers.length)} {text.tiers}
                            </small>
                          </span>
                          <span className="modifier-pool__weight">
                            <span className="sr-only">{text.weight} </span>
                            {published
                              ? number.format(group.weight)
                              : text.unpublished}
                          </span>
                          <span className="modifier-pool__chance">
                            <span className="sr-only">{text.probability} </span>
                            {published
                              ? probability(group.weight, side.totalWeight)
                              : text.unpublished}
                            {published && (
                              <span
                                className="modifier-pool__bar"
                                aria-hidden="true"
                              >
                                <span
                                  style={{
                                    width: `${poolProbability(group.weight, side.totalWeight) * 100}%`,
                                  }}
                                />
                              </span>
                            )}
                          </span>
                        </summary>
                        <div
                          className="modifier-pool__details wb-scrollbar"
                          tabIndex={0}
                          role="region"
                          aria-label={`${heading}: ${label}`}
                        >
                          <table className="modifier-pool__table">
                            <caption className="sr-only">{label}</caption>
                            <thead>
                              <tr>
                                {[
                                  text.tier,
                                  text.level,
                                  text.effect,
                                  text.weight,
                                  text.probability,
                                ].map((label) => (
                                  <th key={label} scope="col">
                                    {label}
                                  </th>
                                ))}
                              </tr>
                            </thead>
                            <tbody>
                              {group.tiers.map((tier, index) => (
                                <tr key={tier.id}>
                                  <td>
                                    {tier.tier > 0
                                      ? `T${number.format(tier.tier)}`
                                      : '—'}
                                  </td>
                                  <td>
                                    {number.format(tier.requiredItemLevel!)}
                                  </td>
                                  <td>
                                    {group.tierTexts[index]}
                                    <small>{tier.name}</small>
                                  </td>
                                  <td>
                                    {published
                                      ? number.format(tier.weight!)
                                      : text.unpublished}
                                  </td>
                                  <td>
                                    {published
                                      ? probability(
                                          tier.weight!,
                                          side.totalWeight,
                                        )
                                      : text.unpublished}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </details>
                    )
                  })}
                </section>
              )
            })}
          </div>
        </>
      )}
    </section>
  )
}
