import type { ReactNode } from 'react'
import { Navbar } from '@/components/layout/Navbar'

export default function DashboardLayout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-screen bg-slate-50">
      <Navbar />
      <div className="dashboard-surface">{children}</div>
    </div>
  )
}
