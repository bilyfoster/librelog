import React, { useState, useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  MenuItem,
  Grid,
  Typography,
  Tabs,
  Tab,
  FormControlLabel,
  Switch,
  Chip,
  Autocomplete,
} from '@mui/material'
import api from '../../utils/api'
import { getAgenciesProxy, getSalesRepsProxy, getSalesTeamsProxy, getSalesOfficesProxy, getSalesRegionsProxy, getStationsProxy, getClustersProxy } from '../../utils/api'
import InfoButton from '../Help/InfoButton'

interface Order {
  id?: string
  order_number?: string
  order_name?: string
  campaign_id?: string
  advertiser_id?: string
  agency_id?: string
  sales_rep_id?: string
  sales_team?: string
  sales_office?: string
  sales_region?: string
  sales_team_id?: string
  sales_office_id?: string
  sales_region_id?: string
  stations?: string[]
  cluster?: string
  order_type?: string
  start_date?: string
  end_date?: string
  spot_lengths?: number[]
  total_spots?: number
  rate_type?: string
  rates?: any
  gross_amount?: number
  net_amount?: number
  total_value?: number
  agency_commission_percent?: number
  agency_commission_amount?: number
  agency_discount?: number
  cash_discount?: number
  trade_barter?: boolean
  trade_value?: number
  taxable?: boolean
  billing_cycle?: string
  invoice_type?: string
  coop_sponsor?: string
  coop_percent?: number
  client_po_number?: string
  billing_address?: string
  billing_contact?: string
  billing_contact_email?: string
  billing_contact_phone?: string
  political_class?: string
  political_window_flag?: boolean
  contract_reference?: string
  insertion_order_number?: string
  regulatory_notes?: string
  fcc_id?: string
  required_disclaimers?: string
  status?: string
  approval_status?: string
  traffic_ready?: boolean
  billing_ready?: boolean
  locked?: boolean
  revision_number?: number
}

