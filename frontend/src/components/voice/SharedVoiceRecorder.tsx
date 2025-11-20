import React, { useState, useEffect } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  IconButton,
  LinearProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Tooltip,
} from '@mui/material'
import {
  Mic,
  Stop,
  Pause,
  PlayArrow,
  Delete,
  CheckCircle,
  ContentCut,
} from '@mui/icons-material'
import { useVoiceRecorder } from '../../hooks/useVoiceRecorder'
import WaveformDisplay from './WaveformDisplay'
import AudioTrimmer from './AudioTrimmer'

interface SharedVoiceRecorderProps {
  context: 'voice_track' | 'production'
  breakId?: number
  requestId?: number
  script?: string
  onUpload?: (blob: Blob) => Promise<void>
  onTakeSelect?: (takeId: number) => void
  onTakeDelete?: (takeId: number) => Promise<void>
  takes?: any[]
}

const SharedVoiceRecorder: React.FC<SharedVoiceRecorderProps> = ({
  context,
  breakId,
  requestId,
  script,
  onUpload,
  onTakeSelect,
  onTakeDelete,
  takes = [],
}) => {
  const {
    isRecording,
    isPaused,
    recordingTime,
    audioBlob,
    selectedTake,
    availableDevices,
    selectedDeviceId,
    setSelectedDeviceId,
    enumerateDevices,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    selectTake,
    reset,
  } = useVoiceRecorder({
    context,
    breakId,
    requestId,
    selectedDeviceId: null, // Will be managed by component state
    onRecordingComplete: async (blob) => {
      if (onUpload) {
        await onUpload(blob)
      }
    },
  })

  const [isTrimming, setIsTrimming] = useState(false)
  const [trimmedBlob, setTrimmedBlob] = useState<Blob | null>(null)
  const [localSelectedDeviceId, setLocalSelectedDeviceId] = useState<string>('')
  const [isEnumeratingDevices, setIsEnumeratingDevices] = useState(false)
  const [playingTakeId, setPlayingTakeId] = useState<number | null>(null)
  const [takeAudioElement, setTakeAudioElement] = useState<HTMLAudioElement | null>(null)

  // Enumerate devices on component mount and set initial device
  useEffect(() => {
    const loadDevices = async () => {
      setIsEnumeratingDevices(true)
      try {
        const devices = await enumerateDevices()
        const savedDeviceId = localStorage.getItem('voiceRecorder.selectedDeviceId')
        
        if (devices.length > 0) {
          // Use saved device if available and still exists in the enumerated devices, otherwise use first device
          const savedDeviceExists = savedDeviceId && devices.some(d => d.deviceId === savedDeviceId)
          const deviceToUse = savedDeviceExists ? savedDeviceId : devices[0].deviceId
          
          // Only set if we have a valid device ID that exists in availableDevices
          setLocalSelectedDeviceId(deviceToUse)
          setSelectedDeviceId(deviceToUse)
          
          // Save to localStorage if not already saved or if we're using a different device
          if (!savedDeviceId || !savedDeviceExists) {
            localStorage.setItem('voiceRecorder.selectedDeviceId', deviceToUse)
          }
        } else {
          // No devices available, clear selection
          setLocalSelectedDeviceId('')
        }
      } catch (error) {
        console.error('Failed to enumerate devices:', error)
        setLocalSelectedDeviceId('')
      } finally {
        setIsEnumeratingDevices(false)
      }
    }
    loadDevices()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []) // Only run on mount

  // Update hook's selectedDeviceId when local state changes (only if valid)
  useEffect(() => {
    if (localSelectedDeviceId && availableDevices.some(d => d.deviceId === localSelectedDeviceId)) {
      setSelectedDeviceId(localSelectedDeviceId)
    }
  }, [localSelectedDeviceId, setSelectedDeviceId, availableDevices])

  // Cleanup audio element on unmount
  useEffect(() => {
    return () => {
      if (takeAudioElement) {
        takeAudioElement.pause()
        takeAudioElement.src = ''
      }
    }
  }, [takeAudioElement])

  const handleTakePlay = async (take: any) => {
    try {
      // If this take is already playing, pause it
      if (playingTakeId === take.id && takeAudioElement) {
        takeAudioElement.pause()
        setPlayingTakeId(null)
        setTakeAudioElement(null)
        return
      }

      // Stop any currently playing audio
      if (takeAudioElement) {
        takeAudioElement.pause()
        takeAudioElement.src = ''
      }

      // Ensure file_url is properly formatted
      const fileUrl = take.file_url?.startsWith('/api/') 
        ? take.file_url 
        : `/api${take.file_url}`
      
      // Create a fresh audio element
      const audio = new Audio(fileUrl)
      audio.volume = 1.0
      
      // Set up event handlers
      const errorHandler = (e: Event) => {
        console.error('Audio error:', e, 'code:', audio.error?.code, 'message:', audio.error?.message)
        setPlayingTakeId(null)
        setTakeAudioElement(null)
        alert(`Failed to play audio: ${audio.error?.message || 'Unknown error'}`)
      }
      
      audio.addEventListener('ended', () => {
        setPlayingTakeId(null)
        setTakeAudioElement(null)
      })
      audio.addEventListener('error', errorHandler)
      
      // Set state before playing
      setPlayingTakeId(take.id)
      setTakeAudioElement(audio)
      
      await audio.play()
    } catch (err: any) {
      console.error('Failed to play take audio:', err)
      setPlayingTakeId(null)
      setTakeAudioElement(null)
      alert(`Failed to play audio: ${err.message || 'Unknown error'}`)
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const handleStart = async () => {
    try {
      await startRecording(localSelectedDeviceId || undefined)
    } catch (error) {
      console.error('Failed to start recording:', error)
      const errorMessage = error instanceof Error ? error.message : 'Unknown error'
      if (errorMessage.includes('device') || errorMessage.includes('not found')) {
        alert('Failed to start recording. The selected audio input device is not available. Please select a different device.')
      } else {
        alert('Failed to start recording. Please check microphone permissions.')
      }
    }
  }

  const handleDeviceChange = (deviceId: string) => {
    setLocalSelectedDeviceId(deviceId)
    setSelectedDeviceId(deviceId)
    // Save to localStorage for persistence
    localStorage.setItem('voiceRecorder.selectedDeviceId', deviceId)
  }

  const handleStop = () => {
    stopRecording()
  }

  const handlePause = () => {
    if (isPaused) {
      resumeRecording()
    } else {
      pauseRecording()
    }
  }

  const handleReset = () => {
    reset()
    setTrimmedBlob(null)
    setIsTrimming(false)
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          {context === 'voice_track' ? 'Voice Track Recording' : 'Voice Talent Recording'}
        </Typography>

        {/* Script display for production context */}
        {context === 'production' && script && (
          <Box sx={{ mb: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
            <Typography variant="subtitle2" gutterBottom>
              Script:
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
              {script}
            </Typography>
          </Box>
        )}

        {/* Audio input device selector */}
        <Box sx={{ mb: 2 }}>
          <FormControl fullWidth size="small" disabled={isRecording || isEnumeratingDevices}>
            <InputLabel id="audio-input-device-label">Audio Input Device</InputLabel>
            <Select
              labelId="audio-input-device-label"
              id="audio-input-device-select"
              value={availableDevices.some(d => d.deviceId === localSelectedDeviceId) ? localSelectedDeviceId : ''}
              label="Audio Input Device"
              onChange={(e) => handleDeviceChange(e.target.value)}
            >
              {availableDevices.length === 0 ? (
                <MenuItem value="" disabled>
                  {isEnumeratingDevices ? 'Loading devices...' : 'No devices available'}
                </MenuItem>
              ) : (
                availableDevices.map((device) => (
                  <MenuItem key={device.deviceId} value={device.deviceId}>
                    {device.label}
                  </MenuItem>
                ))
              )}
            </Select>
          </FormControl>
        </Box>

        {/* Recording controls */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          {!isRecording ? (
            <Button
              variant="contained"
              color="error"
              startIcon={<Mic />}
              onClick={handleStart}
              size="large"
            >
              Start Recording
            </Button>
          ) : (
            <>
              <Button
                variant="contained"
                color="error"
                startIcon={<Stop />}
                onClick={handleStop}
                size="large"
              >
                Stop
              </Button>
              <IconButton
                color="primary"
                onClick={handlePause}
                size="large"
              >
                {isPaused ? <PlayArrow /> : <Pause />}
              </IconButton>
              <Typography variant="h6" sx={{ minWidth: 60 }}>
                {formatTime(recordingTime)}
              </Typography>
            </>
          )}

          {audioBlob && (
            <>
              <Button
                variant="outlined"
                startIcon={<Delete />}
                onClick={handleReset}
                size="small"
              >
                Reset
              </Button>
              {(trimmedBlob || audioBlob) && onUpload && (
                <Button
                  variant="contained"
                  color="primary"
                  onClick={async () => {
                    const blobToUpload = trimmedBlob || audioBlob
                    if (onUpload) {
                      try {
                        await onUpload(blobToUpload)
                        // Clear the trimmed blob after successful save
                        if (trimmedBlob) {
                          setTrimmedBlob(null)
                        }
                      } catch (error) {
                        console.error('Failed to save recording:', error)
                      }
                    }
                  }}
                  size="small"
                >
                  {trimmedBlob ? 'Save Trimmed Recording' : 'Save Recording'}
                </Button>
              )}
            </>
          )}
        </Box>

        {/* Recording indicator */}
        {isRecording && (
          <Box sx={{ mb: 2 }}>
            <LinearProgress />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
              Recording in progress...
            </Typography>
          </Box>
        )}

        {/* Waveform display or trimmer */}
        {audioBlob && !isTrimming && (
          <Box sx={{ mb: 2 }}>
            <WaveformDisplay audioBlob={trimmedBlob || audioBlob} />
            <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                startIcon={<ContentCut />}
                onClick={() => setIsTrimming(true)}
                size="small"
              >
                Trim Audio
              </Button>
              {trimmedBlob && (
                <Button
                  variant="outlined"
                  startIcon={<Delete />}
                  onClick={() => {
                    setTrimmedBlob(null)
                    setIsTrimming(false)
                  }}
                  size="small"
                  color="warning"
                >
                  Reset Trim
                </Button>
              )}
            </Box>
          </Box>
        )}

        {/* Audio trimmer */}
        {audioBlob && isTrimming && (
          <AudioTrimmer
            audioBlob={audioBlob}
            onTrim={(trimmedBlob, startTime, endTime) => {
              setTrimmedBlob(trimmedBlob)
              setIsTrimming(false)
            }}
            onCancel={() => setIsTrimming(false)}
          />
        )}

        {/* Takes list */}
        {takes.length > 0 && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle1" gutterBottom>
              Takes for Break {breakId || 'N/A'} ({takes.length})
            </Typography>
            <List>
              {takes.map((take) => (
                <ListItem
                  key={take.id}
                  secondaryAction={
                    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                      {take.file_url && (
                        <Tooltip title={playingTakeId === take.id ? 'Pause' : 'Play'}>
                          <IconButton
                            size="small"
                            onClick={() => handleTakePlay(take)}
                            color={playingTakeId === take.id ? 'primary' : 'default'}
                          >
                            {playingTakeId === take.id ? <Pause /> : <PlayArrow />}
                          </IconButton>
                        </Tooltip>
                      )}
                      {take.is_final && (
                        <Chip
                          label="Selected"
                          color="success"
                          size="small"
                          icon={<CheckCircle />}
                        />
                      )}
                      {!take.is_final && (
                        <Button
                          size="small"
                          onClick={() => onTakeSelect?.(take.id)}
                        >
                          Select
                        </Button>
                      )}
                      {onTakeDelete && (
                        <IconButton
                          size="small"
                          color="error"
                          onClick={async () => {
                            if (window.confirm(`Delete Take ${take.take_number}? This cannot be undone.`)) {
                              await onTakeDelete(take.id)
                            }
                          }}
                        >
                          <Delete fontSize="small" />
                        </IconButton>
                      )}
                    </Box>
                  }
                >
                  <ListItemText
                    primary={`Take ${take.take_number}${take.track_metadata?.duration ? ` (${Math.round(take.track_metadata.duration)}s)` : ''}`}
                    secondary={new Date(take.created_at).toLocaleString()}
                  />
                </ListItem>
              ))}
            </List>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

export default SharedVoiceRecorder

