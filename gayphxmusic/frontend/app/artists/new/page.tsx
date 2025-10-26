'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Users, Plus } from 'lucide-react';

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

export default function NewArtistPage() {
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
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const router = useRouter();

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

      const response = await fetch('/api/artists', {
        method: 'POST',
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
        const artist = await response.json();
        router.push(`/artists/${artist.id}`);
      } else {
        const error = await response.json();
        if (error.detail) {
          setErrors({ name: error.detail });
        } else {
          setErrors({ general: 'Failed to create artist' });
        }
      }
    } catch (error) {
      console.error('Error creating artist:', error);
      setErrors({ general: 'Failed to create artist' });
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

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        {/* Page Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <Link
                href="/artists"
                className="text-white/80 hover:text-white mr-4 flex items-center"
              >
                ← Back to Artists
              </Link>
              <div>
                <h1 className="text-3xl font-bold text-white">Add New Artist</h1>
                <p className="mt-2 text-white/80">
                  Create a new artist profile for your music submissions
                </p>
              </div>
            </div>
            <div className="flex items-center space-x-2 text-white/60">
              <Plus className="h-5 w-5" />
              <span className="text-sm">New Artist</span>
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
                  className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
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
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-cyan-500"
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
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-cyan-500"
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
                      className={`mt-1 block w-full px-3 py-2 border rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-cyan-500 ${
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
                href="/artists"
                className="px-6 py-3 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-cyan-500 transition-colors font-semibold"
              >
                Cancel
              </Link>
              <button
                type="submit"
                disabled={saving}
                className="px-6 py-3 bg-cyan-600 text-white rounded-lg hover:bg-cyan-700 focus:outline-none focus:ring-2 focus:ring-cyan-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-semibold"
              >
                {saving ? 'Creating...' : 'Create Artist'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
