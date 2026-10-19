import { guideMessages } from './guideMessages'
import { planningNotes } from './planningNotes'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CurrencyPolicy } from './CurrencyPolicy'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { policyMessages } from './policyMessages'
import policy from '../../../../../backend/src/main/resources/crafting/automatic-currency-policy-v1.json'

it.each(locales)(
  'shows the entire fixed policy in %s without editable policy toggles',
  (locale) => {
    setLocale(locale)
    const show = vi
      .spyOn(HTMLDialogElement.prototype, 'showModal')
      .mockImplementation(function (this: HTMLDialogElement) {
        this.setAttribute('open', '')
      })
    const view = render(<CurrencyPolicy />)
    fireEvent.click(
      screen.getByRole('button', { name: policyMessages[locale][0]! }),
    )
    const dialog = screen.getByRole('dialog')
    expect(
      within(dialog).getByRole('heading', {
        name: guideMessages[locale].calculations,
      }),
    ).toBeVisible()
    expect(within(dialog).getByText(planningNotes[locale][2]!)).toBeVisible()
    expect(
      within(dialog).getByRole('heading', {
        name: guideMessages[locale].records,
      }),
    ).toBeVisible()
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(187)
    expect(within(dialog).queryByRole('checkbox')).not.toBeInTheDocument()
    expect(
      within(dialog).getByText(policyMessages[locale][18]!),
    ).toBeInTheDocument()
    expect(
      within(dialog).getByText(policyMessages[locale][19]!),
    ).toBeInTheDocument()
    expect(
      policy.entries.filter((e) => ['USE', 'CONDITIONAL'].includes(e.decision)),
    ).toHaveLength(51)
    expect(policy.entries.map((e) => e.number)).toEqual(
      Array.from({ length: 187 }, (_, i) => i + 1),
    )
    view.unmount()
    show.mockRestore()
  },
)
