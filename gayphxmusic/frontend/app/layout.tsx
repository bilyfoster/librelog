import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { Toaster } from 'react-hot-toast'
import { SystemConfigProvider } from '../lib/contexts/SystemConfigContext'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'GayPHX Music Platform',
  description: 'Submit your music to GayPHX and get official ISRC codes',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <SystemConfigProvider>
          {children}
          <Toaster position="top-right" />
        </SystemConfigProvider>
      </body>
    </html>
  )
}

