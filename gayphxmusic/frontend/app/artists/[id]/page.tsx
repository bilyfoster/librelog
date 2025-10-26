'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import Link from 'next/link';

interface Artist {
  id: string;
  name: string;
  pronouns?: string;
  bio?: string;
  social_links: Record<string, string>;
  is_active: boolean;
  created_at: string;
  updated_at: string;
  submission_count: number;
}

export default function ArtistDetailPage() {
  const [artist, setArtist] = useState<Artist | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const router = useRouter();
  const params = useParams();
  const artistId = params.id as string;

  useEffect(() => {
    if (artistId) {
      fetchArtist();
    }
  }, [artistId]);

  const fetchArtist = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      if (!token) {
        router.push('/auth/login');
        return;
      }

      const response = await fetch(`/api/artists/${artistId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const artistData: Artist = await response.json();
        setArtist(artistData);
      } else if (response.status === 401) {
        localStorage.removeItem('token');
        router.push('/auth/login');
      } else if (response.status === 404) {
        router.push('/artists');
      }
    } catch (error) {
      console.error('Error fetching artist:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this artist? This action cannot be undone.')) {
      return;
    }

    setDeleting(true);

    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      const response = await fetch(`/api/artists/${artistId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        router.push('/artists');
      } else {
        const error = await response.json();
        alert(error.detail || 'Failed to delete artist');
      }
    } catch (error) {
      console.error('Error deleting artist:', error);
      alert('Failed to delete artist');
    } finally {
      setDeleting(false);
    }
  };

  const handleReactivate = async () => {
    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      const response = await fetch(`/api/artists/${artistId}/reactivate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        await fetchArtist();
      } else {
        const error = await response.json();
        alert(error.detail || 'Failed to reactivate artist');
      }
    } catch (error) {
      console.error('Error reactivating artist:', error);
      alert('Failed to reactivate artist');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-white rounded-lg shadow-sm p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-2 text-gray-600">Loading artist...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!artist) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-white rounded-lg shadow-sm p-8 text-center">
            <h3 className="text-lg font-medium text-gray-900 mb-2">Artist not found</h3>
            <p className="text-gray-600 mb-4">The artist you're looking for doesn't exist.</p>
            <Link
              href="/artists"
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              Back to Artists
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const socialLinks = Object.entries(artist.social_links).filter(([_, url]) => url);

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Link
                href="/artists"
                className="text-gray-400 hover:text-gray-600 mr-4"
              >
                ← Back to Artists
              </Link>
              <div>
                <h1 className="text-3xl font-bold text-gray-900">{artist.name}</h1>
                {artist.pronouns && (
                  <p className="text-lg text-gray-600">({artist.pronouns})</p>
                )}
              </div>
            </div>
            <div className="flex items-center space-x-3">
              {!artist.is_active && (
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-800">
                  Inactive
                </span>
              )}
              <Link
                href={`/artists/${artist.id}/edit`}
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Edit Artist
              </Link>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Bio */}
            {artist.bio && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-lg font-medium text-gray-900 mb-4">About</h2>
                <p className="text-gray-700 whitespace-pre-wrap">{artist.bio}</p>
              </div>
            )}

            {/* Social Links */}
            {socialLinks.length > 0 && (
              <div className="bg-white rounded-lg shadow-sm p-6">
                <h2 className="text-lg font-medium text-gray-900 mb-4">Social Links</h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {socialLinks.map(([platform, url]) => (
                    <a
                      key={platform}
                      href={url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex items-center p-3 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors"
                    >
                      <div className="flex-1">
                        <p className="font-medium text-gray-900 capitalize">{platform}</p>
                        <p className="text-sm text-gray-500 truncate">{url}</p>
                      </div>
                      <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                      </svg>
                    </a>
                  ))}
                </div>
              </div>
            )}

            {/* Submissions */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h2 className="text-lg font-medium text-gray-900 mb-4">Submissions</h2>
              {artist.submission_count > 0 ? (
                <div className="text-center py-8">
                  <div className="text-4xl font-bold text-blue-600 mb-2">{artist.submission_count}</div>
                  <p className="text-gray-600">Total submissions</p>
                  <Link
                    href={`/submissions?artist=${artist.id}`}
                    className="mt-4 inline-block bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    View Submissions
                  </Link>
                </div>
              ) : (
                <div className="text-center py-8">
                  <div className="text-gray-400 text-4xl mb-4">🎵</div>
                  <p className="text-gray-600 mb-4">No submissions yet</p>
                  <Link
                    href="/submit"
                    className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Submit Music
                  </Link>
                </div>
              )}
            </div>
          </div>

          {/* Sidebar */}
          <div className="space-y-6">
            {/* Quick Stats */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Quick Stats</h3>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">Submissions</span>
                  <span className="font-medium">{artist.submission_count}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Status</span>
                  <span className={`font-medium ${artist.is_active ? 'text-green-600' : 'text-red-600'}`}>
                    {artist.is_active ? 'Active' : 'Inactive'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Created</span>
                  <span className="font-medium">
                    {new Date(artist.created_at).toLocaleDateString()}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Updated</span>
                  <span className="font-medium">
                    {new Date(artist.updated_at).toLocaleDateString()}
                  </span>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="bg-white rounded-lg shadow-sm p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Actions</h3>
              <div className="space-y-3">
                <Link
                  href={`/artists/${artist.id}/edit`}
                  className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors text-center block"
                >
                  Edit Artist
                </Link>
                
                {artist.is_active ? (
                  <button
                    onClick={handleDelete}
                    disabled={deleting}
                    className="w-full bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {deleting ? 'Deleting...' : 'Delete Artist'}
                  </button>
                ) : (
                  <button
                    onClick={handleReactivate}
                    className="w-full bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
                  >
                    Reactivate Artist
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
