'use client'

import { useState, useEffect } from 'react'
// import { submissionsApi, SubmissionResponse } from '@/lib/api'

interface GalleryArtist {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
  public_track_count: number
  total_submission_count: number
  public_submissions: Array<{
    id: string
    song_title: string
    genre?: string
    tracking_id: string
    created_at: string
  }>
}
import { Music, Play, Pause, ExternalLink, Search, Filter, Heart } from 'lucide-react'
import Link from 'next/link'

export default function GalleryPage() {
  const [artists, setArtists] = useState<GalleryArtist[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [genreFilter, setGenreFilter] = useState('')
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [favoriteArtists, setFavoriteArtists] = useState<Set<string>>(new Set())
  const [playingId, setPlayingId] = useState<string | null>(null)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const [audioElement, setAudioElement] = useState<HTMLAudioElement | null>(null)
  const [isAuthenticated, setIsAuthenticated] = useState(false)

  useEffect(() => {
    fetchGalleryArtists()
    checkAuthentication()
  }, [])

  const checkAuthentication = () => {
    const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token')
    setIsAuthenticated(!!token)
    if (token) {
      fetchFavorites()
    }
  }

  const fetchFavorites = async () => {
    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token')
      const response = await fetch('http://localhost:8000/api/artists/favorites', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
      if (response.ok) {
        const favorites = await response.json()
        const favoriteIds: Set<string> = new Set(favorites.map((fav: any) => fav.id as string))
        setFavoriteArtists(favoriteIds)
      }
    } catch (error) {
      console.error('Error fetching favorites:', error)
    }
  }

  const toggleFavorite = async (artistId: string) => {
    if (!isAuthenticated) {
      alert('Please log in to favorite artists')
      return
    }

    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token')
      const isFavorited = favoriteArtists.has(artistId)
      
      const response = await fetch(`http://localhost:8000/api/artists/favorites/${artistId}`, {
        method: isFavorited ? 'DELETE' : 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        if (isFavorited) {
          setFavoriteArtists(prev => {
            const newSet = new Set(prev)
            newSet.delete(artistId)
            return newSet
          })
        } else {
          setFavoriteArtists(prev => {
            const newSet = new Set(prev)
            newSet.add(artistId)
            return newSet
          })
        }
      } else {
        alert('Failed to update favorite')
      }
    } catch (error) {
      console.error('Error toggling favorite:', error)
      alert('Failed to update favorite')
    }
  }

  // Cleanup audio on unmount
  useEffect(() => {
    return () => {
      if (audioElement) {
        audioElement.pause()
        audioElement.currentTime = 0
      }
      if (audioUrl) {
        URL.revokeObjectURL(audioUrl)
      }
    }
  }, [audioElement, audioUrl])

  const fetchGalleryArtists = async () => {
    try {
      const response = await fetch('http://localhost:8000/api/submissions/gallery')
      const data = await response.json()
      setArtists(data)
    } catch (error) {
      console.error('Error fetching gallery artists:', error)
    } finally {
      setLoading(false)
    }
  }

  const filteredArtists = artists.filter(artist => {
    const matchesSearch = artist.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         artist.email.toLowerCase().includes(searchTerm.toLowerCase())
    
    // Filter by genre if any of their public tracks match
    const matchesGenre = !genreFilter || artist.public_submissions.some(sub => sub.genre === genreFilter)
    
    // Filter by favorites if enabled
    const matchesFavorites = !showFavoritesOnly || favoriteArtists.has(artist.id)
    
    return matchesSearch && matchesGenre && matchesFavorites
  })

  // Get all unique genres from all public submissions
  const genres = Array.from(new Set(
    artists.flatMap(artist => 
      artist.public_submissions.map(sub => sub.genre).filter(Boolean)
    )
  ))

  const togglePlay = async (submissionId: string) => {
    const isCurrentlyPlaying = playingId === submissionId
    
    // Stop current audio if playing
    if (audioElement) {
      audioElement.pause()
      audioElement.currentTime = 0
    }
    
    if (isCurrentlyPlaying) {
      setPlayingId(null)
      setAudioUrl(null)
      setAudioElement(null)
      return
    }
    
    try {
      // Get audio stream URL
      const response = await fetch(`http://localhost:8000/api/admin/submissions/${submissionId}/audio-stream`)
      
      if (!response.ok) {
        throw new Error('Failed to get audio stream')
      }
      
      // Create blob URL for streaming
      const blob = await response.blob()
      const url = URL.createObjectURL(blob)
      
      // Create audio element
      const audio = new Audio(url)
      audio.preload = 'metadata'
      
      // Set up event listeners
      audio.addEventListener('ended', () => {
        setPlayingId(null)
        setAudioUrl(null)
        setAudioElement(null)
        URL.revokeObjectURL(url)
      })
      
      audio.addEventListener('error', (e) => {
        console.error('Audio playback error:', e)
        setPlayingId(null)
        setAudioUrl(null)
        setAudioElement(null)
        URL.revokeObjectURL(url)
      })
      
      // Play the audio
      await audio.play()
      
      setPlayingId(submissionId)
      setAudioUrl(url)
      setAudioElement(audio)
      
      // Track the play event
      try {
        await fetch(`http://localhost:8000/api/plays/track-gallery-play?submission_id=${submissionId}&user_agent=${encodeURIComponent(navigator.userAgent)}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          }
        })
      } catch (error) {
        console.error('Error tracking gallery play:', error)
        // Don't prevent play if tracking fails
      }
      
    } catch (error) {
      console.error('Error playing audio:', error)
      alert('Failed to play audio. Please try again.')
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center">
        <div className="text-white text-xl">Loading gallery...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-500 to-purple-600 relative overflow-hidden">
      {/* Diamond Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(45deg, white 25%, transparent 25%), linear-gradient(-45deg, white 25%, transparent 25%), linear-gradient(45deg, transparent 75%, white 75%), linear-gradient(-45deg, transparent 75%, white 75%)`,
          backgroundSize: '30px 30px',
          backgroundPosition: '0 0, 0 15px, 15px -15px, -15px 0px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">GayPHX Music Gallery</h1>
            </div>
            <nav className="hidden md:flex space-x-8">
              <Link 
                href={isAuthenticated ? "/dashboard" : "/"} 
                className="text-white hover:text-gray-200 transition-colors"
              >
                {isAuthenticated ? "Dashboard" : "Home"}
              </Link>
              {isAuthenticated && (
                <Link href="/artists" className="text-white hover:text-gray-200 transition-colors">
                  Artists
                </Link>
              )}
              <Link href="/submit" className="text-white hover:text-gray-200 transition-colors">
                Submit Music
              </Link>
              {!isAuthenticated && (
                <Link href="/auth/login" className="text-white hover:text-gray-200 transition-colors">
                  Login
                </Link>
              )}
            </nav>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        {/* Hero Section */}
        <div className="text-center mb-12">
          <h1 className="text-4xl md:text-5xl font-bold text-white mb-4">
            Featured Artists
          </h1>
          <p className="text-xl text-white/90 max-w-3xl mx-auto">
            Discover amazing music from LGBTQ+ artists in the Phoenix community. 
            These tracks have been approved and are featured on GayPHX Radio.
          </p>
        </div>

        {/* Search and Filters */}
        <div className="card mb-8">
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search artists or songs..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="input-field pl-10"
              />
            </div>
            <div className="flex items-center">
              <Filter className="h-5 w-5 text-gray-400 mr-2" />
              <select
                value={genreFilter}
                onChange={(e) => setGenreFilter(e.target.value)}
                className="input-field"
              >
                <option value="">All Genres</option>
                {genres.map(genre => (
                  <option key={genre} value={genre}>{genre}</option>
                ))}
              </select>
            </div>
            {isAuthenticated && (
              <div className="flex items-center">
                <button
                  onClick={() => setShowFavoritesOnly(!showFavoritesOnly)}
                  className={`flex items-center px-4 py-2 rounded-lg transition-colors ${
                    showFavoritesOnly 
                      ? 'bg-red-500 text-white' 
                      : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                  }`}
                >
                  <Heart className={`h-4 w-4 mr-2 ${showFavoritesOnly ? 'fill-current' : ''}`} />
                  {showFavoritesOnly ? 'Show All' : 'Favorites Only'}
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Artist Grid */}
        {filteredArtists.length === 0 ? (
          <div className="text-center py-12">
            <Music className="h-16 w-16 text-white/50 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-white mb-2">No artists found</h3>
            <p className="text-white/70">
              {searchTerm || genreFilter 
                ? 'Try adjusting your search or filters'
                : 'No artists have opted into the public gallery yet'
              }
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {filteredArtists.map((artist) => (
              <div key={artist.id} className="card group hover:shadow-xl transition-shadow">
                {/* Artist Info */}
                <div className="text-center mb-4">
                  <div className="w-20 h-20 bg-gayphx-gradient rounded-full flex items-center justify-center mx-auto mb-3">
                    <Music className="h-10 w-10 text-white" />
                  </div>
                  <h3 className="text-xl font-bold text-gray-900 mb-1">
                    {artist.name}
                  </h3>
                  {artist.pronouns && (
                    <p className="text-sm text-gray-600 mb-2">
                      {artist.pronouns}
                    </p>
                  )}
                  <p className="text-sm text-gray-600 mb-2">
                    {artist.public_track_count} public track{artist.public_track_count !== 1 ? 's' : ''}
                  </p>
                  {artist.bio && (
                    <p className="text-sm text-gray-600 line-clamp-2">
                      {artist.bio}
                    </p>
                  )}
                </div>

                {/* Featured Badge */}
                <div className="flex items-center justify-center mb-4">
                  <span className="inline-flex items-center px-3 py-1 bg-gradient-to-r from-yellow-400 to-orange-400 text-white text-sm font-medium rounded-full">
                    <Music className="h-4 w-4 mr-1" />
                    Featured on GayPHX Radio
                  </span>
                </div>

                {/* Public Tracks */}
                <div className="space-y-2 mb-4">
                  <h4 className="text-sm font-medium text-gray-700 text-center">Public Tracks</h4>
                  {artist.public_submissions.slice(0, 3).map((track) => (
                    <div key={track.id} className="flex items-center justify-between bg-gray-50 rounded p-2">
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-900">{track.song_title}</p>
                        {track.genre && (
                          <p className="text-xs text-gray-500">{track.genre}</p>
                        )}
                      </div>
                      <button
                        onClick={() => togglePlay(track.id)}
                        className="p-1 bg-gayphx-gradient rounded-full hover:opacity-90 transition-opacity ml-2"
                      >
                        {playingId === track.id ? (
                          <Pause className="h-3 w-3 text-white" />
                        ) : (
                          <Play className="h-3 w-3 text-white" />
                        )}
                      </button>
                    </div>
                  ))}
                  {artist.public_submissions.length > 3 && (
                    <p className="text-xs text-gray-500 text-center">
                      +{artist.public_submissions.length - 3} more tracks
                    </p>
                  )}
                </div>

                {/* Social Links */}
                {artist.social_links && Object.keys(artist.social_links).some(key => artist.social_links[key]) && (
                  <div className="flex justify-center space-x-4 mb-4">
                    {Object.entries(artist.social_links).map(([platform, url]) => (
                      url && (
                        <a
                          key={platform}
                          href={url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-gray-400 hover:text-gayphx-purple transition-colors"
                          title={`Visit ${platform}`}
                        >
                          <ExternalLink className="h-5 w-5" />
                        </a>
                      )
                    ))}
                  </div>
                )}

                {/* Action Buttons */}
                <div className="flex justify-center space-x-3 mb-4">
                  <Link
                    href={`/artists/${artist.id}`}
                    className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors text-sm font-medium"
                  >
                    View Artist
                  </Link>
                  {isAuthenticated && (
                    <button 
                      onClick={() => toggleFavorite(artist.id)}
                      className={`px-4 py-2 rounded-lg transition-colors text-sm font-medium flex items-center ${
                        favoriteArtists.has(artist.id)
                          ? 'bg-red-500 text-white hover:bg-red-600'
                          : 'bg-gayphx-gradient text-white hover:opacity-90'
                      }`}
                    >
                      <Heart className={`h-4 w-4 mr-2 ${favoriteArtists.has(artist.id) ? 'fill-current' : ''}`} />
                      {favoriteArtists.has(artist.id) ? 'Favorited' : 'Favorite'}
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Call to Action */}
        <div className="text-center mt-16">
          <h2 className="text-3xl font-bold text-white mb-4">
            Want to be featured here?
          </h2>
          <p className="text-white/90 text-lg mb-8">
            Submit your music to GayPHX and opt into our public gallery
          </p>
          <Link href="/submit" className="btn-primary text-lg px-8 py-4">
            Submit Your Music
          </Link>
        </div>
      </main>

      {/* Footer */}
      <footer className="bg-white/10 backdrop-blur-md border-t border-white/20 mt-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <div className="text-center text-white/80">
            <p>&copy; 2025 GayPHX Music Platform. Built with love for the LGBTQ+ community. 🌈</p>
          </div>
        </div>
      </footer>
    </div>
  )
}
