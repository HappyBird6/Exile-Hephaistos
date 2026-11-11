import { Component } from 'react'
import type { PropsWithChildren } from 'react'
import { translate } from '../shared/i18n/i18n'

/** Covers a failed/retired lazy chunk after a deployment; reloading obtains the current entry. */
export class PageBoundary extends Component<
  PropsWithChildren,
  { failed: boolean }
> {
  state = { failed: false }
  static getDerivedStateFromError() {
    return { failed: true }
  }
  render() {
    if (!this.state.failed) return this.props.children
    return (
      <main role="alert">
        <p>{translate('error.load', { subject: 'Exile Hephaistos' })}</p>
        <button onClick={() => location.reload()}>
          {translate('ui.retry')}
        </button>
      </main>
    )
  }
}
