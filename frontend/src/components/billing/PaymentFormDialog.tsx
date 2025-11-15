import React, { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  IconButton,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import { Close as CloseIcon } from '@mui/icons-material'
import { createPayment, updatePayment } from '../../utils/api'

interface PaymentFormDialogProps {
  open: boolean
  invoiceId: number
  payment?: any
  onClose: () => void
  onSuccess: () => void
}

const PaymentFormDialog: React.FC<PaymentFormDialogProps> = ({
  open,
  invoiceId,
  payment,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    amount: '',
    payment_date: new Date().toISOString().split('T')[0],
    payment_method: '',
    reference_number: '',
    notes: '',
  })

  React.useEffect(() => {
    if (payment) {
      setFormData({
        amount: payment.amount?.toString() || '',
        payment_date: payment.payment_date ? new Date(payment.payment_date).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
        payment_method: payment.payment_method || '',
        reference_number: payment.reference_number || '',
        notes: payment.notes || '',
      })
    } else {
      setFormData({
        amount: '',
        payment_date: new Date().toISOString().split('T')[0],
        payment_method: '',
        reference_number: '',
        notes: '',
      })
    }
  }, [payment, open])

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async (data: any) => {
      if (payment) {
        return await updatePayment(payment.id, {
          amount: parseFloat(data.amount),
          payment_date: new Date(data.payment_date).toISOString(),
          payment_method: data.payment_method || undefined,
          reference_number: data.reference_number || undefined,
          notes: data.notes || undefined,
        })
      } else {
        return await createPayment({
          invoice_id: invoiceId,
          amount: parseFloat(data.amount),
          payment_date: new Date(data.payment_date).toISOString(),
          payment_method: data.payment_method || undefined,
          reference_number: data.reference_number || undefined,
          notes: data.notes || undefined,
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] })
      queryClient.invalidateQueries({ queryKey: ['invoices'] })
      onSuccess()
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    mutation.mutate(formData)
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6">{payment ? 'Edit Payment' : 'Record Payment'}</Typography>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <TextField
              label="Amount"
              type="number"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: e.target.value })}
              required
              fullWidth
              inputProps={{ step: '0.01', min: '0' }}
            />

            <TextField
              label="Payment Date"
              type="date"
              value={formData.payment_date}
              onChange={(e) => setFormData({ ...formData, payment_date: e.target.value })}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />

            <FormControl fullWidth>
              <InputLabel>Payment Method</InputLabel>
              <Select
                value={formData.payment_method}
                onChange={(e) => setFormData({ ...formData, payment_method: e.target.value })}
                label="Payment Method"
              >
                <MenuItem value="">Select Method</MenuItem>
                <MenuItem value="Check">Check</MenuItem>
                <MenuItem value="Wire Transfer">Wire Transfer</MenuItem>
                <MenuItem value="ACH">ACH</MenuItem>
                <MenuItem value="Credit Card">Credit Card</MenuItem>
                <MenuItem value="Cash">Cash</MenuItem>
                <MenuItem value="Other">Other</MenuItem>
              </Select>
            </FormControl>

            <TextField
              label="Reference Number"
              value={formData.reference_number}
              onChange={(e) => setFormData({ ...formData, reference_number: e.target.value })}
              fullWidth
            />

            <TextField
              label="Notes"
              value={formData.notes}
              onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
              fullWidth
              multiline
              rows={3}
            />

            {mutation.isError && (
              <Alert severity="error">
                {mutation.error instanceof Error ? mutation.error.message : 'Failed to record payment'}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            type="submit"
            variant="contained"
            disabled={mutation.isPending}
          >
            {mutation.isPending ? <CircularProgress size={20} /> : payment ? 'Update' : 'Record Payment'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default PaymentFormDialog

