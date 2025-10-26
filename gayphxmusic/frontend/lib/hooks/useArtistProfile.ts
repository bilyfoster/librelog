import { useState, useEffect } from 'react';
import { api } from '../api';

interface ArtistProfile {
  id: string;
  name: string;
  email: string;
  pronouns?: string;
  bio?: string;
  social_links?: {
    instagram?: string;
    twitter?: string;
    tiktok?: string;
    spotify?: string;
  };
}

export function useArtistProfile(email: string) {
  const [profile, setProfile] = useState<ArtistProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!email) {
      setProfile(null);
      return;
    }

    const fetchProfile = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const response = await fetch(`http://localhost:8000/api/auth/profile/${encodeURIComponent(email)}`);
        
        if (response.ok) {
          const data = await response.json();
          setProfile(data);
        } else if (response.status === 404) {
          // Artist not found - this is normal for new users
          setProfile(null);
        } else {
          throw new Error('Failed to fetch profile');
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Unknown error');
        setProfile(null);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [email]);

  return { profile, loading, error };
}
