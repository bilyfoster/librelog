import React, { useState } from 'react'
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  CircularProgress,
} from '@mui/material'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useQuery } from '@tanstack/react-query'

const Login: React.FC = () => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login, user, isLoading: authLoading } = useAuth()
  
  // Fetch public branding settings (logo and system name) - no auth required
  const { data: brandingData } = useQuery({
    queryKey: ['branding-public'],
    queryFn: async () => {
      try {
        const response = await fetch('/api/settings/branding/public')
        if (!response.ok) return null
        return response.json()
      } catch {
        return null
      }
    },
    staleTime: 5 * 60 * 1000, // Cache for 5 minutes
    retry: false, // Don't retry on login page
  })
  
  const systemName = brandingData?.system_name || 'GayPHX Radio Traffic System'
  const logoUrl = brandingData?.logo_url || null
  const headerColor = brandingData?.header_color || '#424242'
  
  // Show logo section even if logoUrl is not available (for spacing/consistency)
  // The logo will be hidden by onError handler if it fails to load

  // Redirect to dashboard if already logged in
  if (authLoading) {
    return (
      <Box
        display="flex"
        justifyContent="center"
        alignItems="center"
        minHeight="100vh"
      >
        <CircularProgress />
      </Box>
    )
  }

  if (user) {
    return <Navigate to="/dashboard" replace />
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')

    try {
      await login(username, password)
    } catch (err: any) {
      setError(err.message || 'Invalid username or password')
      console.error('Login error:', err)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Box
      display="flex"
      justifyContent="center"
      alignItems="center"
      minHeight="100vh"
      sx={{
        background: `linear-gradient(135deg, ${headerColor}15 0%, ${headerColor}05 100%)`,
        position: 'relative',
        '&::before': {
          content: '""',
          position: 'absolute',
          top: 0,
          left: 0,
          right: 0,
          height: '4px',
          background: `linear-gradient(90deg, ${headerColor} 0%, ${headerColor}dd 100%)`,
        }
      }}
    >
      <Card 
        sx={{ 
          maxWidth: 400, 
          width: '100%', 
          mx: 2,
          boxShadow: `0 4px 20px ${headerColor}20`,
          borderTop: `3px solid ${headerColor}`,
        }}
      >
        <CardContent sx={{ p: 4 }}>
          <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2, minHeight: 80 }}>
            {logoUrl ? (
              <img 
                src={logoUrl} 
                alt="Logo" 
                style={{ maxHeight: 80, maxWidth: 300, objectFit: 'contain' }}
                onError={(e) => {
                  console.error('Failed to load logo:', logoUrl)
                  e.currentTarget.style.display = 'none'
                }}
              />
            ) : (
              // Placeholder when logo is not configured or API fails
              <Box
                sx={{
                  width: 200,
                  height: 60,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: `2px dashed ${headerColor}40`,
                  borderRadius: 1,
                  color: headerColor,
                  fontSize: '0.875rem',
                }}
              >
                Logo
              </Box>
            )}
          </Box>
          <Typography 
            variant="h4" 
            component="h1" 
            gutterBottom 
            align="center"
            sx={{ color: headerColor, fontWeight: 'bold' }}
          >
            {systemName}
          </Typography>
          <Typography variant="subtitle1" align="center" color="textSecondary" gutterBottom>
            LibreLog
          </Typography>
          
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          
          <Box component="form" onSubmit={handleSubmit}>
            <TextField
              fullWidth
              label="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              margin="normal"
              required
              autoComplete="username"
            />
            <TextField
              fullWidth
              label="Password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              margin="normal"
              required
              autoComplete="current-password"
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ 
                mt: 3, 
                mb: 2,
                backgroundColor: headerColor,
                '&:hover': {
                  backgroundColor: headerColor,
                  opacity: 0.9,
                },
                '&:disabled': {
                  backgroundColor: headerColor,
                  opacity: 0.6,
                }
              }}
              disabled={isLoading}
            >
              {isLoading ? 'Signing In...' : 'Sign In'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Box>
  )
}

export default Login
