import React, { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
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
  Chip,
  IconButton,
  TextField,
  InputAdornment,
  Button,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
} from '@mui/material'
import { Search, Edit, Delete, PlayArrow, Sync } from '@mui/icons-material'
import api from '../../utils/api'
import { syncTracks, getTracksCount } from '../../utils/api'
import TrackEditDialog from '../../components/tracks/TrackEditDialog'
import TrackPlayDialog from '../../components/tracks/TrackPlayDialog'

const TRACK_TYPES = [
  { value: 'MUS', label: 'Music', color: 'primary' },
  { value: 'ADV', label: 'Ads', color: 'secondary' },
  { value: 'NEW', label: 'News', color: 'warning' },
  { value: 'LIN', label: 'Liner', color: 'info' },
  { value: 'INT', label: 'Interview', color: 'success' },
  { value: 'PRO', label: 'Promo', color: 'error' },
  { value: 'SHO', label: 'Show segment', color: 'default' },
  { value: 'IDS', label: 'IDs', color: 'default' },
  { value: 'COM', label: 'Community', color: 'default' },
  { value: 'PSA', label: 'Public Service Announcement', color: 'info' },
]

const LibraryList: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedType, setSelectedType] = useState<string>('MUS')
  const [playingTrackId, setPlayingTrackId] = useState<number | null>(null)
  const [editingTrackId, setEditingTrackId] = useState<number | null>(null)
  const [selectedTrack, setSelectedTrack] = useState<any>(null)

  // Fetch tracks from API by type
  const { data: tracksData, isLoading, error, refetch, isError } = useQuery({
    queryKey: ['tracks', selectedType, searchTerm],
    queryFn: async () => {
      try {
        const response = await api.get('/tracks', {
          params: {
            limit: 1000,
            skip: 0,
            track_type: selectedType,
            ...(searchTerm && { search: searchTerm }),
          },
          timeout: 8000,
        })
        const tracks = Array.isArray(response.data) ? response.data : []
        console.log('Loaded tracks:', tracks.length, tracks.slice(0, 3))
        return tracks
      } catch (err: any) {
        if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
          console.error('Tracks API timeout')
          return []
        }
        throw err
      }
    },
    retry: false,
    staleTime: 30000,
  })

  const tracks = Array.isArray(tracksData) ? tracksData : []
  
  // Check track count separately (faster endpoint)
  const { data: countData } = useQuery({
    queryKey: ['tracks-count', selectedType],
    queryFn: () => getTracksCount(selectedType),
    retry: false,
    staleTime: 60000,
  })
  
  const totalTracksInDB = countData?.count || 0

  // Handle sync
  const handleSync = async () => {
    try {
      await syncTracks(1000, 0)
      refetch()
    } catch (error) {
      console.error('Sync failed:', error)
    }
  }

  // Handle play button
  const handlePlay = (track: any) => {
    setSelectedTrack(track)
    setPlayingTrackId(track.id)
  }

  // Handle edit button
  const handleEdit = (track: any) => {
    setSelectedTrack(track)
    setEditingTrackId(track.id)
  }

  // Handle close dialogs
  const handleClosePlayDialog = () => {
    setSelectedTrack(null)
    setPlayingTrackId(null)
  }

  const handleCloseEditDialog = () => {
    setSelectedTrack(null)
    setEditingTrackId(null)
  }

  const getTypeColor = (type: string) => {
    const typeInfo = TRACK_TYPES.find(t => t.value === type)
    return typeInfo?.color || 'default'
  }

  if (isLoading && !tracksData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if ((isError || error) && !tracksData) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error'
    const isTimeout = errorMessage.includes('timeout')
    
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Audio Library</Typography>
        {totalTracksInDB > 0 && (
          <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
            There are {totalTracksInDB} tracks in the database, but loading them is timing out. 
            The database query may be slow. Try reducing the limit or check backend performance.
          </Alert>
        )}
        <Alert severity={isTimeout ? 'warning' : 'error'} sx={{ mt: totalTracksInDB > 0 ? 0 : 2 }}>
          {isTimeout 
            ? 'The tracks API is taking too long to respond. The backend may be slow or unavailable. You can try syncing tracks or check your connection.'
            : `Failed to load tracks: ${errorMessage}`
          }
        </Alert>
        <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
          <Button onClick={() => refetch()} variant="contained">Retry</Button>
          <Button onClick={handleSync} variant="outlined">Sync from LibreTime</Button>
        </Box>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Audio Library</Typography>
        <Box>
          <Button variant="outlined" color="primary" onClick={handleSync} startIcon={<Sync />} sx={{ mr: 2 }}>
            Sync from LibreTime
          </Button>
          <Button variant="contained" color="primary">
            Upload Track
          </Button>
        </Box>
      </Box>

      <Card>
        <CardContent>
          {/* Type Tabs */}
          <Tabs
            value={selectedType}
            onChange={(e, newValue) => setSelectedType(newValue)}
            sx={{ mb: 2 }}
            variant="scrollable"
            scrollButtons="auto"
          >
            {TRACK_TYPES.map((type) => (
              <Tab
                key={type.value}
                label={type.label}
                value={type.value}
              />
            ))}
          </Tabs>

          {/* Search */}
          <Box mb={2}>
            <TextField
              placeholder={`Search ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()}...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Search />
                  </InputAdornment>
                ),
              }}
              sx={{ width: 300 }}
            />
          </Box>

          {/* Tracks Table */}
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Artist</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Genre</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tracks.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      <Box sx={{ py: 3 }}>
                        <Typography color="textSecondary" variant="body1" gutterBottom>
                          {searchTerm
                            ? `No ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()} found matching "${searchTerm}"`
                            : totalTracksInDB > 0
                            ? `There are ${totalTracksInDB} ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()} in the database, but they failed to load. Try refreshing or check backend logs.`
                            : `No ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()} found.`
                          }
                        </Typography>
                        {!searchTerm && totalTracksInDB === 0 && (
                          <Typography color="textSecondary" variant="body2" sx={{ mt: 1 }}>
                            Try switching to another tab or click "Sync from LibreTime" to import tracks.
                          </Typography>
                        )}
                      </Box>
                    </TableCell>
                  </TableRow>
                ) : (
                  tracks.map((track: any) => (
                    <TableRow key={track.id}>
                      <TableCell>{track.title || 'Untitled'}</TableCell>
                      <TableCell>{track.artist || 'Unknown'}</TableCell>
                      <TableCell>
                        <Chip
                          label={track.type || selectedType}
                          color={getTypeColor(track.type || selectedType) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>{track.duration ? `${Math.floor(track.duration / 60)}:${(track.duration % 60).toString().padStart(2, '0')}` : 'N/A'}</TableCell>
                      <TableCell>{track.genre || '-'}</TableCell>
                      <TableCell>
                        <IconButton 
                          size="small" 
                          onClick={() => handlePlay(track)}
                          color={playingTrackId === track.id ? 'primary' : 'default'}
                          disabled={!track.filepath}
                          title="Play preview"
                        >
                          <PlayArrow />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleEdit(track)} title="Edit">
                          <Edit />
                        </IconButton>
                        <IconButton size="small" title="Delete">
                          <Delete />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>

          {tracks.length > 0 && (
            <Typography variant="body2" color="textSecondary" sx={{ mt: 2 }}>
              Showing {tracks.length} {TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()}
            </Typography>
          )}
        </CardContent>
      </Card>

      {/* Play Dialog */}
      {selectedTrack && playingTrackId === selectedTrack.id && (
        <TrackPlayDialog
          open={true}
          track={selectedTrack}
          onClose={handleClosePlayDialog}
        />
      )}

      {/* Edit Dialog */}
      {selectedTrack && editingTrackId === selectedTrack.id && (
        <TrackEditDialog
          open={true}
          track={selectedTrack}
          onClose={handleCloseEditDialog}
          onUpdate={() => {
            refetch()
          }}
        />
      )}
    </Box>
  )
}

export default LibraryList
