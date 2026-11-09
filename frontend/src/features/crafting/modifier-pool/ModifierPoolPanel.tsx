import { useQuery } from '@tanstack/react-query'
import { loadModifierPools } from '../workbenchReferenceApi'
import { referenceMessages } from '../referenceMessages'
import { concreteInitial } from '../workbenchApi'
import { Fragment, useId, useMemo, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { Initial } from '../craftingApi'
import type { ConcreteItem, WorkbenchAction } from '../workbenchApi'
import { localizedAction } from '../localizedCrafting'
import type { ModifierPoolMode } from './modifierPool'
import { localizedModifierText } from '../localizedModifiers'
import { modifierPoolSections, poolProbability } from './modifierPool'
import { modifierPoolMessages } from './modifierPoolMessages'
import './modifier-pool.css'

export function ModifierPoolPanel({
  catalog,
  baseName,
  itemLevel,
  loading,
  item,
}: {
  catalog: Initial | undefined
  baseName: string
  itemLevel: number
  loading: boolean
  item?: ConcreteItem | undefined
}) {
  const { locale } = useI18n()
  const text = modifierPoolMessages[locale]
  const titleId = useId()
  const reasonsText = referenceMessages[locale]
  const [query, setQuery] = useState('')
  const [mode, setMode] = useState<ModifierPoolMode>('dynamic')
  const state = item ?? (catalog ? concreteInitial(catalog) : undefined)
  const reference = useQuery({
    queryKey: [
      'crafting',
      'modifier-pools',
      catalog?.rulesetIdentity,
      state,
      mode,
    ],
    queryFn: ({ signal }) =>
      loadModifierPools(catalog!, state!, mode === 'dynamic', signal),
    enabled: !!catalog && !!state && !loading,
    retry: false,
    staleTime: 60_000,
  })
  const pending = loading || (!!catalog && reference.isPending)
  const sides = useMemo(
    () =>
      catalog && reference.data
        ? modifierPoolSections(catalog, reference.data).map((side) => ({
            ...side,
            groups: side.groups
              .map((group) => {
                const tierTexts = group.tiers.map((tier) =>
                  localizedModifierText(tier, undefined, locale),
                )
                return {
                  ...group,
                  label:
                    tierTexts[
                      Math.max(
                        0,
                        group.tiers.findIndex(
                          (tier) => group.eligibility[tier.id]!.eligible,
                        ),
                      )
                    ]!,
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
          }))
        : [],
    [catalog, locale, reference.data],
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
          <div
            className="modifier-pool__mode"
            role="group"
            aria-label={text.mode}
          >
            {(['dynamic', 'static'] as const).map((value) => (
              <button
                type="button"
                key={value}
                aria-pressed={mode === value}
                onClick={() => setMode(value)}
              >
                {text[value]}
              </button>
            ))}
          </div>
          <input
            className="modifier-pool__search"
            type="search"
            aria-label={text.search}
            placeholder={text.search}
            value={query}
            disabled={!catalog || pending || reference.isError}
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
      {pending || !catalog || reference.isError ? (
        <p className="modifier-pool__status" role="status">
          {pending ? text.loading : text.unavailable}
          {reference.isError && catalog && (
            <button type="button" onClick={() => void reference.refetch()}>
              {reasonsText.retry}
            </button>
          )}
        </p>
      ) : (
        <>
          <div className="modifier-pool__columns">
            {sides.map((side) => {
              const published =
                side.kind === 'ordinary' &&
                catalog.metadata.weightPolicy === 'POE2DB_AS_PUBLISHED'
              const unavailable =
                side.kind === 'essence' ? '—' : text.unpublished
              const category = side.kind === 'ordinary' ? '' : text[side.kind]
              const heading =
                side.affix === 'PREFIX' ? text.prefix : text.suffix
              const visible = side.groups.filter((group) =>
                group.search.includes(needle),
              )
              return (
                <Fragment key={`${side.kind}:${side.affix}`}>
                  {side.kind !== 'ordinary' && side.affix === 'PREFIX' && (
                    <div className="modifier-pool__category">
                      <h3>{category}</h3>
                      {side.kind === 'essence' && <p>{text.essenceNote}</p>}
                    </div>
                  )}
                  <section
                    key={side.affix}
                    className={`modifier-pool__column${side.affix === 'SUFFIX' ? ' modifier-pool__column--suffix' : ''}`}
                    aria-label={category ? `${category} ${heading}` : heading}
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
                          : unavailable}
                      </span>
                    </header>
                    <div className="modifier-pool__labels" aria-hidden="true">
                      <span>{text.modifier}</span>
                      <span>{text.weight}</span>
                      <span>{text.probability}</span>
                    </div>
                    {!visible.length && (
                      <p className="modifier-pool__status" role="status">
                        {side.groups.length
                          ? text.noMatch
                          : mode === 'dynamic' && item
                            ? text.dynamicEmpty
                            : text.empty}
                      </p>
                    )}
                    {visible.map((group) => {
                      const label = group.label
                      return (
                        <details
                          className={`modifier-pool__group${group.tiers.every((t) => !group.eligibility[t.id]!.eligible) ? ' modifier-pool__group--excluded' : ''}`}
                          key={group.id}
                        >
                          <summary>
                            <span className="modifier-pool__effect">
                              {label}
                              {group.tiers.every(
                                (t) => !group.eligibility[t.id]!.eligible,
                              ) && (
                                <small className="modifier-pool__reason-summary">
                                  ⊘{' '}
                                  {[
                                    ...new Set(
                                      group.tiers.flatMap(
                                        (t) => group.eligibility[t.id]!.reasons,
                                      ),
                                    ),
                                  ]
                                    .map((r) => reasonsText.reasons[r])
                                    .join(' · ')}
                                </small>
                              )}
                              <small>
                                {number.format(group.tiers.length)} {text.tiers}
                              </small>
                              {group.actions.length > 0 && (
                                <small>
                                  {group.actions
                                    .map((action) =>
                                      localizedAction(
                                        action as WorkbenchAction,
                                      ),
                                    )
                                    .join(' · ')}
                                </small>
                              )}
                            </span>
                            <span className="modifier-pool__weight">
                              <span className="sr-only">{text.weight} </span>
                              {published
                                ? number.format(group.weight)
                                : unavailable}
                            </span>
                            <span className="modifier-pool__chance">
                              <span className="sr-only">
                                {text.probability}{' '}
                              </span>
                              {published
                                ? probability(group.weight, side.totalWeight)
                                : unavailable}
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
                                  <tr
                                    key={tier.id}
                                    className={
                                      group.eligibility[tier.id]!.eligible
                                        ? ''
                                        : 'modifier-pool__tier--excluded'
                                    }
                                  >
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
                                      {!group.eligibility[tier.id]!
                                        .eligible && (
                                        <span
                                          className="modifier-pool__exclusion"
                                          tabIndex={0}
                                          title={group.eligibility[
                                            tier.id
                                          ]!.reasons.map(
                                            (r) => reasonsText.reasons[r],
                                          ).join(' · ')}
                                          aria-label={`${reasonsText.excluded}: ${group.eligibility[tier.id]!.reasons.map((r) => reasonsText.reasons[r]).join(' · ')}`}
                                        >
                                          <span aria-hidden="true">⊘</span>
                                          <span className="modifier-pool__reason">
                                            {group.eligibility[
                                              tier.id
                                            ]!.reasons.map(
                                              (r) => reasonsText.reasons[r],
                                            ).join(' · ')}
                                          </span>
                                        </span>
                                      )}
                                      <small>{tier.name}</small>
                                    </td>
                                    <td>
                                      {published
                                        ? number.format(tier.weight!)
                                        : unavailable}
                                    </td>
                                    <td>
                                      {published
                                        ? probability(
                                            group.eligibility[tier.id]!.eligible
                                              ? tier.weight!
                                              : 0,
                                            side.totalWeight,
                                          )
                                        : unavailable}
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
                </Fragment>
              )
            })}
          </div>
        </>
      )}
    </section>
  )
}
