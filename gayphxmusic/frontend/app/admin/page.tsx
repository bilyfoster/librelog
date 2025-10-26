'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
// import { adminApi } from '@/lib/api'
import { Music, Users, Award, Download, Filter, Search, Shield, Key, BarChart3, Settings } from 'lucide-react'
import Link from 'next/link'
import toast from 'react-hot-toast'

interface Submission {
  id: string
  tracking_id: string
  status: string
  song_title: string
  genre?: string
  submitted_at: string
  isrc_requested: boolean
  radio_permission: boolean
  public_display: boolean
  play_stats?: {
    total_plays: number
    radio_plays: number
    gallery_plays: number
    other_plays: number
    last_played_at?: string
  }
  artist: {
    name: string
    email: string
    pronouns?: string
  }
  isrc?: {
    isrc_code: string
    assigned_at: string
  }
}

interface Stats {
  total_submissions: number
  isrc_requests: number
  approved_submissions: number
  isrcs_assigned: number
  status_breakdown: Record<string, number>
}

interface TopTrack {
  id: string
  song_title: string
  artist_name: string
  total_plays: number
  radio_plays: number
  gallery_plays: number
  last_played_at?: string
  status: string
  radio_permission: boolean
  public_display: boolean
}

export default function AdminPage() {
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [stats, setStats] = useState<Stats | null>(null)
  const [topTracks, setTopTracks] = useState<TopTrack[]>([])
  const [loading, setLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [adminData, setAdminData] = useState<any>(null)
  const router = useRouter()
  const [filter, setFilter] = useState({
    status: '',
    isrc_requested: undefined as boolean | undefined
  })
  const [radioPermissions, setRadioPermissions] = useState<any[]>([])
  const [radioPermissionsLoading, setRadioPermissionsLoading] = useState(false)

  useEffect(() => {
    // Check admin authentication
    if (typeof window !== 'undefined') {
      const adminToken = localStorage.getItem('admin_token')
      const adminData = localStorage.getItem('admin_data')
      
      if (!adminToken || !adminData) {
        router.push('/admin/login')
        return
      }
      
      try {
        setIsAuthenticated(true)
        setAdminData(JSON.parse(adminData))
      } catch (error) {
        console.error('Error parsing admin data:', error)
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_data')
        router.push('/admin/login')
        return
      }
    }
  }, [router])

  useEffect(() => {
    if (isAuthenticated) {
      fetchData()
    }
  }, [filter, isAuthenticated])

  const fetchData = async () => {
    try {
      const filterParams = new URLSearchParams()
      if (filter.status) filterParams.append('status', filter.status)
      if (filter.isrc_requested !== undefined) filterParams.append('isrc_requested', filter.isrc_requested.toString())
      
      const [submissionsResponse, statsResponse, topTracksResponse] = await Promise.all([
        fetch(`http://localhost:8000/api/admin/submissions?${filterParams.toString()}`),
        fetch('http://localhost:8000/api/admin/stats'),
        fetch('http://localhost:8000/api/admin/top-tracks')
      ])
      
      const submissionsData = await submissionsResponse.json()
      const statsData = await statsResponse.json()
      const topTracksData = await topTracksResponse.json()
      setSubmissions(submissionsData)
      setStats(statsData)
      setTopTracks(topTracksData)
    } catch (error) {
      console.error('Error fetching data:', error)
    } finally {
      setLoading(false)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'pending':
        return 'bg-yellow-100 text-yellow-800'
      case 'approved':
        return 'bg-green-100 text-green-800'
      case 'rejected':
        return 'bg-red-100 text-red-800'
      case 'needs_info':
        return 'bg-orange-100 text-orange-800'
      case 'isrc_assigned':
        return 'bg-blue-100 text-blue-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const handleStatusChange = async (submissionId: string, newStatus: string) => {
    try {
      await fetch(`http://localhost:8000/api/admin/submissions/${submissionId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: newStatus })
      })
      fetchData() // Refresh data
    } catch (error) {
      console.error('Error updating status:', error)
    }
  }

  const handleAssignIsrc = async (submissionId: string) => {
    try {
      await fetch(`http://localhost:8000/api/admin/submissions/${submissionId}/assign-isrc`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ assigned_by: 'admin' }) // TODO: Get actual admin ID
      })
      fetchData() // Refresh data
    } catch (error) {
      console.error('Error assigning ISRC:', error)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_data')
    setIsAuthenticated(false)
    setAdminData(null)
    toast.success('Logged out successfully!')
    router.push('/admin/login')
  }

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <Shield className="h-16 w-16 text-gray-400 mx-auto mb-4" />
          <div className="text-xl text-gray-600">Checking authentication...</div>
        </div>
      </div>
    )
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-xl">Loading...</div>
      </div>
    )
  }

  const loadRadioPermissions = async () => {
    setRadioPermissionsLoading(true)
    try {
      const response = await fetch('/api/admin/radio-permissions')
      if (response.ok) {
        const data = await response.json()
        setRadioPermissions(data)
      } else {
        toast.error('Failed to load radio permissions')
      }
    } catch (error) {
      console.error('Error loading radio permissions:', error)
      toast.error('Failed to load radio permissions')
    } finally {
      setRadioPermissionsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-yellow-400 to-orange-500 relative overflow-hidden">
      {/* Checkerboard Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(45deg, white 25%, transparent 25%), linear-gradient(-45deg, white 25%, transparent 25%), linear-gradient(45deg, transparent 75%, white 75%), linear-gradient(-45deg, transparent 75%, white 75%)`,
          backgroundSize: '20px 20px',
          backgroundPosition: '0 0, 0 10px, 10px -10px, -10px 0px'
        }}></div>
      </div>
      
      {/* Admin Warning Banner */}
      <div className="bg-yellow-600 border-b-4 border-yellow-800 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2">
          <div className="flex items-center justify-center">
            <Shield className="h-5 w-5 text-yellow-100 mr-2" />
            <span className="text-yellow-100 font-semibold">ADMIN AREA - Authorized Personnel Only</span>
          </div>
        </div>
      </div>

      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center">
              <Music className="h-8 w-8 text-white mr-3" />
              <h1 className="text-2xl font-bold text-white">GayPHX Admin Dashboard</h1>
            </div>
            <div className="flex space-x-4">
              <Link href="/admin/play-tracking" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors flex items-center">
                <BarChart3 className="h-4 w-4 mr-2" />
                Play Tracking
              </Link>
              <Link href="/admin/users" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors flex items-center">
                <Users className="h-4 w-4 mr-2" />
                Users
              </Link>
              <Link href="/admin/admins" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors flex items-center">
                <Shield className="h-4 w-4 mr-2" />
                Admin Management
              </Link>
              <Link href="/admin/profile" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors flex items-center">
                <Shield className="h-4 w-4 mr-2" />
                Profile
              </Link>
              <Link href="/admin/config" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors flex items-center">
                <Settings className="h-4 w-4 mr-2" />
                Configure
              </Link>
              <Link href="/help" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors">
                Help
              </Link>
              <Link href="/" className="bg-white/20 text-white hover:bg-white/30 px-4 py-2 rounded-lg transition-colors">
                View Site
              </Link>
              <button onClick={handleLogout} className="bg-white text-orange-600 hover:bg-gray-100 px-4 py-2 rounded-lg transition-colors font-semibold">
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 relative z-10">
        {/* Stats Cards */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
            <div className="card">
              <div className="flex items-center">
                <Users className="h-8 w-8 text-blue-500" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Total Submissions</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.total_submissions}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <Award className="h-8 w-8 text-green-500" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">ISRC Requests</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.isrc_requests}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <Music className="h-8 w-8 text-purple-500" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">Processed</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.approved_submissions}</p>
                </div>
              </div>
            </div>

            <div className="card">
              <div className="flex items-center">
                <Award className="h-8 w-8 text-yellow-500" />
                <div className="ml-4">
                  <p className="text-sm font-medium text-gray-600">ISRCs Assigned</p>
                  <p className="text-2xl font-bold text-gray-900">{stats.isrcs_assigned}</p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Filters */}
        <div className="card mb-6">
          <div className="flex flex-wrap gap-4">
            <div className="flex items-center">
              <Filter className="h-5 w-5 text-gray-400 mr-2" />
              <select
                value={filter.status}
                onChange={(e) => setFilter({ ...filter, status: e.target.value })}
                className="input-field"
              >
                <option value="">All Statuses</option>
                <option value="pending">Pending</option>
                <option value="approved">Approved</option>
                <option value="rejected">Rejected</option>
                <option value="needs_info">Needs Info</option>
                <option value="isrc_assigned">ISRC Assigned</option>
              </select>
            </div>

            <div className="flex items-center">
                <select
                  value={filter.isrc_requested ? 'true' : 'false'}
                  onChange={(e) => setFilter({ ...filter, isrc_requested: e.target.value === 'true' })}
                  className="input-field"
                >
                <option value="">All ISRC Requests</option>
                <option value="true">ISRC Requested</option>
                <option value="false">No ISRC Request</option>
              </select>
            </div>

            <div className="flex items-center">
              <button
                onClick={() => setFilter({ status: '', isrc_requested: undefined })}
                className="btn-secondary"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>

        {/* Top Performing Tracks - Full Width */}
        {topTracks.length > 0 && (
          <div className="mb-6">
            <div className="card">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <BarChart3 className="h-5 w-5 mr-2 text-orange-600" />
                  Top Performing Tracks
                </h2>
                <span className="text-sm text-gray-500">Based on total play count</span>
              </div>
            <div className="space-y-3">
              {topTracks.map((track, index) => (
                <div key={track.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors">
                  <div className="flex items-center space-x-4">
                    <div className="flex-shrink-0 w-8 h-8 bg-orange-100 text-orange-600 rounded-full flex items-center justify-center font-bold text-sm">
                      {index + 1}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center space-x-2">
                        <h3 className="text-sm font-medium text-gray-900 truncate">
                          {track.song_title}
                        </h3>
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          track.status === 'approved' ? 'bg-green-100 text-green-800' : 
                          track.status === 'pending' ? 'bg-yellow-100 text-yellow-800' : 
                          'bg-gray-100 text-gray-800'
                        }`}>
                          {track.status}
                        </span>
                        {!track.radio_permission && track.total_plays > 0 && (
                          <span className="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800">
                            Not in Radio
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-500">{track.artist_name}</p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-6 text-sm text-gray-600">
                    <div className="text-center">
                      <div className="font-semibold text-gray-900">{track.total_plays}</div>
                      <div className="text-xs">Total</div>
                    </div>
                    <div className="text-center">
                      <div className="font-semibold text-green-600">{track.radio_plays}</div>
                      <div className="text-xs">Radio</div>
                    </div>
                    <div className="text-center">
                      <div className="font-semibold text-purple-600">{track.gallery_plays}</div>
                      <div className="text-xs">Gallery</div>
                    </div>
                    <div className="text-right">
                      <Link
                        href={`/admin/submissions/${track.id}`}
                        className="text-orange-600 hover:text-orange-500 font-medium"
                      >
                        View Details
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            </div>
          </div>
        )}

        {/* Submissions Table - Full Width */}
        <div className="mt-6">
          <div className="card">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Artist
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Song
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Submitted
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    ISRC
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total Plays
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Radio Plays
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Gallery Plays
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {submissions.map((submission) => (
                  <tr key={submission.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {submission.artist.name}
                        </div>
                        <div className="text-sm text-gray-500">
                          {submission.artist.email}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {submission.song_title}
                        </div>
                        <div className="text-sm text-gray-500">
                          {submission.genre || 'No genre'}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(submission.status)}`}>
                        {submission.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(submission.submitted_at).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      {submission.isrc ? (
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {submission.isrc.isrc_code}
                          </div>
                          <div className="text-sm text-gray-500">
                            {new Date(submission.isrc.assigned_at).toLocaleDateString()}
                          </div>
                        </div>
                      ) : submission.isrc_requested ? (
                        <span className="text-sm text-orange-600">Pending</span>
                      ) : (
                        <span className="text-sm text-gray-400">Not requested</span>
                      )}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 text-center">
                      <div className="font-semibold">
                        {submission.play_stats?.total_plays || 0}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-green-600 text-center">
                      <div className="font-semibold">
                        {submission.play_stats?.radio_plays || 0}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-purple-600 text-center">
                      <div className="font-semibold">
                        {submission.play_stats?.gallery_plays || 0}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        {submission.status === 'pending' && (
                          <>
                            <button
                              onClick={() => handleStatusChange(submission.id, 'approved')}
                              className="text-green-600 hover:text-green-900"
                            >
                              Approve
                            </button>
                            <button
                              onClick={() => handleStatusChange(submission.id, 'rejected')}
                              className="text-red-600 hover:text-red-900"
                            >
                              Reject
                            </button>
                          </>
                        )}
                        {submission.status === 'approved' && submission.isrc_requested && !submission.isrc && (
                          <button
                            onClick={() => handleAssignIsrc(submission.id)}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            Assign ISRC
                          </button>
                        )}
                        <Link
                          href={`/admin/submissions/${submission.id}`}
                          className="text-gray-600 hover:text-gray-900"
                        >
                          View
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          </div>
        </div>

        {/* Radio Permissions Section */}
        <div className="card mt-8">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900">Radio Play Permissions</h2>
            <button 
              onClick={loadRadioPermissions}
              className="btn-secondary flex items-center"
            >
              <Music className="h-4 w-4 mr-2" />
              Refresh
            </button>
          </div>
          
          <RadioPermissionsList />
        </div>

        {/* Export Options */}
        <div className="mt-6 flex justify-end space-x-4">
          <button className="btn-secondary flex items-center">
            <Download className="h-4 w-4 mr-2" />
            Export CSV
          </button>
          <button className="btn-secondary flex items-center">
            <Download className="h-4 w-4 mr-2" />
            Export JSON
          </button>
        </div>
      </div>
    </div>
  )
}

// Radio Permissions List Component
function RadioPermissionsList() {
  const [permissions, setPermissions] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadPermissions()
  }, [])

  const loadPermissions = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/admin/radio-permissions')
      if (response.ok) {
        const data = await response.json()
        setPermissions(data)
      }
    } catch (error) {
      console.error('Error loading radio permissions:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="text-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gayphx-purple mx-auto mb-4"></div>
        <p className="text-gray-600">Loading radio permissions...</p>
      </div>
    )
  }

  if (permissions.length === 0) {
    return (
      <div className="text-center py-8">
        <Music className="h-12 w-12 text-gray-400 mx-auto mb-4" />
        <p className="text-gray-600">No radio play permissions granted yet.</p>
      </div>
    )
  }

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Song
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Artist
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Permission Status
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Granted Date
            </th>
            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {permissions.map((permission) => (
            <tr key={permission.submission_id}>
              <td className="px-6 py-4 whitespace-nowrap">
                <div>
                  <div className="text-sm font-medium text-gray-900">{permission.song_title}</div>
                  <div className="text-sm text-gray-500">ID: {permission.tracking_id}</div>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <div>
                  <div className="text-sm font-medium text-gray-900">{permission.artist_name}</div>
                  <div className="text-sm text-gray-500">{permission.artist_email}</div>
                </div>
              </td>
              <td className="px-6 py-4 whitespace-nowrap">
                <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                  permission.is_active 
                    ? 'bg-green-100 text-green-800' 
                    : 'bg-red-100 text-red-800'
                }`}>
                  {permission.is_active ? 'Active' : 'Revoked'}
                </span>
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {permission.radio_permission_granted_at 
                  ? new Date(permission.radio_permission_granted_at).toLocaleDateString()
                  : 'N/A'
                }
              </td>
              <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <Link 
                  href={`/admin/submissions/${permission.submission_id}`}
                  className="text-gayphx-purple hover:text-gayphx-pink"
                >
                  View Details
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
