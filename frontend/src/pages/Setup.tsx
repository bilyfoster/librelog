import React, { useState, useEffect } from 'react'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  TextField,
  Alert,
  Stepper,
  Step,
  StepLabel,
  CircularProgress,
  Grid,
  Chip,
} from '@mui/material'
import { CheckCircle, Error, Warning } from '@mui/icons-material'
import api from '../utils/api'

interface SetupStatus {
  admin_user_exists: boolean
  api_keys_configured: boolean
  libretime_configured: boolean
  azuracast_configured: boolean
}

interface ApiKeyStatus {
  libretime_configured: boolean
  azuracast_configured: boolean
  libretime_url: string
  azuracast_url: string
}

const SetupPage: React.FC = () => {
  const [setupStatus, setSetupStatus] = useState<SetupStatus | null>(null)
  const [apiKeyStatus, setApiKeyStatus] = useState<ApiKeyStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  
  const [adminForm, setAdminForm] = useState({
    username: 'admin',
    password: 'admin123',
    confirmPassword: 'admin123'
  })

  useEffect(() => {
    checkSetupStatus()
  }, [])

  const checkSetupStatus = async () => {
    try {
      const [statusResponse, apiResponse] = await Promise.all([
        api.get('/setup/status'),
        api.get('/setup/api-keys')
      ])
      
      setSetupStatus(statusResponse.data)
      setApiKeyStatus(apiResponse.data)
    } catch (err) {
      console.error('Failed to check setup status:', err)
    } finally {
      setLoading(false)
    }
  }

  const createAdminUser = async () => {
    if (adminForm.password !== adminForm.confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setCreating(true)
    setError('')
    
    try {
      await api.post('/setup/create-admin', {
        username: adminForm.username,
        password: adminForm.password,
        role: 'admin'
      })
      
      setSuccess('Admin user created successfully!')
      await checkSetupStatus()
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to create admin user')
    } finally {
      setCreating(false)
    }
  }

  const runInitialSetup = async () => {
    setCreating(true)
    setError('')
    
    try {
      await api.post('/setup/run-initial-setup')
      setSuccess('Initial setup completed!')
      await checkSetupStatus()
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to run initial setup')
    } finally {
      setCreating(false)
    }
  }

  const getStepStatus = (step: number) => {
    if (!setupStatus) return 'pending'
    
    switch (step) {
      case 0:
        return setupStatus.admin_user_exists ? 'completed' : 'active'
      case 1:
        return setupStatus.api_keys_configured ? 'completed' : 'active'
      case 2:
        return setupStatus.admin_user_exists && setupStatus.api_keys_configured ? 'completed' : 'pending'
      default:
        return 'pending'
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box maxWidth={800} mx="auto" p={3}>
      <Typography variant="h4" gutterBottom>
        LibreLog Setup
      </Typography>
      
      <Typography variant="body1" color="textSecondary" paragraph>
        Welcome to LibreLog! Let's get your radio traffic system configured.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {success}
        </Alert>
      )}

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Setup Progress
          </Typography>
          
          <Stepper activeStep={setupStatus?.admin_user_exists ? 1 : 0} orientation="vertical">
            <Step completed={setupStatus?.admin_user_exists}>
              <StepLabel>
                Create Admin User
                {setupStatus?.admin_user_exists && <CheckCircle color="success" sx={{ ml: 1 }} />}
              </StepLabel>
            </Step>
            
            <Step completed={setupStatus?.api_keys_configured}>
              <StepLabel>
                Configure API Keys
                {setupStatus?.api_keys_configured && <CheckCircle color="success" sx={{ ml: 1 }} />}
              </StepLabel>
            </Step>
            
            <Step completed={setupStatus?.admin_user_exists && setupStatus?.api_keys_configured}>
              <StepLabel>
                Ready to Use
                {setupStatus?.admin_user_exists && setupStatus?.api_keys_configured && 
                  <CheckCircle color="success" sx={{ ml: 1 }} />}
              </StepLabel>
            </Step>
          </Stepper>
        </CardContent>
      </Card>

      {/* Admin User Creation */}
      {!setupStatus?.admin_user_exists && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Create Admin User
            </Typography>
            
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Username"
                  value={adminForm.username}
                  onChange={(e) => setAdminForm({...adminForm, username: e.target.value})}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Password"
                  type="password"
                  value={adminForm.password}
                  onChange={(e) => setAdminForm({...adminForm, password: e.target.value})}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Confirm Password"
                  type="password"
                  value={adminForm.confirmPassword}
                  onChange={(e) => setAdminForm({...adminForm, confirmPassword: e.target.value})}
                />
              </Grid>
            </Grid>
            
            <Box mt={2}>
              <Button
                variant="contained"
                onClick={createAdminUser}
                disabled={creating}
                startIcon={creating ? <CircularProgress size={20} /> : null}
              >
                {creating ? 'Creating...' : 'Create Admin User'}
              </Button>
            </Box>
          </CardContent>
        </Card>
      )}

      {/* API Keys Status */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            API Configuration
          </Typography>
          
          <Grid container spacing={2}>
            <Grid item xs={12} sm={6}>
              <Box display="flex" alignItems="center" gap={1}>
                <Typography variant="body2">LibreTime:</Typography>
                <Chip
                  label={apiKeyStatus?.libretime_configured ? 'Configured' : 'Not Configured'}
                  color={apiKeyStatus?.libretime_configured ? 'success' : 'error'}
                  size="small"
                />
              </Box>
              {apiKeyStatus?.libretime_url && (
                <Typography variant="caption" color="textSecondary">
                  {apiKeyStatus.libretime_url}
                </Typography>
              )}
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <Box display="flex" alignItems="center" gap={1}>
                <Typography variant="body2">AzuraCast:</Typography>
                <Chip
                  label={apiKeyStatus?.azuracast_configured ? 'Configured' : 'Not Configured'}
                  color={apiKeyStatus?.azuracast_configured ? 'success' : 'error'}
                  size="small"
                />
              </Box>
              {apiKeyStatus?.azuracast_url && (
                <Typography variant="caption" color="textSecondary">
                  {apiKeyStatus.azuracast_url}
                </Typography>
              )}
            </Grid>
          </Grid>
          
          {!apiKeyStatus?.libretime_configured || !apiKeyStatus?.azuracast_configured ? (
            <Alert severity="warning" sx={{ mt: 2 }}>
              <Typography variant="body2">
                To configure API keys, edit your <code>.env</code> file with:
              </Typography>
              <Box component="pre" sx={{ mt: 1, fontSize: '0.8rem' }}>
{`LIBRETIME_URL=https://your-libretime-url/api
LIBRETIME_API_KEY=your-libretime-api-key
AZURACAST_URL=https://your-azuracast-url/api
AZURACAST_API_KEY=your-azuracast-api-key`}
              </Box>
            </Alert>
          ) : (
            <Alert severity="success" sx={{ mt: 2 }}>
              All API keys are configured correctly!
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* Quick Setup */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Quick Setup
          </Typography>
          
          <Typography variant="body2" color="textSecondary" paragraph>
            Run the initial setup to create a default admin user and check configuration.
          </Typography>
          
          <Button
            variant="outlined"
            onClick={runInitialSetup}
            disabled={creating}
            startIcon={creating ? <CircularProgress size={20} /> : null}
          >
            {creating ? 'Running Setup...' : 'Run Initial Setup'}
          </Button>
        </CardContent>
      </Card>
    </Box>
  )
}

export default SetupPage
