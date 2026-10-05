import fs from 'node:fs'
import path from 'node:path'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const root = 'docs/evidence/belts-source-bundle-2026-10-05'
const walk = p => fs.readdirSync(p, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(p, e.name)) : [path.join(p, e.name)])
const files = walk(root).filter(p => p.endsWith('.html'))
for (const file of files) assert(fs.readFileSync(file).equals(execFileSync('git', ['show', `:${file.replaceAll('\\', '/')}`], { maxBuffer: 8 * 1024 * 1024 })), file)
console.log(`${files.length} staged raw source HTML files preserve exact captured bytes, including trailing whitespace`)
