'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Mail, ArrowRight, Loader2 } from 'lucide-react'
import Link from 'next/link'
import toast from 'react-hot-toast'
import { api } from '@/lib/api'

interface LoginFormData {
  email: string
}

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const [emailSent, setEmailSent] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<LoginFormData>()

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true)
    
    try {
      const response = await fetch(api.requestMagicLink, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email: data.email }),
      })

      if (response.ok) {
        setEmailSent(true)
        toast.success('Magic link sent! Check your email.')
      } else {
        const error = await response.json()
        toast.error(error.detail || 'Failed to send magic link')
      }
    } catch (error) {
      console.error('Login error:', error)
      toast.error('Failed to send magic link. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  if (emailSent) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
        <div className="card max-w-md w-full text-center">
          <Mail className="h-16 w-16 text-gayphx-purple mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-4">Check Your Email</h1>
          <p className="text-gray-600 mb-6">
            We've sent you a magic link to sign in. Click the link in your email to access your dashboard.
          </p>
          <div className="space-y-2">
            <button
              onClick={() => setEmailSent(false)}
              className="btn-secondary w-full"
            >
              Try Different Email
            </button>
            <Link href="/" className="btn-primary w-full">
              Back to Home
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
      <div className="card max-w-md w-full">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back</h1>
          <p className="text-gray-600">Sign in to your artist dashboard</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email Address
            </label>
            <input
              {...register('email', { 
                required: 'Email is required',
                pattern: {
                  value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                  message: 'Invalid email address'
                }
              })}
              type="email"
              className="input-field"
              placeholder="your@email.com"
            />
            {errors.email && (
              <p className="text-red-500 text-sm mt-1">{errors.email.message}</p>
            )}
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="btn-primary w-full flex items-center justify-center"
          >
            {isLoading ? (
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
            ) : (
              <ArrowRight className="h-4 w-4 mr-2" />
            )}
            Send Magic Link
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-gray-600">
            Don't have an account?{' '}
            <Link href="/signup" className="text-gayphx-purple hover:text-gayphx-purple/80 font-medium">
              Sign up here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}