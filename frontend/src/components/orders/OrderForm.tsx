import React, { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  MenuItem,
  Grid,
  Typography,
  FormControl,
  InputLabel,
  Select,
  FormHelperText,
} from '@mui/material'
import api from '../../utils/api'
import { getAgenciesProxy, getSalesRepsProxy } from '../../utils/api'
import InfoButton from '../Help/InfoButton'

interface Order {
  id?: number
  order_number?: string
  campaign_id?: number
  advertiser_id?: number
  agency_id?: number
  sales_rep_id?: number
  start_date?: string
  end_date?: string
  spot_lengths?: number[]
  total_spots?: number
  rate_type?: string
  rates?: any
  total_value?: number
  status?: string
  approval_status?: string
}

interface OrderFormProps {
  open: boolean
  onClose: () => void
  order?: Order | null
  advertisers?: any[]
}

const OrderForm: React.FC<OrderFormProps> = ({ open, onClose, order, advertisers = [] }) => {
  const [formData, setFormData] = useState<Partial<Order>>({
    order_number: '',
    advertiser_id: undefined,
    agency_id: undefined,
    sales_rep_id: undefined,
    start_date: '',
    end_date: '',
    spot_lengths: [30, 60],
    total_spots: 0,
    rate_type: 'ROS',
    rates: {},
    total_value: 0,
    status: 'DRAFT',
    approval_status: 'NOT_REQUIRED',
  })
  const queryClient = useQueryClient()

  const { data: agencies } = useQuery({
    queryKey: ['agencies'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAgenciesProxy({ limit: 1000 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesReps } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getSalesRepsProxy({ limit: 1000 })
      return Array.isArray(data) ? data : []
    },
  })


  useEffect(() => {
    if (order) {
      setFormData({
        ...order,
        start_date: order.start_date ? order.start_date.split('T')[0] : '',
        end_date: order.end_date ? order.end_date.split('T')[0] : '',
      })
    } else {
      setFormData({
        order_number: '',
        advertiser_id: undefined,
        agency_id: undefined,
        sales_rep_id: undefined,
        start_date: '',
        end_date: '',
        spot_lengths: [30, 60],
        total_spots: 0,
        rate_type: 'ROS',
        rates: {},
        total_value: 0,
        status: 'DRAFT',
        approval_status: 'NOT_REQUIRED',
      })
    }
  }, [order, open])

  const createMutation = useMutation({
    mutationFn: async (data: Partial<Order>) => {
      const response = await api.post('/orders', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      onClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: number; data: Partial<Order> }) => {
      const response = await api.put(`/orders/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      onClose()
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // Prepare data - omit order_number if it's empty (for auto-generation)
    const submitData = { ...formData }
    if (!submitData.order_number || submitData.order_number.trim() === '') {
      delete submitData.order_number
    }
    
    if (order?.id) {
      updateMutation.mutate({ id: order.id, data: submitData })
    } else {
      createMutation.mutate(submitData)
    }
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{order ? 'Edit Order' : 'Create Order'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Order Number"
                    fullWidth
                    value={formData.order_number || ''}
                    onChange={(e) => setFormData({ ...formData, order_number: e.target.value || undefined })}
                    helperText={!order?.id ? "Leave blank to auto-generate (e.g., 20241215-0001)" : undefined}
                    placeholder="Auto-generated if left blank"
                  />
                  <InfoButton fieldPath="order.order_number" label="Order Number" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Advertiser"
                  select
                  required
                  fullWidth
                  value={formData.advertiser_id || ''}
                  onChange={(e) => setFormData({ ...formData, advertiser_id: parseInt(e.target.value) })}
                >
                  {advertisers.map((adv) => (
                    <MenuItem key={adv.id} value={adv.id}>
                      {adv.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Agency"
                  select
                  fullWidth
                  value={formData.agency_id || ''}
                  onChange={(e) => setFormData({ ...formData, agency_id: e.target.value ? parseInt(e.target.value) : undefined })}
                >
                  <MenuItem value="">None</MenuItem>
                  {agencies?.map((ag) => (
                    <MenuItem key={ag.id} value={ag.id}>
                      {ag.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Sales Rep"
                  select
                  fullWidth
                  value={formData.sales_rep_id || ''}
                  onChange={(e) => setFormData({ ...formData, sales_rep_id: e.target.value ? parseInt(e.target.value) : undefined })}
                >
                  <MenuItem value="">None</MenuItem>
                  {salesReps?.map((rep) => (
                    <MenuItem key={rep.id} value={rep.id}>
                      {rep.username || `Rep ${rep.id}`}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Start Date"
                  type="date"
                  required
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={formData.start_date || ''}
                  onChange={(e) => setFormData({ ...formData, start_date: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="End Date"
                  type="date"
                  required
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={formData.end_date || ''}
                  onChange={(e) => setFormData({ ...formData, end_date: e.target.value })}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Total Spots"
                    type="number"
                    fullWidth
                    value={formData.total_spots || 0}
                    onChange={(e) => setFormData({ ...formData, total_spots: parseInt(e.target.value) || 0 })}
                  />
                  <InfoButton fieldPath="order.total_spots" label="Total Spots" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Rate Type"
                    select
                    required
                    fullWidth
                    value={formData.rate_type || 'ROS'}
                    onChange={(e) => setFormData({ ...formData, rate_type: e.target.value })}
                  >
                    <MenuItem value="ROS">Run of Schedule</MenuItem>
                    <MenuItem value="DAYPART">Daypart</MenuItem>
                    <MenuItem value="PROGRAM">Program</MenuItem>
                    <MenuItem value="FIXED_TIME">Fixed Time</MenuItem>
                  </TextField>
                  <InfoButton fieldPath="order.rate_type" label="Rate Type" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Status"
                    select
                    fullWidth
                    value={formData.status || 'DRAFT'}
                    onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                  >
                    <MenuItem value="DRAFT">Draft</MenuItem>
                    <MenuItem value="PENDING">Pending</MenuItem>
                    <MenuItem value="APPROVED">Approved</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="COMPLETED">Completed</MenuItem>
                    <MenuItem value="CANCELLED">Cancelled</MenuItem>
                  </TextField>
                  <InfoButton fieldPath="order.status" label="Order Status" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Total Value"
                  type="number"
                  fullWidth
                  value={formData.total_value || 0}
                  onChange={(e) => setFormData({ ...formData, total_value: parseFloat(e.target.value) || 0 })}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={createMutation.isPending || updateMutation.isPending}>
            {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default OrderForm

