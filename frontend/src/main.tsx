import { PageBoundary } from './app/PageBoundary'
import { prepareLocale } from './shared/i18n/i18n'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import { AppProviders } from './app/AppProviders'
import './app/styles.css'

const root = document.getElementById('root')
if (!root) throw new Error('The application root element is missing.')

void prepareLocale().then(() =>
  createRoot(root).render(
    <StrictMode>
      <AppProviders>
        <PageBoundary>
          <App />
        </PageBoundary>
      </AppProviders>
    </StrictMode>,
  ),
)
