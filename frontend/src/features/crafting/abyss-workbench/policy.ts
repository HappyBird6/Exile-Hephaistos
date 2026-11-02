/** Selection policy only. Historical currency IDs and saved records stay valid. */
export const excludedWorkbenchBoneIds = [
  'Gnawed_Rib',
  'Gnawed_Jawbone',
  'Gnawed_Collarbone',
] as const

export function isExcludedWorkbenchBone(id: string): boolean {
  return excludedWorkbenchBoneIds.some((excluded) => excluded === id)
}
