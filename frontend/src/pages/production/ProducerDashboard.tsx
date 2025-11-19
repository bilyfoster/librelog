import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Chip,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
} from '@mui/material'
import { Warning, CheckCircle, Schedule } from '@mui/icons-material'
import { getProductionOrders, getProductionAssignments } from '../../utils/api'

const ProducerDashboard: React.FC = () => {
  const { data: myAssignments, isLoading: assignmentsLoading } = useQuery({
    queryKey: ['production-assignments', 'my-assignments'],
    queryFn: async () => {
      const data = await getProductionAssignments({})
      return Array.isArray(data) ? data : []
    },
  })

  const { data: assignedOrders, isLoading: ordersLoading } = useQuery({
    queryKey: ['production-orders', 'assigned'],
    queryFn: async () => {
      // Get orders assigned to current user
      const assignments = await getProductionAssignments({})
      const assignmentIds = Array.isArray(assignments)
        ? assignments.map((a: any) => a.production_order_id)
        : []
      
      if (assignmentIds.length === 0) return []
      
      const orders = await getProductionOrders({})
      return Array.isArray(orders)
        ? orders.filter((o: any) => assignmentIds.includes(o.id))
        : []
    },
  })

  const isLoading = assignmentsLoading || ordersLoading

  const overdueOrders = assignedOrders
    ? assignedOrders.filter(
        (o: any) =>
          o.deadline &&
          new Date(o.deadline) < new Date() &&
          o.status !== 'DELIVERED' &&
          o.status !== 'COMPLETED'
      )
    : []

  const dueTodayOrders = assignedOrders
    ? assignedOrders.filter(
        (o: any) =>
          o.deadline &&
          new Date(o.deadline).toDateString() === new Date().toDateString() &&
          o.status !== 'DELIVERED' &&
          o.status !== 'COMPLETED'
      )
    : []

  const inProgressOrders = assignedOrders
    ? assignedOrders.filter((o: any) => o.status === 'IN_PROGRESS')
    : []

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleDateString()
  }

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
      default:
        return 'default'
    }
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Producer Dashboard
      </Typography>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Assigned
              </Typography>
              <Typography variant="h4">
                {assignedOrders ? assignedOrders.length : 0}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                In Progress
              </Typography>
              <Typography variant="h4">{inProgressOrders.length}</Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Due Today
              </Typography>
              <Typography variant="h4" color="warning.main">
                {dueTodayOrders.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Overdue
              </Typography>
              <Typography variant="h4" color="error.main">
                {overdueOrders.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {overdueOrders.length > 0 && (
        <Alert severity="error" sx={{ mb: 2 }}>
          <Typography variant="h6">Overdue Orders ({overdueOrders.length})</Typography>
          <Box component="ul" sx={{ mt: 1, mb: 0 }}>
            {overdueOrders.map((order: any) => (
              <li key={order.id}>
                {order.po_number} - {order.client_name} (Due: {formatDate(order.deadline)})
              </li>
            ))}
          </Box>
        </Alert>
      )}

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            My Production Orders
          </Typography>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>PO Number</TableCell>
                  <TableCell>Client</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Deadline</TableCell>
                  <TableCell>Type</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {assignedOrders && assignedOrders.length > 0 ? (
                  assignedOrders.map((order: any) => (
                    <TableRow key={order.id}>
                      <TableCell>{order.po_number}</TableCell>
                      <TableCell>{order.client_name}</TableCell>
                      <TableCell>
                        <Chip
                          label={order.status}
                          color={getStatusColor(order.status) as any}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        {order.deadline ? (
                          <Box display="flex" alignItems="center" gap={1}>
                            {new Date(order.deadline) < new Date() &&
                            order.status !== 'DELIVERED' ? (
                              <Warning color="error" fontSize="small" />
                            ) : null}
                            {formatDate(order.deadline)}
                          </Box>
                        ) : (
                          'N/A'
                        )}
                      </TableCell>
                      <TableCell>{order.order_type}</TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      <Typography variant="body2" color="text.secondary">
                        No assigned production orders
                      </Typography>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    </Box>
  )
}

export default ProducerDashboard

