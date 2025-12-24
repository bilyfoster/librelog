/**
 * MultiCutManager - Component for managing multiple cuts per copy
 */

import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Chip,
  Switch,
  FormControlLabel,
  Slider,
  Tooltip,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  History as HistoryIcon,
  Upload as UploadIcon,
} from '@mui/icons-material'
import api from '../../utils/api'

interface AudioCut {
  id?: string
  cut_id: string
  cut_name?: string
  version: number
  rotation_weight: number
  active: boolean
  expires_at?: string
  audio_file_url?: string
}

interface MultiCutManagerProps {
  copyId: number
  onClose?: () => void
}

const MultiCutManager: React.FC<MultiCutManagerProps> = ({ copyId, onClose }) => {
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false)
  const [versionDialogOpen, setVersionDialogOpen] = useState(false)
  const [selectedCut, setSelectedCut] = useState<AudioCut | null>(null)
  const [newCutId, setNewCutId] = useState('')
  const [uploadFile, setUploadFile] = useState<File | null>(null)

  const queryClient = useQueryClient()

  // Fetch cuts for this copy
  const { data: cuts, isLoading } = useQuery({
    queryKey: ['audio-cuts', copyId],
    queryFn: async () => {
      const response = await api.get('/audio-cuts', {
        params: { copy_id: copyId }
      })
      return response.data
    }
  })

  // Upload new cut
  const uploadMutation = useMutation({
    mutationFn: async (formData: FormData) => {
      const response = await api.post('/audio-cuts/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['audio-cuts', copyId] })
      setUploadDialogOpen(false)
      setUploadFile(null)
      setNewCutId('')
    }
  })

  // Update cut
  const updateMutation = useMutation({
    mutationFn: async ({ cutId, updates }: { cutId: number; updates: any }) => {
      const response = await api.put(`/audio-cuts/${cutId}`, updates)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['audio-cuts', copyId] })
    }
  })

  // Delete cut
  const deleteMutation = useMutation({
    mutationFn: async (cutId: number) => {
      await api.delete(`/audio-cuts/${cutId}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['audio-cuts', copyId] })
    }
  })

  const handleUpload = () => {
    if (!uploadFile || !newCutId) return

    const formData = new FormData()
    formData.append('file', uploadFile)
    formData.append('copy_id', copyId.toString())
    formData.append('cut_id', newCutId)

    uploadMutation.mutate(formData)
  }

  const handleToggleActive = (cut: AudioCut) => {
    updateMutation.mutate({
      cutId: cut.id,
      updates: { active: !cut.active }
    })
  }

  const handleWeightChange = (cut: AudioCut, weight: number) => {
    updateMutation.mutate({
      cutId: cut.id,
      updates: { rotation_weight: weight }
    })
  }

  const handleViewVersions = (cut: AudioCut) => {
    setSelectedCut(cut)
    setVersionDialogOpen(true)
  }

  if (isLoading) {
    return <CircularProgress />
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <Typography variant="h6">Multi-Cut Manager</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setUploadDialogOpen(true)}
        >
          Add Cut
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Cut ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Rotation Weight</TableCell>
              <TableCell>Active</TableCell>
              <TableCell>Expires</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {cuts?.map((cut: AudioCut) => (
              <TableRow key={cut.id}>
                <TableCell>{cut.cut_id}</TableCell>
                <TableCell>{cut.cut_name || '-'}</TableCell>
                <TableCell>{cut.version}</TableCell>
                <TableCell>
                  <Slider
                    value={cut.rotation_weight}
                    min={0}
                    max={2}
                    step={0.1}
                    onChange={(_, value) => handleWeightChange(cut, value as number)}
                    sx={{ width: 100 }}
                  />
                  <Typography variant="caption">{cut.rotation_weight.toFixed(1)}</Typography>
                </TableCell>
                <TableCell>
                  <Switch
                    checked={cut.active}
                    onChange={() => handleToggleActive(cut)}
                  />
                </TableCell>
                <TableCell>
                  {cut.expires_at ? new Date(cut.expires_at).toLocaleDateString() : '-'}
                </TableCell>
                <TableCell>
                  <Tooltip title="View Versions">
                    <IconButton size="small" onClick={() => handleViewVersions(cut)}>
                      <HistoryIcon />
                    </IconButton>
                  </Tooltip>
                  <IconButton
                    size="small"
                    color="error"
                    onClick={() => deleteMutation.mutate(cut.id)}
                  >
                    <DeleteIcon />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Upload Dialog */}
      <Dialog open={uploadDialogOpen} onClose={() => setUploadDialogOpen(false)}>
        <DialogTitle>Upload New Cut</DialogTitle>
        <DialogContent>
          <TextField
            label="Cut ID (A, B, C, etc.)"
            value={newCutId}
            onChange={(e) => setNewCutId(e.target.value)}
            fullWidth
            margin="normal"
          />
          <Button
            variant="outlined"
            component="label"
            startIcon={<UploadIcon />}
            fullWidth
            sx={{ mt: 2 }}
          >
            Select Audio File
            <input
              type="file"
              hidden
              accept="audio/*"
              onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
            />
          </Button>
          {uploadFile && (
            <Typography variant="body2" sx={{ mt: 1 }}>
              Selected: {uploadFile.name}
            </Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleUpload}
            variant="contained"
            disabled={!uploadFile || !newCutId || uploadMutation.isPending}
          >
            {uploadMutation.isPending ? <CircularProgress size={20} /> : 'Upload'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Version History Dialog */}
      <Dialog
        open={versionDialogOpen}
        onClose={() => setVersionDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Version History - {selectedCut?.cut_id}</DialogTitle>
        <DialogContent>
          <Typography>Version history will be displayed here</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setVersionDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default MultiCutManager

