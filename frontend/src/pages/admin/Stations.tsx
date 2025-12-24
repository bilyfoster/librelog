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
  Grid,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  InputAdornment,
  IconButton as MuiIconButton,
} from '@mui/material'
import { Add, Edit, Delete, ExpandMore, Visibility, VisibilityOff } from '@mui/icons-material'
import api from '../../utils/api'
import { getStationsProxy } from '../../utils/api'

interface Station {
  id?: string
  call_letters: string
  frequency?: string
  market?: string
  format?: string
  ownership?: string
  contacts?: any
  rates?: any
  inventory_class?: string
  active: boolean
  created_at: string
  updated_at: string
  libretime_config?: {
    api_url?: string
    api_key?: string
    public_url?: string
  }
}

const Stations: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingStation, setEditingStation] = useState<Station | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({
    call_letters: '',
    frequency: '',
    market: '',
    format: '',
    ownership: '',
    inventory_class: '',
    active: true,
    libretime_config: {
      api_url: '',
      api_key: '',
      public_url: '',
    },
  })
  const [showApiKey, setShowApiKey] = useState(false)
  const queryClient = useQueryClient()

  const { data: stations, isLoading, error } = useQuery({
    queryKey: ['stations', searchTerm],
    queryFn: async () => {
      const data = await getStationsProxy({
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
    mutationFn: async (data: Partial<Station>) => {
      const response = await api.post('/stations', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      setOpenDialog(false)
      setEditingStation(null)
      setFormData({
        call_letters: '',
        frequency: '',
        market: '',
        format: '',
        ownership: '',
        inventory_class: '',
        active: true,
        libretime_config: {
          api_url: '',
          api_key: '',
          public_url: '',
        },
      })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to create station'
      setErrorMessage(message)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<Station> }) => {
      const response = await api.put(`/stations/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      setOpenDialog(false)
      setEditingStation(null)
      setFormData({
        call_letters: '',
        frequency: '',
        market: '',
        format: '',
        ownership: '',
        inventory_class: '',
        active: true,
        libretime_config: {
          api_url: '',
          api_key: '',
          public_url: '',
        },
      })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to update station'
      setErrorMessage(message)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/stations/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stations'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      const message = error?.response?.data?.detail || 'Failed to delete station'
      setErrorMessage(message)
    },
  })

  const handleOpenDialog = (station?: Station) => {
    if (station) {
      setEditingStation(station)
      setFormData({
        call_letters: station.call_letters,
        frequency: station.frequency || '',
        market: station.market || '',
        format: station.format || '',
        ownership: station.ownership || '',
        inventory_class: station.inventory_class || '',
        active: station.active,
        libretime_config: station.libretime_config || {
          api_url: '',
          api_key: '',
          public_url: '',
        },
      })
    } else {
      setEditingStation(null)
      setFormData({
        call_letters: '',
        frequency: '',
        market: '',
        format: '',
        ownership: '',
        inventory_class: '',
        active: true,
        libretime_config: {
          api_url: '',
          api_key: '',
          public_url: '',
        },
      })
    }
    setOpenDialog(true)
    setShowApiKey(false)
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingStation(null)
    setFormData({
      call_letters: '',
      frequency: '',
      market: '',
      format: '',
      ownership: '',
      inventory_class: '',
      active: true,
    })
    setErrorMessage(null)
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const submitData: any = { ...formData }
    
    // Remove empty strings from top-level fields
    Object.keys(submitData).forEach(key => {
      if (key !== 'libretime_config' && submitData[key] === '') {
        delete submitData[key]
      }
    })
    
    // Handle libretime_config - only include if at least one field is set
    if (submitData.libretime_config) {
      const libretimeConfig: any = {}
      if (submitData.libretime_config.api_url) {
        libretimeConfig.api_url = submitData.libretime_config.api_url
      }
      if (submitData.libretime_config.api_key) {
        libretimeConfig.api_key = submitData.libretime_config.api_key
      }
      if (submitData.libretime_config.public_url) {
        libretimeConfig.public_url = submitData.libretime_config.public_url
      }
      
      // Only include libretime_config if it has at least one field
      if (Object.keys(libretimeConfig).length > 0) {
        submitData.libretime_config = libretimeConfig
      } else {
        delete submitData.libretime_config
      }
    } else {
      // Ensure libretime_config is not included if it's undefined
      delete submitData.libretime_config
    }
    
    if (editingStation) {
      updateMutation.mutate({ id: editingStation.id, data: submitData })
    } else {
      createMutation.mutate(submitData)
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
        <Typography variant="h4">Stations</Typography>
        <Box display="flex" gap={2}>
          <TextField
            label="Search"
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          <Button variant="contained" startIcon={<Add />} onClick={() => handleOpenDialog()}>
            Add Station
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
            <Alert severity="error">Failed to load stations</Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Call Letters</TableCell>
                    <TableCell>Frequency</TableCell>
                    <TableCell>Market</TableCell>
                    <TableCell>Format</TableCell>
                    <TableCell>Ownership</TableCell>
                    <TableCell>Active</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {stations?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No stations found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    stations?.map((station: Station) => (
                      <TableRow key={station.id}>
                        <TableCell>{station.call_letters}</TableCell>
                        <TableCell>{station.frequency || '-'}</TableCell>
                        <TableCell>{station.market || '-'}</TableCell>
                        <TableCell>{station.format || '-'}</TableCell>
                        <TableCell>{station.ownership || '-'}</TableCell>
                        <TableCell>{station.active ? 'Yes' : 'No'}</TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => handleOpenDialog(station)}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(station.id)}>
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

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingStation ? 'Edit Station' : 'Create Station'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Call Letters"
                    required
                    fullWidth
                    value={formData.call_letters}
                    onChange={(e) => setFormData({ ...formData, call_letters: e.target.value.toUpperCase() })}
                    placeholder="KABC"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Frequency"
                    fullWidth
                    value={formData.frequency}
                    onChange={(e) => setFormData({ ...formData, frequency: e.target.value })}
                    placeholder="101.5 FM"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Market"
                    fullWidth
                    value={formData.market}
                    onChange={(e) => setFormData({ ...formData, market: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Format"
                    fullWidth
                    value={formData.format}
                    onChange={(e) => setFormData({ ...formData, format: e.target.value })}
                    placeholder="Top 40, Country, News/Talk"
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Ownership"
                    fullWidth
                    value={formData.ownership}
                    onChange={(e) => setFormData({ ...formData, ownership: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12} sm={6}>
                  <TextField
                    label="Inventory Class"
                    fullWidth
                    value={formData.inventory_class}
                    onChange={(e) => setFormData({ ...formData, inventory_class: e.target.value })}
                  />
                </Grid>
                <Grid item xs={12}>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={formData.active}
                        onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
                      />
                    }
                    label="Active"
                  />
                </Grid>
              </Grid>
              
              <Accordion sx={{ mt: 2 }}>
                <AccordionSummary expandIcon={<ExpandMore />}>
                  <Typography variant="h6">LibreTime Integration</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <Grid container spacing={2}>
                    <Grid item xs={12}>
                      <TextField
                        label="LibreTime API URL"
                        fullWidth
                        value={formData.libretime_config?.api_url || ''}
                        onChange={(e) => setFormData({
                          ...formData,
                          libretime_config: {
                            ...(formData.libretime_config || { api_url: '', api_key: '', public_url: '' }),
                            api_url: e.target.value,
                          },
                        })}
                        placeholder="https://libretime.example.com"
                        helperText="Base URL for LibreTime API (without /api/v2)"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="LibreTime API Key"
                        fullWidth
                        type={showApiKey ? 'text' : 'password'}
                        value={formData.libretime_config?.api_key || ''}
                        onChange={(e) => setFormData({
                          ...formData,
                          libretime_config: {
                            ...(formData.libretime_config || { api_url: '', api_key: '', public_url: '' }),
                            api_key: e.target.value,
                          },
                        })}
                        placeholder="Your LibreTime API key"
                        helperText="API key for authenticating with LibreTime"
                        InputProps={{
                          endAdornment: (
                            <InputAdornment position="end">
                              <MuiIconButton
                                onClick={() => setShowApiKey(!showApiKey)}
                                edge="end"
                              >
                                {showApiKey ? <VisibilityOff /> : <Visibility />}
                              </MuiIconButton>
                            </InputAdornment>
                          ),
                        }}
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="LibreTime Public URL (Optional)"
                        fullWidth
                        value={formData.libretime_config?.public_url || ''}
                        onChange={(e) => setFormData({
                          ...formData,
                          libretime_config: {
                            ...(formData.libretime_config || { api_url: '', api_key: '', public_url: '' }),
                            public_url: e.target.value,
                          },
                        })}
                        placeholder="https://libretime.example.com"
                        helperText="Public-facing URL for LibreTime (for external links)"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <Alert severity="info" sx={{ mt: 1 }}>
                        If not configured, the station will use global LibreTime settings from environment variables.
                      </Alert>
                    </Grid>
                  </Grid>
                </AccordionDetails>
              </Accordion>
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

export default Stations

