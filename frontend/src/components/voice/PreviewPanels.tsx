import React, { useEffect, useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  CircularProgress,
  Alert,
} from '@mui/material'
import WaveformDisplay from './WaveformDisplay'
import api from '../../utils/api'
import { getVoiceSlotContext } from '../../utils/api'

interface PreviewPanelsProps {
  breakId: number
  logId?: number | null
}

interface PreviewData {
  slot_id?: string
  log_id?: string
  hour: number
  break_position: string
  ramp_time: number | null
  previous_track: {
    id?: string
    title: string
    artist?: string
    duration?: number
    type?: string
    libretime_id?: string
  } | null
  next_track: {
    id?: string
    title: string
    artist?: string
    duration?: number
    type?: string
    libretime_id?: string
  } | null
  previous_element?: any
  next_element?: any
}

const PreviewPanels: React.FC<PreviewPanelsProps> = ({ breakId, logId }) => {
  const [previewData, setPreviewData] = useState<PreviewData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchPreview = async () => {
      try {
        setLoading(true)
        setError(null)
        
        // Try new context endpoint first if logId is available
        if (logId) {
          try {
            const response = await getVoiceSlotContext(logId, breakId)
            setPreviewData(response)
            return
          } catch (err: any) {
            // Fall back to old endpoint if new one fails
            console.warn('New context endpoint failed, trying old endpoint:', err)
          }
        }
        
        // Fallback to old endpoint
        const response = await api.get(`/voice/breaks/${breakId}/preview`)
        setPreviewData(response.data)
      } catch (err: any) {
        setError(err.response?.data?.detail || 'Failed to load preview')
        console.error('Preview fetch error:', err)
      } finally {
        setLoading(false)
      }
    }

    if (breakId) {
      fetchPreview()
    }
  }, [breakId, logId])

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ p: 2 }}>
        <Typography color="error">{error}</Typography>
      </Box>
    )
  }

  if (!previewData) {
    return null
  }

  return (
    <Grid container spacing={2}>
      {/* Previous track */}
      {previewData.previous_track && (
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Previous Track (Outro)
              </Typography>
              <Typography variant="h6" gutterBottom>
                {previewData.previous_track.title || 'Unknown'}
              </Typography>
              {previewData.previous_track.artist && (
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {previewData.previous_track.artist}
                </Typography>
              )}
              {previewData.previous_track.duration && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  Duration: {Math.floor(previewData.previous_track.duration / 60)}:{(previewData.previous_track.duration % 60).toString().padStart(2, '0')}
                </Typography>
              )}
              {previewData.previous_track.id && (
                <Box sx={{ mt: 2 }}>
                  <WaveformDisplay
                    audioUrl={`/api/tracks/${previewData.previous_track.id}/preview`}
                    height={150}
                  />
                </Box>
              )}
              {previewData.previous_element && (
                <Box sx={{ mt: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    Type: {previewData.previous_element.type || previewData.previous_track.type || 'N/A'}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      )}

      {/* Next track */}
      {previewData.next_track && (
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Next Track (Intro)
              </Typography>
              <Typography variant="h6" gutterBottom>
                {previewData.next_track.title || 'Unknown'}
              </Typography>
              {previewData.next_track.artist && (
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  {previewData.next_track.artist}
                </Typography>
              )}
              {previewData.next_track.duration && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
                  Duration: {Math.floor(previewData.next_track.duration / 60)}:{(previewData.next_track.duration % 60).toString().padStart(2, '0')}
                </Typography>
              )}
              {previewData.ramp_time && (
                <Typography variant="caption" color="primary" sx={{ display: 'block', mb: 1 }}>
                  Ramp time: {previewData.ramp_time.toFixed(1)}s
                </Typography>
              )}
              {previewData.next_track.id && (
                <Box sx={{ mt: 2 }}>
                  <WaveformDisplay
                    audioUrl={`/api/tracks/${previewData.next_track.id}/preview`}
                    height={150}
                  />
                </Box>
              )}
              {previewData.next_element && (
                <Box sx={{ mt: 1 }}>
                  <Typography variant="caption" color="text.secondary">
                    Type: {previewData.next_element.type || previewData.next_track.type || 'N/A'}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      )}

      {/* Show message if no context available */}
      {!previewData.previous_track && !previewData.next_track && (
        <Grid item xs={12}>
          <Alert severity="info">
            No context information available for this break. The tracks before and after will be determined when the log is finalized.
          </Alert>
        </Grid>
      )}

      {/* Ramp timer info */}
      {previewData.ramp_time && (
        <Grid item xs={12}>
          <Card sx={{ bgcolor: 'info.light', color: 'info.contrastText' }}>
            <CardContent>
              <Typography variant="subtitle1">
                Talk Window: {previewData.ramp_time.toFixed(1)} seconds
              </Typography>
              <Typography variant="body2">
                Record your voice track within this time window before the next song starts.
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      )}
    </Grid>
  )
}

export default PreviewPanels

