import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Chip,
  CircularProgress,
  Alert,
  Grid,
  Divider,
  TextField,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Tabs,
  Tab,
  List,
  ListItem,
  ListItemText,
} from '@mui/material'
import {
  ArrowBack,
  CheckCircle,
  Schedule,
  Person,
  Assignment,
  Comment,
  History,
} from '@mui/icons-material'
import {
  getProductionOrder,
  updateProductionOrder,
  updateProductionOrderStatus,
  assignProductionOrder,
  deliverProductionOrder,
  getProductionAssignments,
} from '../../utils/api'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const ProductionOrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [tabValue, setTabValue] = useState(0)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const { data: order, isLoading, error } = useQuery({
    queryKey: ['production-order', id],
    queryFn: async () => {
      if (!id) throw new Error('No ID provided')
      return await getProductionOrder(parseInt(id))
    },
    enabled: !!id,
  })

  const { data: assignments } = useQuery({
    queryKey: ['production-assignments', id],
    queryFn: async () => {
      if (!id) return []
      return await getProductionAssignments({ production_order_id: parseInt(id) })
    },
    enabled: !!id,
  })

  const statusUpdateMutation = useMutation({
    mutationFn: async (new_status: string) => {
      if (!id) throw new Error('No ID provided')
      return await updateProductionOrderStatus(parseInt(id), new_status)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-order', id] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to update status')
    },
  })

  const deliverMutation = useMutation({
    mutationFn: async () => {
      if (!id) throw new Error('No ID provided')
      return await deliverProductionOrder(parseInt(id))
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['production-order', id] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to deliver')
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
      default:
        return 'default'
    }
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'N/A'
    return new Date(dateString).toLocaleString()
  }

  const canUpdateStatus = (currentStatus: string, newStatus: string) => {
    const validTransitions: Record<string, string[]> = {
      PENDING: ['ASSIGNED', 'CANCELLED'],
      ASSIGNED: ['IN_PROGRESS', 'CANCELLED'],
      IN_PROGRESS: ['QC', 'CANCELLED'],
      QC: ['COMPLETED', 'IN_PROGRESS'],
      COMPLETED: ['DELIVERED'],
    }
    return validTransitions[currentStatus]?.includes(newStatus) || false
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error || !order) {
    return (
      <Box p={3}>
        <Alert severity="error">Failed to load production order</Alert>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/production/orders')} sx={{ mt: 2 }}>
          Back to List
        </Button>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <Button startIcon={<ArrowBack />} onClick={() => navigate('/production/orders')}>
          Back
        </Button>
        <Typography variant="h4">{order.po_number}</Typography>
        {order.order_type === 'spec' && (
          <Chip
            label="SPEC SPOT"
            color="warning"
            size="medium"
            sx={{ fontWeight: 'bold' }}
          />
        )}
        <Chip
          label={order.status}
          color={getStatusColor(order.status) as any}
          size="medium"
        />
      </Box>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
                <Tab label="Details" />
                <Tab label="Assignments" />
                <Tab label="Comments" />
                <Tab label="History" />
              </Tabs>

              <TabPanel value={tabValue} index={0}>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Client Name
                    </Typography>
                    <Typography variant="body1">{order.client_name}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Campaign
                    </Typography>
                    <Typography variant="body1">{order.campaign_title || 'N/A'}</Typography>
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Order Type
                    </Typography>
                    <Box display="flex" alignItems="center" gap={1}>
                      <Typography variant="body1">{order.order_type}</Typography>
                      {order.order_type === 'spec' && (
                        <Chip label="SPEC" color="warning" size="small" />
                      )}
                    </Box>
                  </Grid>
                  {order.order_type === 'spec' && (
                    <Grid item xs={12}>
                      <Alert severity="info" sx={{ mt: 1 }}>
                        <Typography variant="body2">
                          <strong>Spec Spot:</strong> This production order was created without an associated sales order. 
                          It can be used for pitches, demos, or speculative work. When the client commits, you can link it to an order later.
                        </Typography>
                      </Alert>
                    </Grid>
                  )}
                  {!order.order_id && order.order_type !== 'spec' && (
                    <Grid item xs={12}>
                      <Alert severity="warning" sx={{ mt: 1 }}>
                        <Typography variant="body2">
                          <strong>No Order:</strong> This production order is not linked to a sales order.
                        </Typography>
                      </Alert>
                    </Grid>
                  )}
                  {order.order_id && (
                    <Grid item xs={12} sm={6}>
                      <Typography variant="subtitle2" color="text.secondary">
                        Sales Order ID
                      </Typography>
                      <Typography variant="body1">#{order.order_id}</Typography>
                    </Grid>
                  )}
                  <Grid item xs={12} sm={6}>
                    <Typography variant="subtitle2" color="text.secondary">
                      Deadline
                    </Typography>
                    <Typography variant="body1">
                      {order.deadline ? formatDate(order.deadline) : 'N/A'}
                    </Typography>
                  </Grid>
                  {order.instructions && (
                    <Grid item xs={12}>
                      <Typography variant="subtitle2" color="text.secondary">
                        Instructions
                      </Typography>
                      <Typography variant="body1">{order.instructions}</Typography>
                    </Grid>
                  )}
                  {order.spot_lengths && (
                    <Grid item xs={12}>
                      <Typography variant="subtitle2" color="text.secondary">
                        Spot Lengths
                      </Typography>
                      <Typography variant="body1">
                        {order.spot_lengths.join(', ')} seconds
                      </Typography>
                    </Grid>
                  )}
                </Grid>
              </TabPanel>

              <TabPanel value={tabValue} index={1}>
                {assignments && assignments.length > 0 ? (
                  <List>
                    {assignments.map((assignment: any) => (
                      <ListItem key={assignment.id}>
                        <ListItemText
                          primary={assignment.assignment_type}
                          secondary={`Status: ${assignment.status} - Assigned: ${formatDate(assignment.assigned_at)}`}
                        />
                        <Chip label={assignment.status} size="small" />
                      </ListItem>
                    ))}
                  </List>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    No assignments yet
                  </Typography>
                )}
              </TabPanel>

              <TabPanel value={tabValue} index={2}>
                <Typography variant="body2" color="text.secondary">
                  Comments feature coming soon
                </Typography>
              </TabPanel>

              <TabPanel value={tabValue} index={3}>
                <Typography variant="body2" color="text.secondary">
                  History feature coming soon
                </Typography>
              </TabPanel>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Actions
              </Typography>
              <Box display="flex" flexDirection="column" gap={2}>
                {order.status === 'COMPLETED' && (
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={() => deliverMutation.mutate()}
                    disabled={deliverMutation.isPending}
                  >
                    Deliver to Traffic
                  </Button>
                )}
                {order.status === 'QC' && (
                  <Button
                    variant="contained"
                    color="success"
                    onClick={() => statusUpdateMutation.mutate('COMPLETED')}
                    disabled={statusUpdateMutation.isPending}
                  >
                    Approve QC
                  </Button>
                )}
                {order.status === 'IN_PROGRESS' && (
                  <Button
                    variant="outlined"
                    onClick={() => statusUpdateMutation.mutate('QC')}
                    disabled={statusUpdateMutation.isPending}
                  >
                    Send to QC
                  </Button>
                )}
                {order.status === 'ASSIGNED' && (
                  <Button
                    variant="outlined"
                    onClick={() => statusUpdateMutation.mutate('IN_PROGRESS')}
                    disabled={statusUpdateMutation.isPending}
                  >
                    Start Production
                  </Button>
                )}
              </Box>
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Status Information
              </Typography>
              <Box display="flex" flexDirection="column" gap={1}>
                <Typography variant="body2">
                  <strong>Created:</strong> {formatDate(order.created_at)}
                </Typography>
                <Typography variant="body2">
                  <strong>Updated:</strong> {formatDate(order.updated_at)}
                </Typography>
                {order.completed_at && (
                  <Typography variant="body2">
                    <strong>Completed:</strong> {formatDate(order.completed_at)}
                  </Typography>
                )}
                {order.delivered_at && (
                  <Typography variant="body2">
                    <strong>Delivered:</strong> {formatDate(order.delivered_at)}
                  </Typography>
                )}
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}

export default ProductionOrderDetail

