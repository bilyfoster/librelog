import React from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Chip,
} from '@mui/material'
import AudioPlayer from '../audio/AudioPlayer'
import { getTrackType } from '../../utils/trackTypes'

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
  if (!track || !open) return null

  const previewUrl = `/api/tracks/${track.id}/preview`

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="md" 
      fullWidth
      disableEnforceFocus
      disableAutoFocus
      disableScrollLock
      hideBackdrop={false}
      disablePortal={false}
    >
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
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Type:
              </Typography>
              {track.type && (
                <Chip
                  label={track.type}
                  size="small"
                  sx={{
                    backgroundColor: getTrackType(track.type)?.color || '#757575',
                    color: '#fff',
                    fontWeight: 'bold',
                  }}
                />
              )}
            </Box>
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

