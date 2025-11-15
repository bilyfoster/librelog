import React, { useState, useEffect } from 'react'
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
import { createSalesGoal } from '../../utils/api'
import { getSalesReps } from '../../utils/api'
import { useQuery } from '@tanstack/react-query'

interface SalesGoalFormDialogProps {
  open: boolean
  goal?: any
  onClose: () => void
  onSuccess: () => void
  onError?: (error: string) => void
}

const SalesGoalFormDialog: React.FC<SalesGoalFormDialogProps> = ({
  open,
  goal,
  onClose,
  onSuccess,
  onError,
}) => {
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [formData, setFormData] = useState({
    sales_rep_id: '',
    period: 'MONTHLY',
    target_date: new Date().toISOString().split('T')[0],
    goal_amount: '',
  })

  const { data: salesReps } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: () => getSalesReps({ limit: 100 }),
  })

  useEffect(() => {
    if (goal) {
      setFormData({
        sales_rep_id: goal.sales_rep_id?.toString() || '',
        period: goal.period || 'MONTHLY',
        target_date: goal.target_date ? new Date(goal.target_date).toISOString().split('T')[0] : '',
        goal_amount: goal.goal_amount?.toString() || '',
      })
    } else {
      setFormData({
        sales_rep_id: '',
        period: 'MONTHLY',
        target_date: new Date().toISOString().split('T')[0],
        goal_amount: '',
      })
    }
  }, [goal, open])

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: async (data: any) => {
      return await createSalesGoal({
        sales_rep_id: parseInt(data.sales_rep_id),
        period: data.period,
        target_date: data.target_date,
        goal_amount: parseFloat(data.goal_amount),
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sales-goals'] })
      queryClient.invalidateQueries({ queryKey: ['sales-goals-progress'] })
      setErrorMessage(null)
      onSuccess()
    },
    onError: (error: any) => {
      let message = 'Failed to create sales goal'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      if (onError) onError(message)
      console.error('Create sales goal error:', error)
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
              {goal ? 'Edit Sales Goal' : 'Create Sales Goal'}
            </Typography>
            <IconButton onClick={onClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {errorMessage && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {errorMessage}
            </Alert>
          )}
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
            <FormControl fullWidth required>
              <InputLabel>Sales Rep</InputLabel>
              <Select
                value={formData.sales_rep_id}
                onChange={(e) => setFormData({ ...formData, sales_rep_id: e.target.value })}
                label="Sales Rep"
              >
                {salesReps?.map((rep: any) => (
                  <MenuItem key={rep.id} value={rep.id.toString()}>
                    {rep.first_name} {rep.last_name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth required>
              <InputLabel>Period</InputLabel>
              <Select
                value={formData.period}
                onChange={(e) => setFormData({ ...formData, period: e.target.value })}
                label="Period"
              >
                <MenuItem value="DAILY">Daily</MenuItem>
                <MenuItem value="WEEKLY">Weekly</MenuItem>
                <MenuItem value="MONTHLY">Monthly</MenuItem>
                <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                <MenuItem value="YEARLY">Yearly</MenuItem>
              </Select>
            </FormControl>

            <TextField
              label="Target Date"
              type="date"
              value={formData.target_date}
              onChange={(e) => setFormData({ ...formData, target_date: e.target.value })}
              required
              fullWidth
              InputLabelProps={{ shrink: true }}
            />

            <TextField
              label="Goal Amount"
              type="number"
              value={formData.goal_amount}
              onChange={(e) => setFormData({ ...formData, goal_amount: e.target.value })}
              required
              fullWidth
              inputProps={{ step: '0.01', min: '0' }}
            />

            {mutation.isError && (
              <Alert severity="error">
                {mutation.error instanceof Error ? mutation.error.message : 'Failed to save sales goal'}
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
            {mutation.isPending ? <CircularProgress size={20} /> : goal ? 'Update' : 'Create'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default SalesGoalFormDialog

