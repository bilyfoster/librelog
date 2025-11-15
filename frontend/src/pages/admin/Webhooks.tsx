import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Checkbox,
  Stack,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Send as TestIcon,
  CheckCircle as ActiveIcon,
  Cancel as InactiveIcon,
} from '@mui/icons-material'
import {
  getWebhooks,
  createWebhook,
  updateWebhook,
  deleteWebhook,
  testWebhook,
} from '../../utils/api'

const WEBHOOK_TYPES = [
  { value: 'DISCORD', label: 'Discord' },
  { value: 'SLACK', label: 'Slack' },
  { value: 'CUSTOM', label: 'Custom' },
]

const WEBHOOK_EVENTS = [
  { value: 'LOG_PUBLISHED', label: 'Log Published' },
  { value: 'LOG_LOCKED', label: 'Log Locked' },
  { value: 'ORDER_CREATED', label: 'Order Created' },
  { value: 'ORDER_APPROVED', label: 'Order Approved' },
  { value: 'INVOICE_CREATED', label: 'Invoice Created' },
  { value: 'INVOICE_PAID', label: 'Invoice Paid' },
  { value: 'CAMPAIGN_CREATED', label: 'Campaign Created' },
  { value: 'CAMPAIGN_UPDATED', label: 'Campaign Updated' },
  { value: 'COPY_EXPIRING', label: 'Copy Expiring' },
  { value: 'SPOT_CONFLICT', label: 'Spot Conflict' },
  { value: 'USER_ACTIVITY', label: 'User Activity' },
]

interface WebhookFormDialogProps {
  open: boolean
  webhook?: any
  onClose: () => void
  onSuccess: () => void
}

