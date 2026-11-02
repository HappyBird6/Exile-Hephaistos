import type { MaterialTab } from '../materials'

export type WorkbenchResource = {
  id: string
  name: string
  image: string
  category: MaterialTab
}

export type StashPointer = { x: number; y: number }

export type WorkbenchStashProps = {
  resources: readonly WorkbenchResource[]
  excludedIds?: readonly string[] | ReadonlySet<string>
  activeTab: MaterialTab
  onTabChange: (tab: MaterialTab) => void
  selectedId: string | null
  activeOmenIds: readonly string[]
  /** Zero-based slots; only positions 0–9 are displayed. Caller owns storage. */
  favorites: readonly (WorkbenchResource | null)[]
  held: WorkbenchResource | null
  disabled?: boolean
  showLegacyOmens?: boolean
  onShowLegacyOmensChange?: (show: boolean) => void
  onSelect: (resource: WorkbenchResource, pointer?: StashPointer) => void
  onPickUp: (resource: WorkbenchResource, pointer?: StashPointer) => void
  /** Empty slots also activate, allowing the caller to cancel use selection. */
  onFavoriteActivate: (
    index: number,
    resource: WorkbenchResource | null,
    pointer?: StashPointer,
  ) => void
  onFavoritePlace: (index: number, resource: WorkbenchResource) => void
}
