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
} from '@mui/material'
import { Upload, CheckCircle, Schedule } from '@mui/icons-material'
import { getVoiceTalentRequests, uploadTake, approveTake } from '../../utils/api'

const VoiceTalentPortal: React.FC = () => {
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false)
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

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box p={3}>
        <Alert severity="error">Failed to load voice talent requests</Alert>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Voice Talent Portal
      </Typography>

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
                    >
                      Upload Take
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
    </Box>
  )
}

export default VoiceTalentPortal

