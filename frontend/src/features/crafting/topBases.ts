import data from './topBases.json'

export type TopBaseKey = keyof typeof data
export function topBase(key: string) {
  return Object.hasOwn(data, key) ? data[key as TopBaseKey] : undefined
}
export function topBaseKey(id: string | undefined): TopBaseKey | undefined {
  return (Object.keys(data) as TopBaseKey[]).find((key) => data[key].id === id)
}
