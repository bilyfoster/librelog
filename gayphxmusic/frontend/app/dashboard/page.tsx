'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { Music, Upload, User, LogOut, Plus, FileAudio, CheckCircle, Clock, XCircle, Settings, Users } from 'lucide-react'
import Link from 'next/link'
import { useSystemConfig } from '../../lib/contexts/SystemConfigContext'
import toast from 'react-hot-toast'

interface Artist {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
}

interface Submission {
  id: string
  title: string
  status: string
  tracking_id: string
  created_at: string
  isrc_code?: string
  play_count?: number
  last_played_at?: string
}

interface PlayStats {
  total_plays: number
  radio_plays: number
  gallery_plays: number
  other_plays: number
  plays_this_week: number
  plays_this_month: number
  plays_this_year: number
}

export default function DashboardPage() {
  const { config } = useSystemConfig()
  const [artist, setArtist] = useState<Artist | null>(null)
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [playStats, setPlayStats] = useState<PlayStats | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()
  
  const organizationName = config?.organization_name || 'GayPHX Music Platform'

  useEffect(() => {
    // Only run on client side
    if (typeof window === 'undefined') return
    
    // Check if user is authenticated
    const token = localStorage.getItem('auth_token')
    const artistData = localStorage.getItem('artist_data')
    
    if (!token || !artistData) {
      router.push('/auth/login')
      return
    }

    try {
      setArtist(JSON.parse(artistData))
    } catch (error) {
      console.error('Error parsing artist data:', error)
      localStorage.removeItem('auth_token')
      localStorage.removeItem('artist_data')
      router.push('/auth/login')
      return
    }
    loadSubmissions()
  }, [router])

  const loadSubmissions = async () => {
    try {
      const token = localStorage.getItem('auth_token')
      const response = await fetch('/api/submissions/my', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setSubmissions(data)
        // Load play stats after submissions are loaded
        await loadPlayStatsForSubmissions(data)
      }
    } catch (error) {
      console.error('Error loading submissions:', error)
    } finally {
      setIsLoading(false)
    }
  }

  const loadPlayStatsForSubmissions = async (submissionsData: Submission[]) => {
    try {
      const token = localStorage.getItem('auth_token')
      
      // Calculate total play stats for this artist's submissions
      let totalStats = {
        total_plays: 0,
        radio_plays: 0,
        gallery_plays: 0,
        other_plays: 0,
        plays_this_week: 0,
        plays_this_month: 0,
        plays_this_year: 0
      }
      
      // Get play stats for each submission
      for (const submission of submissionsData) {
        const response = await fetch(`/api/plays/statistics/${submission.id}`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
        
        if (response.ok) {
          const submissionStats = await response.json()
          totalStats.total_plays += submissionStats.total_plays || 0
          totalStats.radio_plays += submissionStats.radio_plays || 0
          totalStats.gallery_plays += submissionStats.gallery_plays || 0
          totalStats.other_plays += submissionStats.other_plays || 0
          totalStats.plays_this_week += submissionStats.plays_this_week || 0
          totalStats.plays_this_month += submissionStats.plays_this_month || 0
          totalStats.plays_this_year += submissionStats.plays_this_year || 0
        }
      }
      
      setPlayStats(totalStats)
    } catch (error) {
      console.error('Error loading play statistics:', error)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('auth_token')
    localStorage.removeItem('artist_data')
    toast.success('Signed out successfully')
    router.push('/')
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'approved':
        return <CheckCircle className="h-5 w-5 text-green-500" />
      case 'pending':
        return <Clock className="h-5 w-5 text-yellow-500" />
      case 'rejected':
        return <XCircle className="h-5 w-5 text-red-500" />
      default:
        return <Clock className="h-5 w-5 text-gray-500" />
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'approved':
        return 'text-green-600 bg-green-100'
      case 'pending':
        return 'text-yellow-600 bg-yellow-100'
      case 'rejected':
        return 'text-red-600 bg-red-100'
      default:
        return 'text-gray-600 bg-gray-100'
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center">
        <div className="card text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gayphx-purple mx-auto mb-4"></div>
          <p className="text-gray-600">Loading your dashboard...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink relative overflow-hidden">
      {/* Background Pattern */}
      <div 
        className="absolute inset-0 opacity-10"
        style={{
          backgroundImage: `radial-gradient(circle at 25% 25%, white 2px, transparent 2px),
                           radial-gradient(circle at 75% 75%, white 2px, transparent 2px),
                           linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%)`,
          backgroundSize: '60px 60px, 60px 60px, 20px 20px',
          backgroundPosition: '0 0, 30px 30px, 0 0'
        }}
      />
      
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Music className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">{organizationName}</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-white">Welcome, {artist?.name}</span>
              <Link
                href="/gallery"
                className="text-white hover:text-gray-200 transition-colors flex items-center"
              >
                <Music className="h-4 w-4 mr-1" />
                Gallery
              </Link>
              <Link
                href="/artists"
                className="text-white hover:text-gray-200 transition-colors flex items-center"
              >
                <Users className="h-4 w-4 mr-1" />
                Artist Management
              </Link>
              <Link
                href="/profile"
                className="text-white hover:text-gray-200 transition-colors flex items-center"
              >
                <Settings className="h-4 w-4 mr-1" />
                Profile
              </Link>
              <button
                onClick={handleLogout}
                className="text-white hover:text-gray-200 transition-colors flex items-center"
              >
                <LogOut className="h-4 w-4 mr-1" />
                Sign Out
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 relative z-10">
        {/* Welcome Section */}
        <div className="card mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Welcome back, {artist?.name}!
              </h2>
              <p className="text-gray-600">
                Manage your music submissions and track your ISRC codes.
              </p>
            </div>
            <Link href="/submit" className="btn-primary flex items-center">
              <Plus className="h-4 w-4 mr-2" />
              Submit New Track
            </Link>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
          <div className="card text-center">
            <FileAudio className="h-8 w-8 text-gayphx-purple mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">{submissions.length}</div>
            <div className="text-gray-600">Total Submissions</div>
          </div>
          <div className="card text-center">
            <CheckCircle className="h-8 w-8 text-green-500 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">
              {submissions.filter(s => s.status === 'isrc_assigned').length}
            </div>
            <div className="text-gray-600">Processed</div>
          </div>
          <div className="card text-center">
            <Clock className="h-8 w-8 text-yellow-500 mx-auto mb-2" />
            <div className="text-2xl font-bold text-gray-900">
              {submissions.filter(s => s.status === 'pending').length}
            </div>
            <div className="text-gray-600">Pending Review</div>
          </div>
        </div>

        {/* Main Dashboard Cards */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Submissions */}
          <div className="card">
          <h3 className="text-xl font-semibold mb-4">Your Submissions</h3>
          
          {submissions.length === 0 ? (
            <div className="text-center py-8">
              <FileAudio className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-600 mb-4">You haven't submitted any music yet.</p>
              <Link href="/submit" className="btn-primary">
                Submit Your First Track
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {submissions
                .sort((a, b) => (b.play_count || 0) - (a.play_count || 0)) // Sort by play count descending
                .map((submission) => (
                <div key={submission.id} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      {getStatusIcon(submission.status)}
                      <div className="flex-1">
                        <h4 className="font-medium text-gray-900">{submission.title}</h4>
                        <div className="flex items-center space-x-4 text-sm text-gray-500">
                          <span>Submitted {new Date(submission.created_at).toLocaleDateString()}</span>
                          <div className="flex items-center space-x-1">
                            <span className="text-blue-600 font-medium">🎵 {submission.play_count || 0} plays</span>
                            {submission.last_played_at && (
                              <span className="text-gray-400">•</span>
                            )}
                            {submission.last_played_at && (
                              <span>Last played: {new Date(submission.last_played_at).toLocaleDateString()}</span>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-3">
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(submission.status)}`}>
                        {submission.status}
                      </span>
                      {submission.isrc_code && (
                        <span className="text-sm font-mono text-gray-600">
                          ISRC: {submission.isrc_code}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Play Statistics */}
        {submissions.length > 0 && (
          <div className="card">
            <h3 className="text-xl font-semibold mb-4">Play Statistics</h3>
            <p className="text-gray-600 mb-4">
              Track how often your music is played on GayPHX Radio and other platforms.
            </p>
            
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-2xl font-bold text-blue-600 mb-2">
                  {playStats?.total_plays || 0}
                </div>
                <div className="text-sm text-gray-600">Total Plays</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-2xl font-bold text-green-600 mb-2">
                  {playStats?.radio_plays || 0}
                </div>
                <div className="text-sm text-gray-600">Radio Plays</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-2xl font-bold text-purple-600 mb-2">
                  {playStats?.gallery_plays || 0}
                </div>
                <div className="text-sm text-gray-600">Gallery Plays</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-2xl font-bold text-orange-600 mb-2">
                  {playStats?.other_plays || 0}
                </div>
                <div className="text-sm text-gray-600">Other Plays</div>
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-lg font-semibold text-gray-900 mb-1">
                  {playStats?.plays_this_week || 0}
                </div>
                <div className="text-sm text-gray-600">This Week</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-lg font-semibold text-gray-900 mb-1">
                  {playStats?.plays_this_month || 0}
                </div>
                <div className="text-sm text-gray-600">This Month</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-gray-300 shadow-sm">
                <div className="text-lg font-semibold text-gray-900 mb-1">
                  {playStats?.plays_this_year || 0}
                </div>
                <div className="text-sm text-gray-600">This Year</div>
              </div>
            </div>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="bg-white rounded-lg p-4 text-center border border-green-300 shadow-sm">
                <div className="text-lg font-semibold text-gray-900 mb-1">N/A</div>
                <div className="text-sm text-gray-600">Most Played Time</div>
              </div>
              <div className="bg-white rounded-lg p-4 text-center border border-green-300 shadow-sm">
                <div className="text-lg font-semibold text-gray-900 mb-1">N/A</div>
                <div className="text-sm text-gray-600">Most Played Day</div>
              </div>
            </div>
          </div>
        )}
        </div>

        {/* Top Tracks - Full Width */}
        {submissions.length > 0 && (
          <div className="mt-6">
            <div className="card">
              <h3 className="text-xl font-semibold mb-4">🎵 Your Top Tracks</h3>
              <p className="text-gray-600 mb-4">
                Your most played tracks on GayPHX Radio. Tracks are sorted by total play count.
              </p>
              
              <div className="space-y-3">
                {submissions
                  .filter(sub => (sub.play_count || 0) > 0) // Only show tracks with plays
                  .sort((a, b) => (b.play_count || 0) - (a.play_count || 0)) // Sort by play count descending
                  .slice(0, 10) // Show top 10
                  .map((submission, index) => (
                    <div key={submission.id} className="flex items-center justify-between p-4 bg-gradient-to-r from-blue-50 to-purple-50 rounded-lg border border-blue-200">
                      <div className="flex items-center space-x-4">
                        <div className="flex items-center justify-center w-8 h-8 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-full font-bold text-sm">
                          {index + 1}
                        </div>
                        <div>
                          <h4 className="font-medium text-gray-900">{submission.title}</h4>
                          <div className="flex items-center space-x-4 text-sm text-gray-500">
                            <span>ISRC: {submission.isrc_code || 'Pending'}</span>
                            {submission.last_played_at && (
                              <span>Last played: {new Date(submission.last_played_at).toLocaleDateString()}</span>
                            )}
                          </div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-bold text-blue-600">{submission.play_count || 0}</div>
                        <div className="text-sm text-gray-500">plays</div>
                      </div>
                    </div>
                  ))}
                
                {submissions.filter(sub => (sub.play_count || 0) > 0).length === 0 && (
                  <div className="text-center py-8">
                    <div className="text-6xl mb-4">🎵</div>
                    <p className="text-gray-600 mb-2">No plays yet</p>
                    <p className="text-sm text-gray-500">Your tracks will appear here once they start getting played on GayPHX Radio</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Rights Management - Full Width */}
        <div className="mt-6">
          <div className="card">
          <h3 className="text-xl font-semibold mb-4">Rights Management</h3>
          <p className="text-gray-600 mb-4">
            Manage permissions for your submitted tracks. You can grant or revoke rights for radio play, 
            public display, streaming, and commercial use.
          </p>
          
          {submissions.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-gray-600">Submit tracks to manage their rights.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {submissions.map((submission) => (
                <RightsManagementCard key={submission.id} submission={submission} />
              ))}
            </div>
          )}
          </div>
        </div>
      </main>
    </div>
  )
}

// Rights Management Component
function RightsManagementCard({ submission }: { submission: any }) {
  const [rights, setRights] = useState<any>(null)
  const [playStats, setPlayStats] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [expanded, setExpanded] = useState(false)

  useEffect(() => {
    loadRights()
    loadPlayStats()
  }, [submission.id])

  const loadRights = async () => {
    try {
      const response = await fetch(`/api/rights/submissions/${submission.id}/rights`)
      if (response.ok) {
        const data = await response.json()
        setRights(data)
      }
    } catch (error) {
      console.error('Error loading rights:', error)
    }
  }

  const loadPlayStats = async () => {
    try {
      const response = await fetch(`/api/plays/statistics/${submission.id}`)
      if (response.ok) {
        const data = await response.json()
        setPlayStats(data)
      }
    } catch (error) {
      console.error('Error loading play stats:', error)
    }
  }

  const updateRights = async (permissionType: string, value: boolean) => {
    setLoading(true)
    try {
      const response = await fetch(`/api/rights/submissions/${submission.id}/rights`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('auth_token')}`
        },
        body: JSON.stringify({
          [permissionType]: value,
          reason: `${value ? 'Granted' : 'Revoked'} ${permissionType.replace('_permission', '')} permission`
        })
      })

      if (response.ok) {
        await loadRights()
        toast.success(`Rights updated successfully`)
      } else {
        toast.error('Failed to update rights')
      }
    } catch (error) {
      console.error('Error updating rights:', error)
      toast.error('Failed to update rights')
    } finally {
      setLoading(false)
    }
  }

  if (!rights) {
    return (
      <div className="border border-gray-200 rounded-lg p-4">
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
          <div className="h-3 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    )
  }

  return (
    <div className="border border-gray-200 rounded-lg p-4">
      <div className="flex items-center justify-between mb-3">
        <div>
          <h4 className="font-medium text-gray-900">{submission.title}</h4>
          <p className="text-sm text-gray-500">Tracking ID: {submission.tracking_id}</p>
          {playStats && (
            <div className="mt-1 flex items-center space-x-4 text-sm">
              <span className="text-blue-600 font-medium">
                🎵 {playStats.total_plays || 0} plays
              </span>
              {playStats.last_played_at && (
                <span className="text-gray-500">
                  Last played: {new Date(playStats.last_played_at).toLocaleDateString()}
                </span>
              )}
            </div>
          )}
        </div>
        <button
          onClick={() => setExpanded(!expanded)}
          className="text-gayphx-purple hover:text-gayphx-pink transition-colors"
        >
          {expanded ? 'Collapse' : 'Manage Rights'}
        </button>
      </div>

      {expanded && (
        <div className="space-y-3 pt-3 border-t border-gray-100">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <RightsToggle
              label="Radio Play Permission"
              description="Allow GayPHX Radio to play this track"
              value={rights.radio_play_permission}
              onChange={(value) => updateRights('radio_play_permission', value)}
              loading={loading}
              grantedAt={rights.radio_play_granted_at}
              revokedAt={rights.radio_play_revoked_at}
            />
            
            <RightsToggle
              label="Public Display Permission"
              description="Allow public display on website/gallery"
              value={rights.public_display_permission}
              onChange={(value) => updateRights('public_display_permission', value)}
              loading={loading}
              grantedAt={rights.public_display_granted_at}
              revokedAt={rights.public_display_revoked_at}
            />
            
            <RightsToggle
              label="Streaming Permission"
              description="Allow streaming on digital platforms"
              value={rights.streaming_permission}
              onChange={(value) => updateRights('streaming_permission', value)}
              loading={loading}
              grantedAt={rights.streaming_granted_at}
              revokedAt={rights.streaming_revoked_at}
            />
            
            <RightsToggle
              label="Commercial Use Permission"
              description="Allow commercial use and licensing"
              value={rights.commercial_use_permission}
              onChange={(value) => updateRights('commercial_use_permission', value)}
              loading={loading}
              grantedAt={rights.commercial_use_granted_at}
              revokedAt={rights.commercial_use_revoked_at}
            />
          </div>

          {rights.custom_terms && (
            <div className="mt-4 p-3 bg-gray-50 rounded">
              <h5 className="font-medium text-gray-900 mb-1">Custom Terms</h5>
              <p className="text-sm text-gray-600">{rights.custom_terms}</p>
            </div>
          )}

          {rights.restrictions && (
            <div className="mt-4 p-3 bg-yellow-50 rounded">
              <h5 className="font-medium text-yellow-800 mb-1">Restrictions</h5>
              <p className="text-sm text-yellow-700">{rights.restrictions}</p>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// Rights Toggle Component
function RightsToggle({ 
  label, 
  description, 
  value, 
  onChange, 
  loading, 
  grantedAt, 
  revokedAt 
}: {
  label: string
  description: string
  value: boolean
  onChange: (value: boolean) => void
  loading: boolean
  grantedAt?: string
  revokedAt?: string
}) {
  return (
    <div className="flex items-center justify-between p-3 border border-gray-200 rounded-lg">
      <div className="flex-1">
        <h5 className="font-medium text-gray-900">{label}</h5>
        <p className="text-sm text-gray-600">{description}</p>
        {grantedAt && (
          <p className="text-xs text-green-600 mt-1">
            Granted: {new Date(grantedAt).toLocaleDateString()}
          </p>
        )}
        {revokedAt && (
          <p className="text-xs text-red-600 mt-1">
            Revoked: {new Date(revokedAt).toLocaleDateString()}
          </p>
        )}
      </div>
      <button
        onClick={() => onChange(!value)}
        disabled={loading}
        className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
          value ? 'bg-gayphx-purple' : 'bg-gray-200'
        } ${loading ? 'opacity-50 cursor-not-allowed' : ''}`}
      >
        <span
          className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
            value ? 'translate-x-6' : 'translate-x-1'
          }`}
        />
      </button>
    </div>
  )
}