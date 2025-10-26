'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { User, Mail, Music, Save, ArrowLeft, Edit3, Globe, Instagram, Twitter, Youtube, Music2 } from 'lucide-react'
import Link from 'next/link'
import toast from 'react-hot-toast'

interface ArtistProfile {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
  created_at: string
  updated_at: string
  submission_count: number
}

export default function ArtistProfilePage() {
  const [profile, setProfile] = useState<ArtistProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editing, setEditing] = useState(false)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    pronouns: '',
    bio: '',
    social_links: {
      website: '',
      instagram: '',
      twitter: '',
      youtube: '',
      tiktok: '',
      spotify: ''
    }
  })
  const router = useRouter()

  useEffect(() => {
    fetchProfile()
  }, [])

  const fetchProfile = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token')
      if (!token) {
        setIsAuthenticated(false)
        return
      }

      const response = await fetch('http://localhost:8000/api/artists/profile', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      if (response.ok) {
        const data = await response.json()
        setProfile(data)
        setIsAuthenticated(true)
        setFormData({
          name: data.name,
          pronouns: data.pronouns || '',
          bio: data.bio || '',
          social_links: data.social_links || {
            website: '',
            instagram: '',
            twitter: '',
            youtube: '',
            tiktok: '',
            spotify: ''
          }
        })
      } else {
        setIsAuthenticated(false)
      }
    } catch (error) {
      console.error('Error fetching profile:', error)
      toast.error('Error fetching profile')
      router.push('/dashboard')
    } finally {
      setLoading(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      setSaving(true)
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token')
      if (!token) {
        router.push('/auth/login')
        return
      }

      const response = await fetch('http://localhost:8000/api/artists/profile', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      })

      if (response.ok) {
        toast.success('Profile updated successfully')
        setEditing(false)
        fetchProfile() // Refresh profile data
      } else {
        const error = await response.json()
        toast.error(error.detail || 'Failed to update profile')
      }
    } catch (error) {
      console.error('Error updating profile:', error)
      toast.error('Error updating profile')
    } finally {
      setSaving(false)
    }
  }

  const handleInputChange = (field: string, value: any) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }

  const handleSocialLinkChange = (platform: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      social_links: {
        ...prev.social_links,
        [platform]: value
      }
    }))
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }

  if (!isAuthenticated && !loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
        {/* Grid Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute inset-0" style={{
            backgroundImage: `linear-gradient(white 1px, transparent 1px), linear-gradient(90deg, white 1px, transparent 1px)`,
            backgroundSize: '20px 20px'
          }}></div>
        </div>
        {/* Header */}
        <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-4">
              <div className="flex items-center space-x-2">
                <User className="h-8 w-8 text-white" />
                <h1 className="text-2xl font-bold text-white">Artist Profile</h1>
              </div>
              <nav className="hidden md:flex space-x-8">
                <Link href="/" className="text-white hover:text-gray-200 transition-colors">
                  Landing
                </Link>
                <Link href="/artists" className="text-white hover:text-gray-200 transition-colors">
                  Artists
                </Link>
                <Link href="/submit-new" className="text-white hover:text-gray-200 transition-colors">
                  Submit Music
                </Link>
                <Link href="/gallery" className="text-white hover:text-gray-200 transition-colors">
                  Gallery
                </Link>
                <Link href="/help" className="text-white hover:text-gray-200 transition-colors">
                  Help
                </Link>
              </nav>
            </div>
          </div>
        </header>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 relative z-10">
          <div className="text-center">
            <h1 className="text-4xl md:text-5xl font-bold text-white mb-6">
              Artist Profile
            </h1>
            <p className="text-xl text-white/90 mb-8 max-w-2xl mx-auto">
              Please sign in to view and manage your artist profile.
            </p>
            <div className="space-x-4">
              <Link
                href="/auth/login"
                className="bg-white text-cyan-600 px-8 py-4 rounded-lg hover:bg-gray-100 transition-colors inline-block font-semibold"
              >
                Sign In
              </Link>
              <Link
                href="/signup"
                className="bg-white/20 text-white px-8 py-4 rounded-lg hover:bg-white/30 transition-colors inline-block font-semibold border border-white/30"
              >
                Sign Up
              </Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gayphx-purple mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading profile...</p>
        </div>
      </div>
    )
  }

  if (!profile) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">Profile not found</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
      {/* Grid Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(white 1px, transparent 1px), linear-gradient(90deg, white 1px, transparent 1px)`,
          backgroundSize: '20px 20px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <User className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">Artist Profile</h1>
            </div>
            <nav className="hidden md:flex space-x-8">
              <Link href="/dashboard" className="text-white hover:text-gray-200 transition-colors">
                Dashboard
              </Link>
              <Link href="/artists" className="text-white hover:text-gray-200 transition-colors">
                Artists
              </Link>
              <Link href="/submit-new" className="text-white hover:text-gray-200 transition-colors">
                Submit Music
              </Link>
              <Link href="/gallery" className="text-white hover:text-gray-200 transition-colors">
                Gallery
              </Link>
              <Link href="/help" className="text-white hover:text-gray-200 transition-colors">
                Help
              </Link>
            </nav>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        {/* Page Header */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-white">Your Profile</h1>
              <p className="mt-2 text-white/80">Manage your artist information and social links</p>
            </div>
            <div className="flex space-x-3">
              <button
                onClick={() => router.push('/dashboard')}
                className="bg-white/20 text-white px-4 py-2 rounded-lg hover:bg-white/30 transition-colors flex items-center"
              >
                <ArrowLeft className="h-4 w-4 mr-2" />
                Back to Dashboard
              </button>
              {!editing && (
                <button
                  onClick={() => setEditing(true)}
                  className="bg-white text-cyan-600 px-6 py-3 rounded-lg hover:bg-gray-100 transition-colors font-semibold flex items-center"
                >
                  <Edit3 className="h-4 w-4 mr-2" />
                  Edit Profile
                </button>
              )}
            </div>
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Profile Info */}
          <div className="lg:col-span-1">
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Profile Information</h2>
              
              <div className="space-y-4">
                <div className="flex items-center">
                  <User className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">{profile.name}</span>
                </div>
                
                <div className="flex items-center">
                  <Mail className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">{profile.email}</span>
                </div>
                
                <div className="flex items-center">
                  <Music className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">{profile.submission_count} submissions</span>
                </div>
                
                <div className="flex items-center">
                  <User className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">
                    Joined {formatDate(profile.created_at)}
                  </span>
                </div>
              </div>

              {/* Social Links */}
              <div className="mt-6">
                <h3 className="text-md font-medium text-gray-900 mb-3">Social Links</h3>
                <div className="space-y-2">
                  {Object.entries(profile.social_links).map(([platform, url]) => {
                    if (!url) return null
                    const icons = {
                      website: Globe,
                      instagram: Instagram,
                      twitter: Twitter,
                      youtube: Youtube,
                      tiktok: Music2,
                      spotify: Music2
                    }
                    const Icon = icons[platform as keyof typeof icons] || Globe
                    return (
                      <a
                        key={platform}
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center text-sm text-gayphx-purple hover:text-gayphx-pink"
                      >
                        <Icon className="h-4 w-4 mr-2" />
                        {platform.charAt(0).toUpperCase() + platform.slice(1)}
                      </a>
                    )
                  })}
                </div>
              </div>
            </div>
          </div>

          {/* Edit Form */}
          <div className="lg:col-span-2">
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-6">
                {editing ? 'Edit Profile' : 'Profile Details'}
              </h2>
              
              {editing ? (
                <form onSubmit={handleSubmit} className="space-y-6">
                  {/* Basic Information */}
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                        Artist Name
                      </label>
                      <input
                        type="text"
                        id="name"
                        value={formData.name}
                        onChange={(e) => handleInputChange('name', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                        required
                      />
                    </div>
                    
                    <div>
                      <label htmlFor="pronouns" className="block text-sm font-medium text-gray-700 mb-2">
                        Pronouns
                      </label>
                      <input
                        type="text"
                        id="pronouns"
                        value={formData.pronouns}
                        onChange={(e) => handleInputChange('pronouns', e.target.value)}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                        placeholder="e.g., they/them, he/him, she/her"
                      />
                    </div>
                  </div>

                  <div>
                    <label htmlFor="bio" className="block text-sm font-medium text-gray-700 mb-2">
                      Bio
                    </label>
                    <textarea
                      id="bio"
                      value={formData.bio}
                      onChange={(e) => handleInputChange('bio', e.target.value)}
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                      placeholder="Tell us about yourself and your music..."
                    />
                  </div>

                  {/* Social Links */}
                  <div>
                    <h3 className="text-md font-medium text-gray-900 mb-4">Social Media Links</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {[
                        { key: 'website', label: 'Website', placeholder: 'https://yourwebsite.com' },
                        { key: 'instagram', label: 'Instagram', placeholder: 'https://instagram.com/yourhandle' },
                        { key: 'twitter', label: 'Twitter', placeholder: 'https://twitter.com/yourhandle' },
                        { key: 'youtube', label: 'YouTube', placeholder: 'https://youtube.com/yourchannel' },
                        { key: 'tiktok', label: 'TikTok', placeholder: 'https://tiktok.com/@yourhandle' },
                        { key: 'spotify', label: 'Spotify', placeholder: 'https://open.spotify.com/artist/...' }
                      ].map((social) => (
                        <div key={social.key}>
                          <label className="block text-sm font-medium text-gray-700 mb-2">
                            {social.label}
                          </label>
                          <input
                            type="url"
                            value={formData.social_links[social.key as keyof typeof formData.social_links]}
                            onChange={(e) => handleSocialLinkChange(social.key, e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-gayphx-purple focus:border-transparent"
                            placeholder={social.placeholder}
                          />
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Submit Buttons */}
                  <div className="flex justify-end space-x-3 pt-6">
                    <button
                      type="button"
                      onClick={() => setEditing(false)}
                      className="btn-secondary"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={saving}
                      className="btn-primary"
                    >
                      {saving ? (
                        <>
                          <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                          Saving...
                        </>
                      ) : (
                        <>
                          <Save className="h-4 w-4 mr-2" />
                          Save Changes
                        </>
                      )}
                    </button>
                  </div>
                </form>
              ) : (
                <div className="space-y-6">
                  <div>
                    <h3 className="text-sm font-medium text-gray-500 mb-1">Artist Name</h3>
                    <p className="text-lg text-gray-900">{profile.name}</p>
                  </div>
                  
                  {profile.pronouns && (
                    <div>
                      <h3 className="text-sm font-medium text-gray-500 mb-1">Pronouns</h3>
                      <p className="text-lg text-gray-900">{profile.pronouns}</p>
                    </div>
                  )}
                  
                  {profile.bio && (
                    <div>
                      <h3 className="text-sm font-medium text-gray-500 mb-1">Bio</h3>
                      <p className="text-lg text-gray-900 whitespace-pre-wrap">{profile.bio}</p>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
