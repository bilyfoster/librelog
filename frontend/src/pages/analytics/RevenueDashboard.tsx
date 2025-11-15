import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Card,
  CardContent,
  Alert,
  CircularProgress,
  LinearProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'
import {
  AttachMoney as MoneyIcon,
  TrendingUp as TrendingUpIcon,
  Assessment as AssessmentIcon,
  Timeline as TimelineIcon,
} from '@mui/icons-material'
import {
  getRevenueSummary,
  getRevenuePacing,
  getRevenueForecast,
} from '../../utils/api'

const RevenueDashboard: React.FC = () => {
  const [startDate, setStartDate] = useState(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [endDate, setEndDate] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [forecastMonths, setForecastMonths] = useState(3)

  const { data: summary, isLoading: summaryLoading, error: summaryError } = useQuery({
    queryKey: ['revenue-summary', startDate, endDate],
    queryFn: () => getRevenueSummary(startDate, endDate),
    retry: 1,
  })

  const { data: pacing, isLoading: pacingLoading, error: pacingError } = useQuery({
    queryKey: ['revenue-pacing', startDate, endDate],
    queryFn: () => getRevenuePacing(startDate, endDate),
    retry: 1,
  })

  const { data: forecast, isLoading: forecastLoading, error: forecastError } = useQuery({
    queryKey: ['revenue-forecast', forecastMonths],
    queryFn: () => getRevenueForecast(forecastMonths),
    retry: 1,
  })

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  const calculatePacingPercentage = () => {
    if (!pacing || typeof pacing !== 'object') return 0
    const actual = typeof pacing.actual_revenue === 'string' 
      ? parseFloat(pacing.actual_revenue) 
      : pacing.actual_revenue || 0
    const target = typeof pacing.target_revenue === 'string'
      ? parseFloat(pacing.target_revenue)
      : pacing.target_revenue || 0
    return target > 0 ? (actual / target) * 100 : 0
  }

  const pacingPercent = calculatePacingPercentage()

  const hasError = summaryError || pacingError || forecastError
  const isLoading = summaryLoading || pacingLoading || forecastLoading

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (hasError) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error" action={
          <Button color="inherit" size="small" onClick={() => {
            window.location.reload()
          }}>
            Retry
          </Button>
        }>
          Failed to load revenue data: {summaryError instanceof Error ? summaryError.message : pacingError instanceof Error ? pacingError.message : forecastError instanceof Error ? forecastError.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Revenue Dashboard
        </Typography>

        {/* Date Range Selector */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3, alignItems: 'center' }}>
          <TextField
            label="Start Date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            label="End Date"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
          />
        </Box>

        {/* Revenue Summary Cards */}
        {summaryLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : summary ? (
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <MoneyIcon sx={{ mr: 1, color: 'primary.main' }} />
                    <Typography variant="h6">Total Revenue</Typography>
                  </Box>
                  <Typography variant="h4">
                    {formatCurrency(summary.total_revenue || summary.total || 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Period total
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <AssessmentIcon sx={{ mr: 1, color: 'success.main' }} />
                    <Typography variant="h6">Invoiced</Typography>
                  </Box>
                  <Typography variant="h4">
                    {formatCurrency(summary.invoiced || summary.invoiced_revenue || 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Billed amount
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <TrendingUpIcon sx={{ mr: 1, color: 'info.main' }} />
                    <Typography variant="h6">Paid</Typography>
                  </Box>
                  <Typography variant="h4">
                    {formatCurrency(summary.paid || summary.paid_revenue || 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Collected
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <TimelineIcon sx={{ mr: 1, color: 'warning.main' }} />
                    <Typography variant="h6">Outstanding</Typography>
                  </Box>
                  <Typography variant="h4">
                    {formatCurrency(summary.outstanding || summary.outstanding_revenue || 0)}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    AR balance
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        ) : (
          <Alert severity="info">No revenue data available for the selected period.</Alert>
        )}

        {/* Revenue Pacing */}
        {pacingLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <CircularProgress />
          </Box>
        ) : pacing ? (
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              Revenue Pacing vs Goals
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={6}>
                <Box sx={{ mb: 2 }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Target Revenue</Typography>
                    <Typography variant="body2" fontWeight="medium">
                      {formatCurrency(
                        typeof pacing.target_revenue === 'string'
                          ? parseFloat(pacing.target_revenue)
                          : pacing.target_revenue || 0
                      )}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
                    <Typography variant="body2">Actual Revenue</Typography>
                    <Typography variant="body2" fontWeight="medium">
                      {formatCurrency(
                        typeof pacing.actual_revenue === 'string'
                          ? parseFloat(pacing.actual_revenue)
                          : pacing.actual_revenue || 0
                      )}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                    <Typography variant="body2">Progress</Typography>
                    <Typography
                      variant="body2"
                      fontWeight="medium"
                      color={pacingPercent >= 100 ? 'success.main' : pacingPercent >= 75 ? 'warning.main' : 'error.main'}
                    >
                      {pacingPercent.toFixed(1)}%
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={Math.min(pacingPercent, 100)}
                    sx={{ height: 10, borderRadius: 5 }}
                    color={pacingPercent >= 100 ? 'success' : pacingPercent >= 75 ? 'warning' : 'error'}
                  />
                </Box>
              </Grid>
              <Grid item xs={12} md={6}>
                {pacing.variance !== undefined && (
                  <Box>
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      Variance
                    </Typography>
                    <Typography
                      variant="h5"
                      color={pacing.variance >= 0 ? 'success.main' : 'error.main'}
                    >
                      {formatCurrency(
                        typeof pacing.variance === 'string'
                          ? parseFloat(pacing.variance)
                          : pacing.variance
                      )}
                    </Typography>
                  </Box>
                )}
              </Grid>
            </Grid>
          </Paper>
        ) : null}

        {/* Revenue Forecast */}
        <Paper sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Revenue Forecast</Typography>
            <FormControl size="small" sx={{ minWidth: 150 }}>
              <InputLabel>Months Ahead</InputLabel>
              <Select
                value={forecastMonths}
                onChange={(e) => setForecastMonths(e.target.value as number)}
                label="Months Ahead"
              >
                <MenuItem value={1}>1 Month</MenuItem>
                <MenuItem value={3}>3 Months</MenuItem>
                <MenuItem value={6}>6 Months</MenuItem>
                <MenuItem value={12}>12 Months</MenuItem>
              </Select>
            </FormControl>
          </Box>

          {forecastLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          ) : forecast ? (
            <Grid container spacing={2}>
              {Array.isArray(forecast) ? (
                forecast.map((item: any, idx: number) => (
                  <Grid item xs={12} sm={6} md={4} key={idx}>
                    <Card variant="outlined">
                      <CardContent>
                        <Typography variant="h6" gutterBottom>
                          {item.period || item.month || `Period ${idx + 1}`}
                        </Typography>
                        <Typography variant="h4" color="primary">
                          {formatCurrency(item.forecasted_revenue || item.revenue || 0)}
                        </Typography>
                        {item.confidence && (
                          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                            Confidence: {item.confidence}%
                          </Typography>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                ))
              ) : (
                <Grid item xs={12}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        Forecasted Revenue
                      </Typography>
                      <Typography variant="h4" color="primary">
                        {formatCurrency(forecast.forecasted_revenue || forecast.revenue || 0)}
                      </Typography>
                      {forecast.confidence && (
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                          Confidence: {forecast.confidence}%
                        </Typography>
                      )}
                    </CardContent>
                  </Card>
                </Grid>
              )}
            </Grid>
          ) : (
            <Alert severity="info">No forecast data available.</Alert>
          )}
        </Paper>
      </Paper>
    </Box>
  )
}

export default RevenueDashboard

