import axios from 'axios'

// Use relative path for API calls (works with Traefik proxy)
// This ensures we use the same protocol (HTTPS) as the current page
const API_BASE_URL = import.meta.env.VITE_API_URL || ''

// Determine baseURL based on environment
// In browser, always use relative path to match current page protocol
// This prevents mixed content errors
let baseURL = '/api'
if (typeof window !== 'undefined') {
  // Force relative path - browser will use same protocol as page
  baseURL = '/api'
} else if (API_BASE_URL) {
  // Server-side rendering or build-time: use provided URL
  baseURL = API_BASE_URL.startsWith('http') ? `${API_BASE_URL}/api` : `/api`
}

const api = axios.create({
  baseURL: baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
})

// Request interceptor to add auth token and ensure HTTPS
api.interceptors.request.use(
  (config) => {
    // Force relative URLs to use the current page's protocol
    if (typeof window !== 'undefined') {
      const currentProtocol = window.location.protocol
      
      // CRITICAL: Always force baseURL to be relative
      // This ensures axios uses the same protocol as the current page
      const originalBaseURL = config.baseURL
      config.baseURL = '/api'
      
      // If url is an absolute URL, convert to relative
      if (config.url && (config.url.startsWith('http://') || config.url.startsWith('https://'))) {
        try {
          const urlObj = new URL(config.url)
          config.url = urlObj.pathname + (urlObj.search || '')
        } catch (e) {
          // If URL parsing fails, strip protocol and domain
          config.url = config.url.replace(/^https?:\/\/[^/]+/, '')
        }
      }
      
      // Ensure url is relative (starts with /)
      if (config.url && !config.url.startsWith('/') && !config.url.startsWith('http')) {
        config.url = '/' + config.url
      }
      
      // Debug: Log if we detect HTTP URLs (remove in production)
      if (process.env.NODE_ENV === 'development') {
        const finalUrl = (config.baseURL || '') + (config.url || '')
        if (finalUrl.includes('http://') && currentProtocol === 'https:') {
          console.warn('[API] Detected HTTP URL in HTTPS context:', {
            originalBaseURL,
            baseURL: config.baseURL,
            url: config.url,
            finalUrl,
            currentProtocol
          })
        }
      }
      
      // Final safety: if config has an absolute URL somehow, reconstruct with relative paths
      const testUrl = (config.baseURL || '') + (config.url || '')
      if (testUrl.includes('http://') && currentProtocol === 'https:') {
        // Force reconstruction with relative paths
        config.baseURL = '/api'
        if (config.url) {
          config.url = config.url.replace(/^https?:\/\/[^/]+/, '')
          if (!config.url.startsWith('/')) {
            config.url = '/' + config.url
          }
        }
      }
    }
    
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    
    // Always log requests for debugging API issues
    console.log('[API] Request:', config.method?.toUpperCase(), config.baseURL + (config.url || ''), {
      hasToken: !!token,
      timeout: config.timeout
    })
    
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => {
    // Log successful responses in dev mode for debugging
    if (process.env.NODE_ENV === 'development') {
      console.log('[API] Success:', response.config.method?.toUpperCase(), response.config.url, response.status)
    }
    return response
  },
  (error) => {
    // Enhanced error logging with more diagnostic info
    if (error.code === 'ECONNABORTED') {
      const url = error.config?.baseURL + (error.config?.url || '')
      console.error('[API] Request timeout:', error.config?.method?.toUpperCase(), url, 'after', error.config?.timeout, 'ms')
      console.error('[API] Timeout diagnostic:', {
        fullUrl: url,
        baseURL: error.config?.baseURL,
        url: error.config?.url,
        timeout: error.config?.timeout,
        currentLocation: window.location.href,
        message: 'Backend may not be running or not reachable. Check backend container status and network connectivity.'
      })
    } else if (error.response) {
      console.error('[API] Error response:', error.config?.method?.toUpperCase(), error.config?.baseURL + (error.config?.url || ''), error.response.status, error.response.data)
    } else if (error.request) {
      const url = error.config?.baseURL + (error.config?.url || '')
      console.error('[API] No response received:', error.config?.method?.toUpperCase(), url, error.message)
      console.error('[API] Network error diagnostic:', {
        fullUrl: url,
        errorCode: error.code,
        errorMessage: error.message,
        requestMade: !!error.request,
        responseReceived: !!error.response,
        message: 'Request was sent but no response received. Backend may be down or unreachable.'
      })
    } else {
      console.error('[API] Request setup error:', error.message)
    }
    
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// Health check - test if API is reachable
export const checkApiHealth = async () => {
  try {
    // Health endpoint is at /api/health (full path)
    const response = await api.get('/health', {
      timeout: 5000,
      // Don't require auth for health check
    })
    return response.data
  } catch (error: any) {
    // Enhanced error logging for health check
    if (error.code === 'ECONNABORTED') {
      console.error('[API] Health check timeout - backend not responding')
    } else if (error.request && !error.response) {
      console.error('[API] Health check - no response from backend. Backend may be down.')
    } else {
      console.error('[API] Health check failed:', error.response?.status, error.message)
    }
    throw error
  }
}

// Sync API functions
export const syncTracks = async (limit = 1000, offset = 0) => {
  const response = await api.post('/sync/tracks', null, {
    params: { limit, offset },
    timeout: 30000, // 30 second timeout for sync (can take longer)
  })
  return response.data
}

export const getTracksCount = async (track_type?: string) => {
  const response = await api.get('/tracks/count', {
    params: track_type ? { track_type } : {},
    timeout: 5000, // 5 second timeout
  })
  return response.data
}

// Tracks API functions
export const getTrack = async (track_id: number) => {
  const response = await api.get(`/tracks/${track_id}`)
  return response.data
}

export const updateTrack = async (track_id: number, updates: {
  title?: string
  artist?: string
  genre?: string
  type?: string
  duration?: number
}) => {
  const response = await api.put(`/tracks/${track_id}`, updates)
  return response.data
}

export const syncPlaybackHistory = async (startDate: string, endDate: string) => {
  const response = await api.post('/sync/playback-history', {
    start_date: startDate,
    end_date: endDate
  })
  return response.data
}

export const getSyncStatus = async () => {
  const response = await api.get('/sync/status')
  return response.data
}

// Activity API functions
export const getRecentActivity = async (limit = 20) => {
  try {
    const response = await api.get('/activity/recent', {
      params: { limit },
      timeout: 3000, // 3 second timeout - fail fast
    })
    return response.data
  } catch (error: any) {
    // If endpoint doesn't exist or fails, return empty activities silently
    // Don't log to console as this is expected behavior when endpoint is unavailable
    return { activities: [] }
  }
}

// Dashboard stats - get actual counts from count endpoints
export const getDashboardStats = async () => {
  const [tracksRes, campaignsRes, clocksRes, logsRes] = await Promise.allSettled([
    api.get('/tracks/count', { timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/campaigns/count', { params: { active_only: true }, timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/clocks/count', { timeout: 5000 }).catch(() => ({ data: { count: 0 } })),
    api.get('/logs/count', { params: { published_only: false }, timeout: 5000 }).catch(() => ({ data: { count: 0 } }))
  ])
  
  // Extract counts from responses
  const tracksCount = tracksRes.status === 'fulfilled' ? (tracksRes.value.data?.count || 0) : 0
  const campaignsCount = campaignsRes.status === 'fulfilled' ? (campaignsRes.value.data?.count || 0) : 0
  const clocksCount = clocksRes.status === 'fulfilled' ? (clocksRes.value.data?.count || 0) : 0
  const logsCount = logsRes.status === 'fulfilled' ? (logsRes.value.data?.count || 0) : 0
  
  // Format numbers with commas
  const formatNumber = (num: number) => {
    return num.toLocaleString('en-US')
  }
  
  return {
    totalTracks: formatNumber(tracksCount),
    activeCampaigns: formatNumber(campaignsCount),
    clockTemplates: formatNumber(clocksCount),
    reportsGenerated: formatNumber(logsCount)
  }
}

// Spots API functions
export const getSpots = async (params?: {
  skip?: number
  limit?: number
  order_id?: number
  scheduled_date?: string
  status_filter?: string
}) => {
  const response = await api.get('/spots', { params })
  return response.data
}

export const createSpot = async (spot: {
  order_id: number
  campaign_id?: number
  scheduled_date: string
  scheduled_time: string
  spot_length: number
  break_position?: string
  daypart?: string
  status?: string
}) => {
  const response = await api.post('/spots', spot)
  return response.data
}

export const createSpotsBulk = async (order_id: number, spots: any[]) => {
  const response = await api.post('/spots/bulk', spots, {
    params: { order_id }
  })
  return response.data
}

export const updateSpot = async (spot_id: number, updates: {
  scheduled_date?: string
  scheduled_time?: string
  spot_length?: number
  break_position?: string
  daypart?: string
  status?: string
  conflict_resolved?: boolean
}) => {
  const response = await api.put(`/spots/${spot_id}`, updates)
  return response.data
}

export const deleteSpot = async (spot_id: number) => {
  await api.delete(`/spots/${spot_id}`)
}

export const resolveSpotConflict = async (spot_id: number) => {
  const response = await api.post(`/spots/${spot_id}/resolve-conflict`)
  return response.data
}

// Logs API functions
export const getLogs = async (params?: {
  skip?: number
  limit?: number
  published_only?: boolean
  date_filter?: string
}) => {
  const response = await api.get('/logs', { params })
  return response.data
}

export const getLog = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}`)
  return response.data
}

export const getLogTimeline = async (log_id: number, hour?: string) => {
  const response = await api.get(`/logs/${log_id}/timeline`, {
    params: { hour }
  })
  return response.data
}

export const lockLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/lock`)
  return response.data
}

export const unlockLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/unlock`)
  return response.data
}

export const getLogConflicts = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}/conflicts`)
  return response.data
}

