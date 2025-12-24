import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import api from '../../utils/api'
import { getOrdersProxy, getAdvertisersProxy } from '../../utils/api'
import OrderForm from '../../components/orders/OrderForm'
import {
  Box,
  Typography,
  Button,
  Alert,
  Card,
  CardContent,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Chip,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Stack,
} from '@mui/material'
import {
  Add,
  Edit,
  Check,
  ContentCopy,
  Delete,
} from '@mui/icons-material'

interface Order {
  id?: string
  order_number: string
  order_name?: string
  campaign_id?: string
  advertiser_id?: string
  agency_id?: string
  sales_rep_id?: string
  sales_team?: string
  sales_office?: string
  sales_region?: string
  stations?: string[]
  cluster?: string
  order_type?: string
  start_date: string
  end_date: string
  spot_lengths?: number[]
  total_spots: number
  rate_type: string
  rates?: any
  gross_amount?: number
  net_amount?: number
  total_value: number
  agency_commission_percent?: number
  agency_commission_amount?: number
  agency_discount?: number
  cash_discount?: number
  trade_barter?: boolean
  trade_value?: number
  taxable?: boolean
  billing_cycle?: string
  invoice_type?: string
  coop_sponsor?: string
  coop_percent?: number
  client_po_number?: string
  billing_address?: string
  billing_contact?: string
  billing_contact_email?: string
  billing_contact_phone?: string
  political_class?: string
  political_window_flag?: boolean
  contract_reference?: string
  insertion_order_number?: string
  regulatory_notes?: string
  fcc_id?: string
  required_disclaimers?: string
  status: string
  approval_status: string
  approved_by?: number
  approved_at?: string
  traffic_ready?: boolean
  billing_ready?: boolean
  locked?: boolean
  revision_number?: number
  created_at: string
  updated_at: string
  advertiser_name?: string
  agency_name?: string
  sales_rep_name?: string
}

const Orders: React.FC = () => {
  const navigate = useNavigate()
  const [openDialog, setOpenDialog] = useState(false)
  const [editingOrder, setEditingOrder] = useState<Order | null>(null)
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['orders', statusFilter],
    queryFn: async () => {
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
      const data = await getAdvertisersProxy({ limit: 1000, active_only: false })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const approveMutation = useMutation({
    mutationFn: async (id?: string) => {
      const response = await api.post(`/orders/${id}/approve`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to approve order')
    },
  })

  const duplicateMutation = useMutation({
    mutationFn: async (id?: string) => {
      const response = await api.post(`/orders/${id}/duplicate`)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to duplicate order')
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
      await api.delete(`/orders/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to delete order')
    },
  })

  const getStatusColor = (status: string): 'default' | 'primary' | 'success' | 'warning' | 'error' => {
    const colors: { [key: string]: 'default' | 'primary' | 'success' | 'warning' | 'error' } = {
      DRAFT: 'default',
      PENDING: 'warning',
      APPROVED: 'primary',
      ACTIVE: 'success',
      COMPLETED: 'primary',
      CANCELLED: 'error',
    }
    return colors[status] || 'default'
  }

  const handleNewOrder = () => {
    setEditingOrder(null)
    setOpenDialog(true)
  }

  const handleEditOrder = (order: Order) => {
    setEditingOrder(order)
    setOpenDialog(true)
  }

  const handleDeleteOrder = (id?: string) => {
    if (confirm('Are you sure you want to delete this order?')) {
      deleteMutation.mutate(id)
    }
  }

  const formatCurrency = (amount: number | undefined) => {
    if (amount === undefined || amount === null) return '$0.00'
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString()
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" component="h1" sx={{ fontWeight: 500 }}>
          Orders
        </Typography>
        <Button
          variant="contained"
          startIcon={<Add />}
          onClick={handleNewOrder}
        >
          New Order
        </Button>
      </Box>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <FormControl fullWidth>
            <InputLabel>Filter by Status</InputLabel>
            <Select
              value={statusFilter}
              label="Filter by Status"
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <MenuItem value="">All Statuses</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="PENDING">Pending</MenuItem>
              <MenuItem value="APPROVED">Approved</MenuItem>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="COMPLETED">Completed</MenuItem>
              <MenuItem value="CANCELLED">Cancelled</MenuItem>
            </Select>
          </FormControl>
        </CardContent>
      </Card>

      {isLoading ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Alert severity="error">
          Failed to load orders: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      ) : !orders || orders.length === 0 ? (
        <Card>
          <CardContent sx={{ py: 6, textAlign: 'center' }}>
            <Typography color="text.secondary">
              No orders found. Create your first order to get started.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ backgroundColor: 'action.hover' }}>
                <TableCell sx={{ fontWeight: 500 }}>Order #</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>Name</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>Advertiser</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>Agency</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>Sales Rep</TableCell>
                <TableCell sx={{ fontWeight: 500 }}>Dates</TableCell>
                <TableCell align="right" sx={{ fontWeight: 500 }}>Total Value</TableCell>
                <TableCell align="center" sx={{ fontWeight: 500 }}>Status</TableCell>
                <TableCell align="center" sx={{ fontWeight: 500 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.map((order) => (
                <TableRow
                  key={order.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => navigate(`/traffic/orders/${order.id}`)}
                >
                  <TableCell>{order.order_number}</TableCell>
                  <TableCell>{order.order_name || '-'}</TableCell>
                  <TableCell>{order.advertiser_name || '-'}</TableCell>
                  <TableCell>{order.agency_name || '-'}</TableCell>
                  <TableCell>{order.sales_rep_name || '-'}</TableCell>
                  <TableCell>
                    {formatDate(order.start_date)} - {formatDate(order.end_date)}
                  </TableCell>
                  <TableCell align="right">
                    {formatCurrency(order.total_value)}
                  </TableCell>
                  <TableCell align="center">
                    <Chip
                      label={order.status}
                      color={getStatusColor(order.status)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="center">
                    <Stack
                      direction="row"
                      spacing={0.5}
                      justifyContent="center"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <IconButton
                        size="small"
                        onClick={() => handleEditOrder(order)}
                        title="Edit"
                      >
                        <Edit fontSize="small" />
                      </IconButton>
                      {order.approval_status === 'PENDING' && (
                        <IconButton
                          size="small"
                          onClick={() => approveMutation.mutate(order.id)}
                          title="Approve"
                          color="success"
                        >
                          <Check fontSize="small" />
                        </IconButton>
                      )}
                      <IconButton
                        size="small"
                        onClick={() => duplicateMutation.mutate(order.id)}
                        title="Duplicate"
                      >
                        <ContentCopy fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={() => handleDeleteOrder(order.id)}
                        title="Delete"
                        color="error"
                      >
                        <Delete fontSize="small" />
                      </IconButton>
                    </Stack>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <OrderForm
        open={openDialog}
        onClose={() => {
          setOpenDialog(false)
          setEditingOrder(null)
        }}
        order={editingOrder}
        advertisers={advertisers || []}
      />
    </Box>
  )
}

export default Orders
