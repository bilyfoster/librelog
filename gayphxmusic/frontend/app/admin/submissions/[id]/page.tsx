'use client'

import { useState, useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { ArrowLeft, Music, User, Mail, Calendar, Download, Award, FileAudio, Shield, Key, ExternalLink, Settings } from 'lucide-react'
import Link from 'next/link'
import toast from 'react-hot-toast'
import AudioPlayer from '@/components/AudioPlayer'

interface SubmissionDetail {
  id: string
  tracking_id: string
  status: string
  song_title: string
  genre?: string
  submitted_at: string
  isrc_requested: boolean
  radio_permission: boolean
  public_display: boolean
  admin_notes?: string
  play_stats?: {
    total_plays: number
    radio_plays: number
    gallery_plays: number
    other_plays: number
    last_played_at?: string
    libretime_plays: number
  }
  artist: {
    id: string
    name: string
    email: string
    pronouns?: string
    bio?: string
    social_links: Record<string, string>
  }
  isrc?: {
    isrc_code: string
    assigned_at: string
  }
  file_info?: {
    file_name: string
    file_size: number
    file_type: string
    duration_seconds?: number
    loudness_lufs?: number
    true_peak_dbfs?: number
  }
}

export default function SubmissionDetailPage({ params }: { params: { id: string } }) {
  const [submission, setSubmission] = useState<SubmissionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const [audioFileName, setAudioFileName] = useState<string>('')
  const [audioLoading, setAudioLoading] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [deletePassword, setDeletePassword] = useState('')
  const [deleting, setDeleting] = useState(false)
  const [radioPermission, setRadioPermission] = useState(false)
  const [publicDisplay, setPublicDisplay] = useState(false)
  const [adminNotes, setAdminNotes] = useState('')
  const router = useRouter()

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
      fetchSubmissionDetails()
    }
  }, [params.id, isAuthenticated])

  useEffect(() => {
    if (submission) {
      fetchAudioUrl()
      setRadioPermission(submission.radio_permission)
      setPublicDisplay(submission.public_display)
      setAdminNotes(submission.admin_notes || '')
    }
  }, [submission])

  const fetchAudioUrl = async () => {
    if (!submission) return
    
    try {
      setAudioLoading(true)
      // Use streaming endpoint to avoid CORS issues
      const audioStreamUrl = `/api/admin/submissions/${submission.id}/audio-stream`
      setAudioUrl(audioStreamUrl)
      setAudioFileName(submission.file_info?.file_name || 'audio')
    } catch (error) {
      console.error('Error setting up audio stream:', error)
    } finally {
      setAudioLoading(false)
    }
  }

  const fetchSubmissionDetails = async () => {
    try {
      setLoading(true)
      const adminToken = localStorage.getItem('admin_token')
      const response = await fetch(`/api/admin/submissions/${params.id}`, {
        headers: {
          'Authorization': `Bearer ${adminToken}`,
        },
      })
      if (response.ok) {
        const data = await response.json()
        setSubmission(data)
      } else {
        toast.error('Failed to fetch submission details')
        router.push('/admin')
      }
    } catch (error) {
      console.error('Error fetching submission details:', error)
      toast.error('Error fetching submission details')
      router.push('/admin')
    } finally {
      setLoading(false)
    }
  }

  const handleStatusChange = async (newStatus: string) => {
    if (!submission) return
    
    try {
      const response = await fetch(`/api/admin/submissions/${submission.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          status: newStatus,
          admin_notes: adminNotes
        })
      })
      
      if (response.ok) {
        toast.success(`Submission ${newStatus} successfully`)
        fetchSubmissionDetails() // Refresh the data
      } else {
        toast.error('Failed to update submission status')
      }
    } catch (error) {
      console.error('Error updating submission status:', error)
      toast.error('Error updating submission status')
    }
  }

  const handleApproveWithPermissions = async () => {
    if (!submission) return
    
    try {
      const response = await fetch(`/api/admin/submissions/${submission.id}/approve`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          radio_permission: radioPermission,
          public_display: publicDisplay,
          admin_notes: adminNotes
        })
      })
      
      if (response.ok) {
        toast.success('Submission approved successfully')
        fetchSubmissionDetails() // Refresh the data
      } else {
        const errorData = await response.json()
        toast.error(errorData.detail || 'Failed to approve submission')
      }
    } catch (error) {
      console.error('Error approving submission:', error)
      toast.error('Error approving submission')
    }
  }

  const handleAssignIsrc = async () => {
    if (!submission) return
    
    try {
      const response = await fetch(`/api/admin/submissions/${submission.id}/assign-isrc`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          assigned_by: 'admin'
        })
      })
      
      if (response.ok) {
        toast.success('ISRC assigned successfully')
        fetchSubmissionDetails() // Refresh the data
      } else {
        toast.error('Failed to assign ISRC')
      }
    } catch (error) {
      console.error('Error assigning ISRC:', error)
      toast.error('Error assigning ISRC')
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

  const handleLogout = () => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_data')
    router.push('/admin/login')
  }

  const handleDeleteSubmission = async () => {
    if (!deletePassword.trim()) {
      toast.error('Please enter your password to confirm deletion')
      return
    }

    try {
      setDeleting(true)
      const response = await fetch(`/api/admin/submissions/${params.id}/delete`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ password: deletePassword }),
      })

      if (response.ok) {
        const data = await response.json()
        toast.success(data.message)
        router.push('/admin')
      } else {
        const errorData = await response.json()
        toast.error(errorData.detail || 'Failed to delete submission')
      }
    } catch (error) {
      console.error('Error deleting submission:', error)
      toast.error('Error deleting submission')
    } finally {
      setDeleting(false)
      setShowDeleteModal(false)
      setDeletePassword('')
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
        return 'bg-blue-100 text-blue-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  if (!isAuthenticated) {
    return null
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600"></div>
      </div>
    )
  }

  if (!submission) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Submission Not Found</h1>
          <Link href="/admin" className="btn-primary">
            Back to Admin Dashboard
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div className="flex items-center">
              <Music className="h-8 w-8 text-gayphx-purple mr-3" />
              <div>
                <h1 className="text-3xl font-bold text-gray-900">Submission Review</h1>
                <p className="text-gray-600">{submission.song_title} • Tracking ID: {submission.tracking_id}</p>
              </div>
            </div>
            <div className="flex items-center space-x-4">
              <span className={`inline-flex px-3 py-1 text-sm font-semibold rounded-full ${getStatusColor(submission.status)}`}>
                {submission.status.replace('_', ' ')}
              </span>
              <Link href="/admin/users" className="btn-secondary flex items-center">
                <User className="h-4 w-4 mr-2" />
                Users
              </Link>
              <Link href="/admin/profile" className="btn-secondary flex items-center">
                <Shield className="h-4 w-4 mr-2" />
                Profile
              </Link>
              <Link href="/admin/config" className="btn-secondary flex items-center">
                <Settings className="h-4 w-4 mr-2" />
                Configure
              </Link>
              <button
                onClick={() => router.push('/admin')}
                className="btn-secondary"
              >
                Back to Dashboard
              </button>
              <button onClick={handleLogout} className="btn-danger">
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Submission Details */}
          <div className="lg:col-span-2 space-y-6">
            {/* Basic Info */}
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Submission Details</h2>
              <dl className="grid grid-cols-1 gap-4">
                <div>
                  <dt className="text-sm font-medium text-gray-500">Song Title</dt>
                  <dd className="text-sm text-gray-900">{submission.song_title}</dd>
                </div>
                {submission.genre && (
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Genre</dt>
                    <dd className="text-sm text-gray-900">{submission.genre}</dd>
                  </div>
                )}
                <div>
                  <dt className="text-sm font-medium text-gray-500">Submitted</dt>
                  <dd className="text-sm text-gray-900">{formatDate(submission.submitted_at)}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">ISRC Requested</dt>
                  <dd className="text-sm text-gray-900">{submission.isrc_requested ? 'Yes' : 'No'}</dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">Radio Permission</dt>
                  <dd className="text-sm text-gray-900">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                      submission.radio_permission ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {submission.radio_permission ? 'Approved' : 'Not Approved'}
                    </span>
                  </dd>
                </div>
                <div>
                  <dt className="text-sm font-medium text-gray-500">Gallery Display</dt>
                  <dd className="text-sm text-gray-900">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                      submission.public_display ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                    }`}>
                      {submission.public_display ? 'Approved' : 'Not Approved'}
                    </span>
                  </dd>
                </div>
              </dl>
            </div>

            {/* Play Statistics */}
            {submission.play_stats && (
              <div className="card">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Play Statistics</h2>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <div className="text-2xl font-bold text-blue-600">{submission.play_stats.total_plays}</div>
                    <div className="text-sm text-gray-600">Total Plays</div>
                  </div>
                  <div className="text-center p-4 bg-green-50 rounded-lg">
                    <div className="text-2xl font-bold text-green-600">{submission.play_stats.radio_plays}</div>
                    <div className="text-sm text-gray-600">Radio Plays</div>
                  </div>
                  <div className="text-center p-4 bg-purple-50 rounded-lg">
                    <div className="text-2xl font-bold text-purple-600">{submission.play_stats.gallery_plays}</div>
                    <div className="text-sm text-gray-600">Gallery Plays</div>
                  </div>
                  <div className="text-center p-4 bg-orange-50 rounded-lg">
                    <div className="text-2xl font-bold text-orange-600">{submission.play_stats.other_plays}</div>
                    <div className="text-sm text-gray-600">Other Plays</div>
                  </div>
                </div>
                {submission.play_stats.last_played_at && (
                  <div className="mt-3 text-sm text-gray-600">
                    Last played: {formatDate(submission.play_stats.last_played_at)}
                  </div>
                )}
              </div>
            )}

            {/* Audio Player */}
            {audioUrl && (
              <AudioPlayer 
                audioUrl={audioUrl} 
                fileName={audioFileName}
                className="mb-6"
              />
            )}

            {/* File Information */}
            {submission.file_info && (
              <div className="card">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">File Information</h2>
                <dl className="grid grid-cols-1 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">File Name</dt>
                    <dd className="text-sm text-gray-900">{submission.file_info.file_name}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">File Size</dt>
                    <dd className="text-sm text-gray-900">{(submission.file_info.file_size / 1024 / 1024).toFixed(2)} MB</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">File Type</dt>
                    <dd className="text-sm text-gray-900">{submission.file_info.file_type}</dd>
                  </div>
                  {submission.file_info.duration_seconds && (
                    <div>
                      <dt className="text-sm font-medium text-gray-500">Duration</dt>
                      <dd className="text-sm text-gray-900">{Math.floor(submission.file_info.duration_seconds / 60)}:{(submission.file_info.duration_seconds % 60).toString().padStart(2, '0')}</dd>
                    </div>
                  )}
                  {submission.file_info.loudness_lufs && (
                    <div>
                      <dt className="text-sm font-medium text-gray-500">Loudness (LUFS)</dt>
                      <dd className="text-sm text-gray-900">{submission.file_info.loudness_lufs.toFixed(1)}</dd>
                    </div>
                  )}
                  {submission.file_info.true_peak_dbfs && (
                    <div>
                      <dt className="text-sm font-medium text-gray-500">True Peak (dBFS)</dt>
                      <dd className="text-sm text-gray-900">{submission.file_info.true_peak_dbfs.toFixed(1)}</dd>
                    </div>
                  )}
                </dl>
              </div>
            )}

            {/* ISRC Information */}
            {submission.isrc && (
              <div className="bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded-lg shadow-sm p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                    <Award className="h-5 w-5 mr-2 text-green-600" />
                    ISRC Code Assigned
                  </h2>
                  <Link 
                    href="/isrc-info" 
                    className="text-sm text-blue-600 hover:text-blue-800 flex items-center"
                  >
                    How to use ISRC
                    <ExternalLink className="h-4 w-4 ml-1" />
                  </Link>
                </div>
                
                <div className="bg-white rounded-lg p-4 border border-green-200">
                  <div className="text-center">
                    <p className="text-sm text-gray-600 mb-2">Your ISRC Code:</p>
                    <div className="bg-gray-900 text-green-400 font-mono text-2xl font-bold p-4 rounded-lg mb-3">
                      {submission.isrc.isrc_code}
                    </div>
                    <p className="text-xs text-gray-500 mb-4">
                      Assigned on {formatDate(submission.isrc.assigned_at)}
                    </p>
                    <div className="bg-yellow-50 border border-yellow-200 rounded p-3 text-sm">
                      <p className="text-yellow-800">
                        <strong>Important:</strong> Embed this code in your audio file metadata before distribution. 
                        <Link href="/isrc-info" className="text-blue-600 hover:underline ml-1">
                          Learn how →
                        </Link>
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Admin Notes */}
            {submission.admin_notes && (
              <div className="card">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Admin Notes</h2>
                <p className="text-sm text-gray-900">{submission.admin_notes}</p>
              </div>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Artist Information */}
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Artist Information</h2>
              <div className="space-y-4">
                <div className="flex items-center space-x-3">
                  <User className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm font-medium text-gray-900">{submission.artist.name}</p>
                    {submission.artist.pronouns && (
                      <p className="text-xs text-gray-500">{submission.artist.pronouns}</p>
                    )}
                  </div>
                </div>
                <div className="flex items-center space-x-3">
                  <Mail className="h-5 w-5 text-gray-400" />
                  <p className="text-sm text-gray-900">{submission.artist.email}</p>
                </div>
                {submission.artist.bio && (
                  <div>
                    <p className="text-sm text-gray-500 mb-2">Bio:</p>
                    <p className="text-sm text-gray-900">{submission.artist.bio}</p>
                  </div>
                )}
                {Object.keys(submission.artist.social_links).length > 0 && (
                  <div>
                    <p className="text-sm text-gray-500 mb-2">Social Links:</p>
                    <div className="space-y-1">
                      {Object.entries(submission.artist.social_links).map(([platform, url]) => (
                        <a
                          key={platform}
                          href={url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-sm text-indigo-600 hover:text-indigo-900 block"
                        >
                          {platform}: {url}
                        </a>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Permission Controls */}
            {submission.status === 'pending' && (
              <div className="card">
                <h2 className="text-lg font-semibold text-gray-900 mb-4">Approval Settings</h2>
                <div className="space-y-4">
                  <div>
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={radioPermission}
                        onChange={(e) => setRadioPermission(e.target.checked)}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm font-medium text-gray-700">
                        Approve for Radio Play
                      </span>
                    </label>
                    <p className="text-xs text-gray-500 ml-6 mt-1">
                      Allow this track to be played on radio broadcasts
                    </p>
                  </div>
                  
                  <div>
                    <label className="flex items-center">
                      <input
                        type="checkbox"
                        checked={publicDisplay}
                        onChange={(e) => setPublicDisplay(e.target.checked)}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                      />
                      <span className="ml-2 text-sm font-medium text-gray-700">
                        Approve for Gallery Display
                      </span>
                    </label>
                    <p className="text-xs text-gray-500 ml-6 mt-1">
                      Allow this track to be displayed in the public gallery
                    </p>
                  </div>
                  
                  <div>
                    <label htmlFor="adminNotes" className="block text-sm font-medium text-gray-700 mb-1">
                      Admin Notes
                    </label>
                    <textarea
                      id="adminNotes"
                      value={adminNotes}
                      onChange={(e) => setAdminNotes(e.target.value)}
                      rows={3}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                      placeholder="Add notes about this approval..."
                    />
                  </div>
                  
                  <div className="flex space-x-3 pt-2">
                    <button
                      onClick={handleApproveWithPermissions}
                      className="flex-1 bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-colors font-medium"
                    >
                      Approve with Settings
                    </button>
                    <button
                      onClick={() => handleStatusChange('rejected')}
                      className="flex-1 bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 transition-colors font-medium"
                    >
                      Reject
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* Actions */}
            <div className="card">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Actions</h2>
              <div className="space-y-3">
                {submission.status === 'approved' && submission.isrc_requested && !submission.isrc && (
                  <button
                    onClick={handleAssignIsrc}
                    className="w-full bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
                  >
                    Assign ISRC
                  </button>
                )}
                <Link
                  href={`/admin/users/${submission.artist.id}`}
                  className="w-full bg-gray-600 text-white px-4 py-2 rounded-md hover:bg-gray-700 transition-colors text-center block"
                >
                  View Artist Profile
                </Link>
                <button
                  onClick={() => setShowDeleteModal(true)}
                  className="w-full bg-red-800 text-white px-4 py-2 rounded-md hover:bg-red-900 transition-colors"
                >
                  Delete Submission
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <div className="flex items-center mb-4">
              <Music className="h-6 w-6 text-red-600 mr-3" />
              <h3 className="text-lg font-semibold text-gray-900">Delete Submission</h3>
            </div>
            
            <div className="mb-4">
              <p className="text-gray-600 mb-2">
                You are about to permanently delete the submission <strong>"{submission?.song_title}"</strong> by {submission?.artist.name}.
              </p>
              <p className="text-red-600 text-sm mb-4">
                This will also delete the audio file from storage.
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
                onClick={handleDeleteSubmission}
                disabled={deleting || !deletePassword.trim()}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {deleting ? 'Deleting...' : 'Delete Submission'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
