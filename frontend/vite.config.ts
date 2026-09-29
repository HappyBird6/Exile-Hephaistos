import { readFileSync } from 'node:fs'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    environmentOptions: {
      jsdom: {
        html: readFileSync(new URL('./index.html', import.meta.url), 'utf8'),
      },
    },
    setupFiles: ['./src/shared/test/setup.ts'],
    clearMocks: true,
  },
})
