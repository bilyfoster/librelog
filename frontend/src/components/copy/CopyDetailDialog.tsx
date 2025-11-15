import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography,
  Box,
  Chip,
  Tabs,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Switch,
  FormControlLabel,
  Alert,
  CircularProgress,
  IconButton,
} from '@mui/material'
import {
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  PlayArrow as PlayArrowIcon,
} from '@mui/icons-material'
import {
  getCopyById,
  updateCopy,
  getCopyAssignments,
} from '../../utils/api'
import api from '../../utils/api'
import AudioPlayer from '../audio/AudioPlayer'

interface CopyDetailDialogProps {
  open: boolean
  copyId: number
  onClose: () => void
  onUpdate?: () => void
}

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`copy-tabpanel-${index}`}
      aria-labelledby={`copy-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const CopyDetailDialog: React.FC<CopyDetailDialogProps> = ({
  open,
  copyId,
  onClose,
  onUpdate,
}) => {
  const [editing, setEditing] = useState(false)
  const [tabValue, setTabValue] = useState(0)
  const [formData, setFormData] = useState({
    title: '',
    script_text: '',
    expires_at: '',
    active: true,
  })
  const queryClient = useQueryClient()

  const { data: copy, isLoading } = useQuery({
    queryKey: ['copy', copyId],
    queryFn: async () => {
      const data = await getCopyById(copyId)
      return data
    },
    enabled: open && copyId > 0,
  })

  const { data: assignments } = useQuery({
    queryKey: ['copy-assignments', copyId],
    queryFn: async () => {
      const data = await getCopyAssignments({ copy_id: copyId, limit: 1000 })
      return data
    },
    enabled: open && copyId > 0 && tabValue === 2,
  })

  const { data: orders } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const response = await api.get('/orders', { params: { limit: 100 } })
      return response.data.orders || response.data || []
    },
  })

  const { data: advertisers } = useQuery({
    queryKey: ['advertisers'],
    queryFn: async () => {
      const response = await api.get('/advertisers', { params: { limit: 100 } })
      return response.data.advertisers || response.data || []
    },
  })

  React.useEffect(() => {
    if (copy) {
      setFormData({
        title: copy.title || '',
        script_text: copy.script_text || '',
        expires_at: copy.expires_at
          ? new Date(copy.expires_at).toISOString().split('T')[0]
          : '',
        active: copy.active !== undefined ? copy.active : true,
      })
    }
  }, [copy])

  const updateMutation = useMutation({
    mutationFn: async (updates: any) => {
      await updateCopy(copyId, updates)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy', copyId] })
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      setEditing(false)
      if (onUpdate) {
        onUpdate()
      }
    },
  })

  const handleSave = () => {
    const updates: any = {
      title: formData.title,
      script_text: formData.script_text || undefined,
      expires_at: formData.expires_at || undefined,
      active: formData.active,
    }
    updateMutation.mutate(updates)
  }

  const handleCancel = () => {
    if (copy) {
      setFormData({
        title: copy.title || '',
        script_text: copy.script_text || '',
        expires_at: copy.expires_at
          ? new Date(copy.expires_at).toISOString().split('T')[0]
          : '',
        active: copy.active !== undefined ? copy.active : true,
      })
    }
    setEditing(false)
  }

  const getAudioUrl = () => {
    if (copy?.audio_file_url) {
      // audio_file_url is already in format /api/copy/{filename}/file
      return copy.audio_file_url
    }
    if (copy?.audio_file_path) {
      // Extract filename from path and construct URL
      const filename = copy.audio_file_path.split('/').pop()
      if (filename) {
        return `/api/copy/${filename}/file`
      }
      return copy.audio_file_path
    }
    return null
  }

  const audioUrl = getAudioUrl()

  if (isLoading) {
    return (
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        </DialogContent>
      </Dialog>
    )
  }

  if (!copy) {
    return null
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6">Copy Details</Typography>
          {!editing && (
            <IconButton size="small" onClick={() => setEditing(true)}>
              <EditIcon />
            </IconButton>
          )}
        </Box>
      </DialogTitle>
      <DialogContent>
        <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)} sx={{ mb: 2 }}>
          <Tab label="Details" />
          <Tab label="Audio" />
          <Tab label="Assignments" />
        </Tabs>

        {/* Details Tab */}
        <TabPanel value={tabValue} index={0}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              fullWidth
              label="Title"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              disabled={!editing}
              required
            />

            <Box sx={{ display: 'flex', gap: 2 }}>
              <TextField
                fullWidth
                label="Version"
                value={copy.version}
                disabled
                InputProps={{
                  startAdornment: <Chip label={`v${copy.version}`} size="small" sx={{ mr: 1 }} />,
                }}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                    disabled={!editing}
                  />
                }
                label="Active"
              />
            </Box>

            <TextField
              fullWidth
              label="Order"
              value={
                copy.order_id
                  ? orders?.find((o: any) => o.id === copy.order_id)?.order_number || 'N/A'
                  : 'N/A'
              }
              disabled
            />

            <TextField
              fullWidth
              label="Advertiser"
              value={
                copy.advertiser_id
                  ? advertisers?.find((a: any) => a.id === copy.advertiser_id)?.name || 'N/A'
                  : 'N/A'
              }
              disabled
            />

            <TextField
              fullWidth
              label="Expiration Date"
              type="date"
              value={formData.expires_at}
              onChange={(e) => setFormData({ ...formData, expires_at: e.target.value })}
              disabled={!editing}
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              fullWidth
              label="Script Text"
              value={formData.script_text}
              onChange={(e) => setFormData({ ...formData, script_text: e.target.value })}
              disabled={!editing}
              multiline
              rows={6}
            />

            <Box>
              <Typography variant="caption" color="text.secondary">
                Created: {new Date(copy.created_at).toLocaleString()}
              </Typography>
              <br />
              <Typography variant="caption" color="text.secondary">
                Updated: {new Date(copy.updated_at).toLocaleString()}
              </Typography>
            </Box>
          </Box>
        </TabPanel>

        {/* Audio Tab */}
        <TabPanel value={tabValue} index={1}>
          {audioUrl ? (
            <Box>
              <AudioPlayer
                src={audioUrl}
                title="Audio File"
                onError={(error) => {
                  console.error('Audio playback error:', error)
                }}
              />
              <Box sx={{ mt: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  File Path: {copy.audio_file_path || copy.audio_file_url || 'N/A'}
                </Typography>
              </Box>
            </Box>
          ) : (
            <Alert severity="info">No audio file available for this copy.</Alert>
          )}
        </TabPanel>

        {/* Assignments Tab */}
        <TabPanel value={tabValue} index={2}>
          {assignments && assignments.length > 0 ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Spot ID</TableCell>
                    <TableCell>Date</TableCell>
                    <TableCell>Time</TableCell>
                    <TableCell>Assigned At</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {assignments.map((assignment: any) => (
                    <TableRow key={assignment.id}>
                      <TableCell>{assignment.spot_id}</TableCell>
                      <TableCell>
                        {assignment.assigned_at
                          ? new Date(assignment.assigned_at).toLocaleDateString()
                          : 'N/A'}
                      </TableCell>
                      <TableCell>
                        {assignment.assigned_at
                          ? new Date(assignment.assigned_at).toLocaleTimeString()
                          : 'N/A'}
                      </TableCell>
                      <TableCell>
                        {new Date(assignment.assigned_at).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info">No assignments found for this copy.</Alert>
          )}
        </TabPanel>
      </DialogContent>
      <DialogActions>
        {editing ? (
          <>
            <Button onClick={handleCancel} startIcon={<CancelIcon />}>
              Cancel
            </Button>
            <Button
              onClick={handleSave}
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={updateMutation.isPending || !formData.title.trim()}
            >
              {updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </>
        ) : (
          <Button onClick={onClose}>Close</Button>
        )}
      </DialogActions>
    </Dialog>
  )
}

export default CopyDetailDialog

