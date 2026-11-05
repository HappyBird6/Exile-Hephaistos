import { chaosFirstAffixHints } from './chaosFirstAffixHints'
import { guideMessages } from './guideMessages'
import { planningNotes } from './planningNotes'
import { rarityUpgradeHints } from './rarityUpgradeHints'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { CurrencyPolicy } from './CurrencyPolicy'
import { locales, setLocale } from '../../../shared/i18n/i18n'
import { policyMessages } from './policyMessages'
import policy from '../../../../../backend/src/main/resources/crafting/automatic-currency-policy-v1.json'

it.each(locales)(
  'shows currencies and action conditions without development notes in %s without editable policy toggles',
  (locale) => {
    setLocale(locale)
    const show = vi
      .spyOn(HTMLDialogElement.prototype, 'showModal')
      .mockImplementation(function (this: HTMLDialogElement) {
        this.setAttribute('open', '')
      })
    const view = render(<CurrencyPolicy />)
    fireEvent.click(
      screen.getByRole('button', { name: guideMessages[locale].supported }),
    )
    const dialog = screen.getByRole('dialog')
    expect(
      within(dialog).queryByText(planningNotes[locale][1]!),
    ).not.toBeInTheDocument()
    expect(
      within(dialog).queryByText(policyMessages[locale][6]!),
    ).not.toBeInTheDocument()
    expect(
      within(dialog).queryByText(policyMessages[locale][20]!),
    ).not.toBeInTheDocument()
    expect(within(dialog).getAllByRole('listitem')).toHaveLength(187)
    expect(within(dialog).getByText(rarityUpgradeHints[locale])).toBeVisible()
    expect(within(dialog).queryByRole('checkbox')).not.toBeInTheDocument()
    expect(
      within(dialog).getByText(policyMessages[locale][18]!),
    ).toBeInTheDocument()
    expect(
      within(dialog).queryByText(policyMessages[locale][19]!),
    ).not.toBeInTheDocument()
    expect(
      policy.entries.filter((e) => ['USE', 'CONDITIONAL'].includes(e.decision)),
    ).toHaveLength(49)
    expect(within(dialog).getByText(chaosFirstAffixHints[locale])).toBeVisible()
    expect(policy.entries.map((e) => e.number)).toEqual(
      Array.from({ length: 187 }, (_, i) => i + 1),
    )
    view.unmount()
    show.mockRestore()
  },
)
