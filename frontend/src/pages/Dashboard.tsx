import React from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  CircularProgress,
  Alert,
} from '@mui/material'
import {
  MusicNote,
  Campaign,
  Schedule,
  Assessment,
} from '@mui/icons-material'
import { getDashboardStats, getRecentActivity, checkApiHealth } from '../utils/api'
import api from '../utils/api'
import APIDiagnostics from '../components/APIDiagnostics'

const Dashboard: React.FC = () => {
  // Check API health first
  const { data: healthData, error: healthError } = useQuery({
    queryKey: ['api-health'],
    queryFn: () => checkApiHealth(),
    retry: false,
    refetchInterval: 30000, // Check every 30 seconds
  })

  // Fetch dashboard stats using count endpoints
  const { data: statsData, isLoading: statsLoading, error: statsError } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => getDashboardStats(),
    retry: 1,
    retryDelay: 1000,
    staleTime: 60000, // Cache for 1 minute
  })

  // Fetch recent activity
  const { data: activityData, isLoading: activityLoading } = useQuery({
    queryKey: ['recentActivity'],
    queryFn: () => getRecentActivity(20),
    retry: false, // Don't retry - getRecentActivity already handles errors
    staleTime: 30000, // Cache for 30 seconds
  })

  const stats = [
    { title: 'Total Tracks', value: statsData?.totalTracks || '0', icon: <MusicNote /> },
    { title: 'Active Campaigns', value: statsData?.activeCampaigns || '0', icon: <Campaign /> },
    { title: 'Clock Templates', value: statsData?.clockTemplates || '0', icon: <Schedule /> },
    { title: 'Reports Generated', value: statsData?.reportsGenerated || '0', icon: <Assessment /> },
  ]

  const recentActivity = activityData?.activities || []

  // Show API connectivity issue first
  if (healthError) {
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Dashboard</Typography>
        <Alert severity="error" sx={{ mt: 2, mb: 2 }}>
          <Typography variant="h6" gutterBottom>API Connection Failed</Typography>
          <Typography variant="body2">
            Cannot reach the backend API. The backend may be down or unreachable.
          </Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Error: {healthError instanceof Error ? healthError.message : 'Unknown error'}
          </Typography>
          <Typography variant="body2" sx={{ mt: 2, fontWeight: 'bold' }}>
            Troubleshooting Steps:
          </Typography>
          <Box component="ol" sx={{ mt: 1, mb: 2, pl: 3 }}>
            <li>Check if backend container is running: <code>docker-compose ps | grep api</code></li>
            <li>Check backend logs: <code>docker-compose logs api</code></li>
            <li>Verify backend is listening on port 8000</li>
            <li>Check Traefik routing configuration (if in production)</li>
            <li>Verify network connectivity between frontend and backend containers</li>
            <li>Check browser Network tab for actual HTTP status codes</li>
            <li>Verify CORS configuration allows requests from this origin</li>
          </Box>
          <Typography variant="body2" sx={{ mt: 1 }}>
            <strong>Common Issues:</strong>
          </Typography>
          <Box component="ul" sx={{ mt: 1, mb: 2, pl: 3 }}>
            <li>Backend container not started → Start with <code>docker-compose up -d api</code></li>
            <li>Backend crashed → Check logs for Python errors</li>
            <li>Network isolation → Containers may not be on same Docker network</li>
            <li>Port conflict → Another service may be using port 8000</li>
            <li>Traefik misconfiguration → Backend service not registered with Traefik</li>
          </Box>
        </Alert>
        <APIDiagnostics />
      </Box>
    )
  }

  if (statsLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
        {healthData && (
          <Typography variant="body2" sx={{ ml: 2 }}>
            API Status: {healthData.status}
          </Typography>
        )}
      </Box>
    )
  }

  if (statsError) {
    return (
      <Box p={3}>
        <Typography variant="h4" gutterBottom>Dashboard</Typography>
        {healthData && (
          <Alert severity="info" sx={{ mt: 2, mb: 2 }}>
            API is reachable (Status: {healthData.status}), but some endpoints are failing.
          </Alert>
        )}
        <Alert severity="error" sx={{ mt: healthData ? 0 : 2 }}>
          Failed to load dashboard data: {statsError instanceof Error ? statsError.message : 'Unknown error'}
        </Alert>
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Dashboard
      </Typography>
      
      <Grid container spacing={3}>
        {stats.map((stat, index) => (
          <Grid item xs={12} sm={6} md={3} key={index}>
            <Card>
              <CardContent>
                <Box display="flex" alignItems="center">
                  <Box mr={2}>{stat.icon}</Box>
                  <Box>
                    <Typography variant="h6">{stat.value}</Typography>
                    <Typography color="textSecondary">{stat.title}</Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
        
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Recent Activity
            </Typography>
            {activityLoading ? (
              <Box display="flex" justifyContent="center" p={2}>
                <CircularProgress size={24} />
              </Box>
            ) : recentActivity.length > 0 ? (
              <List>
                {recentActivity.map((activity, index) => (
                  <ListItem key={index}>
                    <ListItemText
                      primary={activity.text}
                      secondary={activity.time}
                    />
                  </ListItem>
                ))}
              </List>
            ) : (
              <Typography color="textSecondary" sx={{ p: 2 }}>
                No recent activity
              </Typography>
            )}
          </Paper>
        </Grid>
        
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Quick Actions
            </Typography>
            <List>
              <ListItem button>
                <ListItemIcon>
                  <MusicNote />
                </ListItemIcon>
                <ListItemText primary="Upload Track" />
              </ListItem>
              <ListItem button>
                <ListItemIcon>
                  <Campaign />
                </ListItemIcon>
                <ListItemText primary="Create Campaign" />
              </ListItem>
              <ListItem button>
                <ListItemIcon>
                  <Schedule />
                </ListItemIcon>
                <ListItemText primary="Generate Log" />
              </ListItem>
            </List>
          </Paper>
        </Grid>
      </Grid>
    </Box>
  )
}

export default Dashboard