export const getLogAvails = async (log_id: number) => {
  const response = await api.get(`/logs/${log_id}/avails`)
  return response.data
}

export const addSpotToLog = async (log_id: number, spot: any) => {
  const response = await api.post(`/logs/${log_id}/spots`, spot)
  return response.data
}

export const updateSpotInLog = async (log_id: number, spot_id: number, updates: any) => {
  const response = await api.put(`/logs/${log_id}/spots/${spot_id}`, updates)
  return response.data
}

export const removeSpotFromLog = async (log_id: number, spot_id: number) => {
  await api.delete(`/logs/${log_id}/spots/${spot_id}`)
}

export const generateLog = async (target_date: string, clock_template_id: number) => {
  const response = await api.post('/logs/generate', {
    target_date,
    clock_template_id,
  })
  return response.data
}

export const previewLog = async (target_date: string, clock_template_id: number, preview_hours?: string[]) => {
  const response = await api.post('/logs/preview', {
    target_date,
    clock_template_id,
    preview_hours,
  })
  return response.data
}

export const publishLog = async (log_id: number) => {
  const response = await api.post(`/logs/${log_id}/publish`)
  return response.data
}

// Clock Templates API functions
export const getClocks = async (params?: {
  skip?: number
  limit?: number
  search?: string
}) => {
  const response = await api.get('/clocks', { params })
  return response.data
}

