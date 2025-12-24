import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Switch,
  FormControlLabel,
  CircularProgress,
  Alert,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import {
  getDaypartsProxy,
  createDaypart,
  updateDaypart,
  deleteDaypart,
  getDaypartCategoriesProxy,
  getStationsProxy,
} from '../../utils/api'
import { MenuItem, Select, FormControl, InputLabel } from '@mui/material'

interface Daypart {
  id?: string
  name: string
  start_time: string
  end_time: string
  description?: string
  category_id?: string
  category_name?: string
  station_id?: string
  station_name?: string
  active: boolean
}

const Dayparts: React.FC = () => {
  const [dayparts, setDayparts] = useState<Daypart[]>([])
  const [categories, setCategories] = useState<any[]>([])
  const [stations, setStations] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editing, setEditing] = useState<Daypart | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    start_time: '06:00:00',
    end_time: '10:00:00',
    description: '',
    category_id: undefined as string | undefined,
    station_id: undefined as string | undefined,
    active: true,
  })

  useEffect(() => {
    loadDayparts()
    loadCategories()
    loadStations()
  }, [])

  const loadDayparts = async () => {
    try {
      setLoading(true)
      setError(null)
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getDaypartsProxy({ active_only: true })
      setDayparts(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load dayparts:', err)
      setError(err instanceof Error ? err.message : 'Failed to load dayparts')
    } finally {
      setLoading(false)
    }
  }

  const loadCategories = async () => {
    try {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getDaypartCategoriesProxy({ active_only: false })
      setCategories(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load categories:', err)
    }
  }

  const loadStations = async () => {
    try {
      // Load active stations for selection
      const data = await getStationsProxy({ active_only: true })
      setStations(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Failed to load stations:', err)
    }
  }

  const handleOpenDialog = (daypart?: Daypart) => {
    if (daypart) {
      setEditing(daypart)
      setFormData({
        name: daypart.name,
        start_time: daypart.start_time,
        end_time: daypart.end_time,
        description: daypart.description || '',
        category_id: daypart.category_id,
        station_id: daypart.station_id,
        active: daypart.active,
      })
    } else {
      setEditing(null)
      setFormData({
        name: '',
        start_time: '06:00:00',
        end_time: '10:00:00',
        description: '',
        category_id: undefined,
        station_id: stations.length === 1 ? stations[0].id : undefined,
        active: true,
      })
    }
    setDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setDialogOpen(false)
    setEditing(null)
  }

  const handleSave = async () => {
    try {
      setErrorMessage(null)
      if (editing) {
        await updateDaypart(editing.id, formData)
      } else {
        await createDaypart(formData)
      }
      await loadDayparts()
      handleCloseDialog()
    } catch (err: any) {
      let message = 'Failed to save daypart'
      if (err?.response?.data?.detail) {
        message = err.response.data.detail
      } else if (err?.response?.data?.message) {
        message = err.response.data.message
      } else if (err?.message) {
        message = err.message
      }
      setErrorMessage(message)
      console.error('Failed to save daypart:', err)
    }
  }

  const handleDelete = async (id?: string) => {
    if (!window.confirm('Are you sure you want to delete this daypart?')) return

    try {
      setErrorMessage(null)
      await deleteDaypart(id)
      await loadDayparts()
    } catch (err: any) {
      let message = 'Failed to delete daypart'
      if (err?.response?.data?.detail) {
        message = err.response.data.detail
      } else if (err?.response?.data?.message) {
        message = err.response.data.message
      } else if (err?.message) {
        message = err.message
      }
      setErrorMessage(message)
      console.error('Failed to delete daypart:', err)
    }
  }

  const formatTime = (timeStr: string) => {
    return timeStr.substring(0, 5) // HH:MM
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>Daypart Management</Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
        <Button onClick={loadDayparts} sx={{ mt: 2 }}>Retry</Button>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4">Daypart Management</Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Add Daypart
          </Button>
        </Box>

        {dayparts.length === 0 ? (
          <Alert severity="info">
            No dayparts configured. Create dayparts to organize your scheduling by time periods.
          </Alert>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Station</TableCell>
                  <TableCell>Start Time</TableCell>
                  <TableCell>End Time</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Description</TableCell>
                  <TableCell>Active</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {dayparts.map(daypart => (
                  <TableRow key={daypart.id}>
                    <TableCell>{daypart.name}</TableCell>
                    <TableCell>{daypart.station_name || '-'}</TableCell>
                    <TableCell>{formatTime(daypart.start_time)}</TableCell>
                    <TableCell>{formatTime(daypart.end_time)}</TableCell>
                    <TableCell>{daypart.category_name || '-'}</TableCell>
                    <TableCell>{daypart.description || '-'}</TableCell>
                    <TableCell>
                      {daypart.active ? 'Yes' : 'No'}
                    </TableCell>
                    <TableCell>
                      <IconButton
                        size="small"
                        onClick={() => handleOpenDialog(daypart)}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleDelete(daypart.id)}
                        color="error"
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editing ? 'Edit Daypart' : 'Create Daypart'}
        </DialogTitle>
        <DialogContent>
          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
              {errorMessage}
            </Alert>
          )}
          <TextField
            fullWidth
            label="Name"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Start Time"
            type="time"
            value={formData.start_time.substring(0, 5)}
            onChange={(e) => setFormData({ ...formData, start_time: e.target.value + ':00' })}
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            fullWidth
            label="End Time"
            type="time"
            value={formData.end_time.substring(0, 5)}
            onChange={(e) => setFormData({ ...formData, end_time: e.target.value + ':00' })}
            margin="normal"
            InputLabelProps={{ shrink: true }}
            required
          />
          <FormControl fullWidth margin="normal" required>
            <InputLabel>Station</InputLabel>
            <Select
              value={formData.station_id || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  station_id: e.target.value ? (e.target.value as string) : undefined,
                })
              }
              label="Station"
            >
              {stations.map((station) => (
                <MenuItem key={station.id} value={station.id}>
                  {station.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth margin="normal">
            <InputLabel>Category</InputLabel>
            <Select
              value={formData.category_id || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  category_id: e.target.value ? (e.target.value as string) : undefined,
                })
              }
              label="Category"
            >
              <MenuItem value="">None</MenuItem>
              {categories.map((cat) => (
                <MenuItem key={cat.id} value={cat.id}>
                  {cat.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            fullWidth
            label="Description"
            value={formData.description}
            onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            margin="normal"
            multiline
            rows={3}
          />
          <FormControlLabel
            control={
              <Switch
                checked={formData.active}
                onChange={(e) => setFormData({ ...formData, active: e.target.checked })}
              />
            }
            label="Active"
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!formData.name || !formData.start_time || !formData.end_time || !formData.station_id}
          >
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Dayparts


