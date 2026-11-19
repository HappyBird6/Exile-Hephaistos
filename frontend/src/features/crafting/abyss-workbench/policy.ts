import bones from '../desecrationBones.json'
/** Selection policy only. Historical currency IDs and saved records stay valid. */
export const excludedWorkbenchBoneIds = Object.entries(bones)
  .filter(([, rule]) => !rule.selectable)
  .map(([id]) => id)

export function isExcludedWorkbenchBone(id: string): boolean {
  return excludedWorkbenchBoneIds.some((excluded) => excluded === id)
}
