'use client';

import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { useDropzone } from 'react-dropzone';
import { useRouter } from 'next/navigation';
import { Upload, Music, User, Mail, FileAudio, CheckCircle, Loader2, ArrowLeft, Plus } from 'lucide-react';
import toast from 'react-hot-toast';
import Link from 'next/link';

interface Artist {
  id: string;
  name: string;
  pronouns?: string;
  bio?: string;
  social_links: Record<string, string>;
}

interface FormData {
  artist_id: string;
  song_title: string;
  genre: string;
  isrc_requested: boolean;
  radio_permission: boolean;
  public_display: boolean;
  podcast_permission: boolean;
  commercial_use: boolean;
  rights_attestation: boolean;
  pro_info: {
    pro_affiliation?: string;
    writer_splits?: string;
    publisher?: string;
  };
}

export default function SubmitNewPage() {
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [submissionComplete, setSubmissionComplete] = useState(false);
  const [trackingId, setTrackingId] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [availableArtists, setAvailableArtists] = useState<Artist[]>([]);
  const [selectedArtist, setSelectedArtist] = useState<Artist | null>(null);
  const [loadingArtists, setLoadingArtists] = useState(true);
  const router = useRouter();

  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<FormData>({
    defaultValues: {
      pro_info: {}
    }
  });

  const watchedArtistId = watch('artist_id');

  const onDrop = (acceptedFiles: File[]) => {
    const file = acceptedFiles[0];
    if (file) {
      // Validate file size (150MB)
      if (file.size > 150 * 1024 * 1024) {
        toast.error('File size must be less than 150MB');
        return;
      }
      
      // Validate file type - MP3 only
      if (file.type !== 'audio/mpeg' && !file.name.toLowerCase().endsWith('.mp3')) {
        toast.error('Only MP3 files are accepted. Please convert your audio file to MP3 format.');
        return;
      }
      
      setUploadedFile(file);
      toast.success('File selected successfully');
    }
  };

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'audio/mpeg': ['.mp3']
    },
    multiple: false
  });

  // Check authentication and fetch artists on mount
  useEffect(() => {
    if (typeof window === 'undefined') return;
    
    const token = localStorage.getItem('token');
    if (!token) {
      router.push('/auth/login');
      return;
    }

    setIsAuthenticated(true);
    fetchArtists();
  }, []);

  // Update selected artist when artist_id changes
  useEffect(() => {
    if (watchedArtistId && availableArtists.length > 0) {
      const artist = availableArtists.find(a => a.id === watchedArtistId);
      setSelectedArtist(artist || null);
    } else {
      setSelectedArtist(null);
    }
  }, [watchedArtistId, availableArtists]);

  const fetchArtists = async () => {
    try {
      setLoadingArtists(true);
      const token = localStorage.getItem('token');
      const response = await fetch('/api/artists/dropdown/list', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const artists = await response.json();
        setAvailableArtists(artists);
      } else if (response.status === 401) {
        localStorage.removeItem('token');
        router.push('/auth/login');
      }
    } catch (error) {
      console.error('Error fetching artists:', error);
      toast.error('Failed to load artists');
    } finally {
      setLoadingArtists(false);
    }
  };

  const onSubmit = async (data: FormData) => {
    if (!uploadedFile) {
      toast.error('Please select a file to upload');
      return;
    }

    if (!data.artist_id) {
      toast.error('Please select an artist');
      return;
    }

    setIsUploading(true);
    setUploadProgress(0);

    try {
      const token = localStorage.getItem('token');
      
      // Create FormData for file upload
      const formData = new FormData();
      formData.append('file', uploadedFile);
      formData.append('artist_id', data.artist_id);
      formData.append('song_title', data.song_title);
      formData.append('genre', data.genre);
      formData.append('isrc_requested', data.isrc_requested.toString());
      formData.append('radio_permission', data.radio_permission.toString());
      formData.append('public_display', data.public_display.toString());
      formData.append('podcast_permission', data.podcast_permission.toString());
      formData.append('commercial_use', data.commercial_use.toString());
      formData.append('rights_attestation', data.rights_attestation.toString());
      
      if (data.pro_info.pro_affiliation) {
        formData.append('pro_info[pro_affiliation]', data.pro_info.pro_affiliation);
      }
      if (data.pro_info.writer_splits) {
        formData.append('pro_info[writer_splits]', data.pro_info.writer_splits);
      }
      if (data.pro_info.publisher) {
        formData.append('pro_info[publisher]', data.pro_info.publisher);
      }

      const response = await fetch('/api/submissions', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (response.ok) {
        const result = await response.json();
        setTrackingId(result.tracking_id);
        setSubmissionComplete(true);
        toast.success('Submission created successfully!');
      } else {
        const error = await response.json();
        toast.error(error.detail || 'Failed to create submission');
      }
    } catch (error) {
      console.error('Error creating submission:', error);
      toast.error('Failed to create submission');
    } finally {
      setIsUploading(false);
    }
  };

  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (submissionComplete) {
    return (
      <div className="min-h-screen bg-gray-50 py-8">
        <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-white rounded-lg shadow-sm p-8 text-center">
            <CheckCircle className="mx-auto h-16 w-16 text-green-500 mb-4" />
            <h1 className="text-2xl font-bold text-gray-900 mb-2">Submission Complete!</h1>
            <p className="text-gray-600 mb-4">
              Your music has been submitted successfully.
            </p>
            <div className="bg-gray-50 rounded-lg p-4 mb-6">
              <p className="text-sm text-gray-600 mb-1">Tracking ID</p>
              <p className="font-mono text-lg font-bold text-gray-900">{trackingId}</p>
            </div>
            <div className="space-y-3">
              <Link
                href="/dashboard"
                className="w-full bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors block"
              >
                Go to Dashboard
              </Link>
              <button
                onClick={() => {
                  setSubmissionComplete(false);
                  setTrackingId('');
                  setUploadedFile(null);
                  setValue('artist_id', '');
                  setValue('song_title', '');
                  setValue('genre', '');
                }}
                className="w-full bg-gray-200 text-gray-800 px-4 py-2 rounded-lg hover:bg-gray-300 transition-colors"
              >
                Submit Another Track
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-500 to-emerald-600 relative overflow-hidden py-8">
      {/* Squares Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(45deg, white 25%, transparent 25%), linear-gradient(-45deg, white 25%, transparent 25%), linear-gradient(45deg, transparent 75%, white 75%), linear-gradient(-45deg, transparent 75%, white 75%)`,
          backgroundSize: '20px 20px',
          backgroundPosition: '0 0, 0 10px, 10px -10px, -10px 0px'
        }}></div>
      </div>
      <div className="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center">
            <Link
              href="/dashboard"
              className="text-gray-400 hover:text-gray-600 mr-4"
            >
              <ArrowLeft className="h-5 w-5" />
            </Link>
            <div>
              <h1 className="text-3xl font-bold text-white">Submit Music</h1>
              <p className="mt-2 text-white/80">
                Upload your music and submit it for review
              </p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
          {/* Artist Selection */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">Artist Information</h2>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="artist_id" className="block text-sm font-medium text-gray-700">
                  Select Artist *
                </label>
                {loadingArtists ? (
                  <div className="mt-1 flex items-center">
                    <Loader2 className="animate-spin h-4 w-4 mr-2" />
                    <span className="text-gray-500">Loading artists...</span>
                  </div>
                ) : availableArtists.length === 0 ? (
                  <div className="mt-1 p-4 border border-gray-200 rounded-lg text-center">
                    <p className="text-gray-600 mb-3">No artists found. Create one first.</p>
                    <Link
                      href="/artists/new"
                      className="inline-flex items-center bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Add New Artist
                    </Link>
                  </div>
                ) : (
                  <select
                    id="artist_id"
                    {...register('artist_id', { required: 'Please select an artist' })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="">Select an artist...</option>
                    {availableArtists.map((artist) => (
                      <option key={artist.id} value={artist.id}>
                        {artist.name} {artist.pronouns && `(${artist.pronouns})`}
                      </option>
                    ))}
                  </select>
                )}
                {errors.artist_id && (
                  <p className="mt-1 text-sm text-red-600">{errors.artist_id.message}</p>
                )}
              </div>

              {/* Selected Artist Info */}
              {selectedArtist && (
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="font-medium text-gray-900 mb-2">Selected Artist</h3>
                  <div className="space-y-1 text-sm text-gray-600">
                    <p><span className="font-medium">Name:</span> {selectedArtist.name}</p>
                    {selectedArtist.pronouns && (
                      <p><span className="font-medium">Pronouns:</span> {selectedArtist.pronouns}</p>
                    )}
                    {selectedArtist.bio && (
                      <p><span className="font-medium">Bio:</span> {selectedArtist.bio}</p>
                    )}
                  </div>
                  <div className="mt-3">
                    <Link
                      href={`/artists/${selectedArtist.id}/edit`}
                      className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                    >
                      Edit Artist Information →
                    </Link>
                  </div>
                </div>
              )}

              <div className="text-center">
                <Link
                  href="/artists/new"
                  className="inline-flex items-center text-blue-600 hover:text-blue-800 text-sm font-medium"
                >
                  <Plus className="h-4 w-4 mr-1" />
                  Add New Artist
                </Link>
              </div>
            </div>
          </div>

          {/* Track Information */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">Track Information</h2>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="song_title" className="block text-sm font-medium text-gray-700">
                  Song Title *
                </label>
                <input
                  type="text"
                  id="song_title"
                  {...register('song_title', { required: 'Song title is required' })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Enter song title"
                />
                {errors.song_title && (
                  <p className="mt-1 text-sm text-red-600">{errors.song_title.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="genre" className="block text-sm font-medium text-gray-700">
                  Genre
                </label>
                <input
                  type="text"
                  id="genre"
                  {...register('genre')}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., Pop, Rock, Electronic"
                />
              </div>
            </div>
          </div>

          {/* File Upload */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">Audio File</h2>
            
            <div
              {...getRootProps()}
              className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
                isDragActive
                  ? 'border-blue-400 bg-blue-50'
                  : 'border-gray-300 hover:border-gray-400'
              }`}
            >
              <input {...getInputProps()} />
              {uploadedFile ? (
                <div className="space-y-2">
                  <FileAudio className="mx-auto h-12 w-12 text-green-500" />
                  <p className="text-sm font-medium text-gray-900">{uploadedFile.name}</p>
                  <p className="text-xs text-gray-500">
                    {(uploadedFile.size / (1024 * 1024)).toFixed(2)} MB
                  </p>
                </div>
              ) : (
                <div className="space-y-2">
                  <Upload className="mx-auto h-12 w-12 text-gray-400" />
                  <p className="text-sm font-medium text-gray-900">
                    {isDragActive ? 'Drop the MP3 file here' : 'Drag & drop your MP3 file here'}
                  </p>
                  <p className="text-xs text-gray-500">
                    MP3 only up to 150MB • Missing metadata will be added automatically
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Permissions */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">Permissions & Rights</h2>
            
            <div className="space-y-4">
              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="isrc_requested"
                  {...register('isrc_requested')}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="isrc_requested" className="ml-2 block text-sm text-gray-900">
                  Request ISRC code for this track
                </label>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="radio_permission"
                  {...register('radio_permission')}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="radio_permission" className="ml-2 block text-sm text-gray-900">
                  Grant permission for radio play
                </label>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="public_display"
                  {...register('public_display')}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="public_display" className="ml-2 block text-sm text-gray-900">
                  Allow public display on website
                </label>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="podcast_permission"
                  {...register('podcast_permission')}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="podcast_permission" className="ml-2 block text-sm text-gray-900">
                  Grant permission for podcast use
                </label>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="commercial_use"
                  {...register('commercial_use')}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="commercial_use" className="ml-2 block text-sm text-gray-900">
                  Grant permission for commercial use
                </label>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="rights_attestation"
                  {...register('rights_attestation', { required: 'You must attest to having the rights' })}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="rights_attestation" className="ml-2 block text-sm text-gray-900">
                  I attest that I have the rights to submit this music *
                </label>
              </div>
              {errors.rights_attestation && (
                <p className="text-sm text-red-600">{errors.rights_attestation.message}</p>
              )}
            </div>
          </div>

          {/* Professional Information */}
          <div className="bg-white rounded-lg shadow-sm p-6">
            <h2 className="text-lg font-medium text-gray-900 mb-4">Professional Information (Optional)</h2>
            
            <div className="space-y-4">
              <div>
                <label htmlFor="pro_affiliation" className="block text-sm font-medium text-gray-700">
                  PRO Affiliation
                </label>
                <input
                  type="text"
                  id="pro_affiliation"
                  {...register('pro_info.pro_affiliation')}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., ASCAP, BMI, SESAC"
                />
              </div>

              <div>
                <label htmlFor="writer_splits" className="block text-sm font-medium text-gray-700">
                  Writer Splits
                </label>
                <input
                  type="text"
                  id="writer_splits"
                  {...register('pro_info.writer_splits')}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="e.g., 50% Writer, 50% Publisher"
                />
              </div>

              <div>
                <label htmlFor="publisher" className="block text-sm font-medium text-gray-700">
                  Publisher
                </label>
                <input
                  type="text"
                  id="publisher"
                  {...register('pro_info.publisher')}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Publisher name"
                />
              </div>
            </div>
          </div>

          {/* Submit Button */}
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={isUploading || !uploadedFile || !watchedArtistId}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            >
              {isUploading ? (
                <>
                  <Loader2 className="animate-spin h-4 w-4 mr-2" />
                  Uploading...
                </>
              ) : (
                'Submit Music'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
