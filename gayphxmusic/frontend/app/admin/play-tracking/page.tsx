'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { BarChart3, RefreshCw, Settings, ArrowLeft, Play, Pause, AlertCircle } from 'lucide-react';

interface PlayStats {
  total_submissions: number;
  total_plays: number;
  plays_this_week: number;
  average_plays_per_submission: number;
}

interface TopTrack {
  submission_id: string;
  song_title: string;
  artist_name: string;
  play_count: number;
  tracking_id: string;
}

interface RecentPlay {
  id: string;
  submission_id: string;
  song_title: string;
  artist_name: string;
  played_at: string;
  dj_name: string;
  show_name: string;
  time_slot: string;
}

interface LibreTimeConfig {
  configured: boolean;
  libretime_url: string;
  sync_interval_minutes: number;
  auto_sync_enabled: boolean;
  sync_status: string;
  last_sync_at: string;
  error_count: number;
  last_error: string;
}

export default function PlayTrackingPage() {
  const [overallStats, setOverallStats] = useState<PlayStats | null>(null);
  const [topTracks, setTopTracks] = useState<TopTrack[]>([]);
  const [recentPlays, setRecentPlays] = useState<RecentPlay[]>([]);
  const [libretimeConfig, setLibretimeConfig] = useState<LibreTimeConfig | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      
      // Fetch all data in parallel
      const [statsRes, topTracksRes, recentPlaysRes, configRes] = await Promise.all([
        fetch('/api/plays/statistics'),
        fetch('/api/plays/top-tracks?period=month&limit=10'),
        fetch('/api/plays/recent-plays?hours=24&limit=20'),
        fetch('/api/plays/libretime-config')
      ]);

      if (statsRes.ok) {
        const stats = await statsRes.json();
        setOverallStats(stats);
      }

      if (topTracksRes.ok) {
        const tracks = await topTracksRes.json();
        setTopTracks(tracks);
      }

      if (recentPlaysRes.ok) {
        const plays = await recentPlaysRes.json();
        setRecentPlays(plays);
      }

      if (configRes.ok) {
        const config = await configRes.json();
        setLibretimeConfig(config);
      }
    } catch (error) {
      console.error('Error fetching play tracking data:', error);
    } finally {
      setLoading(false);
    }
  };

  const syncLibreTime = async () => {
    try {
      setSyncing(true);
      const response = await fetch('/api/plays/sync-libretime?hours_back=24', {
        method: 'POST'
      });

      if (response.ok) {
        alert('LibreTime sync started in background. Check back in a few minutes.');
        // Refresh data after a short delay
        setTimeout(fetchData, 5000);
      } else {
        alert('Failed to start LibreTime sync');
      }
    } catch (error) {
      console.error('Error syncing LibreTime:', error);
      alert('Error syncing LibreTime');
    } finally {
      setSyncing(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="animate-pulse">
            <div className="h-8 bg-gray-200 rounded w-1/4 mb-6"></div>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-24 bg-gray-200 rounded"></div>
              ))}
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-lime-400 to-green-500 relative overflow-hidden">
      {/* Zigzag Pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `linear-gradient(45deg, white 25%, transparent 25%), linear-gradient(-45deg, white 25%, transparent 25%), linear-gradient(45deg, transparent 75%, white 75%), linear-gradient(-45deg, transparent 75%, white 75%)`,
          backgroundSize: '30px 30px',
          backgroundPosition: '0 0, 0 15px, 15px -15px, -15px 0px'
        }}></div>
      </div>
      
      {/* Play Syncing Area Banner */}
      <div className="bg-lime-600 border-b-4 border-lime-800 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2">
          <div className="flex items-center justify-center">
            <AlertCircle className="h-5 w-5 text-lime-100 mr-2" />
            <span className="text-lime-100 font-semibold">PLAY SYNCING AREA - Total Play Statistics from LibreTime</span>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 relative z-10">
        {/* Header */}
        <div className="mb-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Play Tracking</h1>
              <p className="mt-2 text-gray-600">Monitor total plays from LibreTime radio automation</p>
            </div>
            <div className="flex space-x-4">
              <button
                onClick={syncLibreTime}
                disabled={syncing}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
              >
                {syncing ? 'Syncing...' : 'Sync LibreTime'}
              </button>
              <Link
                href="/admin/libretime-config"
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
              >
                Configure LibreTime
              </Link>
              <Link
                href="/admin"
                className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700"
              >
                Back to Dashboard
              </Link>
            </div>
          </div>
        </div>

        {/* LibreTime Configuration Status */}
        {libretimeConfig && (
          <div className="mb-6">
            <div className={`p-4 rounded-lg ${
              libretimeConfig.configured 
                ? 'bg-green-50 border border-green-200' 
                : 'bg-yellow-50 border border-yellow-200'
            }`}>
              <div className="flex items-center">
                <div className={`w-3 h-3 rounded-full mr-3 ${
                  libretimeConfig.configured ? 'bg-green-500' : 'bg-yellow-500'
                }`}></div>
                <div>
                  <h3 className="font-semibold text-gray-900">
                    LibreTime Integration: {libretimeConfig.configured ? 'Configured' : 'Not Configured'}
                  </h3>
                  {libretimeConfig.configured ? (
                    <div className="text-sm text-gray-600 mt-1">
                      <p>URL: {libretimeConfig.libretime_url}</p>
                      <p>Status: {libretimeConfig.sync_status}</p>
                      <p>Last Sync: {libretimeConfig.last_sync_at ? formatDate(libretimeConfig.last_sync_at) : 'Never'}</p>
                      {libretimeConfig.error_count > 0 && (
                        <p className="text-red-600">Errors: {libretimeConfig.error_count}</p>
                      )}
                    </div>
                  ) : (
                    <p className="text-sm text-gray-600 mt-1">
                      Configure LibreTime integration to automatically sync play data.
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Overall Statistics */}
        {overallStats && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
            <div className="card">
              <div className="text-2xl font-bold text-blue-600">{overallStats.total_plays}</div>
              <div className="text-sm text-gray-600">Total Plays</div>
            </div>
            <div className="card">
              <div className="text-2xl font-bold text-green-600">{overallStats.total_submissions}</div>
              <div className="text-sm text-gray-600">Total Submissions</div>
            </div>
            <div className="card">
              <div className="text-2xl font-bold text-purple-600">{overallStats.plays_this_week}</div>
              <div className="text-sm text-gray-600">Plays This Week</div>
            </div>
            <div className="card">
              <div className="text-2xl font-bold text-orange-600">
                {overallStats.average_plays_per_submission.toFixed(1)}
              </div>
              <div className="text-sm text-gray-600">Avg Plays/Submission</div>
            </div>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Top Tracks */}
          <div className="card">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">Top Tracks This Month</h2>
            </div>
            <div className="p-6">
              {topTracks.length > 0 ? (
                <div className="space-y-4">
                  {topTracks.map((track, index) => (
                    <div key={track.submission_id} className="flex items-center justify-between">
                      <div className="flex items-center">
                        <div className="w-8 h-8 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-sm font-semibold mr-3">
                          {index + 1}
                        </div>
                        <div>
                          <div className="font-medium text-gray-900">{track.song_title}</div>
                          <div className="text-sm text-gray-600">by {track.artist_name}</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-semibold text-gray-900">{track.play_count}</div>
                        <div className="text-sm text-gray-600">plays</div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-600">No play data available yet.</p>
              )}
            </div>
          </div>

          {/* Recent Plays */}
          <div className="card">
            <div className="p-6 border-b border-gray-200">
              <h2 className="text-lg font-semibold text-gray-900">Recent Plays (24h)</h2>
            </div>
            <div className="p-6">
              {recentPlays.length > 0 ? (
                <div className="space-y-4">
                  {recentPlays.map((play) => (
                    <div key={play.id} className="flex items-center justify-between">
                      <div>
                        <div className="font-medium text-gray-900">{play.song_title}</div>
                        <div className="text-sm text-gray-600">
                          by {play.artist_name} • {play.time_slot}
                        </div>
                        {play.dj_name && (
                          <div className="text-xs text-gray-500">DJ: {play.dj_name}</div>
                        )}
                      </div>
                      <div className="text-right">
                        <div className="text-sm text-gray-600">{formatDate(play.played_at)}</div>
                        <div className="text-xs text-gray-500">Radio Play</div>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-gray-600">No recent plays found.</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
