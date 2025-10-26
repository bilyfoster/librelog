'use client';

import React, { useState, useEffect } from 'react';

interface PlayStats {
  total_plays: number;
  radio_plays: number;
  podcast_plays: number;
  commercial_plays: number;
  plays_this_week: number;
  plays_this_month: number;
  plays_this_year: number;
  most_played_hour: number;
  most_played_day: number;
  last_played_at: string;
  last_played_by: string;
}

interface PlayStatisticsProps {
  submissionId: string;
}

export default function PlayStatistics({ submissionId }: PlayStatisticsProps) {
  const [stats, setStats] = useState<PlayStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchPlayStatistics();
  }, [submissionId]);

  const fetchPlayStatistics = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/api/plays/statistics/${submissionId}`);
      
      if (!response.ok) {
        throw new Error('Failed to fetch play statistics');
      }
      
      const data = await response.json();
      setStats(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setLoading(false);
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

  const getDayName = (dayNumber: number) => {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    return days[dayNumber];
  };

  const getTimeSlot = (hour: number) => {
    if (6 <= hour && hour < 12) return 'Morning';
    if (12 <= hour && hour < 17) return 'Afternoon';
    if (17 <= hour && hour < 22) return 'Evening';
    return 'Late Night';
  };

  if (loading) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Play Statistics</h3>
        <div className="animate-pulse">
          <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
          <div className="h-4 bg-gray-200 rounded w-2/3"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Play Statistics</h3>
        <div className="text-red-600">
          <p>Error loading statistics: {error}</p>
          <button 
            onClick={fetchPlayStatistics}
            className="mt-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!stats) {
    return (
      <div className="card">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Play Statistics</h3>
        <p className="text-gray-600">No play data available yet.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Play Statistics</h3>
      
      {/* Overall Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="text-center">
          <div className="text-2xl font-bold text-blue-600">{stats.total_plays}</div>
          <div className="text-sm text-gray-600">Total Plays</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-green-600">{stats.radio_plays}</div>
          <div className="text-sm text-gray-600">Radio Plays</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-purple-600">{stats.podcast_plays}</div>
          <div className="text-sm text-gray-600">Podcast Plays</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-orange-600">{stats.commercial_plays}</div>
          <div className="text-sm text-gray-600">Commercial Plays</div>
        </div>
      </div>

      {/* Time-based Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div className="bg-gray-50 p-4 rounded-lg">
          <div className="text-lg font-semibold text-gray-900">{stats.plays_this_week}</div>
          <div className="text-sm text-gray-600">This Week</div>
        </div>
        <div className="bg-gray-50 p-4 rounded-lg">
          <div className="text-lg font-semibold text-gray-900">{stats.plays_this_month}</div>
          <div className="text-sm text-gray-600">This Month</div>
        </div>
        <div className="bg-gray-50 p-4 rounded-lg">
          <div className="text-lg font-semibold text-gray-900">{stats.plays_this_year}</div>
          <div className="text-sm text-gray-600">This Year</div>
        </div>
      </div>

      {/* Peak Times */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <div className="bg-blue-50 p-4 rounded-lg">
          <div className="text-sm text-gray-600 mb-1">Most Played Time</div>
          <div className="font-semibold text-gray-900">
            {stats.most_played_hour !== null ? `${stats.most_played_hour}:00` : 'N/A'} 
            {stats.most_played_hour !== null && ` (${getTimeSlot(stats.most_played_hour)})`}
          </div>
        </div>
        <div className="bg-green-50 p-4 rounded-lg">
          <div className="text-sm text-gray-600 mb-1">Most Played Day</div>
          <div className="font-semibold text-gray-900">
            {stats.most_played_day !== null ? getDayName(stats.most_played_day) : 'N/A'}
          </div>
        </div>
      </div>

      {/* Last Play */}
      {stats.last_played_at && (
        <div className="bg-yellow-50 p-4 rounded-lg">
          <div className="text-sm text-gray-600 mb-1">Last Played</div>
          <div className="font-semibold text-gray-900">
            {formatDate(stats.last_played_at)}
          </div>
          {stats.last_played_by && (
            <div className="text-sm text-gray-600">
              by {stats.last_played_by}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

