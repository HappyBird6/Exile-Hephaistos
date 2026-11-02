import sources from './baseImageSources.json'

const images = import.meta.glob<string>('./images/*.webp', {
  eager: true,
  query: '?url',
  import: 'default',
})

/** Existing source evidence, original art copyright retained by Grinding Gear Games. */
export function baseImageSource(baseItemId: string): string | undefined {
  const source = (sources as Record<string, { image: string; file?: string }>)[
    baseItemId
  ]
  // Uncached/denied source art stays a placeholder; do not retry third-party URLs at runtime.
  return source?.file
    ? images[`./images/${source.file}`]
    : source?.image.startsWith('/')
      ? source.image
      : undefined
}
