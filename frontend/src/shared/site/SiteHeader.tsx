import type { ReactNode } from 'react'
import { useI18n } from '../i18n/i18n'
import { LocaleSelector } from '../i18n/LocaleSelector'
import { AccountMenu } from '../../features/account/AccountMenu'
import { LeagueMark } from '../../features/crafting/currency-prices/LeagueMark'
import './site-controls.css'
/** Shared identity and header grid; each workspace owns its navigation and locks. */
export function SiteHeader({
  navigation,
  languageControls,
}: {
  navigation: ReactNode
  languageControls?: ReactNode
}) {
  const { t } = useI18n()
  return (
    <header className="craft-header">
      <div className="craft-header__identity">
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
        <LeagueMark />
        <AccountMenu />
      </div>
      {navigation}
      {languageControls ?? <LocaleSelector />}
    </header>
  )
}
