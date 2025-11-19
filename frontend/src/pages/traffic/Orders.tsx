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
  Chip,
  CircularProgress,
  Alert,
  MenuItem,
} from '@mui/material'
import { Add, Edit, Delete, CheckCircle, ContentCopy } from '@mui/icons-material'
import api from '../../utils/api'
import { getOrdersProxy, getAdvertisersProxy } from '../../utils/api'
import OrderForm from '../../components/orders/OrderForm'

interface Order {
  id: number
  order_number: string
  campaign_id?: number
  advertiser_id: number
  agency_id?: number
  sales_rep_id?: number
  start_date: string
  end_date: string
  spot_lengths?: number[]
  total_spots: number
  rate_type: string
  rates?: any
  total_value: number
  status: string
  approval_status: string
  approved_by?: number
  approved_at?: string
  created_at: string
  updated_at: string
  advertiser_name?: string
  agency_name?: string
}

const Orders: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingOrder, setEditingOrder] = useState<Order | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['orders', statusFilter],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getOrdersProxy({
        limit: 100,
        skip: 0,
        status: statusFilter || undefined,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const { data: advertisers } = useQuery({
    queryKey: ['advertisers'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAdvertisersProxy({ limit: 1000, active_only: false })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const approveMutation = useMutation({
    mutationFn: async (id: number) => {
      const response = await api.post(`/orders/${id}/approve`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to approve order'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Approve order error:', error)
    },
  })

  const duplicateMutation = useMutation({
    mutationFn: async (id: number) => {
      const response = await api.post(`/orders/${id}/duplicate`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to duplicate order'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Duplicate order error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await api.delete(`/orders/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete order'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete order error:', error)
    },
  })

  const getStatusColor = (status: string) => {
    const colors: { [key: string]: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' } = {
      DRAFT: 'default',
      PENDING: 'warning',
      APPROVED: 'info',
      ACTIVE: 'success',
      COMPLETED: 'primary',
      CANCELLED: 'error',
    }
    return colors[status] || 'default'
  }

  return (
    <Box>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Orders</Typography>
        <Box display="flex" gap={2}>
          <TextField
            select
            label="Status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="DRAFT">Draft</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="APPROVED">Approved</MenuItem>
            <MenuItem value="ACTIVE">Active</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="CANCELLED">Cancelled</MenuItem>
          </TextField>
          <Button
            variant="contained"
            color="primary"
            startIcon={<Add />}
            onClick={() => {
              setEditingOrder(null)
              setOpenDialog(true)
            }}
          >
            Add Order
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
            <Alert severity="error" action={
              <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['orders'] })}>
                Retry
              </Button>
            }>
              Failed to load orders: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
          ) : (
            <TableContainer component={Paper}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Order Number</TableCell>
                    <TableCell>Advertiser</TableCell>
                    <TableCell>Start Date</TableCell>
                    <TableCell>End Date</TableCell>
                    <TableCell>Total Spots</TableCell>
                    <TableCell>Total Value</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Approval</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {orders?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={9} align="center">
                        <Typography color="textSecondary" sx={{ py: 3 }}>
                          No orders found
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ) : (
                    orders?.map((order: Order) => (
                      <TableRow key={order.id}>
                        <TableCell>{order.order_number}</TableCell>
                        <TableCell>{order.advertiser_name || `Advertiser ${order.advertiser_id}`}</TableCell>
                        <TableCell>{new Date(order.start_date).toLocaleDateString()}</TableCell>
                        <TableCell>{new Date(order.end_date).toLocaleDateString()}</TableCell>
                        <TableCell>{order.total_spots}</TableCell>
                        <TableCell>${order.total_value.toLocaleString()}</TableCell>
                        <TableCell>
                          <Chip label={order.status} color={getStatusColor(order.status)} size="small" />
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={order.approval_status}
                            color={order.approval_status === 'APPROVED' ? 'success' : 'default'}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          {order.approval_status !== 'APPROVED' && (
                            <IconButton
                              size="small"
                              onClick={() => approveMutation.mutate(order.id)}
                              title="Approve"
                            >
                              <CheckCircle />
                            </IconButton>
                          )}
                          <IconButton
                            size="small"
                            onClick={() => duplicateMutation.mutate(order.id)}
                            title="Duplicate"
                          >
                            <ContentCopy />
                          </IconButton>
                          <IconButton size="small" onClick={() => { setEditingOrder(order); setOpenDialog(true) }}>
                            <Edit />
                          </IconButton>
                          <IconButton size="small" onClick={() => deleteMutation.mutate(order.id)}>
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

      {openDialog && (
        <OrderForm
          open={openDialog}
          onClose={() => {
            setOpenDialog(false)
            setEditingOrder(null)
          }}
          order={editingOrder}
          advertisers={advertisers || []}
        />
      )}
    </Box>
  )
}

export default Orders

