import type { Metadata } from 'next'
import { Manrope, Noto_Sans_JP } from 'next/font/google'
import './globals.css'
import 'react-big-calendar/lib/css/react-big-calendar.css'

const manrope = Manrope({
  subsets: ['latin'],
  weight: ['500', '600', '700'],
  variable: '--font-display',
  display: 'swap',
})

const notoSansJP = Noto_Sans_JP({
  subsets: ['latin'],
  weight: ['400', '500', '600', '700'],
  variable: '--font-sans',
  display: 'swap',
})

export const metadata: Metadata = {
  title: 'FMC',
  description: 'FMC CRM',
  icons: {
    icon: [
      { url: '/favicon.svg', type: 'image/svg+xml' },
      { url: '/favicon.svg' },
    ],
    shortcut: '/favicon.svg',
    apple: '/favicon.svg',
  },
  manifest: '/site.webmanifest',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ja" className={`${manrope.variable} ${notoSansJP.variable}`}>
      <body>{children}</body>
    </html>
  )
}
