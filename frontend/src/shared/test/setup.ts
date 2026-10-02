import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import { fixtureFetch } from './craftingFixtures'

beforeEach(() => {
  vi.stubGlobal('fetch', fixtureFetch)
  window.localStorage.removeItem('hephaistos.workbench.films.v1')
})
afterEach(() => vi.unstubAllGlobals())

afterEach(cleanup)

// jsdom does not implement the native dialog lifecycle.
HTMLDialogElement.prototype.showModal = function () {
  this.setAttribute('open', '')
}
HTMLDialogElement.prototype.close = function () {
  this.removeAttribute('open')
}
