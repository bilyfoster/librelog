import React, { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
  Tooltip,
  Alert,
  CircularProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Category as CategoryIcon,
} from '@mui/icons-material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getDaypartCategoriesProxy,
  createDaypartCategory,
  updateDaypartCategory,
  deleteDaypartCategory,
} from '../../utils/api'

interface DaypartCategory {
  id: number
  name: string
  description?: string
  color?: string
  icon?: string
  sort_order: number
  active: boolean
  created_at: string
  updated_at: string
}

const DaypartCategories: React.FC = () => {
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<DaypartCategory | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    color: '#1976d2',
    icon: '',
    sort_order: 0,
  })

  const queryClient = useQueryClient()

  const { data: categoriesData, isLoading, error } = useQuery({
    queryKey: ['daypart-categories'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getDaypartCategoriesProxy({ active_only: false })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const categories = Array.isArray(categoriesData) ? categoriesData : (categoriesData?.categories || [])

  const createMutation = useMutation({
    mutationFn: createDaypartCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daypart-categories'] })
      handleClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) =>
      updateDaypartCategory(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daypart-categories'] })
      handleClose()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteDaypartCategory,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['daypart-categories'] })
    },
  })

  const handleOpen = (category?: DaypartCategory) => {
    if (category) {
      setEditing(category)
      setFormData({
        name: category.name,
        description: category.description || '',
        color: category.color || '#1976d2',
        icon: category.icon || '',
        sort_order: category.sort_order,
      })
    } else {
      setEditing(null)
      setFormData({
        name: '',
        description: '',
        color: '#1976d2',
        icon: '',
        sort_order: 0,
      })
    }
    setOpen(true)
  }

  const handleClose = () => {
    setOpen(false)
    setEditing(null)
    setFormData({
      name: '',
      description: '',
      color: '#1976d2',
      icon: '',
      sort_order: 0,
    })
  }

  const handleSubmit = () => {
    if (editing) {
      updateMutation.mutate({ id: editing.id, data: formData })
    } else {
      createMutation.mutate(formData)
    }
  }

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this category?')) {
      deleteMutation.mutate(id)
    }
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Daypart Categories</Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load daypart categories: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Daypart Categories</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          New Category
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Color</TableCell>
              <TableCell>Icon</TableCell>
              <TableCell>Sort Order</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {categories.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography color="textSecondary">No categories found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              categories.map((category: DaypartCategory) => (
                <TableRow key={category.id}>
                  <TableCell>
                    <Box display="flex" alignItems="center" gap={1}>
                      {category.icon && <CategoryIcon />}
                      {category.name}
                    </Box>
                  </TableCell>
                  <TableCell>{category.description || '-'}</TableCell>
                  <TableCell>
                    {category.color && (
                      <Box
                        sx={{
                          width: 24,
                          height: 24,
                          backgroundColor: category.color,
                          borderRadius: 1,
                          border: '1px solid #ccc',
                        }}
                      />
                    )}
                  </TableCell>
                  <TableCell>{category.icon || '-'}</TableCell>
                  <TableCell>{category.sort_order}</TableCell>
                  <TableCell>
                    <Chip
                      label={category.active ? 'Active' : 'Inactive'}
                      color={category.active ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit">
                      <IconButton
                        size="small"
                        onClick={() => handleOpen(category)}
                      >
                        <EditIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDelete(category.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {editing ? 'Edit Category' : 'New Category'}
        </DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} pt={1}>
            <TextField
              label="Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              fullWidth
            />
            <TextField
              label="Description"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              multiline
              rows={3}
              fullWidth
            />
            <TextField
              label="Color"
              type="color"
              value={formData.color}
              onChange={(e) => setFormData({ ...formData, color: e.target.value })}
              fullWidth
            />
            <TextField
              label="Icon"
              value={formData.icon}
              onChange={(e) => setFormData({ ...formData, icon: e.target.value })}
              placeholder="Icon name (e.g., 'morning', 'afternoon')"
              fullWidth
            />
            <TextField
              label="Sort Order"
              type="number"
              value={formData.sort_order}
              onChange={(e) =>
                setFormData({ ...formData, sort_order: parseInt(e.target.value) || 0 })
              }
              fullWidth
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleClose}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            variant="contained"
            disabled={!formData.name || createMutation.isPending || updateMutation.isPending}
          >
            {editing ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default DaypartCategories

