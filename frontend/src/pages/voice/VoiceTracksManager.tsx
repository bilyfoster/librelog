import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Tooltip,
  Chip,
  Button,
  TextField,
  InputAdornment,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
} from '@mui/material'
import {
  PlayArrow,
  Pause,
  CloudUpload,
  CheckCircle,
  Delete,
  Search,
  FilterList,
  Refresh,
} from '@mui/icons-material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import api from '../../utils/api'

interface VoiceTrack {
  id: number
  show_name: string
  file_url: string
  scheduled_time?: string
  created_at: string
  status: string
  libretime_id?: string
  break_id?: number
  track_metadata?: any
}

const VoiceTracksManager: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [libretimeFilter, setLibretimeFilter] = useState<string>('all')
  const [playingId, setPlayingId] = useState<number | null>(null)
  const [audioElement, setAudioElement] = useState<HTMLAudioElement | null>(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedTrack, setSelectedTrack] = useState<VoiceTrack | null>(null)
  const queryClient = useQueryClient()

  // Fetch all voice tracks (with high limit to get all recordings)
  const { data: voiceTracksData, isLoading, error, refetch } = useQuery({
    queryKey: ['voice-tracks', 'all'],
    queryFn: async () => {
      try {
        console.log('VoiceTracksManager: Making API request to /voice')
        console.log('VoiceTracksManager: API baseURL:', api.defaults.baseURL)
        const response = await api.get('/voice', {
          params: {
            limit: 1000, // Get up to 1000 recordings
            skip: 0
          },
          // Ensure we get JSON response
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          }
        })
        console.log('VoiceTracksManager: API response status:', response.status)
        console.log('VoiceTracksManager: API response headers:', response.headers)
        console.log('VoiceTracksManager: API response data type:', typeof response.data)
        console.log('VoiceTracksManager: API response data:', response.data)
        
        // Check if response is HTML (error case)
        if (typeof response.data === 'string' && response.data.trim().startsWith('<!DOCTYPE')) {
          console.error('VoiceTracksManager: Received HTML instead of JSON! This indicates a routing/proxy issue.')
          return []
        }
        
        // Ensure we always return an array
        const data = response.data
        console.log('VoiceTracksManager: Is array?', Array.isArray(data))
        console.log('VoiceTracksManager: Data length:', Array.isArray(data) ? data.length : 'N/A')
        return Array.isArray(data) ? data : []
      } catch (err: any) {
        console.error('VoiceTracksManager: API request failed:', err)
        console.error('VoiceTracksManager: Error response:', err.response)
        return []
      }
    },
  })

  // Ensure voiceTracks is always an array
  const voiceTracks: VoiceTrack[] = Array.isArray(voiceTracksData) ? voiceTracksData : []

  // Upload to LibreTime mutation
  const uploadToLibreTimeMutation = useMutation({
    mutationFn: async (trackId: number) => {
      const response = await api.post(`/voice/${trackId}/upload-to-libretime`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-tracks'] })
    },
  })

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: async (trackId: number) => {
      const response = await api.delete(`/voice/${trackId}`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-tracks'] })
      setDeleteDialogOpen(false)
      setSelectedTrack(null)
    },
  })

  // Filter tracks
  const filteredTracks = voiceTracks.filter((track) => {
    const matchesSearch =
      searchTerm === '' ||
      track.show_name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      track.id.toString().includes(searchTerm) ||
      track.libretime_id?.toLowerCase().includes(searchTerm.toLowerCase())

    const matchesStatus = statusFilter === 'all' || track.status === statusFilter

    const matchesLibretime =
      libretimeFilter === 'all' ||
      (libretimeFilter === 'uploaded' && track.libretime_id) ||
      (libretimeFilter === 'not_uploaded' && !track.libretime_id)

    return matchesSearch && matchesStatus && matchesLibretime
  })
  
  // Debug logging
  useEffect(() => {
    console.log('VoiceTracksManager: voiceTracksData:', voiceTracksData)
    console.log('VoiceTracksManager: voiceTracks:', voiceTracks)
    console.log('VoiceTracksManager: filteredTracks count:', filteredTracks.length)
    console.log('VoiceTracksManager: isLoading:', isLoading)
    console.log('VoiceTracksManager: error:', error)
    if (filteredTracks.length > 0) {
      console.log('VoiceTracksManager: First track sample:', filteredTracks[0])
    }
  }, [voiceTracksData, voiceTracks, filteredTracks, isLoading, error])

  const handlePlay = (track: VoiceTrack) => {
    if (playingId === track.id && audioElement) {
      audioElement.pause()
      setPlayingId(null)
      setAudioElement(null)
    } else {
      if (audioElement) {
        audioElement.pause()
      }
      // Ensure file_url is properly formatted
      const fileUrl = track.file_url?.startsWith('/api/') 
        ? track.file_url 
        : `/api${track.file_url}`
      const audio = new Audio(fileUrl)
      audio.play().catch(err => {
        console.error('Failed to play audio:', err)
        alert('Failed to play audio. Please check the file URL.')
      })
      audio.onended = () => {
        setPlayingId(null)
        setAudioElement(null)
      }
      setPlayingId(track.id)
      setAudioElement(audio)
    }
  }

  const handleDeleteClick = (track: VoiceTrack) => {
    setSelectedTrack(track)
    setDeleteDialogOpen(true)
  }

  const handleDeleteConfirm = () => {
    if (selectedTrack) {
      deleteMutation.mutate(selectedTrack.id)
    }
  }

  const handleUploadToLibreTime = (track: VoiceTrack) => {
    if (window.confirm(`Upload "${track.show_name}" to LibreTime library?`)) {
      uploadToLibreTimeMutation.mutate(track.id)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'READY':
        return 'success'
      case 'APPROVED':
        return 'info'
      case 'AIR':
        return 'warning'
      default:
        return 'default'
    }
  }

  if (isLoading) {
    return (
      <Box p={3} display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box p={3}>
        <Alert severity="error">
          Failed to load voice tracks. Please try again.
        </Alert>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Voice Tracks Management</Typography>
        <Button
          startIcon={<Refresh />}
          onClick={() => refetch()}
          variant="outlined"
        >
          Refresh
        </Button>
      </Box>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            placeholder="Search by name, ID, or LibreTime ID..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            size="small"
            sx={{ minWidth: 300 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              label="Status"
            >
              <MenuItem value="all">All Status</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="APPROVED">Approved</MenuItem>
              <MenuItem value="READY">Ready</MenuItem>
              <MenuItem value="AIR">On Air</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>LibreTime</InputLabel>
            <Select
              value={libretimeFilter}
              onChange={(e) => setLibretimeFilter(e.target.value)}
              label="LibreTime"
            >
              <MenuItem value="all">All</MenuItem>
              <MenuItem value="uploaded">Uploaded</MenuItem>
              <MenuItem value="not_uploaded">Not Uploaded</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Paper>

      {/* Success/Error messages */}
      {uploadToLibreTimeMutation.isSuccess && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Recording uploaded to LibreTime successfully!
        </Alert>
      )}

      {uploadToLibreTimeMutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to upload to LibreTime: {uploadToLibreTimeMutation.error?.message}
        </Alert>
      )}

      {/* Tracks table */}
      <TableContainer component={Paper}>
        <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Name</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Scheduled</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>LibreTime ID</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
          <TableBody>
            {filteredTracks.length > 0 ? (
              filteredTracks.map((track) => (
                <TableRow key={track.id} hover>
                  <TableCell>{track.id}</TableCell>
                  <TableCell>{track.show_name || `Recording #${track.id}`}</TableCell>
                  <TableCell>
                    {track.created_at
                      ? new Date(track.created_at).toLocaleString()
                      : 'N/A'}
                  </TableCell>
                  <TableCell>
                    {track.track_metadata?.duration 
                      ? `${(track.track_metadata.duration).toFixed(1)}s`
                      : 'N/A'}
                  </TableCell>
                  <TableCell>
                    {track.scheduled_time
                      ? new Date(track.scheduled_time).toLocaleString()
                      : 'N/A'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={track.status || 'DRAFT'}
                      size="small"
                      color={getStatusColor(track.status) as any}
                    />
                  </TableCell>
                  <TableCell>
                    {track.libretime_id ? (
                      <Chip
                        label={track.libretime_id}
                        size="small"
                        color="success"
                        icon={<CheckCircle />}
                      />
                    ) : (
                      <Chip label="Not uploaded" size="small" variant="outlined" />
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title={playingId === track.id ? 'Pause' : 'Play'}>
                      <IconButton
                        size="small"
                        onClick={() => handlePlay(track)}
                        color={playingId === track.id ? 'primary' : 'default'}
                      >
                        {playingId === track.id ? <Pause /> : <PlayArrow />}
                      </IconButton>
                    </Tooltip>
                    {!track.libretime_id && (
                      <Tooltip title="Upload to LibreTime">
                        <IconButton
                          size="small"
                          color="primary"
                          onClick={() => handleUploadToLibreTime(track)}
                          disabled={uploadToLibreTimeMutation.isPending}
                        >
                          <CloudUpload />
                        </IconButton>
                      </Tooltip>
                    )}
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDeleteClick(track)}
                        disabled={deleteMutation.isPending}
                      >
                        <Delete />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    {voiceTracks.length === 0
                      ? 'No voice tracks found. Record some tracks to get started.'
                      : 'No tracks match your filters.'}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Delete confirmation dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Delete Voice Track</DialogTitle>
        <DialogContent>
          <Typography>
            Are you sure you want to delete "{selectedTrack?.show_name || `Recording #${selectedTrack?.id}`}"?
            This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleDeleteConfirm}
            color="error"
            variant="contained"
            disabled={deleteMutation.isPending}
          >
            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default VoiceTracksManager

