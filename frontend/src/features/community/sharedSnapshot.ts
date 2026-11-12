import type { Film } from '../crafting/workbenchHistory'
import type { Options } from './api'
/** Same public fields as SharedSession: never send private clipboard provenance. */
export function publicFilm(film: Film): Film {
  return {
    id: film.id,
    createdAt: film.createdAt,
    frames: film.frames,
    ...(film.rulesetIdentity ? { rulesetIdentity: film.rulesetIdentity } : {}),
    ...(film.compatibilityId ? { compatibilityId: film.compatibilityId } : {}),
  }
}
export function exceedsShareLimit(film: Film, limits: Options['limits']) {
  return (
    film.frames.length > limits.frames ||
    new TextEncoder().encode(JSON.stringify(publicFilm(film))).byteLength >
      limits.sessionBytes
  )
}
