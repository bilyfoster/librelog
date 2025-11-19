import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Button,
} from '@mui/material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import SharedVoiceRecorder from '../../components/voice/SharedVoiceRecorder'
import PreviewPanels from '../../components/voice/PreviewPanels'
import TimingDisplay from '../../components/voice/TimingDisplay'
import api from '../../utils/api'

const VoiceRecorder: React.FC = () => {
  const [mode, setMode] = useState<'log' | 'standalone'>('log')
  const [selectedBreakId, setSelectedBreakId] = useState<number | null>(null)
  const [selectedLogId, setSelectedLogId] = useState<number | null>(null)
  const [selectedHour, setSelectedHour] = useState<number | null>(null)
  const queryClient = useQueryClient()

  // Fetch available logs
  const { data: logsData } = useQuery({
    queryKey: ['logs'],
    queryFn: async () => {
      const response = await api.get('/logs')
      return response.data
    },
  })

  // Fetch voice slots for selected log
  const { data: slotsData, isLoading: slotsLoading } = useQuery({
    queryKey: ['voice-slots', selectedLogId],
    queryFn: async () => {
      if (!selectedLogId) return null
      const response = await api.get(`/logs/${selectedLogId}/voice-slots`)
      return response.data
    },
    enabled: !!selectedLogId,
  })

  // Fetch takes for selected break
  const { data: takesData } = useQuery({
    queryKey: ['voice-takes', selectedBreakId],
    queryFn: async () => {
      if (!selectedBreakId) return []
      const response = await api.get(`/voice/breaks/${selectedBreakId}/takes`)
      return response.data
    },
    enabled: !!selectedBreakId,
  })

  // Upload recording mutation (for log-based)
  const uploadMutation = useMutation({
    mutationFn: async (blob: Blob) => {
      if (!selectedBreakId) throw new Error('No break selected')
      
      const formData = new FormData()
      formData.append('file', blob, `recording_${Date.now()}.webm`)
      
      const response = await api.post(
        `/voice/breaks/${selectedBreakId}/record`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      )
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-takes', selectedBreakId] })
    },
  })

  // Upload standalone recording mutation
  const uploadStandaloneMutation = useMutation({
    mutationFn: async (blob: Blob) => {
      const formData = new FormData()
      formData.append('file', blob, `test_recording_${Date.now()}.webm`)
      
      // Save as a test recording (not tied to a break)
      const response = await api.post(
        `/voice/recordings/standalone`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      )
      return response.data
    },
    onSuccess: () => {
      // Could show success message
    },
  })

  // Select take mutation
  const selectTakeMutation = useMutation({
    mutationFn: async (takeId: number) => {
      const response = await api.put(`/voice/takes/${takeId}/select`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-takes', selectedBreakId] })
    },
  })

  const handleUpload = async (blob: Blob) => {
    if (mode === 'standalone') {
      await uploadStandaloneMutation.mutateAsync(blob)
    } else {
      await uploadMutation.mutateAsync(blob)
    }
  }

  const handleTakeSelect = (takeId: number) => {
    selectTakeMutation.mutate(takeId)
  }

  // Auto-select first break when slots load
  useEffect(() => {
    if (slotsData?.slots && slotsData.slots.length > 0 && !selectedBreakId) {
      const firstSlot = slotsData.slots[0]
      setSelectedBreakId(firstSlot.id)
      setSelectedHour(firstSlot.hour)
    }
  }, [slotsData, selectedBreakId])

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Voice Tracking
      </Typography>

      {/* Mode selection */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={mode} onChange={(e, newValue) => setMode(newValue)}>
          <Tab label="Log-Based Recording" value="log" />
          <Tab label="Test Recording" value="standalone" />
        </Tabs>
      </Box>

      {mode === 'standalone' ? (
        <Box>
          <Alert severity="info" sx={{ mb: 3 }}>
            Test recording mode - Record without selecting a log or break. This is useful for testing your setup or recording practice takes.
          </Alert>
          <SharedVoiceRecorder
            context="voice_track"
            onUpload={handleUpload}
            takes={[]}
          />
        </Box>
      ) : (
        <>
          {/* Log selection */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={4}>
          <FormControl fullWidth>
            <InputLabel>Select Log</InputLabel>
            <Select
              value={selectedLogId || ''}
              onChange={(e) => {
                setSelectedLogId(e.target.value as number)
                setSelectedBreakId(null)
              }}
              label="Select Log"
            >
              {logsData?.logs?.map((log: any) => (
                <MenuItem key={log.id} value={log.id}>
                  {new Date(log.date).toLocaleDateString()}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>

        {slotsData && (
          <Grid item xs={12} md={4}>
            <FormControl fullWidth>
              <InputLabel>Select Break</InputLabel>
              <Select
                value={selectedBreakId || ''}
                onChange={(e) => {
                  const breakId = e.target.value as number
                  setSelectedBreakId(breakId)
                  const slot = slotsData.slots.find((s: any) => s.id === breakId)
                  if (slot) {
                    setSelectedHour(slot.hour)
                  }
                }}
                label="Select Break"
                disabled={!slotsData}
              >
                {slotsData?.slots?.map((slot: any) => (
                  <MenuItem key={slot.id} value={slot.id}>
                    Hour {slot.hour}:00 - Break {slot.break_position || 'N/A'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        )}
      </Grid>

      {slotsLoading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </Box>
      )}

      {uploadMutation.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to upload recording: {uploadMutation.error?.message}
        </Alert>
      )}

      {selectedBreakId && (
        <Grid container spacing={3}>
          {/* Preview panels */}
          <Grid item xs={12}>
            <PreviewPanels breakId={selectedBreakId} />
          </Grid>

          {/* Timing display */}
          {selectedLogId && selectedHour !== null && (
            <Grid item xs={12} md={4}>
              <TimingDisplay logId={selectedLogId} hour={selectedHour} />
            </Grid>
          )}

          {/* Recording interface */}
          <Grid item xs={12} md={selectedLogId && selectedHour !== null ? 8 : 12}>
            <SharedVoiceRecorder
              context="voice_track"
              breakId={selectedBreakId}
              onUpload={handleUpload}
              onTakeSelect={handleTakeSelect}
              takes={takesData || []}
            />
          </Grid>
        </Grid>
      )}

      {!selectedLogId && (
        <Alert severity="info">
          Please select a log to begin voice tracking.
        </Alert>
      )}
        </>
      )}

      {(uploadMutation.isError || uploadStandaloneMutation.isError) && (
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to upload recording: {(uploadMutation.error || uploadStandaloneMutation.error)?.message}
        </Alert>
      )}
    </Box>
  )
}

export default VoiceRecorder
