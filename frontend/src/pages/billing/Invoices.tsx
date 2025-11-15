import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  IconButton,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Chip,
  Tooltip,
  Alert,
  CircularProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Visibility as VisibilityIcon,
  Send as SendIcon,
  CheckCircle as CheckCircleIcon,
  Download as DownloadIcon,
} from '@mui/icons-material'
import {
  getInvoices,
  sendInvoice,
  markInvoicePaid,
  getAgingReport,
} from '../../utils/api'
import api from '../../utils/api'
import InvoiceDetailDialog from '../../components/billing/InvoiceDetailDialog'
import InvoiceFormDialog from '../../components/billing/InvoiceFormDialog'
import ARAgingDashboard from '../../components/billing/ARAgingDashboard'

interface Invoice {
  id: number
  invoice_number: string
  advertiser_id: number
  agency_id?: number
  order_id?: number
  campaign_id?: number
  invoice_date: string
  due_date: string
  subtotal: number | string
  tax: number | string
  total: number | string
  status: string
  payment_terms?: string
  notes?: string
  created_at: string
  updated_at: string
}

const Invoices: React.FC = () => {
  const [advertiserFilter, setAdvertiserFilter] = useState<number | ''>('')
  const [statusFilter, setStatusFilter] = useState<string>('all')
  const [detailOpen, setDetailOpen] = useState(false)
  const [formOpen, setFormOpen] = useState(false)
  const [selectedInvoice, setSelectedInvoice] = useState<Invoice | null>(null)
  const [showAging, setShowAging] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: invoices, isLoading, error } = useQuery({
    queryKey: ['invoices', advertiserFilter, statusFilter],
    queryFn: async () => {
      const params: any = { limit: 100 }
      if (advertiserFilter) params.advertiser_id = advertiserFilter
      if (statusFilter !== 'all') params.status_filter = statusFilter
      const data = await getInvoices(params)
      return data
    },
    retry: 1,
  })

  const { data: advertisers } = useQuery({
    queryKey: ['advertisers'],
    queryFn: async () => {
      const response = await api.get('/advertisers', { params: { limit: 100 } })
      return response.data.advertisers || response.data || []
    },
  })

  const sendMutation = useMutation({
    mutationFn: async (invoiceId: number) => {
      await sendInvoice(invoiceId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to send invoice'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Send invoice error:', error)
    },
  })

  const markPaidMutation = useMutation({
    mutationFn: async (invoiceId: number) => {
      await markInvoicePaid(invoiceId, {})
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to mark invoice as paid'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Mark paid error:', error)
    },
  })

  const handleView = (invoice: Invoice) => {
    setSelectedInvoice(invoice)
    setDetailOpen(true)
  }

  const handleEdit = (invoice: Invoice) => {
    setSelectedInvoice(invoice)
    setFormOpen(true)
  }

  const handleSend = async (invoiceId: number) => {
    if (window.confirm('Send this invoice to the advertiser?')) {
      sendMutation.mutate(invoiceId)
    }
  }

  const handleMarkPaid = async (invoiceId: number) => {
    if (window.confirm('Mark this invoice as paid?')) {
      markPaidMutation.mutate(invoiceId)
    }
  }

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'DRAFT':
        return 'default'
      case 'SENT':
        return 'info'
      case 'PAID':
        return 'success'
      case 'OVERDUE':
        return 'error'
      case 'CANCELLED':
        return 'default'
      default:
        return 'default'
    }
  }

  const isOverdue = (invoice: Invoice) => {
    const dueDate = new Date(invoice.due_date)
    const today = new Date()
    return dueDate < today && invoice.status !== 'PAID' && invoice.status !== 'CANCELLED'
  }

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" action={
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['invoices'] })}>
            Retry
          </Button>
        }>
          Failed to load invoices: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h4">Invoices</Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              onClick={() => setShowAging(!showAging)}
            >
              {showAging ? 'Hide' : 'Show'} AR Aging
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => {
                setSelectedInvoice(null)
                setFormOpen(true)
              }}
            >
              Create Invoice
            </Button>
          </Box>
        </Box>

        {/* AR Aging Dashboard */}
        {showAging && (
          <Box sx={{ mb: 3 }}>
            <ARAgingDashboard />
          </Box>
        )}

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Advertiser</InputLabel>
            <Select
              value={advertiserFilter}
              onChange={(e) => setAdvertiserFilter(e.target.value as number | '')}
              label="Advertiser"
            >
              <MenuItem value="">All Advertisers</MenuItem>
              {advertisers?.map((advertiser: any) => (
                <MenuItem key={advertiser.id} value={advertiser.id}>
                  {advertiser.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              label="Status"
            >
              <MenuItem value="all">All</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="SENT">Sent</MenuItem>
              <MenuItem value="PAID">Paid</MenuItem>
              <MenuItem value="OVERDUE">Overdue</MenuItem>
              <MenuItem value="CANCELLED">Cancelled</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Invoices Table */}
        {invoices && invoices.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Invoice #</TableCell>
                  <TableCell>Advertiser</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Due Date</TableCell>
                  <TableCell>Total</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {invoices.map((invoice: Invoice) => (
                  <TableRow key={invoice.id}>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {invoice.invoice_number}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {advertisers?.find((a: any) => a.id === invoice.advertiser_id)?.name || 'N/A'}
                    </TableCell>
                    <TableCell>
                      {new Date(invoice.invoice_date).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <Box>
                        {new Date(invoice.due_date).toLocaleDateString()}
                        {isOverdue(invoice) && (
                          <Chip label="Overdue" size="small" color="error" sx={{ ml: 1 }} />
                        )}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {formatCurrency(invoice.total)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={invoice.status}
                        size="small"
                        color={getStatusColor(invoice.status) as any}
                      />
                    </TableCell>
                    <TableCell>
                      <Tooltip title="View Details">
                        <IconButton size="small" onClick={() => handleView(invoice)}>
                          <VisibilityIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => handleEdit(invoice)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {invoice.status === 'DRAFT' && (
                        <Tooltip title="Send Invoice">
                          <IconButton
                            size="small"
                            onClick={() => handleSend(invoice.id)}
                            disabled={sendMutation.isPending}
                          >
                            <SendIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      {invoice.status !== 'PAID' && invoice.status !== 'CANCELLED' && (
                        <Tooltip title="Mark as Paid">
                          <IconButton
                            size="small"
                            onClick={() => handleMarkPaid(invoice.id)}
                            disabled={markPaidMutation.isPending}
                            color="success"
                          >
                            <CheckCircleIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No invoices found. Create your first invoice to get started.</Alert>
        )}
      </Paper>

      {/* Invoice Detail Dialog */}
      {selectedInvoice && (
        <InvoiceDetailDialog
          open={detailOpen}
          invoiceId={selectedInvoice.id}
          onClose={() => {
            setDetailOpen(false)
            setSelectedInvoice(null)
          }}
          onUpdate={() => {
            queryClient.invalidateQueries({ queryKey: ['invoices'] })
          }}
        />
      )}

      {/* Invoice Form Dialog */}
      <InvoiceFormDialog
        open={formOpen}
        invoice={selectedInvoice}
        onClose={() => {
          setFormOpen(false)
          setSelectedInvoice(null)
        }}
        onSuccess={() => {
          setFormOpen(false)
          setSelectedInvoice(null)
          queryClient.invalidateQueries({ queryKey: ['invoices'] })
        }}
      />
    </Box>
  )
}

export default Invoices

