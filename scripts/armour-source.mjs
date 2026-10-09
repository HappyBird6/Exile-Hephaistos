import crypto from 'node:crypto'
import assert from 'node:assert/strict'

export const sourceLocales = ['us', 'kr', 'jp', 'cn', 'tw', 'sp']
export const cleanSource = s => s.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').trim()
export const sourceProperties = card => Object.fromEntries(['Armour', 'Evasion Rating', 'Energy Shield', 'Base Movement Speed'].flatMap(label => {
  const match = card.match(new RegExp(`${label}: (-?[\\d.]+)`))
  return match ? [[label, Number(match[1])]] : []
}))
export async function collectArmourSource(slug, locale) {
  const url = `https://poe2db.tw/${locale}/${slug}`
  const response = await fetch(url)
  assert(response.ok, `${url}: HTTP ${response.status}`)
  const html = await response.text()
  const popup = html.match(/<div class="newItemPopup NormalPopup[^]*?(?=<div class="itemboximage")/)?.[0]
  assert(popup, `${slug}/${locale}: missing normal popup`)
  const fields = Object.fromEntries([...html.matchAll(/<tr><td>([^]*?)<\/td><td>([^]*?)<\/td><\/tr>/g)].map(m => [cleanSource(m[1]), cleanSource(m[2])]))
  const record = { url, retrievedAt: new Date().toISOString(), sha256: crypto.createHash('sha256').update(html).digest('hex'), name: cleanSource(popup.match(/<span class="lc">([^]*?)<\/span>/)?.[1] ?? ''), card: cleanSource(popup), requirements: cleanSource(popup.match(/<div class="requirements">([^]*?)<\/div>/)?.[1] ?? ''), fields }
  assert(record.name && fields.Type, `${slug}/${locale}: incomplete identity`)
  return record
}
