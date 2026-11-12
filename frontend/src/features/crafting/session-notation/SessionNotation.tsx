import {
  currencyRank,
  chanceText,
  customExaltMessages,
} from '../custom-exalt/messages'
import { workbenchCurrencyActions } from '../workbenchApi'
import '../custom-exalt/custom-exalt.css'
import { freshCraftMessages } from '../session-library/freshCraftMessages'
import { RevealIcon } from '../abyss-workbench/RevealIcon'
import { revealAction } from '../abyss-workbench/revealAction'
import { abyssWorkbenchMessages } from '../abyss-workbench/messages'
import { useId, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { WorkbenchResource } from '../stash/stashTypes'
import type { Film } from '../workbenchHistory'
import { sessionNotationMessages } from './messages'
import { historyMessages } from '../session-history/messages'
import { localHistoryUrl } from '../session-history/transfer'
import { extractSessionNotation } from './extractSessionNotation'
import type { NotationStep } from './extractSessionNotation'
import '../workbench-scrollbar.css'
import './session-notation.css'

export type SessionNotationProps = {
  film: Film | null | undefined
  cursor: number
  /** Full display registry, including retained legacy resources and omens. */
  resources: readonly WorkbenchResource[]
  onNavigate?: (frameIndex: number) => void
  onNewCraft?: () => void
  disabled?: boolean
  showOverview?: boolean
}

function ResourceIcon({
  id,
  resource,
  omen = false,
  focusable = true,
  custom = false,
}: {
  custom?: boolean
  id: string
  resource: WorkbenchResource | undefined
  omen?: boolean
  focusable?: boolean
}) {
  const { locale, name } = useI18n()
  const text = sessionNotationMessages[locale]
  const [failedImage, setFailedImage] = useState<string | null>(null)
  if (id === revealAction)
    return (
      <span
        className="wb-session-notation__icon"
        role="img"
        tabIndex={focusable ? 0 : undefined}
        aria-label={`${abyssWorkbenchMessages[locale].desecration} ${abyssWorkbenchMessages[locale].reveal}`}
      >
        <RevealIcon />
      </span>
    )
  const image = resource?.image
  const missing = !image || failedImage === image
  const label = resource
    ? name(id, resource.name)
    : omen
      ? text.unknownOmen
      : text.unknownCurrency
  return (
    <span
      className={`wb-session-notation__icon ${custom ? 'custom-currency-art' : ''}`}
      role="img"
      tabIndex={focusable ? 0 : undefined}
      aria-label={`${custom ? customExaltMessages[locale].title + ' · ' : ''}${label}${missing ? ` · ${text.imageUnavailable}` : ''}`}
      {...(resource ? { 'data-material-tooltip': id } : {})}
    >
      {custom && <b aria-hidden="true">C</b>}
      {missing ? (
        <span aria-hidden="true">◇</span>
      ) : (
        <img
          src={image}
          alt=""
          draggable="false"
          onError={() => setFailedImage(image)}
        />
      )}
    </span>
  )
}

/** An informational view; the parent alone owns application, cursor and storage. */
export function SessionNotation({
  film,
  cursor,
  resources,
  onNavigate,
  onNewCraft,
  disabled = false,
  showOverview = true,
}: SessionNotationProps) {
  const { locale, name } = useI18n()
  const text = sessionNotationMessages[locale]
  const headingId = useId()
  const notation = extractSessionNotation(film, cursor)
  const registry = new Map(resources.map((resource) => [resource.id, resource]))
  const currentFrameIndex = notation.steps.reduce(
    (current, step) => (step.frameIndex <= cursor ? step.frameIndex : current),
    -1,
  )
  return (
    <section className="wb-session-notation" aria-labelledby={headingId}>
      <div className="wb-session-notation__heading">
        <h3 id={headingId}>{text.title}</h3>
        {film && showOverview && (
          <a
            className="session-overview-entry"
            href={localHistoryUrl(film.id, 'simulation')}
          >
            {historyMessages[locale].title} <span aria-hidden="true">↗</span>
          </a>
        )}
      </div>
      {notation.steps.length === 0 ? (
        <p className="wb-session-notation__empty">{text.empty}</p>
      ) : (
        <ol className="wb-session-notation__steps wb-scrollbar">
          {notation.steps.map((step) => {
            const current = step.frameIndex === currentFrameIndex
            const uses = text.repeatUses(
              new Intl.NumberFormat(locale).format(step.uses),
            )
            const label = `${text.step} ${step.number}${
              step.kind === 'reveal'
                ? ` · ${abyssWorkbenchMessages[locale].desecration} ${abyssWorkbenchMessages[locale].reveal}`
                : step.repeated
                  ? ` · ${name(step.currencyId, registry.get(step.currencyId)?.name ?? text.unknownCurrency)} · ${uses}`
                  : ''
            }`
            const content = (
              <NotationResources
                step={step}
                resources={resources}
                focusable={!onNavigate}
              />
            )
            return (
              <li
                key={step.frameIndex}
                className="wb-session-notation__step"
                data-frame-index={step.frameIndex}
                aria-label={label}
                aria-current={current ? 'step' : undefined}
              >
                {onNavigate ? (
                  <button
                    type="button"
                    className="wb-session-notation__jump"
                    aria-label={label}
                    aria-current={current ? 'step' : undefined}
                    data-material-tooltip={
                      registry.has(step.currencyId)
                        ? step.currencyId
                        : undefined
                    }
                    disabled={disabled}
                    onClick={() => onNavigate(step.frameIndex)}
                  >
                    {content}
                  </button>
                ) : (
                  content
                )}
              </li>
            )
          })}
        </ol>
      )}
      {notation.omittedFrames > 0 && (
        <p className="wb-session-notation__limitation">{text.omitted}</p>
      )}
      {notation.incompleteOmens && (
        <p className="wb-session-notation__limitation">{text.omensMissing}</p>
      )}
      {film && onNewCraft && (
        <button
          type="button"
          className="wb-session-notation__new-craft"
          disabled={disabled}
          onClick={onNewCraft}
        >
          {freshCraftMessages[locale].fromCurrent}
        </button>
      )}
    </section>
  )
}

export function NotationResources({
  step,
  resources,
  focusable = true,
}: {
  step: NotationStep
  focusable?: boolean
  resources: readonly WorkbenchResource[]
}) {
  const { locale } = useI18n()
  const text = sessionNotationMessages[locale]
  const registry = new Map(resources.map((r) => [r.id, r]))
  const uses = text.repeatUses(new Intl.NumberFormat(locale).format(step.uses))
  return (
    <>
      <span className="wb-session-notation__number" aria-hidden="true">
        {step.number}.
      </span>
      <span className="wb-session-notation__resources">
        {step.consumedOmenIds.map((id, index) => (
          <ResourceIcon
            key={`${id}-${index}`}
            id={id}
            resource={registry.get(id)}
            omen
            focusable={focusable}
          />
        ))}
        {step.consumedOmenIds.length > 0 && <span aria-hidden="true">+</span>}
        <span className="wb-session-notation__currency">
          <ResourceIcon
            custom={!!step.customExalt}
            id={step.currencyId}
            resource={registry.get(step.currencyId)}
            focusable={focusable}
          />
          {currencyRank(workbenchCurrencyActions[step.currencyId]) && (
            <span className="wb-session-notation__rank" aria-hidden="true">
              {currencyRank(workbenchCurrencyActions[step.currencyId])}
            </span>
          )}
        </span>
        {step.customExalt && (
          <span
            className="wb-session-notation__chance"
            aria-label={customExaltMessages[locale].chance}
          >
            {chanceText(step.customExalt.probability, locale)}
          </span>
        )}
        {step.repeated && (
          <span className="wb-session-notation__uses">{uses}</span>
        )}
      </span>
      {!step.omensKnown && (
        <span
          className="wb-session-notation__incomplete"
          aria-label={text.incomplete}
        >
          ?
        </span>
      )}
    </>
  )
}
