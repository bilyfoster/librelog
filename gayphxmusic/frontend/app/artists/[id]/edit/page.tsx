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

interface SocialLinks {
  website?: string;
  instagram?: string;
  twitter?: string;
  facebook?: string;
  youtube?: string;
  spotify?: string;
  soundcloud?: string;
  bandcamp?: string;
  [key: string]: string | undefined;
}

export default function EditArtistPage() {
  const [artist, setArtist] = useState<Artist | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    pronouns: '',
    bio: '',
    social_links: {
      website: '',
      instagram: '',
      twitter: '',
      facebook: '',
      youtube: '',
      spotify: '',
      soundcloud: '',
      bandcamp: ''
    } as SocialLinks
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
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
        setFormData({
          name: artistData.name,
          pronouns: artistData.pronouns || '',
          bio: artistData.bio || '',
          social_links: {
            website: artistData.social_links.website || '',
            instagram: artistData.social_links.instagram || '',
            twitter: artistData.social_links.twitter || '',
            facebook: artistData.social_links.facebook || '',
            youtube: artistData.social_links.youtube || '',
            spotify: artistData.social_links.spotify || '',
            soundcloud: artistData.social_links.soundcloud || '',
            bandcamp: artistData.social_links.bandcamp || ''
          }
        });
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

  const handleInputChange = (field: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
    
    // Clear error when user starts typing
    if (errors[field]) {
      setErrors(prev => ({
        ...prev,
        [field]: ''
      }));
    }
  };

  const handleSocialLinkChange = (platform: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      social_links: {
        ...prev.social_links,
        [platform]: value
      }
    }));
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Artist name is required';
    }

    // Validate URLs
    Object.entries(formData.social_links).forEach(([platform, url]) => {
      if (url && !isValidUrl(url)) {
        newErrors[`social_${platform}`] = 'Please enter a valid URL';
      }
    });

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const isValidUrl = (url: string) => {
    try {
      new URL(url);
      return true;
    } catch {
      return false;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setSaving(true);

    try {
      const token = localStorage.getItem('auth_token') || localStorage.getItem('token') || localStorage.getItem('artist_token');
      if (!token) {
        router.push('/auth/login');
        return;
      }

      // Clean up social links - remove empty values
      const cleanedSocialLinks = Object.fromEntries(
        Object.entries(formData.social_links).filter(([_, value]) => value && value.trim())
      );

      const response = await fetch(`/api/artists/${artistId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          ...formData,
          social_links: cleanedSocialLinks
        })
      });

      if (response.ok) {
        router.push(`/artists/${artistId}`);
      } else {
        const error = await response.json();
        if (error.detail) {
          setErrors({ name: error.detail });
        } else {
          setErrors({ general: 'Failed to update artist' });
        }
      }
    } catch (error) {
      console.error('Error updating artist:', error);
      setErrors({ general: 'Failed to update artist' });
    } finally {
      setSaving(false);
    }
  };

  const socialPlatforms = [
    { key: 'website', label: 'Website', placeholder: 'https://example.com' },
    { key: 'instagram', label: 'Instagram', placeholder: 'https://instagram.com/username' },
    { key: 'twitter', label: 'Twitter', placeholder: 'https://twitter.com/username' },
    { key: 'facebook', label: 'Facebook', placeholder: 'https://facebook.com/username' },
    { key: 'youtube', label: 'YouTube', placeholder: 'https://youtube.com/channel/...' },
    { key: 'spotify', label: 'Spotify', placeholder: 'https://open.spotify.com/artist/...' },
    { key: 'soundcloud', label: 'SoundCloud', placeholder: 'https://soundcloud.com/username' },
    { key: 'bandcamp', label: 'Bandcamp', placeholder: 'https://username.bandcamp.com' }
  ];

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
        <div 
          className="absolute inset-0 opacity-10"
          style={{
            backgroundImage: `linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%),
                             linear-gradient(-45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%)`,
            backgroundSize: '20px 20px',
            backgroundPosition: '0 0, 10px 10px'
          }}
        />
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
          <div className="bg-white rounded-lg shadow-sm p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-cyan-600 mx-auto"></div>
            <p className="mt-2 text-gray-600">Loading artist...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!artist) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
        <div 
          className="absolute inset-0 opacity-10"
          style={{
            backgroundImage: `linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%),
                             linear-gradient(-45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%)`,
            backgroundSize: '20px 20px',
            backgroundPosition: '0 0, 10px 10px'
          }}
        />
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
          <div className="bg-white rounded-lg shadow-sm p-8 text-center">
            <h3 className="text-lg font-medium text-gray-900 mb-2">Artist not found</h3>
            <p className="text-gray-600 mb-4">The artist you're looking for doesn't exist.</p>
            <Link
              href="/artists"
              className="bg-cyan-600 text-white px-4 py-2 rounded-lg hover:bg-cyan-700 transition-colors"
            >
              Back to Artists
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-cyan-500 to-teal-700 relative overflow-hidden">
      {/* Background Pattern */}
      <div 
        className="absolute inset-0 opacity-10"
        style={{
          backgroundImage: `linear-gradient(45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%),
                           linear-gradient(-45deg, transparent 40%, rgba(255,255,255,0.1) 50%, transparent 60%)`,
          backgroundSize: '20px 20px',
          backgroundPosition: '0 0, 10px 10px'
        }}
      />
      
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center">
            <Link
              href="/artists"
              className="text-white hover:text-gray-200 mr-4"
            >
              ← Back to Artists
            </Link>
            <div>
              <h1 className="text-3xl font-bold text-white">Edit Artist</h1>
              <p className="mt-2 text-white/80">
                Update {artist.name}'s information
              </p>
            </div>
          </div>
        </div>

        {/* Form */}
        <div className="bg-white rounded-lg shadow-sm">
          <form onSubmit={handleSubmit} className="p-6 space-y-6">
            {/* General Error */}
            {errors.general && (
              <div className="bg-red-50 border border-red-200 rounded-md p-4">
                <p className="text-red-800">{errors.general}</p>
              </div>
            )}

            {/* Basic Information */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium text-gray-900">Basic Information</h3>
              
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                  Artist Name *
                </label>
                <input
                  type="text"
                  id="name"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    errors.name ? 'border-red-300' : 'border-gray-300'
                  }`}
                  placeholder="Enter artist name"
                />
                {errors.name && (
                  <p className="mt-1 text-sm text-red-600">{errors.name}</p>
                )}
              </div>

              <div>
                <label htmlFor="pronouns" className="block text-sm font-medium text-gray-700">
                  Pronouns
                </label>
                <input
                  type="text"
                  id="pronouns"
                  value={formData.pronouns}
                  onChange={(e) => handleInputChange('pronouns', e.target.value)}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., they/them, she/her, he/him"
                />
              </div>

              <div>
                <label htmlFor="bio" className="block text-sm font-medium text-gray-700">
                  Bio
                </label>
                <textarea
                  id="bio"
                  rows={4}
                  value={formData.bio}
                  onChange={(e) => handleInputChange('bio', e.target.value)}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Tell us about this artist..."
                />
              </div>
            </div>

            {/* Social Links */}
            <div className="space-y-4">
              <h3 className="text-lg font-medium text-gray-900">Social Links</h3>
              <p className="text-sm text-gray-600">
                Add social media and music platform links for this artist
              </p>
              
              <div className="grid grid-cols-1 gap-4">
                {socialPlatforms.map((platform) => (
                  <div key={platform.key}>
                    <label htmlFor={platform.key} className="block text-sm font-medium text-gray-700">
                      {platform.label}
                    </label>
                    <input
                      type="url"
                      id={platform.key}
                      value={formData.social_links[platform.key] || ''}
                      onChange={(e) => handleSocialLinkChange(platform.key, e.target.value)}
                      className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                        errors[`social_${platform.key}`] ? 'border-red-300' : 'border-gray-300'
                      }`}
                      placeholder={platform.placeholder}
                    />
                    {errors[`social_${platform.key}`] && (
                      <p className="mt-1 text-sm text-red-600">{errors[`social_${platform.key}`]}</p>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Submit Buttons */}
            <div className="flex justify-end space-x-3 pt-6 border-t border-gray-200">
              <Link
                href={`/artists/${artistId}`}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                Cancel
              </Link>
              <button
                type="submit"
                disabled={saving}
                className="px-4 py-2 bg-cyan-600 text-white rounded-md hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-cyan-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? 'Saving...' : 'Save Changes'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
