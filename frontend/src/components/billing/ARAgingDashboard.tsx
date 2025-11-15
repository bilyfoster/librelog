import React from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Alert,
  CircularProgress,
  Chip,
} from '@mui/material'
import { getAgingReport } from '../../utils/api'

const ARAgingDashboard: React.FC = () => {
  const { data: aging, isLoading, error } = useQuery({
    queryKey: ['aging-report'],
    queryFn: getAgingReport,
  })

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Alert severity="error">Failed to load aging report</Alert>
    )
  }

  if (!aging) {
    return (
      <Alert severity="info">No aging data available</Alert>
    )
  }

  const totalOutstanding = aging.total_outstanding || 0
  const buckets = aging.buckets || {}
  const byAdvertiser = aging.by_advertiser || []

  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h6" sx={{ mb: 3 }}>
        Accounts Receivable Aging Report
      </Typography>

      {/* Summary Cards */}
      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <Paper sx={{ p: 2, minWidth: 150, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
          <Typography variant="body2">Total Outstanding</Typography>
          <Typography variant="h5">{formatCurrency(totalOutstanding)}</Typography>
        </Paper>
        <Paper sx={{ p: 2, minWidth: 150, bgcolor: 'success.light', color: 'success.contrastText' }}>
          <Typography variant="body2">0-30 Days</Typography>
          <Typography variant="h5">{formatCurrency(buckets['0-30'] || 0)}</Typography>
        </Paper>
        <Paper sx={{ p: 2, minWidth: 150, bgcolor: 'warning.light', color: 'warning.contrastText' }}>
          <Typography variant="body2">31-60 Days</Typography>
          <Typography variant="h5">{formatCurrency(buckets['31-60'] || 0)}</Typography>
        </Paper>
        <Paper sx={{ p: 2, minWidth: 150, bgcolor: 'error.light', color: 'error.contrastText' }}>
          <Typography variant="body2">61-90 Days</Typography>
          <Typography variant="h5">{formatCurrency(buckets['61-90'] || 0)}</Typography>
        </Paper>
        <Paper sx={{ p: 2, minWidth: 150, bgcolor: 'error.dark', color: 'error.contrastText' }}>
          <Typography variant="body2">90+ Days</Typography>
          <Typography variant="h5">{formatCurrency(buckets['90+'] || 0)}</Typography>
        </Paper>
      </Box>

      {/* By Advertiser */}
      {byAdvertiser.length > 0 && (
        <Box sx={{ mt: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Outstanding by Advertiser
          </Typography>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Advertiser</TableCell>
                  <TableCell align="right">0-30 Days</TableCell>
                  <TableCell align="right">31-60 Days</TableCell>
                  <TableCell align="right">61-90 Days</TableCell>
                  <TableCell align="right">90+ Days</TableCell>
                  <TableCell align="right">Total</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {byAdvertiser.map((item: any) => (
                  <TableRow key={item.advertiser_id || item.advertiser_name}>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {item.advertiser_name || `Advertiser #${item.advertiser_id}`}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">{formatCurrency(item['0-30'] || 0)}</TableCell>
                    <TableCell align="right">{formatCurrency(item['31-60'] || 0)}</TableCell>
                    <TableCell align="right">{formatCurrency(item['61-90'] || 0)}</TableCell>
                    <TableCell align="right">{formatCurrency(item['90+'] || 0)}</TableCell>
                    <TableCell align="right">
                      <Typography variant="body2" fontWeight="medium">
                        {formatCurrency(item.total || 0)}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </Box>
      )}
    </Paper>
  )
}

export default ARAgingDashboard

