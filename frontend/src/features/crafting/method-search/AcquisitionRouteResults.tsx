import { useQuery } from '@tanstack/react-query'
import { useRef, useState } from 'react'
import { formatNumber, gameName, useI18n } from '../../../shared/i18n/i18n'
import { materialNames } from '../catalogData'
import type { Initial } from '../craftingApi'
import {
  workbenchActionNames,
  type WorkbenchAction,
} from '../currencyDefinitions'
import { GoalFilterApiError } from '../goal-filter/api'
import { craftStartMessages } from '../goal-filter/craftStartMessages'
import { localizedAction } from '../localizedCrafting'
import { localizedModifierText } from '../localizedModifiers'
import { pathTreeMessages } from '../path-tree/messages'
import type { CreateRequest } from '../path-tree/types'
import { SessionListCard } from '../session-library/SessionListCard'
import { socketText } from '../socketAugments'
import { SessionPlansLayout } from './RankedCraftingSessions'
import {
  loadAcquisitionRoutes,
  type AcquisitionResult,
  type AcquisitionRoute,
  type AcquisitionStep,
} from './acquisitionRouteApi'
import { acquisitionRouteMessages } from './acquisitionRouteMessages'
import { graphStateMessages } from './graphStateMessages'
import { policyCandidateMessages } from './policyCandidateMessages'

type Props = {
  request: CreateRequest
  definitions: Initial['modifiers']
  baseName: string
  active: boolean
}

function materialLabel(id: string) {
  if (Object.hasOwn(workbenchActionNames, id))
    return localizedAction(id as WorkbenchAction)
  return gameName(id, materialNames[id] ?? id.replaceAll('_', ' '))
}

function stepLabel(step: AcquisitionStep) {
  const materials = [step.action, ...step.omens].map(materialLabel).join(' + ')
  return step.socketIndex == null
    ? materials
    : `${materials} (${socketText().socket} ${step.socketIndex + 1})`
}

export function AcquisitionRouteResults({
  request,
  definitions,
  baseName,
  active,
}: Props) {
  const { locale } = useI18n()
  const messages = pathTreeMessages[locale]
  const query = useQuery({
    queryKey: [
      'support-test-routes',
      request.start.provenance.rulesetIdentity,
      request.start.item,
      request.goal,
    ],
    queryFn: ({ signal }) => loadAcquisitionRoutes(request, signal),
    enabled: active,
    staleTime: Infinity,
    retry: false,
  })
  if (query.isError) {
    const changed =
      query.error instanceof GoalFilterApiError &&
      query.error.issues.some(
        (issue) => issue.code === 'RULESET_IDENTITY_MISMATCH',
      )
    return (
      <p role="alert">
        {changed ? messages.changed : messages.error}{' '}
        <button type="button" onClick={() => void query.refetch()}>
          {craftStartMessages[locale].retry}
        </button>
      </p>
    )
  }
  if (!query.data) return <p role="status">{messages.calculating}</p>
  return (
    <RouteList
      key={request.clientRequestId}
      result={query.data}
      definitions={definitions}
      baseId={request.start.item.baseItemId}
      baseName={baseName}
    />
  )
}

function RouteList({
  result,
  definitions,
  baseId,
  baseName,
}: {
  result: AcquisitionResult
  definitions: Initial['modifiers']
  baseId: string
  baseName: string
}) {
  const { locale } = useI18n()
  const copy = acquisitionRouteMessages[locale]
  const messages = pathTreeMessages[locale]
  const changes = graphStateMessages[locale]
  const section = useRef<HTMLElement>(null)
  const [selection, setSelection] = useState(0)
  const selected = result.routes[selection] ?? result.routes[0]
  if (!selected)
    return (
      <p role="status">{result.complete ? copy.empty : messages.unknown}</p>
    )

  const card = result.cards[selected.cardIndex]!
  const title = (route: AcquisitionRoute) => {
    const cardName = copy.card.replace('{n}', String(route.cardIndex + 1))
    if (route.steps.length === 0) return cardName + ' · ' + messages.achieved
    return [cardName, route.order.join(' → ')].filter(Boolean).join(' · ')
  }
  const chance = (route: AcquisitionRoute) =>
    route.probability == null
      ? policyCandidateMessages[locale].unresolved
      : formatNumber(
          route.probability,
          { style: 'percent', maximumSignificantDigits: 4 },
          locale,
        )
  return (
    <>
      {!result.complete && <p role="status">{messages.partial}</p>}
      <SessionPlansLayout
        sectionRef={section}
        detail={
          <section
            className="wb-history-preview session-plan-steps"
            aria-label={copy.order}
          >
            <div className="wb-history-preview__heading">
              <div className="wb-history-title">
                <h3>{title(selected)}</h3>
              </div>
            </div>
            <p>
              {copy.chance} · {chance(selected)}
            </p>
            {card.options.length > 0 && (
              <dl>
                {card.options.map((option) => (
                  <div key={option.number}>
                    <dt>{option.number}</dt>
                    <dd>
                      {option.modifierIds
                        .map((id) =>
                          definitions[id]
                            ? localizedModifierText(
                                definitions[id],
                                undefined,
                                locale,
                              )
                            : id,
                        )
                        .join(' / ')}
                    </dd>
                  </div>
                ))}
              </dl>
            )}
            {selected.steps.length > 0 && (
              <ol>
                {selected.steps.map((step, index) => (
                  <li key={index}>
                    <strong>{stepLabel(step)}</strong>
                    {step.lost.length > 0 && (
                      <>
                        {' '}
                        · {changes.removed} {step.lost.join(', ')}
                      </>
                    )}
                    {step.acquired.length > 0 && (
                      <>
                        {' '}
                        · {changes.added} {step.acquired.join(', ')}
                      </>
                    )}
                  </li>
                ))}
              </ol>
            )}
          </section>
        }
      >
        {result.routes.map((route, index) => (
          <div role="listitem" key={index}>
            <SessionListCard
              title={title(route)}
              baseId={baseId}
              baseName={baseName}
              selected={selected === route}
              selectLabel={`${index + 1}. ${title(route)} · ${chance(route)}`}
              metadata={
                <small>
                  {chance(route)}
                  {route.steps.length > 0 &&
                    ' · ' + route.steps.map(stepLabel).join(' → ')}
                </small>
              }
              onSelect={() => {
                setSelection(index)
                if (window.innerWidth <= 760)
                  section.current
                    ?.querySelector('.wb-history-preview')
                    ?.scrollIntoView?.({ block: 'nearest' })
              }}
            />
          </div>
        ))}
      </SessionPlansLayout>
    </>
  )
}
