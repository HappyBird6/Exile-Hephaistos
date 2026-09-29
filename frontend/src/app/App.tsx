import { useEffect } from 'react'
import { CraftingPage } from '../features/crafting/CraftingPage'

export function App() {
  useEffect(() => {
    if (window.location.pathname === '/admin/crawling') {
      window.history.replaceState(null, '', '/admin')
    }
  }, [])
  if (
    window.location.pathname === '/admin' ||
    window.location.pathname === '/admin/crawling'
  ) {
    return (
      <main>
        <nav aria-label="Main navigation">
          <a href="/">Crafting workbench</a>
        </nav>
        <h1>Admin</h1>
        <p>No admin tools are available yet.</p>
      </main>
    )
  }
  return <CraftingPage />
}