export const getClock = async (clock_id: number) => {
  const response = await api.get(`/clocks/${clock_id}`)
  return response.data
}

// Dayparts API functions
export const getDayparts = async () => {
  const response = await api.get('/dayparts')
  return response.data
}

export const createDaypart = async (daypart: any) => {
  const response = await api.post('/dayparts', daypart)
  return response.data
}

export const updateDaypart = async (daypart_id: number, updates: any) => {
  const response = await api.put(`/dayparts/${daypart_id}`, updates)
  return response.data
}

export const deleteDaypart = async (daypart_id: number) => {
  await api.delete(`/dayparts/${daypart_id}`)
}

// Advertisers API functions
export const getAdvertisers = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/advertisers', { params })
  return response.data
}

export const getAdvertiser = async (advertiser_id: number) => {
  const response = await api.get(`/advertisers/${advertiser_id}`)
  return response.data
}

export const createAdvertiser = async (advertiser: any) => {
  const response = await api.post('/advertisers', advertiser)
  return response.data
}

export const updateAdvertiser = async (advertiser_id: number, updates: any) => {
  const response = await api.put(`/advertisers/${advertiser_id}`, updates)
  return response.data
}

export const deleteAdvertiser = async (advertiser_id: number) => {
  await api.delete(`/advertisers/${advertiser_id}`)
}

