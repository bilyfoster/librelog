import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Chip,
} from '@mui/material'
import { PlayArrow, Pause, Download, CloudUpload, CheckCircle, Delete, Link as LinkIcon } from '@mui/icons-material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import SharedVoiceRecorder from '../../components/voice/SharedVoiceRecorder'
import PreviewPanels from '../../components/voice/PreviewPanels'
import TimingDisplay from '../../components/voice/TimingDisplay'
import api from '../../utils/api'

const VoiceRecorder: React.FC = () => {
  const [mode, setMode] = useState<'log' | 'standalone'>('log')
  const [selectedBreakId, setSelectedBreakId] = useState<number | null>(null)
  const [selectedLogId, setSelectedLogId] = useState<number | null>(null)
  const [selectedHour, setSelectedHour] = useState<number | null>(null)
  const [playingId, setPlayingId] = useState<number | null>(null)
  const [audioElement, setAudioElement] = useState<HTMLAudioElement | null>(null)
  const [audioProgress, setAudioProgress] = useState<number>(0)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  // Cleanup audio element on unmount
  useEffect(() => {
    return () => {
      if (audioElement) {
        audioElement.pause()
        // Don't clear src to avoid "Empty src attribute" error
        // Just pause and let it be garbage collected
      }
    }
  }, [audioElement])

  // Update audio progress
  useEffect(() => {
    if (!audioElement) {
      setAudioProgress(0)
      return
    }

    const updateProgress = () => {
      if (audioElement.duration && !isNaN(audioElement.duration) && audioElement.duration > 0) {
        const progress = (audioElement.currentTime / audioElement.duration) * 100
        setAudioProgress(Math.min(100, Math.max(0, progress)))
      }
    }

    const handleEnded = () => {
      console.log('Audio ended')
      setPlayingId(null)
      setAudioElement(null)
      setAudioProgress(0)
    }

    const handlePlaying = () => {
      console.log('Audio is playing, duration:', audioElement.duration, 'currentTime:', audioElement.currentTime)
    }

    audioElement.addEventListener('timeupdate', updateProgress)
    audioElement.addEventListener('ended', handleEnded)
    audioElement.addEventListener('playing', handlePlaying)

    return () => {
      audioElement.removeEventListener('timeupdate', updateProgress)
      audioElement.removeEventListener('ended', handleEnded)
      audioElement.removeEventListener('playing', handlePlaying)
    }
  }, [audioElement])

  // Fetch available logs
  const { data: logsData } = useQuery({
    queryKey: ['logs'],
    queryFn: async () => {
      const response = await api.get('/logs')
      return response.data
    },
  })

  // Fetch voice slots for selected log
  const { data: slotsData, isLoading: slotsLoading } = useQuery({
    queryKey: ['voice-slots', selectedLogId],
    queryFn: async () => {
      if (!selectedLogId) return null
      const response = await api.get(`/logs/${selectedLogId}/voice-slots`)
      return response.data
    },
    enabled: !!selectedLogId,
  })

  // Fetch takes for selected break
  const { data: takesData } = useQuery({
    queryKey: ['voice-takes', selectedBreakId],
    queryFn: async () => {
      if (!selectedBreakId) return []
      const response = await api.get(`/voice/breaks/${selectedBreakId}/takes`)
      return response.data
    },
    enabled: !!selectedBreakId,
  })

  // Upload recording mutation (for log-based)
  const uploadMutation = useMutation({
    mutationFn: async (blob: Blob) => {
      if (!selectedBreakId) throw new Error('No break selected')
      
      const formData = new FormData()
      formData.append('file', blob, `recording_${Date.now()}.webm`)
      
      const response = await api.post(
        `/voice/breaks/${selectedBreakId}/record`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          timeout: 300000, // 5 minutes for large audio file uploads
        }
      )
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-takes', selectedBreakId] })
    },
  })

  // Fetch standalone recordings
  const { data: standaloneRecordings, refetch: refetchStandalone, isLoading: isLoadingRecordings } = useQuery({
    queryKey: ['standalone-recordings'],
    queryFn: async () => {
      const response = await api.get('/voice/recordings/production')
      // Filter for standalone test recordings
      const recordings = response.data || []
      console.log('All recordings from API:', recordings)
      // Handle both object and string JSON formats
      const filtered = recordings.filter((r: any) => {
        const metadata = r.track_metadata
        if (!metadata) return false
        // Handle if metadata is already an object or needs parsing
        const type = typeof metadata === 'string' ? JSON.parse(metadata)?.type : metadata?.type
        const matches = type === 'standalone_test'
        if (!matches) {
          console.log('Recording filtered out:', r.id, 'metadata:', metadata, 'type:', type)
        }
        return matches
      })
      console.log('Filtered standalone recordings:', filtered)
      return filtered
    },
    refetchOnMount: true,
    refetchOnWindowFocus: false,
  })

  // Upload standalone recording mutation
  const uploadStandaloneMutation = useMutation({
    mutationFn: async (blob: Blob) => {
      const formData = new FormData()
      formData.append('file', blob, `test_recording_${Date.now()}.webm`)
      
      // Save as a test recording (not tied to a break)
      // Use longer timeout for file uploads (5 minutes for large audio files)
      const response = await api.post(
        `/voice/recordings/standalone`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          timeout: 300000, // 5 minutes for large audio file uploads
        }
      )
      return response.data
    },
    onSuccess: async () => {
      // Invalidate and refetch immediately
      await queryClient.invalidateQueries({ queryKey: ['standalone-recordings'] })
      await queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
      // Invalidate all voice-tracks queries (including 'all' and paginated)
      await queryClient.invalidateQueries({ queryKey: ['voice-tracks'] })
      // Also refetch the standalone recordings query
      await refetchStandalone()
    },
  })

  // Upload to LibreTime mutation
  const uploadToLibreTimeMutation = useMutation({
    mutationFn: async (recordingId: number) => {
      const response = await api.post(`/voice/${recordingId}/upload-to-libretime`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['standalone-recordings'] })
      queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
    },
  })

  // Delete recording mutation
  const deleteRecordingMutation = useMutation({
    mutationFn: async (recordingId: number) => {
      await api.delete(`/voice/recordings/${recordingId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['standalone-recordings'] })
      queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
      queryClient.invalidateQueries({ queryKey: ['voice-tracks'] })
    },
  })

  // Select take mutation
  const selectTakeMutation = useMutation({
    mutationFn: async (takeId: number) => {
      const response = await api.put(`/voice/takes/${takeId}/select`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-takes', selectedBreakId] })
    },
  })

  const handleUpload = async (blob: Blob) => {
    if (mode === 'standalone') {
      await uploadStandaloneMutation.mutateAsync(blob)
    } else {
      await uploadMutation.mutateAsync(blob)
    }
  }

  const handleTakeSelect = (takeId: number) => {
    selectTakeMutation.mutate(takeId)
  }

  const handleTakeDelete = async (takeId: number) => {
    try {
      await api.delete(`/voice/${takeId}`)
      queryClient.invalidateQueries({ queryKey: ['voice-takes', selectedBreakId] })
    } catch (error) {
      console.error('Failed to delete take:', error)
      throw error
    }
  }

  // Auto-select first break when slots load
  useEffect(() => {
    if (slotsData?.slots && slotsData.slots.length > 0 && !selectedBreakId) {
      const firstSlot = slotsData.slots[0]
      setSelectedBreakId(firstSlot.id)
      setSelectedHour(firstSlot.hour)
    }
  }, [slotsData, selectedBreakId])

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Voice Tracking
      </Typography>

      {/* Mode selection */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={mode} onChange={(e, newValue) => setMode(newValue)}>
          <Tab label="Log-Based Recording" value="log" />
          <Tab label="Test Recording" value="standalone" />
        </Tabs>
      </Box>

      {mode === 'standalone' ? (
        <Box>
          <Alert severity="info" sx={{ mb: 3 }}>
            Test recording mode - Record without selecting a log or break. This is useful for testing your setup or recording practice takes.
            You can upload your recordings directly to LibreTime library after recording.
            <Box sx={{ mt: 1 }}>
              <Button
                size="small"
                startIcon={<LinkIcon />}
                onClick={() => navigate('/voice/tracks')}
                variant="outlined"
              >
                View All Recordings
              </Button>
            </Box>
          </Alert>
          
          {uploadStandaloneMutation.isSuccess && (
            <Alert severity="success" sx={{ mb: 3 }}>
              Recording saved successfully! The list below will refresh automatically.
              <Button
                size="small"
                onClick={() => {
                  queryClient.invalidateQueries({ queryKey: ['standalone-recordings'] })
                  queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
                  queryClient.invalidateQueries({ queryKey: ['voice-tracks', 'all'] })
                  navigate('/voice/tracks')
                }}
                sx={{ ml: 1 }}
              >
                View in Manager
              </Button>
            </Alert>
          )}
          
          {uploadStandaloneMutation.isError && (
            <Alert severity="error" sx={{ mb: 3 }}>
              Failed to save recording: {uploadStandaloneMutation.error?.message}
            </Alert>
          )}
          <SharedVoiceRecorder
            context="voice_track"
            onUpload={handleUpload}
            takes={[]}
          />
          
          {/* Standalone recordings list */}
          <Box sx={{ mt: 4 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">
                My Test Recordings {standaloneRecordings ? `(${standaloneRecordings.length})` : ''}
              </Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button
                  size="small"
                  onClick={() => refetchStandalone()}
                  variant="outlined"
                >
                  Refresh
                </Button>
                <Button
                  size="small"
                  startIcon={<LinkIcon />}
                  onClick={() => navigate('/voice/tracks')}
                  variant="outlined"
                >
                  View All in Manager
                </Button>
              </Box>
            </Box>
            {isLoadingRecordings ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : standaloneRecordings && standaloneRecordings.length > 0 ? (
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Name</TableCell>
                      <TableCell>Date</TableCell>
                      <TableCell>Duration</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>LibreTime</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {standaloneRecordings.map((recording: any) => (
                      <TableRow key={recording.id}>
                        <TableCell>{recording.show_name || `Recording #${recording.id}`}</TableCell>
                        <TableCell>
                          {recording.created_at
                            ? new Date(recording.created_at).toLocaleString()
                            : 'N/A'}
                        </TableCell>
                        <TableCell>
                          {recording.track_metadata?.duration 
                            ? `${(recording.track_metadata.duration).toFixed(1)}s`
                            : 'N/A'}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={recording.status || 'DRAFT'}
                            size="small"
                            color={recording.status === 'READY' ? 'success' : 'default'}
                          />
                        </TableCell>
                        <TableCell>
                          {recording.libretime_id ? (
                            <Chip
                              label={recording.libretime_id}
                              size="small"
                              color="success"
                              icon={<CheckCircle />}
                            />
                          ) : (
                            <Chip label="Not uploaded" size="small" />
                          )}
                        </TableCell>
                        <TableCell>
                          <Tooltip title={playingId === recording.id ? 'Pause' : 'Play'}>
                            <Box sx={{ position: 'relative', display: 'inline-flex' }}>
                              <IconButton
                                size="small"
                                onClick={async () => {
                                  try {
                                    // If this recording is already playing, pause it
                                    if (playingId === recording.id && audioElement) {
                                      audioElement.pause()
                                      setPlayingId(null)
                                      setAudioElement(null)
                                      setAudioProgress(0)
                                      return
                                    }

                                    // Stop any currently playing audio
                                    if (audioElement) {
                                      audioElement.pause()
                                      // Don't clear src or remove listeners - let React cleanup handle it
                                    }

                                    // Ensure file_url is properly formatted
                                    const fileUrl = recording.file_url?.startsWith('/api/') 
                                      ? recording.file_url 
                                      : `/api${recording.file_url}`
                                    
                                    // Create a fresh audio element
                                    const audio = new Audio(fileUrl)
                                    
                                    // Set volume to 100% (some browsers default to muted)
                                    audio.volume = 1.0
                                    
                                    // Store handlers for cleanup
                                    const errorHandler = (e: Event) => {
                                      console.error('Audio error:', e, 'code:', audio.error?.code, 'message:', audio.error?.message)
                                      setPlayingId(null)
                                      setAudioElement(null)
                                      setAudioProgress(0)
                                      alert(`Failed to play audio: ${audio.error?.message || 'Unknown error'}`)
                                    }
                                    
                                    // Add event listeners
                                    audio.addEventListener('loadstart', () => console.log('Audio: loadstart'))
                                    audio.addEventListener('loadeddata', () => {
                                      console.log('Audio: loadeddata', 'duration:', audio.duration)
                                    })
                                    audio.addEventListener('canplay', () => console.log('Audio: canplay'))
                                    audio.addEventListener('playing', () => {
                                      console.log('Audio: playing - audio is actually playing now')
                                    })
                                    audio.addEventListener('pause', () => {
                                      console.log('Audio: paused')
                                    })
                                    audio.addEventListener('error', errorHandler)
                                    
                                    // Set state before playing to ensure UI updates
                                    setPlayingId(recording.id)
                                    setAudioElement(audio)
                                    setAudioProgress(0)
                                    
                                    await audio.play()
                                    console.log('Audio playback started, duration:', audio.duration, 'seconds')
                                  } catch (err: any) {
                                    console.error('Failed to play audio:', err)
                                    setPlayingId(null)
                                    setAudioElement(null)
                                    setAudioProgress(0)
                                    alert(`Failed to play audio: ${err.message || 'Unknown error'}`)
                                  }
                                }}
                                color={playingId === recording.id ? 'primary' : 'default'}
                              >
                                {playingId === recording.id ? <Pause /> : <PlayArrow />}
                              </IconButton>
                              {playingId === recording.id && (
                                <CircularProgress
                                  variant="determinate"
                                  value={audioProgress}
                                  size={40}
                                  thickness={4}
                                  sx={{
                                    position: 'absolute',
                                    top: '50%',
                                    left: '50%',
                                    marginTop: '-20px',
                                    marginLeft: '-20px',
                                    color: 'primary.main',
                                    zIndex: 1,
                                  }}
                                />
                              )}
                            </Box>
                          </Tooltip>
                          {!recording.libretime_id && (
                            <Tooltip title="Upload to LibreTime">
                              <IconButton
                                size="small"
                                color="primary"
                                onClick={() => {
                                  if (window.confirm('Upload this recording to LibreTime library?')) {
                                    uploadToLibreTimeMutation.mutate(recording.id)
                                  }
                                }}
                                disabled={uploadToLibreTimeMutation.isPending}
                              >
                                <CloudUpload />
                              </IconButton>
                            </Tooltip>
                          )}
                          <Tooltip title="Delete recording">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => {
                                if (window.confirm(`Are you sure you want to delete "${recording.show_name || `Recording #${recording.id}`}"?`)) {
                                  deleteRecordingMutation.mutate(recording.id)
                                }
                              }}
                              disabled={deleteRecordingMutation.isPending}
                            >
                              <Delete />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">
                No recordings yet. Record a test take to get started. After saving, your recordings will appear here.
              </Alert>
            )}
          </Box>
        </Box>
      ) : (
        <>
          {/* Log selection */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={4}>
          <FormControl fullWidth>
            <InputLabel>Select Log</InputLabel>
            <Select
              value={selectedLogId || ''}
              onChange={(e) => {
                setSelectedLogId(e.target.value as number)
                setSelectedBreakId(null)
              }}
              label="Select Log"
            >
              {logsData?.logs?.map((log: any) => (
                <MenuItem key={log.id} value={log.id}>
                  {new Date(log.date).toLocaleDateString()}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>

        {slotsData && (
          <Grid item xs={12} md={4}>
            <FormControl fullWidth>
              <InputLabel>Select Break</InputLabel>
              <Select
                value={selectedBreakId || ''}
                onChange={(e) => {
                  const breakId = e.target.value as number
                  setSelectedBreakId(breakId)
                  // Clear takes cache when switching breaks
                  queryClient.setQueryData(['voice-takes', selectedBreakId], [])
                  queryClient.setQueryData(['voice-takes', breakId], undefined)
                  const slot = slotsData.slots.find((s: any) => s.id === breakId)
                  if (slot) {
                    setSelectedHour(slot.hour)
                  }
                }}
                label="Select Break"
                disabled={!slotsData}
              >
                {slotsData?.slots?.map((slot: any) => (
                  <MenuItem key={slot.id} value={slot.id}>
                    Hour {slot.hour}:00 - Break {slot.break_position || 'N/A'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        )}
      </Grid>

      {slotsLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </Box>
      )}

      {uploadMutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to upload recording: {uploadMutation.error?.message}
        </Alert>
      )}

      {selectedBreakId && (
        <Grid container spacing={3}>
          {/* Preview panels */}
          <Grid item xs={12}>
            <PreviewPanels breakId={selectedBreakId} logId={selectedLogId} />
          </Grid>

          {/* Timing display */}
          {selectedLogId && selectedHour !== null && (
            <Grid item xs={12} md={4}>
              <TimingDisplay logId={selectedLogId} hour={selectedHour} />
            </Grid>
          )}

          {/* Recording interface */}
          <Grid item xs={12} md={selectedLogId && selectedHour !== null ? 8 : 12}>
            <SharedVoiceRecorder
              context="voice_track"
              breakId={selectedBreakId}
              onUpload={handleUpload}
              onTakeSelect={handleTakeSelect}
              onTakeDelete={handleTakeDelete}
              takes={takesData || []}
            />
          </Grid>
        </Grid>
      )}

      {!selectedLogId && (
        <Alert severity="info">
          Please select a log to begin voice tracking.
        </Alert>
      )}
        </>
      )}

      {(uploadMutation.isError || uploadStandaloneMutation.isError) && (
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to upload recording: {(uploadMutation.error || uploadStandaloneMutation.error)?.message}
        </Alert>
      )}

      {uploadToLibreTimeMutation.isSuccess && (
        <Alert severity="success" sx={{ mt: 2 }}>
          Recording uploaded to LibreTime successfully!
        </Alert>
      )}

      {uploadToLibreTimeMutation.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to upload to LibreTime: {uploadToLibreTimeMutation.error?.message}
        </Alert>
      )}
    </Box>
  )
}

export default VoiceRecorder
