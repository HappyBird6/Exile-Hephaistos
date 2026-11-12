import { act, fireEvent, screen, within } from '@testing-library/react'

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

export async function selectSavedSession(index: number) {
  await act(async () => {
    fireEvent.click(screen.getByRole('tab', { name: 'History' }))
  })
  const panel = document.getElementById('workbench-panel-history')!
  const buttons = await within(panel).findAllByRole('button', {
    name: /^Select session:/,
  })
  await act(async () => {
    fireEvent.click(buttons[index]!)
  })
  await act(async () => {
    fireEvent.click(
      screen.getByRole('button', { name: 'Continue in simulation' }),
    )
  })
}
