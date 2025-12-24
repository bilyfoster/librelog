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
  MenuItem,
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import api from '../../utils/api'
import { getSalesOfficesProxy, getSalesRegionsProxy } from '../../utils/api'

interface SalesOffice {
  id?: string
  name: string
  address?: string
  region_id?: string
  region_name?: string
  active: boolean
  created_at: string
  updated_at: string
}

const SalesOffices: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingOffice, setEditingOffice] = useState<SalesOffice | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({ name: '', address: '', region_id: undefined as number | undefined, active: true })
  const queryClient = useQueryClient()

  const { data: offices, isLoading, error } = useQuery({
    queryKey: ['sales-offices', searchTerm],
    queryFn: async () => {
      const data = await getSalesOfficesProxy({
        limit: 1000,
        skip: 0,
        active_only: false,
        search: searchTerm || undefined,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const { data: regions } = useQuery({
    queryKey: ['sales-regions'],
    queryFn: async () => {
      const data = await getSalesRegionsProxy({ limit: 1000, active_only: false })
      return Array.isArray(data) ? data : []
    },
  })

  const createMutation = useMutation({
    mutationFn: async (data: { name: string; address?: string; region_id?: string; active: boolean }) => {
      const response = await api.post('/sales-offices', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-offices'] })
      setOpenDialog(false)
      setEditingOffice(null)
      setFormData({ name: '', address: '', region_id: undefined, active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to create sales office'
      setErrorMessage(message)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<SalesOffice> }) => {
      const response = await api.put(`/sales-offices/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-offices'] })
      setOpenDialog(false)
      setEditingOffice(null)
      setFormData({ name: '', address: '', region_id: undefined, active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to update sales office'
      setErrorMessage(message)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/sales-offices/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-offices'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to delete sales office'
      setErrorMessage(message)
    },
  })

  const handleOpenDialog = (office?: SalesOffice) => {
    if (office) {
      setEditingOffice(office)
      setFormData({
        name: office.name,
        address: office.address || '',
        region_id: office.region_id,
        active: office.active,
      })
    } else {
      setEditingOffice(null)
      setFormData({ name: '', address: '', region_id: undefined, active: true })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingOffice(null)
    setFormData({ name: '', address: '', region_id: undefined, active: true })
    setErrorMessage(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingOffice) {
      updateMutation.mutate({ id: editingOffice.id, data: formData })
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
        <Typography variant="h4">Sales Offices</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="Search"
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
            Add Office
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
            <Alert severity="error">Failed to load sales offices</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Address</TableCell>
                    <TableCell>Region</TableCell>
                    <TableCell>Active</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {offices?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No sales offices found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    offices?.map((office: SalesOffice) => (
                      <TableRow key={office.id}>
                        <TableCell>{office.name}</TableCell>
                        <TableCell>{office.address || '-'}</TableCell>
                        <TableCell>{office.region_name || '-'}</TableCell>
                        <TableCell>{office.active ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleOpenDialog(office)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(office.id)}>
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
          <DialogTitle>{editingOffice ? 'Edit Sales Office' : 'Create Sales Office'}</DialogTitle>
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
                label="Address"
                fullWidth
                multiline
                rows={3}
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
              <TextField
                label="Region"
                select
                fullWidth
                value={formData.region_id || ''}
                onChange={(e) => setFormData({ ...formData, region_id: e.target.value ? parseInt(e.target.value) : undefined })}
              >
                <MenuItem value="">None</MenuItem>
                {regions?.map((region: any) => (
                  <MenuItem key={region.id} value={region.id}>
                    {region.name}
                  </MenuItem>
                ))}
              </TextField>
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

export default SalesOffices

