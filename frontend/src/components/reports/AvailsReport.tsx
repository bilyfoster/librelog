import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material'
import { getLogAvails } from '../../utils/api'

interface AvailSlot {
  hour: number
  break_position: string
  available: number
  booked: number
  sold_out: boolean
  revenue?: number
}

interface AvailsReportProps {
  logId: number
}

const AvailsReport: React.FC<AvailsReportProps> = ({ logId }) => {
  const [avails, setAvails] = useState<AvailSlot[]>([])
  const [loading, setLoading] = useState(true)
  const [date, setDate] = useState<string>('')

  useEffect(() => {
    loadAvails()
  }, [logId])

  const loadAvails = async () => {
    try {
      setLoading(true)
      const data = await getLogAvails(logId)
      setAvails(data.avails || [])
      setDate(data.date || '')
    } catch (error) {
      console.error('Failed to load avails:', error)
    } finally {
      setLoading(false)
    }
  }

  const calculateSellout = (available: number, booked: number) => {
    const total = available + booked
    if (total === 0) return 0
    return Math.round((booked / total) * 100)
  }

  const groupByHour = () => {
    const grouped: Record<number, AvailSlot[]> = {}
    avails.forEach(slot => {
      if (!grouped[slot.hour]) {
        grouped[slot.hour] = []
      }
      grouped[slot.hour].push(slot)
    })
    return grouped
  }

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  const grouped = groupByHour()
  const totalAvailable = avails.reduce((sum, slot) => sum + slot.available, 0)
  const totalBooked = avails.reduce((sum, slot) => sum + slot.booked, 0)
  const overallSellout = calculateSellout(totalAvailable, totalBooked)

  return (
    <Box>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5">
            Available Inventory Report
          </Typography>
          {date && (
            <Typography variant="body2" color="textSecondary">
              {new Date(date).toLocaleDateString()}
            </Typography>
          )}
        </Box>

        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2, bgcolor: 'primary.light', color: 'primary.contrastText' }}>
              <Typography variant="h6">Total Available</Typography>
              <Typography variant="h4">{totalAvailable}</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2, bgcolor: 'success.light', color: 'success.contrastText' }}>
              <Typography variant="h6">Total Booked</Typography>
              <Typography variant="h4">{totalBooked}</Typography>
            </Paper>
          </Grid>
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2, bgcolor: 'warning.light', color: 'warning.contrastText' }}>
              <Typography variant="h6">Sellout %</Typography>
              <Typography variant="h4">{overallSellout}%</Typography>
            </Paper>
          </Grid>
        </Grid>

        {avails.length === 0 ? (
          <Alert severity="info">No availability data available for this log.</Alert>
        ) : (
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Hour</TableCell>
                  <TableCell>Break Position</TableCell>
                  <TableCell>Available</TableCell>
                  <TableCell>Booked</TableCell>
                  <TableCell>Sellout %</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {Object.keys(grouped)
                  .sort((a, b) => Number(a) - Number(b))
                  .map(hour => {
                    const slots = grouped[Number(hour)]
                    return slots.map((slot, index) => {
                      const sellout = calculateSellout(slot.available, slot.booked)
                      return (
                        <TableRow key={`${hour}-${slot.break_position}-${index}`}>
                          {index === 0 && (
                            <TableCell rowSpan={slots.length}>
                              {hour}:00
                            </TableCell>
                          )}
                          <TableCell>{slot.break_position}</TableCell>
                          <TableCell>{slot.available}</TableCell>
                          <TableCell>{slot.booked}</TableCell>
                          <TableCell>
                            <Chip
                              label={`${sellout}%`}
                              size="small"
                              color={
                                sellout >= 90
                                  ? 'error'
                                  : sellout >= 70
                                  ? 'warning'
                                  : 'success'
                              }
                            />
                          </TableCell>
                          <TableCell>
                            {slot.sold_out ? (
                              <Chip label="Sold Out" size="small" color="error" />
                            ) : (
                              <Chip label="Available" size="small" color="success" />
                            )}
                          </TableCell>
                        </TableRow>
                      )
                    })
                  })}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </Box>
  )
}

export default AvailsReport


