import { useId, useState } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import type { WorkbenchResource } from '../stash/stashTypes'
import type { Film } from '../workbenchHistory'
import { sessionNotationMessages } from './messages'
import { extractSessionNotation } from './extractSessionNotation'
import './session-notation.css'

export type SessionNotationProps = {
  film: Film | null | undefined
  cursor: number
  /** Full display registry, including retained legacy resources and omens. */
  resources: readonly WorkbenchResource[]
}

function ResourceIcon({
  id,
  resource,
  omen = false,
}: {
  id: string
  resource: WorkbenchResource | undefined
  omen?: boolean
}) {
  const { locale, name } = useI18n()
  const text = sessionNotationMessages[locale]
  const [failedImage, setFailedImage] = useState<string | null>(null)
  const image = resource?.image
  const missing = !image || failedImage === image
  const label = resource
    ? name(id, resource.name)
    : omen
      ? text.unknownOmen
      : text.unknownCurrency
  return (
    <span
      className="wb-session-notation__icon"
      role="img"
      tabIndex={0}
      aria-label={`${label}${missing ? ` · ${text.imageUnavailable}` : ''}`}
      {...(resource ? { 'data-material-tooltip': id } : {})}
    >
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
}: SessionNotationProps) {
  const { locale } = useI18n()
  const text = sessionNotationMessages[locale]
  const headingId = useId()
  const scopeId = useId()
  const notation = extractSessionNotation(film, cursor)
  const registry = new Map(resources.map((resource) => [resource.id, resource]))
  return (
    <section
      className="wb-session-notation"
      aria-labelledby={headingId}
      aria-describedby={scopeId}
    >
      <h3 id={headingId}>{text.title}</h3>
      <p id={scopeId} className="wb-session-notation__scope">
        {text.scope}
      </p>
      {notation.steps.length === 0 ? (
        <p className="wb-session-notation__empty">{text.empty}</p>
      ) : (
        <ol className="wb-session-notation__steps">
          {notation.steps.map((step, index) => {
            const current = index === notation.steps.length - 1
            return (
              <li
                key={step.frameIndex}
                className="wb-session-notation__step"
                data-frame-index={step.frameIndex}
                aria-label={`${text.step} ${step.number}`}
                aria-current={current ? 'step' : undefined}
              >
                <span
                  className="wb-session-notation__number"
                  aria-hidden="true"
                >
                  {step.number}.
                </span>
                <span className="wb-session-notation__resources">
                  {step.consumedOmenIds.map((id) => (
                    <ResourceIcon
                      key={id}
                      id={id}
                      resource={registry.get(id)}
                      omen
                    />
                  ))}
                  {step.consumedOmenIds.length > 0 && (
                    <span aria-hidden="true">+</span>
                  )}
                  <ResourceIcon
                    id={step.currencyId}
                    resource={registry.get(step.currencyId)}
                  />
                </span>
                {!step.omensKnown && (
                  <span
                    className="wb-session-notation__incomplete"
                    aria-label={text.incomplete}
                  >
                    ?
                  </span>
                )}
                {current && (
                  <small className="wb-session-notation__current">
                    {text.current}
                  </small>
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
    </section>
  )
}
