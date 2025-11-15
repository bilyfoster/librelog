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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Tooltip,
  Alert,
  CircularProgress,
  Chip,
} from '@mui/material'
import {
  Add as AddIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material'
import {
  getMakegoods,
  createMakegood,
  approveMakegood,
  getCampaigns,
} from '../../utils/api'
import MakegoodFormDialog from '../../components/billing/MakegoodFormDialog'

interface Makegood {
  id: number
  original_spot_id: number
  makegood_spot_id: number
  campaign_id?: number
  reason?: string
  approved_by?: number
  approved_at?: string
  created_at: string
}

const Makegoods: React.FC = () => {
  const [campaignFilter, setCampaignFilter] = useState<number | ''>('')
  const [formOpen, setFormOpen] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const queryClient = useQueryClient()

  const { data: makegoods, isLoading, error } = useQuery({
    queryKey: ['makegoods', campaignFilter],
    queryFn: async () => {
      const params: any = { limit: 100 }
      if (campaignFilter) params.campaign_id = campaignFilter
      const data = await getMakegoods(params)
      return data
    },
    retry: 1,
  })

  const { data: campaigns } = useQuery({
    queryKey: ['campaigns', 'active'],
    queryFn: async () => {
      const data = await getCampaigns({ active_only: true, limit: 1000 })
      return data || []
    },
    retry: 1,
  })

  const approveMutation = useMutation({
    mutationFn: async (makegoodId: number) => {
      await approveMakegood(makegoodId)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['makegoods'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to approve makegood'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Approve makegood error:', error)
    },
  })

  const handleApprove = async (makegoodId: number) => {
    if (window.confirm('Approve this makegood?')) {
      approveMutation.mutate(makegoodId)
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
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['makegoods'] })}>
            Retry
          </Button>
        }>
          Failed to load makegoods: {error instanceof Error ? error.message : 'Unknown error'}
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
          <Typography variant="h4">Makegoods</Typography>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setFormOpen(true)}
          >
            Create Makegood
          </Button>
        </Box>

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>Campaign</InputLabel>
            <Select
              value={campaignFilter}
              onChange={(e) => setCampaignFilter(e.target.value as number | '')}
              label="Campaign"
            >
              <MenuItem value="">All Campaigns</MenuItem>
              {campaigns && campaigns.map((campaign: any) => (
                <MenuItem key={campaign.id} value={campaign.id}>
                  {campaign.name || `Campaign #${campaign.id}`}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Box>

        {/* Makegoods Table */}
        {makegoods && makegoods.length > 0 ? (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Original Spot</TableCell>
                  <TableCell>Makegood Spot</TableCell>
                  <TableCell>Campaign</TableCell>
                  <TableCell>Reason</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {makegoods.map((makegood: Makegood) => (
                  <TableRow key={makegood.id}>
                    <TableCell>
                      <Typography variant="body2">
                        Spot #{makegood.original_spot_id}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2">
                        Spot #{makegood.makegood_spot_id}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {makegood.campaign_id ? `Campaign #${makegood.campaign_id}` : 'N/A'}
                    </TableCell>
                    <TableCell>{makegood.reason || '-'}</TableCell>
                    <TableCell>
                      {makegood.approved_by ? (
                        <Chip
                          label="Approved"
                          size="small"
                          color="success"
                        />
                      ) : (
                        <Chip
                          label="Pending"
                          size="small"
                          color="warning"
                        />
                      )}
                    </TableCell>
                    <TableCell>
                      {new Date(makegood.created_at).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      {!makegood.approved_by && (
                        <Tooltip title="Approve Makegood">
                          <IconButton
                            size="small"
                            onClick={() => handleApprove(makegood.id)}
                            disabled={approveMutation.isPending}
                            color="success"
                          >
                            <CheckCircleIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Alert severity="info">No makegoods found. Create your first makegood to get started.</Alert>
        )}
      </Paper>

      {/* Makegood Form Dialog */}
      <MakegoodFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSuccess={() => {
          setFormOpen(false)
          queryClient.invalidateQueries({ queryKey: ['makegoods'] })
        }}
      />
    </Box>
  )
}

export default Makegoods