const WebhookFormDialog: React.FC<WebhookFormDialogProps> = ({
  open,
  webhook,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    name: '',
    webhook_type: 'DISCORD',
    url: '',
    events: [] as string[],
    secret: '',
    active: true,
  })

  React.useEffect(() => {
    if (webhook) {
      setFormData({
        name: webhook.name || '',
        webhook_type: webhook.webhook_type || 'DISCORD',
        url: webhook.url || '',
        events: webhook.events || [],
        secret: '',
        active: webhook.active !== false,
      })
    } else {
      setFormData({
        name: '',
        webhook_type: 'DISCORD',
        url: '',
        events: [],
        secret: '',
        active: true,
      })
    }
  }, [webhook, open])

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async (data: any) => {
      if (webhook) {
        return await updateWebhook(webhook.id, data)
      } else {
        return await createWebhook(data)
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] })
      onSuccess()
    },
    onError: (error: any) => {
      let message = 'Failed to save webhook'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      console.error('Save webhook error:', error)
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    mutation.mutate(formData)
  }

  const toggleEvent = (event: string) => {
    setFormData({
      ...formData,
      events: formData.events.includes(event)
        ? formData.events.filter((e) => e !== event)
        : [...formData.events, event],
    })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {webhook ? 'Edit Webhook' : 'Create Webhook'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 2 }}>
            <TextField
              label="Name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              required
              fullWidth
            />

            <FormControl fullWidth required>
              <InputLabel>Type</InputLabel>
              <Select
                value={formData.webhook_type}
                onChange={(e) =>
                  setFormData({ ...formData, webhook_type: e.target.value })
                }
                label="Type"
              >
                {WEBHOOK_TYPES.map((type) => (
                  <MenuItem key={type.value} value={type.value}>
                    {type.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Webhook URL"
              value={formData.url}
              onChange={(e) => setFormData({ ...formData, url: e.target.value })}
              required
              fullWidth
              type="url"
              helperText="Enter the full webhook URL (e.g., https://discord.com/api/webhooks/...)"
            />

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Events
              </Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {WEBHOOK_EVENTS.map((event) => (
                  <FormControlLabel
                    key={event.value}
                    control={
                      <Checkbox
                        checked={formData.events.includes(event.value)}
                        onChange={() => toggleEvent(event.value)}
                      />
                    }
                    label={event.label}
                  />
                ))}
              </Box>
            </Box>

            <TextField
              label="Secret (optional)"
              value={formData.secret}
              onChange={(e) => setFormData({ ...formData, secret: e.target.value })}
              fullWidth
              type="password"
              helperText="HMAC secret for signature verification"
            />

            <FormControlLabel
              control={
                <Switch
                  checked={formData.active}
                  onChange={(e) =>
                    setFormData({ ...formData, active: e.target.checked })
                  }
                />
              }
              label="Active"
            />

            {mutation.isError && (
              <Alert severity="error">
                {mutation.error instanceof Error
                  ? mutation.error.message
                  : 'Failed to save webhook'}
              </Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={mutation.isPending}>
            {mutation.isPending ? <CircularProgress size={20} /> : webhook ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

const Webhooks: React.FC = () => {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [selectedWebhook, setSelectedWebhook] = useState<any>(null)
  const [activeOnly, setActiveOnly] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const { data: webhooks, isLoading, error } = useQuery({
    queryKey: ['webhooks', { active_only: activeOnly }],
    queryFn: () => getWebhooks({ active_only: activeOnly, limit: 100 }),
    retry: 1,
  })

  const queryClient = useQueryClient()

  const deleteMutation = useMutation({
    mutationFn: deleteWebhook,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete webhook'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete webhook error:', error)
    },
  })

  const testMutation = useMutation({
    mutationFn: testWebhook,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['webhooks'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to test webhook'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Test webhook error:', error)
    },
  })

  const handleCreate = () => {
    setSelectedWebhook(null)
    setDialogOpen(true)
  }

  const handleEdit = (webhook: any) => {
    setSelectedWebhook(webhook)
    setDialogOpen(true)
  }

  const handleDelete = (webhookId: number) => {
    if (window.confirm('Are you sure you want to delete this webhook?')) {
      deleteMutation.mutate(webhookId)
    }
  }

  const handleTest = (webhookId: number) => {
    testMutation.mutate(webhookId)
  }

  const handleDialogClose = () => {
    setDialogOpen(false)
    setSelectedWebhook(null)
  }

  const handleDialogSuccess = () => {
    handleDialogClose()
  }

  return (
    <Box>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Webhooks</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleCreate}
        >
          Create Webhook
        </Button>
      </Box>

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={activeOnly}
                  onChange={(e) => setActiveOnly(e.target.checked)}
                />
              }
              label="Active Only"
            />
          </Box>

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error" sx={{ m: 2 }} action={
              <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['webhooks'] })}>
                Retry
              </Button>
            }>
              Failed to load webhooks: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Name</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>URL</TableCell>
                    <TableCell>Events</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Last Triggered</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {webhooks?.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center">
                        No webhooks found
                      </TableCell>
                    </TableRow>
                  ) : (
                    webhooks?.map((webhook: any) => (
                      <TableRow key={webhook.id}>
                        <TableCell>{webhook.name}</TableCell>
                        <TableCell>
                          <Chip label={webhook.webhook_type} size="small" />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" noWrap sx={{ maxWidth: 300 }}>
                            {webhook.url}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {webhook.events?.slice(0, 3).map((event: string) => (
                              <Chip
                                key={event}
                                label={event.replace('_', ' ')}
                                size="small"
                                variant="outlined"
                              />
                            ))}
                            {webhook.events?.length > 3 && (
                              <Chip
                                label={`+${webhook.events.length - 3}`}
                                size="small"
                                variant="outlined"
                              />
                            )}
                          </Box>
                        </TableCell>
                        <TableCell>
                          {webhook.active ? (
                            <Chip
                              icon={<ActiveIcon />}
                              label="Active"
                              color="success"
                              size="small"
                            />
                          ) : (
                            <Chip
                              icon={<InactiveIcon />}
                              label="Inactive"
                              color="default"
                              size="small"
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {webhook.last_triggered_at
                            ? new Date(webhook.last_triggered_at).toLocaleString()
                            : 'Never'}
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleTest(webhook.id)}
                            title="Test"
                          >
                            <TestIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => handleEdit(webhook)}
                            title="Edit"
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={() => handleDelete(webhook.id)}
                            title="Delete"
                            color="error"
                          >
                            <DeleteIcon fontSize="small" />
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

      <WebhookFormDialog
        open={dialogOpen}
        webhook={selectedWebhook}
        onClose={handleDialogClose}
        onSuccess={handleDialogSuccess}
      />

      {testMutation.isSuccess && (
        <Alert severity="success" sx={{ mt: 2 }}>
          Webhook test sent successfully!
        </Alert>
      )}
      {testMutation.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          Webhook test failed: {testMutation.error instanceof Error ? testMutation.error.message : 'Unknown error'}
        </Alert>
      )}
    </Box>
  )
}

export default Webhooks
