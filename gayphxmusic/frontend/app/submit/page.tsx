'use client'

import { useState, useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useDropzone } from 'react-dropzone'
import { useRouter } from 'next/navigation'
import { Upload, Music, User, Mail, FileAudio, CheckCircle, Loader2, ArrowLeft } from 'lucide-react'
// import { submissionsApi, SubmissionCreate } from '@/lib/api'

interface SubmissionCreate {
  artist: {
    name: string
    email: string
    pronouns?: string
    bio?: string
    social_links?: Record<string, string>
  }
  song_title: string
  genre?: string
  file_name: string
  file_size: number // Size in bytes
  file_type?: string
  isrc_requested: boolean
  radio_permission: boolean
  public_display: boolean
  rights_attestation: boolean
  pro_info?: Record<string, any>
}
// import { useArtistProfile } from '@/lib/hooks/useArtistProfile'
import toast from 'react-hot-toast'
import Link from 'next/link'

interface FormData {
  artist: {
    email: string
    name: string
    pronouns?: string
    bio?: string
    social_links: {
      instagram?: string
      twitter?: string
      tiktok?: string
      spotify?: string
      soundcloud?: string
    }
  }
  song_title: string
  genre: string
  isrc_requested: boolean
  radio_permission: boolean
  public_display: boolean
  podcast_permission: boolean
  commercial_use: boolean
  rights_attestation: boolean
  pro_info: {
    pro_affiliation?: string
    writer_splits?: string
    publisher?: string
  }
}

