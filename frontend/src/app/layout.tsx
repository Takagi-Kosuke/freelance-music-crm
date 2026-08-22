import type { Metadata } from 'next'
import './globals.css'
import 'react-big-calendar/lib/css/react-big-calendar.css'

export const metadata: Metadata = {
  title: 'FreelanceMusicCRM',
  description: 'フリーランス音楽クリエイターの案件管理システム',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="ja">
      <body>{children}</body>
    </html>
  )
}
