import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Alert,
  Autocomplete,
  Checkbox,
  FormControlLabel,
  Typography,
  CircularProgress,
} from '@mui/material'
import { getCopyProxy } from '../../utils/api'
import { createProductionOrder } from '../../utils/api'

interface Copy {
  id?: string
  title: string
  order_id?: string
  advertiser_id?: string
  script_text?: string
  audio_file_path?: string
}

interface ProductionOrderFormDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: (productionOrderId: number) => void
  initialCopyId?: number
}

const ProductionOrderFormDialog: React.FC<ProductionOrderFormDialogProps> = ({
  open,
  onClose,
  onSuccess,
  initialCopyId,
}) => {
  const [formData, setFormData] = useState({
    copy_id: initialCopyId || null as number | null,
    client_name: '',
    is_spec: false,
    spot_lengths: [] as number[],
    deadline: '',
    instructions: '',
  })
  const [copyOptions, setCopyOptions] = useState<Copy[]>([])
  const [loadingCopy, setLoadingCopy] = useState(false)
  const [selectedCopy, setSelectedCopy] = useState<Copy | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (open) {
      loadCopyOptions()
      if (initialCopyId) {
        // Find the copy in the options
        const copy = copyOptions.find(c => c.id === initialCopyId)
        if (copy) {
          setSelectedCopy(copy)
          setFormData(prev => ({ ...prev, copy_id: initialCopyId }))
        }
      } else {
        // Reset form when opening
        setFormData({
          copy_id: null,
          client_name: '',
          is_spec: false,
          spot_lengths: [],
          deadline: '',
          instructions: '',
        })
        setSelectedCopy(null)
        setError(null)
      }
    }
  }, [open, initialCopyId])

  const loadCopyOptions = async () => {
    try {
      setLoadingCopy(true)
      const data = await getCopyProxy({ limit: 1000 })
      setCopyOptions(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('Error loading copy options:', err)
      setCopyOptions([])
    } finally {
      setLoadingCopy(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!formData.copy_id) {
      setError('Please select a copy item')
      return
    }

    if (formData.is_spec && !formData.client_name) {
      setError('Client name is required for spec spots')
      return
    }

    try {
      setSubmitting(true)
      const result = await createProductionOrder({
        copy_id: formData.copy_id,
        client_name: formData.client_name || undefined,
        is_spec: formData.is_spec,
        spot_lengths: formData.spot_lengths.length > 0 ? formData.spot_lengths : undefined,
        deadline: formData.deadline || undefined,
        instructions: formData.instructions || undefined,
      })
      onSuccess(result.id)
      onClose()
    } catch (err: any) {
      setError(err?.response?.data?.detail || 'Failed to create production order')
    } finally {
      setSubmitting(false)
    }
  }

  const handleCopyChange = (copy: Copy | null) => {
    setSelectedCopy(copy)
    setFormData(prev => ({ ...prev, copy_id: copy?.id || null }))
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>Create Production Order</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 1 }}>
            {error && (
              <Alert severity="error" onClose={() => setError(null)}>
                {error}
              </Alert>
            )}

            <Autocomplete
              options={copyOptions}
              getOptionLabel={(option) => option.title || `Copy #${option.id}`}
              loading={loadingCopy}
              value={selectedCopy}
              onChange={(_, newValue) => handleCopyChange(newValue)}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Copy"
                  required
                  helperText="Select the copy item to create a production order for"
                />
              )}
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={formData.is_spec}
                  onChange={(e) => setFormData(prev => ({ ...prev, is_spec: e.target.checked }))}
                />
              }
              label="Create as Spec Spot (no sales order required)"
            />

            {formData.is_spec && (
              <Alert severity="info">
                Spec spots are created without an associated sales order. Use for pitches, demos, or speculative work.
              </Alert>
            )}

            <TextField
              label="Client Name"
              required={formData.is_spec}
              fullWidth
              value={formData.client_name}
              onChange={(e) => setFormData(prev => ({ ...prev, client_name: e.target.value }))}
              placeholder="Enter client name"
              helperText={formData.is_spec ? "Required for spec spots" : "Optional - will use advertiser name from copy if not provided"}
            />

            <FormControl fullWidth>
              <InputLabel>Spot Lengths (seconds)</InputLabel>
              <Select
                multiple
                value={formData.spot_lengths}
                onChange={(e) => setFormData(prev => ({ ...prev, spot_lengths: e.target.value as number[] }))}
                label="Spot Lengths (seconds)"
                renderValue={(selected) => (selected as number[]).join(', ') + ' sec'}
              >
                <MenuItem value={15}>15 seconds</MenuItem>
                <MenuItem value={30}>30 seconds</MenuItem>
                <MenuItem value={60}>60 seconds</MenuItem>
              </Select>
            </FormControl>

            <TextField
              label="Deadline"
              type="datetime-local"
              fullWidth
              value={formData.deadline}
              onChange={(e) => setFormData(prev => ({ ...prev, deadline: e.target.value }))}
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="Instructions"
              multiline
              rows={4}
              fullWidth
              value={formData.instructions}
              onChange={(e) => setFormData(prev => ({ ...prev, instructions: e.target.value }))}
              placeholder="Production instructions, talent needs, audio references, etc."
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={submitting || !formData.copy_id || (formData.is_spec && !formData.client_name)}
          >
            {submitting ? <CircularProgress size={20} /> : 'Create Production Order'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default ProductionOrderFormDialog

