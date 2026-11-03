import { readFileSync } from 'node:fs'
import { expect, it } from 'vitest'

it('stretches the full source image across the entire left viewport without cover cropping', () => {
  const style = document.createElement('style')
  style.textContent = readFileSync('src/features/crafting/crafting.css', 'utf8')
  const viewport = document.createElement('div')
  viewport.className = 'stash-viewport'
  document.head.append(style)
  document.body.append(viewport)
  try {
    const computed = getComputedStyle(viewport)
    expect(computed.alignSelf).toBe('stretch')
    expect(computed.backgroundImage).toContain('stash-background.webp')
    expect(computed.backgroundSize).toBe('100% 100%')
    expect(computed.backgroundRepeat).toBe('no-repeat')
  } finally {
    style.remove()
    viewport.remove()
  }
})