// Agencies API functions
export const getAgencies = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  search?: string
}) => {
  const response = await api.get('/agencies', { params })
  return response.data
}

export const getAgency = async (agency_id: number) => {
  const response = await api.get(`/agencies/${agency_id}`)
  return response.data
}

export const createAgency = async (agency: any) => {
  const response = await api.post('/agencies', agency)
  return response.data
}

export const updateAgency = async (agency_id: number, updates: any) => {
  const response = await api.put(`/agencies/${agency_id}`, updates)
  return response.data
}

export const deleteAgency = async (agency_id: number) => {
  await api.delete(`/agencies/${agency_id}`)
}

// Campaigns API functions
export const getCampaigns = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  date_filter?: string
}) => {
  const response = await api.get('/campaigns', { params })
  return response.data
}

export const getCampaign = async (campaign_id: number) => {
  const response = await api.get(`/campaigns/${campaign_id}`)
  return response.data
}

export const createCampaign = async (campaign: any) => {
  const response = await api.post('/campaigns', campaign)
  return response.data
}

export const updateCampaign = async (campaign_id: number, updates: any) => {
  const response = await api.put(`/campaigns/${campaign_id}`, updates)
  return response.data
}

export const deleteCampaign = async (campaign_id: number) => {
  await api.delete(`/campaigns/${campaign_id}`)
}

// Orders API functions (for spot scheduling)
export const getOrders = async (params?: {
  skip?: number
  limit?: number
  status?: string
  advertiser_id?: number
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/orders', { params })
  return response.data
}

export const getOrder = async (order_id: number) => {
  const response = await api.get(`/orders/${order_id}`)
  return response.data
}

export const createOrder = async (order: any) => {
  const response = await api.post('/orders', order)
  return response.data
}

export const updateOrder = async (order_id: number, updates: any) => {
  const response = await api.put(`/orders/${order_id}`, updates)
  return response.data
}

export const deleteOrder = async (order_id: number) => {
  await api.delete(`/orders/${order_id}`)
}

// Copy API functions
export const getCopy = async (params?: {
  skip?: number
  limit?: number
  order_id?: number
  advertiser_id?: number
  search?: string
}) => {
  const response = await api.get('/copy', { params })
  return response.data
}

export const getCopyById = async (copy_id: number) => {
  const response = await api.get(`/copy/${copy_id}`)
  return response.data
}

export const createCopy = async (copy: {
  order_id?: number
  advertiser_id?: number
  title: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  expires_at?: string
}) => {
  const response = await api.post('/copy', copy)
  return response.data
}

export const uploadCopy = async (
  file: File,
  title: string,
  order_id?: number,
  advertiser_id?: number,
  onUploadProgress?: (progress: number) => void
) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('title', title)
  if (order_id) formData.append('order_id', order_id.toString())
  if (advertiser_id) formData.append('advertiser_id', advertiser_id.toString())

  const response = await api.post('/copy/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    onUploadProgress: (progressEvent) => {
      if (onUploadProgress && progressEvent.total) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onUploadProgress(progress)
      }
    },
  })
  return response.data
}

export const updateCopy = async (copy_id: number, updates: {
  title?: string
  script_text?: string
  audio_file_path?: string
  audio_file_url?: string
  expires_at?: string
  active?: boolean
}) => {
  const response = await api.put(`/copy/${copy_id}`, updates)
  return response.data
}

export const deleteCopy = async (copy_id: number) => {
  await api.delete(`/copy/${copy_id}`)
}

export const getExpiringCopy = async (days_ahead: number = 30) => {
  const response = await api.get('/copy/expiring', {
    params: { days_ahead: days_ahead }
  })
  return response.data
}

