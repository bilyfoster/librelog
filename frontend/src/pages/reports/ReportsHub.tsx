import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Paper,
  Tabs,
  Tab,
  TextField,
  Button,
  Grid,
  Card,
  CardContent,
  CardActions,
  Alert,
  CircularProgress,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from '@mui/material'
import {
  Download as DownloadIcon,
  Assessment as ReportIcon,
} from '@mui/icons-material'
import {
  getDailyLogReport,
  getMissingCopyReport,
  getAvailsReport,
  getConflictsReport,
  getExpirationsReport,
  getContractActualizationReport,
  getRevenueSummaryReport,
  getARAgingReport,
  getMakegoodsReport,
  getRevenueByRepReport,
  getRevenueByAdvertiserReport,
  getPendingOrdersReport,
  getExpiringContractsReport,
  exportReport,
} from '../../utils/api'
import { getOrdersProxy } from '../../utils/api'
import api from '../../utils/api'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`report-tabpanel-${index}`}
      aria-labelledby={`report-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const ReportsHub: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0)
  const [selectedReport, setSelectedReport] = useState<string | null>(null)
  const [reportData, setReportData] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  // Date range state
  const [startDate, setStartDate] = useState(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
  )
  const [endDate, setEndDate] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [logDate, setLogDate] = useState(
    new Date().toISOString().split('T')[0]
  )
  const [daysAhead, setDaysAhead] = useState(30)
  const [selectedOrderId, setSelectedOrderId] = useState<number | ''>('')

  const { data: orders } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getOrdersProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue)
    setSelectedReport(null)
    setReportData(null)
    setError(null)
  }

  const handleGenerateReport = async (reportType: string) => {
    setLoading(true)
    setError(null)
    setSelectedReport(reportType)

    try {
      let data: any

      switch (reportType) {
        case 'daily-log':
          data = await getDailyLogReport(logDate)
          break
        case 'missing-copy':
          data = await getMissingCopyReport(startDate, endDate)
          break
        case 'avails':
          data = await getAvailsReport(startDate, endDate)
          break
        case 'conflicts':
          data = await getConflictsReport(logDate)
          break
        case 'expirations':
          data = await getExpirationsReport(daysAhead)
          break
        case 'contract-actualization':
          if (!selectedOrderId) {
            throw new Error('Please select an order')
          }
          data = await getContractActualizationReport(
            selectedOrderId as number,
            startDate,
            endDate
          )
          break
        case 'revenue-summary':
          data = await getRevenueSummaryReport(startDate, endDate)
          break
        case 'ar-aging':
          data = await getARAgingReport()
          break
        case 'makegoods':
          data = await getMakegoodsReport(startDate, endDate)
          break
        case 'revenue-by-rep':
          data = await getRevenueByRepReport(startDate, endDate)
          break
        case 'revenue-by-advertiser':
          data = await getRevenueByAdvertiserReport(startDate, endDate)
          break
        case 'pending-orders':
          data = await getPendingOrdersReport()
          break
        case 'expiring-contracts':
          data = await getExpiringContractsReport(daysAhead)
          break
        default:
          throw new Error('Unknown report type')
      }

      setReportData(data)
    } catch (err: any) {
      let message = 'Failed to generate report'
      if (err?.response?.data?.detail) {
        message = err.response.data.detail
      } else if (err?.response?.data?.message) {
        message = err.response.data.message
      } else if (err?.message) {
        message = err.message
      }
      setError(message)
      setErrorMessage(message)
      setReportData(null)
      console.error('Generate report error:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async (format: 'pdf' | 'excel' | 'csv') => {
    if (!selectedReport) return

    try {
      const params: any = {}
      if (startDate && endDate) {
        params.start_date = startDate
        params.end_date = endDate
      }
      if (logDate) {
        params.log_date = logDate
      }
      if (daysAhead) {
        params.days_ahead = daysAhead
      }
      if (selectedOrderId) {
        params.order_id = selectedOrderId
      }

      const blob = await exportReport(selectedReport, format, params)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${selectedReport}-${new Date().toISOString()}.${format === 'excel' ? 'xlsx' : format}`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
    } catch (err: any) {
      let message = 'Failed to export report'
      if (err?.response?.data?.detail) {
        message = err.response.data.detail
      } else if (err?.response?.data?.message) {
        message = err.response.data.message
      } else if (err?.message) {
        message = err.message
      }
      setError(message)
      setErrorMessage(message)
      console.error('Export report error:', err)
    }
  }

  const formatCurrency = (amount: number | string) => {
    const num = typeof amount === 'string' ? parseFloat(amount) : amount
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num)
  }

  const renderReportData = () => {
    if (!reportData) return null

    // Handle different report data structures
    if (Array.isArray(reportData)) {
      if (reportData.length === 0) {
        return <Alert severity="info">No data found for this report</Alert>
      }

      // Try to render as table
      const firstItem = reportData[0]
      if (typeof firstItem === 'object') {
        const keys = Object.keys(firstItem)
        return (
          <TableContainer component={Paper} sx={{ mt: 2 }}>
            <Table>
              <TableHead>
                <TableRow>
                  {keys.map((key) => (
                    <TableCell key={key} sx={{ textTransform: 'capitalize' }}>
                      {key.replace(/_/g, ' ')}
                    </TableCell>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {reportData.map((row: any, idx: number) => (
                  <TableRow key={idx}>
                    {keys.map((key) => (
                      <TableCell key={key}>
                        {typeof row[key] === 'number' && key.includes('amount') || key.includes('revenue') || key.includes('total')
                          ? formatCurrency(row[key])
                          : String(row[key] || '-')}
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )
      }
    }

    // Handle object with summary data
    if (typeof reportData === 'object') {
      return (
        <Box sx={{ mt: 2 }}>
          <Grid container spacing={2}>
            {Object.entries(reportData).map(([key, value]) => (
              <Grid item xs={12} sm={6} md={4} key={key}>
                <Card>
                  <CardContent>
                    <Typography variant="h6" color="text.secondary">
                      {key.replace(/_/g, ' ').replace(/\b\w/g, (l) => l.toUpperCase())}
                    </Typography>
                    <Typography variant="h4">
                      {typeof value === 'number' && (key.includes('amount') || key.includes('revenue') || key.includes('total'))
                        ? formatCurrency(value)
                        : String(value)}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Box>
      )
    }

    return <Alert severity="info">Report data format not recognized</Alert>
  }

  const trafficReports = [
    { id: 'daily-log', name: 'Daily Log Report', requiresDate: true },
    { id: 'missing-copy', name: 'Missing Copy Report', requiresDateRange: true },
    { id: 'avails', name: 'Avails Report', requiresDateRange: true },
    { id: 'conflicts', name: 'Conflicts Report', requiresDate: true },
    { id: 'expirations', name: 'Expirations Report', requiresDaysAhead: true },
  ]

  const billingReports = [
    { id: 'contract-actualization', name: 'Contract Actualization', requiresOrder: true, requiresDateRange: true },
    { id: 'revenue-summary', name: 'Revenue Summary', requiresDateRange: true },
    { id: 'ar-aging', name: 'AR Aging Report', noParams: true },
    { id: 'makegoods', name: 'Makegoods Report', requiresDateRange: true },
  ]

  const salesReports = [
    { id: 'revenue-by-rep', name: 'Revenue by Rep', requiresDateRange: true },
    { id: 'revenue-by-advertiser', name: 'Revenue by Advertiser', requiresDateRange: true },
    { id: 'pending-orders', name: 'Pending Orders', noParams: true },
    { id: 'expiring-contracts', name: 'Expiring Contracts', requiresDaysAhead: true },
  ]

  return (
    <Box sx={{ p: 3 }}>
      {(error || errorMessage) && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => { setError(null); setErrorMessage(null); }}>
          {errorMessage || error || 'An error occurred'}
        </Alert>
      )}
      <Typography variant="h4" gutterBottom>
        Reports & Analytics
      </Typography>

      <Paper sx={{ mt: 2 }}>
        <Tabs value={activeTab} onChange={handleTabChange}>
          <Tab label="Traffic Reports" />
          <Tab label="Billing Reports" />
          <Tab label="Sales Reports" />
        </Tabs>

        {/* Traffic Reports */}
        <TabPanel value={activeTab} index={0}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Select Report
                </Typography>
                {trafficReports.map((report) => (
                  <Card key={report.id} sx={{ mb: 2 }}>
                    <CardContent>
                      <Typography variant="h6">{report.name}</Typography>
                    </CardContent>
                    <CardActions>
                      <Button
                        size="small"
                        variant="contained"
                        startIcon={<ReportIcon />}
                        onClick={() => handleGenerateReport(report.id)}
                        disabled={loading}
                      >
                        Generate
                      </Button>
                    </CardActions>
                  </Card>
                ))}
              </Paper>

              <Paper sx={{ p: 2, mt: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Parameters
                </Typography>
                {trafficReports.find((r) => r.id === selectedReport)?.requiresDate && (
                  <TextField
                    label="Log Date"
                    type="date"
                    value={logDate}
                    onChange={(e) => setLogDate(e.target.value)}
                    fullWidth
                    margin="normal"
                    InputLabelProps={{ shrink: true }}
                  />
                )}
                {trafficReports.find((r) => r.id === selectedReport)?.requiresDateRange && (
                  <>
                    <TextField
                      label="Start Date"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                      label="End Date"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                  </>
                )}
                {trafficReports.find((r) => r.id === selectedReport)?.requiresDaysAhead && (
                  <TextField
                    label="Days Ahead"
                    type="number"
                    value={daysAhead}
                    onChange={(e) => setDaysAhead(parseInt(e.target.value) || 30)}
                    fullWidth
                    margin="normal"
                    inputProps={{ min: 1, max: 365 }}
                  />
                )}
              </Paper>
            </Grid>

            <Grid item xs={12} md={8}>
              <Paper sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6">Report Results</Typography>
                  {selectedReport && (
                    <Box>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('pdf')}
                        sx={{ mr: 1 }}
                      >
                        PDF
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('excel')}
                        sx={{ mr: 1 }}
                      >
                        Excel
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('csv')}
                      >
                        CSV
                      </Button>
                    </Box>
                  )}
                </Box>

                {loading && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                    <CircularProgress />
                  </Box>
                )}

                {error && (
                  <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                  </Alert>
                )}

                {renderReportData()}
              </Paper>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Billing Reports */}
        <TabPanel value={activeTab} index={1}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Select Report
                </Typography>
                {billingReports.map((report) => (
                  <Card key={report.id} sx={{ mb: 2 }}>
                    <CardContent>
                      <Typography variant="h6">{report.name}</Typography>
                    </CardContent>
                    <CardActions>
                      <Button
                        size="small"
                        variant="contained"
                        startIcon={<ReportIcon />}
                        onClick={() => handleGenerateReport(report.id)}
                        disabled={loading}
                      >
                        Generate
                      </Button>
                    </CardActions>
                  </Card>
                ))}
              </Paper>

              <Paper sx={{ p: 2, mt: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Parameters
                </Typography>
                {billingReports.find((r) => r.id === selectedReport)?.requiresOrder && (
                  <FormControl fullWidth margin="normal">
                    <InputLabel>Order</InputLabel>
                    <Select
                      value={selectedOrderId}
                      onChange={(e) => setSelectedOrderId(e.target.value as number)}
                      label="Order"
                    >
                      {orders?.map((order: any) => (
                        <MenuItem key={order.id} value={order.id}>
                          {order.order_number || `Order #${order.id}`}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
                {billingReports.find((r) => r.id === selectedReport)?.requiresDateRange && (
                  <>
                    <TextField
                      label="Start Date"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                      label="End Date"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                  </>
                )}
              </Paper>
            </Grid>

            <Grid item xs={12} md={8}>
              <Paper sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6">Report Results</Typography>
                  {selectedReport && (
                    <Box>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('pdf')}
                        sx={{ mr: 1 }}
                      >
                        PDF
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('excel')}
                        sx={{ mr: 1 }}
                      >
                        Excel
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('csv')}
                      >
                        CSV
                      </Button>
                    </Box>
                  )}
                </Box>

                {loading && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                    <CircularProgress />
                  </Box>
                )}

                {error && (
                  <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                  </Alert>
                )}

                {renderReportData()}
              </Paper>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Sales Reports */}
        <TabPanel value={activeTab} index={2}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <Paper sx={{ p: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Select Report
                </Typography>
                {salesReports.map((report) => (
                  <Card key={report.id} sx={{ mb: 2 }}>
                    <CardContent>
                      <Typography variant="h6">{report.name}</Typography>
                    </CardContent>
                    <CardActions>
                      <Button
                        size="small"
                        variant="contained"
                        startIcon={<ReportIcon />}
                        onClick={() => handleGenerateReport(report.id)}
                        disabled={loading}
                      >
                        Generate
                      </Button>
                    </CardActions>
                  </Card>
                ))}
              </Paper>

              <Paper sx={{ p: 2, mt: 2 }}>
                <Typography variant="h6" gutterBottom>
                  Parameters
                </Typography>
                {salesReports.find((r) => r.id === selectedReport)?.requiresDateRange && (
                  <>
                    <TextField
                      label="Start Date"
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                    <TextField
                      label="End Date"
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      fullWidth
                      margin="normal"
                      InputLabelProps={{ shrink: true }}
                    />
                  </>
                )}
                {salesReports.find((r) => r.id === selectedReport)?.requiresDaysAhead && (
                  <TextField
                    label="Days Ahead"
                    type="number"
                    value={daysAhead}
                    onChange={(e) => setDaysAhead(parseInt(e.target.value) || 30)}
                    fullWidth
                    margin="normal"
                    inputProps={{ min: 1, max: 365 }}
                  />
                )}
              </Paper>
            </Grid>

            <Grid item xs={12} md={8}>
              <Paper sx={{ p: 2 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
                  <Typography variant="h6">Report Results</Typography>
                  {selectedReport && (
                    <Box>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('pdf')}
                        sx={{ mr: 1 }}
                      >
                        PDF
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('excel')}
                        sx={{ mr: 1 }}
                      >
                        Excel
                      </Button>
                      <Button
                        size="small"
                        startIcon={<DownloadIcon />}
                        onClick={() => handleExport('csv')}
                      >
                        CSV
                      </Button>
                    </Box>
                  )}
                </Box>

                {loading && (
                  <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
                    <CircularProgress />
                  </Box>
                )}

                {error && (
                  <Alert severity="error" sx={{ mb: 2 }}>
                    {error}
                  </Alert>
                )}

                {renderReportData()}
              </Paper>
            </Grid>
          </Grid>
        </TabPanel>
      </Paper>
    </Box>
  )
}

export default ReportsHub
