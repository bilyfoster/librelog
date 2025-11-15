import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Button,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  TextField,
  CircularProgress,
} from '@mui/material'
import {
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Edit as EditIcon,
} from '@mui/icons-material'
import { getLogConflicts, resolveSpotConflict, updateSpot } from '../../utils/api'

interface Conflict {
  spot_id: number
  conflict_type: string
  message: string
  conflicting_spots?: number[]
  suggested_resolution?: string
}

interface ConflictResolverProps {
  logId: number
  onResolved?: () => void
}

const ConflictResolver: React.FC<ConflictResolverProps> = ({ logId, onResolved }) => {
  const [conflicts, setConflicts] = useState<Conflict[]>([])
  const [loading, setLoading] = useState(true)
  const [resolving, setResolving] = useState<number | null>(null)
  const [editOpen, setEditOpen] = useState(false)
  const [selectedConflict, setSelectedConflict] = useState<Conflict | null>(null)
  const [newTime, setNewTime] = useState('')

  useEffect(() => {
    loadConflicts()
  }, [logId])

  const loadConflicts = async () => {
    try {
      setLoading(true)
      const data = await getLogConflicts(logId)
      setConflicts(data.conflicts || [])
    } catch (error) {
      console.error('Failed to load conflicts:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleResolve = async (spotId: number) => {
    try {
      setResolving(spotId)
      await resolveSpotConflict(spotId)
      await loadConflicts()
      if (onResolved) onResolved()
    } catch (error) {
      console.error('Failed to resolve conflict:', error)
      alert('Failed to resolve conflict')
    } finally {
      setResolving(null)
    }
  }

  const handleEditTime = (conflict: Conflict) => {
    setSelectedConflict(conflict)
    setEditOpen(true)
  }

  const handleSaveNewTime = async () => {
    if (!selectedConflict || !newTime) return

    try {
      setResolving(selectedConflict.spot_id)
      await updateSpot(selectedConflict.spot_id, { scheduled_time: newTime })
      await loadConflicts()
      setEditOpen(false)
      setSelectedConflict(null)
      setNewTime('')
      if (onResolved) onResolved()
    } catch (error) {
      console.error('Failed to update spot time:', error)
      alert('Failed to update spot time')
    } finally {
      setResolving(null)
    }
  }

  const getConflictColor = (type: string) => {
    switch (type) {
      case 'OVERLAP':
        return 'error'
      case 'OVERSOLD':
        return 'warning'
      case 'TIMING':
        return 'info'
      default:
        return 'default'
    }
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (conflicts.length === 0) {
    return (
      <Paper sx={{ p: 3 }}>
        <Alert severity="success" icon={<CheckCircleIcon />}>
          No conflicts found. This log is ready to publish.
        </Alert>
      </Paper>
    )
  }

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h5">
            Conflict Resolution
          </Typography>
          <Chip
            icon={<WarningIcon />}
            label={`${conflicts.length} Conflict(s)`}
            color="error"
          />
        </Box>

        <Alert severity="warning" sx={{ mb: 3 }}>
          Please resolve all conflicts before publishing this log. Conflicts can occur due to overlapping spots,
          oversold inventory, or timing issues.
        </Alert>

        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Spot ID</TableCell>
                <TableCell>Conflict Type</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {conflicts.map((conflict, index) => (
                <TableRow key={index}>
                  <TableCell>{conflict.spot_id}</TableCell>
                  <TableCell>
                    <Chip
                      label={conflict.conflict_type}
                      color={getConflictColor(conflict.conflict_type) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{conflict.message}</TableCell>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => handleEditTime(conflict)}
                      disabled={resolving === conflict.spot_id}
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <Button
                      size="small"
                      variant="contained"
                      color="success"
                      onClick={() => handleResolve(conflict.spot_id)}
                      disabled={resolving === conflict.spot_id}
                      sx={{ ml: 1 }}
                    >
                      {resolving === conflict.spot_id ? (
                        <CircularProgress size={16} />
                      ) : (
                        'Resolve'
                      )}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      {/* Edit Time Dialog */}
      <Dialog open={editOpen} onClose={() => setEditOpen(false)}>
        <DialogTitle>Edit Spot Time</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
            Change the scheduled time for spot {selectedConflict?.spot_id} to resolve the conflict.
          </Typography>
          <TextField
            fullWidth
            label="New Time"
            type="time"
            value={newTime}
            onChange={(e) => setNewTime(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ mt: 2 }}
          />
          {selectedConflict?.suggested_resolution && (
            <Alert severity="info" sx={{ mt: 2 }}>
              Suggestion: {selectedConflict.suggested_resolution}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSaveNewTime}
            disabled={!newTime || resolving !== null}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default ConflictResolver


