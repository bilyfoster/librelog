import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
} from '@mui/material'
import AudioPlayer from '../audio/AudioPlayer'

interface TrackPlayDialogProps {
  open: boolean
  track: any
  onClose: () => void
}

const TrackPlayDialog: React.FC<TrackPlayDialogProps> = ({
  open,
  track,
  onClose,
}) => {
  if (!track) return null

  const previewUrl = `/api/tracks/${track.id}/preview`

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        {track.title || 'Untitled'} {track.artist ? `- ${track.artist}` : ''}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ mt: 2 }}>
          <AudioPlayer
            src={previewUrl}
            title="Track Preview"
            onError={(error) => {
              console.error('Audio playback error:', error)
            }}
          />
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary">
              Type: {track.type || 'N/A'}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Genre: {track.genre || 'N/A'}
            </Typography>
            {track.duration && (
              <Typography variant="body2" color="text.secondary">
                Duration: {Math.floor(track.duration / 60)}:
                {(track.duration % 60).toString().padStart(2, '0')}
              </Typography>
            )}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
      </DialogActions>
    </Dialog>
  )
}

export default TrackPlayDialog

