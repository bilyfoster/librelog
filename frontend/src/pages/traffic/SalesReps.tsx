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
  MenuItem,
  Tabs,
  Tab,
  Chip,
  Autocomplete,
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import api from '../../utils/api'
import { getSalesRepsProxy, getUsersProxy, getSalesTeamsProxy, getSalesOfficesProxy, getSalesRegionsProxy } from '../../utils/api'

interface SalesRep {
  id?: string
  user_id?: string
  employee_id?: string
  commission_rate?: number
  sales_goal?: number
  active: boolean
  username?: string
  created_at: string
  updated_at: string
}

interface User {
  id?: string
  username: string
}

const SalesReps: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [membershipDialog, setMembershipDialog] = useState(false)
  const [editingRep, setEditingRep] = useState<SalesRep | null>(null)
  const [selectedRep, setSelectedRep] = useState<SalesRep | null>(null)
  const [membershipTab, setMembershipTab] = useState(0)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: salesReps, isLoading, error } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getSalesRepsProxy({
        limit: 100,
        skip: 0,
        active_only: false,
      })
      console.log('[SalesReps] Loaded sales reps:', data.length)
      return Array.isArray(data) ? data : []
    },
  })

  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getUsersProxy({ limit: 1000 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesTeams } = useQuery({
    queryKey: ['sales-teams'],
    queryFn: async () => {
      const data = await getSalesTeamsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesOffices } = useQuery({
    queryKey: ['sales-offices'],
    queryFn: async () => {
      const data = await getSalesOfficesProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesRegions } = useQuery({
    queryKey: ['sales-regions'],
    queryFn: async () => {
      const data = await getSalesRegionsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: repTeams } = useQuery({
    queryKey: ['sales-rep-teams', selectedRep?.id],
    queryFn: async () => {
      if (!selectedRep?.id) return []
      const response = await api.get(`/sales-reps/${selectedRep.id}/teams`)
      return response.data || []
    },
    enabled: !!selectedRep?.id && membershipDialog && membershipTab === 0,
  })

  const { data: repOffices } = useQuery({
    queryKey: ['sales-rep-offices', selectedRep?.id],
    queryFn: async () => {
      if (!selectedRep?.id) return []
      const response = await api.get(`/sales-reps/${selectedRep.id}/offices`)
      return response.data || []
    },
    enabled: !!selectedRep?.id && membershipDialog && membershipTab === 1,
  })

  const { data: repRegions } = useQuery({
    queryKey: ['sales-rep-regions', selectedRep?.id],
    queryFn: async () => {
      if (!selectedRep?.id) return []
      const response = await api.get(`/sales-reps/${selectedRep.id}/regions`)
      return response.data || []
    },
    enabled: !!selectedRep?.id && membershipDialog && membershipTab === 2,
  })

  const createMutation = useMutation({
    mutationFn: async (data: Partial<SalesRep>) => {
      const response = await api.post('/sales-reps/', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-reps'] })
      setOpenDialog(false)
      setEditingRep(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to create sales rep'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Create sales rep error:', error)
      console.error('Error response data:', error?.response?.data)
      console.error('Error status:', error?.response?.status)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<SalesRep> }) => {
      const response = await api.put(`/sales-reps/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-reps'] })
      setOpenDialog(false)
      setEditingRep(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to update sales rep'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Update sales rep error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/sales-reps/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-reps'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete sales rep'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete sales rep error:', error)
    },
  })

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const user_id_str = formData.get('user_id') as string
    const user_id = user_id_str ? parseInt(user_id_str) : undefined
    
    if (!user_id) {
      setErrorMessage('Please select a user')
      return
    }
    
    const data: Partial<SalesRep> = {
      user_id,
      employee_id: formData.get('employee_id') as string || undefined,
      commission_rate: formData.get('commission_rate') ? parseFloat(formData.get('commission_rate') as string) : undefined,
      sales_goal: formData.get('sales_goal') ? parseFloat(formData.get('sales_goal') as string) : undefined,
    }

    if (editingRep) {
      updateMutation.mutate({ id: editingRep.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Sales Reps</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<Add />}
          onClick={() => {
            setEditingRep(null)
            setErrorMessage(null)
            setOpenDialog(true)
          }}
        >
          Add Sales Rep
        </Button>
      </Box>

      {errorMessage && (
        <Alert 
          severity="error" 
          sx={{ mb: 2 }}
          onClose={() => setErrorMessage(null)}
        >
          {errorMessage}
        </Alert>
      )}

      <Card>
        <CardContent>
          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error">Failed to load sales reps: {error instanceof Error ? error.message : 'Unknown error'}</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>User</TableCell>
                    <TableCell>Employee ID</TableCell>
                    <TableCell>Commission Rate</TableCell>
                    <TableCell>Sales Goal</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {!salesReps || salesReps.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No sales reps found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    salesReps?.map((rep: SalesRep) => (
                      <TableRow key={rep.id}>
                        <TableCell>{rep.username || `User ${rep.user_id}`}</TableCell>
                        <TableCell>{rep.employee_id || 'N/A'}</TableCell>
                        <TableCell>{rep.commission_rate ? `${rep.commission_rate}%` : 'N/A'}</TableCell>
                        <TableCell>{rep.sales_goal ? `$${rep.sales_goal.toLocaleString()}` : 'N/A'}</TableCell>
                        <TableCell>
                          {rep.active ? (
                            <Typography color="success.main">Active</Typography>
                          ) : (
                            <Typography color="text.secondary">Inactive</Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => { setEditingRep(rep); setOpenDialog(true) }}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(rep.id)}>
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

      <Dialog open={openDialog} onClose={() => {
        setOpenDialog(false)
        setErrorMessage(null)
      }} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingRep ? 'Edit Sales Rep' : 'Add Sales Rep'}</DialogTitle>
          <DialogContent>
            {errorMessage && (
              <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
                {errorMessage}
              </Alert>
            )}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                name="user_id"
                label="User"
                select
                required
                fullWidth
                defaultValue={editingRep?.user_id || ''}
                disabled={!!editingRep}
                SelectProps={{
                  native: false,
                }}
              >
                {users && users.length > 0 ? (
                  users.map((user: any) => (
                    <MenuItem key={user.id} value={user.id}>
                      {user.username} ({user.role})
                    </MenuItem>
                  ))
                ) : (
                  <MenuItem disabled value="">
                    No users available
                  </MenuItem>
                )}
              </TextField>
              <TextField
                name="employee_id"
                label="Employee ID"
                fullWidth
                defaultValue={editingRep?.employee_id || ''}
              />
              <TextField
                name="commission_rate"
                label="Commission Rate (%)"
                type="number"
                fullWidth
                defaultValue={editingRep?.commission_rate || ''}
              />
              <TextField
                name="sales_goal"
                label="Sales Goal ($)"
                type="number"
                fullWidth
                defaultValue={editingRep?.sales_goal || ''}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={createMutation.isPending || updateMutation.isPending}>
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Membership Management Dialog */}
      <Dialog open={membershipDialog} onClose={() => {
        setMembershipDialog(false)
        setSelectedRep(null)
        setMembershipTab(0)
      }} maxWidth="md" fullWidth>
        <DialogTitle>Manage Memberships - {selectedRep?.username}</DialogTitle>
        <DialogContent>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
            <Tabs value={membershipTab} onChange={(e, newValue) => setMembershipTab(newValue)}>
              <Tab label="Teams" />
              <Tab label="Offices" />
              <Tab label="Regions" />
            </Tabs>
          </Box>

          {membershipTab === 0 && (
            <Box>
              <Typography variant="h6" gutterBottom>Sales Teams</Typography>
              <Autocomplete
                multiple
                options={salesTeams || []}
                getOptionLabel={(option) => option.name}
                value={repTeams || []}
                onChange={async (e, newValue) => {
                  if (!selectedRep?.id) return
                  // Get current teams
                  const currentTeamIds = (repTeams || []).map((t: any) => t.id)
                  const newTeamIds = newValue.map(t => t.id)
                  
                  // Add new teams
                  for (const teamId of newTeamIds) {
                    if (!currentTeamIds.includes(teamId)) {
                      try {
                        await api.post(`/sales-teams/${teamId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to add team:', err)
                      }
                    }
                  }
                  
                  // Remove teams
                  for (const teamId of currentTeamIds) {
                    if (!newTeamIds.includes(teamId)) {
                      try {
                        await api.delete(`/sales-teams/${teamId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to remove team:', err)
                      }
                    }
                  }
                  
                  queryClient.invalidateQueries({ queryKey: ['sales-rep-teams', selectedRep.id] })
                }}
                renderInput={(params) => (
                  <TextField {...params} label="Select Teams" placeholder="Choose teams" />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option: any, index: number) => (
                    <Chip label={option.name} {...getTagProps({ index })} key={option.id} />
                  ))
                }
              />
            </Box>
          )}

          {membershipTab === 1 && (
            <Box>
              <Typography variant="h6" gutterBottom>Sales Offices</Typography>
              <Autocomplete
                multiple
                options={salesOffices || []}
                getOptionLabel={(option) => option.name}
                value={repOffices || []}
                onChange={async (e, newValue) => {
                  if (!selectedRep?.id) return
                  const currentOfficeIds = (repOffices || []).map((o: any) => o.id)
                  const newOfficeIds = newValue.map(o => o.id)
                  
                  for (const officeId of newOfficeIds) {
                    if (!currentOfficeIds.includes(officeId)) {
                      try {
                        await api.post(`/sales-offices/${officeId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to add office:', err)
                      }
                    }
                  }
                  
                  for (const officeId of currentOfficeIds) {
                    if (!newOfficeIds.includes(officeId)) {
                      try {
                        await api.delete(`/sales-offices/${officeId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to remove office:', err)
                      }
                    }
                  }
                  
                  queryClient.invalidateQueries({ queryKey: ['sales-rep-offices', selectedRep.id] })
                }}
                renderInput={(params) => (
                  <TextField {...params} label="Select Offices" placeholder="Choose offices" />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option: any, index: number) => (
                    <Chip label={option.name} {...getTagProps({ index })} key={option.id} />
                  ))
                }
              />
            </Box>
          )}

          {membershipTab === 2 && (
            <Box>
              <Typography variant="h6" gutterBottom>Sales Regions</Typography>
              <Autocomplete
                multiple
                options={salesRegions || []}
                getOptionLabel={(option) => option.name}
                value={repRegions || []}
                onChange={async (e, newValue) => {
                  if (!selectedRep?.id) return
                  const currentRegionIds = (repRegions || []).map((r: any) => r.id)
                  const newRegionIds = newValue.map(r => r.id)
                  
                  for (const regionId of newRegionIds) {
                    if (!currentRegionIds.includes(regionId)) {
                      try {
                        await api.post(`/sales-regions/${regionId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to add region:', err)
                      }
                    }
                  }
                  
                  for (const regionId of currentRegionIds) {
                    if (!newRegionIds.includes(regionId)) {
                      try {
                        await api.delete(`/sales-regions/${regionId}/sales-reps/${selectedRep.id}`)
                      } catch (err) {
                        console.error('Failed to remove region:', err)
                      }
                    }
                  }
                  
                  queryClient.invalidateQueries({ queryKey: ['sales-rep-regions', selectedRep.id] })
                }}
                renderInput={(params) => (
                  <TextField {...params} label="Select Regions" placeholder="Choose regions" />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option: any, index: number) => (
                    <Chip label={option.name} {...getTagProps({ index })} key={option.id} />
                  ))
                }
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setMembershipDialog(false)
            setSelectedRep(null)
            setMembershipTab(0)
          }}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default SalesReps

