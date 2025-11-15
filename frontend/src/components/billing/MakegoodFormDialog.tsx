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
import { createMakegood } from '../../utils/api'
import api from '../../utils/api'

interface MakegoodFormDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

const MakegoodFormDialog: React.FC<MakegoodFormDialogProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [formData, setFormData] = useState({
    original_spot_id: '',
    makegood_spot_id: '',
    campaign_id: '',
    reason: '',
  })

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async (data: any) => {
      return await createMakegood({
        original_spot_id: parseInt(data.original_spot_id),
        makegood_spot_id: parseInt(data.makegood_spot_id),
        campaign_id: data.campaign_id ? parseInt(data.campaign_id) : undefined,
        reason: data.reason || undefined,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['makegoods'] })
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
            <Typography variant="h6">Create Makegood</Typography>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <TextField
              label="Original Spot ID"
              type="number"
              value={formData.original_spot_id}
              onChange={(e) => setFormData({ ...formData, original_spot_id: e.target.value })}
              required
              fullWidth
              helperText="The spot that needs to be made good"
            />

            <TextField
              label="Makegood Spot ID"
              type="number"
              value={formData.makegood_spot_id}
              onChange={(e) => setFormData({ ...formData, makegood_spot_id: e.target.value })}
              required
              fullWidth
              helperText="The spot that replaces the original"
            />

            <TextField
              label="Campaign ID"
              type="number"
              value={formData.campaign_id}
              onChange={(e) => setFormData({ ...formData, campaign_id: e.target.value })}
              fullWidth
              helperText="Optional: Associated campaign"
            />

            <TextField
              label="Reason"
              value={formData.reason}
              onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
              fullWidth
              multiline
              rows={3}
              helperText="Reason for the makegood (e.g., technical issue, missed airing)"
            />

            {mutation.isError && (
              <Alert severity="error">
                {mutation.error instanceof Error ? mutation.error.message : 'Failed to create makegood'}
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
            {mutation.isPending ? <CircularProgress size={20} /> : 'Create Makegood'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default MakegoodFormDialog