export const assignCopyToSpot = async (copy_id: number, spot_id: number) => {
  const response = await api.post(`/copy/${copy_id}/assign`, null, {
    params: { spot_id }
  })
  return response.data
}

// Copy Assignments API functions
export const getCopyAssignments = async (params?: {
  skip?: number
  limit?: number
  spot_id?: number
  copy_id?: number
}) => {
  const response = await api.get('/copy-assignments', { params })
  return response.data
}

export const createCopyAssignment = async (assignment: {
  spot_id: number
  copy_id: number
  order_id?: number
}) => {
  const response = await api.post('/copy-assignments', null, {
    params: assignment
  })
  return response.data
}

export const deleteCopyAssignment = async (assignment_id: number) => {
  await api.delete(`/copy-assignments/${assignment_id}`)
}

// Invoices API functions
export const getInvoices = async (params?: {
  skip?: number
  limit?: number
  advertiser_id?: number
  status_filter?: string
}) => {
  const response = await api.get('/invoices', { params })
  return response.data
}

export const getInvoice = async (invoice_id: number) => {
  const response = await api.get(`/invoices/${invoice_id}`)
  return response.data
}

export const createInvoice = async (invoice: {
  invoice_number: string
  advertiser_id: number
  agency_id?: number
  order_id?: number
  campaign_id?: number
  invoice_date: string
  due_date: string
  payment_terms?: string
  notes?: string
  lines?: Array<{
    description: string
    quantity?: number
    unit_price: number | string
    spot_ids?: number[]
  }>
}) => {
  const response = await api.post('/invoices', invoice)
  return response.data
}

export const updateInvoice = async (invoice_id: number, updates: {
  invoice_number?: string
  invoice_date?: string
  due_date?: string
  status?: string
  payment_terms?: string
  notes?: string
}) => {
  const response = await api.put(`/invoices/${invoice_id}`, updates)
  return response.data
}

export const sendInvoice = async (invoice_id: number) => {
  const response = await api.post(`/invoices/${invoice_id}/send`)
  return response.data
}

export const markInvoicePaid = async (invoice_id: number, payment_data: {
  amount?: number | string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.post(`/invoices/${invoice_id}/mark-paid`, payment_data)
  return response.data
}

export const getAgingReport = async () => {
  const response = await api.get('/invoices/aging')
  return response.data
}

// Payments API functions
export const getPayments = async (params?: {
  skip?: number
  limit?: number
  invoice_id?: number
}) => {
  const response = await api.get('/payments', { params })
  return response.data
}

export const getPayment = async (payment_id: number) => {
  const response = await api.get(`/payments/${payment_id}`)
  return response.data
}

export const createPayment = async (payment: {
  invoice_id: number
  amount: number | string
  payment_date: string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.post('/payments', payment)
  return response.data
}

export const updatePayment = async (payment_id: number, updates: {
  amount?: number | string
  payment_date?: string
  payment_method?: string
  reference_number?: string
  notes?: string
}) => {
  const response = await api.put(`/payments/${payment_id}`, updates)
  return response.data
}

export const deletePayment = async (payment_id: number) => {
  await api.delete(`/payments/${payment_id}`)
}

// Makegoods API functions
export const getMakegoods = async (params?: {
  skip?: number
  limit?: number
  campaign_id?: number
}) => {
  const response = await api.get('/makegoods', { params })
  return response.data
}

export const createMakegood = async (makegood: {
  original_spot_id: number
  makegood_spot_id: number
  campaign_id?: number
  reason?: string
}) => {
  const response = await api.post('/makegoods', makegood)
  return response.data
}

export const approveMakegood = async (makegood_id: number) => {
  const response = await api.post(`/makegoods/${makegood_id}/approve`)
  return response.data
}

// Reports API functions
export const getReconciliationReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/reconciliation', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getComplianceReport = async (start_date: string, end_date: string, format: string = 'csv') => {
  const response = await api.get('/reports/compliance', {
    params: { start_date, end_date, format }
  })
  return response.data
}

export const getPlaybackHistory = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/playback', {
    params: { start_date, end_date }
  })
  return response.data
}

