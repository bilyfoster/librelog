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
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
  Build as BuildIcon,
  Create as CreateIcon,
} from '@mui/icons-material'
import {
  getCopyProxy,
  deleteCopy,
  getOrdersProxy,
  getAdvertisersProxy,
  setNeedsProduction,
  approveCopy,
  rejectCopy,
  getCopyProductionStatus,
  createProductionOrder,
} from '../../utils/api'
import api from '../../utils/api'
import CopyUpload from '../../components/copy/CopyUpload'
import CopyDetailDialog from '../../components/copy/CopyDetailDialog'
import CopyAssignment from '../../components/copy/CopyAssignment'
import ExpiringCopyAlert from '../../components/copy/ExpiringCopyAlert'

interface Copy {
  id?: string
  order_id?: string
  advertiser_id?: string
  title: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  version: number
  expires_at?: string
  active: boolean
  created_at: string
  updated_at: string
  needs_production?: boolean
  copy_status?: string
  copy_approval_status?: string
  production_order_id?: string
}

const CopyLibrary: React.FC = () => {
  const [searchTerm, setSearchTerm] = useState('')
  const [orderFilter, setOrderFilter] = useState<number | ''>('')
  const [advertiserFilter, setAdvertiserFilter] = useState<number | ''>('')
  const [activeFilter, setActiveFilter] = useState<string>('all')
  const [uploadOpen, setUploadOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [assignmentOpen, setAssignmentOpen] = useState(false)
  const [specOrderOpen, setSpecOrderOpen] = useState(false)
  const [selectedCopy, setSelectedCopy] = useState<Copy | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [specOrderForm, setSpecOrderForm] = useState({
    client_name: '',
    spot_lengths: [] as number[],
    deadline: '',
    instructions: '',
  })
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
      
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getCopyProxy(params)
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
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getAdvertisersProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: orders } = useQuery({
    queryKey: ['orders'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getOrdersProxy({ limit: 100 })
      return Array.isArray(data) ? data : []
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id?: string) => {
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

  const needsProductionMutation = useMutation({
    mutationFn: async ({ id, needs_production }: { id?: string; needs_production: boolean }) => {
      return await setNeedsProduction(id, needs_production)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to update production flag')
    },
  })

  const approveCopyMutation = useMutation({
    mutationFn: async (id?: string) => {
      return await approveCopy(id, true)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      queryClient.invalidateQueries({ queryKey: ['production-orders'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to approve copy')
    },
  })

  const createSpecOrderMutation = useMutation({
    mutationFn: async (data: { copy_id?: string; client_name: string; spot_lengths?: number[]; deadline?: string; instructions?: string }) => {
      return await createProductionOrder({
        ...data,
        is_spec: true,
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      queryClient.invalidateQueries({ queryKey: ['production-orders'] })
      setSpecOrderOpen(false)
      setSpecOrderForm({ client_name: '', spot_lengths: [], deadline: '', instructions: '' })
      setSelectedCopy(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to create spec production order')
    },
  })

  const rejectCopyMutation = useMutation({
    mutationFn: async (id?: string) => {
      return await rejectCopy(id)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['copy'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      setErrorMessage(error?.response?.data?.detail || 'Failed to reject copy')
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

  const handleDelete = async (id?: string) => {
    if (window.confirm('Are you sure you want to delete this copy?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleAssign = (copy: Copy) => {
    setSelectedCopy(copy)
    setAssignmentOpen(true)
  }

  const handleCreateSpecOrder = (copy: Copy) => {
    setSelectedCopy(copy)
    setSpecOrderForm({
      client_name: '',
      spot_lengths: [],
      deadline: '',
      instructions: '',
    })
    setSpecOrderOpen(true)
  }

  const handleSpecOrderSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedCopy || !specOrderForm.client_name) {
      setErrorMessage('Client name is required for spec spots')
      return
    }
    createSpecOrderMutation.mutate({
      copy_id: selectedCopy.id,
      client_name: specOrderForm.client_name,
      spot_lengths: specOrderForm.spot_lengths.length > 0 ? specOrderForm.spot_lengths : undefined,
      deadline: specOrderForm.deadline || undefined,
      instructions: specOrderForm.instructions || undefined,
    })
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
                  <TableCell>Production</TableCell>
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
                      <Box display="flex" flexDirection="column" gap={0.5}>
                        {copy.copy_approval_status && (
                          <Chip
                            label={copy.copy_approval_status}
                            size="small"
                            color={
                              copy.copy_approval_status === 'approved'
                                ? 'success'
                                : copy.copy_approval_status === 'rejected'
                                ? 'error'
                                : 'default'
                            }
                          />
                        )}
                        {copy.needs_production && (
                          <Chip label="Needs Production" size="small" color="warning" />
                        )}
                        {copy.production_order_id && (
                          <Chip
                            label={`PO: ${copy.production_order_id}`}
                            size="small"
                            color="info"
                          />
                        )}
                        {copy.copy_status && copy.copy_status !== 'draft' && (
                          <Chip label={copy.copy_status} size="small" />
                        )}
                      </Box>
                    </TableCell>
                    <TableCell>
                      <Box display="flex" gap={0.5}>
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
                        {copy.copy_approval_status === 'pending' && (
                          <>
                            <Tooltip title="Approve Copy">
                              <IconButton
                                size="small"
                                onClick={() => approveCopyMutation.mutate(copy.id)}
                                color="success"
                              >
                                <CheckCircleIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Reject Copy">
                              <IconButton
                                size="small"
                                onClick={() => rejectCopyMutation.mutate(copy.id)}
                                color="error"
                              >
                                <CancelIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </>
                        )}
                        <Tooltip title={copy.needs_production ? 'Production Required' : 'Mark for Production'}>
                          <IconButton
                            size="small"
                            onClick={() =>
                              needsProductionMutation.mutate({
                                id: copy.id,
                                needs_production: !copy.needs_production,
                              })
                            }
                            color={copy.needs_production ? 'warning' : 'default'}
                          >
                            <BuildIcon fontSize="small" />
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
                      </Box>
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

