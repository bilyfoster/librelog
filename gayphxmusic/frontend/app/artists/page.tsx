'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Users } from 'lucide-react';

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

interface ArtistsResponse {
  artists: Artist[];
  total: number;
}

export default function ArtistsPage() {
  const [artists, setArtists] = useState<Artist[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [showInactive, setShowInactive] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isAdmin, setIsAdmin] = useState(false);
  const router = useRouter();

  useEffect(() => {
    fetchArtists();
  }, [searchTerm, showInactive]);

  const fetchArtists = async () => {
    try {
      setLoading(true);
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token');
      const adminToken = localStorage.getItem('admin_token');
      
      const params = new URLSearchParams();
      if (searchTerm) params.append('search', searchTerm);
      if (showInactive) params.append('include_inactive', 'true');

      const headers: HeadersInit = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // If admin token exists, use admin endpoint to see all artists
      const baseEndpoint = adminToken ? `http://localhost:8000/api/admin/users` : `/api/artists`;
      const endpoint = params ? `${baseEndpoint}?${params}` : baseEndpoint;
      if (adminToken) {
        headers['Authorization'] = `Bearer ${adminToken}`;
      }

      const response = await fetch(endpoint, {
        headers
      });

      if (response.ok) {
        const data = await response.json();
        // Admin endpoint returns array directly, regular endpoint returns {artists: []}
        const artists = adminToken ? data : data.artists;
        setArtists(artists);
        setIsAuthenticated(true);
        setIsAdmin(!!adminToken);
      } else if (response.status === 401) {
        // Don't redirect immediately, just show login prompt
        setIsAuthenticated(false);
        console.log('Authentication required');
      }
    } catch (error) {
      console.error('Error fetching artists:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteArtist = async (artistId: string) => {
    if (!confirm('Are you sure you want to delete this artist? This action cannot be undone.')) {
      return;
    }

    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      const response = await fetch(`/api/artists/${artistId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        await fetchArtists();
      } else {
        const error = await response.json();
        alert(error.detail || 'Failed to delete artist');
      }
    } catch (error) {
      console.error('Error deleting artist:', error);
      alert('Failed to delete artist');
    }
  };

  const handleReactivateArtist = async (artistId: string) => {
    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      const response = await fetch(`/api/artists/${artistId}/reactivate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        await fetchArtists();
      } else {
        const error = await response.json();
        alert(error.detail || 'Failed to reactivate artist');
      }
    } catch (error) {
      console.error('Error reactivating artist:', error);
      alert('Failed to reactivate artist');
    }
  };

  const filteredArtists = artists.filter(artist => 
    showInactive || artist.is_active
  );

  if (!isAuthenticated && !loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
        {/* Diagonal Stripes Pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute inset-0" style={{
            backgroundImage: `repeating-linear-gradient(45deg, white 0px, white 2px, transparent 2px, transparent 10px)`,
            backgroundSize: '20px 20px'
          }}></div>
        </div>
        {/* Header */}
        <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex justify-between items-center py-4">
              <div className="flex items-center space-x-2">
                <Users className="h-8 w-8 text-white" />
                <h1 className="text-2xl font-bold text-white">Artist Management</h1>
              </div>
              <nav className="hidden md:flex space-x-8">
                <Link href="/" className="text-white hover:text-gray-200 transition-colors">
                  Landing
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
              Artist Management
            </h1>
            <p className="text-xl text-white/90 mb-8 max-w-2xl mx-auto">
              Please sign in to manage your artist profiles and music submissions.
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
      {/* Diagonal Stripes Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `repeating-linear-gradient(45deg, white 0px, white 2px, transparent 2px, transparent 10px)`,
          backgroundSize: '20px 20px'
        }}></div>
      </div>
      {/* Header */}
      <header className="bg-white/10 backdrop-blur-md border-b border-white/20 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <Users className="h-8 w-8 text-white" />
              <h1 className="text-2xl font-bold text-white">Artist Management</h1>
            </div>
            <nav className="hidden md:flex space-x-8">
              <Link href="/dashboard" className="text-white hover:text-gray-200 transition-colors">
                Dashboard
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
              <h1 className="text-3xl font-bold text-white">
                {isAdmin ? 'All Artists' : 'Your Artists'}
              </h1>
              <p className="mt-2 text-white/80">
                {isAdmin 
                  ? 'Manage all artists and their information across the platform'
                  : 'Manage your artists and their information for music submissions'
                }
              </p>
            </div>
            <Link
              href="/artists/new"
              className="bg-white text-cyan-600 px-6 py-3 rounded-lg hover:bg-gray-100 transition-colors font-semibold"
            >
              Add New Artist
            </Link>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <input
                type="text"
                placeholder="Search artists..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="flex items-center">
              <label className="flex items-center">
                <input
                  type="checkbox"
                  checked={showInactive}
                  onChange={(e) => setShowInactive(e.target.checked)}
                  className="mr-2"
                />
                Show inactive artists
              </label>
            </div>
          </div>
        </div>

        {/* Artists List */}
        <div className="bg-white rounded-lg shadow-sm">
          {loading ? (
            <div className="p-8 text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
              <p className="mt-2 text-gray-600">Loading artists...</p>
            </div>
          ) : filteredArtists.length === 0 ? (
            <div className="p-8 text-center">
              <div className="text-gray-400 text-6xl mb-4">🎵</div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">No artists found</h3>
              <p className="text-gray-600 mb-4">
                {searchTerm ? 'Try adjusting your search terms' : 'Get started by adding your first artist'}
              </p>
              <Link
                href="/artists/new"
                className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Add New Artist
              </Link>
            </div>
          ) : (
            <div className="divide-y divide-gray-200">
              {filteredArtists.map((artist) => (
                <div key={artist.id} className="p-6 hover:bg-gray-50">
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-center">
                        <h3 className="text-lg font-medium text-gray-900">
                          {artist.name}
                        </h3>
                        {artist.pronouns && (
                          <span className="ml-2 text-sm text-gray-500">
                            ({artist.pronouns})
                          </span>
                        )}
                        {!artist.is_active && (
                          <span className="ml-2 inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            Inactive
                          </span>
                        )}
                      </div>
                      
                      {artist.bio && (
                        <p className="mt-1 text-gray-600 line-clamp-2">
                          {artist.bio}
                        </p>
                      )}
                      
                      <div className="mt-2 flex items-center text-sm text-gray-500">
                        <span>{artist.submission_count} submissions</span>
                        <span className="mx-2">•</span>
                        <span>Created {new Date(artist.created_at).toLocaleDateString()}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <Link
                        href={`/artists/${artist.id}/edit`}
                        className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                      >
                        Edit
                      </Link>
                      
                      {artist.is_active ? (
                        <button
                          onClick={() => handleDeleteArtist(artist.id)}
                          className="text-red-600 hover:text-red-800 text-sm font-medium"
                        >
                          Delete
                        </button>
                      ) : (
                        <button
                          onClick={() => handleReactivateArtist(artist.id)}
                          className="text-green-600 hover:text-green-800 text-sm font-medium"
                        >
                          Reactivate
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
