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
  selectedId: string | null
  activeOmenIds: readonly string[]
  held: WorkbenchResource | null
  disabled?: boolean
  showLegacyOmens?: boolean
  onShowLegacyOmensChange?: (show: boolean) => void
  onSelect: (resource: WorkbenchResource, pointer?: StashPointer) => void
  onPickUp: (resource: WorkbenchResource, pointer?: StashPointer) => void
}

export type WorkbenchFavoritesProps = Pick<
  WorkbenchStashProps,
  'excludedIds' | 'selectedId' | 'activeOmenIds' | 'held' | 'disabled'
> & {
  /** Zero-based slots; caller owns storage and preserves any retained entries. */
  favorites: readonly (WorkbenchResource | null)[]
  /** Empty slots also activate, allowing the caller to cancel use selection. */
  onFavoriteActivate: (
    index: number,
    resource: WorkbenchResource | null,
    pointer?: StashPointer,
  ) => void
  onFavoritePlace: (index: number, resource: WorkbenchResource) => void
}
