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
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import api from '../../utils/api'
import { getSalesRegionsProxy } from '../../utils/api'

interface SalesRegion {
  id?: string
  name: string
  description?: string
  active: boolean
  created_at: string
  updated_at: string
}

const SalesRegions: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingRegion, setEditingRegion] = useState<SalesRegion | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({ name: '', description: '', active: true })
  const queryClient = useQueryClient()

  const { data: regions, isLoading, error } = useQuery({
    queryKey: ['sales-regions', searchTerm],
    queryFn: async () => {
      const data = await getSalesRegionsProxy({
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
      const response = await api.post('/sales-regions', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-regions'] })
      setOpenDialog(false)
      setEditingRegion(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to create sales region'
      setErrorMessage(message)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<SalesRegion> }) => {
      const response = await api.put(`/sales-regions/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-regions'] })
      setOpenDialog(false)
      setEditingRegion(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to update sales region'
      setErrorMessage(message)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/sales-regions/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-regions'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to delete sales region'
      setErrorMessage(message)
    },
  })

  const handleOpenDialog = (region?: SalesRegion) => {
    if (region) {
      setEditingRegion(region)
      setFormData({
        name: region.name,
        description: region.description || '',
        active: region.active,
      })
    } else {
      setEditingRegion(null)
      setFormData({ name: '', description: '', active: true })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingRegion(null)
    setFormData({ name: '', description: '', active: true })
    setErrorMessage(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingRegion) {
      updateMutation.mutate({ id: editingRegion.id, data: formData })
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
        <Typography variant="h4">Sales Regions</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="Search"
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
            Add Region
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
            <Alert severity="error">Failed to load sales regions</Alert>
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
                  {regions?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No sales regions found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    regions?.map((region: SalesRegion) => (
                      <TableRow key={region.id}>
                        <TableCell>{region.name}</TableCell>
                        <TableCell>{region.description || '-'}</TableCell>
                        <TableCell>{region.active ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleOpenDialog(region)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(region.id)}>
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
          <DialogTitle>{editingRegion ? 'Edit Sales Region' : 'Create Sales Region'}</DialogTitle>
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
    </Box>
  )
}

export default SalesRegions

