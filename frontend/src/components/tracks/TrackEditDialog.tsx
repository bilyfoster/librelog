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
  Chip,
} from '@mui/material'
import { updateTrack } from '../../utils/api'
import { TRACK_TYPES, getTrackType } from '../../utils/trackTypes'

interface TrackEditDialogProps {
  open: boolean
  track: any
  onClose: () => void
  onUpdate?: () => void
}

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
            InputProps={{
              startAdornment: formData.type ? (
                <Box sx={{ mr: 1 }}>
                  <Chip
                    label={formData.type}
                    size="small"
                    sx={{
                      backgroundColor: getTrackType(formData.type)?.color || '#757575',
                      color: '#fff',
                      fontWeight: 'bold',
                      height: 24,
                    }}
                  />
                </Box>
              ) : undefined,
            }}
          >
            {TRACK_TYPES.map((type) => (
              <MenuItem 
                key={type.value} 
                value={type.value}
                sx={{
                  '&::before': {
                    content: '""',
                    display: 'inline-block',
                    width: 12,
                    height: 12,
                    backgroundColor: type.color,
                    borderRadius: '50%',
                    mr: 1,
                  }
                }}
              >
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




