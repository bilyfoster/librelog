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
  id: number
  name: string
  contact_name?: string
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
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAdvertisersProxy({
        limit: 100,
        skip: 0,
        active_only: false,
        search: searchTerm || undefined,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
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
    mutationFn: async ({ id, data }: { id: number; data: Partial<Advertiser> }) => {
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
    mutationFn: async (id: number) => {
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

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this advertiser?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const data: Partial<Advertiser> = {
      name: formData.get('name') as string,
      contact_name: formData.get('contact_name') as string || undefined,
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
            <Alert severity="error" action={
              <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['advertisers'] })}>
                Retry
              </Button>
            }>
              Failed to load advertisers: {error instanceof Error ? error.message : 'Unknown error'}
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
                    <TableCell>Payment Terms</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {advertisers?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No advertisers found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    advertisers?.map((advertiser: Advertiser) => (
                      <TableRow key={advertiser.id}>
                        <TableCell>{advertiser.name}</TableCell>
                        <TableCell>{advertiser.contact_name || 'N/A'}</TableCell>
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
                    ))
                  )}
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
                name="contact_name"
                label="Contact Name"
                fullWidth
                defaultValue={editingAdvertiser?.contact_name || ''}
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

