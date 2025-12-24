import React from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getDashboardStats, getRecentActivity, checkApiHealth } from '../utils/api'
import APIDiagnostics from '../components/APIDiagnostics'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Alert,
  CircularProgress,
  Button,
  Stack,
  Divider,
} from '@mui/material'
import {
  MusicNote,
  Campaign,
  Schedule,
  BarChart,
  Upload,
  AddCircle,
  PlayArrow,
} from '@mui/icons-material'

const Dashboard: React.FC = () => {
  const navigate = useNavigate()
  
  // Check API health first
  const { data: healthData, error: healthError } = useQuery({
    queryKey: ['api-health'],
    queryFn: () => checkApiHealth(),
    retry: false,
    refetchInterval: 30000,
  })

  // Fetch dashboard stats
  const { data: statsData, isLoading: statsLoading, error: statsError } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => getDashboardStats(),
    retry: 1,
    retryDelay: 1000,
    staleTime: 60000,
  })

  // Fetch recent activity
  const { data: activityData, isLoading: activityLoading } = useQuery({
    queryKey: ['recentActivity'],
    queryFn: () => getRecentActivity(20),
    retry: false,
    staleTime: 30000,
  })

  const stats = [
    { title: 'Total Tracks', value: statsData?.totalTracks || '0', icon: MusicNote },
    { title: 'Active Campaigns', value: statsData?.activeCampaigns || '0', icon: Campaign },
    { title: 'Clock Templates', value: statsData?.clockTemplates || '0', icon: Schedule },
    { title: 'Reports Generated', value: statsData?.reportsGenerated || '0', icon: BarChart },
  ]

  const recentActivity = activityData?.activities || []

  // Show API connectivity issue first
  if (healthError) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" component="h1" sx={{ mb: 2, fontWeight: 500 }}>
          Dashboard
        </Typography>
        <Alert severity="error" sx={{ mb: 2 }}>
          <Typography variant="h6" sx={{ mb: 1 }}>
            API Connection Failed
          </Typography>
          <Typography variant="body2" sx={{ mb: 1 }}>
            Cannot reach the backend API. The backend may be down or unreachable.
          </Typography>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Error: {healthError instanceof Error ? healthError.message : 'Unknown error'}
          </Typography>
          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
              Troubleshooting Steps:
            </Typography>
            <Box component="ol" sx={{ mt: 1, ml: 3 }}>
              <li>Check if backend container is running: <code>docker-compose ps | grep api</code></li>
              <li>Check backend logs: <code>docker-compose logs api</code></li>
              <li>Verify backend is listening on port 8000</li>
              <li>Check Traefik routing configuration (if in production)</li>
              <li>Verify network connectivity between frontend and backend containers</li>
              <li>Check browser Network tab for actual HTTP status codes</li>
              <li>Verify CORS configuration allows requests from this origin</li>
            </Box>
          </Box>
          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
              Common Issues:
            </Typography>
            <Box component="ul" sx={{ mt: 1, ml: 3 }}>
              <li>Backend container not started → Start with <code>docker-compose up -d api</code></li>
              <li>Backend crashed → Check logs for Python errors</li>
              <li>Network isolation → Containers may not be on same Docker network</li>
              <li>Port conflict → Another service may be using port 8000</li>
              <li>Traefik misconfiguration → Backend service not registered with Traefik</li>
            </Box>
          </Box>
        </Alert>
        <APIDiagnostics />
      </Box>
    )
  }

  if (statsLoading) {
    return (
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        minHeight: '400px',
        flexDirection: 'column',
        gap: 2
      }}>
        <CircularProgress size={48} />
        {healthData && (
          <Typography>API Status: {healthData.status}</Typography>
        )}
      </Box>
    )
  }

  if (statsError) {
    return (
      <Box sx={{ p: 3 }}>
        <Typography variant="h4" component="h1" sx={{ mb: 2, fontWeight: 500 }}>
          Dashboard
        </Typography>
        {healthData && (
          <Alert severity="info" sx={{ mb: 2 }}>
            API is reachable (Status: {healthData.status}), but some endpoints are failing.
          </Alert>
        )}
        <Alert severity="error">
          Failed to load dashboard data: {statsError instanceof Error ? statsError.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" sx={{ mb: 3, fontWeight: 500 }}>
        Dashboard
      </Typography>
      
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {stats.map((stat, index) => {
          const IconComponent = stat.icon
          return (
            <Grid item xs={12} sm={6} md={3} key={index}>
              <Card>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <IconComponent sx={{ fontSize: '2rem', color: 'primary.main' }} />
                    <Box>
                      <Typography variant="h5" sx={{ fontWeight: 'bold', mb: 0.5 }}>
                        {stat.value}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {stat.title}
                      </Typography>
                    </Box>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          )
        })}
      </Grid>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 500 }}>
                Recent Activity
              </Typography>
              {activityLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress />
                </Box>
              ) : recentActivity.length > 0 ? (
                <Stack spacing={1}>
                  {recentActivity.map((activity: any, index: number) => (
                    <Box key={index}>
                      <Typography variant="body1" sx={{ fontWeight: 500, mb: 0.5 }}>
                        {activity.text}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {activity.time}
                      </Typography>
                      {index < recentActivity.length - 1 && <Divider sx={{ mt: 1 }} />}
                    </Box>
                  ))}
                </Stack>
              ) : (
                <Box sx={{ py: 4, textAlign: 'center' }}>
                  <Typography color="text.secondary">
                    No recent activity
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2, fontWeight: 500 }}>
                Quick Actions
              </Typography>
              <Stack spacing={1}>
                <Button
                  variant="outlined"
                  startIcon={<Upload />}
                  onClick={() => navigate('/library')}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Upload Track
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<AddCircle />}
                  onClick={() => navigate('/traffic/orders')}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Create Campaign
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<PlayArrow />}
                  onClick={() => navigate('/logs')}
                  fullWidth
                  sx={{ justifyContent: 'flex-start' }}
                >
                  Generate Log
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  )
}

export default Dashboard
