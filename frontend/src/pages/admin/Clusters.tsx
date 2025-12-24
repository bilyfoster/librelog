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
import { getClustersProxy, getStationsProxy } from '../../utils/api'

interface Cluster {
  id?: string
  name: string
  description?: string
  active: boolean
  created_at: string
  updated_at: string
}

const Clusters: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [stationsDialog, setStationsDialog] = useState(false)
  const [selectedCluster, setSelectedCluster] = useState<Cluster | null>(null)
  const [editingCluster, setEditingCluster] = useState<Cluster | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({ name: '', description: '', active: true })
  const queryClient = useQueryClient()

  const { data: stations } = useQuery({
    queryKey: ['stations'],
    queryFn: async () => {
      const data = await getStationsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: clusterStations } = useQuery({
    queryKey: ['cluster-stations', selectedCluster?.id],
    queryFn: async () => {
      if (!selectedCluster?.id) return []
      try {
        const response = await api.get(`/clusters/${selectedCluster.id}`)
        return response.data.stations || []
      } catch {
        return []
      }
    },
    enabled: !!selectedCluster?.id && stationsDialog,
  })

  const { data: clusters, isLoading, error } = useQuery({
    queryKey: ['clusters', searchTerm],
    queryFn: async () => {
      const data = await getClustersProxy({
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
      const response = await api.post('/clusters', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusters'] })
      setOpenDialog(false)
      setEditingCluster(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to create cluster'
      setErrorMessage(message)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<Cluster> }) => {
      const response = await api.put(`/clusters/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusters'] })
      setOpenDialog(false)
      setEditingCluster(null)
      setFormData({ name: '', description: '', active: true })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to update cluster'
      setErrorMessage(message)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/clusters/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clusters'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to delete cluster'
      setErrorMessage(message)
    },
  })

  const handleOpenDialog = (cluster?: Cluster) => {
    if (cluster) {
      setEditingCluster(cluster)
      setFormData({
        name: cluster.name,
        description: cluster.description || '',
        active: cluster.active,
      })
    } else {
      setEditingCluster(null)
      setFormData({ name: '', description: '', active: true })
    }
    setOpenDialog(true)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingCluster(null)
    setFormData({ name: '', description: '', active: true })
    setErrorMessage(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (editingCluster) {
      updateMutation.mutate({ id: editingCluster.id, data: formData })
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
        <Typography variant="h4">Clusters</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="Search"
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
            Add Cluster
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
            <Alert severity="error">Failed to load clusters</Alert>
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
                  {clusters?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No clusters found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    clusters?.map((cluster: Cluster) => (
                      <TableRow key={cluster.id}>
                        <TableCell>{cluster.name}</TableCell>
                        <TableCell>{cluster.description || '-'}</TableCell>
                        <TableCell>{cluster.active ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          <IconButton 
                            size="small" 
                            onClick={() => {
                              setSelectedCluster(cluster)
                              setStationsDialog(true)
                            }}
                            title="Manage Stations"
                          >
                            <Settings />
                          </IconButton>
                          <IconButton size="small" onClick={() => handleOpenDialog(cluster)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(cluster.id)}>
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
          <DialogTitle>{editingCluster ? 'Edit Cluster' : 'Create Cluster'}</DialogTitle>
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

      {/* Manage Stations Dialog */}
      <Dialog open={stationsDialog} onClose={() => {
        setStationsDialog(false)
        setSelectedCluster(null)
      }} maxWidth="md" fullWidth>
        <DialogTitle>Manage Stations - {selectedCluster?.name}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <Autocomplete
              multiple
              options={stations || []}
              getOptionLabel={(option) => `${option.call_letters}${option.frequency ? ` (${option.frequency})` : ''}`}
              value={(stations || []).filter(s => clusterStations?.some((cs: any) => cs.id === s.id)) || []}
              onChange={async (e, newValue) => {
                if (!selectedCluster?.id) return
                const currentStationIds = (clusterStations || []).map((s: any) => s.id)
                const newStationIds = newValue.map(s => s.id)
                
                // Add new stations
                for (const stationId of newStationIds) {
                  if (!currentStationIds.includes(stationId)) {
                    try {
                      await api.post(`/stations/${stationId}/clusters/${selectedCluster.id}`)
                    } catch (err) {
                      console.error('Failed to add station:', err)
                    }
                  }
                }
                
                // Remove stations
                for (const stationId of currentStationIds) {
                  if (!newStationIds.includes(stationId)) {
                    try {
                      await api.delete(`/stations/${stationId}/clusters/${selectedCluster.id}`)
                    } catch (err) {
                      console.error('Failed to remove station:', err)
                    }
                  }
                }
                
                queryClient.invalidateQueries({ queryKey: ['cluster-stations', selectedCluster.id] })
                queryClient.invalidateQueries({ queryKey: ['clusters'] })
              }}
              renderInput={(params) => (
                <TextField {...params} label="Select Stations" placeholder="Choose stations for this cluster" />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option: any, index: number) => (
                  <Chip 
                    label={`${option.call_letters}${option.frequency ? ` (${option.frequency})` : ''}`} 
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
            setStationsDialog(false)
            setSelectedCluster(null)
          }}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Clusters

