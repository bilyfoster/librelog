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
  Select,
  FormControl,
  InputLabel,
} from '@mui/material'
import { Visibility, CheckCircle, Schedule, Warning } from '@mui/icons-material'
import { getProductionOrders, updateProductionOrderStatus } from '../../utils/api'
import { useNavigate } from 'react-router-dom'

interface ProductionOrder {
  id: number
  po_number: string
  copy_id: number
  order_id?: number
  advertiser_id: number
  client_name: string
  campaign_title?: string
  deadline?: string
  status: string
  order_type: string
  created_at: string
  updated_at: string
}

const ProductionOrders: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const { data: orders, isLoading, error } = useQuery({
    queryKey: ['production-orders', statusFilter],
    queryFn: async () => {
      const data = await getProductionOrders({
        status: statusFilter || undefined,
        limit: 100,
        offset: 0,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const statusUpdateMutation = useMutation({
    mutationFn: async ({ po_id, new_status }: { po_id: number; new_status: string }) => {
      return await updateProductionOrderStatus(po_id, new_status)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to update status')
    },
  })

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING':
        return 'default'
      case 'ASSIGNED':
        return 'info'
      case 'IN_PROGRESS':
        return 'warning'
      case 'QC':
        return 'secondary'
      case 'COMPLETED':
        return 'success'
      case 'DELIVERED':
        return 'success'
      case 'CANCELLED':
        return 'error'
      default:
        return 'default'
    }
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString()
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
        <Alert severity="error">Failed to load production orders</Alert>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Production Orders</Typography>
        <FormControl size="small" sx={{ minWidth: 200 }}>
          <InputLabel>Filter by Status</InputLabel>
          <Select
            value={statusFilter}
            label="Filter by Status"
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="PENDING">Pending</MenuItem>
            <MenuItem value="ASSIGNED">Assigned</MenuItem>
            <MenuItem value="IN_PROGRESS">In Progress</MenuItem>
            <MenuItem value="QC">QC</MenuItem>
            <MenuItem value="COMPLETED">Completed</MenuItem>
            <MenuItem value="DELIVERED">Delivered</MenuItem>
          </Select>
        </FormControl>
      </Box>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>PO Number</TableCell>
              <TableCell>Client</TableCell>
              <TableCell>Campaign</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Deadline</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {orders && orders.length > 0 ? (
              orders.map((order: ProductionOrder) => (
                <TableRow key={order.id}>
                  <TableCell>{order.po_number}</TableCell>
                  <TableCell>{order.client_name}</TableCell>
                  <TableCell>{order.campaign_title || 'N/A'}</TableCell>
                  <TableCell>
                    <Chip
                      label={order.status}
                      color={getStatusColor(order.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{order.order_type}</TableCell>
                  <TableCell>
                    {order.deadline ? (
                      <Box display="flex" alignItems="center" gap={1}>
                        {new Date(order.deadline) < new Date() && order.status !== 'DELIVERED' ? (
                          <Warning color="error" fontSize="small" />
                        ) : null}
                        {formatDate(order.deadline)}
                      </Box>
                    ) : (
                      'N/A'
                    )}
                  </TableCell>
                  <TableCell>{formatDate(order.created_at)}</TableCell>
                  <TableCell>
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/production/orders/${order.id}`)}
                    >
                      <Visibility />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  <Typography variant="body2" color="text.secondary">
                    No production orders found
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  )
}

export default ProductionOrders