export default function SubmitPage() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null)
  const [isUploading, setIsUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState(0)
  const [submissionComplete, setSubmissionComplete] = useState(false)
  const [trackingId, setTrackingId] = useState('')
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [artist, setArtist] = useState<any>(null)
  const [availableArtists, setAvailableArtists] = useState<any[]>([])
  const [showNewArtistForm, setShowNewArtistForm] = useState(false)
  const router = useRouter()

  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<FormData>({
    defaultValues: {
      artist: {
        social_links: {}
      },
      pro_info: {}
    }
  })

  // const watchedEmail = watch('artist.email')
  // const { profile, loading: profileLoading } = useArtistProfile(watchedEmail)

  const onDrop = (acceptedFiles: File[]) => {
    const file = acceptedFiles[0]
    if (file) {
      // Validate file size (150MB)
      if (file.size > 150 * 1024 * 1024) {
        toast.error('File size must be less than 150MB')
        return
      }
      
      // Validate file type - MP3 only
      if (file.type !== 'audio/mpeg' && !file.name.toLowerCase().endsWith('.mp3')) {
        toast.error('Only MP3 files are accepted. Please convert your audio file to MP3 format.')
        return
      }
      
      setUploadedFile(file)
      toast.success('File selected successfully')
    }
  }

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'audio/mpeg': ['.mp3']
    },
    multiple: false
  })

  // Fetch available artists for email
  const fetchAvailableArtists = async (email: string) => {
    try {
      const response = await fetch(`http://localhost:8000/api/submissions/artists/${email}`)
      if (response.ok) {
        const artists = await response.json()
        setAvailableArtists(artists)
        if (artists.length > 0) {
          setShowNewArtistForm(false)
        } else {
          setShowNewArtistForm(true)
        }
      }
    } catch (error) {
      console.error('Error fetching artists:', error)
    }
  }

  // Check authentication on mount
  useEffect(() => {
    // Only run on client side
    if (typeof window === 'undefined') return
    
    const token = localStorage.getItem('auth_token')
    const artistData = localStorage.getItem('artist_data')
    
    if (!token || !artistData) {
      router.push('/auth/login')
      return
    }

    try {
      setIsAuthenticated(true)
      const parsedArtist = JSON.parse(artistData)
      setArtist(parsedArtist)
      
      // Auto-fill form with authenticated user data
      setValue('artist.name', parsedArtist.name)
      setValue('artist.email', parsedArtist.email)
      setValue('artist.pronouns', parsedArtist.pronouns || '')
      setValue('artist.bio', parsedArtist.bio || '')
      if (parsedArtist.social_links) {
        setValue('artist.social_links.instagram', parsedArtist.social_links.instagram || '')
        setValue('artist.social_links.twitter', parsedArtist.social_links.twitter || '')
        setValue('artist.social_links.tiktok', parsedArtist.social_links.tiktok || '')
        setValue('artist.social_links.spotify', parsedArtist.social_links.spotify || '')
      }
      
      // Fetch available artists for this email
      fetchAvailableArtists(parsedArtist.email)
    } catch (error) {
      console.error('Error parsing artist data:', error)
      localStorage.removeItem('auth_token')
      localStorage.removeItem('artist_data')
      router.push('/auth/login')
      return
    }
  }, [router, setValue])

  // Show loading state while checking authentication
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center">
        <div className="card text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gayphx-purple mx-auto mb-4"></div>
          <p className="text-gray-600">Checking authentication...</p>
        </div>
      </div>
    )
  }

  const onSubmit = async (data: FormData) => {
    if (!uploadedFile) {
      toast.error('Please select an audio file')
      return
    }

    if (!data.rights_attestation) {
      toast.error('You must attest to having the rights to this music')
      return
    }

    setIsUploading(true)
    setUploadProgress(0)

    try {
      // Create FormData for direct upload
      const formData = new FormData()
      formData.append('file', uploadedFile)
      formData.append('artist_email', data.artist.email)
      formData.append('artist_name', data.artist.name)  // Added artist_name
      formData.append('song_title', data.song_title)
      formData.append('genre', data.genre || '')
      formData.append('isrc_requested', data.isrc_requested.toString())
      formData.append('radio_permission', data.radio_permission.toString())
      formData.append('public_display', data.public_display.toString())
      formData.append('podcast_permission', data.podcast_permission.toString())
      formData.append('commercial_use', data.commercial_use.toString())
      formData.append('rights_attestation', data.rights_attestation.toString())

      setUploadProgress(25)

      // Upload file directly to backend
      const response = await fetch('http://localhost:8000/api/submissions/upload', {
        method: 'POST',
        body: formData
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.detail || 'Upload failed')
      }

      const result = await response.json()
      setTrackingId(result.tracking_id)
      
      setUploadProgress(100)
      setSubmissionComplete(true)
      toast.success('Submission successful! Check your email for confirmation.')

    } catch (error) {
      console.error('Submission error:', error)
      toast.error('Submission failed. Please try again.')
    } finally {
      setIsUploading(false)
    }
  }

  if (submissionComplete) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center p-4">
        <div className="card max-w-md w-full text-center">
          <CheckCircle className="h-16 w-16 text-green-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-4">Submission Complete!</h1>
          <p className="text-gray-600 mb-6">
            Your music has been submitted successfully. We'll review it and notify you by email.
          </p>
          <div className="bg-gray-100 p-4 rounded-lg mb-6">
            <p className="text-sm text-gray-600">Tracking ID:</p>
            <p className="font-mono text-lg font-bold">{trackingId}</p>
          </div>
          <div className="space-y-2">
            <Link href={`/track/${trackingId}`} className="btn-primary w-full">
              Track Your Submission
            </Link>
            <Link href="/dashboard" className="btn-secondary w-full">
              Back to Dashboard
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-500 to-pink-600 relative overflow-hidden">
      {/* Circles Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `radial-gradient(circle at 25% 25%, white 2px, transparent 2px), radial-gradient(circle at 75% 75%, white 2px, transparent 2px)`,
          backgroundSize: '40px 40px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">GayPHX Music</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-white">Welcome, {artist?.name}</span>
              <Link href="/dashboard" className="text-white hover:text-gray-200 transition-colors flex items-center">
                <ArrowLeft className="h-4 w-4 mr-1" />
                Back to Dashboard
              </Link>
            </div>
          </div>
        </div>
      </header>

      <div className="py-8 relative z-10">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold text-white mb-4">Submit Your Music</h1>
            <p className="text-white/90 text-lg">
              Share your music with the GayPHX community and get official ISRC codes
            </p>
          </div>

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
            {/* File Upload */}
            <div className="card">
              <h2 className="text-xl font-semibold mb-4 flex items-center">
                <FileAudio className="h-5 w-5 mr-2" />
                Audio File
              </h2>
              
              <div
                {...getRootProps()}
                className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                  isDragActive
                    ? 'border-gayphx-purple bg-gayphx-purple/10'
                    : 'border-gray-300 hover:border-gray-400'
                }`}
              >
                <input {...getInputProps()} />
                <Upload className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                {uploadedFile ? (
                  <div>
                    <p className="text-lg font-medium text-gray-900">{uploadedFile.name}</p>
                    <p className="text-sm text-gray-500">
                      {(uploadedFile.size / (1024 * 1024)).toFixed(1)} MB
                    </p>
                  </div>
                ) : (
                  <div>
                    <p className="text-lg font-medium text-gray-900">
                      {isDragActive ? 'Drop your MP3 file here' : 'Drag & drop your MP3 file here'}
                    </p>
                    <p className="text-sm text-gray-500 mt-2">
                      MP3 only up to 150MB • Missing metadata will be added automatically
                    </p>
                  </div>
                )}
              </div>

              {isUploading && (
                <div className="mt-4">
                  <div className="bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-gayphx-purple h-2 rounded-full transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    />
                  </div>
                  <p className="text-sm text-gray-600 mt-2">Uploading... {uploadProgress}%</p>
                </div>
              )}
            </div>

            {/* Artist Information */}
            <div className="card">
              <h2 className="text-xl font-semibold mb-4 flex items-center">
                <User className="h-5 w-5 mr-2" />
                Artist Information
                <span className="ml-2 text-sm text-green-600 bg-green-100 px-2 py-1 rounded-full">
                  ✓ Pre-filled from your account
                </span>
              </h2>
              
              {/* Artist Selection */}
              {availableArtists.length > 0 && (
                <div className="mb-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Select Artist Profile
                  </label>
                  <div className="space-y-2">
                    {availableArtists.map((artistOption) => (
                      <label key={artistOption.id} className="flex items-center p-3 border rounded-lg cursor-pointer hover:bg-gray-50">
                        <input
                          type="radio"
                          name="selected_artist"
                          value={artistOption.id}
                          onChange={() => {
                            setValue('artist.name', artistOption.name)
                            setValue('artist.pronouns', artistOption.pronouns || '')
                            setValue('artist.bio', artistOption.bio || '')
                            if (artistOption.social_links) {
                              setValue('artist.social_links.instagram', artistOption.social_links.instagram || '')
                              setValue('artist.social_links.twitter', artistOption.social_links.twitter || '')
                              setValue('artist.social_links.tiktok', artistOption.social_links.tiktok || '')
                              setValue('artist.social_links.spotify', artistOption.social_links.spotify || '')
                            }
                            setShowNewArtistForm(false)
                          }}
                          className="mr-3"
                        />
                        <div>
                          <div className="font-medium">{artistOption.name}</div>
                          {artistOption.pronouns && (
                            <div className="text-sm text-gray-500">{artistOption.pronouns}</div>
                          )}
                        </div>
                      </label>
                    ))}
                    <button
                      type="button"
                      onClick={() => setShowNewArtistForm(true)}
                      className="w-full p-3 border-2 border-dashed border-gray-300 rounded-lg text-gray-600 hover:border-gayphx-purple hover:text-gayphx-purple transition-colors"
                    >
                      + Create New Artist Profile
                    </button>
                  </div>
                </div>
              )}
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Artist Name *
                  </label>
                  <input
                    {...register('artist.name', { required: 'Artist name is required' })}
                    className="input-field"
                    placeholder="Your stage name or real name"
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                  {errors.artist?.name && (
                    <p className="text-red-500 text-sm mt-1">{errors.artist.name.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Email Address *
                  </label>
                  <input
                    {...register('artist.email', { 
                      required: 'Email is required',
                      pattern: {
                        value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                        message: 'Invalid email address'
                      }
                    })}
                    type="email"
                    className="input-field"
                    placeholder="your@email.com"
                    disabled
                  />
                  {errors.artist?.email && (
                    <p className="text-red-500 text-sm mt-1">{errors.artist.email.message}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Pronouns
                  </label>
                  <input
                    {...register('artist.pronouns')}
                    className="input-field"
                    placeholder="they/them, she/her, he/him, etc."
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Genre
                  </label>
                  <select {...register('genre')} className="input-field">
                    <option value="">Select a genre</option>
                    <option value="pop">Pop</option>
                    <option value="rock">Rock</option>
                    <option value="hip-hop">Hip-Hop</option>
                    <option value="electronic">Electronic</option>
                    <option value="r&b">R&B</option>
                    <option value="country">Country</option>
                    <option value="jazz">Jazz</option>
                    <option value="classical">Classical</option>
                    <option value="alternative">Alternative</option>
                    <option value="other">Other</option>
                  </select>
                </div>
              </div>

              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Bio
                </label>
                <textarea
                  {...register('artist.bio')}
                  rows={3}
                  className="input-field"
                  placeholder="Tell us about yourself and your music..."
                  disabled={!showNewArtistForm && availableArtists.length > 0}
                />
              </div>

              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Social Links
                </label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <input
                    {...register('artist.social_links.instagram')}
                    className="input-field"
                    placeholder="Instagram @username"
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                  <input
                    {...register('artist.social_links.twitter')}
                    className="input-field"
                    placeholder="Twitter @username"
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                  <input
                    {...register('artist.social_links.tiktok')}
                    className="input-field"
                    placeholder="TikTok @username"
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                  <input
                    {...register('artist.social_links.spotify')}
                    className="input-field"
                    placeholder="Spotify Artist URL"
                    disabled={!showNewArtistForm && availableArtists.length > 0}
                  />
                </div>
              </div>
            </div>

            {/* Song Information */}
            <div className="card">
              <h2 className="text-xl font-semibold mb-4 flex items-center">
                <Music className="h-5 w-5 mr-2" />
                Song Information
              </h2>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Song Title *
                </label>
                <input
                  {...register('song_title', { required: 'Song title is required' })}
                  className="input-field"
                  placeholder="Enter the title of your song"
                />
                {errors.song_title && (
                  <p className="text-red-500 text-sm mt-1">{errors.song_title.message}</p>
                )}
              </div>
            </div>

            {/* Permissions */}
            <div className="card">
              <h2 className="text-xl font-semibold mb-4">Permissions & Requests</h2>
              
              <div className="space-y-4">
                <label className="flex items-start">
                  <input
                    {...register('isrc_requested')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                  />
                  <div>
                    <span className="font-medium">Request ISRC Code</span>
                    <p className="text-sm text-gray-600">
                      Get an official ISRC code for this track (required for distribution)
                    </p>
                  </div>
                </label>

                <label className="flex items-start">
                  <input
                    {...register('radio_permission')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                  />
                  <div>
                    <span className="font-medium">Radio Permission</span>
                    <p className="text-sm text-gray-600">
                      Allow GayPHX Radio to play this track on air
                    </p>
                  </div>
                </label>

                <label className="flex items-start">
                  <input
                    {...register('public_display')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                  />
                  <div>
                    <span className="font-medium">Public Gallery</span>
                    <p className="text-sm text-gray-600">
                      Display this track in our public artist gallery
                    </p>
                  </div>
                </label>

                <label className="flex items-start">
                  <input
                    {...register('podcast_permission')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                  />
                  <div>
                    <span className="font-medium">Podcast Permission</span>
                    <p className="text-sm text-gray-600">
                      Allow GayPHX to use this track in podcasts and audio content
                    </p>
                  </div>
                </label>

                <label className="flex items-start">
                  <input
                    {...register('commercial_use')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                  />
                  <div>
                    <span className="font-medium">Commercial Use</span>
                    <p className="text-sm text-gray-600">
                      Allow GayPHX to use this track for commercial purposes (with compensation)
                    </p>
                  </div>
                </label>

                <label className="flex items-start">
                  <input
                    {...register('rights_attestation')}
                    type="checkbox"
                    className="mt-1 mr-3 h-4 w-4 text-gayphx-purple focus:ring-gayphx-purple border-gray-300 rounded"
                    required
                  />
                  <div>
                    <span className="font-medium">Rights Attestation *</span>
                    <p className="text-sm text-gray-600">
                      I attest that I have the legal rights to submit this music and grant the permissions above
                    </p>
                  </div>
                </label>
              </div>
            </div>

            {/* Submit Button */}
            <div className="text-center">
              <button
                type="submit"
                disabled={isUploading || !uploadedFile}
                className="btn-primary text-lg px-8 py-4 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isUploading ? 'Submitting...' : 'Submit Your Music'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}