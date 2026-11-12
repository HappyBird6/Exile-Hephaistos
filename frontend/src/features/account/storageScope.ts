export function scopedStorage(
  storage: Pick<Storage, 'getItem' | 'setItem'>,
  scope: string | null = null,
) {
  const key = (name: string) =>
    scope ? `hephaistos.account.${scope}:${name}` : name
  return {
    getItem: (name: string) => storage.getItem(key(name)),
    setItem: (name: string, value: string) => storage.setItem(key(name), value),
  }
}
