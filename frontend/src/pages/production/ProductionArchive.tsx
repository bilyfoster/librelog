import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Card,
  CardContent,
  TextField,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Grid,
} from '@mui/material'
import { Search } from '@mui/icons-material'
import api from '../../utils/api'

interface TabPanelProps {
  children?: React.ReactNode
  index: number
  value: number
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const ProductionArchive: React.FC = () => {
  const [tabValue, setTabValue] = useState(0)
  const [clientSearch, setClientSearch] = useState('')
  const [scriptSearch, setScriptSearch] = useState('')
  const [spotLengthSearch, setSpotLengthSearch] = useState('')

  const { data: clientResults, isLoading: clientLoading } = useQuery({
    queryKey: ['production-archive-client', clientSearch],
    queryFn: async () => {
      if (!clientSearch) return { results: [] }
      const response = await api.get('/production-archive/search/client', {
        params: { client_name: clientSearch, limit: 50 }
      })
      return response.data
    },
    enabled: !!clientSearch && clientSearch.length >= 2,
  })

  const { data: scriptResults, isLoading: scriptLoading } = useQuery({
    queryKey: ['production-archive-script', scriptSearch],
    queryFn: async () => {
      if (!scriptSearch) return { results: [] }
      const response = await api.get('/production-archive/search/script', {
        params: { keyword: scriptSearch, limit: 50 }
      })
      return response.data
    },
    enabled: !!scriptSearch && scriptSearch.length >= 3,
  })

  const { data: spotLengthResults, isLoading: spotLengthLoading } = useQuery({
    queryKey: ['production-archive-spot-length', spotLengthSearch],
    queryFn: async () => {
      if (!spotLengthSearch) return { results: [] }
      const response = await api.get('/production-archive/search/spot-length', {
        params: { spot_length: parseInt(spotLengthSearch), limit: 50 }
      })
      return response.data
    },
    enabled: !!spotLengthSearch && !isNaN(parseInt(spotLengthSearch)),
  })

  const { data: historyResults, isLoading: historyLoading } = useQuery({
    queryKey: ['production-archive-history'],
    queryFn: async () => {
      const response = await api.get('/production-archive/history', {
        params: { limit: 100 }
      })
      return response.data
    },
  })

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Production Archive
      </Typography>

      <Card>
        <CardContent>
          <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
            <Tab label="Search by Client" />
            <Tab label="Search by Script" />
            <Tab label="Search by Spot Length" />
            <Tab label="Recent History" />
          </Tabs>

          <TabPanel value={tabValue} index={0}>
            <Box mb={2}>
              <TextField
                fullWidth
                label="Client Name"
                value={clientSearch}
                onChange={(e) => setClientSearch(e.target.value)}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />,
                }}
              />
            </Box>
            {clientLoading ? (
              <CircularProgress />
            ) : clientResults?.results?.length > 0 ? (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>PO Number</TableCell>
                      <TableCell>Client</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Created</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {clientResults.results.map((item: any) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.po_number}</TableCell>
                        <TableCell>{item.client_name}</TableCell>
                        <TableCell>
                          <Chip label={item.status} size="small" />
                        </TableCell>
                        <TableCell>
                          {item.created_at
                            ? new Date(item.created_at).toLocaleDateString()
                            : 'N/A'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : clientSearch ? (
              <Alert severity="info">No results found</Alert>
            ) : (
              <Alert severity="info">Enter a client name to search</Alert>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={1}>
            <Box mb={2}>
              <TextField
                fullWidth
                label="Script Keyword"
                value={scriptSearch}
                onChange={(e) => setScriptSearch(e.target.value)}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />,
                }}
              />
            </Box>
            {scriptLoading ? (
              <CircularProgress />
            ) : scriptResults?.results?.length > 0 ? (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>PO Number</TableCell>
                      <TableCell>Client</TableCell>
                      <TableCell>Copy Title</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {scriptResults.results.map((item: any, idx: number) => (
                      <TableRow key={idx}>
                        <TableCell>{item.production_order.po_number}</TableCell>
                        <TableCell>{item.production_order.client_name}</TableCell>
                        <TableCell>{item.copy.title}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : scriptSearch ? (
              <Alert severity="info">No results found</Alert>
            ) : (
              <Alert severity="info">Enter a keyword to search scripts</Alert>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={2}>
            <Box mb={2}>
              <TextField
                fullWidth
                label="Spot Length (seconds)"
                type="number"
                value={spotLengthSearch}
                onChange={(e) => setSpotLengthSearch(e.target.value)}
                InputProps={{
                  startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} />,
                }}
              />
            </Box>
            {spotLengthLoading ? (
              <CircularProgress />
            ) : spotLengthResults?.results?.length > 0 ? (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>PO Number</TableCell>
                      <TableCell>Client</TableCell>
                      <TableCell>Spot Lengths</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {spotLengthResults.results.map((item: any) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.po_number}</TableCell>
                        <TableCell>{item.client_name}</TableCell>
                        <TableCell>{item.spot_lengths?.join(', ') || 'N/A'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : spotLengthSearch ? (
              <Alert severity="info">No results found</Alert>
            ) : (
              <Alert severity="info">Enter a spot length to search</Alert>
            )}
          </TabPanel>

          <TabPanel value={tabValue} index={3}>
            {historyLoading ? (
              <CircularProgress />
            ) : historyResults?.results?.length > 0 ? (
              <TableContainer>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>PO Number</TableCell>
                      <TableCell>Client</TableCell>
                      <TableCell>Campaign</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Created</TableCell>
                      <TableCell>Completed</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {historyResults.results.map((item: any) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.po_number}</TableCell>
                        <TableCell>{item.client_name}</TableCell>
                        <TableCell>{item.campaign_title || 'N/A'}</TableCell>
                        <TableCell>
                          <Chip label={item.status} size="small" />
                        </TableCell>
                        <TableCell>
                          {item.created_at
                            ? new Date(item.created_at).toLocaleDateString()
                            : 'N/A'}
                        </TableCell>
                        <TableCell>
                          {item.completed_at
                            ? new Date(item.completed_at).toLocaleDateString()
                            : 'N/A'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">No production history found</Alert>
            )}
          </TabPanel>
        </CardContent>
      </Card>
    </Box>
  )
}

export default ProductionArchive

