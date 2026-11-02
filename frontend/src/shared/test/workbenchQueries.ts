import { fireEvent, screen, within } from '@testing-library/react'

/** Explicitly target the preserved outer stash, separate from the new inner tabs. */
export const outerMaterialTab = (name: string) =>
  within(document.querySelector('.stash-heading') as HTMLElement).getByRole(
    'tab',
    { name },
  )

/** Currency application tests choose the main shelf; moved bones use the inner shelf. */
export function currencyButton(name: string) {
  const outer = document.getElementById('material-list')
  const primary = outer ? within(outer).queryByRole('button', { name }) : null
  if (primary) return primary
  return within(
    document.querySelector('.workbench-stash') as HTMLElement,
  ).getByRole('button', { name })
}

export const outerMaterialSearch = () =>
  within(document.getElementById('material-list')!).getByRole('searchbox')
export const textInputMethod = () =>
  screen.getByRole('dialog', { name: 'Starting item' })

export function selectSavedSession(index: number) {
  fireEvent.click(screen.getByRole('button', { name: 'Crafting session' }))
  const panel = document.getElementById('workbench-session-library')!
  fireEvent.click(
    within(panel).getAllByRole('button', { name: /^Select session:/ })[index]!,
  )
}
