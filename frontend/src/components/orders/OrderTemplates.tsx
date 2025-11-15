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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  CircularProgress,
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import api from '../../utils/api'

interface OrderTemplate {
  id: number
  name: string
  description?: string
  default_spot_lengths?: number[]
  default_rate_type?: string
  default_rates?: any
  created_by: number
  created_at: string
  updated_at: string
}

const OrderTemplates: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingTemplate, setEditingTemplate] = useState<OrderTemplate | null>(null)
  const queryClient = useQueryClient()

  const { data: templates, isLoading } = useQuery({
    queryKey: ['order-templates'],
    queryFn: async () => {
      const response = await api.get('/orders/templates')
      return response.data
    },
  })

  const createMutation = useMutation({
    mutationFn: async (data: Partial<OrderTemplate>) => {
      const response = await api.post('/orders/templates', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['order-templates'] })
      setOpenDialog(false)
      setEditingTemplate(null)
    },
  })

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const data: Partial<OrderTemplate> = {
      name: formData.get('name') as string,
      description: formData.get('description') as string || undefined,
      default_rate_type: formData.get('default_rate_type') as string || undefined,
    }

    createMutation.mutate(data)
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Order Templates</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<Add />}
          onClick={() => {
            setEditingTemplate(null)
            setOpenDialog(true)
          }}
        >
          Add Template
        </Button>
      </Box>

      <Card>
        <CardContent>
          {isLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>Rate Type</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {templates?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={4} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No templates found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    templates?.map((template: OrderTemplate) => (
                      <TableRow key={template.id}>
                        <TableCell>{template.name}</TableCell>
                        <TableCell>{template.description || 'N/A'}</TableCell>
                        <TableCell>{template.default_rate_type || 'N/A'}</TableCell>
                        <TableCell>
                          <IconButton size="small" onClick={() => { setEditingTemplate(template); setOpenDialog(true) }}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small">
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

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingTemplate ? 'Edit Template' : 'Add Template'}</DialogTitle>
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
              <TextField
                name="name"
                label="Name"
                required
                fullWidth
                defaultValue={editingTemplate?.name || ''}
              />
              <TextField
                name="description"
                label="Description"
                multiline
                rows={3}
                fullWidth
                defaultValue={editingTemplate?.description || ''}
              />
              <TextField
                name="default_rate_type"
                label="Default Rate Type"
                select
                fullWidth
                defaultValue={editingTemplate?.default_rate_type || 'ROS'}
              >
                <MenuItem value="ROS">Run of Schedule</MenuItem>
                <MenuItem value="DAYPART">Daypart</MenuItem>
                <MenuItem value="PROGRAM">Program</MenuItem>
                <MenuItem value="FIXED_TIME">Fixed Time</MenuItem>
              </TextField>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={createMutation.isPending}>
              {createMutation.isPending ? 'Saving...' : 'Save'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  )
}

export default OrderTemplates

