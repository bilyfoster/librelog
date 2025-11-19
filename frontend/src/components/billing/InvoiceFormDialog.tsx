import React, { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  IconButton,
  Alert,
  CircularProgress,
} from '@mui/material'
import { Close as CloseIcon } from '@mui/icons-material'
import { createInvoice, updateInvoice, getOrdersProxy, getAdvertisersProxy, getAgenciesProxy } from '../../utils/api'
import api from '../../utils/api'

interface InvoiceFormDialogProps {
  open: boolean
  invoice?: any
  onClose: () => void
  onSuccess: () => void
}

const InvoiceFormDialog: React.FC<InvoiceFormDialogProps> = ({
  open,
  invoice,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    invoice_number: '',
    advertiser_id: '',
    agency_id: '',
    order_id: '',
    invoice_date: new Date().toISOString().split('T')[0],
    due_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    payment_terms: '',
    notes: '',
  })

  const { data: advertisers } = useQuery({
    queryKey: ['advertisers'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAdvertisersProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: agencies } = useQuery({
    queryKey: ['agencies'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAgenciesProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: orders } = useQuery({
    queryKey: ['orders', formData.advertiser_id],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getOrdersProxy({ 
        advertiser_id: formData.advertiser_id ? parseInt(formData.advertiser_id) : undefined 
      })
      return Array.isArray(data) ? data : []
    },
    enabled: !!formData.advertiser_id,
  })

  useEffect(() => {
    if (invoice) {
      setFormData({
        invoice_number: invoice.invoice_number || '',
        advertiser_id: invoice.advertiser_id?.toString() || '',
        agency_id: invoice.agency_id?.toString() || '',
        order_id: invoice.order_id?.toString() || '',
        invoice_date: invoice.invoice_date ? new Date(invoice.invoice_date).toISOString().split('T')[0] : '',
        due_date: invoice.due_date ? new Date(invoice.due_date).toISOString().split('T')[0] : '',
        payment_terms: invoice.payment_terms || '',
        notes: invoice.notes || '',
      })
    } else {
      setFormData({
        invoice_number: '',
        advertiser_id: '',
        agency_id: '',
        order_id: '',
        invoice_date: new Date().toISOString().split('T')[0],
        due_date: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        payment_terms: '',
        notes: '',
      })
    }
  }, [invoice, open])

  const mutation = useMutation({
    mutationFn: async (data: any) => {
      if (invoice) {
        return await updateInvoice(invoice.id, data)
      } else {
        return await createInvoice({
          ...data,
          advertiser_id: parseInt(data.advertiser_id),
          agency_id: data.agency_id ? parseInt(data.agency_id) : undefined,
          order_id: data.order_id ? parseInt(data.order_id) : undefined,
        })
      }
    },
    onSuccess: () => {
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
            <Typography variant="h6">
              {invoice ? 'Edit Invoice' : 'Create Invoice'}
            </Typography>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <TextField
              label="Invoice Number"
              value={formData.invoice_number}
              onChange={(e) => setFormData({ ...formData, invoice_number: e.target.value })}
              required
              fullWidth
            />

            <FormControl fullWidth required>
              <InputLabel>Advertiser</InputLabel>
              <Select
                value={formData.advertiser_id}
                onChange={(e) => setFormData({ ...formData, advertiser_id: e.target.value, order_id: '' })}
                label="Advertiser"
              >
                {advertisers?.map((advertiser: any) => (
                  <MenuItem key={advertiser.id} value={advertiser.id.toString()}>
                    {advertiser.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel>Agency</InputLabel>
              <Select
                value={formData.agency_id}
                onChange={(e) => setFormData({ ...formData, agency_id: e.target.value })}
                label="Agency"
              >
                <MenuItem value="">None</MenuItem>
                {agencies?.map((agency: any) => (
                  <MenuItem key={agency.id} value={agency.id.toString()}>
                    {agency.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel>Order</InputLabel>
              <Select
                value={formData.order_id}
                onChange={(e) => setFormData({ ...formData, order_id: e.target.value })}
                label="Order"
                disabled={!formData.advertiser_id}
              >
                <MenuItem value="">None</MenuItem>
                {orders?.map((order: any) => (
                  <MenuItem key={order.id} value={order.id.toString()}>
                    {order.order_number || `Order #${order.id}`}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Invoice Date"
              type="date"
              value={formData.invoice_date}
              onChange={(e) => setFormData({ ...formData, invoice_date: e.target.value })}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="Due Date"
              type="date"
              value={formData.due_date}
              onChange={(e) => setFormData({ ...formData, due_date: e.target.value })}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="Payment Terms"
              value={formData.payment_terms}
              onChange={(e) => setFormData({ ...formData, payment_terms: e.target.value })}
              fullWidth
              multiline
              rows={2}
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
                {mutation.error instanceof Error ? mutation.error.message : 'Failed to save invoice'}
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
            {mutation.isPending ? <CircularProgress size={20} /> : invoice ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default InvoiceFormDialog

