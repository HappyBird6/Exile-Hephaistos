import type { ReactNode } from 'react'
import { useI18n } from '../../../shared/i18n/i18n'
import { AppLink } from '../../../shared/navigation/AppLink'
import { historyMessages } from './messages'
import './history.css'
import '../compact-viewport/mobileWorkbench.css'

export function SessionOverviewLayout({
  title,
  backHref,
  backLabel,
  actions,
  summary,
  children,
}: {
  title: string
  backHref: string
  backLabel: string
  actions?: ReactNode
  summary?: ReactNode
  children: ReactNode
}) {
  const { locale, t } = useI18n()
  const text = historyMessages[locale]
  return (
    <main className="craft-page craft-page--framed history-page">
      <header className="craft-header">
        <a
          className="craft-brand"
          href="/"
          aria-label={t('ui.exile_hephaistos_home')}
        >
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>{t('ui.path_of_exile_2_crafting_workbench')}</small>
          </span>
        </a>
        <div className="history-title-group">
          <span className="history-page-label">{text.title}</span>
          <h1 className="history-title" title={title}>
            {title}
          </h1>
        </div>
        <div className="history-actions">{actions} </div>
      </header>
      <div className="history-toolbar">
        <AppLink className="history-back" href={backHref}>
          <span aria-hidden="true">←</span> {backLabel}
        </AppLink>
        {summary}
      </div>
      {children}
    </main>
  )
}

export function SessionFlowSurface({ children }: { children: ReactNode }) {
  return (
    <div className="session-flow">
      <img
        className="session-flow-background"
        src="/assets/currency/stash-background.webp"
        alt=""
        aria-hidden="true"
      />
      {children}
    </div>
  )
}