// Traffic Reports
export const getDailyLogReport = async (log_date: string) => {
  const response = await api.get('/reports/traffic/daily-log', {
    params: { log_date }
  })
  return response.data
}

export const getMissingCopyReport = async (start_date?: string, end_date?: string) => {
  const response = await api.get('/reports/traffic/missing-copy', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getAvailsReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/traffic/avails', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getConflictsReport = async (log_date: string) => {
  const response = await api.get('/reports/traffic/conflicts', {
    params: { log_date }
  })
  return response.data
}

export const getExpirationsReport = async (days_ahead: number = 30) => {
  const response = await api.get('/reports/traffic/expirations', {
    params: { days_ahead }
  })
  return response.data
}

// Billing Reports
export const getContractActualizationReport = async (
  order_id: number,
  start_date?: string,
  end_date?: string
) => {
  const response = await api.get('/reports/billing/contract-actualization', {
    params: { order_id, start_date, end_date }
  })
  return response.data
}

export const getRevenueSummaryReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/billing/revenue-summary', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getARAgingReport = async () => {
  const response = await api.get('/reports/billing/ar-aging')
  return response.data
}

export const getMakegoodsReport = async (start_date?: string, end_date?: string) => {
  const response = await api.get('/reports/billing/makegoods', {
    params: { start_date, end_date }
  })
  return response.data
}

