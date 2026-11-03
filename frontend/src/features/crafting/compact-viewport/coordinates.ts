/** Display coordinates only. Currency IDs, actions and saved state stay intact. */
export const compactCurrencyRows = [
  [
    'Orb_of_Transmutation',
    'Greater_Orb_of_Transmutation',
    'Perfect_Orb_of_Transmutation',
    'Orb_of_Augmentation',
    'Greater_Orb_of_Augmentation',
    'Perfect_Orb_of_Augmentation',
    'Orb_of_Alchemy',
  ],
  [
    'Regal_Orb',
    'Greater_Regal_Orb',
    'Perfect_Regal_Orb',
    'Exalted_Orb',
    'Greater_Exalted_Orb',
    'Perfect_Exalted_Orb',
    'Vaal_Orb',
  ],
  [
    'Chaos_Orb',
    'Greater_Chaos_Orb',
    'Perfect_Chaos_Orb',
    'Orb_of_Annulment',
    'Fracturing_Orb',
    'Divine_Orb',
    'Hinekoras_Lock',
  ],
] as const

export const compactCurrencyPositions: Readonly<
  Record<string, { x: number; y: number }>
> = Object.fromEntries(
  compactCurrencyRows.flatMap((row, y) =>
    row.map((id, x) => [id, { x: 29 + x * 90, y: 40 + y * 100 }]),
  ),
)
