import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Tabs,
  Tab,
  IconButton,
  Tooltip,
} from '@mui/material'
import { Upload, CheckCircle, Schedule, Mic, PlayArrow, Download, Delete, LibraryMusic } from '@mui/icons-material'
import { getVoiceTalentRequests, uploadTake, approveTake } from '../../utils/api'
import SharedVoiceRecorder from '../../components/voice/SharedVoiceRecorder'
import api from '../../utils/api'

const VoiceTalentPortal: React.FC = () => {
  const [tabValue, setTabValue] = useState(0)
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false)
  const [recordDialogOpen, setRecordDialogOpen] = useState(false)
  const [standaloneRecordOpen, setStandaloneRecordOpen] = useState(false)
  const [selectedRequest, setSelectedRequest] = useState<any>(null)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const queryClient = useQueryClient()

  const { data: requests, isLoading, error } = useQuery({
    queryKey: ['voice-talent-requests'],
    queryFn: async () => {
      const data = await getVoiceTalentRequests()
      return Array.isArray(data) ? data : []
    },
  })

  // Fetch user's recordings
  const { data: recordingsData, isLoading: recordingsLoading } = useQuery({
    queryKey: ['production-recordings'],
    queryFn: async () => {
      const response = await api.get('/voice/recordings/production')
      return response.data || []
    },
  })

  const uploadMutation = useMutation({
    mutationFn: async ({ request_id, file }: { request_id: number; file: File }) => {
      return await uploadTake(request_id, file)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-talent-requests'] })
      setUploadDialogOpen(false)
      setSelectedFile(null)
      setSelectedRequest(null)
    },
    onError: (error: any) => {
      console.error('Upload failed:', error)
    },
  })

  const approveMutation = useMutation({
    mutationFn: async ({ request_id, take_number }: { request_id: number; take_number: number }) => {
      return await approveTake(request_id, take_number)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['voice-talent-requests'] })
    },
  })

  const handleUploadClick = (request: any) => {
    setSelectedRequest(request)
    setUploadDialogOpen(true)
  }

  const handleRecordClick = (request: any) => {
    setSelectedRequest(request)
    setRecordDialogOpen(true)
  }

  const handleRecordingUpload = async (blob: Blob) => {
    if (standaloneRecordOpen) {
      // Standalone recording
      const formData = new FormData()
      formData.append('file', blob, `test_recording_${Date.now()}.webm`)
      
      try {
        await api.post('/voice/recordings/standalone', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
        setStandaloneRecordOpen(false)
      } catch (error) {
        console.error('Standalone recording upload failed:', error)
      }
    } else if (selectedRequest) {
      // Request-based recording
      const file = new File([blob], `recording_${Date.now()}.webm`, { type: blob.type })
      
      try {
        await uploadMutation.mutateAsync({
          request_id: selectedRequest.id,
          file: file,
        })
        setRecordDialogOpen(false)
        setSelectedRequest(null)
      } catch (error) {
        console.error('Recording upload failed:', error)
      }
    }
  }

  const handleFileSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0])
    }
  }

  const handleUpload = () => {
    if (selectedRequest && selectedFile) {
      uploadMutation.mutate({
        request_id: selectedRequest.id,
        file: selectedFile,
      })
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'default'
      case 'ASSIGNED':
        return 'info'
      case 'RECORDING':
        return 'warning'
      case 'UPLOADED':
        return 'secondary'
      case 'APPROVED':
        return 'success'
      case 'REJECTED':
        return 'error'
      default:
        return 'default'
    }
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString()
  }

  const deleteRecordingMutation = useMutation({
    mutationFn: async (recordingId: number) => {
      await api.delete(`/voice/recordings/${recordingId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-recordings'] })
    },
  })

  const handleDeleteRecording = (recordingId: number) => {
    if (window.confirm('Are you sure you want to delete this recording?')) {
      deleteRecordingMutation.mutate(recordingId)
    }
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Voice Talent Portal
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
          <Tab label="Assigned Requests" />
          <Tab label="My Recordings" />
        </Tabs>
      </Box>

      {tabValue === 0 && (
        <>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Assigned Voice Work</Typography>
            <Button
              variant="outlined"
              startIcon={<Mic />}
              onClick={() => setStandaloneRecordOpen(true)}
            >
              Test Recording
            </Button>
          </Box>

          <TableContainer component={Paper} sx={{ mt: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Request ID</TableCell>
              <TableCell>Talent Type</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Deadline</TableCell>
              <TableCell>Takes</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {requests && requests.length > 0 ? (
              requests.map((request: any) => (
                <TableRow key={request.id}>
                  <TableCell>#{request.id}</TableCell>
                  <TableCell>{request.talent_type}</TableCell>
                  <TableCell>
                    <Chip
                      label={request.status}
                      color={getStatusColor(request.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{formatDate(request.deadline)}</TableCell>
                  <TableCell>
                    {request.takes && Array.isArray(request.takes)
                      ? `${request.takes.length} take(s)`
                      : '0 takes'}
                  </TableCell>
                  <TableCell>
                    <Button
                      size="small"
                      startIcon={<Upload />}
                      onClick={() => handleUploadClick(request)}
                      disabled={request.status === 'APPROVED'}
                      sx={{ mr: 1 }}
                    >
                      Upload Take
                    </Button>
                    <Button
                      size="small"
                      startIcon={<Mic />}
                      onClick={() => handleRecordClick(request)}
                      disabled={request.status === 'APPROVED'}
                      variant="outlined"
                      color="primary"
                    >
                      Record
                    </Button>
                    {request.takes &&
                      Array.isArray(request.takes) &&
                      request.takes.map((take: any, index: number) => (
                        <Button
                          key={index}
                          size="small"
                          onClick={() =>
                            approveMutation.mutate({
                              request_id: request.id,
                              take_number: take.take_number,
                            })
                          }
                          disabled={take.approved}
                          sx={{ ml: 1 }}
                        >
                          {take.approved ? 'Approved' : 'Approve Take ' + take.take_number}
                        </Button>
                      ))}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No voice talent requests assigned
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
        </>
      )}

      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)}>
        <DialogTitle>Upload Take</DialogTitle>
        <DialogContent>
          {selectedRequest && (
            <Box>
              <Typography variant="body2" color="text.secondary" gutterBottom>
                Script:
              </Typography>
              <Typography variant="body1" sx={{ mb: 2, whiteSpace: 'pre-wrap' }}>
                {selectedRequest.script}
              </Typography>
              {selectedRequest.pronunciation_guides && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    Pronunciation Guides:
                  </Typography>
                  <Typography variant="body1">{selectedRequest.pronunciation_guides}</Typography>
                </Box>
              )}
              <TextField
                type="file"
                inputProps={{ accept: 'audio/*' }}
                onChange={handleFileSelect}
                fullWidth
                sx={{ mt: 2 }}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleUpload}
            variant="contained"
            disabled={!selectedFile || uploadMutation.isPending}
          >
            Upload
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog 
        open={recordDialogOpen} 
        onClose={() => setRecordDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Record Take</DialogTitle>
        <DialogContent>
          {selectedRequest && (
            <Box>
              <SharedVoiceRecorder
                context="production"
                requestId={selectedRequest.id}
                script={selectedRequest.script}
                onUpload={handleRecordingUpload}
                takes={selectedRequest.takes || []}
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRecordDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Standalone Recording Dialog */}
      <Dialog 
        open={standaloneRecordOpen} 
        onClose={() => setStandaloneRecordOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Test Recording</DialogTitle>
        <DialogContent>
          <Alert severity="info" sx={{ mb: 2 }}>
            Record a test take without being assigned to a request. This is useful for testing your setup or practicing.
          </Alert>
          <SharedVoiceRecorder
            context="production"
            onUpload={handleRecordingUpload}
            takes={[]}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setStandaloneRecordOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* My Recordings Tab */}
      {tabValue === 1 && (
        <Box>
          <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">My Recordings</Typography>
            <Button
              variant="outlined"
              startIcon={<Mic />}
              onClick={() => setStandaloneRecordOpen(true)}
            >
              New Test Recording
            </Button>
          </Box>

          {recordingsLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : recordingsData && recordingsData.length > 0 ? (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {recordingsData.map((recording: any) => (
                    <TableRow key={recording.id}>
                      <TableCell>{recording.show_name || `Recording #${recording.id}`}</TableCell>
                      <TableCell>
                        <Chip
                          label={recording.track_metadata?.type === 'standalone_test' ? 'Test' : 'Production'}
                          size="small"
                          color={recording.track_metadata?.type === 'standalone_test' ? 'default' : 'primary'}
                        />
                      </TableCell>
                      <TableCell>
                        {recording.created_at
                          ? new Date(recording.created_at).toLocaleString()
                          : 'N/A'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={recording.status || 'DRAFT'}
                          size="small"
                          color={recording.status === 'APPROVED' ? 'success' : 'default'}
                        />
                      </TableCell>
                      <TableCell>
                        <Tooltip title="Play">
                          <IconButton
                            size="small"
                            onClick={() => {
                              const audio = new Audio(recording.file_url)
                              audio.play()
                            }}
                          >
                            <PlayArrow />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Download">
                          <IconButton
                            size="small"
                            onClick={() => {
                              window.open(recording.file_url, '_blank')
                            }}
                          >
                            <Download />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDeleteRecording(recording.id)}
                          >
                            <Delete />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info">
              No recordings yet. Create a test recording to get started.
            </Alert>
          )}
        </Box>
      )}
    </Box>
  )
}

export default VoiceTalentPortal

