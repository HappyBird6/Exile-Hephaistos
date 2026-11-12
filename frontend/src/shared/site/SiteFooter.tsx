import { useI18n } from '../i18n/i18n'
import './site-footer.css'

export function SiteFooter() {
  const { t } = useI18n()
  return (
    <footer className="craft-footer" role="contentinfo">
      <span>
        EXILE HEPHAISTOS <span aria-hidden="true">/</span>
        {t('ui.your_personal_crafting_workbench')}
      </span>
      <a href="https://poe2db.tw/us/Currency" target="_blank" rel="noreferrer">
        {t('ui.currency_images_poe2db')}
      </a>
    </footer>
  )
}
