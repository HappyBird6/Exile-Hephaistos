/** Per-account durable outbox/checkpoint. Large session baselines never consume localStorage quota. */
export async function syncStorage<T>(
  owner: string,
  value?: T,
): Promise<T | null> {
  if (!globalThis.indexedDB) {
    const key = `hephaistos.account.${owner}.sync`
    if (value !== undefined) localStorage.setItem(key, JSON.stringify(value))
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  }
  const db = await new Promise<IDBDatabase>((resolve, reject) => {
    const r = indexedDB.open('hephaistos-account-sync', 1)
    r.onupgradeneeded = () => r.result.createObjectStore('checkpoints')
    r.onsuccess = () => resolve(r.result)
    r.onerror = () => reject(r.error)
  })
  try {
    return await new Promise<T | null>((resolve, reject) => {
      const tx = db.transaction(
        'checkpoints',
        value === undefined ? 'readonly' : 'readwrite',
      )
      const store = tx.objectStore('checkpoints')
      const r = value === undefined ? store.get(owner) : store.put(value, owner)
      let result: T | null = value ?? null
      r.onsuccess = () => {
        if (value === undefined) result = (r.result as T | undefined) ?? null
      }
      tx.oncomplete = () => resolve(result)
      tx.onabort = () => reject(tx.error)
      tx.onerror = () => reject(tx.error)
    })
  } finally {
    db.close()
  }
}
