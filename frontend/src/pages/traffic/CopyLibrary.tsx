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
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  Chip,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Tooltip,
} from '@mui/material'
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Visibility as VisibilityIcon,
  Assignment as AssignmentIcon,
} from '@mui/icons-material'
import {
  getCopy,
  deleteCopy,
  getOrders,
} from '../../utils/api'
import api from '../../utils/api'
import CopyUpload from '../../components/copy/CopyUpload'
import CopyDetailDialog from '../../components/copy/CopyDetailDialog'
import CopyAssignment from '../../components/copy/CopyAssignment'
import ExpiringCopyAlert from '../../components/copy/ExpiringCopyAlert'

interface Copy {
  id: number
  order_id?: number
  advertiser_id?: number
  title: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  version: number
  expires_at?: string
  active: boolean
  created_at: string
  updated_at: string
}

const CopyLibrary: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [orderFilter, setOrderFilter] = useState<number | ''>('')
  const [advertiserFilter, setAdvertiserFilter] = useState<number | ''>('')
  const [activeFilter, setActiveFilter] = useState<string>('all')
  const [uploadOpen, setUploadOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [assignmentOpen, setAssignmentOpen] = useState(false)
  const [selectedCopy, setSelectedCopy] = useState<Copy | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: copyItems, isLoading, error } = useQuery({
    queryKey: ['copy', searchTerm, orderFilter, advertiserFilter, activeFilter],
    queryFn: async () => {
      const params: any = {
        limit: 100,
        skip: 0,
      }
      if (searchTerm) params.search = searchTerm
      if (orderFilter) params.order_id = orderFilter
      if (advertiserFilter) params.advertiser_id = advertiserFilter
      
      const data = await getCopy(params)
      // Filter by active status on client side if needed
      if (activeFilter === 'active') {
        return data.filter((item: Copy) => item.active)
      } else if (activeFilter === 'inactive') {
        return data.filter((item: Copy) => !item.active)
      }
      return data
    },
    retry: 1,
  })

  const { data: advertisers } = useQuery({
    queryKey: ['advertisers'],
    queryFn: async () => {
      const response = await api.get('/advertisers', { params: { limit: 100 } })
      return response.data.advertisers || response.data || []
    },
  })

  const { data: orders } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      const response = await getOrders({ limit: 100 })
      return response.orders || response || []
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await deleteCopy(id)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete copy'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete copy error:', error)
    },
  })

  const handleView = (copy: Copy) => {
    setSelectedCopy(copy)
    setDetailOpen(true)
  }

  const handleEdit = (copy: Copy) => {
    setSelectedCopy(copy)
    setDetailOpen(true)
  }

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this copy?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleAssign = (copy: Copy) => {
    setSelectedCopy(copy)
    setAssignmentOpen(true)
  }

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return 'N/A'
    return new Date(dateStr).toLocaleDateString()
  }

  const isExpiring = (expiresAt?: string) => {
    if (!expiresAt) return false
    const expiry = new Date(expiresAt)
    const now = new Date()
    const daysUntilExpiry = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    return daysUntilExpiry <= 30 && daysUntilExpiry >= 0
  }

  const isExpired = (expiresAt?: string) => {
    if (!expiresAt) return false
    return new Date(expiresAt) < new Date()
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
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['copy'] })}>
            Retry
          </Button>
        }>
          Failed to load copy library: {error instanceof Error ? error.message : 'Unknown error'}
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
          <Typography variant="h4">Copy Library</Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setUploadOpen(true)}
          >
            Upload Copy
          </Button>
        </Box>

        {/* Expiring Copy Alert */}
        <Box sx={{ mb: 3 }}>
          <ExpiringCopyAlert />
        </Box>

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
          <TextField
            placeholder="Search by title..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
            }}
            sx={{ minWidth: 250 }}
          />
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Order</InputLabel>
            <Select
              value={orderFilter}
              onChange={(e) => setOrderFilter(e.target.value as number | '')}
              label="Order"
            >
              <MenuItem value="">All Orders</MenuItem>
              {orders?.map((order: any) => (
                <MenuItem key={order.id} value={order.id}>
                  {order.order_number}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Advertiser</InputLabel>
            <Select
              value={advertiserFilter}
              onChange={(e) => setAdvertiserFilter(e.target.value as number | '')}
              label="Advertiser"
            >
              <MenuItem value="">All Advertisers</MenuItem>
              {advertisers?.map((advertiser: any) => (
                <MenuItem key={advertiser.id} value={advertiser.id}>
                  {advertiser.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 150 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={activeFilter}
              onChange={(e) => setActiveFilter(e.target.value)}
              label="Status"
            >
              <MenuItem value="all">All</MenuItem>
              <MenuItem value="active">Active</MenuItem>
              <MenuItem value="inactive">Inactive</MenuItem>
            </Select>
          </FormControl>
        </Box>

        {/* Copy Table */}
        {copyItems && copyItems.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Title</TableCell>
                  <TableCell>Version</TableCell>
                  <TableCell>Advertiser</TableCell>
                  <TableCell>Order</TableCell>
                  <TableCell>Expiration</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {copyItems.map((copy: Copy) => (
                  <TableRow key={copy.id}>
                    <TableCell>
                      <Typography variant="body2" fontWeight="medium">
                        {copy.title}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label={`v${copy.version}`} size="small" />
                    </TableCell>
                    <TableCell>
                      {copy.advertiser_id ? (
                        advertisers?.find((a: any) => a.id === copy.advertiser_id)?.name || 'N/A'
                      ) : (
                        'N/A'
                      )}
                    </TableCell>
                    <TableCell>
                      {copy.order_id ? (
                        orders?.find((o: any) => o.id === copy.order_id)?.order_number || 'N/A'
                      ) : (
                        'N/A'
                      )}
                    </TableCell>
                    <TableCell>
                      {copy.expires_at ? (
                        <Box>
                          <Typography variant="body2">
                            {formatDate(copy.expires_at)}
                          </Typography>
                          {isExpired(copy.expires_at) && (
                            <Chip label="Expired" size="small" color="error" sx={{ mt: 0.5 }} />
                          )}
                          {isExpiring(copy.expires_at) && !isExpired(copy.expires_at) && (
                            <Chip label="Expiring Soon" size="small" color="warning" sx={{ mt: 0.5 }} />
                          )}
                        </Box>
                      ) : (
                        'No expiration'
                      )}
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={copy.active ? 'Active' : 'Inactive'}
                        size="small"
                        color={copy.active ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Tooltip title="View Details">
                        <IconButton size="small" onClick={() => handleView(copy)}>
                          <VisibilityIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Edit">
                        <IconButton size="small" onClick={() => handleEdit(copy)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Assign to Spot">
                        <IconButton size="small" onClick={() => handleAssign(copy)}>
                          <AssignmentIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      <Tooltip title="Delete">
                        <IconButton
                          size="small"
                          onClick={() => handleDelete(copy.id)}
                          color="error"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No copy items found. Upload your first copy to get started.</Alert>
        )}
      </Paper>

      {/* Upload Dialog */}
      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Upload Copy</DialogTitle>
        <DialogContent>
          <CopyUpload
            onSuccess={() => {
              setUploadOpen(false)
              queryClient.invalidateQueries({ queryKey: ['copy'] })
            }}
            onCancel={() => setUploadOpen(false)}
          />
        </DialogContent>
      </Dialog>

      {/* Detail/Edit Dialog */}
      {selectedCopy && (
        <CopyDetailDialog
          open={detailOpen}
          copyId={selectedCopy.id}
          onClose={() => {
            setDetailOpen(false)
            setSelectedCopy(null)
          }}
          onUpdate={() => {
            queryClient.invalidateQueries({ queryKey: ['copy'] })
          }}
        />
      )}

      {/* Assignment Dialog */}
      {selectedCopy && (
        <Dialog open={assignmentOpen} onClose={() => setAssignmentOpen(false)} maxWidth="md" fullWidth>
          <DialogTitle>Assign Copy to Spot</DialogTitle>
          <DialogContent>
            <CopyAssignment
              copyId={selectedCopy.id}
              onSuccess={() => {
                setAssignmentOpen(false)
                setSelectedCopy(null)
              }}
              onCancel={() => {
                setAssignmentOpen(false)
                setSelectedCopy(null)
              }}
            />
          </DialogContent>
        </Dialog>
      )}
    </Box>
  )
}

export default CopyLibrary

