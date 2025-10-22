import React from 'react'
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
} from '@mui/material'
import {
  MusicNote,
  Campaign,
  Schedule,
  Assessment,
} from '@mui/icons-material'

const Dashboard: React.FC = () => {
  const stats = [
    { title: 'Total Tracks', value: '1,234', icon: <MusicNote /> },
    { title: 'Active Campaigns', value: '12', icon: <Campaign /> },
    { title: 'Clock Templates', value: '8', icon: <Schedule /> },
    { title: 'Reports Generated', value: '45', icon: <Assessment /> },
  ]

  const recentActivity = [
    { text: 'New track uploaded: "Morning Show Intro"', time: '2 hours ago' },
    { text: 'Campaign "Local Business" published', time: '4 hours ago' },
    { text: 'Daily log generated for 2024-01-15', time: '6 hours ago' },
    { text: 'Voice track recorded for Evening Show', time: '8 hours ago' },
  ]

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
