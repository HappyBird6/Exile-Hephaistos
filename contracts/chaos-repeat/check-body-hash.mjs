import { readFileSync } from 'node:fs'
import { createHash } from 'node:crypto'

const bytes = readFileSync(new URL('./request-body.json', import.meta.url))
const expected = readFileSync(new URL('./request-body.sha256', import.meta.url), 'utf8').trim()
JSON.parse(bytes.toString('utf8'))
const actual = createHash('sha256').update(bytes).digest('hex')
if (actual !== expected) throw new Error('Shared UTF-8 body digest mismatch')
console.log(actual)