// Sales Reports
export const getRevenueByRepReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/sales/revenue-by-rep', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenueByAdvertiserReport = async (start_date: string, end_date: string) => {
  const response = await api.get('/reports/sales/revenue-by-advertiser', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getPendingOrdersReport = async () => {
  const response = await api.get('/reports/sales/pending-orders')
  return response.data
}

export const getExpiringContractsReport = async (days_ahead: number = 30) => {
  const response = await api.get('/reports/sales/expiring-contracts', {
    params: { days_ahead }
  })
  return response.data
}

// Export Report
export const exportReport = async (
  report_type: string,
  format: 'pdf' | 'excel' | 'csv',
  report_params?: Record<string, any>
) => {
  const response = await api.post('/reports/export', report_params || {}, {
    params: { report_type, format },
    responseType: format === 'pdf' ? 'blob' : 'blob'
  })
  return response.data
}

// Audit Logs API functions
export const getAuditLogs = async (params?: {
  skip?: number
  limit?: number
  user_id?: number
  action?: string
  resource_type?: string
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/audit-logs', { params })
  return response.data
}

export const getAuditLog = async (audit_log_id: number) => {
  const response = await api.get(`/audit-logs/${audit_log_id}`)
  return response.data
}

// Log Revisions API functions
export const getLogRevisions = async (log_id: number) => {
  const response = await api.get(`/log-revisions/logs/${log_id}/revisions`)
  return response.data
}

export const getLogRevision = async (log_id: number, revision_id: number) => {
  const response = await api.get(`/log-revisions/logs/${log_id}/revisions/${revision_id}`)
  return response.data
}

export const revertToRevision = async (log_id: number, revision_id: number) => {
  const response = await api.post(`/log-revisions/logs/${log_id}/revert`, null, {
    params: { revision_id }
  })
  return response.data
}

// Inventory API functions
export const getInventory = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getInventoryHeatmap = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory/heatmap', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getSelloutPercentages = async (start_date: string, end_date: string) => {
  const response = await api.get('/inventory/sellout', {
    params: { start_date, end_date }
  })
  return response.data
}

// Revenue API functions
export const getRevenueSummary = async (start_date: string, end_date: string) => {
  const response = await api.get('/revenue/summary', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenuePacing = async (start_date: string, end_date: string) => {
  const response = await api.get('/revenue/pacing', {
    params: { start_date, end_date }
  })
  return response.data
}

export const getRevenueForecast = async (months_ahead: number = 3) => {
  const response = await api.get('/revenue/forecast', {
    params: { months_ahead }
  })
  return response.data
}

// Sales Reps API functions
export const getSalesReps = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/sales-reps', { params })
  return response.data
}

// Sales Goals API functions
export const getSalesGoals = async (params?: {
  skip?: number
  limit?: number
  sales_rep_id?: number
}) => {
  const response = await api.get('/sales-goals', { params })
  return response.data
}

export const createSalesGoal = async (goal: {
  sales_rep_id: number
  period: string
  target_date: string
  goal_amount: number | string
}) => {
  const response = await api.post('/sales-goals', goal)
  return response.data
}

export const getSalesGoalsProgress = async (sales_rep_id?: number) => {
  const response = await api.get('/sales-goals/progress', {
    params: sales_rep_id ? { sales_rep_id } : {}
  })
  return response.data
}

// User management API functions
export const getUsers = async (params?: { limit?: number; skip?: number; role_filter?: string }) => {
  const response = await api.get('/users', { params: params || {} })
  return response.data
}

export const getUser = async (userId: number) => {
  const response = await api.get(`/users/${userId}`)
  return response.data
}

export const createUser = async (userData: { username: string; password: string; role: string }) => {
  const response = await api.post('/users', userData)
  return response.data
}

export const updateUser = async (userId: number, userData: { username?: string; password?: string; role?: string }) => {
  const response = await api.put(`/users/${userId}`, userData)
  return response.data
}

export const deleteUser = async (userId: number) => {
  await api.delete(`/users/${userId}`)
}

// Webhooks API functions
export const getWebhooks = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/webhooks', { params })
  return response.data
}

export const getWebhook = async (webhook_id: number) => {
  const response = await api.get(`/webhooks/${webhook_id}`)
  return response.data
}

export const createWebhook = async (webhook: {
  name: string
  webhook_type: string
  url: string
  events: string[]
  secret?: string
  headers?: Record<string, string>
}) => {
  const response = await api.post('/webhooks', webhook)
  return response.data
}

export const updateWebhook = async (webhook_id: number, updates: {
  name?: string
  url?: string
  events?: string[]
  secret?: string
  active?: boolean
  headers?: Record<string, string>
}) => {
  const response = await api.put(`/webhooks/${webhook_id}`, updates)
  return response.data
}

export const deleteWebhook = async (webhook_id: number) => {
  await api.delete(`/webhooks/${webhook_id}`)
}

export const testWebhook = async (webhook_id: number) => {
  const response = await api.post(`/webhooks/${webhook_id}/test`)
  return response.data
}

// Notifications API functions
export const getNotifications = async (params?: {
  unread_only?: boolean
  skip?: number
  limit?: number
}) => {
  const response = await api.get('/notifications', { params })
  return response.data
}

export const getUnreadCount = async () => {
  const response = await api.get('/notifications/unread-count')
  return response.data
}

export const markNotificationRead = async (notification_id: number) => {
  const response = await api.post(`/notifications/${notification_id}/read`)
  return response.data
}

export const markAllNotificationsRead = async () => {
  const response = await api.post('/notifications/mark-all-read')
  return response.data
}

// Settings API functions
export const getAllSettings = async () => {
  const response = await api.get('/settings')
  return response.data
}

export const getCategorySettings = async (category: string) => {
  const response = await api.get(`/settings/${category}`)
  return response.data
}

export const updateCategorySettings = async (category: string, settings: Record<string, { value: string; encrypted?: boolean; description?: string }>) => {
  const response = await api.put(`/settings/${category}`, { settings })
  return response.data
}

export const testSMTPConnection = async (config: {
  host: string
  port: number
  username: string
  password: string
  use_tls?: boolean
}) => {
  const response = await api.post('/settings/test-smtp', config)
  return response.data
}

export const testS3Connection = async (config: {
  access_key_id: string
  secret_access_key: string
  bucket_name: string
  region?: string
}) => {
  const response = await api.post('/settings/test-s3', config)
  return response.data
}

export const testBackblazeConnection = async (config: {
  application_key_id: string
  application_key: string
  bucket_name: string
}) => {
  const response = await api.post('/settings/test-backblaze', config)
  return response.data
}

// Backups API functions
export const getBackups = async (params?: {
  skip?: number
  limit?: number
  status_filter?: string
}) => {
  const response = await api.get('/backups', { params })
  return response.data
}

export const getBackup = async (backup_id: number) => {
  const response = await api.get(`/backups/${backup_id}`)
  return response.data
}

export const createBackup = async (backup: {
  backup_type: 'FULL' | 'DATABASE' | 'FILES'
  storage_provider: 'LOCAL' | 'S3' | 'BACKBLAZE_B2'
  description?: string
}) => {
  const response = await api.post('/backups', backup)
  return response.data
}

export const restoreBackup = async (backup_id: number, restore: {
  restore_database?: boolean
  restore_files?: boolean
}) => {
  const response = await api.post(`/backups/${backup_id}/restore`, restore)
  return response.data
}

export const deleteBackup = async (backup_id: number) => {
  await api.delete(`/backups/${backup_id}`)
}

// Daypart Categories API functions
export const getDaypartCategories = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
}) => {
  const response = await api.get('/daypart-categories', { params })
  return response.data
}

