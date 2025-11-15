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
import { syncTracks } from '../../utils/api'

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

const SpotsLibrary: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedType, setSelectedType] = useState<string>('ADV') // Default to ADV since it's more likely to have data

  // Fetch tracks by type
  const { data: tracksData, isLoading, error, refetch, isError } = useQuery({
    queryKey: ['spots-tracks', selectedType, searchTerm],
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
        console.log('[SpotsLibrary] API response:', response.data)
        console.log('[SpotsLibrary] Response type:', typeof response.data, 'Is array:', Array.isArray(response.data))
        const tracks = Array.isArray(response.data) ? response.data : (response.data?.tracks || response.data?.items || [])
        console.log('[SpotsLibrary] Processed tracks:', tracks.length, tracks)
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
    </Box>
  )
}

export default SpotsLibrary

