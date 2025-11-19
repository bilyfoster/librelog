import React, { useEffect, useRef, useState } from 'react'
import { Box, Typography } from '@mui/material'
import WaveSurfer from 'wavesurfer.js'

interface WaveformDisplayProps {
  audioBlob?: Blob
  audioUrl?: string
  width?: number
  height?: number
}

const WaveformDisplay: React.FC<WaveformDisplayProps> = ({
  audioBlob,
  audioUrl,
  width = 800,
  height = 200,
}) => {
  const waveformRef = useRef<HTMLDivElement>(null)
  const wavesurferRef = useRef<WaveSurfer | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isPlaying, setIsPlaying] = useState(false)
  const [duration, setDuration] = useState(0)
  const [currentTime, setCurrentTime] = useState(0)

  useEffect(() => {
    if (!waveformRef.current) return

    // Initialize WaveSurfer
    // Note: wavesurfer.js v7 uses WaveSurfer.create() static method
    const wavesurfer = WaveSurfer.create({
      container: waveformRef.current,
      waveColor: '#1976d2',
      progressColor: '#42a5f5',
      cursorColor: '#1976d2',
      barWidth: 2,
      barRadius: 3,
      responsive: true,
      height: height,
      normalize: true,
    })

    wavesurferRef.current = wavesurfer

    // Event listeners
    wavesurfer.on('ready', () => {
      setIsLoading(false)
      setDuration(wavesurfer.getDuration())
    })

    wavesurfer.on('play', () => setIsPlaying(true))
    wavesurfer.on('pause', () => setIsPlaying(false))
    wavesurfer.on('finish', () => setIsPlaying(false))

    wavesurfer.on('timeupdate', (time) => {
      setCurrentTime(time)
    })

    // Load audio
    if (audioBlob) {
      const url = URL.createObjectURL(audioBlob)
      setIsLoading(true)
      wavesurfer.load(url)
    } else if (audioUrl) {
      setIsLoading(true)
      wavesurfer.load(audioUrl)
    }

    return () => {
      if (wavesurferRef.current) {
        wavesurferRef.current.destroy()
      }
      if (audioBlob) {
        URL.revokeObjectURL(URL.createObjectURL(audioBlob))
      }
    }
  }, [audioBlob, audioUrl, height])

  const handlePlayPause = () => {
    if (wavesurferRef.current) {
      wavesurferRef.current.playPause()
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <Box>
      <Box
        ref={waveformRef}
        sx={{
          width: '100%',
          minHeight: height,
          bgcolor: 'grey.100',
          borderRadius: 1,
          mb: 1,
        }}
      />
      {isLoading && (
        <Typography variant="caption" color="text.secondary">
          Loading waveform...
        </Typography>
      )}
      {!isLoading && duration > 0 && (
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="caption" color="text.secondary">
            {formatTime(currentTime)} / {formatTime(duration)}
          </Typography>
          <button
            onClick={handlePlayPause}
            style={{
              border: 'none',
              background: 'none',
              cursor: 'pointer',
              padding: '4px 8px',
            }}
          >
            {isPlaying ? '⏸ Pause' : '▶ Play'}
          </button>
        </Box>
      )}
    </Box>
  )
}

export default WaveformDisplay

