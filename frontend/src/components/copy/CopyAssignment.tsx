import React, { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Alert,
  CircularProgress,
  Chip,
  Checkbox,
} from '@mui/material'
import {
  Delete as DeleteIcon,
  Assignment as AssignmentIcon,
} from '@mui/icons-material'
import {
  getSpots,
  getCopyAssignments,
  createCopyAssignment,
  deleteCopyAssignment,
} from '../../utils/api'

interface CopyAssignmentProps {
  copyId: number
  onSuccess?: () => void
  onCancel?: () => void
}

const CopyAssignment: React.FC<CopyAssignmentProps> = ({ copyId, onSuccess, onCancel }) => {
  const [selectedSpots, setSelectedSpots] = useState<number[]>([])
  const [dateFilter, setDateFilter] = useState('')
  const queryClient = useQueryClient()

  // Load existing assignments
  const { data: assignments, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['copy-assignments', copyId],
    queryFn: async () => {
      const data = await getCopyAssignments({ copy_id: copyId, limit: 1000 })
      return data
    },
  })

  // Load spots for selection
  const { data: spots, isLoading: spotsLoading } = useQuery({
    queryKey: ['spots', dateFilter],
    queryFn: async () => {
      const params: any = { limit: 1000 }
      if (dateFilter) {
        params.scheduled_date = dateFilter
      }
      const data = await getSpots(params)
      return data
    },
  })

  const createMutation = useMutation({
    mutationFn: async (spotId: number) => {
      await createCopyAssignment({
        spot_id: spotId,
        copy_id: copyId,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy-assignments', copyId] })
      queryClient.invalidateQueries({ queryKey: ['spots'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (assignmentId: number) => {
      await deleteCopyAssignment(assignmentId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy-assignments', copyId] })
    },
  })

  const handleBulkAssign = async () => {
    if (selectedSpots.length === 0) {
      alert('Please select at least one spot')
      return
    }

    try {
      // Filter out spots that already have assignments
      const assignedSpotIds = new Set(
        assignments?.map((a: any) => a.spot_id) || []
      )
      const spotsToAssign = selectedSpots.filter(id => !assignedSpotIds.has(id))

      if (spotsToAssign.length === 0) {
        alert('All selected spots already have this copy assigned')
        return
      }

      // Assign to all selected spots
      await Promise.all(
        spotsToAssign.map(spotId => createMutation.mutateAsync(spotId))
      )

      setSelectedSpots([])
      if (onSuccess) {
        onSuccess()
      }
    } catch (error) {
      console.error('Failed to assign copy:', error)
      alert('Failed to assign copy to some spots')
    }
  }

  const handleRemoveAssignment = async (assignmentId: number) => {
    if (window.confirm('Are you sure you want to remove this assignment?')) {
      deleteMutation.mutate(assignmentId)
    }
  }

  const toggleSpotSelection = (spotId: number) => {
    setSelectedSpots(prev =>
      prev.includes(spotId)
        ? prev.filter(id => id !== spotId)
        : [...prev, spotId]
    )
  }

  const assignedSpotIds = new Set(assignments?.map((a: any) => a.spot_id) || [])
  const availableSpots = spots?.filter((spot: any) => !assignedSpotIds.has(spot.id)) || []

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Assign Copy to Spots
      </Typography>

      {/* Date Filter */}
      <Box sx={{ mb: 3 }}>
        <TextField
          label="Filter by Date (Optional)"
          type="date"
          value={dateFilter}
          onChange={(e) => setDateFilter(e.target.value)}
          InputLabelProps={{ shrink: true }}
          sx={{ minWidth: 200 }}
        />
      </Box>

      {/* Existing Assignments */}
      {assignments && assignments.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Existing Assignments ({assignments.length})
          </Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Date</TableCell>
                  <TableCell>Time</TableCell>
                  <TableCell>Spot Length</TableCell>
                  <TableCell>Assigned At</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {assignments.map((assignment: any) => {
                  const spot = spots?.find((s: any) => s.id === assignment.spot_id)
                  return (
                    <TableRow key={assignment.id}>
                      <TableCell>
                        {spot ? new Date(spot.scheduled_date).toLocaleDateString() : 'N/A'}
                      </TableCell>
                      <TableCell>
                        {spot ? spot.scheduled_time.substring(0, 5) : 'N/A'}
                      </TableCell>
                      <TableCell>
                        {spot ? `${spot.spot_length}s` : 'N/A'}
                      </TableCell>
                      <TableCell>
                        {new Date(assignment.assigned_at).toLocaleString()}
                      </TableCell>
                      <TableCell>
                        <IconButton
                          size="small"
                          onClick={() => handleRemoveAssignment(assignment.id)}
                          color="error"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}

      {/* Available Spots for Assignment */}
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="subtitle1">
            Available Spots ({availableSpots.length})
          </Typography>
          {selectedSpots.length > 0 && (
            <Button
              variant="contained"
              startIcon={<AssignmentIcon />}
              onClick={handleBulkAssign}
              disabled={createMutation.isPending}
            >
              Assign to {selectedSpots.length} Spot{selectedSpots.length !== 1 ? 's' : ''}
            </Button>
          )}
        </Box>

        {spotsLoading ? (
          <CircularProgress />
        ) : availableSpots.length > 0 ? (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell padding="checkbox">Select</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Time</TableCell>
                  <TableCell>Spot Length</TableCell>
                  <TableCell>Break Position</TableCell>
                  <TableCell>Daypart</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {availableSpots.map((spot: any) => (
                  <TableRow key={spot.id} hover>
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={selectedSpots.includes(spot.id)}
                        onChange={() => toggleSpotSelection(spot.id)}
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(spot.scheduled_date).toLocaleDateString()}
                    </TableCell>
                    <TableCell>{spot.scheduled_time.substring(0, 5)}</TableCell>
                    <TableCell>{spot.spot_length}s</TableCell>
                    <TableCell>{spot.break_position || 'N/A'}</TableCell>
                    <TableCell>{spot.daypart || 'N/A'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">
            {dateFilter
              ? 'No available spots found for the selected date'
              : 'No available spots found. All spots may already have copy assigned.'}
          </Alert>
        )}
      </Box>

      {/* Actions */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
        {onCancel && (
          <Button onClick={onCancel}>Cancel</Button>
        )}
      </Box>
    </Box>
  )
}

export default CopyAssignment

