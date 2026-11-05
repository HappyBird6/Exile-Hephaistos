/** Text comes from the read-only rendered cards; never execute imported markup. */
export async function snapshotPages(
  root: HTMLElement,
  title: string,
): Promise<Blob[]> {
  await document.fonts.ready
  const texts = Array.from(
    root.querySelectorAll<HTMLElement>('[data-snapshot-card]'),
  ).map(
    (node) =>
      Array.from(
        node.querySelectorAll<HTMLElement>('.wb-session-notation__icon'),
      )
        .map((icon) => icon.getAttribute('aria-label'))
        .join(' + ') +
      '\n' +
      node.innerText,
  )
  const canvas = document.createElement('canvas')
  canvas.width = 1680
  const context = canvas.getContext('2d')
  if (!context) throw Error('canvas')
  context.font = '16px sans-serif'
  const wrap = (text: string) =>
    text.split('\n').flatMap((line) => {
      const result: string[] = []
      let current = ''
      for (const ch of line) {
        if (context.measureText(current + ch).width > 350) {
          result.push(current)
          current = ''
        }
        current += ch
      }
      result.push(current)
      return result
    })
  const cards = texts.map(wrap)
  const totalPixels = cards.reduce(
    (sum, _, index) =>
      index % 4
        ? sum
        : sum +
          1680 *
            (Math.max(
              ...cards
                .slice(index, index + 4)
                .map((lines) => lines.length * 23 + 90),
            ) +
              110),
    0,
  )
  if (totalPixels > 100_000_000) throw Error('snapshot-size')
  const pages: Blob[] = []
  // A complete zigzag row per page keeps allocation bounded and never truncates cards.
  for (let start = 0; start < cards.length; start += 4) {
    const row = cards.slice(start, start + 4)
    const height = Math.max(...row.map((lines) => lines.length * 23 + 90))
    if (height > 15000) throw Error('snapshot-size')
    canvas.height = height + 110
    context.fillStyle = '#10151c'
    context.fillRect(0, 0, canvas.width, canvas.height)
    context.font = '20px sans-serif'
    context.fillStyle = '#e9d5a0'
    context.fillText(
      `${title} · ${start + 1}–${start + row.length} / ${cards.length}`,
      30,
      35,
    )
    row.forEach((lines, index) => {
      const column = Math.floor(start / 4) % 2 ? 3 - index : index
      const x = 30 + column * 415
      context.fillStyle = '#202936'
      context.fillRect(x, 65, 375, height)
      context.font = '16px sans-serif'
      context.fillStyle = '#f0eee6'
      lines.forEach((line, i) => context.fillText(line, x + 12, 92 + i * 23))
      if (index < row.length - 1)
        context.fillText(
          Math.floor(start / 4) % 2 ? '←' : '→',
          Math.floor(start / 4) % 2 ? x - 28 : x + 382,
          105,
        )
    })
    if (start + 4 < cards.length)
      context.fillText('↓', Math.floor(start / 4) % 2 ? 35 : 1640, height + 85)
    const blob = await new Promise<Blob>((resolve, reject) =>
      canvas.toBlob(
        (b) => (b ? resolve(b) : reject(Error('png'))),
        'image/png',
      ),
    )
    pages.push(blob)
  }
  return pages
}
