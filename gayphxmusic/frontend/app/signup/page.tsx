'use client'

import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { User, Mail, ArrowRight, Loader2 } from 'lucide-react'
import Link from 'next/link'
import toast from 'react-hot-toast'
import { api } from '@/lib/api'

interface SignupFormData {
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: {
    instagram?: string
    twitter?: string
    tiktok?: string
    spotify?: string
  }
}

export default function SignupPage() {
  const [isLoading, setIsLoading] = useState(false)
  const [signupComplete, setSignupComplete] = useState(false)

  const { register, handleSubmit, formState: { errors } } = useForm<SignupFormData>({
    defaultValues: {
      social_links: {}
    }
  })

  const onSubmit = async (data: SignupFormData) => {
    setIsLoading(true)
    
    try {
      const response = await fetch(api.signup, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
      })

      if (response.ok) {
        setSignupComplete(true)
        toast.success('Account created! Check your email for a magic link.')
      } else {
        const error = await response.json()
        toast.error(error.detail || 'Failed to create account')
      }
    } catch (error) {
      console.error('Signup error:', error)
      toast.error('Failed to create account. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  if (signupComplete) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
        <div className="card max-w-md w-full text-center">
          <User className="h-16 w-16 text-gayphx-purple mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-4">Account Created!</h1>
          <p className="text-gray-600 mb-6">
            We've sent you a magic link to complete your signup. Check your email and click the link to access your dashboard.
          </p>
          <div className="space-y-2">
            <Link href="/auth/login" className="btn-primary w-full">
              Sign In Instead
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
      <div className="card max-w-md w-full">
        <div className="text-center mb-6">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Join GayPHX Music</h1>
          <p className="text-gray-600">Create your artist account</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Artist Name *
            </label>
            <input
              {...register('name', { required: 'Artist name is required' })}
              className="input-field"
              placeholder="Your stage name or real name"
            />
            {errors.name && (
              <p className="text-red-500 text-sm mt-1">{errors.name.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email Address *
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

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Pronouns
            </label>
            <input
              {...register('pronouns')}
              className="input-field"
              placeholder="they/them, she/her, he/him, etc."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Bio
            </label>
            <textarea
              {...register('bio')}
              rows={3}
              className="input-field"
              placeholder="Tell us about yourself and your music..."
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Social Links (Optional)
            </label>
            <div className="space-y-2">
              <input
                {...register('social_links.instagram')}
                className="input-field"
                placeholder="Instagram @username"
              />
              <input
                {...register('social_links.twitter')}
                className="input-field"
                placeholder="Twitter @username"
              />
              <input
                {...register('social_links.tiktok')}
                className="input-field"
                placeholder="TikTok @username"
              />
              <input
                {...register('social_links.spotify')}
                className="input-field"
                placeholder="Spotify Artist URL"
              />
            </div>
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
            Create Account
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-gray-600">
            Already have an account?{' '}
            <Link href="/auth/login" className="text-gayphx-purple hover:text-gayphx-purple/80 font-medium">
              Sign in here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}