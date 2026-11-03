import { fireEvent, screen, within } from '@testing-library/react'

/** The single material selector now lives inside the canvas right panel. */
export const outerMaterialTab = (name: string) =>
  within(
    screen.getByRole('tablist', { name: 'Material stash tabs' }),
  ).getByRole('tab', { name })

/** Fixed currencies live on the left; additional currencies and bones on the right. */
export function currencyButton(name: string) {
  const fixed = document.querySelector('.currency-catalog')
  const primary = fixed
    ? within(fixed as HTMLElement).queryByRole('button', { name })
    : null
  if (primary) return primary
  return within(document.getElementById('material-list')!).getByRole('button', {
    name,
  })
}

export const outerMaterialSearch = () =>
  within(document.getElementById('material-list')!).getByRole('searchbox')
export const textInputMethod = () =>
  screen.getByRole('dialog', { name: 'Starting item' })

export function selectSavedSession(index: number) {
  fireEvent.click(screen.getByRole('button', { name: 'History' }))
  const panel = document.getElementById('workbench-session-library')!
  fireEvent.click(
    within(panel).getAllByRole('button', { name: /^Select session:/ })[index]!,
  )
}
