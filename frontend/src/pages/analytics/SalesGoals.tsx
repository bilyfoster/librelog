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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Tooltip,
  Alert,
  CircularProgress,
  Chip,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material'
import {
  getSalesGoals,
  createSalesGoal,
  getSalesGoalsProgress,
} from '../../utils/api'
import { getSalesReps } from '../../utils/api'
import SalesGoalFormDialog from '../../components/analytics/SalesGoalFormDialog'

interface SalesGoal {
  id: number
  sales_rep_id: number
  period: string
  target_date: string
  goal_amount: number | string
  actual_amount: number | string
  created_at: string
  updated_at: string
}

const SalesGoals: React.FC = () => {
  const [salesRepFilter, setSalesRepFilter] = useState<number | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [selectedGoal, setSelectedGoal] = useState<SalesGoal | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: goals, isLoading, error } = useQuery({
    queryKey: ['sales-goals', salesRepFilter],
    queryFn: async () => {
      const params: any = { limit: 100 }
      if (salesRepFilter) params.sales_rep_id = salesRepFilter
      const data = await getSalesGoals(params)
      return data
    },
    retry: 1,
  })

  const { data: salesReps } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: () => getSalesReps({ limit: 100 }),
  })

  const { data: progress } = useQuery({
    queryKey: ['sales-goals-progress', salesRepFilter],
    queryFn: () => getSalesGoalsProgress(salesRepFilter ? salesRepFilter as number : undefined),
  })

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  const getProgressPercentage = (goal: SalesGoal) => {
    const goalAmount = typeof goal.goal_amount === 'string' ? parseFloat(goal.goal_amount) : goal.goal_amount
    const actualAmount = typeof goal.actual_amount === 'string' ? parseFloat(goal.actual_amount) : goal.actual_amount
    if (goalAmount === 0) return 0
    return (actualAmount / goalAmount) * 100
  }

  const getSalesRepName = (salesRepId: number) => {
    const rep = salesReps?.find((r: any) => r.id === salesRepId)
    return rep ? `${rep.first_name} ${rep.last_name}` : `Rep #${salesRepId}`
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
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['sales-goals'] })}>
            Retry
          </Button>
        }>
          Failed to load sales goals: {error instanceof Error ? error.message : 'Unknown error'}
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
          <Typography variant="h4">Sales Goals</Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => {
              setSelectedGoal(null)
              setFormOpen(true)
            }}
          >
            Create Goal
          </Button>
        </Box>

        {/* Progress Summary */}
        {progress && typeof progress === 'object' && (
          <Paper sx={{ p: 2, mb: 3, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
            <Typography variant="h6" gutterBottom>
              Overall Progress
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <Typography variant="body2">Total Goals</Typography>
                <Typography variant="h5">{progress.total_goals || goals?.length || 0}</Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2">Total Target</Typography>
                <Typography variant="h5">
                  {formatCurrency(progress.total_target || progress.total_goal_amount || 0)}
                </Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="body2">Total Actual</Typography>
                <Typography variant="h5">
                  {formatCurrency(progress.total_actual || progress.total_actual_amount || 0)}
                </Typography>
              </Grid>
            </Grid>
            {progress.overall_progress !== undefined && (
              <Box sx={{ mt: 2 }}>
                <LinearProgress
                  variant="determinate"
                  value={Math.min(progress.overall_progress, 100)}
                  sx={{ height: 10, borderRadius: 5 }}
                />
                <Typography variant="body2" sx={{ mt: 1 }}>
                  {progress.overall_progress.toFixed(1)}% of goal achieved
                </Typography>
              </Box>
            )}
          </Paper>
        )}

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Sales Rep</InputLabel>
            <Select
              value={salesRepFilter}
              onChange={(e) => setSalesRepFilter(e.target.value as number | '')}
              label="Sales Rep"
            >
              <MenuItem value="">All Sales Reps</MenuItem>
              {salesReps?.map((rep: any) => (
                <MenuItem key={rep.id} value={rep.id}>
                  {rep.first_name} {rep.last_name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        {/* Goals Table */}
        {goals && goals.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Sales Rep</TableCell>
                  <TableCell>Period</TableCell>
                  <TableCell>Target Date</TableCell>
                  <TableCell>Goal Amount</TableCell>
                  <TableCell>Actual Amount</TableCell>
                  <TableCell>Progress</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {goals.map((goal: SalesGoal) => {
                  const progressPercent = getProgressPercentage(goal)
                  const goalAmount = typeof goal.goal_amount === 'string' ? parseFloat(goal.goal_amount) : goal.goal_amount
                  const actualAmount = typeof goal.actual_amount === 'string' ? parseFloat(goal.actual_amount) : goal.actual_amount
                  return (
                    <TableRow key={goal.id}>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {getSalesRepName(goal.sales_rep_id)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip label={goal.period} size="small" />
                      </TableCell>
                      <TableCell>
                        {new Date(goal.target_date).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {formatCurrency(goalAmount)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" fontWeight="medium">
                          {formatCurrency(actualAmount)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Box sx={{ minWidth: 100 }}>
                          <LinearProgress
                            variant="determinate"
                            value={Math.min(progressPercent, 100)}
                            sx={{ height: 8, borderRadius: 4, mb: 0.5 }}
                            color={progressPercent >= 100 ? 'success' : progressPercent >= 75 ? 'warning' : 'error'}
                          />
                          <Typography variant="caption">
                            {progressPercent.toFixed(1)}%
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        {progressPercent >= 100 ? (
                          <Chip label="Achieved" size="small" color="success" />
                        ) : progressPercent >= 75 ? (
                          <Chip label="On Track" size="small" color="warning" />
                        ) : (
                          <Chip label="Behind" size="small" color="error" />
                        )}
                      </TableCell>
                      <TableCell>
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => {
                              setSelectedGoal(goal)
                              setFormOpen(true)
                            }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No sales goals found. Create your first goal to get started.</Alert>
        )}
      </Paper>

      {/* Sales Goal Form Dialog */}
      <SalesGoalFormDialog
        open={formOpen}
        goal={selectedGoal}
        onClose={() => {
          setFormOpen(false)
          setSelectedGoal(null)
        }}
        onSuccess={() => {
          setFormOpen(false)
          setSelectedGoal(null)
          queryClient.invalidateQueries({ queryKey: ['sales-goals'] })
          queryClient.invalidateQueries({ queryKey: ['sales-goals-progress'] })
        }}
      />
    </Box>
  )
}

export default SalesGoals

