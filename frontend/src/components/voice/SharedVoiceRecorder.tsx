import React, { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  IconButton,
  LinearProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
} from '@mui/material'
import {
  Mic,
  Stop,
  Pause,
  PlayArrow,
  Delete,
  CheckCircle,
  ContentCut,
} from '@mui/icons-material'
import { useVoiceRecorder } from '../../hooks/useVoiceRecorder'
import WaveformDisplay from './WaveformDisplay'
import AudioTrimmer from './AudioTrimmer'

interface SharedVoiceRecorderProps {
  context: 'voice_track' | 'production'
  breakId?: number
  requestId?: number
  script?: string
  onUpload?: (blob: Blob) => Promise<void>
  onTakeSelect?: (takeId: number) => void
  takes?: any[]
}

const SharedVoiceRecorder: React.FC<SharedVoiceRecorderProps> = ({
  context,
  breakId,
  requestId,
  script,
  onUpload,
  onTakeSelect,
  takes = [],
}) => {
  const {
    isRecording,
    isPaused,
    recordingTime,
    audioBlob,
    selectedTake,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    selectTake,
    reset,
  } = useVoiceRecorder({
    context,
    breakId,
    requestId,
    onRecordingComplete: async (blob) => {
      if (onUpload) {
        await onUpload(blob)
      }
    },
  })

  const [isTrimming, setIsTrimming] = useState(false)
  const [trimmedBlob, setTrimmedBlob] = useState<Blob | null>(null)

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  const handleStart = async () => {
    try {
      await startRecording()
    } catch (error) {
      console.error('Failed to start recording:', error)
      alert('Failed to start recording. Please check microphone permissions.')
    }
  }

  const handleStop = () => {
    stopRecording()
  }

  const handlePause = () => {
    if (isPaused) {
      resumeRecording()
    } else {
      pauseRecording()
    }
  }

  const handleReset = () => {
    reset()
    setTrimmedBlob(null)
    setIsTrimming(false)
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          {context === 'voice_track' ? 'Voice Track Recording' : 'Voice Talent Recording'}
        </Typography>

        {/* Script display for production context */}
        {context === 'production' && script && (
          <Box sx={{ mb: 2, p: 2, bgcolor: 'grey.100', borderRadius: 1 }}>
            <Typography variant="subtitle2" gutterBottom>
              Script:
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
              {script}
            </Typography>
          </Box>
        )}

        {/* Recording controls */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          {!isRecording ? (
            <Button
              variant="contained"
              color="error"
              startIcon={<Mic />}
              onClick={handleStart}
              size="large"
            >
              Start Recording
            </Button>
          ) : (
            <>
              <Button
                variant="contained"
                color="error"
                startIcon={<Stop />}
                onClick={handleStop}
                size="large"
              >
                Stop
              </Button>
              <IconButton
                color="primary"
                onClick={handlePause}
                size="large"
              >
                {isPaused ? <PlayArrow /> : <Pause />}
              </IconButton>
              <Typography variant="h6" sx={{ minWidth: 60 }}>
                {formatTime(recordingTime)}
              </Typography>
            </>
          )}

          {audioBlob && (
            <>
              <Button
                variant="outlined"
                startIcon={<Delete />}
                onClick={handleReset}
                size="small"
              >
                Reset
              </Button>
              {(trimmedBlob || audioBlob) && onUpload && (
                <Button
                  variant="contained"
                  color="primary"
                  onClick={async () => {
                    const blobToUpload = trimmedBlob || audioBlob
                    if (onUpload) {
                      try {
                        await onUpload(blobToUpload)
                        // Clear the trimmed blob after successful save
                        if (trimmedBlob) {
                          setTrimmedBlob(null)
                        }
                      } catch (error) {
                        console.error('Failed to save recording:', error)
                      }
                    }
                  }}
                  size="small"
                >
                  {trimmedBlob ? 'Save Trimmed Recording' : 'Save Recording'}
                </Button>
              )}
            </>
          )}
        </Box>

        {/* Recording indicator */}
        {isRecording && (
          <Box sx={{ mb: 2 }}>
            <LinearProgress />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5 }}>
              Recording in progress...
            </Typography>
          </Box>
        )}

        {/* Waveform display or trimmer */}
        {audioBlob && !isTrimming && (
          <Box sx={{ mb: 2 }}>
            <WaveformDisplay audioBlob={trimmedBlob || audioBlob} />
            <Box sx={{ mt: 2, display: 'flex', gap: 2 }}>
              <Button
                variant="outlined"
                startIcon={<ContentCut />}
                onClick={() => setIsTrimming(true)}
                size="small"
              >
                Trim Audio
              </Button>
              {trimmedBlob && (
                <Button
                  variant="outlined"
                  startIcon={<Delete />}
                  onClick={() => {
                    setTrimmedBlob(null)
                    setIsTrimming(false)
                  }}
                  size="small"
                  color="warning"
                >
                  Reset Trim
                </Button>
              )}
            </Box>
          </Box>
        )}

        {/* Audio trimmer */}
        {audioBlob && isTrimming && (
          <AudioTrimmer
            audioBlob={audioBlob}
            onTrim={(trimmedBlob, startTime, endTime) => {
              setTrimmedBlob(trimmedBlob)
              setIsTrimming(false)
            }}
            onCancel={() => setIsTrimming(false)}
          />
        )}

        {/* Takes list */}
        {takes.length > 0 && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle1" gutterBottom>
              Takes ({takes.length})
            </Typography>
            <List>
              {takes.map((take) => (
                <ListItem
                  key={take.id}
                  secondaryAction={
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      {take.is_final && (
                        <Chip
                          label="Selected"
                          color="success"
                          size="small"
                          icon={<CheckCircle />}
                        />
                      )}
                      {!take.is_final && (
                        <Button
                          size="small"
                          onClick={() => onTakeSelect?.(take.id)}
                        >
                          Select
                        </Button>
                      )}
                    </Box>
                  }
                >
                  <ListItemText
                    primary={`Take ${take.take_number}`}
                    secondary={new Date(take.created_at).toLocaleString()}
                  />
                </ListItem>
              ))}
            </List>
          </Box>
        )}
      </CardContent>
    </Card>
  )
}

export default SharedVoiceRecorder

