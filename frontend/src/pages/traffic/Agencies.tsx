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
} from '@mui/material'
import { Add, Edit, Delete, Search } from '@mui/icons-material'
import api from '../../utils/api'

interface Agency {
  id: number
  name: string
  contact_name?: string
  email?: string
  phone?: string
  address?: string
  commission_rate?: number
  active: boolean
  created_at: string
  updated_at: string
}

const Agencies: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [openDialog, setOpenDialog] = useState(false)
  const [editingAgency, setEditingAgency] = useState<Agency | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: agencies, isLoading, error } = useQuery({
    queryKey: ['agencies', searchTerm],
    queryFn: async () => {
      const response = await api.get('/agencies', {
        params: {
          limit: 100,
          skip: 0,
          active_only: false,
          ...(searchTerm && { search: searchTerm }),
        },
      })
      return response.data
    },
    retry: 1,
  })

  const createMutation = useMutation({
    mutationFn: async (data: Partial<Agency>) => {
      const response = await api.post('/agencies', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agencies'] })
      setOpenDialog(false)
      setEditingAgency(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to create agency'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Create agency error:', error)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: number; data: Partial<Agency> }) => {
      const response = await api.put(`/agencies/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agencies'] })
      setOpenDialog(false)
      setEditingAgency(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to update agency'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Update agency error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/agencies/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['agencies'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete agency'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete agency error:', error)
    },
  })

  const handleEdit = (agency: Agency) => {
    setEditingAgency(agency)
    setOpenDialog(true)
    setErrorMessage(null)
  }

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this agency?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const data: Partial<Agency> = {
      name: formData.get('name') as string,
      contact_name: formData.get('contact_name') as string || undefined,
      email: formData.get('email') as string || undefined,
      phone: formData.get('phone') as string || undefined,
      address: formData.get('address') as string || undefined,
      commission_rate: formData.get('commission_rate') ? parseFloat(formData.get('commission_rate') as string) : undefined,
    }

    if (editingAgency) {
      updateMutation.mutate({ id: editingAgency.id, data })
    } else {
      createMutation.mutate(data)
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
        <Typography variant="h4">Agencies</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<Add />}
          onClick={() => {
            setEditingAgency(null)
            setOpenDialog(true)
          }}
        >
          Add Agency
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Box mb={2}>
            <TextField
              placeholder="Search agencies..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              InputProps={{
                startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />,
              }}
              sx={{ width: 300 }}
            />
          </Box>

          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error" action={
              <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['agencies'] })}>
                Retry
              </Button>
            }>
              Failed to load agencies: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Contact</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Phone</TableCell>
                    <TableCell>Commission Rate</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {agencies?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No agencies found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    agencies?.map((agency: Agency) => (
                      <TableRow key={agency.id}>
                        <TableCell>{agency.name}</TableCell>
                        <TableCell>{agency.contact_name || 'N/A'}</TableCell>
                        <TableCell>{agency.email || 'N/A'}</TableCell>
                        <TableCell>{agency.phone || 'N/A'}</TableCell>
                        <TableCell>{agency.commission_rate ? `${agency.commission_rate}%` : 'N/A'}</TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleEdit(agency)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => handleDelete(agency.id)}>
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

      <Dialog open={openDialog} onClose={() => { setOpenDialog(false); setEditingAgency(null); setErrorMessage(null); }} maxWidth="md" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingAgency ? 'Edit Agency' : 'Add Agency'}</DialogTitle>
          <DialogContent>
            {errorMessage && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {errorMessage}
              </Alert>
            )}
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                name="name"
                label="Name"
                required
                fullWidth
                defaultValue={editingAgency?.name || ''}
              />
              <TextField
                name="contact_name"
                label="Contact Name"
                fullWidth
                defaultValue={editingAgency?.contact_name || ''}
              />
              <TextField
                name="email"
                label="Email"
                type="email"
                fullWidth
                defaultValue={editingAgency?.email || ''}
              />
              <TextField
                name="phone"
                label="Phone"
                fullWidth
                defaultValue={editingAgency?.phone || ''}
              />
              <TextField
                name="address"
                label="Address"
                multiline
                rows={2}
                fullWidth
                defaultValue={editingAgency?.address || ''}
              />
              <TextField
                name="commission_rate"
                label="Commission Rate (%)"
                type="number"
                fullWidth
                defaultValue={editingAgency?.commission_rate || ''}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { setOpenDialog(false); setEditingAgency(null); setErrorMessage(null); }}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={createMutation.isPending || updateMutation.isPending}>
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  )
}

export default Agencies

