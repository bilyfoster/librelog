import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Divider,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material'
import {
  Close as CloseIcon,
  Send as SendIcon,
  CheckCircle as CheckCircleIcon,
  Edit as EditIcon,
} from '@mui/icons-material'
import {
  getInvoice,
  sendInvoice,
  markInvoicePaid,
  getPayments,
} from '../../utils/api'
import PaymentFormDialog from './PaymentFormDialog'

interface InvoiceDetailDialogProps {
  open: boolean
  invoiceId: number
  onClose: () => void
  onUpdate: () => void
}

const InvoiceDetailDialog: React.FC<InvoiceDetailDialogProps> = ({
  open,
  invoiceId,
  onClose,
  onUpdate,
}) => {
  const [paymentFormOpen, setPaymentFormOpen] = useState(false)
  const queryClient = useQueryClient()

  const { data: invoice, isLoading, error } = useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: () => getInvoice(invoiceId),
    enabled: open && !!invoiceId,
  })

  const { data: payments } = useQuery({
    queryKey: ['payments', invoiceId],
    queryFn: () => getPayments({ invoice_id: invoiceId }),
    enabled: open && !!invoiceId,
  })

  const sendMutation = useMutation({
    mutationFn: () => sendInvoice(invoiceId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      onUpdate()
    },
  })

  const markPaidMutation = useMutation({
    mutationFn: () => markInvoicePaid(invoiceId, {}),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      queryClient.invalidateQueries({ queryKey: ['payments', invoiceId] })
      onUpdate()
    },
  })

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
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

  const totalPaid = payments?.reduce((sum: number, p: any) => {
    const amount = typeof p.amount === 'string' ? parseFloat(p.amount) : p.amount
    return sum + amount
  }, 0) || 0

  const invoiceTotal = invoice ? (typeof invoice.total === 'string' ? parseFloat(invoice.total) : invoice.total) : 0
  const balanceDue = invoiceTotal - totalPaid

  if (isLoading) {
    return (
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        </DialogContent>
      </Dialog>
    )
  }

  if (error || !invoice) {
    return (
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogContent>
          <Alert severity="error">Failed to load invoice details</Alert>
        </DialogContent>
      </Dialog>
    )
  }

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">Invoice {invoice.invoice_number}</Typography>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Invoice Date
                </Typography>
                <Typography variant="body1">
                  {new Date(invoice.invoice_date).toLocaleDateString()}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Due Date
                </Typography>
                <Typography variant="body1">
                  {new Date(invoice.due_date).toLocaleDateString()}
                </Typography>
              </Box>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Status
                </Typography>
                <Chip
                  label={invoice.status}
                  size="small"
                  color={getStatusColor(invoice.status) as any}
                />
              </Box>
            </Box>

            {invoice.payment_terms && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Payment Terms
                </Typography>
                <Typography variant="body1">{invoice.payment_terms}</Typography>
              </Box>
            )}

            {invoice.notes && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Notes
                </Typography>
                <Typography variant="body1">{invoice.notes}</Typography>
              </Box>
            )}
          </Box>

          <Divider sx={{ my: 2 }} />

          {/* Invoice Lines */}
          <Typography variant="h6" sx={{ mb: 2 }}>
            Line Items
          </Typography>
          {invoice.invoice_lines && invoice.invoice_lines.length > 0 ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Description</TableCell>
                    <TableCell align="right">Quantity</TableCell>
                    <TableCell align="right">Unit Price</TableCell>
                    <TableCell align="right">Total</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {invoice.invoice_lines.map((line: any, index: number) => (
                    <TableRow key={index}>
                      <TableCell>{line.description}</TableCell>
                      <TableCell align="right">{line.quantity || 1}</TableCell>
                      <TableCell align="right">
                        {formatCurrency(line.unit_price || 0)}
                      </TableCell>
                      <TableCell align="right">
                        {formatCurrency(line.total_amount || 0)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info">No line items</Alert>
          )}

          {/* Totals */}
          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
            <Box sx={{ minWidth: 200 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Subtotal:</Typography>
                <Typography variant="body2">
                  {formatCurrency(invoice.subtotal || 0)}
                </Typography>
              </Box>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="body2">Tax:</Typography>
                <Typography variant="body2">
                  {formatCurrency(invoice.tax || 0)}
                </Typography>
              </Box>
              <Divider sx={{ my: 1 }} />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                <Typography variant="h6">Total:</Typography>
                <Typography variant="h6">
                  {formatCurrency(invoice.total || 0)}
                </Typography>
              </Box>
              {totalPaid > 0 && (
                <>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1, mt: 1 }}>
                    <Typography variant="body2" color="text.secondary">
                      Total Paid:
                    </Typography>
                    <Typography variant="body2" color="success.main">
                      {formatCurrency(totalPaid)}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                    <Typography variant="body1" fontWeight="medium">
                      Balance Due:
                    </Typography>
                    <Typography variant="body1" fontWeight="medium" color={balanceDue > 0 ? 'error.main' : 'success.main'}>
                      {formatCurrency(balanceDue)}
                    </Typography>
                  </Box>
                </>
              )}
            </Box>
          </Box>

          {/* Payment History */}
          <Divider sx={{ my: 3 }} />
          <Typography variant="h6" sx={{ mb: 2 }}>
            Payment History
          </Typography>
          {payments && payments.length > 0 ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Method</TableCell>
                    <TableCell>Reference</TableCell>
                    <TableCell>Notes</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {payments.map((payment: any) => (
                    <TableRow key={payment.id}>
                      <TableCell>
                        {new Date(payment.payment_date).toLocaleDateString()}
                      </TableCell>
                      <TableCell>{formatCurrency(payment.amount)}</TableCell>
                      <TableCell>{payment.payment_method || 'N/A'}</TableCell>
                      <TableCell>{payment.reference_number || 'N/A'}</TableCell>
                      <TableCell>{payment.notes || '-'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info">No payments recorded</Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Close</Button>
          {invoice.status === 'DRAFT' && (
            <Button
              variant="contained"
              startIcon={<SendIcon />}
              onClick={() => {
                if (window.confirm('Send this invoice to the advertiser?')) {
                  sendMutation.mutate()
                }
              }}
              disabled={sendMutation.isPending}
            >
              Send Invoice
            </Button>
          )}
          {invoice.status !== 'PAID' && invoice.status !== 'CANCELLED' && (
            <>
              <Button
                variant="outlined"
                onClick={() => setPaymentFormOpen(true)}
              >
                Record Payment
              </Button>
              <Button
                variant="contained"
                color="success"
                startIcon={<CheckCircleIcon />}
                onClick={() => {
                  if (window.confirm('Mark this invoice as fully paid?')) {
                    markPaidMutation.mutate()
                  }
                }}
                disabled={markPaidMutation.isPending}
              >
                Mark as Paid
              </Button>
            </>
          )}
        </DialogActions>
      </Dialog>

      {/* Payment Form Dialog */}
      <PaymentFormDialog
        open={paymentFormOpen}
        invoiceId={invoiceId}
        onClose={() => setPaymentFormOpen(false)}
        onSuccess={() => {
          setPaymentFormOpen(false)
          queryClient.invalidateQueries({ queryKey: ['payments', invoiceId] })
          queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
          onUpdate()
        }}
      />
    </>
  )
}

export default InvoiceDetailDialog

