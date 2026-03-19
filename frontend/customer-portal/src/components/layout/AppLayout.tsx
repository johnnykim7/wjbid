import { Outlet } from 'react-router-dom'
import { GlobalNav } from './GlobalNav'

export function AppLayout() {
  return (
    <div className="min-h-screen" style={{ background: '#f5f7fa' }}>
      <GlobalNav />
      <main>
        <Outlet />
      </main>
    </div>
  )
}
