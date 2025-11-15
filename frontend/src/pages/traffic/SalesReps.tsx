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
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import api from '../../utils/api'

interface SalesRep {
  id: number
  user_id: number
  employee_id?: string
  commission_rate?: number
  sales_goal?: number
  active: boolean
  username?: string
  created_at: string
  updated_at: string
}

interface User {
  id: number
  username: string
}

const SalesReps: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingRep, setEditingRep] = useState<SalesRep | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: salesReps, isLoading, error } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: async () => {
      const response = await api.get('/sales-reps/', {
        params: { limit: 100, skip: 0, active_only: false },
      })
      return response.data
    },
  })

  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      // This would need a users endpoint - for now return empty
      return []
    },
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
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: number; data: Partial<SalesRep> }) => {
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
    mutationFn: async (id: number) => {
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
    const data: Partial<SalesRep> = {
      user_id: parseInt(formData.get('user_id') as string),
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
            <Alert severity="error">Failed to load sales reps</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>User</TableCell>
                    <TableCell>Employee ID</TableCell>
                    <TableCell>Commission Rate</TableCell>
                    <TableCell>Sales Goal</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {salesReps?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
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
                label="User ID"
                type="number"
                required
                fullWidth
                defaultValue={editingRep?.user_id || ''}
                disabled={!!editingRep}
              />
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
    </Box>
  )
}

export default SalesReps

