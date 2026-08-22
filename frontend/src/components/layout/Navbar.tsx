'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { usePathname } from 'next/navigation'
import { apiFetch, setAuthToken } from '@/lib/api'
import { getCsrfToken } from '@/lib/csrf'

const NAV_ITEMS = [
  { href: '/dashboard', label: 'ダッシュボード' },
  { href: '/quote-requests', label: '見積依頼' },
  { href: '/tasks', label: 'タスク一覧' },
  { href: '/tasks/calendar', label: 'カレンダー' },
  { href: '/invoices', label: '請求書' },
  { href: '/categories', label: '依頼区分' },
  { href: '/settings', label: '設定' },
]

export function Navbar() {
  const pathname = usePathname()
  const [currentEmail, setCurrentEmail] = useState<string | null>(null)

  useEffect(() => {
    const loadCurrentUser = async () => {
      try {
        const response = await apiFetch('/api/auth/me', {
          method: 'GET',
        })

        if (!response.ok) {
          if (response.status === 401) {
            window.location.href = '/login'
            return
          }
          setCurrentEmail(null)
          return
        }

        const data = (await response.json()) as { email?: string }
        setCurrentEmail(data.email ?? null)
      } catch {
        setCurrentEmail(null)
      }
    }

    loadCurrentUser()
  }, [])

  const isActive = (href: string) => {
    if (href === '/dashboard') {
      return pathname === href
    }
    return pathname === href || pathname.startsWith(`${href}/`)
  }

  const handleLogout = async () => {
    try {
      const csrf = await getCsrfToken()
      await apiFetch('/api/auth/logout', {
        method: 'POST',
        headers: {
          [csrf.headerName]: csrf.token,
        },
      })
    } finally {
      setAuthToken(null)
      window.location.href = '/login'
    }
  }

  return (
    <header className="sticky top-0 z-40 border-b border-[#CBB9A8] bg-[#F7F2EE]/95 backdrop-blur">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-4 px-4 py-4 md:px-6 lg:px-8">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-xl font-bold tracking-tight text-[#1F271B]">FreelanceMusicCRM</p>
            <p className="text-xs font-medium text-[#0B4F6C]">案件管理ダッシュボード</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="rounded-xl border border-[#CBB9A8] bg-[#EFE4DB] px-3 py-2 text-sm text-[#1F271B]">
              <span className="mr-1 text-xs font-semibold text-[#0B4F6C]">ID:</span>
              <span className="font-medium text-[#1F271B]">{currentEmail ?? '-'}</span>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="min-h-12 rounded-xl border border-[#0B4F6C] bg-[#EAF1F6] px-4 py-2 text-sm font-semibold text-[#0B4F6C] transition hover:bg-[#D9E7F1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#145C9E]"
            >
              ログアウト
            </button>
          </div>
        </div>

        <nav className="overflow-x-auto pb-1">
          <ul className="flex min-w-max items-center gap-2">
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.href)
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={`flex min-h-12 items-center rounded-xl px-4 py-2 text-sm font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#145C9E] ${
                      active
                        ? 'bg-[#145C9E] text-white shadow-sm shadow-[#145C9E]/30'
                        : 'bg-white text-[#1F271B] ring-1 ring-[#CBB9A8] hover:bg-[#EFE4DB] hover:text-[#0B4F6C]'
                    }`}
                  >
                    {item.label}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>
      </div>
    </header>
  )
}
