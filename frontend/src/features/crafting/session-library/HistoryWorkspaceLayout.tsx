import type { ReactNode } from 'react'
import './session-library.css'
/** Personal sessions and public boards share the same rail, heading, list and preview grid. */
export function HistoryWorkspaceLayout({
  navigation,
  heading,
  beforeColumns,
  list,
  preview,
}: {
  navigation: ReactNode
  heading: ReactNode
  beforeColumns?: ReactNode
  list: ReactNode
  preview?: ReactNode
}) {
  return (
    <div className="wb-history-workspace">
      {navigation}
      <div className="wb-history-content">
        <header className="wb-history-heading">{heading}</header>
        {beforeColumns}
        <div
          className={
            'wb-history-columns' + (preview ? '' : ' wb-history-columns--empty')
          }
        >
          <div>{list}</div>
          {preview}
        </div>
      </div>
    </div>
  )
}
