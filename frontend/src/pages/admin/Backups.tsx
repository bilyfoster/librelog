import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Stack,
  Tooltip,
  LinearProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Restore as RestoreIcon,
  CloudDownload as DownloadIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  Schedule as PendingIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material'
import {
  getBackupsProxy,
  createBackup,
  restoreBackup,
  deleteBackup,
} from '../../utils/api'
import { formatDistanceToNow } from 'date-fns'

interface Backup {
  id?: string
  backup_type: 'FULL' | 'DATABASE' | 'FILES'
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'
  storage_provider: 'LOCAL' | 'S3' | 'BACKBLAZE_B2'
  filename: string
  file_path?: string
  remote_path?: string
  file_size?: number
  database_dump: boolean
  files_included: boolean
  description?: string
  started_at?: string
  completed_at?: string
  error_message?: string
  created_by: number
  created_at: string
}

const Backups: React.FC = () => {
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [restoreDialogOpen, setRestoreDialogOpen] = useState(false)
  const [selectedBackup, setSelectedBackup] = useState<Backup | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: backups, isLoading, error } = useQuery({
    queryKey: ['backups', statusFilter],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const response = await getBackupsProxy({
        limit: 100,
        skip: 0,
        ...(statusFilter && { status_filter: statusFilter }),
      })
      return Array.isArray(response) ? response : []
    },
    retry: 1,
    refetchInterval: (data) => {
      // Auto-refresh if there are in-progress backups
      if (!data || !Array.isArray(data)) {
        return false
      }
      const hasInProgress = data.some((b: Backup) => b.status === 'IN_PROGRESS' || b.status === 'PENDING')
      return hasInProgress ? 5000 : false
    },
  })

  const createMutation = useMutation({
    mutationFn: createBackup,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backups'] })
      setErrorMessage(null)
      setCreateDialogOpen(false)
    },
    onError: (error: any) => {
      let message = 'Failed to create backup'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Create backup error:', error)
    },
  })

  const restoreMutation = useMutation({
    mutationFn: ({ backup_id, restore }: { backup_id?: string; restore: { restore_database?: boolean; restore_files?: boolean } }) =>
      restoreBackup(backup_id, restore),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backups'] })
      setErrorMessage(null)
      setRestoreDialogOpen(false)
      setSelectedBackup(null)
    },
    onError: (error: any) => {
      let message = 'Failed to restore backup'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Restore backup error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteBackup,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['backups'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete backup'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete backup error:', error)
    },
  })

  const handleCreateBackup = (formData: {
    backup_type: 'FULL' | 'DATABASE' | 'FILES'
    storage_provider: 'LOCAL' | 'S3' | 'BACKBLAZE_B2'
    description?: string
  }) => {
    createMutation.mutate(formData)
  }

  const handleRestore = (backup: Backup, restoreDatabase: boolean, restoreFiles: boolean) => {
    restoreMutation.mutate({
      backup_id: backup.id,
      restore: {
        restore_database: restoreDatabase,
        restore_files: restoreFiles,
      },
    })
  }

  const handleDelete = (backup: Backup) => {
    if (window.confirm(`Are you sure you want to delete backup "${backup.filename}"? This action cannot be undone.`)) {
      deleteMutation.mutate(backup.id)
    }
  }

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'N/A'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let size = bytes
    let unitIndex = 0
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024
      unitIndex++
    }
    return `${size.toFixed(2)} ${units[unitIndex]}`
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success'
      case 'FAILED':
        return 'error'
      case 'IN_PROGRESS':
        return 'info'
      case 'PENDING':
        return 'warning'
      default:
        return 'default'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return <SuccessIcon fontSize="small" />
      case 'FAILED':
        return <ErrorIcon fontSize="small" />
      case 'IN_PROGRESS':
        return <CircularProgress size={16} />
      case 'PENDING':
        return <PendingIcon fontSize="small" />
      default:
        return null
    }
  }

  return (
    <Box>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Backups</Typography>
        <Stack direction="row" spacing={2}>
          <TextField
            select
            label="Status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="FAILED">Failed</MenuItem>
          </TextField>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => queryClient.invalidateQueries({ queryKey: ['backups'] })}
          >
            Refresh
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateDialogOpen(true)}
          >
            Create Backup
          </Button>
        </Stack>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load backups: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      )}

      <Card>
        <CardContent>
          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : !backups || backups.length === 0 ? (
            <Alert severity="info">
              No backups found. Create your first backup to get started.
            </Alert>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Type</TableCell>
                    <TableCell>Filename</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Storage</TableCell>
                    <TableCell>Size</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {backups.map((backup: Backup) => (
                    <TableRow key={backup.id}>
                      <TableCell>
                        <Chip label={backup.backup_type} size="small" />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>
                          {backup.filename}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          icon={getStatusIcon(backup.status)}
                          label={backup.status}
                          color={getStatusColor(backup.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Chip label={backup.storage_provider} size="small" variant="outlined" />
                      </TableCell>
                      <TableCell>{formatFileSize(backup.file_size)}</TableCell>
                      <TableCell>
                        <Typography variant="body2" noWrap sx={{ maxWidth: 150 }}>
                          {backup.description || 'N/A'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Tooltip title={new Date(backup.created_at).toLocaleString()}>
                          <Typography variant="body2">
                            {formatDistanceToNow(new Date(backup.created_at), { addSuffix: true })}
                          </Typography>
                        </Tooltip>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1}>
                          {backup.status === 'COMPLETED' && (
                            <Tooltip title="Restore">
                              <IconButton
                                size="small"
                                color="primary"
                                onClick={() => {
                                  setSelectedBackup(backup)
                                  setRestoreDialogOpen(true)
                                }}
                              >
                                <RestoreIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                          {backup.status === 'COMPLETED' && backup.file_path && (
                            <Tooltip title="Download">
                              <IconButton
                                size="small"
                                color="primary"
                                component="a"
                                href={`/api/backups/${backup.id}/download`}
                                download
                              >
                                <DownloadIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleDelete(backup)}
                              disabled={backup.status === 'IN_PROGRESS'}
                            >
                              <DeleteIcon />
                            </IconButton>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Create Backup Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create Backup</DialogTitle>
        <DialogContent>
          <CreateBackupForm
            onSubmit={handleCreateBackup}
            onCancel={() => setCreateDialogOpen(false)}
            loading={createMutation.isPending}
          />
        </DialogContent>
      </Dialog>

      {/* Restore Backup Dialog */}
      <Dialog open={restoreDialogOpen} onClose={() => setRestoreDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Restore Backup</DialogTitle>
        <DialogContent>
          {selectedBackup && (
            <RestoreBackupForm
              backup={selectedBackup}
              onSubmit={(restoreDatabase, restoreFiles) => handleRestore(selectedBackup, restoreDatabase, restoreFiles)}
              onCancel={() => {
                setRestoreDialogOpen(false)
                setSelectedBackup(null)
              }}
              loading={restoreMutation.isPending}
            />
          )}
        </DialogContent>
      </Dialog>
    </Box>
  )
}

interface CreateBackupFormProps {
  onSubmit: (data: {
    backup_type: 'FULL' | 'DATABASE' | 'FILES'
    storage_provider: 'LOCAL' | 'S3' | 'BACKBLAZE_B2'
    description?: string
  }) => void
  onCancel: () => void
  loading: boolean
}

const CreateBackupForm: React.FC<CreateBackupFormProps> = ({ onSubmit, onCancel, loading }) => {
  const [backupType, setBackupType] = useState<'FULL' | 'DATABASE' | 'FILES'>('FULL')
  const [storageProvider, setStorageProvider] = useState<'LOCAL' | 'S3' | 'BACKBLAZE_B2'>('LOCAL')
  const [description, setDescription] = useState('')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit({
      backup_type: backupType,
      storage_provider: storageProvider,
      description: description || undefined,
    })
  }

  return (
    <form onSubmit={handleSubmit}>
      <Grid container spacing={2} sx={{ mt: 1 }}>
        <Grid item xs={12}>
          <FormControl fullWidth>
            <InputLabel>Backup Type</InputLabel>
            <Select
              value={backupType}
              onChange={(e) => setBackupType(e.target.value as 'FULL' | 'DATABASE' | 'FILES')}
              label="Backup Type"
            >
              <MenuItem value="FULL">Full (Database + Files)</MenuItem>
              <MenuItem value="DATABASE">Database Only</MenuItem>
              <MenuItem value="FILES">Files Only</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12}>
          <FormControl fullWidth>
            <InputLabel>Storage Provider</InputLabel>
            <Select
              value={storageProvider}
              onChange={(e) => setStorageProvider(e.target.value as 'LOCAL' | 'S3' | 'BACKBLAZE_B2')}
              label="Storage Provider"
            >
              <MenuItem value="LOCAL">Local Storage</MenuItem>
              <MenuItem value="S3">Amazon S3</MenuItem>
              <MenuItem value="BACKBLAZE_B2">Backblaze B2</MenuItem>
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12}>
          <TextField
            fullWidth
            label="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            multiline
            rows={3}
          />
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button onClick={onCancel} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={loading}>
              {loading ? <CircularProgress size={20} /> : 'Create Backup'}
            </Button>
          </Stack>
        </Grid>
      </Grid>
    </form>
  )
}

interface RestoreBackupFormProps {
  backup: Backup
  onSubmit: (restoreDatabase: boolean, restoreFiles: boolean) => void
  onCancel: () => void
  loading: boolean
}

const RestoreBackupForm: React.FC<RestoreBackupFormProps> = ({ backup, onSubmit, onCancel, loading }) => {
  const [restoreDatabase, setRestoreDatabase] = useState(backup.database_dump)
  const [restoreFiles, setRestoreFiles] = useState(backup.files_included)

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit(restoreDatabase, restoreFiles)
  }

  return (
    <form onSubmit={handleSubmit}>
      <Alert severity="warning" sx={{ mb: 2 }}>
        Warning: Restoring a backup will overwrite existing data. This action cannot be undone.
      </Alert>
      <Typography variant="body2" color="text.secondary" paragraph>
        Backup: <strong>{backup.filename}</strong>
      </Typography>
      <Typography variant="body2" color="text.secondary" paragraph>
        Type: <strong>{backup.backup_type}</strong>
      </Typography>
      <Grid container spacing={2} sx={{ mt: 1 }}>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={restoreDatabase}
                onChange={(e) => setRestoreDatabase(e.target.checked)}
                disabled={!backup.database_dump}
              />
            }
            label="Restore Database"
          />
          {!backup.database_dump && (
            <Typography variant="caption" color="text.secondary" display="block">
              This backup does not include database data
            </Typography>
          )}
        </Grid>
        <Grid item xs={12}>
          <FormControlLabel
            control={
              <Switch
                checked={restoreFiles}
                onChange={(e) => setRestoreFiles(e.target.checked)}
                disabled={!backup.files_included}
              />
            }
            label="Restore Files"
          />
          {!backup.files_included && (
            <Typography variant="caption" color="text.secondary" display="block">
              This backup does not include files
            </Typography>
          )}
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button onClick={onCancel} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" color="error" disabled={loading || (!restoreDatabase && !restoreFiles)}>
              {loading ? <CircularProgress size={20} /> : 'Restore Backup'}
            </Button>
          </Stack>
        </Grid>
      </Grid>
    </form>
  )
}

export default Backups

