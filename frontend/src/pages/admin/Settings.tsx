import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  Tab,
  Tabs,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Stack,
  Divider,
} from '@mui/material'
import {
  Save as SaveIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
  CloudUpload as CloudIcon,
  Email as EmailIcon,
  Storage as StorageIcon,
  Settings as SettingsIcon,
  Backup as BackupIcon,
  Webhook as WebhookIcon,
  Palette as PaletteIcon,
  Image as ImageIcon,
} from '@mui/icons-material'
import {
  getSettingsProxy,
  updateCategorySettings,
  testSMTPConnection,
  testS3Connection,
  testBackblazeConnection,
} from '../../utils/api'

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
      id={`settings-tabpanel-${index}`}
      aria-labelledby={`settings-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  )
}

const Settings: React.FC = () => {
  const [tabValue, setTabValue] = useState(0)
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [testDialog, setTestDialog] = useState<{ open: boolean; type: 'smtp' | 's3' | 'backblaze' | null }>({ open: false, type: null })
  const [testResult, setTestResult] = useState<{ success: boolean; message: string } | null>(null)
  const [testLoading, setTestLoading] = useState(false)
  const [logoUploading, setLogoUploading] = useState(false)

  const queryClient = useQueryClient()

  const { data: allSettings, isLoading, error } = useQuery({
    queryKey: ['settings'],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getSettingsProxy()
      return data
    },
    retry: 1,
  })

  const [formData, setFormData] = useState<Record<string, Record<string, string>>>({})

  React.useEffect(() => {
    if (allSettings) {
      // Transform settings to match form data structure
      const transformed: Record<string, Record<string, string>> = {}
      for (const [category, settings] of Object.entries(allSettings)) {
        transformed[category] = {}
        if (settings && typeof settings === 'object') {
          for (const [key, value] of Object.entries(settings)) {
            if (value && typeof value === 'object' && 'value' in value) {
              transformed[category][key] = value.value || ''
            } else {
              transformed[category][key] = value || ''
            }
          }
        }
      }
      setFormData(transformed)
    }
  }, [allSettings])

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue)
    setSaveMessage(null)
  }

  const handleChange = (category: string, key: string, value: string) => {
    setFormData((prev) => ({
      ...prev,
      [category]: {
        ...prev[category],
        [key]: value,
      },
    }))
  }

  const saveMutation = useMutation({
    mutationFn: async ({ category, settings }: { category: string; settings: Record<string, any> }) => {
      return updateCategorySettings(category, settings)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['settings'] })
      queryClient.removeQueries({ queryKey: ['settings'] }) // Force refetch
      setSaveMessage({ type: 'success', text: 'Settings saved successfully' })
      setTimeout(() => setSaveMessage(null), 3000)
    },
    onError: (error: any) => {
      let message = 'Failed to save settings'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setSaveMessage({ type: 'error', text: message })
      console.error('Save settings error:', error)
    },
  })

  const handleSave = async (category: string) => {
    const settings = formData[category] || {}
    const settingsToSave: Record<string, { value: string; encrypted?: boolean }> = {}
    
    for (const [key, value] of Object.entries(settings)) {
      const isPassword = key.toLowerCase().includes('password') || key.toLowerCase().includes('secret') || key.toLowerCase().includes('key')
      settingsToSave[key] = {
        value: value || '',
        encrypted: isPassword,
      }
    }
    
    // Ensure logo_url is included if it exists in formData (even if empty string)
    if (category === 'branding' && 'logo_url' in settings) {
      settingsToSave['logo_url'] = {
        value: settings.logo_url || '',
        encrypted: false,
      }
    }
    
    saveMutation.mutate({ category, settings: settingsToSave })
  }

  const handleTestConnection = async (type: 'smtp' | 's3' | 'backblaze') => {
    setTestLoading(true)
    setTestResult(null)
    
    try {
      const category = type === 'smtp' ? 'smtp' : 'storage'
      const settings = formData[category] || {}
      
      if (type === 'smtp') {
        const result = await testSMTPConnection({
          host: settings.host || '',
          port: parseInt(settings.port || '587'),
          username: settings.username || '',
          password: settings.password || '',
          use_tls: settings.use_tls === 'true' || settings.use_tls === '1',
        })
        setTestResult(result)
      } else if (type === 's3') {
        const result = await testS3Connection({
          access_key_id: settings.access_key_id || '',
          secret_access_key: settings.secret_access_key || '',
          bucket_name: settings.bucket_name || '',
          region: settings.region || 'us-east-1',
        })
        setTestResult(result)
      } else if (type === 'backblaze') {
        const result = await testBackblazeConnection({
          application_key_id: settings.application_key_id || '',
          application_key: settings.application_key || '',
          bucket_name: settings.bucket_name || '',
        })
        setTestResult(result)
      }
    } catch (error: any) {
      let message = 'Connection test failed'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setTestResult({
        success: false,
        message: message,
      })
      console.error('Test connection error:', error)
    } finally {
      setTestLoading(false)
    }
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box>
        <Alert severity="error" action={
          <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['settings'] })}>
            Retry
          </Button>
        }>
          Failed to load settings: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  const categories = [
    { id: 'branding', label: 'Branding', icon: <PaletteIcon /> },
    { id: 'general', label: 'General', icon: <SettingsIcon /> },
    { id: 'smtp', label: 'SMTP', icon: <EmailIcon /> },
    { id: 'storage', label: 'Storage', icon: <StorageIcon /> },
    { id: 'backup', label: 'Backup', icon: <BackupIcon /> },
    { id: 'integrations', label: 'Integrations', icon: <WebhookIcon /> },
  ]

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Settings
      </Typography>
      <Typography variant="body2" color="text.secondary" paragraph>
        Configure application settings across all categories
      </Typography>

      {saveMessage && (
        <Alert severity={saveMessage.type} sx={{ mb: 2 }}>
          {saveMessage.text}
        </Alert>
      )}

      <Card>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={handleTabChange} aria-label="settings tabs">
            {categories.map((cat, index) => (
              <Tab
                key={cat.id}
                label={cat.label}
                icon={cat.icon}
                iconPosition="start"
                id={`settings-tab-${index}`}
                aria-controls={`settings-tabpanel-${index}`}
              />
            ))}
          </Tabs>
        </Box>

        {/* Branding Settings */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom>
                Branding Configuration
              </Typography>
              <Typography variant="body2" color="text.secondary" paragraph>
                Customize the appearance and name of your traffic system. Changes will be reflected in the header.
              </Typography>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="System Name"
                value={formData.branding?.system_name || ''}
                onChange={(e) => handleChange('branding', 'system_name', e.target.value)}
                margin="normal"
                helperText="Name displayed in the header"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Header Color"
                value={formData.branding?.header_color || '#424242'}
                onChange={(e) => handleChange('branding', 'header_color', e.target.value)}
                margin="normal"
                helperText="Hex color code (e.g., #424242). Ensure API status indicators remain visible."
                InputProps={{
                  startAdornment: (
                    <Box
                      sx={{
                        width: 24,
                        height: 24,
                        backgroundColor: formData.branding?.header_color || '#424242',
                        border: '1px solid #ccc',
                        borderRadius: 1,
                        mr: 1,
                      }}
                    />
                  ),
                }}
              />
            </Grid>
            <Grid item xs={12}>
              <Typography variant="h6" gutterBottom sx={{ mt: 2 }}>
                Logo
              </Typography>
              {formData.branding?.logo_url && (
                <Box sx={{ mb: 2 }}>
                  <img 
                    src={formData.branding.logo_url} 
                    alt="Current logo" 
                    style={{ maxHeight: 100, maxWidth: 300 }}
                  />
                </Box>
              )}
              <input
                accept="image/*"
                style={{ display: 'none' }}
                id="logo-upload"
                type="file"
                onChange={async (e) => {
                  const file = e.target.files?.[0]
                  if (!file) return
                  
                  setLogoUploading(true)
                  try {
                    const formData = new FormData()
                    formData.append('file', file)
                    
                    const token = localStorage.getItem('token')
                    const response = await fetch('/api/settings/branding/upload-logo', {
                      method: 'POST',
                      headers: {
                        'Authorization': `Bearer ${token}`,
                      },
                      body: formData,
                    })
                    
                    if (!response.ok) {
                      const error = await response.json()
                      throw new Error(error.detail || 'Failed to upload logo')
                    }
                    
                    const result = await response.json()
                    handleChange('branding', 'logo_url', result.logo_url)
                    // Also save the logo_url to settings immediately
                    await updateCategorySettings('branding', {
                      logo_url: {
                        value: result.logo_url,
                        encrypted: false,
                      }
                    })
                    queryClient.invalidateQueries({ queryKey: ['settings'] })
                    setSaveMessage({ type: 'success', text: 'Logo uploaded and saved successfully' })
                    setTimeout(() => setSaveMessage(null), 3000)
                  } catch (error: any) {
                    setSaveMessage({ type: 'error', text: error.message || 'Failed to upload logo' })
                    setTimeout(() => setSaveMessage(null), 5000)
                  } finally {
                    setLogoUploading(false)
                    // Reset input
                    e.target.value = ''
                  }
                }}
              />
              <label htmlFor="logo-upload">
                <Button
                  variant="outlined"
                  component="span"
                  startIcon={<ImageIcon />}
                  disabled={logoUploading}
                  sx={{ mt: 1 }}
                >
                  {logoUploading ? 'Uploading...' : formData.branding?.logo_url ? 'Replace Logo' : 'Upload Logo'}
                </Button>
              </label>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Upload a logo image (PNG, JPG, GIF, SVG, or WebP, max 5MB). Logo will appear in the header and login page.
              </Typography>
            </Grid>
            <Grid item xs={12}>
              <Alert severity="info" sx={{ mt: 2 }}>
                <Typography variant="body2">
                  <strong>Note:</strong> The header color should provide sufficient contrast for the API status indicators (green for online, red for offline) to remain clearly visible.
                </Typography>
              </Alert>
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                startIcon={<SaveIcon />}
                onClick={() => handleSave('branding')}
                disabled={saveMutation.isPending}
              >
                {saveMutation.isPending ? 'Saving...' : 'Save Branding Settings'}
              </Button>
            </Grid>
          </Grid>
        </TabPanel>

        {/* General Settings */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Application Name"
                value={formData.general?.app_name || ''}
                onChange={(e) => handleChange('general', 'app_name', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Time Zone"
                value={formData.general?.timezone || 'UTC'}
                onChange={(e) => handleChange('general', 'timezone', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Date Format"
                value={formData.general?.date_format || 'YYYY-MM-DD'}
                onChange={(e) => handleChange('general', 'date_format', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Time Format"
                value={formData.general?.time_format || 'HH:mm'}
                onChange={(e) => handleChange('general', 'time_format', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                startIcon={<SaveIcon />}
                onClick={() => handleSave('general')}
                disabled={saveMutation.isPending}
              >
                Save General Settings
              </Button>
            </Grid>
          </Grid>
        </TabPanel>

        {/* SMTP Settings */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="SMTP Host"
                value={formData.smtp?.host || ''}
                onChange={(e) => handleChange('smtp', 'host', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="SMTP Port"
                type="number"
                value={formData.smtp?.port || '587'}
                onChange={(e) => handleChange('smtp', 'port', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Username"
                value={formData.smtp?.username || ''}
                onChange={(e) => handleChange('smtp', 'username', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Password"
                type="password"
                value={formData.smtp?.password || ''}
                onChange={(e) => handleChange('smtp', 'password', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="From Email"
                type="email"
                value={formData.smtp?.from_email || ''}
                onChange={(e) => handleChange('smtp', 'from_email', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.smtp?.use_tls === 'true' || formData.smtp?.use_tls === '1'}
                    onChange={(e) => handleChange('smtp', 'use_tls', e.target.checked ? 'true' : 'false')}
                  />
                }
                label="Use TLS"
              />
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" spacing={2}>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  onClick={() => handleSave('smtp')}
                  disabled={saveMutation.isPending}
                >
                  Save SMTP Settings
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => handleTestConnection('smtp')}
                  disabled={testLoading}
                >
                  Test Connection
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Storage Settings */}
        <TabPanel value={tabValue} index={3}>
          <Typography variant="h6" gutterBottom>
            S3 Configuration
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Access Key ID"
                value={formData.storage?.access_key_id || ''}
                onChange={(e) => handleChange('storage', 'access_key_id', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Secret Access Key"
                type="password"
                value={formData.storage?.secret_access_key || ''}
                onChange={(e) => handleChange('storage', 'secret_access_key', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Bucket Name"
                value={formData.storage?.bucket_name || ''}
                onChange={(e) => handleChange('storage', 'bucket_name', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Region"
                value={formData.storage?.region || 'us-east-1'}
                onChange={(e) => handleChange('storage', 'region', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="outlined"
                onClick={() => handleTestConnection('s3')}
                disabled={testLoading}
                sx={{ mr: 2 }}
              >
                Test S3 Connection
              </Button>
            </Grid>
          </Grid>

          <Divider sx={{ my: 4 }} />

          <Typography variant="h6" gutterBottom>
            Backblaze B2 Configuration
          </Typography>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Application Key ID"
                value={formData.storage?.application_key_id || ''}
                onChange={(e) => handleChange('storage', 'application_key_id', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Application Key"
                type="password"
                value={formData.storage?.application_key || ''}
                onChange={(e) => handleChange('storage', 'application_key', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Bucket Name"
                value={formData.storage?.b2_bucket_name || ''}
                onChange={(e) => handleChange('storage', 'b2_bucket_name', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12}>
              <Stack direction="row" spacing={2}>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  onClick={() => handleSave('storage')}
                  disabled={saveMutation.isPending}
                >
                  Save Storage Settings
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => handleTestConnection('backblaze')}
                  disabled={testLoading}
                >
                  Test Backblaze Connection
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Backup Settings */}
        <TabPanel value={tabValue} index={4}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth margin="normal">
                <InputLabel>Storage Provider</InputLabel>
                <Select
                  value={formData.backup?.storage_provider || 'local'}
                  onChange={(e) => handleChange('backup', 'storage_provider', e.target.value)}
                  label="Storage Provider"
                >
                  <MenuItem value="local">Local</MenuItem>
                  <MenuItem value="s3">Amazon S3</MenuItem>
                  <MenuItem value="backblaze">Backblaze B2</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Backup Retention (days)"
                type="number"
                value={formData.backup?.retention_days || '30'}
                onChange={(e) => handleChange('backup', 'retention_days', e.target.value)}
                margin="normal"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Backup Schedule (cron)"
                value={formData.backup?.schedule || '0 2 * * *'}
                onChange={(e) => handleChange('backup', 'schedule', e.target.value)}
                margin="normal"
                helperText="Cron expression (e.g., '0 2 * * *' for daily at 2 AM)"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                startIcon={<SaveIcon />}
                onClick={() => handleSave('backup')}
                disabled={saveMutation.isPending}
              >
                Save Backup Settings
              </Button>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Integrations Settings */}
        <TabPanel value={tabValue} index={5}>
          <Alert severity="info" sx={{ mb: 2 }}>
            LibreTime configuration is loaded from environment variables (LIBRETIME_URL, LIBRETIME_API_KEY).
            Changes here will be saved to the database but environment variables take precedence at runtime.
          </Alert>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="LibreTime API URL"
                value={formData.integrations?.libretime_url || ''}
                onChange={(e) => handleChange('integrations', 'libretime_url', e.target.value)}
                margin="normal"
                helperText="Current value from environment or database"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="LibreTime API Key"
                type="password"
                value={formData.integrations?.libretime_api_key || ''}
                onChange={(e) => handleChange('integrations', 'libretime_api_key', e.target.value)}
                margin="normal"
                helperText="Current value from environment or database (masked if from env)"
              />
            </Grid>
            <Grid item xs={12}>
              <Button
                variant="contained"
                startIcon={<SaveIcon />}
                onClick={() => handleSave('integrations')}
                disabled={saveMutation.isPending}
              >
                Save Integration Settings
              </Button>
            </Grid>
          </Grid>
        </TabPanel>
      </Card>

      {/* Test Connection Dialog */}
      <Dialog open={testDialog.open} onClose={() => setTestDialog({ open: false, type: null })} maxWidth="sm" fullWidth>
        <DialogTitle>Test Connection</DialogTitle>
        <DialogContent>
          {testLoading ? (
            <Box display="flex" justifyContent="center" p={3}>
              <CircularProgress />
            </Box>
          ) : testResult ? (
            <Alert severity={testResult.success ? 'success' : 'error'} icon={testResult.success ? <SuccessIcon /> : <ErrorIcon />}>
              {testResult.message}
            </Alert>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTestDialog({ open: false, type: null })}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default Settings

