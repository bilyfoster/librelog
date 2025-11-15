import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  Paper,
  Button,
  Grid,
  TextField,
  MenuItem,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  CircularProgress,
} from '@mui/material'
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material'
import { getOrders, createSpotsBulk, getSpots } from '../../utils/api'

interface Order {
  id: number
  order_number: string
  advertiser_id: number
  start_date: string
  end_date: string
  total_spots: number
  spot_lengths: number[]
  rate_type: string
  status: string
}

interface Spot {
  id: number
  order_id: number
  scheduled_date: string
  scheduled_time: string
  spot_length: number
  break_position?: string
  daypart?: string
  status: string
}

const SpotScheduler: React.FC = () => {
  const [orders, setOrders] = useState<Order[]>([])
  const [selectedOrder, setSelectedOrder] = useState<number | ''>('')
  const [startDate, setStartDate] = useState<Date | null>(new Date())
  const [endDate, setEndDate] = useState<Date | null>(new Date())
  const [spotLength, setSpotLength] = useState<number>(30)
  const [breakPosition, setBreakPosition] = useState<string>('')
  const [daypart, setDaypart] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [scheduling, setScheduling] = useState(false)
  const [scheduledSpots, setScheduledSpots] = useState<Spot[]>([])
  const [previewOpen, setPreviewOpen] = useState(false)

  useEffect(() => {
    loadOrders()
  }, [])

  const loadOrders = async () => {
    try {
      setLoading(true)
      const data = await getOrders({ status: 'APPROVED', limit: 100 })
      setOrders(data.orders || data || [])
    } catch (error) {
      console.error('Failed to load orders:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleSchedule = async () => {
    if (!selectedOrder || !startDate || !endDate) {
      alert('Please select an order and date range')
      return
    }

    try {
      setScheduling(true)
      
      // Generate spots for the date range
      const spots: any[] = []
      const currentDate = new Date(startDate)
      const end = new Date(endDate)
      
      while (currentDate <= end) {
        // Generate spots for this day based on order requirements
        // This is a simplified version - the actual scheduling logic is in the backend
        const dateStr = currentDate.toISOString().split('T')[0]
        
        // Add a few example spots per day
        for (let hour = 6; hour < 22; hour += 2) {
          spots.push({
            order_id: selectedOrder,
            scheduled_date: dateStr,
            scheduled_time: `${hour.toString().padStart(2, '0')}:00:00`,
            spot_length: spotLength,
            break_position: breakPosition || undefined,
            daypart: daypart || undefined,
            status: 'SCHEDULED',
          })
        }
        
        currentDate.setDate(currentDate.getDate() + 1)
      }

      const created = await createSpotsBulk(Number(selectedOrder), spots)
      setScheduledSpots(created)
      setPreviewOpen(true)
      
      // Reload orders to update spot counts
      await loadOrders()
    } catch (error) {
      console.error('Failed to schedule spots:', error)
      alert('Failed to schedule spots. Please check the console for details.')
    } finally {
      setScheduling(false)
    }
  }

  const selectedOrderData = orders.find(o => o.id === Number(selectedOrder))

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Spot Scheduler
        </Typography>
        <Typography variant="body2" color="textSecondary" sx={{ mb: 3 }}>
          Schedule spots from approved orders into daily logs.
        </Typography>

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <FormControl fullWidth>
              <InputLabel>Select Order</InputLabel>
              <Select
                value={selectedOrder}
                onChange={(e) => setSelectedOrder(e.target.value as number | '')}
                label="Select Order"
              >
                {orders.map(order => (
                  <MenuItem key={order.id} value={order.id}>
                    {order.order_number} - {order.total_spots} spots
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          {selectedOrderData && (
            <>
              <Grid item xs={12} md={6}>
                <Alert severity="info">
                  Order: {selectedOrderData.order_number}
                  <br />
                  Spot Lengths: {selectedOrderData.spot_lengths?.join(', ') || 'N/A'}
                  <br />
                  Rate Type: {selectedOrderData.rate_type}
                </Alert>
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="Start Date"
                  type="date"
                  value={startDate ? startDate.toISOString().split('T')[0] : ''}
                  onChange={(e) => setStartDate(e.target.value ? new Date(e.target.value) : null)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <TextField
                  fullWidth
                  label="End Date"
                  type="date"
                  value={endDate ? endDate.toISOString().split('T')[0] : ''}
                  onChange={(e) => setEndDate(e.target.value ? new Date(e.target.value) : null)}
                  InputLabelProps={{ shrink: true }}
                />
              </Grid>

                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Spot Length (seconds)"
                    type="number"
                    value={spotLength}
                    onChange={(e) => setSpotLength(Number(e.target.value))}
                    select
                  >
                    {selectedOrderData.spot_lengths?.map(length => (
                      <MenuItem key={length} value={length}>
                        {length}s
                      </MenuItem>
                    ))}
                    {(!selectedOrderData.spot_lengths || selectedOrderData.spot_lengths.length === 0) && (
                      <MenuItem value={30}>30s</MenuItem>
                    )}
                  </TextField>
                </Grid>

                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Break Position"
                    value={breakPosition}
                    onChange={(e) => setBreakPosition(e.target.value)}
                    select
                  >
                    <MenuItem value="">Any</MenuItem>
                    <MenuItem value="A">A</MenuItem>
                    <MenuItem value="B">B</MenuItem>
                    <MenuItem value="C">C</MenuItem>
                    <MenuItem value="D">D</MenuItem>
                  </TextField>
                </Grid>

                <Grid item xs={12} md={4}>
                  <TextField
                    fullWidth
                    label="Daypart"
                    value={daypart}
                    onChange={(e) => setDaypart(e.target.value)}
                    select
                  >
                    <MenuItem value="">Any</MenuItem>
                    <MenuItem value="MORNING">Morning</MenuItem>
                    <MenuItem value="MIDDAY">Midday</MenuItem>
                    <MenuItem value="AFTERNOON">Afternoon</MenuItem>
                    <MenuItem value="EVENING">Evening</MenuItem>
                    <MenuItem value="OVERNIGHT">Overnight</MenuItem>
                  </TextField>
                </Grid>

                <Grid item xs={12}>
                  <Button
                    variant="contained"
                    startIcon={<ScheduleIcon />}
                    onClick={handleSchedule}
                    disabled={scheduling || !startDate || !endDate}
                    size="large"
                  >
                    {scheduling ? <CircularProgress size={24} /> : 'Schedule Spots'}
                  </Button>
                </Grid>
              </>
            )}
          </Grid>
        </Paper>

        {/* Preview Dialog */}
        <Dialog open={previewOpen} onClose={() => setPreviewOpen(false)} maxWidth="md" fullWidth>
          <DialogTitle>Scheduled Spots Preview</DialogTitle>
          <DialogContent>
            <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
              {scheduledSpots.length} spots have been scheduled.
            </Typography>
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Date</TableCell>
                    <TableCell>Time</TableCell>
                    <TableCell>Length</TableCell>
                    <TableCell>Break</TableCell>
                    <TableCell>Status</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {scheduledSpots.slice(0, 20).map(spot => (
                    <TableRow key={spot.id}>
                      <TableCell>{new Date(spot.scheduled_date).toLocaleDateString()}</TableCell>
                      <TableCell>{spot.scheduled_time.substring(0, 5)}</TableCell>
                      <TableCell>{spot.spot_length}s</TableCell>
                      <TableCell>{spot.break_position || 'N/A'}</TableCell>
                      <TableCell>
                        <Chip label={spot.status} size="small" color="primary" />
                      </TableCell>
                    </TableRow>
                  ))}
                  {scheduledSpots.length > 20 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography variant="body2" color="textSecondary">
                          ... and {scheduledSpots.length - 20} more spots
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setPreviewOpen(false)}>Close</Button>
            <Button variant="contained" onClick={() => window.location.href = '/logs'}>
              View Logs
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
  )
}

export default SpotScheduler

