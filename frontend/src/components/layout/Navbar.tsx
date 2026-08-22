'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import { usePathname } from 'next/navigation'
import { apiFetch, setAuthToken } from '@/lib/api'

const NAV_ITEMS = [
  { href: '/dashboard', label: 'Overview' },
  { href: '/quote-requests', label: 'Quotes' },
  { href: '/tasks', label: 'Tasks' },
  { href: '/tasks/calendar', label: 'Calendar' },
  { href: '/invoices', label: 'Invoices' },
  { href: '/categories', label: 'Categories' },
  { href: '/settings', label: 'Settings' },
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
      await apiFetch('/api/auth/logout', {
        method: 'POST',
      })
    } finally {
      setAuthToken(null)
      window.location.href = '/login'
    }
  }

  return (
    <header className="sticky top-0 z-40 border-b border-[#d9e3ec] bg-white/85 backdrop-blur-sm">
      <div className="mx-auto flex w-full max-w-7xl flex-col gap-3 px-3 py-3 md:px-5 lg:px-6">
        <div className="flex items-center justify-between gap-2">
          <div>
            <p className="text-base font-black tracking-[-0.05em] text-[#0f172a]">FMC</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="rounded-lg border border-[#d9e3ec] bg-[#f4f9fd] px-2.5 py-1.5 text-[10px] text-[#0f172a]">
              <span className="mr-1 text-[8px] font-black tracking-[0.14em] text-[#0f4c7a] uppercase">ID:</span>
              <span className="font-medium text-[#0f172a]">{currentEmail ?? '-'}</span>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="min-h-9 rounded-lg border border-[#bfd0df] bg-[#f8fbff] px-2.5 py-1.5 text-[10px] font-semibold text-[#0f172a] transition hover:border-[#9bb7d0] hover:bg-[#edf5fb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0f4c7a]"
            >
              ログアウト
            </button>
          </div>
        </div>

        <nav className="overflow-x-auto pb-0.5">
          <ul className="flex min-w-max items-center gap-1.5">
            {NAV_ITEMS.map((item) => {
              const active = isActive(item.href)
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={`flex min-h-8 items-center rounded-lg px-2.5 py-1.5 text-[10px] font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#0f4c7a] ${
                      active
                        ? 'bg-[#0f4c7a] text-white shadow-[0_10px_24px_rgba(15,76,122,0.22)]'
                        : 'bg-white text-[#0f172a] ring-1 ring-[#d9e3ec] hover:bg-[#f4f9fd] hover:text-[#0f4c7a]'
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
