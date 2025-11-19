import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Button,
  Card,
  CardContent,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material'
import {
  TrendingUp as TrendingUpIcon,
  Inventory as InventoryIcon,
  Sell as SellIcon,
} from '@mui/icons-material'
import { getInventoryProxy, getInventoryHeatmap, getSelloutPercentages } from '../../utils/api'
import InventoryHeatmap from '../../components/analytics/InventoryHeatmap'

const InventoryDashboard: React.FC = () => {
  const [startDate, setStartDate] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [endDate, setEndDate] = useState(
    new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )

  const { data: inventory, isLoading: inventoryLoading, error: inventoryError } = useQuery({
    queryKey: ['inventory', startDate, endDate],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getInventoryProxy({ start_date: startDate, end_date: endDate })
      return data
    },
    retry: 1,
  })

  const { data: heatmap, isLoading: heatmapLoading, error: heatmapError } = useQuery({
    queryKey: ['inventory-heatmap', startDate, endDate],
    queryFn: () => getInventoryHeatmap(startDate, endDate),
    retry: 1,
  })

  const { data: sellout, isLoading: selloutLoading, error: selloutError } = useQuery({
    queryKey: ['sellout', startDate, endDate],
    queryFn: () => getSelloutPercentages(startDate, endDate),
    retry: 1,
  })

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  const calculateTotalAvailable = () => {
    if (!inventory || !Array.isArray(inventory)) return 0
    return inventory.reduce((sum: number, slot: any) => sum + (slot.available || 0), 0)
  }

  const calculateTotalBooked = () => {
    if (!inventory || !Array.isArray(inventory)) return 0
    return inventory.reduce((sum: number, slot: any) => sum + (slot.booked || 0), 0)
  }

  const calculateTotalRevenue = () => {
    if (!inventory || !Array.isArray(inventory)) return 0
    return inventory.reduce((sum: number, slot: any) => {
      const revenue = typeof slot.revenue === 'string' ? parseFloat(slot.revenue) : slot.revenue
      return sum + (revenue || 0)
    }, 0)
  }

  const totalAvailable = calculateTotalAvailable()
  const totalBooked = calculateTotalBooked()
  const totalRevenue = calculateTotalRevenue()
  const utilizationRate = totalAvailable > 0 ? (totalBooked / totalAvailable) * 100 : 0

  const hasError = inventoryError || heatmapError || selloutError
  const isLoading = inventoryLoading || heatmapLoading || selloutLoading

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
          Failed to load inventory data: {inventoryError instanceof Error ? inventoryError.message : heatmapError instanceof Error ? heatmapError.message : selloutError instanceof Error ? selloutError.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Inventory Dashboard
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

        {/* Summary Cards */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <InventoryIcon sx={{ mr: 1, color: 'primary.main' }} />
                  <Typography variant="h6">Total Available</Typography>
                </Box>
                <Typography variant="h4">{totalAvailable}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Inventory slots
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <SellIcon sx={{ mr: 1, color: 'success.main' }} />
                  <Typography variant="h6">Total Booked</Typography>
                </Box>
                <Typography variant="h4">{totalBooked}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {utilizationRate.toFixed(1)}% utilization
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <TrendingUpIcon sx={{ mr: 1, color: 'info.main' }} />
                  <Typography variant="h6">Total Revenue</Typography>
                </Box>
                <Typography variant="h4">{formatCurrency(totalRevenue)}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Projected revenue
                </Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <SellIcon sx={{ mr: 1, color: 'warning.main' }} />
                  <Typography variant="h6">Remaining</Typography>
                </Box>
                <Typography variant="h4">{totalAvailable - totalBooked}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Available slots
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>

        {/* Heatmap */}
        {heatmapLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : heatmap ? (
          <Box sx={{ mb: 3 }}>
            <InventoryHeatmap data={heatmap} />
          </Box>
        ) : null}

        {/* Sellout Report */}
        {selloutLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <CircularProgress />
          </Box>
        ) : sellout && Array.isArray(sellout) && sellout.length > 0 ? (
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              Sellout Percentages
            </Typography>
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Hour</TableCell>
                    <TableCell>Daypart</TableCell>
                    <TableCell>Available</TableCell>
                    <TableCell>Booked</TableCell>
                    <TableCell>Sellout %</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {sellout.map((item: any, idx: number) => {
                    const selloutPercent = item.available > 0
                      ? (item.booked / item.available) * 100
                      : 0
                    return (
                      <TableRow key={idx}>
                        <TableCell>
                          {item.date ? new Date(item.date).toLocaleDateString() : '-'}
                        </TableCell>
                        <TableCell>{item.hour || '-'}</TableCell>
                        <TableCell>{item.daypart || '-'}</TableCell>
                        <TableCell>{item.available || 0}</TableCell>
                        <TableCell>{item.booked || 0}</TableCell>
                        <TableCell>
                          <Chip
                            label={`${selloutPercent.toFixed(1)}%`}
                            size="small"
                            color={
                              selloutPercent >= 90
                                ? 'error'
                                : selloutPercent >= 70
                                ? 'warning'
                                : 'success'
                            }
                          />
                        </TableCell>
                        <TableCell>
                          {item.sold_out ? (
                            <Chip label="Sold Out" size="small" color="error" />
                          ) : (
                            <Chip label="Available" size="small" color="success" />
                          )}
                        </TableCell>
                      </TableRow>
                    )
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>
        ) : null}

        {/* Inventory Details */}
        {inventoryLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
            <CircularProgress />
          </Box>
        ) : inventory && Array.isArray(inventory) && inventory.length > 0 ? (
          <Box sx={{ mt: 3 }}>
            <Typography variant="h6" gutterBottom>
              Inventory Details
            </Typography>
            <TableContainer component={Paper} variant="outlined">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Hour</TableCell>
                    <TableCell>Break Position</TableCell>
                    <TableCell>Daypart</TableCell>
                    <TableCell>Available</TableCell>
                    <TableCell>Booked</TableCell>
                    <TableCell>Revenue</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {inventory.slice(0, 50).map((slot: any) => (
                    <TableRow key={slot.id}>
                      <TableCell>
                        {slot.date ? new Date(slot.date).toLocaleDateString() : '-'}
                      </TableCell>
                      <TableCell>{slot.hour || '-'}</TableCell>
                      <TableCell>{slot.break_position || '-'}</TableCell>
                      <TableCell>{slot.daypart || '-'}</TableCell>
                      <TableCell>{slot.available || 0}</TableCell>
                      <TableCell>{slot.booked || 0}</TableCell>
                      <TableCell>{formatCurrency(slot.revenue || 0)}</TableCell>
                      <TableCell>
                        {slot.sold_out ? (
                          <Chip label="Sold Out" size="small" color="error" />
                        ) : (
                          <Chip label="Available" size="small" color="success" />
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
            {inventory.length > 50 && (
              <Alert severity="info" sx={{ mt: 2 }}>
                Showing first 50 of {inventory.length} inventory slots. Use date filters to narrow results.
              </Alert>
            )}
          </Box>
        ) : (
          <Alert severity="info">No inventory data available for the selected date range.</Alert>
        )}
      </Paper>
    </Box>
  )
}

export default InventoryDashboard

