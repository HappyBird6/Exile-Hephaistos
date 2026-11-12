import { afterEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import { AppLink } from './AppLink'
import { navigate, useAppLocation } from './navigation'
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  history.replaceState(null, '', '/')
})
function Example() {
  const path = useAppLocation()
  return (
    <>
      <output>{path}</output>
      <AppLink href="/community?selected=one">Board</AppLink>
      <AppLink href="/community/new" target="_blank">
        New tab
      </AppLink>
    </>
  )
}
it('changes the active view via history without a document navigation and reacts to browser history', () => {
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  render(<Example />)
  fireEvent.click(screen.getByRole('link', { name: 'Board' }))
  expect(screen.getByText('/community?selected=one')).toBeInTheDocument()
  act(() => {
    history.replaceState(null, '', '/?workbench=history')
    window.dispatchEvent(new PopStateEvent('popstate'))
  })
  expect(screen.getByText('/?workbench=history')).toBeInTheDocument()
  act(() => navigate('/community?page=2', { replace: true, scroll: false }))
  expect(screen.getByText('/community?page=2')).toBeInTheDocument()
})
it('does not intercept modified clicks or links intended for another browser tab', () => {
  render(<Example />)
  const push = vi.spyOn(history, 'pushState')
  fireEvent.click(screen.getByRole('link', { name: 'Board' }), {
    ctrlKey: true,
  })
  fireEvent.click(screen.getByRole('link', { name: 'New tab' }))
  expect(push).not.toHaveBeenCalled()
})
