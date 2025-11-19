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
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Rule as RuleIcon,
} from '@mui/icons-material'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  getRotationRulesProxy,
  createRotationRule,
  updateRotationRule,
  deleteRotationRule,
  getDaypartsProxy,
  getCampaignsProxy,
} from '../../utils/api'

interface RotationRule {
  id: number
  name: string
  description?: string
  rotation_type: string
  daypart_id?: number
  campaign_id?: number
  min_separation: number
  max_per_hour?: number
  max_per_day?: number
  priority: number
  active: boolean
  daypart_name?: string
  campaign_name?: string
  created_at: string
  updated_at: string
}

const RotationRules: React.FC = () => {
  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<RotationRule | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    rotation_type: 'SEQUENTIAL',
    daypart_id: undefined as number | undefined,
    campaign_id: undefined as number | undefined,
    min_separation: 0,
    max_per_hour: undefined as number | undefined,
    max_per_day: undefined as number | undefined,
    priority: 0,
  })

  const queryClient = useQueryClient()

  const { data: rulesData, isLoading, error } = useQuery({
    queryKey: ['rotation-rules'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getRotationRulesProxy({ active_only: false })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const { data: daypartsData, error: daypartsError } = useQuery({
    queryKey: ['dayparts'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getDaypartsProxy({ active_only: false })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const { data: campaignsData, error: campaignsError } = useQuery({
    queryKey: ['campaigns'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getCampaignsProxy({ active_only: false })
      return Array.isArray(data?.campaigns) ? data.campaigns : (Array.isArray(data) ? data : [])
    },
    retry: 1,
  })

  const rules = Array.isArray(rulesData) ? rulesData : (rulesData?.rules || rulesData || [])
  const daypartsList = Array.isArray(daypartsData) ? daypartsData : (daypartsData?.dayparts || [])
  const campaignsList = Array.isArray(campaignsData) ? campaignsData : (campaignsData?.campaigns || [])

  const createMutation = useMutation({
    mutationFn: createRotationRule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rotation-rules'] })
      handleClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: any }) =>
      updateRotationRule(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rotation-rules'] })
      handleClose()
    },
  })

  const deleteMutation = useMutation({
    mutationFn: deleteRotationRule,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['rotation-rules'] })
    },
  })

  const handleOpen = (rule?: RotationRule) => {
    if (rule) {
      setEditing(rule)
      setFormData({
        name: rule.name,
        description: rule.description || '',
        rotation_type: rule.rotation_type,
        daypart_id: rule.daypart_id,
        campaign_id: rule.campaign_id,
        min_separation: rule.min_separation,
        max_per_hour: rule.max_per_hour,
        max_per_day: rule.max_per_day,
        priority: rule.priority,
      })
    } else {
      setEditing(null)
      setFormData({
        name: '',
        description: '',
        rotation_type: 'SEQUENTIAL',
        daypart_id: undefined,
        campaign_id: undefined,
        min_separation: 0,
        max_per_hour: undefined,
        max_per_day: undefined,
        priority: 0,
      })
    }
    setOpen(true)
  }

  const handleClose = () => {
    setOpen(false)
    setEditing(null)
  }

  const handleSubmit = () => {
    const data = {
      ...formData,
      daypart_id: formData.daypart_id || undefined,
      campaign_id: formData.campaign_id || undefined,
      max_per_hour: formData.max_per_hour || undefined,
      max_per_day: formData.max_per_day || undefined,
    }
    if (editing) {
      updateMutation.mutate({ id: editing.id, data })
    } else {
      createMutation.mutate(data)
    }
  }

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this rotation rule?')) {
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
        <Typography variant="h4" gutterBottom>Rotation Rules</Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load rotation rules: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }


  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Rotation Rules</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          New Rule
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Daypart</TableCell>
              <TableCell>Campaign</TableCell>
              <TableCell>Min Separation</TableCell>
              <TableCell>Priority</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rules.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography color="textSecondary">No rotation rules found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              rules.map((rule: RotationRule) => (
                <TableRow key={rule.id}>
                  <TableCell>{rule.name}</TableCell>
                  <TableCell>
                    <Chip label={rule.rotation_type} size="small" />
                  </TableCell>
                  <TableCell>{rule.daypart_name || '-'}</TableCell>
                  <TableCell>{rule.campaign_name || '-'}</TableCell>
                  <TableCell>{rule.min_separation}</TableCell>
                  <TableCell>{rule.priority}</TableCell>
                  <TableCell>
                    <Chip
                      label={rule.active ? 'Active' : 'Inactive'}
                      color={rule.active ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Edit">
                      <IconButton size="small" onClick={() => handleOpen(rule)}>
                        <EditIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={() => handleDelete(rule.id)}
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

      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>
          {editing ? 'Edit Rotation Rule' : 'New Rotation Rule'}
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
            <FormControl fullWidth>
              <InputLabel>Rotation Type</InputLabel>
              <Select
                value={formData.rotation_type}
                onChange={(e) =>
                  setFormData({ ...formData, rotation_type: e.target.value })
                }
                label="Rotation Type"
              >
                <MenuItem value="SEQUENTIAL">Sequential</MenuItem>
                <MenuItem value="RANDOM">Random</MenuItem>
                <MenuItem value="WEIGHTED">Weighted</MenuItem>
                <MenuItem value="EVEN">Even Distribution</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Daypart (Optional)</InputLabel>
              <Select
                value={formData.daypart_id || ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    daypart_id: e.target.value ? (e.target.value as number) : undefined,
                  })
                }
                label="Daypart (Optional)"
              >
                <MenuItem value="">None</MenuItem>
                {daypartsList.map((dp: any) => (
                  <MenuItem key={dp.id} value={dp.id}>
                    {dp.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Campaign (Optional)</InputLabel>
              <Select
                value={formData.campaign_id || ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    campaign_id: e.target.value ? (e.target.value as number) : undefined,
                  })
                }
                label="Campaign (Optional)"
              >
                <MenuItem value="">None</MenuItem>
                {campaignsList.map((camp: any) => (
                  <MenuItem key={camp.id} value={camp.id}>
                    {camp.name || camp.advertiser}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Min Separation (spots)"
              type="number"
              value={formData.min_separation}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  min_separation: parseInt(e.target.value) || 0,
                })
              }
              fullWidth
            />
            <TextField
              label="Max Per Hour (optional)"
              type="number"
              value={formData.max_per_hour || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  max_per_hour: e.target.value ? parseInt(e.target.value) : undefined,
                })
              }
              fullWidth
            />
            <TextField
              label="Max Per Day (optional)"
              type="number"
              value={formData.max_per_day || ''}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  max_per_day: e.target.value ? parseInt(e.target.value) : undefined,
                })
              }
              fullWidth
            />
            <TextField
              label="Priority"
              type="number"
              value={formData.priority}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  priority: parseInt(e.target.value) || 0,
                })
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

export default RotationRules

