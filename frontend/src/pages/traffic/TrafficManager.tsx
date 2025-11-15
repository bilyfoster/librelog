import React from 'react'
import { Box, Typography, Grid, Card, CardContent, CardActionArea, Button } from '@mui/material'
import { useNavigate } from 'react-router-dom'
import {
  Campaign as CampaignIcon,
  Business as BusinessIcon,
  Assignment as OrderIcon,
  People as PeopleIcon,
  Description as CopyIcon,
  Schedule as ScheduleIcon,
  AccessTime as DaypartIcon,
} from '@mui/icons-material'

const TrafficManager: React.FC = () => {
  const navigate = useNavigate()

  const menuItems = [
    { title: 'Advertisers', icon: <BusinessIcon />, path: '/traffic/advertisers', description: 'Manage advertising clients' },
    { title: 'Agencies', icon: <BusinessIcon />, path: '/traffic/agencies', description: 'Manage advertising agencies' },
    { title: 'Orders', icon: <OrderIcon />, path: '/traffic/orders', description: 'View and manage advertising orders' },
    { title: 'Sales Reps', icon: <PeopleIcon />, path: '/traffic/sales-reps', description: 'Manage sales representatives' },
    { title: 'Spot Scheduler', icon: <ScheduleIcon />, path: '/traffic/spot-scheduler', description: 'Schedule spots for orders' },
    { title: 'Dayparts', icon: <DaypartIcon />, path: '/traffic/dayparts', description: 'Configure broadcast dayparts' },
    { title: 'Copy Library', icon: <CopyIcon />, path: '/traffic/copy', description: 'Manage audio copy and scripts' },
  ]

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Traffic Manager
      </Typography>
      <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
        Manage clients, orders, spots, and copy assets
      </Typography>
      
      <Grid container spacing={3}>
        {menuItems.map((item) => (
          <Grid item xs={12} sm={6} md={4} key={item.path}>
            <Card>
              <CardActionArea onClick={() => navigate(item.path)}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <Box sx={{ mr: 2, color: 'primary.main' }}>
                      {item.icon}
                    </Box>
                    <Typography variant="h6">{item.title}</Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    {item.description}
                  </Typography>
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}

export default TrafficManager
