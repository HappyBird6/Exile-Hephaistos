import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    include: ['acceptance/path-search-runtime.ts'],
    testTimeout: 60000,
    maxWorkers: 1,
  },
})
