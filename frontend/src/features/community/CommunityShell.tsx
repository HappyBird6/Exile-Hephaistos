import type { PropsWithChildren } from 'react'
import { AccountMenu } from '../account/AccountMenu'
import { useI18n } from '../../shared/i18n/i18n'
import { communityText } from './messages'
import './community.css'
export function CommunityShell({ children }: PropsWithChildren) {
  const { locale } = useI18n(),
    t = communityText(locale)
  return (
    <main className="craft-page craft-page--framed community-page">
      <header className="craft-header">
        <a className="craft-brand" href="/">
          EXILE <b>HEPHAISTOS</b>
        </a>
        <AccountMenu />
      </header>
      <nav className="community-nav">
        <a href="/">{t.back}</a>
        <a href="/community">{t.board}</a>
        <a href="/community/new">{t.newPost}</a>
      </nav>
      {children}
    </main>
  )
}
