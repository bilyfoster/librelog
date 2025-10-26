'use client'

import { useState, useEffect } from 'react'
import { useParams } from 'next/navigation'
// import { submissionsApi, SubmissionResponse } from '@/lib/api'

interface SubmissionResponse {
  id: string
  title: string
  song_title: string
  artist_name: string
  artist: {
    name: string
    email: string
    pronouns?: string
  }
  status: string
  admin_notes?: string
  isrc_code?: string
  tracking_id: string
  genre?: string
  isrc_requested: boolean
  radio_permission: boolean
  public_display: boolean
  created_at: string
  play_stats?: {
    total_plays: number
    radio_plays: number
    gallery_plays: number
    other_plays: number
    last_played_at?: string
  }
}
import { Music, Clock, CheckCircle, XCircle, AlertCircle, Award } from 'lucide-react'
import Link from 'next/link'

export default function TrackPage() {
  const params = useParams()
  const trackingId = params.id as string
  const [submission, setSubmission] = useState<SubmissionResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (trackingId) {
      fetchSubmission()
    }
  }, [trackingId])

  const fetchSubmission = async () => {
    try {
      const response = await fetch(`http://localhost:8000/api/submissions/track/${trackingId}`)
      const data = await response.json()
      setSubmission(data)
      
      // Fetch play statistics
      if (data.id) {
        try {
          const playStatsResponse = await fetch(`http://localhost:8000/api/plays/statistics/${data.id}`)
          if (playStatsResponse.ok) {
            const playStats = await playStatsResponse.json()
            setSubmission(prev => prev ? { ...prev, play_stats: playStats } : null)
          }
        } catch (playErr) {
          console.error('Error fetching play statistics:', playErr)
        }
      }
    } catch (err) {
      setError('Submission not found')
    } finally {
      setLoading(false)
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'pending':
        return <Clock className="h-5 w-5 text-yellow-500" />
      case 'approved':
        return <CheckCircle className="h-5 w-5 text-green-500" />
      case 'rejected':
        return <XCircle className="h-5 w-5 text-red-500" />
      case 'needs_info':
        return <AlertCircle className="h-5 w-5 text-orange-500" />
      case 'isrc_assigned':
        return <Award className="h-5 w-5 text-blue-500" />
      default:
        return <Clock className="h-5 w-5 text-gray-500" />
    }
  }

  const getStatusText = (status: string) => {
    switch (status) {
      case 'pending':
        return 'Under Review'
      case 'approved':
        return 'Approved'
      case 'rejected':
        return 'Rejected'
      case 'needs_info':
        return 'Needs More Information'
      case 'isrc_assigned':
        return 'ISRC Assigned'
      default:
        return status
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

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    )
  }

  if (error || !submission) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gayphx-purple to-gayphx-pink flex items-center justify-center">
        <div className="card max-w-md w-full text-center">
          <XCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
          <h1 className="text-2xl font-bold mb-4">Submission Not Found</h1>
          <p className="text-gray-600 mb-6">
            We couldn't find a submission with that tracking ID.
          </p>
          <Link href="/dashboard" className="btn-primary">
            Back to Dashboard
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-teal-500 to-cyan-600 relative overflow-hidden py-8">
      {/* Crosshatch Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(90deg, white 1px, transparent 1px), linear-gradient(white 1px, transparent 1px)`,
          backgroundSize: '15px 15px'
        }}></div>
      </div>
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-white mb-4">Track Your Submission</h1>
          <p className="text-white/90 text-lg">
            Monitor the status of your music submission
          </p>
        </div>

        <div className="card">
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center">
              <Music className="h-8 w-8 text-gayphx-purple mr-3" />
              <div>
                <h2 className="text-2xl font-bold">{submission.song_title}</h2>
                <p className="text-gray-600">by {submission.artist.name}</p>
              </div>
            </div>
            <div className={`px-4 py-2 rounded-full flex items-center ${getStatusColor(submission.status)}`}>
              {getStatusIcon(submission.status)}
              <span className="ml-2 font-medium">{getStatusText(submission.status)}</span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <h3 className="text-lg font-semibold mb-4">Submission Details</h3>
              <div className="space-y-3">
                <div>
                  <span className="text-sm font-medium text-gray-600">Tracking ID:</span>
                  <p className="font-mono text-lg">{submission.tracking_id}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-600">Submitted:</span>
                   <p>{new Date(submission.created_at).toLocaleDateString()}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-600">Genre:</span>
                  <p>{submission.genre || 'Not specified'}</p>
                </div>
                <div>
                  <span className="text-sm font-medium text-gray-600">Artist Email:</span>
                  <p>{submission.artist.email}</p>
                </div>
                {submission.artist.pronouns && (
                  <div>
                    <span className="text-sm font-medium text-gray-600">Pronouns:</span>
                    <p>{submission.artist.pronouns}</p>
                  </div>
                )}
              </div>
            </div>

            <div>
              <h3 className="text-lg font-semibold mb-4">Permissions & Requests</h3>
              <div className="space-y-3">
                <div className="flex items-center">
                  <span className="text-sm font-medium text-gray-600 mr-2">ISRC Requested:</span>
                  {submission.isrc_requested ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : (
                    <XCircle className="h-4 w-4 text-gray-400" />
                  )}
                </div>
                <div className="flex items-center">
                  <span className="text-sm font-medium text-gray-600 mr-2">Radio Permission:</span>
                  {submission.radio_permission ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : (
                    <XCircle className="h-4 w-4 text-gray-400" />
                  )}
                </div>
                <div className="flex items-center">
                  <span className="text-sm font-medium text-gray-600 mr-2">Public Display:</span>
                  {submission.public_display ? (
                    <CheckCircle className="h-4 w-4 text-green-500" />
                  ) : (
                    <XCircle className="h-4 w-4 text-gray-400" />
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Play Statistics */}
          {submission.play_stats && (
            <div className="mt-8">
              <h3 className="text-lg font-semibold mb-4">Play Statistics</h3>
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
                <div className="mt-4 text-sm text-gray-600 text-center">
                  Last played: {new Date(submission.play_stats.last_played_at).toLocaleString()}
                </div>
              )}
            </div>
          )}

          {submission.status === 'isrc_assigned' && (
            <div className="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
              <h3 className="text-lg font-semibold text-blue-900 mb-2">ISRC Code Assigned!</h3>
              <p className="text-blue-800">
                Your track has been approved and an ISRC code has been assigned. 
                Check your email for the official certificate and details.
              </p>
            </div>
          )}

          {submission.status === 'rejected' && (
            <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <h3 className="text-lg font-semibold text-red-900 mb-2">Submission Rejected</h3>
              <p className="text-red-800">
                Unfortunately, your submission was not approved. If you have questions, 
                please contact us at music@gayphx.com.
              </p>
            </div>
          )}

          {submission.status === 'needs_info' && (
            <div className="mt-6 p-4 bg-orange-50 border border-orange-200 rounded-lg">
              <h3 className="text-lg font-semibold text-orange-900 mb-2">More Information Needed</h3>
              <p className="text-orange-800">
                We need additional information to process your submission. 
                Please check your email for details on what's needed.
              </p>
            </div>
          )}

          <div className="mt-8 flex flex-col sm:flex-row gap-4">
            <Link href="/dashboard" className="btn-primary text-center">
              Back to Dashboard
            </Link>
            <Link href="/submit" className="btn-secondary text-center">
              Submit Another Track
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
