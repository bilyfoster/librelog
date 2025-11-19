import React, { useState } from 'react'
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
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material'
import {
  Visibility as VisibilityIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material'
import { getAuditLogsProxy, getAuditLog } from '../../utils/api'
import api from '../../utils/api'

interface AuditLog {
  id: number
  user_id: number
  username?: string
  action: string
  resource_type: string
  resource_id?: number
  changes?: Record<string, any>
  ip_address?: string
  user_agent?: string
  created_at: string
}

const AuditLogs: React.FC = () => {
  const [userFilter, setUserFilter] = useState<number | ''>('')
  const [actionFilter, setActionFilter] = useState<string>('')
  const [resourceTypeFilter, setResourceTypeFilter] = useState<string>('')
  const [startDate, setStartDate] = useState(
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [endDate, setEndDate] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null)

  const { data: auditLogs, isLoading, error } = useQuery({
    queryKey: ['audit-logs', userFilter, actionFilter, resourceTypeFilter, startDate, endDate],
    queryFn: async () => {
      const params: any = { limit: 100 }
      if (userFilter) params.user_id = userFilter
      if (actionFilter) params.action = actionFilter
      if (resourceTypeFilter) params.resource_type = resourceTypeFilter
      if (startDate) params.start_date = startDate
      if (endDate) params.end_date = endDate
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAuditLogsProxy(params)
      return data
    },
    retry: 1,
  })

  const { data: users } = useQuery({
    queryKey: ['users'],
    queryFn: async () => {
      // Note: We may need to add a users endpoint if it doesn't exist
      // For now, we'll extract unique users from audit logs
      return []
    },
  })

  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const handleViewDetails = async (log: AuditLog) => {
    try {
      setErrorMessage(null)
      const detail = await getAuditLog(log.id)
      setSelectedLog(detail)
      setDetailOpen(true)
    } catch (err: any) {
      let message = 'Failed to load audit log details'
      if (err?.response?.data?.detail) {
        message = err.response.data.detail
      } else if (err?.response?.data?.message) {
        message = err.response.data.message
      } else if (err?.message) {
        message = err.message
      }
      setErrorMessage(message)
      console.error('Failed to load audit log details', err)
    }
  }

  const getActionColor = (action: string) => {
    switch (action.toUpperCase()) {
      case 'CREATE':
        return 'success'
      case 'UPDATE':
        return 'info'
      case 'DELETE':
        return 'error'
      case 'VIEW':
        return 'default'
      default:
        return 'default'
    }
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
          <Button color="inherit" size="small" onClick={() => window.location.reload()}>
            Retry
          </Button>
        }>
          Failed to load audit logs: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>
          Audit Logs
        </Typography>

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <TextField
            label="Start Date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ minWidth: 150 }}
          />
          <TextField
            label="End Date"
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            sx={{ minWidth: 150 }}
          />
          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel>Action</InputLabel>
            <Select
              value={actionFilter}
              onChange={(e) => setActionFilter(e.target.value)}
              label="Action"
            >
              <MenuItem value="">All Actions</MenuItem>
              <MenuItem value="CREATE">Create</MenuItem>
              <MenuItem value="UPDATE">Update</MenuItem>
              <MenuItem value="DELETE">Delete</MenuItem>
              <MenuItem value="VIEW">View</MenuItem>
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel>Resource Type</InputLabel>
            <Select
              value={resourceTypeFilter}
              onChange={(e) => setResourceTypeFilter(e.target.value)}
              label="Resource Type"
            >
              <MenuItem value="">All Types</MenuItem>
              <MenuItem value="Order">Order</MenuItem>
              <MenuItem value="Invoice">Invoice</MenuItem>
              <MenuItem value="Spot">Spot</MenuItem>
              <MenuItem value="Copy">Copy</MenuItem>
              <MenuItem value="Campaign">Campaign</MenuItem>
              <MenuItem value="DailyLog">Daily Log</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Audit Logs Table */}
        {auditLogs && auditLogs.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Timestamp</TableCell>
                  <TableCell>User</TableCell>
                  <TableCell>Action</TableCell>
                  <TableCell>Resource</TableCell>
                  <TableCell>Resource ID</TableCell>
                  <TableCell>IP Address</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {auditLogs.map((log: AuditLog) => (
                  <TableRow key={log.id}>
                    <TableCell>
                      {new Date(log.created_at).toLocaleString()}
                    </TableCell>
                    <TableCell>
                      {log.username || `User #${log.user_id}`}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={log.action}
                        size="small"
                        color={getActionColor(log.action) as any}
                      />
                    </TableCell>
                    <TableCell>{log.resource_type}</TableCell>
                    <TableCell>{log.resource_id || '-'}</TableCell>
                    <TableCell>{log.ip_address || '-'}</TableCell>
                    <TableCell>
                      <Tooltip title="View Details">
                        <IconButton
                          size="small"
                          onClick={() => handleViewDetails(log)}
                        >
                          <VisibilityIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No audit logs found for the selected filters.</Alert>
        )}
      </Paper>

      {/* Detail Dialog */}
      <Dialog open={detailOpen} onClose={() => setDetailOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Audit Log Details</DialogTitle>
        <DialogContent>
          {selectedLog && (
            <Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Timestamp
                </Typography>
                <Typography variant="body1">
                  {new Date(selectedLog.created_at).toLocaleString()}
                </Typography>
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  User
                </Typography>
                <Typography variant="body1">
                  {selectedLog.username || `User #${selectedLog.user_id}`}
                </Typography>
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Action
                </Typography>
                <Chip
                  label={selectedLog.action}
                  size="small"
                  color={getActionColor(selectedLog.action) as any}
                />
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Resource Type
                </Typography>
                <Typography variant="body1">{selectedLog.resource_type}</Typography>
              </Box>
              {selectedLog.resource_id && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Resource ID
                  </Typography>
                  <Typography variant="body1">{selectedLog.resource_id}</Typography>
                </Box>
              )}
              {selectedLog.ip_address && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    IP Address
                  </Typography>
                  <Typography variant="body1">{selectedLog.ip_address}</Typography>
                </Box>
              )}
              {selectedLog.user_agent && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    User Agent
                  </Typography>
                  <Typography variant="body1">{selectedLog.user_agent}</Typography>
                </Box>
              )}
              {selectedLog.changes && Object.keys(selectedLog.changes).length > 0 && (
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="h6">Changes</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <pre style={{ whiteSpace: 'pre-wrap', fontSize: '0.875rem' }}>
                      {JSON.stringify(selectedLog.changes, null, 2)}
                    </pre>
                  </AccordionDetails>
                </Accordion>
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

export default AuditLogs

