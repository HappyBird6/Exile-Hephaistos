/** Capture the rendered flow with its exact styles, images and wrapping. */
export async function snapshotPages(root: HTMLElement): Promise<Blob[]> {
  await document.fonts.ready
  for (const image of root.querySelectorAll('img')) image.loading = 'eager'
  await Promise.all(
    Array.from(root.querySelectorAll('img')).map((image) =>
      image.decode().catch(() => {}),
    ),
  )
  const bounds = root.getBoundingClientRect(),
    width = Math.ceil(bounds.right) - Math.floor(bounds.left),
    height = Math.ceil(bounds.bottom) - Math.floor(bounds.top)
  if (!width || !height || width * height > 100_000_000)
    throw Error('snapshot-size')
  let zoom = 1
  for (let node: HTMLElement | null = root; node; node = node.parentElement) {
    const factor = Number.parseFloat(getComputedStyle(node).zoom)
    if (Number.isFinite(factor) && factor > 0) zoom *= factor
  }
  const clone = root.cloneNode(true) as HTMLElement
  const sources = [root, ...root.querySelectorAll('*')],
    targets = [clone, ...clone.querySelectorAll('*')]
  for (let i = 0; i < sources.length; i++) {
    const source = sources[i]!,
      target = targets[i] as HTMLElement | SVGElement,
      computed = getComputedStyle(source)
    for (const property of computed) {
      if (
        /^(inset-|(?:min-|max-)?(?:block|inline)-size|(?:margin|padding|border)-(?:block|inline)|-webkit-)/.test(
          property,
        )
      )
        continue
      target.style.setProperty(property, computed.getPropertyValue(property))
    }
    if (source.matches('.wb-session-notation__resources')) {
      const rows = Array.from(source.children).map(
        (child) => child.getBoundingClientRect().top,
      )
      if (rows.every((top) => Math.abs(top - (rows[0] ?? top)) < 1))
        target.style.setProperty('flex-wrap', 'nowrap', 'important')
      target.style.setProperty(
        'min-width',
        `${source.getBoundingClientRect().width / zoom}px`,
        'important',
      )
    }
    if (source.matches('.wb-base-image')) {
      target.style.setProperty('flex-shrink', '0', 'important')
      target.style.setProperty(
        'min-height',
        `${source.getBoundingClientRect().height / zoom}px`,
        'important',
      )
    }
    if (source instanceof SVGSVGElement) {
      // Nested SVG rendering inside foreignObject is inconsistent in Chromium.
      // Rasterize this exact on-screen vector at its measured size before embedding.
      const vector = source.cloneNode(true) as SVGSVGElement
      const rect = source.getBoundingClientRect()
      const vectorWidth = rect.width / zoom,
        vectorHeight = rect.height / zoom
      vector.removeAttribute('style')
      vector.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
      vector.setAttribute('width', String(vectorWidth))
      vector.setAttribute('height', String(vectorHeight))
      const originalPaths = source.querySelectorAll('path')
      vector.querySelectorAll('path').forEach((path, index) => {
        const style = getComputedStyle(originalPaths[index]!)
        for (const key of [
          'fill',
          'stroke',
          'stroke-width',
          'stroke-linecap',
          'stroke-linejoin',
          'vector-effect',
        ])
          path.setAttribute(key, style.getPropertyValue(key))
      })
      const picture = new Image()
      picture.src =
        'data:image/svg+xml;charset=utf-8,' +
        encodeURIComponent(new XMLSerializer().serializeToString(vector))
      await picture.decode()
      const raster = document.createElement('canvas')
      raster.width = Math.ceil(vectorWidth * 2)
      raster.height = Math.ceil(vectorHeight * 2)
      const brush = raster.getContext('2d')
      if (!brush) throw Error('canvas')
      brush.scale(2, 2)
      brush.drawImage(picture, 0, 0, vectorWidth, vectorHeight)
      const replacement = document.createElement('img')
      replacement.style.cssText = target.style.cssText
      replacement.src = raster.toDataURL('image/png')
      target.replaceWith(replacement)
      targets[i] = replacement
    }
    if (
      source instanceof HTMLImageElement &&
      target instanceof HTMLImageElement
    ) {
      const raster = document.createElement('canvas')
      raster.width = source.naturalWidth
      raster.height = source.naturalHeight
      const brush = raster.getContext('2d')
      if (!brush || !raster.width || !raster.height)
        throw Error('snapshot-image')
      brush.drawImage(source, 0, 0)
      target.src = raster.toDataURL('image/png')
      target.removeAttribute('srcset')
      target.removeAttribute('loading')
    }
  }
  clone.style.width = `${bounds.width / zoom}px`
  clone.style.height = `${bounds.height / zoom}px`
  clone.style.zoom = String(zoom)
  clone.style.margin = '0'
  clone.style.transform = 'none'
  clone.setAttribute('xmlns', 'http://www.w3.org/1999/xhtml')
  const offsetX = bounds.left - Math.floor(bounds.left),
    offsetY = bounds.top - Math.floor(bounds.top)
  const markup = new XMLSerializer().serializeToString(clone),
    scale = Math.min(window.devicePixelRatio || 1, 2)
  const ends = new Map<string, number>()
  for (const node of root.querySelectorAll<HTMLElement>('[data-flow-row]')) {
    const row = node.dataset.flowRow!
    ends.set(
      row,
      Math.max(
        ends.get(row) ?? 0,
        node.getBoundingClientRect().bottom - bounds.top + offsetY,
      ),
    )
  }
  const limit = Math.min(
      12000,
      Math.floor(32_000_000 / (width * scale * scale)),
    ),
    cuts = [0]
  let start = 0,
    last = 0
  for (const bottom of [...ends.values(), height]) {
    if (bottom - start > limit) {
      if (last <= start) throw Error('snapshot-size')
      cuts.push(Math.ceil(last))
      start = Math.ceil(last)
      if (bottom - start > limit) throw Error('snapshot-size')
    }
    last = bottom
  }
  if (cuts.at(-1)! < height) cuts.push(height)
  const pages: Blob[] = []
  for (let i = 0; i < cuts.length - 1; i++) {
    const top = cuts[i]!,
      pageHeight = cuts[i + 1]! - top
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${pageHeight}"><rect width="100%" height="100%" fill="${getComputedStyle(root).backgroundColor}"/><foreignObject x="${offsetX}" y="${offsetY - top}" width="${bounds.width}" height="${bounds.height}">${markup}</foreignObject></svg>`
    const image = new Image()
    image.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg)
    await image.decode()
    const canvas = document.createElement('canvas')
    canvas.width = Math.ceil(width * scale)
    canvas.height = Math.ceil(pageHeight * scale)
    const context = canvas.getContext('2d')
    if (!context) throw Error('canvas')
    context.scale(scale, scale)
    context.drawImage(image, 0, 0)
    pages.push(
      await new Promise<Blob>((resolve, reject) =>
        canvas.toBlob(
          (blob) => (blob ? resolve(blob) : reject(Error('png'))),
          'image/png',
        ),
      ),
    )
  }
  return pages
}
