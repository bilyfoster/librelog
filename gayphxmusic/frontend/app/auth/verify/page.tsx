'use client'

import { useEffect, useState } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'
import Link from 'next/link'
import { api } from '@/lib/api'
import toast from 'react-hot-toast'

export default function VerifyPage() {
  const searchParams = useSearchParams()
  const router = useRouter()
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading')
  const [artist, setArtist] = useState<any>(null)

  useEffect(() => {
    const token = searchParams.get('token')
    
    if (!token) {
      setStatus('error')
      return
    }

    const verifyToken = async () => {
      try {
        const response = await fetch(`${api.verify}?token=${token}`)
        
        if (response.ok) {
          const data = await response.json()
          setArtist(data.artist)
          setStatus('success')
          
          // Store token in localStorage
          localStorage.setItem('auth_token', data.access_token)
          localStorage.setItem('artist_data', JSON.stringify(data.artist))
          
          toast.success('Successfully signed in!')
          
          // Redirect to dashboard after a short delay
          setTimeout(() => {
            router.push('/dashboard')
          }, 2000)
        } else {
          setStatus('error')
          const error = await response.json()
          toast.error(error.detail || 'Verification failed')
        }
      } catch (error) {
        console.error('Verification error:', error)
        setStatus('error')
        toast.error('Verification failed. Please try again.')
      }
    }

    verifyToken()
  }, [searchParams, router])

  if (status === 'loading') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
        <div className="card max-w-md w-full text-center">
          <Loader2 className="h-16 w-16 text-gayphx-purple mx-auto mb-4 animate-spin" />
          <h1 className="text-2xl font-bold mb-4">Verifying...</h1>
          <p className="text-gray-600">
            Please wait while we verify your magic link.
          </p>
        </div>
      </div>
    )
  }

  if (status === 'error') {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
        <div className="card max-w-md w-full text-center">
          <XCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-4">Verification Failed</h1>
          <p className="text-gray-600 mb-6">
            The magic link is invalid or has expired. Please request a new one.
          </p>
          <div className="space-y-4">
            <Link href="/auth/login" className="btn-primary w-full">
              Request New Magic Link
            </Link>
            <Link href="/" className="btn-secondary w-full">
              Back to Home
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
      <div className="card max-w-md w-full text-center">
        <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
        <h1 className="text-2xl font-bold mb-4">Welcome, {artist?.name}!</h1>
        <p className="text-gray-600 mb-6">
          You've successfully signed in to your artist dashboard.
        </p>
        <div className="space-y-4">
          <Link href="/dashboard" className="btn-primary w-full">
            Go to Dashboard
          </Link>
          <Link href="/gallery" className="btn-secondary w-full">
            Browse Gallery
          </Link>
          <Link href="/artists" className="btn-secondary w-full">
            Manage Artists
          </Link>
          <Link href="/submit-new" className="btn-secondary w-full">
            Submit Music
          </Link>
        </div>
      </div>
    </div>
  )
}