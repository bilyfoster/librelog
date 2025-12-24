import React, { useState } from 'react'
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Chip,
  Tooltip,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
  Grid,
  Alert,
  CircularProgress,
} from '@mui/material'
import {
  Refresh as RefreshIcon,
  FilterList as FilterIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material'
import { useQuery } from '@tanstack/react-query'
import {
  getTrafficLogsProxy,
  getTrafficLogStats,
} from '../../utils/api'
import { format } from 'date-fns'

interface TrafficLog {
  id?: string
  log_type: string
  log_id?: string
  spot_id?: string
  order_id?: string
  campaign_id?: string
  copy_id?: string
  user_id?: string
  message: string
  metadata?: Record<string, any>
  username?: string
  created_at: string
}

const TrafficLogs: React.FC = () => {
  const [filters, setFilters] = useState({
    log_type: '',
    start_date: '',
    end_date: '',
    limit: 100,
  })
  const [selectedLog, setSelectedLog] = useState<TrafficLog | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  const { data: logsData, isLoading, error, refetch } = useQuery({
    queryKey: ['traffic-logs', filters],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getTrafficLogsProxy({
        log_type: filters.log_type || undefined,
        limit: filters.limit,
      })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const logs = Array.isArray(logsData) ? logsData : (logsData?.logs || logsData || [])

  const { data: stats } = useQuery({
    queryKey: ['traffic-log-stats', filters],
    queryFn: () =>
      getTrafficLogStats({
        start_date: filters.start_date || undefined,
        end_date: filters.end_date || undefined,
      }),
    retry: 1,
  })

  const handleViewDetail = (log: TrafficLog) => {
    setSelectedLog(log)
    setDetailOpen(true)
  }

  const logTypes = [
    'SPOT_SCHEDULED',
    'SPOT_MOVED',
    'SPOT_DELETED',
    'LOG_LOCKED',
    'LOG_UNLOCKED',
    'LOG_PUBLISHED',
    'CONFLICT_DETECTED',
    'CONFLICT_RESOLVED',
    'COPY_ASSIGNED',
    'COPY_UNASSIGNED',
    'ORDER_CREATED',
    'ORDER_UPDATED',
    'ORDER_APPROVED',
    'ORDER_CANCELLED',
  ]

  if (isLoading && logs.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px" p={3}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Traffic Logs</Typography>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load traffic logs: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
        <Button onClick={() => refetch()} sx={{ mt: 2 }}>Retry</Button>
      </Box>
    )
  }

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Traffic Logs</Typography>
        <Button
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={() => refetch()}
        >
          Refresh
        </Button>
      </Box>

      {/* Stats Cards */}
      {stats && (
        <Grid container spacing={2} mb={3}>
          <Grid item xs={12} sm={6} md={3}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  Total Logs
                </Typography>
                <Typography variant="h5">{stats.total || 0}</Typography>
              </CardContent>
            </Card>
          </Grid>
          {stats.by_type &&
            Object.entries(stats.by_type).slice(0, 3).map(([type, count]) => (
              <Grid item xs={12} sm={6} md={3} key={type}>
                <Card>
                  <CardContent>
                    <Typography color="textSecondary" gutterBottom>
                      {type.replace(/_/g, ' ')}
                    </Typography>
                    <Typography variant="h5">{count as number}</Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
        </Grid>
      )}

      {/* Filters */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6} md={3}>
              <FormControl fullWidth>
                <InputLabel>Log Type</InputLabel>
                <Select
                  value={filters.log_type}
                  onChange={(e) =>
                    setFilters({ ...filters, log_type: e.target.value })
                  }
                  label="Log Type"
                >
                  <MenuItem value="">All</MenuItem>
                  {logTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="Start Date"
                type="date"
                value={filters.start_date}
                onChange={(e) =>
                  setFilters({ ...filters, start_date: e.target.value })
                }
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="End Date"
                type="date"
                value={filters.end_date}
                onChange={(e) =>
                  setFilters({ ...filters, end_date: e.target.value })
                }
                fullWidth
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <TextField
                label="Limit"
                type="number"
                value={filters.limit}
                onChange={(e) =>
                  setFilters({
                    ...filters,
                    limit: parseInt(e.target.value) || 100,
                  })
                }
                fullWidth
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Logs Table */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Time</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>Message</TableCell>
              <TableCell>User</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography>Loading...</Typography>
                </TableCell>
              </TableRow>
            ) : logs.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  <Typography color="textSecondary">No logs found</Typography>
                </TableCell>
              </TableRow>
            ) : (
              logs.map((log: TrafficLog) => (
                <TableRow key={log.id}>
                  <TableCell>
                    {format(new Date(log.created_at), 'MMM dd, yyyy HH:mm:ss')}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={log.log_type.replace(/_/g, ' ')}
                      size="small"
                      color="primary"
                    />
                  </TableCell>
                  <TableCell>{log.message}</TableCell>
                  <TableCell>{log.username || 'Unknown'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="View Details">
                      <IconButton
                        size="small"
                        onClick={() => handleViewDetail(log)}
                      >
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Detail Dialog */}
      <Dialog
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>Traffic Log Details</DialogTitle>
        <DialogContent>
          {selectedLog && (
            <Box display="flex" flexDirection="column" gap={2} pt={1}>
              <Typography>
                <strong>Type:</strong> {selectedLog.log_type.replace(/_/g, ' ')}
              </Typography>
              <Typography>
                <strong>Message:</strong> {selectedLog.message}
              </Typography>
              <Typography>
                <strong>User:</strong> {selectedLog.username || 'Unknown'}
              </Typography>
              <Typography>
                <strong>Time:</strong>{' '}
                {format(new Date(selectedLog.created_at), 'PPpp')}
              </Typography>
              {selectedLog.metadata && (
                <Box>
                  <Typography>
                    <strong>Metadata:</strong>
                  </Typography>
                  <pre style={{ background: '#f5f5f5', padding: '8px', borderRadius: '4px' }}>
                    {JSON.stringify(selectedLog.metadata, null, 2)}
                  </pre>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default TrafficLogs

