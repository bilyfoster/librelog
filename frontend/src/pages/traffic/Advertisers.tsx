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
import { getAdvertisersProxy } from '../../utils/api'

interface Advertiser {
  id?: string
  name: string
  contact_first_name?: string
  contact_last_name?: string
  email?: string
  phone?: string
  address?: string
  tax_id?: string
  payment_terms?: string
  credit_limit?: number
  active: boolean
  created_at: string
  updated_at: string
}

const Advertisers: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [openDialog, setOpenDialog] = useState(false)
  const [editingAdvertiser, setEditingAdvertiser] = useState<Advertiser | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: advertisers, isLoading, error } = useQuery({
    queryKey: ['advertisers', searchTerm],
    queryFn: async () => {
      try {
        // Use server-side proxy endpoint - all processing happens on backend
        const data = await getAdvertisersProxy({
          limit: 100,
          skip: 0,
          active_only: false,
          search: searchTerm || undefined,
        })
        return Array.isArray(data) ? data : []
      } catch (err: any) {
        // Re-throw with more context for authentication errors
        if (err?.response?.status === 401) {
          throw new Error('Authentication required. Please log in to view advertisers.')
        }
        throw err
      }
    },
    retry: false, // Don't retry on auth errors
  })

  const createMutation = useMutation({
    mutationFn: async (data: Partial<Advertiser>) => {
      const response = await api.post('/advertisers', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['advertisers'] })
      setOpenDialog(false)
      setEditingAdvertiser(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to create advertiser'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Create advertiser error:', error)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<Advertiser> }) => {
      const response = await api.put(`/advertisers/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['advertisers'] })
      setOpenDialog(false)
      setEditingAdvertiser(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to update advertiser'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Update advertiser error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/advertisers/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['advertisers'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete advertiser'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete advertiser error:', error)
    },
  })

  const handleEdit = (advertiser: Advertiser) => {
    setEditingAdvertiser(advertiser)
    setOpenDialog(true)
    setErrorMessage(null)
  }

  const handleDelete = (id?: string) => {
    if (window.confirm('Are you sure you want to delete this advertiser?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const data: Partial<Advertiser> = {
      name: formData.get('name') as string,
      contact_first_name: formData.get('contact_first_name') as string || undefined,
      contact_last_name: formData.get('contact_last_name') as string || undefined,
      email: formData.get('email') as string || undefined,
      phone: formData.get('phone') as string || undefined,
      address: formData.get('address') as string || undefined,
      tax_id: formData.get('tax_id') as string || undefined,
      payment_terms: formData.get('payment_terms') as string || undefined,
      credit_limit: formData.get('credit_limit') ? parseFloat(formData.get('credit_limit') as string) : undefined,
    }

    if (editingAdvertiser) {
      updateMutation.mutate({ id: editingAdvertiser.id, data })
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
        <Typography variant="h4">Advertisers</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<Add />}
          onClick={() => {
            setEditingAdvertiser(null)
            setOpenDialog(true)
          }}
        >
          Add Advertiser
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Box mb={2}>
            <TextField
              placeholder="Search advertisers..."
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
            <Alert 
              severity="error" 
              action={
                <Button 
                  color="inherit" 
                  size="small" 
                  onClick={() => {
                    // If it's an auth error, redirect to login
                    if (error instanceof Error && error.message.includes('Authentication')) {
                      window.location.href = '/login'
                    } else {
                      queryClient.invalidateQueries({ queryKey: ['advertisers'] })
                    }
                  }}
                >
                  {error instanceof Error && error.message.includes('Authentication') ? 'Log In' : 'Retry'}
                </Button>
              }
            >
              {error instanceof Error ? error.message : 'Failed to load advertisers. Please check your connection and try again.'}
            </Alert>
          ) : !advertisers || advertisers.length === 0 ? (
            <Box sx={{ py: 4, textAlign: 'center' }}>
              <Typography color="textSecondary" variant="body1" sx={{ mb: 1 }}>
                No advertisers found
              </Typography>
              <Typography color="textSecondary" variant="body2">
                {searchTerm ? `No advertisers match "${searchTerm}"` : 'Get started by adding your first advertiser'}
              </Typography>
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Contact</TableCell>
                    <TableCell>Email</TableCell>
                    <TableCell>Phone</TableCell>
                    <TableCell>Payment Terms</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {advertisers.map((advertiser: Advertiser) => (
                    <TableRow key={advertiser.id}>
                      <TableCell>{advertiser.name}</TableCell>
                      <TableCell>
                        {advertiser.contact_first_name || advertiser.contact_last_name
                          ? `${advertiser.contact_first_name || ''} ${advertiser.contact_last_name || ''}`.trim()
                          : 'N/A'}
                      </TableCell>
                      <TableCell>{advertiser.email || 'N/A'}</TableCell>
                      <TableCell>{advertiser.phone || 'N/A'}</TableCell>
                      <TableCell>{advertiser.payment_terms || 'N/A'}</TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleEdit(advertiser)}>
                          <Edit />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleDelete(advertiser.id)}>
                          <Delete />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={() => { setOpenDialog(false); setEditingAdvertiser(null); setErrorMessage(null); }} maxWidth="md" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingAdvertiser ? 'Edit Advertiser' : 'Add Advertiser'}</DialogTitle>
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
                defaultValue={editingAdvertiser?.name || ''}
              />
              <TextField
                name="contact_first_name"
                label="Contact First Name"
                fullWidth
                defaultValue={editingAdvertiser?.contact_first_name || ''}
              />
              <TextField
                name="contact_last_name"
                label="Contact Last Name"
                fullWidth
                defaultValue={editingAdvertiser?.contact_last_name || ''}
              />
              <TextField
                name="email"
                label="Email"
                type="email"
                fullWidth
                defaultValue={editingAdvertiser?.email || ''}
              />
              <TextField
                name="phone"
                label="Phone"
                fullWidth
                defaultValue={editingAdvertiser?.phone || ''}
              />
              <TextField
                name="address"
                label="Address"
                multiline
                rows={2}
                fullWidth
                defaultValue={editingAdvertiser?.address || ''}
              />
              <TextField
                name="tax_id"
                label="Tax ID"
                fullWidth
                defaultValue={editingAdvertiser?.tax_id || ''}
              />
              <TextField
                name="payment_terms"
                label="Payment Terms"
                fullWidth
                defaultValue={editingAdvertiser?.payment_terms || ''}
              />
              <TextField
                name="credit_limit"
                label="Credit Limit"
                type="number"
                fullWidth
                defaultValue={editingAdvertiser?.credit_limit || ''}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => { setOpenDialog(false); setEditingAdvertiser(null); setErrorMessage(null); }}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={createMutation.isPending || updateMutation.isPending}>
              {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  )
}

export default Advertisers

