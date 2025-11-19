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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
  Alert,
  CircularProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import {
  getPaymentsProxy,
  deletePayment,
  getInvoicesProxy,
} from '../../utils/api'
import PaymentFormDialog from '../../components/billing/PaymentFormDialog'

interface Payment {
  id: number
  invoice_id: number
  amount: number | string
  payment_date: string
  payment_method?: string
  reference_number?: string
  notes?: string
  created_at: string
}

const Payments: React.FC = () => {
  const [invoiceFilter, setInvoiceFilter] = useState<number | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [selectedPayment, setSelectedPayment] = useState<Payment | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: payments, isLoading, error } = useQuery({
    queryKey: ['payments', invoiceFilter],
    queryFn: async () => {
      const params: any = { limit: 100 }
      if (invoiceFilter) params.invoice_id = invoiceFilter
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getPaymentsProxy(params)
      return data
    },
    retry: 1,
  })

  const { data: invoices } = useQuery({
    queryKey: ['invoices'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getInvoicesProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (paymentId: number) => {
      await deletePayment(paymentId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete payment'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete payment error:', error)
    },
  })

  const handleEdit = (payment: Payment) => {
    setSelectedPayment(payment)
    setFormOpen(true)
  }

  const handleDelete = async (paymentId: number) => {
    if (window.confirm('Are you sure you want to delete this payment?')) {
      deleteMutation.mutate(paymentId)
    }
  }

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  const getInvoiceNumber = (invoiceId: number) => {
    const invoice = invoices?.find((inv: any) => inv.id === invoiceId)
    return invoice?.invoice_number || `Invoice #${invoiceId}`
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
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['payments'] })}>
            Retry
          </Button>
        }>
          Failed to load payments: {error instanceof Error ? error.message : 'Unknown error'}
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
          <Typography variant="h4">Payments</Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setSelectedPayment(null)
              setFormOpen(true)
            }}
          >
            Record Payment
          </Button>
        </Box>

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Invoice</InputLabel>
            <Select
              value={invoiceFilter}
              onChange={(e) => setInvoiceFilter(e.target.value as number | '')}
              label="Invoice"
            >
              <MenuItem value="">All Invoices</MenuItem>
              {invoices?.map((invoice: any) => (
                <MenuItem key={invoice.id} value={invoice.id}>
                  {invoice.invoice_number}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        {/* Payments Table */}
        {payments && payments.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Invoice</TableCell>
                  <TableCell>Date</TableCell>
                  <TableCell>Amount</TableCell>
                  <TableCell>Method</TableCell>
                  <TableCell>Reference</TableCell>
                  <TableCell>Notes</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {payments.map((payment: Payment) => (
                  <TableRow key={payment.id}>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {getInvoiceNumber(payment.invoice_id)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {new Date(payment.payment_date).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {formatCurrency(payment.amount)}
                      </Typography>
                    </TableCell>
                    <TableCell>{payment.payment_method || 'N/A'}</TableCell>
                    <TableCell>{payment.reference_number || 'N/A'}</TableCell>
                    <TableCell>{payment.notes || '-'}</TableCell>
                    <TableCell>
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => handleEdit(payment)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          onClick={() => handleDelete(payment.id)}
                          disabled={deleteMutation.isPending}
                          color="error"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No payments found. Record your first payment to get started.</Alert>
        )}
      </Paper>

      {/* Payment Form Dialog */}
      <PaymentFormDialog
        open={formOpen}
        invoiceId={selectedPayment?.invoice_id || (invoiceFilter ? parseInt(invoiceFilter.toString()) : 0)}
        payment={selectedPayment}
        onClose={() => {
          setFormOpen(false)
          setSelectedPayment(null)
        }}
        onSuccess={() => {
          setFormOpen(false)
          setSelectedPayment(null)
          queryClient.invalidateQueries({ queryKey: ['payments'] })
        }}
      />
    </Box>
  )
}

export default Payments

