import React, { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Paper,
  Typography,
  Button,
  Alert,
  List,
  ListItem,
  ListItemText,
  Chip,
  CircularProgress,
} from '@mui/material'
import { checkApiHealth } from '../utils/api'
import api from '../utils/api'

const APIDiagnostics: React.FC = () => {
  const [testResults, setTestResults] = useState<Record<string, any>>({})

  const { data: healthData, error: healthError, isLoading: healthLoading } = useQuery({
    queryKey: ['api-health-diagnostic'],
    queryFn: () => checkApiHealth(),
    retry: false,
  })

  const runDiagnostics = async () => {
    const results: Record<string, any> = {}
    
    // Test 0: Network connectivity test
    try {
      const start = Date.now()
      // Try to fetch the health endpoint directly using fetch to see if it's a network issue
      const response = await fetch('/api/health', {
        method: 'GET',
        signal: AbortSignal.timeout(5000),
      })
      const data = await response.json()
      results.network = {
        success: true,
        status: response.status,
        data: data,
        time: Date.now() - start,
        method: 'fetch',
      }
    } catch (error: any) {
      results.network = {
        success: false,
        error: error.message,
        name: error.name,
        method: 'fetch',
      }
    }
    
    // Test 1: Health endpoint via axios
    try {
      const start = Date.now()
      const response = await api.get('/health', { timeout: 5000 })
      results.health = {
        success: true,
        status: response.status,
        data: response.data,
        time: Date.now() - start,
        method: 'axios',
      }
    } catch (error: any) {
      results.health = {
        success: false,
        error: error.message,
        code: error.code,
        responseStatus: error.response?.status,
        responseData: error.response?.data,
        noResponse: !error.response,
        time: Date.now() - Date.now(),
        method: 'axios',
      }
    }

    // Test 2: Tracks endpoint (with auth)
    try {
      const start = Date.now()
      const response = await api.get('/tracks', { params: { limit: 1 }, timeout: 5000 })
      results.tracks = {
        success: true,
        status: response.status,
        dataLength: Array.isArray(response.data) ? response.data.length : 'unknown',
        time: Date.now() - start,
      }
    } catch (error: any) {
      results.tracks = {
        success: false,
        error: error.message,
        code: error.code,
        status: error.response?.status,
        noResponse: !error.response,
        time: Date.now() - Date.now(),
      }
    }

    // Test 3: Check token
    const token = localStorage.getItem('token')
    results.auth = {
      hasToken: !!token,
      tokenLength: token?.length || 0,
      tokenPreview: token ? `${token.substring(0, 20)}...` : 'none',
    }

    // Test 4: Check configuration
    results.config = {
      baseURL: api.defaults.baseURL,
      timeout: api.defaults.timeout,
      currentUrl: window.location.href,
      protocol: window.location.protocol,
      hostname: window.location.hostname,
      port: window.location.port,
      isDev: import.meta.env.DEV,
      isProd: import.meta.env.PROD,
      mode: import.meta.env.MODE,
    }

    // Test 5: Check if backend container might be accessible
    results.recommendations = []
    if (!results.health?.success && !results.network?.success) {
      results.recommendations.push('Backend container may not be running or not responding')
      results.recommendations.push('Check container status: docker ps | grep api')
      results.recommendations.push('Check backend logs: docker logs librelog-api-1 --tail 50')
      results.recommendations.push('Verify backend is listening: docker exec librelog-api-1 netstat -tlnp 2>/dev/null || docker exec librelog-api-1 ss -tlnp')
    }
    if (results.config.isProd && results.config.hostname.includes('gayphx.com')) {
      results.recommendations.push('Production mode: Verify Traefik routing is configured correctly')
      results.recommendations.push('Backend should be accessible at: https://log-dev.gayphx.com/api/health')
      results.recommendations.push('Check Traefik dashboard: Verify librelog-api service is registered')
      results.recommendations.push('Verify backend container is on Traefik network: docker network inspect traefik')
      results.recommendations.push('Test direct backend: curl http://api:8000/api/health (from within container network)')
      results.recommendations.push('Restart backend: docker restart librelog-api-1')
      results.recommendations.push('Restart Traefik to pick up new labels: docker restart traefik (if applicable)')
    }
    if (results.config.isDev) {
      results.recommendations.push('Development mode: Vite proxy should forward /api to http://api:8000')
      results.recommendations.push('Ensure backend container is running and accessible from frontend container')
      results.recommendations.push('Check Vite proxy configuration in vite.config.ts')
    }

    setTestResults(results)
  }

  return (
    <Paper sx={{ p: 3, mt: 2 }}>
      <Typography variant="h6" gutterBottom>
        API Diagnostics
      </Typography>

      <Box sx={{ mb: 2 }}>
        <Button variant="contained" onClick={runDiagnostics}>
          Run Diagnostics
        </Button>
      </Box>

      {healthLoading && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <CircularProgress size={20} />
          <Typography>Checking API health...</Typography>
        </Box>
      )}

      {healthError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Health check failed: {healthError instanceof Error ? healthError.message : 'Unknown error'}
        </Alert>
      )}

      {healthData && (
        <Alert severity="success" sx={{ mb: 2 }}>
          Health check passed: {JSON.stringify(healthData)}
        </Alert>
      )}

      {Object.keys(testResults).length > 0 && (
        <Box>
          <Typography variant="subtitle1" gutterBottom sx={{ mt: 2 }}>
            Test Results:
          </Typography>
          <List>
            {Object.entries(testResults).map(([key, value]) => {
              // Skip recommendations - show separately
              if (key === 'recommendations') return null
              
              return (
                <ListItem key={key}>
                  <ListItemText
                    primary={key}
                    secondary={
                      <Box>
                        {typeof value === 'object' && !Array.isArray(value) ? (
                          Object.entries(value).map(([k, v]) => (
                            <Typography key={k} variant="body2" component="span" sx={{ display: 'block' }}>
                              <strong>{k}:</strong> {String(v)}
                            </Typography>
                          ))
                        ) : (
                          <Typography variant="body2">{String(value)}</Typography>
                        )}
                      </Box>
                    }
                  />
                  {value && typeof value === 'object' && value.success !== undefined && (
                    <Chip
                      label={value.success ? 'Success' : 'Failed'}
                      color={value.success ? 'success' : 'error'}
                      size="small"
                    />
                  )}
                </ListItem>
              )
            })}
          </List>
          
          {testResults.recommendations && testResults.recommendations.length > 0 && (
            <Box sx={{ mt: 3 }}>
              <Typography variant="subtitle1" gutterBottom>
                Recommendations:
              </Typography>
              <Alert severity="info" sx={{ mt: 1 }}>
                <List dense>
                  {testResults.recommendations.map((rec: string, idx: number) => (
                    <ListItem key={idx} sx={{ py: 0.5 }}>
                      <ListItemText 
                        primary={rec}
                        primaryTypographyProps={{ variant: 'body2' }}
                      />
                    </ListItem>
                  ))}
                </List>
              </Alert>
            </Box>
          )}
        </Box>
      )}
    </Paper>
  )
}

export default APIDiagnostics

