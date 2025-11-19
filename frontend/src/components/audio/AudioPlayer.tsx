/**
 * AudioPlayer - Reusable audio player component with authentication support
 */

import React, { useState, useRef, useEffect } from 'react'
import {
  Box,
  IconButton,
  Slider,
  Typography,
  CircularProgress,
  Alert,
} from '@mui/material'
import {
  PlayArrow,
  Pause,
  VolumeUp,
  VolumeOff,
} from '@mui/icons-material'
import api from '../../utils/api'

interface AudioPlayerProps {
  src: string
  title?: string
  autoPlay?: boolean
  onEnded?: () => void
  onError?: (error: Error) => void
}

const AudioPlayer: React.FC<AudioPlayerProps> = ({
  src,
  title,
  autoPlay = false,
  onEnded,
  onError,
}) => {
  const [playing, setPlaying] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [duration, setDuration] = useState(0)
  const [currentTime, setCurrentTime] = useState(0)
  const [volume, setVolume] = useState(1)
  const [muted, setMuted] = useState(false)
  const audioRef = useRef<HTMLAudioElement | null>(null)
  const blobUrlRef = useRef<string | null>(null)

  // Cleanup blob URL on unmount
  useEffect(() => {
    return () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current)
      }
      if (audioRef.current) {
        audioRef.current.pause()
        audioRef.current.src = ''
      }
    }
  }, [])

  // Load audio with authentication
  useEffect(() => {
    const loadAudio = async () => {
      if (!src) return

      setLoading(true)
      setError(null)

      try {
        // Construct the full URL
        let audioUrl = src
        
        // If src is already a full URL or blob URL, use it directly
        if (src.startsWith('blob:') || src.startsWith('http://') || src.startsWith('https://')) {
          audioUrl = src
        } else {
          // For relative paths, use as-is (browser will resolve through Traefik)
          // Don't add /api prefix if src already starts with /api
          if (src.startsWith('/api/')) {
            audioUrl = src
          } else if (src.startsWith('/')) {
            audioUrl = `/api${src}`
          } else {
            audioUrl = `/api/${src}`
          }
        }

        // For preview endpoints (which are public), use the URL directly
        // This allows the browser to handle streaming and range requests properly
        if (audioUrl.includes('/preview') || audioUrl.includes('/tracks/')) {
          if (audioRef.current) {
            // Set up the audio element with the URL directly
            // This allows the browser to handle streaming, range requests, and caching
            audioRef.current.src = audioUrl
            audioRef.current.crossOrigin = 'anonymous'
            
            // Wait for metadata to load
            await new Promise((resolve, reject) => {
              if (!audioRef.current) {
                reject(new Error('Audio element not available'))
                return
              }
              
              const audio = audioRef.current
              
              const handleLoadedMetadata = () => {
                audio.removeEventListener('loadedmetadata', handleLoadedMetadata)
                audio.removeEventListener('error', handleError)
                resolve(undefined)
              }
              
              const handleError = (e: Event) => {
                audio.removeEventListener('loadedmetadata', handleLoadedMetadata)
                audio.removeEventListener('error', handleError)
                const error = (e.target as HTMLAudioElement).error
                reject(new Error(error ? `Audio error: ${error.code} - ${error.message}` : 'Failed to load audio'))
              }
              
              audio.addEventListener('loadedmetadata', handleLoadedMetadata)
              audio.addEventListener('error', handleError)
              
              // Start loading
              audio.load()
              
              // Timeout after 30 seconds
              setTimeout(() => {
                audio.removeEventListener('loadedmetadata', handleLoadedMetadata)
                audio.removeEventListener('error', handleError)
                reject(new Error('Audio loading timeout'))
              }, 30000)
            })
          }
          setLoading(false)
          return
        }

        // For other endpoints, fetch with authentication and create blob
        const token = localStorage.getItem('token')
        const response = await fetch(audioUrl, {
          headers: {
            'Authorization': `Bearer ${token}`,
          },
        })

        if (!response.ok) {
          throw new Error(`Failed to load audio: ${response.statusText}`)
        }

        const blob = await response.blob()
        const blobUrl = URL.createObjectURL(blob)
        blobUrlRef.current = blobUrl

        if (audioRef.current) {
          audioRef.current.src = blobUrl
          await audioRef.current.load()
        }

        setLoading(false)
      } catch (err: any) {
        setError(err.message || 'Failed to load audio')
        setLoading(false)
        if (onError) {
          onError(err)
        }
      }
    }

    loadAudio()
  }, [src, onError])

  // Set up audio event listeners
  useEffect(() => {
    const audio = audioRef.current
    if (!audio) return

    const updateTime = () => setCurrentTime(audio.currentTime)
    const updateDuration = () => setDuration(audio.duration)
    const handleEnded = () => {
      setPlaying(false)
      if (onEnded) onEnded()
    }
    const handleError = () => {
      setError('Audio playback error')
      setPlaying(false)
      if (onError) {
        onError(new Error('Audio playback error'))
      }
    }

    audio.addEventListener('timeupdate', updateTime)
    audio.addEventListener('loadedmetadata', updateDuration)
    audio.addEventListener('ended', handleEnded)
    audio.addEventListener('error', handleError)

    return () => {
      audio.removeEventListener('timeupdate', updateTime)
      audio.removeEventListener('loadedmetadata', updateDuration)
      audio.removeEventListener('ended', handleEnded)
      audio.removeEventListener('error', handleError)
    }
  }, [onEnded, onError])

  const togglePlay = async () => {
    const audio = audioRef.current
    if (!audio) return

    try {
      if (playing) {
        audio.pause()
        setPlaying(false)
      } else {
        await audio.play()
        setPlaying(true)
      }
    } catch (err: any) {
      setError(err.message || 'Playback failed')
      if (onError) {
        onError(err)
      }
    }
  }

  const handleSeek = (_event: Event, newValue: number | number[]) => {
    const audio = audioRef.current
    if (!audio) return
    const seekTime = Array.isArray(newValue) ? newValue[0] : newValue
    audio.currentTime = seekTime
    setCurrentTime(seekTime)
  }

  const handleVolumeChange = (_event: Event, newValue: number | number[]) => {
    const audio = audioRef.current
    if (!audio) return
    const newVolume = Array.isArray(newValue) ? newValue[0] : newValue
    audio.volume = newVolume
    setVolume(newVolume)
    setMuted(newVolume === 0)
  }

  const toggleMute = () => {
    const audio = audioRef.current
    if (!audio) return
    audio.muted = !muted
    setMuted(!muted)
  }

  const formatTime = (seconds: number) => {
    if (isNaN(seconds)) return '0:00'
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mt: 2 }}>
        {error}
      </Alert>
    )
  }

  return (
    <Box sx={{ mt: 2 }}>
      {title && (
        <Typography variant="subtitle1" gutterBottom>
          {title}
        </Typography>
      )}
      
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 2 }}>
        <IconButton
          onClick={togglePlay}
          disabled={loading || !src}
          color="primary"
          size="large"
        >
          {loading ? (
            <CircularProgress size={24} />
          ) : playing ? (
            <Pause />
          ) : (
            <PlayArrow />
          )}
        </IconButton>

        <Box sx={{ flex: 1 }}>
          <Slider
            value={currentTime}
            max={duration || 100}
            onChange={handleSeek}
            disabled={loading || !src}
            size="small"
            sx={{ mb: 0.5 }}
          />
          <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
            <Typography variant="caption" color="text.secondary">
              {formatTime(currentTime)}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {formatTime(duration)}
            </Typography>
          </Box>
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, minWidth: 120 }}>
          <IconButton onClick={toggleMute} size="small">
            {muted ? <VolumeOff /> : <VolumeUp />}
          </IconButton>
          <Slider
            value={muted ? 0 : volume}
            min={0}
            max={1}
            step={0.01}
            onChange={handleVolumeChange}
            size="small"
            sx={{ width: 80 }}
          />
        </Box>
      </Box>

      <audio
        ref={audioRef}
        style={{ display: 'none' }}
        preload="metadata"
      />
    </Box>
  )
}

export default AudioPlayer

