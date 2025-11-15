import React, { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  MenuItem,
  Alert,
  CircularProgress,
} from '@mui/material'
import { updateTrack } from '../../utils/api'

interface TrackEditDialogProps {
  open: boolean
  track: any
  onClose: () => void
  onUpdate?: () => void
}

const TRACK_TYPES = [
  { value: 'MUS', label: 'Music' },
  { value: 'ADV', label: 'Ads' },
  { value: 'NEW', label: 'News' },
  { value: 'LIN', label: 'Liner' },
  { value: 'INT', label: 'Interview' },
  { value: 'PRO', label: 'Promo' },
  { value: 'SHO', label: 'Show segment' },
  { value: 'IDS', label: 'IDs' },
  { value: 'COM', label: 'Community' },
  { value: 'PSA', label: 'Public Service Announcement' },
]

const TrackEditDialog: React.FC<TrackEditDialogProps> = ({
  open,
  track,
  onClose,
  onUpdate,
}) => {
  const [formData, setFormData] = useState({
    title: '',
    artist: '',
    genre: '',
    type: 'MUS',
  })
  const queryClient = useQueryClient()

  useEffect(() => {
    if (track) {
      setFormData({
        title: track.title || '',
        artist: track.artist || '',
        genre: track.genre || '',
        type: track.type || 'MUS',
      })
    }
  }, [track])

  const updateMutation = useMutation({
    mutationFn: async (updates: any) => {
      await updateTrack(track.id, updates)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tracks'] })
      onClose()
      if (onUpdate) {
        onUpdate()
      }
    },
  })

  const handleSave = () => {
    const updates: any = {
      title: formData.title,
      artist: formData.artist || undefined,
      genre: formData.genre || undefined,
      type: formData.type,
    }
    updateMutation.mutate(updates)
  }

  if (!track) return null

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Edit Track</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Title"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            fullWidth
            required
          />
          <TextField
            label="Artist"
            value={formData.artist}
            onChange={(e) => setFormData({ ...formData, artist: e.target.value })}
            fullWidth
          />
          <TextField
            label="Genre"
            value={formData.genre}
            onChange={(e) => setFormData({ ...formData, genre: e.target.value })}
            fullWidth
          />
          <TextField
            label="Type"
            value={formData.type}
            onChange={(e) => setFormData({ ...formData, type: e.target.value })}
            select
            fullWidth
          >
            {TRACK_TYPES.map((type) => (
              <MenuItem key={type.value} value={type.value}>
                {type.label}
              </MenuItem>
            ))}
          </TextField>
          {updateMutation.isError && (
            <Alert severity="error">
              {updateMutation.error instanceof Error
                ? updateMutation.error.message
                : 'Failed to update track'}
            </Alert>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={updateMutation.isPending}>
          Cancel
        </Button>
        <Button
          onClick={handleSave}
          variant="contained"
          disabled={updateMutation.isPending || !formData.title}
        >
          {updateMutation.isPending ? <CircularProgress size={20} /> : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

export default TrackEditDialog



