import { AdminCrawlingPage } from '../features/admin-crawling/AdminCrawlingPage'
import { CraftingPage } from '../features/crafting/CraftingPage'

export function App() {
  if (
    window.location.pathname === '/admin' ||
    window.location.pathname === '/admin/crawling'
  )
    return <AdminCrawlingPage />
  return <CraftingPage />
}
