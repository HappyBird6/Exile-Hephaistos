import type { AutomaticArrow } from './automaticApi'
export function removesModifier(method: AutomaticArrow) {
  return method.initial
    .concat(method.cycle)
    .some((a) => /CHAOS|ANNULMENT|PERFECT_ESSENCE/.test(a.currency))
}
