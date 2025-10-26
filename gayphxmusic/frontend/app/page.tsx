'use client'

import Link from 'next/link'
import { Music, Upload, Users, Award } from 'lucide-react'
import { useSystemConfig } from '../lib/contexts/SystemConfigContext'

export default function HomePage() {
  const { config } = useSystemConfig()
  const organizationName = config?.organization_name || 'GayPHX Music Platform'
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-500 to-blue-700 relative overflow-hidden">
      {/* Dots Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `radial-gradient(circle at 1px 1px, white 1px, transparent 0)`,
          backgroundSize: '20px 20px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10" role="banner">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" aria-hidden="true" />
              <h1 className="text-2xl font-bold text-white">{organizationName}</h1>
            </div>
            <nav className="hidden md:flex space-x-8" role="navigation" aria-label="Main navigation">
              <Link 
                href="/submit-new" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Submit your music for review"
              >
                Submit Music
              </Link>
              <Link 
                href="/artists" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Manage your artist profiles"
              >
                Artists
              </Link>
              <Link 
                href="/gallery" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="View community music gallery"
              >
                Gallery
              </Link>
              <Link 
                href="/help" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Get help and support"
              >
                Help
              </Link>
              <Link 
                href="/auth/login" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Sign in to your account"
              >
                Sign In
              </Link>
              <Link 
                href="/signup" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Create a new account"
              >
                Sign Up
              </Link>
              <Link 
                href="/admin" 
                className="text-white hover:text-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded px-2 py-1"
                aria-label="Access admin panel"
              >
                Admin
              </Link>
            </nav>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 relative z-10" role="main">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold text-white mb-6">
            Submit Your Music to<span className="block text-transparent bg-clip-text bg-gradient-to-r from-yellow-400 to-pink-400">{organizationName}</span>
          </h1>
          <p className="text-xl text-white/90 mb-8 max-w-3xl mx-auto">
            Get official ISRC codes for your tracks and join the GayPHX community. Submit your music, get reviewed by our team, and receive your official registration codes.
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link 
              href="/submit-new" 
              className="btn-primary text-lg px-8 py-4 inline-flex items-center focus:outline-none focus:ring-4 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded-lg"
              aria-label="Submit your music for review and get ISRC codes"
            >
              <Upload className="mr-2 h-5 w-5" aria-hidden="true" />
              Submit Your Music
            </Link>
            <Link 
              href="/gallery" 
              className="btn-secondary text-lg px-8 py-4 inline-flex items-center focus:outline-none focus:ring-4 focus:ring-white focus:ring-offset-2 focus:ring-offset-blue-600 rounded-lg"
              aria-label="View the community music gallery"
            >
              <Users className="mr-2 h-5 w-5" aria-hidden="true" />
              View Gallery
            </Link>
          </div>
        </div>

        {/* Features Grid */}
        <div className="mt-20 grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="card text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Upload className="h-8 w-8 text-white" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Easy Submission</h3>
            <p className="text-gray-600">Upload your tracks with our simple form. We support MP3, WAV, M4A, and FLAC files up to 150MB.</p>
          </div>
          
          <div className="card text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Award className="h-8 w-8 text-white" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Official ISRC Codes</h3>
            <p className="text-gray-600">Receive official ISRC codes for your approved tracks. Perfect for distribution and royalty tracking.</p>
          </div>
          
          <div className="card text-center">
            <div className="w-16 h-16 bg-gradient-to-r from-purple-500 to-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
              <Users className="h-8 w-8 text-white" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Community Gallery</h3>
            <p className="text-gray-600">Showcase your music in our public gallery and connect with other LGBTQ+ artists in Phoenix.</p>
          </div>
        </div>

        {/* What We Are */}
        <div className="mt-20">
          <div className="bg-white/10 backdrop-blur-md rounded-2xl p-8 mb-16">
            <h2 className="text-3xl font-bold text-white text-center mb-6">What is GayPHX Music?</h2>
            <div className="max-w-4xl mx-auto text-center">
              <p className="text-xl text-white/90 mb-6">
                GayPHX Music is a <strong>music support platform</strong> for LGBTQ+ artists, not a record label. 
                We provide professional tools and resources while you maintain 100% ownership of your music.
              </p>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-left">
                <div className="bg-white/10 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-white mb-3">✅ What We Provide</h3>
                  <ul className="text-white/80 space-y-2 text-sm">
                    <li>• Official ISRC codes for your tracks</li>
                    <li>• Music submission and tracking platform</li>
                    <li>• Educational resources and industry guidance</li>
                    <li>• LGBTQ+ artist community support</li>
                    <li>• Potential radio airplay opportunities</li>
                    <li>• Commercial use opportunities with fair compensation</li>
                  </ul>
                </div>
                <div className="bg-white/10 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-white mb-3">❌ What We Don't Do</h3>
                  <ul className="text-white/80 space-y-2 text-sm">
                    <li>• We don't own your music or take royalties</li>
                    <li>• We don't sign artists to contracts</li>
                    <li>• We don't handle distribution to streaming platforms</li>
                    <li>• We don't provide marketing or promotion services</li>
                    <li>• We don't control your creative decisions</li>
                  </ul>
                </div>
              </div>
              <div className="mt-6">
                <Link 
                  href="/help" 
                  className="inline-flex items-center text-white hover:text-yellow-300 transition-colors font-semibold"
                >
                  Learn more about our services →
                </Link>
              </div>
            </div>
          </div>
        </div>

        {/* How It Works */}
        <div className="mt-20">
          <h2 className="text-3xl font-bold text-white text-center mb-12">How It Works</h2>
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white font-bold text-lg">1</span>
              </div>
              <h3 className="text-white font-semibold mb-2">Submit</h3>
              <p className="text-white/80 text-sm">Upload your track and fill out the submission form</p>
            </div>
            <div className="text-center">
              <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white font-bold text-lg">2</span>
              </div>
              <h3 className="text-white font-semibold mb-2">Review</h3>
              <p className="text-white/80 text-sm">Our team reviews your submission for quality and rights</p>
            </div>
            <div className="text-center">
              <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white font-bold text-lg">3</span>
              </div>
              <h3 className="text-white font-semibold mb-2">Approve</h3>
              <p className="text-white/80 text-sm">Get approved and receive your official ISRC code</p>
            </div>
            <div className="text-center">
              <div className="w-12 h-12 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-white font-bold text-lg">4</span>
              </div>
              <h3 className="text-white font-semibold mb-2">Track</h3>
              <p className="text-white/80 text-sm">Monitor your submissions in your artist dashboard</p>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white/10 backdrop-blur-md border-t border-white/20 mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center text-white/80">
            <p>{config?.copyright_notice || '© 2025 GayPHX Music Platform. Built with love for the LGBTQ+ community. 🌈'}</p>
          </div>
        </div>
      </footer>
    </div>
  )
}