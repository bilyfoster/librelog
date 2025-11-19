import React, { useState } from 'react'
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
  TextField,
  InputAdornment,
  Button,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
} from '@mui/material'
import { Search, Sync } from '@mui/icons-material'
import api from '../../utils/api'
import { syncTracks, getTracksAggregated } from '../../utils/api'
import { TRACK_TYPES, getTrackType, getTrackTypeChipColor, getTrackTypeBackgroundColor } from '../../utils/trackTypes'

const SpotsLibrary: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedType, setSelectedType] = useState<string>('ADV') // Default to ADV since it's more likely to have data

  // Fetch tracks from server-side proxy endpoint (all processing happens on backend)
  const { data: tracksData, isLoading, error, refetch, isError } = useQuery({
    queryKey: ['spots-tracks', selectedType, searchTerm],
    queryFn: async () => {
      try {
        // Use server-side proxy endpoint - backend handles all API calls
        const data = await getTracksAggregated({
          track_type: selectedType,
          limit: 999,
          skip: 0,
          search: searchTerm || undefined,
        })
        const tracks = Array.isArray(data.tracks) ? data.tracks : []
        console.log('[SpotsLibrary] Loaded tracks:', tracks.length, 'Total count:', data.count)
        return tracks
      } catch (err: any) {
        console.error('[SpotsLibrary] API error:', err)
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
  console.log('[SpotsLibrary] Final tracks for display:', tracks.length, 'selectedType:', selectedType)

  const handleSync = async () => {
    try {
      await syncTracks(1000, 0)
      refetch()
    } catch (err) {
      console.error('Sync failed:', err)
    }
  }

  const getTypeColor = (type: string) => {
    return getTrackTypeChipColor(type)
  }

  if (isLoading && !tracksData) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if ((isError || error) && !tracksData) {
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Spots & Promos Library</Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load tracks: {error instanceof Error ? error.message : 'Unknown error'}
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
        <Typography variant="h4">Spots & Promos Library</Typography>
        <Button variant="outlined" color="primary" onClick={handleSync} startIcon={<Sync />}>
          Sync from LibreTime
        </Button>
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
                label={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box
                      sx={{
                        width: 12,
                        height: 12,
                        backgroundColor: type.color,
                        borderRadius: '50%',
                      }}
                    />
                    {type.label}
                  </Box>
                }
                value={type.value}
                sx={{
                  '&.Mui-selected': {
                    color: type.color,
                    fontWeight: 'bold',
                  }
                }}
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
                </TableRow>
              </TableHead>
              <TableBody>
                {tracks.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      <Box sx={{ py: 3 }}>
                        <Typography color="textSecondary" variant="body1" gutterBottom>
                          {searchTerm
                            ? `No ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()} found matching "${searchTerm}"`
                            : `No ${TRACK_TYPES.find(t => t.value === selectedType)?.label.toLowerCase()} found.`
                          }
                        </Typography>
                        {!searchTerm && (
                          <Typography color="textSecondary" variant="body2" sx={{ mt: 1 }}>
                            Try switching to another tab (Advertisements, News, etc.) or click "Sync from LibreTime" to import tracks.
                          </Typography>
                        )}
                      </Box>
                    </TableCell>
                  </TableRow>
                ) : (
                  tracks.map((track: any) => {
                    const trackType = track.type || selectedType
                    const typeInfo = getTrackType(trackType)
                    return (
                      <TableRow 
                        key={track.id}
                        sx={{
                          backgroundColor: typeInfo?.backgroundColor || 'transparent',
                          '&:hover': {
                            backgroundColor: typeInfo ? `${typeInfo.backgroundColor}dd` : undefined,
                          }
                        }}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <Box
                              sx={{
                                width: 4,
                                height: 40,
                                backgroundColor: typeInfo?.color || '#757575',
                                borderRadius: 1,
                              }}
                            />
                            <Typography>{track.title || 'Untitled'}</Typography>
                          </Box>
                        </TableCell>
                        <TableCell>{track.artist || 'Unknown'}</TableCell>
                        <TableCell>
                          <Chip
                            label={trackType}
                            sx={{
                              backgroundColor: typeInfo?.color || '#757575',
                              color: '#fff',
                              fontWeight: 'bold',
                            }}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{track.duration ? `${Math.floor(track.duration / 60)}:${(track.duration % 60).toString().padStart(2, '0')}` : 'N/A'}</TableCell>
                        <TableCell>{track.genre || '-'}</TableCell>
                      </TableRow>
                    )
                  })
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
    </Box>
  )
}

export default SpotsLibrary

