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
  IconButton,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Switch,
  FormControlLabel,
  Autocomplete,
  Chip,
} from '@mui/material'
import { Add, Edit, Delete, Settings } from '@mui/icons-material'
import api from '../../utils/api'
import { getSalesTeamsProxy, getSalesRepsProxy } from '../../utils/api'

interface SalesTeam {
  id?: string
  name: string
  description?: string
  active: boolean
  created_at: string
  updated_at: string
}

const SalesTeams: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [repsDialog, setRepsDialog] = useState(false)
  const [selectedTeam, setSelectedTeam] = useState<SalesTeam | null>(null)
  const [editingTeam, setEditingTeam] = useState<SalesTeam | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({ name: '', description: '', active: true })
  const queryClient = useQueryClient()

  const { data: salesReps } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: async () => {
      const data = await getSalesRepsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: teamReps } = useQuery({
    queryKey: ['team-reps', selectedTeam?.id],
    queryFn: async () => {
      if (!selectedTeam?.id) return []
      try {
        const response = await api.get(`/sales-teams/${selectedTeam.id}`)
        return response.data.sales_reps || []
      } catch {
        return []
      }
    },
    enabled: !!selectedTeam?.id && repsDialog,
  })

  const { data: teams, isLoading, error } = useQuery({
    queryKey: ['sales-teams', searchTerm],
    queryFn: async () => {
      const data = await getSalesTeamsProxy({
        limit: 1000,
        skip: 0,
        active_only: false,
        search: searchTerm || undefined,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const createMutation = useMutation({
    mutationFn: async (data: { name: string; description?: string; active: boolean }) => {
      const response = await api.post('/sales-teams', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-teams'] })
      setOpenDialog(false)
      setEditingTeam(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to create sales team'
      setErrorMessage(message)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<SalesTeam> }) => {
      const response = await api.put(`/sales-teams/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-teams'] })
      setOpenDialog(false)
      setEditingTeam(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to update sales team'
      setErrorMessage(message)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/sales-teams/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-teams'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to delete sales team'
      setErrorMessage(message)
    },
  })

  const handleOpenDialog = (team?: SalesTeam) => {
    if (team) {
      setEditingTeam(team)
      setFormData({
        name: team.name,
        description: team.description || '',
        active: team.active,
      })
    } else {
      setEditingTeam(null)
      setFormData({ name: '', description: '', active: true })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingTeam(null)
    setFormData({ name: '', description: '', active: true })
    setErrorMessage(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingTeam) {
      updateMutation.mutate({ id: editingTeam.id, data: formData })
    } else {
      createMutation.mutate(formData)
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
        <Typography variant="h4">Sales Teams</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="Search"
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
            Add Team
          </Button>
        </Box>
      </Box>

      <Card>
        <CardContent>
          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error">Failed to load sales teams</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Active</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {teams?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No sales teams found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    teams?.map((team: SalesTeam) => (
                      <TableRow key={team.id}>
                        <TableCell>{team.name}</TableCell>
                        <TableCell>{team.description || '-'}</TableCell>
                        <TableCell>{team.active ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          <IconButton 
                            size="small" 
                            onClick={() => {
                              setSelectedTeam(team)
                              setRepsDialog(true)
                            }}
                            title="Manage Sales Reps"
                          >
                            <Settings />
                          </IconButton>
                          <IconButton size="small" onClick={() => handleOpenDialog(team)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(team.id)}>
                            <Delete />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingTeam ? 'Edit Sales Team' : 'Create Sales Team'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                label="Name"
                required
                fullWidth
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              />
              <TextField
                label="Description"
                fullWidth
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.active}
                    onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                  />
                }
                label="Active"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Manage Sales Reps Dialog */}
      <Dialog open={repsDialog} onClose={() => {
        setRepsDialog(false)
        setSelectedTeam(null)
      }} maxWidth="md" fullWidth>
        <DialogTitle>Manage Sales Reps - {selectedTeam?.name}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <Autocomplete
              multiple
              options={salesReps || []}
              getOptionLabel={(option) => option.username || `Rep ${option.id}`}
              value={(salesReps || []).filter(rep => teamReps?.some((tr: any) => tr.id === rep.id)) || []}
              onChange={async (e, newValue) => {
                if (!selectedTeam?.id) return
                const currentRepIds = (teamReps || []).map((r: any) => r.id)
                const newRepIds = newValue.map(r => r.id)
                
                // Add new reps
                for (const repId of newRepIds) {
                  if (!currentRepIds.includes(repId)) {
                    try {
                      await api.post(`/sales-teams/${selectedTeam.id}/sales-reps/${repId}`)
                    } catch (err) {
                      console.error('Failed to add rep:', err)
                    }
                  }
                }
                
                // Remove reps
                for (const repId of currentRepIds) {
                  if (!newRepIds.includes(repId)) {
                    try {
                      await api.delete(`/sales-teams/${selectedTeam.id}/sales-reps/${repId}`)
                    } catch (err) {
                      console.error('Failed to remove rep:', err)
                    }
                  }
                }
                
                queryClient.invalidateQueries({ queryKey: ['team-reps', selectedTeam.id] })
                queryClient.invalidateQueries({ queryKey: ['sales-teams'] })
              }}
              renderInput={(params) => (
                <TextField {...params} label="Select Sales Reps" placeholder="Choose sales reps for this team" />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option: any, index: number) => (
                  <Chip 
                    label={option.username || `Rep ${option.id}`} 
                    {...getTagProps({ index })} 
                    key={option.id} 
                  />
                ))
              }
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setRepsDialog(false)
            setSelectedTeam(null)
          }}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default SalesTeams

