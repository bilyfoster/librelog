import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  TextField,
  InputAdornment,
  Button,
  CircularProgress,
  Alert,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  FormControlLabel,
  Grid,
  Tabs,
  Tab,
} from '@mui/material'
import { Search, Edit, MusicNote, Save, Cancel } from '@mui/icons-material'
import api from '../../utils/api'
import { getTracksAggregated } from '../../utils/api'

interface MusicTrack {
  id: number
  title: string
  artist: string
  album?: string
  genre?: string
  duration: number
  bpm?: number
  daypart_eligible?: number[]
  is_new_release: boolean
  allow_back_to_back: boolean
  last_played?: string
}

const MusicManager: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [editingTrack, setEditingTrack] = useState<MusicTrack | null>(null)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [tabValue, setTabValue] = useState(0)
  const queryClient = useQueryClient()

  // Fetch music tracks
  const { data: tracksData, isLoading, error } = useQuery({
    queryKey: ['music-tracks', searchTerm],
    queryFn: async () => {
      const data = await getTracksAggregated({
        track_type: 'MUS',
        limit: 999,
        skip: 0,
        search: searchTerm || undefined,
      })
      return { tracks: Array.isArray(data.tracks) ? data.tracks : [], count: data.count }
    },
    staleTime: 30000,
  })

  // Fetch dayparts for eligibility selection
  const { data: daypartsData } = useQuery({
    queryKey: ['dayparts'],
    queryFn: async () => {
      const response = await api.get('/dayparts/')
      return response.data || []
    },
  })

  const tracks = tracksData?.tracks || []
  const dayparts = daypartsData || []

  // Update track mutation
  const updateTrackMutation = useMutation({
    mutationFn: async (trackData: Partial<MusicTrack>) => {
      const response = await api.put(`/tracks/${editingTrack?.id}`, trackData)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['music-tracks'] })
      setEditDialogOpen(false)
      setEditingTrack(null)
    },
  })

  const handleEdit = (track: any) => {
    setEditingTrack({
      id: track.id,
      title: track.title,
      artist: track.artist,
      album: track.album,
      genre: track.genre,
      duration: track.duration,
      bpm: track.bpm || undefined,
      daypart_eligible: track.daypart_eligible || [],
      is_new_release: track.is_new_release || false,
      allow_back_to_back: track.allow_back_to_back || false,
      last_played: track.last_played,
    })
    setEditDialogOpen(true)
  }

  const handleSave = () => {
    if (!editingTrack) return
    updateTrackMutation.mutate({
      bpm: editingTrack.bpm,
      daypart_eligible: editingTrack.daypart_eligible,
      is_new_release: editingTrack.is_new_release,
      allow_back_to_back: editingTrack.allow_back_to_back,
    })
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const filteredTracks = tracks.filter((track: any) => {
    if (!searchTerm) return true
    const search = searchTerm.toLowerCase()
    return (
      track.title?.toLowerCase().includes(search) ||
      track.artist?.toLowerCase().includes(search) ||
      track.album?.toLowerCase().includes(search) ||
      track.genre?.toLowerCase().includes(search)
    )
  })

  // Group tracks by filters
  const tracksWithBPM = filteredTracks.filter((t: any) => t.bpm)
  const tracksWithoutBPM = filteredTracks.filter((t: any) => !t.bpm)
  const newReleases = filteredTracks.filter((t: any) => t.is_new_release)
  const backToBackAllowed = filteredTracks.filter((t: any) => t.allow_back_to_back)

  const displayTracks = tabValue === 0 
    ? filteredTracks 
    : tabValue === 1 
    ? tracksWithBPM 
    : tabValue === 2 
    ? tracksWithoutBPM 
    : tabValue === 3 
    ? newReleases 
    : backToBackAllowed

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        <MusicNote sx={{ mr: 1, verticalAlign: 'middle' }} />
        Music Manager
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Manage BPM, daypart eligibility, and artist separation rules for music tracks
      </Typography>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <TextField
            fullWidth
            placeholder="Search by title, artist, album, or genre..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search />
                </InputAdornment>
              ),
            }}
          />
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ mb: 2 }}>
            <Tab label={`All (${filteredTracks.length})`} />
            <Tab label={`With BPM (${tracksWithBPM.length})`} />
            <Tab label={`No BPM (${tracksWithoutBPM.length})`} />
            <Tab label={`New Releases (${newReleases.length})`} />
            <Tab label={`Back-to-Back Allowed (${backToBackAllowed.length})`} />
          </Tabs>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load tracks: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
          )}

          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Title</TableCell>
                    <TableCell>Artist</TableCell>
                    <TableCell>Album</TableCell>
                    <TableCell>Genre</TableCell>
                    <TableCell>Duration</TableCell>
                    <TableCell>BPM</TableCell>
                    <TableCell>Dayparts</TableCell>
                    <TableCell>Flags</TableCell>
                    <TableCell>Last Played</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {displayTracks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={10} align="center">
                        <Typography color="text.secondary">No tracks found</Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    displayTracks.map((track: any) => (
                      <TableRow key={track.id}>
                        <TableCell>{track.title}</TableCell>
                        <TableCell>{track.artist}</TableCell>
                        <TableCell>{track.album || '-'}</TableCell>
                        <TableCell>{track.genre || '-'}</TableCell>
                        <TableCell>{formatTime(track.duration)}</TableCell>
                        <TableCell>
                          {track.bpm ? (
                            <Chip label={track.bpm} size="small" color="primary" />
                          ) : (
                            <Typography variant="body2" color="text.secondary">-</Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          {track.daypart_eligible && track.daypart_eligible.length > 0 ? (
                            <Chip label={`${track.daypart_eligible.length} dayparts`} size="small" />
                          ) : (
                            <Typography variant="body2" color="text.secondary">All</Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', gap: 0.5 }}>
                            {track.is_new_release && (
                              <Chip label="New" size="small" color="success" />
                            )}
                            {track.allow_back_to_back && (
                              <Chip label="B2B" size="small" color="warning" />
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          {track.last_played
                            ? new Date(track.last_played).toLocaleDateString()
                            : 'Never'}
                        </TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleEdit(track)}>
                            <Edit />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Edit Music Track: {editingTrack?.title}</DialogTitle>
        <DialogContent>
          {editingTrack && (
            <Grid container spacing={2} sx={{ mt: 1 }}>
              <Grid item xs={12}>
                <Typography variant="subtitle2" gutterBottom>
                  {editingTrack.artist} - {editingTrack.title}
                </Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="BPM"
                  type="number"
                  value={editingTrack.bpm || ''}
                  onChange={(e) =>
                    setEditingTrack({
                      ...editingTrack,
                      bpm: e.target.value ? parseInt(e.target.value) : undefined,
                    })
                  }
                  inputProps={{ min: 60, max: 200 }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControl fullWidth>
                  <InputLabel>Daypart Eligibility</InputLabel>
                  <Select
                    multiple
                    value={editingTrack.daypart_eligible || []}
                    onChange={(e) =>
                      setEditingTrack({
                        ...editingTrack,
                        daypart_eligible: e.target.value as number[],
                      })
                    }
                    renderValue={(selected) => {
                      if (selected.length === 0) return 'All Dayparts'
                      const selectedDayparts = dayparts
                        .filter((d: any) => selected.includes(d.id))
                        .map((d: any) => d.name)
                      return selectedDayparts.join(', ')
                    }}
                  >
                    {dayparts.map((daypart: any) => (
                      <MenuItem key={daypart.id} value={daypart.id}>
                        {daypart.name} ({daypart.start_hour}:00 - {daypart.end_hour}:00)
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={editingTrack.is_new_release}
                      onChange={(e) =>
                        setEditingTrack({
                          ...editingTrack,
                          is_new_release: e.target.checked,
                        })
                      }
                    />
                  }
                  label="New Release (allows more frequent play)"
                />
              </Grid>
              <Grid item xs={12}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={editingTrack.allow_back_to_back}
                      onChange={(e) =>
                        setEditingTrack({
                          ...editingTrack,
                          allow_back_to_back: e.target.checked,
                        })
                      }
                    />
                  }
                  label="Allow Back-to-Back (allows consecutive plays)"
                />
              </Grid>
            </Grid>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleSave}
            variant="contained"
            disabled={updateTrackMutation.isPending}
            startIcon={updateTrackMutation.isPending ? <CircularProgress size={20} /> : <Save />}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default MusicManager

