import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Card,
  CardContent,
  Chip,
  List,
  ListItem,
  ListItemText,
  Typography,
  Button,
  CircularProgress,
  Tabs,
  Tab,
  Divider,
  Alert,
} from '@mui/material'
import { CheckCircle as ReadIcon } from '@mui/icons-material'
import {
  getNotificationsProxy,
  markNotificationRead,
  markAllNotificationsRead,
} from '../../utils/api'
import { formatDistanceToNow } from 'date-fns'

const Notifications: React.FC = () => {
  const [tab, setTab] = useState(0)
  const [unreadOnly, setUnreadOnly] = useState(false)

  const { data: notifications, isLoading, error } = useQuery({
    queryKey: ['notifications', { unread_only: unreadOnly }],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getNotificationsProxy({ unread_only: unreadOnly, limit: 100 })
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const queryClient = useQueryClient()

  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to mark notification as read'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Mark read error:', error)
    },
  })

  const markAllReadMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to mark all notifications as read'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Mark all read error:', error)
    },
  })

  const handleMarkRead = (notificationId: number) => {
    markReadMutation.mutate(notificationId)
  }

  const handleMarkAllRead = () => {
    markAllReadMutation.mutate()
  }

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTab(newValue)
    setUnreadOnly(newValue === 1)
  }

  const unreadCount = notifications?.filter((n: any) => n.status !== 'READ').length || 0

  return (
    <Box>
      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h4">Notifications</Typography>
        {unreadCount > 0 && (
          <Button
            variant="outlined"
            startIcon={<ReadIcon />}
            onClick={handleMarkAllRead}
          >
            Mark All Read
          </Button>
        )}
      </Box>

      <Card>
        <CardContent>
          <Tabs value={tab} onChange={handleTabChange} sx={{ mb: 2 }}>
            <Tab label={`All (${notifications?.length || 0})`} />
            <Tab label={`Unread (${unreadCount})`} />
          </Tabs>

          <Divider sx={{ mb: 2 }} />

          {isLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : error ? (
            <Alert severity="error" sx={{ m: 2 }} action={
              <Button color="inherit" size="small" onClick={() => queryClient.invalidateQueries({ queryKey: ['notifications'] })}>
                Retry
              </Button>
            }>
              Failed to load notifications: {error instanceof Error ? error.message : 'Unknown error'}
            </Alert>
          ) : notifications?.length === 0 ? (
            <Box sx={{ p: 3, textAlign: 'center' }}>
              <Typography variant="body1" color="text.secondary">
                No notifications found
              </Typography>
            </Box>
          ) : (
            <List>
              {notifications?.map((notification: any) => {
                const isUnread = notification.status !== 'READ'
                return (
                  <ListItem
                    key={notification.id}
                    sx={{
                      bgcolor: isUnread ? 'action.hover' : 'transparent',
                      borderLeft: isUnread ? '3px solid' : 'none',
                      borderColor: isUnread ? 'primary.main' : 'transparent',
                      mb: 1,
                      '&:hover': { bgcolor: 'action.selected' },
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                          <Typography variant="subtitle1" fontWeight={isUnread ? 'bold' : 'normal'}>
                            {notification.subject || 'Notification'}
                          </Typography>
                          {isUnread && (
                            <Chip label="Unread" size="small" color="primary" />
                          )}
                          <Chip
                            label={notification.notification_type}
                            size="small"
                            variant="outlined"
                          />
                        </Box>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                            {notification.message}
                          </Typography>
                          <Box sx={{ display: 'flex', gap: 2 }}>
                            <Typography variant="caption" color="text.secondary">
                              {formatDistanceToNow(new Date(notification.created_at), {
                                addSuffix: true,
                              })}
                            </Typography>
                            {notification.sent_at && (
                              <Typography variant="caption" color="text.secondary">
                                Sent: {new Date(notification.sent_at).toLocaleString()}
                              </Typography>
                            )}
                          </Box>
                        </Box>
                      }
                    />
                    {isUnread && (
                      <Button
                        size="small"
                        onClick={() => handleMarkRead(notification.id)}
                        startIcon={<ReadIcon />}
                      >
                        Mark Read
                      </Button>
                    )}
                  </ListItem>
                )
              })}
            </List>
          )}
        </CardContent>
      </Card>
    </Box>
  )
}

export default Notifications

