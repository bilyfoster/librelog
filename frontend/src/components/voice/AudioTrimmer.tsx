import React, { useEffect, useRef, useState } from 'react'
import {
  Box,
  Button,
  Typography,
  Slider,
  IconButton,
  Tooltip,
  Paper,
} from '@mui/material'
import {
  PlayArrow,
  Pause,
  ContentCut,
  Save,
  Undo,
} from '@mui/icons-material'
import WaveSurfer from 'wavesurfer.js'
import RegionsPlugin from 'wavesurfer.js/dist/plugins/regions.esm.js'
import api from '../../utils/api'

interface AudioTrimmerProps {
  audioBlob: Blob
  onTrim: (trimmedBlob: Blob, startTime: number, endTime: number) => void
  onCancel?: () => void
}

const AudioTrimmer: React.FC<AudioTrimmerProps> = ({
  audioBlob,
  onTrim,
  onCancel,
}) => {
  const waveformRef = useRef<HTMLDivElement>(null)
  const wavesurferRef = useRef<WaveSurfer | null>(null)
  const regionsRef = useRef<RegionsPlugin | null>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [duration, setDuration] = useState(0)
  const [startTime, setStartTime] = useState(0)
  const [endTime, setEndTime] = useState(0)
  const [isTrimming, setIsTrimming] = useState(false)

  useEffect(() => {
    if (!waveformRef.current) return

    const url = URL.createObjectURL(audioBlob)

    // Initialize WaveSurfer with regions plugin
    const wavesurfer = WaveSurfer.create({
      container: waveformRef.current,
      waveColor: '#1976d2',
      progressColor: '#42a5f5',
      cursorColor: '#1976d2',
      selectionColor: '#ff9800',
      barWidth: 2,
      barRadius: 3,
      responsive: true,
      height: 150,
      normalize: true,
    })

    // Add regions plugin
    const regions = wavesurfer.registerPlugin(RegionsPlugin.create())
    regionsRef.current = regions

    wavesurferRef.current = wavesurfer

    // Event listeners
    wavesurfer.on('ready', () => {
      setDuration(wavesurfer.getDuration())
      setEndTime(wavesurfer.getDuration())
      
      // Create initial region covering entire track
      const region = regions.addRegion({
        start: 0,
        end: wavesurfer.getDuration(),
        color: 'rgba(255, 152, 0, 0.3)',
        drag: true,
        resize: true,
      })

      // Update start/end times when region changes
      region.on('update-end', () => {
        setStartTime(region.start)
        setEndTime(region.end)
      })

      // Play only the selected region
      region.on('click', () => {
        wavesurfer.play(region.start)
        wavesurfer.once('timeupdate', (time) => {
          if (time >= region.end) {
            wavesurfer.pause()
          }
        })
      })
    })

    wavesurfer.on('play', () => setIsPlaying(true))
    wavesurfer.on('pause', () => setIsPlaying(false))
    wavesurfer.on('finish', () => setIsPlaying(false))

    wavesurfer.load(url)

    return () => {
      if (wavesurferRef.current) {
        wavesurferRef.current.destroy()
      }
      URL.revokeObjectURL(url)
    }
  }, [audioBlob])

  const handlePlayPause = () => {
    if (!wavesurferRef.current || !regionsRef.current) return

    const regions = regionsRef.current.getRegions()
    if (regions.length > 0) {
      const region = regions[0]
      if (isPlaying) {
        wavesurferRef.current.pause()
      } else {
        wavesurferRef.current.play(region.start)
        wavesurferRef.current.once('timeupdate', (time) => {
          if (time >= region.end) {
            wavesurferRef.current?.pause()
          }
        })
      }
    } else {
      wavesurferRef.current.playPause()
    }
  }

  const handleTrim = async () => {
    if (!audioBlob || startTime >= endTime) return

    setIsTrimming(true)
    try {
      // Create FormData with trim parameters
      const formData = new FormData()
      formData.append('file', audioBlob, 'recording.webm')
      formData.append('start_time', startTime.toString())
      formData.append('end_time', endTime.toString())

      // Call API to trim
      const response = await api.post('/voice/trim', formData, {
        responseType: 'blob',
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })

      const trimmedBlob = new Blob([response.data], { type: 'audio/webm' })
      onTrim(trimmedBlob, startTime, endTime)
    } catch (error: any) {
      console.error('Trim failed:', error)
      const errorMessage = error?.response?.data?.detail || error?.message || 'Failed to trim audio'
      alert(`Failed to trim audio: ${errorMessage}`)
    } finally {
      setIsTrimming(false)
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <Paper sx={{ p: 2, mt: 2 }}>
      <Typography variant="h6" gutterBottom>
        Trim Audio
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Drag the orange region markers to select the portion to keep. Click and drag the region to move it.
      </Typography>

      {/* Waveform */}
      <Box
        ref={waveformRef}
        sx={{
          width: '100%',
          minHeight: 150,
          bgcolor: 'grey.100',
          borderRadius: 1,
          mb: 2,
        }}
      />

      {/* Trim controls */}
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <Box sx={{ minWidth: 100 }}>
            <Typography variant="caption" color="text.secondary">
              Start: {formatTime(startTime)}
            </Typography>
            <Slider
              value={startTime}
              min={0}
              max={duration}
              step={0.1}
              onChange={(e, value) => {
                setStartTime(value as number)
                const regions = regionsRef.current?.getRegions()
                if (regions && regions.length > 0) {
                  regions[0].setStart(value as number)
                }
              }}
              size="small"
            />
          </Box>
          <Box sx={{ minWidth: 100 }}>
            <Typography variant="caption" color="text.secondary">
              End: {formatTime(endTime)}
            </Typography>
            <Slider
              value={endTime}
              min={0}
              max={duration}
              step={0.1}
              onChange={(e, value) => {
                setEndTime(value as number)
                const regions = regionsRef.current?.getRegions()
                if (regions && regions.length > 0) {
                  regions[0].setEnd(value as number)
                }
              }}
              size="small"
            />
          </Box>
        </Box>

        <Typography variant="caption" color="text.secondary">
          Duration: {formatTime(endTime - startTime)} (Original: {formatTime(duration)})
        </Typography>
      </Box>

      {/* Action buttons */}
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <Tooltip title="Play selected region">
          <IconButton onClick={handlePlayPause} color="primary">
            {isPlaying ? <Pause /> : <PlayArrow />}
          </IconButton>
        </Tooltip>
        <Button
          variant="contained"
          startIcon={<ContentCut />}
          onClick={handleTrim}
          disabled={isTrimming || startTime >= endTime}
        >
          {isTrimming ? 'Trimming...' : 'Apply Trim'}
        </Button>
        {onCancel && (
          <Button variant="outlined" startIcon={<Undo />} onClick={onCancel}>
            Cancel
          </Button>
        )}
      </Box>
    </Paper>
  )
}

export default AudioTrimmer