interface OrderFormProps {
  open: boolean
  onClose: () => void
  order?: Order | null
  advertisers?: any[]
}

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
      id={`order-tabpanel-${index}`}
      aria-labelledby={`order-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  )
}

const OrderForm: React.FC<OrderFormProps> = ({ open, onClose, order, advertisers = [] }) => {
  const [activeTab, setActiveTab] = useState(0)
  const [formData, setFormData] = useState<Partial<Order>>({
    order_number: '',
    advertiser_id: undefined,
    agency_id: undefined,
    sales_rep_id: undefined,
    start_date: '',
    end_date: '',
    spot_lengths: [30, 60],
    total_spots: 0,
    rate_type: 'ROS',
    rates: {},
    gross_amount: 0,
    net_amount: 0,
    total_value: 0,
    trade_barter: false,
    taxable: false,
    political_window_flag: false,
    traffic_ready: false,
    billing_ready: false,
    locked: false,
    revision_number: 1,
    status: 'DRAFT',
    approval_status: 'NOT_REQUIRED',
  })
  const queryClient = useQueryClient()

  const { data: agencies } = useQuery({
    queryKey: ['agencies'],
    queryFn: async () => {
      const data = await getAgenciesProxy({ limit: 1000 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesReps } = useQuery({
    queryKey: ['sales-reps'],
    queryFn: async () => {
      const data = await getSalesRepsProxy({ limit: 1000 })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesTeams } = useQuery({
    queryKey: ['sales-teams'],
    queryFn: async () => {
      const data = await getSalesTeamsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesRegions } = useQuery({
    queryKey: ['sales-regions'],
    queryFn: async () => {
      const data = await getSalesRegionsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: salesOffices } = useQuery({
    queryKey: ['sales-offices', formData.sales_region_id],
    queryFn: async () => {
      const data = await getSalesOfficesProxy({ 
        limit: 1000, 
        active_only: true,
        region_id: formData.sales_region_id || undefined
      })
      return Array.isArray(data) ? data : []
    },
    enabled: true,
  })

  const { data: stations } = useQuery({
    queryKey: ['stations'],
    queryFn: async () => {
      const data = await getStationsProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  const { data: clusters } = useQuery({
    queryKey: ['clusters'],
    queryFn: async () => {
      const data = await getClustersProxy({ limit: 1000, active_only: true })
      return Array.isArray(data) ? data : []
    },
  })

  useEffect(() => {
    if (order) {
      setFormData({
        ...order,
        start_date: order.start_date ? order.start_date.split('T')[0] : '',
        end_date: order.end_date ? order.end_date.split('T')[0] : '',
        stations: Array.isArray(order.stations) ? order.stations : [],
        sales_team_id: (order as any).sales_team_id,
        sales_office_id: (order as any).sales_office_id,
        sales_region_id: (order as any).sales_region_id,
      })
    } else {
      setFormData({
        order_number: '',
        advertiser_id: undefined,
        agency_id: undefined,
        sales_rep_id: undefined,
        start_date: '',
        end_date: '',
        spot_lengths: [30, 60],
        total_spots: 0,
        rate_type: 'ROS',
        rates: {},
        gross_amount: 0,
        net_amount: 0,
        total_value: 0,
        trade_barter: false,
        taxable: false,
        political_window_flag: false,
        traffic_ready: false,
        billing_ready: false,
        locked: false,
        revision_number: 1,
        status: 'DRAFT',
        approval_status: 'NOT_REQUIRED',
        stations: [],
      })
    }
  }, [order, open])

  const createMutation = useMutation({
    mutationFn: async (data: Partial<Order>) => {
      const response = await api.post('/orders', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      onClose()
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id?: string; data: Partial<Order> }) => {
      const response = await api.put(`/orders/${id}`, data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      onClose()
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const submitData = { ...formData }
    if (!submitData.order_number || submitData.order_number.trim() === '') {
      delete submitData.order_number
    }
    
    // Convert empty strings to undefined for optional fields
    Object.keys(submitData).forEach(key => {
      if (submitData[key as keyof Order] === '') {
        submitData[key as keyof Order] = undefined
      }
    })
    
    if (order?.id) {
      updateMutation.mutate({ id: order.id, data: submitData })
    } else {
      createMutation.mutate(submitData)
    }
  }

  const updateField = (field: keyof Order, value: any) => {
    setFormData({ ...formData, [field]: value })
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle>{order ? 'Edit Order' : 'Create Order'}</DialogTitle>
        <DialogContent>
          <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
            <Tabs value={activeTab} onChange={(e, newValue) => setActiveTab(newValue)}>
              <Tab label="Basic Info" />
              <Tab label="Sales & Stations" />
              <Tab label="Financial Terms" />
              <Tab label="Billing" />
              <Tab label="Legal/Compliance" />
              <Tab label="Workflow" />
            </Tabs>
          </Box>

          {/* Basic Info Tab */}
          <TabPanel value={activeTab} index={0}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Order Number"
                    fullWidth
                    value={formData.order_number || ''}
                    onChange={(e) => updateField('order_number', e.target.value || undefined)}
                    helperText={!order?.id ? "Leave blank to auto-generate" : undefined}
                    placeholder="Auto-generated if left blank"
                  />
                  <InfoButton fieldPath="order.order_number" label="Order Number" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Order Name"
                  fullWidth
                  value={formData.order_name || ''}
                  onChange={(e) => updateField('order_name', e.target.value || undefined)}
                  placeholder="Order title/description"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Advertiser"
                  select
                  required
                  fullWidth
                  value={
                    formData.advertiser_id && 
                    advertisers.length > 0 && 
                    advertisers.some(adv => adv.id === formData.advertiser_id)
                      ? formData.advertiser_id
                      : ''
                  }
                  onChange={(e) => updateField('advertiser_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  disabled={advertisers.length === 0}
                >
                  {advertisers.map((adv) => (
                    <MenuItem key={adv.id} value={adv.id}>
                      {adv.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Autocomplete
                  options={agencies || []}
                  getOptionLabel={(option) => option.name || ''}
                  value={agencies?.find(a => a.id === formData.agency_id) || null}
                  onChange={(e, newValue) => updateField('agency_id', newValue?.id)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Agency"
                      placeholder="Search agencies..."
                    />
                  )}
                  filterOptions={(options, params) => {
                    const filtered = options.filter(option =>
                      option.name.toLowerCase().includes(params.inputValue.toLowerCase())
                    )
                    return filtered
                  }}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Sales Rep"
                  select
                  fullWidth
                  value={
                    formData.sales_rep_id && 
                    salesReps && 
                    salesReps.length > 0 && 
                    salesReps.some(rep => rep.id === formData.sales_rep_id)
                      ? formData.sales_rep_id
                      : ''
                  }
                  onChange={(e) => updateField('sales_rep_id', e.target.value ? parseInt(e.target.value) : undefined)}
                  disabled={!salesReps || salesReps.length === 0}
                >
                  <MenuItem value="">None</MenuItem>
                  {salesReps?.map((rep) => (
                    <MenuItem key={rep.id} value={rep.id}>
                      {rep.username || `Rep ${rep.id}`}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Order Type"
                  select
                  fullWidth
                  value={formData.order_type || ''}
                  onChange={(e) => updateField('order_type', e.target.value || undefined)}
                >
                  <MenuItem value="">None</MenuItem>
                  <MenuItem value="LOCAL">Local</MenuItem>
                  <MenuItem value="NATIONAL">National</MenuItem>
                  <MenuItem value="NETWORK">Network</MenuItem>
                  <MenuItem value="DIGITAL">Digital</MenuItem>
                  <MenuItem value="NTR">NTR (Non-traditional revenue)</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Start Date"
                  type="date"
                  required
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={formData.start_date || ''}
                  onChange={(e) => updateField('start_date', e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="End Date"
                  type="date"
                  required
                  fullWidth
                  InputLabelProps={{ shrink: true }}
                  value={formData.end_date || ''}
                  onChange={(e) => updateField('end_date', e.target.value)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Total Spots"
                    type="number"
                    fullWidth
                    value={formData.total_spots || 0}
                    onChange={(e) => updateField('total_spots', parseInt(e.target.value) || 0)}
                  />
                  <InfoButton fieldPath="order.total_spots" label="Total Spots" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Rate Type"
                    select
                    required
                    fullWidth
                    value={formData.rate_type || 'ROS'}
                    onChange={(e) => updateField('rate_type', e.target.value)}
                  >
                    <MenuItem value="ROS">Run of Schedule</MenuItem>
                    <MenuItem value="DAYPART">Daypart</MenuItem>
                    <MenuItem value="PROGRAM">Program</MenuItem>
                    <MenuItem value="FIXED_TIME">Fixed Time</MenuItem>
                  </TextField>
                  <InfoButton fieldPath="order.rate_type" label="Rate Type" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                  <TextField
                    label="Status"
                    select
                    fullWidth
                    value={formData.status || 'DRAFT'}
                    onChange={(e) => updateField('status', e.target.value)}
                  >
                    <MenuItem value="DRAFT">Draft</MenuItem>
                    <MenuItem value="PENDING">Pending</MenuItem>
                    <MenuItem value="APPROVED">Approved</MenuItem>
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="COMPLETED">Completed</MenuItem>
                    <MenuItem value="CANCELLED">Cancelled</MenuItem>
                  </TextField>
                  <InfoButton fieldPath="order.status" label="Order Status" />
                </Box>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Approval Status"
                  select
                  fullWidth
                  value={formData.approval_status || 'NOT_REQUIRED'}
                  onChange={(e) => updateField('approval_status', e.target.value)}
                >
                  <MenuItem value="NOT_REQUIRED">Not Required</MenuItem>
                  <MenuItem value="PENDING">Pending</MenuItem>
                  <MenuItem value="APPROVED">Approved</MenuItem>
                  <MenuItem value="REJECTED">Rejected</MenuItem>
                </TextField>
              </Grid>
            </Grid>
          </TabPanel>

          {/* Sales & Stations Tab */}
          <TabPanel value={activeTab} index={1}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={salesTeams || []}
                  getOptionLabel={(option) => option.name || ''}
                  value={salesTeams?.find(t => t.id === formData.sales_team_id) || null}
                  onChange={(e, newValue) => {
                    updateField('sales_team_id', newValue?.id)
                    updateField('sales_team', newValue?.name)
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Sales Team"
                      placeholder="Select sales team"
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={salesRegions || []}
                  getOptionLabel={(option) => option.name || ''}
                  value={salesRegions?.find(r => r.id === formData.sales_region_id) || null}
                  onChange={(e, newValue) => {
                    updateField('sales_region_id', newValue?.id)
                    updateField('sales_region', newValue?.name)
                    // Clear office when region changes
                    updateField('sales_office_id', undefined)
                    updateField('sales_office', undefined)
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Sales Region"
                      placeholder="Select sales region"
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <Autocomplete
                  options={salesOffices || []}
                  getOptionLabel={(option) => option.name || ''}
                  value={salesOffices?.find(o => o.id === formData.sales_office_id) || null}
                  onChange={(e, newValue) => {
                    updateField('sales_office_id', newValue?.id)
                    updateField('sales_office', newValue?.name)
                  }}
                  disabled={!formData.sales_region_id}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Sales Office"
                      placeholder={formData.sales_region_id ? "Select sales office" : "Select region first"}
                      helperText={!formData.sales_region_id ? "Select a region first" : undefined}
                    />
                  )}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Autocomplete
                  multiple
                  options={stations || []}
                  getOptionLabel={(option) => `${option.call_letters}${option.frequency ? ` (${option.frequency})` : ''}`}
                  value={(stations || []).filter(s => formData.stations?.includes(s.call_letters)) || []}
                  onChange={(e, newValue) => {
                    const stationNames = newValue.map(s => s.call_letters)
                    updateField('stations', stationNames)
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Stations"
                      placeholder="Select stations"
                      helperText="Select multiple stations"
                    />
                  )}
                  renderTags={(value, getTagProps) =>
                    value.map((option, index) => (
                      <Chip 
                        label={`${option.call_letters}${option.frequency ? ` (${option.frequency})` : ''}`} 
                        {...getTagProps({ index })} 
                        key={option.id} 
                      />
                    ))
                  }
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Autocomplete
                  options={clusters || []}
                  getOptionLabel={(option) => option.name || ''}
                  value={clusters?.find(c => c.name === formData.cluster) || null}
                  onChange={(e, newValue) => {
                    updateField('cluster', newValue?.name)
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Cluster"
                      placeholder="Select cluster"
                    />
                  )}
                />
              </Grid>
            </Grid>
          </TabPanel>

          {/* Financial Terms Tab */}
          <TabPanel value={activeTab} index={2}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom>Contract Amounts</Typography>
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Gross Amount"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.gross_amount || 0}
                  onChange={(e) => updateField('gross_amount', parseFloat(e.target.value) || 0)}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Net Amount"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.net_amount || 0}
                  onChange={(e) => updateField('net_amount', parseFloat(e.target.value) || 0)}
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Total Contract Value"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.total_value || 0}
                  onChange={(e) => updateField('total_value', parseFloat(e.target.value) || 0)}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Agency Commission</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Agency Commission %"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01', min: 0, max: 100 }}
                  value={formData.agency_commission_percent || ''}
                  onChange={(e) => updateField('agency_commission_percent', e.target.value ? parseFloat(e.target.value) : undefined)}
                  helperText="Percentage (e.g., 15.00)"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Agency Commission Amount"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.agency_commission_amount || ''}
                  onChange={(e) => updateField('agency_commission_amount', e.target.value ? parseFloat(e.target.value) : undefined)}
                  helperText="Calculated amount"
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Discounts</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Agency Discount"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.agency_discount || ''}
                  onChange={(e) => updateField('agency_discount', e.target.value ? parseFloat(e.target.value) : undefined)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Cash Discount"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.cash_discount || ''}
                  onChange={(e) => updateField('cash_discount', e.target.value ? parseFloat(e.target.value) : undefined)}
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Trade & Tax</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.trade_barter || false}
                      onChange={(e) => updateField('trade_barter', e.target.checked)}
                    />
                  }
                  label="Trade/Barter"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Trade Value"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01' }}
                  value={formData.trade_value || ''}
                  onChange={(e) => updateField('trade_value', e.target.value ? parseFloat(e.target.value) : undefined)}
                  disabled={!formData.trade_barter}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.taxable || false}
                      onChange={(e) => updateField('taxable', e.target.checked)}
                    />
                  }
                  label="Taxable"
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Co-op</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Co-op Sponsor"
                  fullWidth
                  value={formData.coop_sponsor || ''}
                  onChange={(e) => updateField('coop_sponsor', e.target.value || undefined)}
                  placeholder="Co-op sponsor name"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Co-op %"
                  type="number"
                  fullWidth
                  inputProps={{ step: '0.01', min: 0, max: 100 }}
                  value={formData.coop_percent || ''}
                  onChange={(e) => updateField('coop_percent', e.target.value ? parseFloat(e.target.value) : undefined)}
                  helperText="Co-op contribution percentage"
                />
              </Grid>
            </Grid>
          </TabPanel>

          {/* Billing Tab */}
          <TabPanel value={activeTab} index={3}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom>Billing Information</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Billing Cycle"
                  select
                  fullWidth
                  value={formData.billing_cycle || ''}
                  onChange={(e) => updateField('billing_cycle', e.target.value || undefined)}
                >
                  <MenuItem value="">None</MenuItem>
                  <MenuItem value="WEEKLY">Weekly</MenuItem>
                  <MenuItem value="BIWEEKLY">Biweekly</MenuItem>
                  <MenuItem value="MONTHLY">Monthly</MenuItem>
                  <MenuItem value="QUARTERLY">Quarterly</MenuItem>
                  <MenuItem value="ONE_SHOT">One Shot</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Invoice Type"
                  select
                  fullWidth
                  value={formData.invoice_type || ''}
                  onChange={(e) => updateField('invoice_type', e.target.value || undefined)}
                >
                  <MenuItem value="">None</MenuItem>
                  <MenuItem value="STANDARD">Standard</MenuItem>
                  <MenuItem value="COOP">Co-op</MenuItem>
                  <MenuItem value="POLITICAL">Political</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Client PO Number"
                  fullWidth
                  value={formData.client_po_number || ''}
                  onChange={(e) => updateField('client_po_number', e.target.value || undefined)}
                  placeholder="Purchase order number"
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Billing Contact</Typography>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Billing Address"
                  fullWidth
                  multiline
                  rows={3}
                  value={formData.billing_address || ''}
                  onChange={(e) => updateField('billing_address', e.target.value || undefined)}
                  placeholder="Full billing address"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Billing Contact Name"
                  fullWidth
                  value={formData.billing_contact || ''}
                  onChange={(e) => updateField('billing_contact', e.target.value || undefined)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Billing Contact Email"
                  type="email"
                  fullWidth
                  value={formData.billing_contact_email || ''}
                  onChange={(e) => updateField('billing_contact_email', e.target.value || undefined)}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Billing Contact Phone"
                  fullWidth
                  value={formData.billing_contact_phone || ''}
                  onChange={(e) => updateField('billing_contact_phone', e.target.value || undefined)}
                  placeholder="(555) 123-4567"
                />
              </Grid>
            </Grid>
          </TabPanel>

          {/* Legal/Compliance Tab */}
          <TabPanel value={activeTab} index={4}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom>Political/Compliance</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Political Class"
                  select
                  fullWidth
                  value={formData.political_class || ''}
                  onChange={(e) => updateField('political_class', e.target.value || undefined)}
                >
                  <MenuItem value="">None</MenuItem>
                  <MenuItem value="FEDERAL">Federal</MenuItem>
                  <MenuItem value="STATE">State</MenuItem>
                  <MenuItem value="LOCAL">Local</MenuItem>
                  <MenuItem value="ISSUE">Issue</MenuItem>
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.political_window_flag || false}
                      onChange={(e) => updateField('political_window_flag', e.target.checked)}
                    />
                  }
                  label="Political Window Flag (Lowest Unit Rate)"
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Contract References</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Contract Reference"
                  fullWidth
                  value={formData.contract_reference || ''}
                  onChange={(e) => updateField('contract_reference', e.target.value || undefined)}
                  placeholder="Contract reference number"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Insertion Order Number"
                  fullWidth
                  value={formData.insertion_order_number || ''}
                  onChange={(e) => updateField('insertion_order_number', e.target.value || undefined)}
                  placeholder="IO number"
                />
              </Grid>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>Regulatory</Typography>
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="FCC ID"
                  fullWidth
                  value={formData.fcc_id || ''}
                  onChange={(e) => updateField('fcc_id', e.target.value || undefined)}
                  placeholder="FCC identifier"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Regulatory Notes"
                  fullWidth
                  multiline
                  rows={3}
                  value={formData.regulatory_notes || ''}
                  onChange={(e) => updateField('regulatory_notes', e.target.value || undefined)}
                  placeholder="Regulatory compliance notes"
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Required Disclaimers"
                  fullWidth
                  multiline
                  rows={3}
                  value={formData.required_disclaimers || ''}
                  onChange={(e) => updateField('required_disclaimers', e.target.value || undefined)}
                  placeholder="Required legal disclaimers"
                />
              </Grid>
            </Grid>
          </TabPanel>

          {/* Workflow Tab */}
          <TabPanel value={activeTab} index={5}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom>Workflow Status</Typography>
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.traffic_ready || false}
                      onChange={(e) => updateField('traffic_ready', e.target.checked)}
                    />
                  }
                  label="Traffic Ready"
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.billing_ready || false}
                      onChange={(e) => updateField('billing_ready', e.target.checked)}
                    />
                  }
                  label="Billing Ready"
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <FormControlLabel
                  control={
                    <Switch
                      checked={formData.locked || false}
                      onChange={(e) => updateField('locked', e.target.checked)}
                    />
                  }
                  label="Locked"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Revision Number"
                  type="number"
                  fullWidth
                  inputProps={{ min: 1 }}
                  value={formData.revision_number || 1}
                  onChange={(e) => updateField('revision_number', parseInt(e.target.value) || 1)}
                  helperText="Order revision number"
                />
              </Grid>
            </Grid>
          </TabPanel>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={createMutation.isPending || updateMutation.isPending}>
            {createMutation.isPending || updateMutation.isPending ? 'Saving...' : 'Save'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

export default OrderForm