export const getDaypartCategory = async (category_id: number) => {
  const response = await api.get(`/daypart-categories/${category_id}`)
  return response.data
}

export const createDaypartCategory = async (category: {
  name: string
  description?: string
  color?: string
  icon?: string
  sort_order?: number
}) => {
  const response = await api.post('/daypart-categories', category)
  return response.data
}

export const updateDaypartCategory = async (category_id: number, updates: {
  name?: string
  description?: string
  color?: string
  icon?: string
  sort_order?: number
  active?: boolean
}) => {
  const response = await api.put(`/daypart-categories/${category_id}`, updates)
  return response.data
}

export const deleteDaypartCategory = async (category_id: number) => {
  await api.delete(`/daypart-categories/${category_id}`)
}

// Rotation Rules API functions
export const getRotationRules = async (params?: {
  skip?: number
  limit?: number
  active_only?: boolean
  daypart_id?: number
  campaign_id?: number
}) => {
  const response = await api.get('/rotation-rules', { params })
  return response.data
}

export const getRotationRule = async (rule_id: number) => {
  const response = await api.get(`/rotation-rules/${rule_id}`)
  return response.data
}

export const createRotationRule = async (rule: {
  name: string
  description?: string
  rotation_type?: string
  daypart_id?: number
  campaign_id?: number
  min_separation?: number
  max_per_hour?: number
  max_per_day?: number
  weights?: Record<string, any>
  exclude_days?: number[]
  exclude_times?: Array<{ start: string; end: string }>
  priority?: number
}) => {
  const response = await api.post('/rotation-rules', rule)
  return response.data
}

export const updateRotationRule = async (rule_id: number, updates: {
  name?: string
  description?: string
  rotation_type?: string
  daypart_id?: number
  campaign_id?: number
  min_separation?: number
  max_per_hour?: number
  max_per_day?: number
  weights?: Record<string, any>
  exclude_days?: number[]
  exclude_times?: Array<{ start: string; end: string }>
  priority?: number
  active?: boolean
}) => {
  const response = await api.put(`/rotation-rules/${rule_id}`, updates)
  return response.data
}

export const deleteRotationRule = async (rule_id: number) => {
  await api.delete(`/rotation-rules/${rule_id}`)
}

// Traffic Logs API functions
export const getTrafficLogs = async (params?: {
  skip?: number
  limit?: number
  log_type?: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  user_id?: number
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/traffic-logs', { params })
  return response.data
}

export const getTrafficLog = async (log_id: number) => {
  const response = await api.get(`/traffic-logs/${log_id}`)
  return response.data
}

export const createTrafficLog = async (log: {
  log_type: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  copy_id?: number
  message: string
  metadata?: Record<string, any>
}) => {
  const response = await api.post('/traffic-logs', log)
  return response.data
}

export const createTrafficLogsBulk = async (logs: Array<{
  log_type: string
  log_id?: number
  spot_id?: number
  order_id?: number
  campaign_id?: number
  copy_id?: number
  message: string
  metadata?: Record<string, any>
}>) => {
  const response = await api.post('/traffic-logs/bulk', { logs })
  return response.data
}

export const getTrafficLogStats = async (params?: {
  start_date?: string
  end_date?: string
}) => {
  const response = await api.get('/traffic-logs/stats/summary', { params })
  return response.data
}

export default api
