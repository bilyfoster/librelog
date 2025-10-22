import React from 'react'
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
} from '@mui/material'
import { Search, Edit, Delete, PlayArrow } from '@mui/icons-material'

const LibraryList: React.FC = () => {
  // Mock data
  const tracks = [
    { id: 1, title: 'Morning Show Intro', artist: 'GayPHX', type: 'LIN', duration: 30 },
    { id: 2, title: 'Local Business Ad', artist: 'Advertiser', type: 'ADV', duration: 60 },
    { id: 3, title: 'Community PSA', artist: 'Non-Profit', type: 'PSA', duration: 45 },
  ]

  const getTypeColor = (type: string) => {
    const colors: { [key: string]: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' } = {
      MUS: 'primary',
      ADV: 'error',
      PSA: 'info',
      LIN: 'success',
      INT: 'warning',
      PRO: 'secondary',
      BED: 'default',
    }
    return colors[type] || 'default'
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Music Library</Typography>
        <Button variant="contained" color="primary">
          Upload Track
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Box mb={2}>
            <TextField
              placeholder="Search tracks..."
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

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Artist</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Duration</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tracks.map((track) => (
                  <TableRow key={track.id}>
                    <TableCell>{track.title}</TableCell>
                    <TableCell>{track.artist}</TableCell>
                    <TableCell>
                      <Chip
                        label={track.type}
                        color={getTypeColor(track.type)}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{track.duration}s</TableCell>
                    <TableCell>
                      <IconButton size="small">
                        <PlayArrow />
                      </IconButton>
                      <IconButton size="small">
                        <Edit />
                      </IconButton>
                      <IconButton size="small">
                        <Delete />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  )
}

export default LibraryList
