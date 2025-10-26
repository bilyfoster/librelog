'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, User, Mail, Calendar, Music, ExternalLink, UserCheck, UserX } from 'lucide-react'
import toast from 'react-hot-toast'

interface UserDetail {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
  is_active: boolean
  created_at: string
  submissions: Array<{
    id: string
    title: string
    status: string
    tracking_id: string
    isrc_requested: boolean
    created_at: string
    isrc_code?: string
  }>
}

interface ArtistProfile {
  id: string
  name: string
  email: string
  pronouns?: string
  bio?: string
  social_links: Record<string, string>
}

export default function UserDetailPage({ params }: { params: { id: string } }) {
  const [user, setUser] = useState<UserDetail | null>(null)
  const [artists, setArtists] = useState<ArtistProfile[]>([])
  const [loading, setLoading] = useState(true)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const router = useRouter()

  useEffect(() => {
    fetchUserDetails()
  }, [params.id])

  const fetchUserDetails = async () => {
    try {
      setLoading(true)
      const response = await fetch(`/api/admin/users/${params.id}`)
      if (response.ok) {
        const data = await response.json()
        setUser(data)
        
        // Fetch all artists for this user's email
        if (data.email) {
          const artistsResponse = await fetch(`http://localhost:8000/api/submissions/artists/${data.email}`)
          if (artistsResponse.ok) {
            const artistsData = await artistsResponse.json()
            setArtists(artistsData)
          }
        }
      } else {
        toast.error('Failed to fetch user details')
        router.push('/admin/users')
      }
    } catch (error) {
      console.error('Error fetching user details:', error)
      toast.error('Error fetching user details')
      router.push('/admin/users')
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteUser = async () => {
    if (!deletePassword.trim()) {
      toast.error('Please enter your password to confirm deletion')
      return
    }

    try {
      setDeleting(true)
      const response = await fetch(`/api/admin/users/${params.id}/delete`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ password: deletePassword }),
      })

      if (response.ok) {
        const data = await response.json()
        toast.success(data.message)
        router.push('/admin/users')
      } else {
        const errorData = await response.json()
        toast.error(errorData.detail || 'Failed to delete user')
      }
    } catch (error) {
      console.error('Error deleting user:', error)
      toast.error('Error deleting user')
    } finally {
      setDeleting(false)
      setShowDeleteModal(false)
      setDeletePassword('')
    }
  }

  const toggleUserStatus = async () => {
    if (!user) return
    
    try {
      const response = await fetch(`/api/admin/users/${user.id}/toggle-status`, {
        method: 'PUT'
      })
      
      if (response.ok) {
        toast.success(`User ${user.is_active ? 'deactivated' : 'activated'} successfully`)
        fetchUserDetails() // Refresh the data
      } else {
        toast.error('Failed to update user status')
      }
    } catch (error) {
      console.error('Error toggling user status:', error)
      toast.error('Error updating user status')
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'approved':
        return 'bg-green-100 text-green-800'
      case 'rejected':
        return 'bg-red-100 text-red-800'
      case 'pending':
        return 'bg-yellow-100 text-yellow-800'
      case 'needs_info':
        return 'bg-blue-100 text-blue-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gayphx-purple mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading user details...</p>
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-600">User not found</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-orange-400 to-yellow-600 relative overflow-hidden p-6">
      {/* Hexagon Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `radial-gradient(circle at 50% 50%, white 2px, transparent 2px)`,
          backgroundSize: '30px 30px'
        }}></div>
      </div>
      <div className="max-w-4xl mx-auto relative z-10">
        {/* Header */}
        <div className="mb-8">
          <button
            onClick={() => router.push('/admin/users')}
            className="flex items-center text-white hover:text-gray-200 mb-4"
          >
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Users
          </button>
          
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className="h-16 w-16 rounded-full bg-gayphx-gradient flex items-center justify-center">
                <span className="text-white font-bold text-xl">
                  {user.name.charAt(0).toUpperCase()}
                </span>
              </div>
              <div>
                <h1 className="text-3xl font-bold text-white">{user.name}</h1>
                <p className="text-white/80">{user.email}</p>
                {user.pronouns && (
                  <p className="text-sm text-white/60">{user.pronouns}</p>
                )}
              </div>
            </div>
            
            <div className="flex items-center gap-3">
              <span
                className={`inline-flex px-3 py-1 text-sm font-semibold rounded-full ${
                  user.is_active
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}
              >
                {user.is_active ? 'Active' : 'Inactive'}
              </span>
              <button
                onClick={toggleUserStatus}
                className={`flex items-center px-4 py-2 rounded-lg font-medium ${
                  user.is_active
                    ? 'bg-red-600 text-white hover:bg-red-700'
                    : 'bg-green-600 text-white hover:bg-green-700'
                }`}
              >
                {user.is_active ? (
                  <>
                    <UserX className="h-4 w-4 mr-2" />
                    Deactivate
                  </>
                ) : (
                  <>
                    <UserCheck className="h-4 w-4 mr-2" />
                    Activate
                  </>
                )}
              </button>
              
              <button
                onClick={() => setShowDeleteModal(true)}
                className="flex items-center px-4 py-2 rounded-lg font-medium bg-red-800 text-white hover:bg-red-900"
              >
                <UserX className="h-4 w-4 mr-2" />
                Delete User
              </button>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* User Info */}
          <div className="lg:col-span-1">
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">User Information</h2>
              
              <div className="space-y-4">
                <div className="flex items-center">
                  <Mail className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">{user.email}</span>
                </div>
                
                <div className="flex items-center">
                  <Calendar className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">
                    Joined {formatDate(user.created_at)}
                  </span>
                </div>
                
                <div className="flex items-center">
                  <Music className="h-5 w-5 text-gray-400 mr-3" />
                  <span className="text-sm text-gray-600">
                    {user.submissions.length} submissions
                  </span>
                </div>
              </div>

              {user.bio && (
                <div className="mt-6">
                  <h3 className="text-sm font-medium text-gray-900 mb-2">Bio</h3>
                  <p className="text-sm text-gray-600">{user.bio}</p>
                </div>
              )}

              {Object.keys(user.social_links).length > 0 && (
                <div className="mt-6">
                  <h3 className="text-sm font-medium text-gray-900 mb-2">Social Links</h3>
                  <div className="space-y-2">
                    {Object.entries(user.social_links).map(([platform, url]) => (
                      <a
                        key={platform}
                        href={url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex items-center text-sm text-gayphx-purple hover:text-gayphx-pink"
                      >
                        <ExternalLink className="h-4 w-4 mr-2" />
                        {platform}
                      </a>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Submissions */}
          <div className="lg:col-span-2">
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Submissions</h2>
              
              {user.submissions.length === 0 ? (
                <div className="text-center py-8">
                  <Music className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-600">No submissions yet</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {user.submissions.map((submission) => (
                    <div
                      key={submission.id}
                      className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50"
                    >
                      <div className="flex items-center justify-between mb-2">
                        <h3 className="font-medium text-gray-900">{submission.title}</h3>
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(
                            submission.status
                          )}`}
                        >
                          {submission.status.replace('_', ' ')}
                        </span>
                      </div>
                      
                      <div className="flex items-center justify-between text-sm text-gray-600">
                        <div className="flex items-center space-x-4">
                          <span>ID: {submission.tracking_id}</span>
                          {submission.isrc_requested && (
                            <span className="text-gayphx-purple">ISRC Requested</span>
                          )}
                          {submission.isrc_code && (
                            <span className="text-green-600">ISRC: {submission.isrc_code}</span>
                          )}
                        </div>
                        <span>{formatDate(submission.created_at)}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Artists Section */}
        <div className="mt-6">
          <div className="card">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">Artist Profiles</h2>
            
            {artists.length === 0 ? (
              <div className="text-center py-8">
                <User className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-600">No artist profiles found</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {artists.map((artist) => (
                  <div
                    key={artist.id}
                    className="border border-gray-200 rounded-lg p-4 hover:bg-gray-50"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div>
                        <h3 className="font-medium text-gray-900">{artist.name}</h3>
                        {artist.pronouns && (
                          <p className="text-sm text-gray-500">{artist.pronouns}</p>
                        )}
                      </div>
                      <span className="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                        Artist
                      </span>
                    </div>
                    
                    {artist.bio && (
                      <p className="text-sm text-gray-600 mb-3 line-clamp-2">{artist.bio}</p>
                    )}
                    
                    {Object.keys(artist.social_links).length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {Object.entries(artist.social_links).map(([platform, url]) => (
                          <a
                            key={platform}
                            href={url}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-flex items-center text-xs text-gayphx-purple hover:text-gayphx-pink"
                          >
                            <ExternalLink className="h-3 w-3 mr-1" />
                            {platform}
                          </a>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex items-center mb-4">
              <UserX className="h-6 w-6 text-red-600 mr-3" />
              <h3 className="text-lg font-semibold text-gray-900">Delete User</h3>
            </div>
            
            <div className="mb-4">
              <p className="text-gray-600 mb-2">
                You are about to permanently delete <strong>{user?.name}</strong> ({user?.email}).
              </p>
              <p className="text-red-600 text-sm mb-4">
                This action will also delete all {user?.submissions.length} submissions associated with this user.
                <strong> This action cannot be undone.</strong>
              </p>
            </div>
            
            <div className="mb-6">
              <label htmlFor="deletePassword" className="block text-sm font-medium text-gray-700 mb-2">
                Enter your admin password to confirm:
              </label>
              <input
                type="password"
                id="deletePassword"
                value={deletePassword}
                onChange={(e) => setDeletePassword(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-red-500"
                placeholder="Admin password"
                autoFocus
              />
            </div>
            
            <div className="flex space-x-3">
              <button
                onClick={() => {
                  setShowDeleteModal(false)
                  setDeletePassword('')
                }}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                disabled={deleting}
              >
                Cancel
              </button>
              <button
                onClick={handleDeleteUser}
                disabled={deleting || !deletePassword.trim()}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {deleting ? 'Deleting...' : 'Delete User'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
