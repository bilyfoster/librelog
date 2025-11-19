import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Badge,
  IconButton,
  Popover,
  Box,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Divider,
  Chip,
  CircularProgress,
} from '@mui/material'
import { Notifications as NotificationsIcon } from '@mui/icons-material'
import {
  getNotificationsProxy,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '../../utils/api'
import { formatDistanceToNow } from 'date-fns'

const NotificationBell: React.FC = () => {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null)
  const open = Boolean(anchorEl)

  const { data: unreadCount } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
    refetchInterval: 30000, // Refetch every 30 seconds
    retry: 1,
    retryDelay: 1000,
  })

  const { data: notifications } = useQuery({
    queryKey: ['notifications', { unread_only: false, limit: 20 }],
    queryFn: async () => {
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getNotificationsProxy({ unread_only: false, limit: 20 })
      return Array.isArray(data) ? data : []
    },
    enabled: open,
  })

  const queryClient = useQueryClient()

  const markReadMutation = useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllReadMutation = useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  const handleMarkRead = (notificationId: number) => {
    markReadMutation.mutate(notificationId)
  }

  const handleMarkAllRead = () => {
    markAllReadMutation.mutate()
  }

  const count = unreadCount?.count || 0
  const unreadNotifications = notifications?.filter((n: any) => n.status !== 'READ') || []

  return (
    <>
      <IconButton color="inherit" onClick={handleClick}>
        <Badge badgeContent={count} color="error">
          <NotificationsIcon />
        </Badge>
      </IconButton>
      <Popover
        open={open}
        anchorEl={anchorEl}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        PaperProps={{
          sx: { width: 400, maxHeight: 600 },
        }}
      >
        <Box sx={{ p: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Notifications</Typography>
            {unreadNotifications.length > 0 && (
              <Button size="small" onClick={handleMarkAllRead}>
                Mark all read
              </Button>
            )}
          </Box>
          <Divider />
          {notifications?.length === 0 ? (
            <Box sx={{ p: 2, textAlign: 'center' }}>
              <Typography variant="body2" color="text.secondary">
                No notifications
              </Typography>
            </Box>
          ) : (
            <List sx={{ maxHeight: 500, overflow: 'auto' }}>
              {notifications?.map((notification: any) => {
                const isUnread = notification.status !== 'READ'
                return (
                  <ListItem
                    key={notification.id}
                    sx={{
                      bgcolor: isUnread ? 'action.hover' : 'transparent',
                      cursor: 'pointer',
                      '&:hover': { bgcolor: 'action.selected' },
                    }}
                    onClick={() => {
                      if (isUnread) {
                        handleMarkRead(notification.id)
                      }
                    }}
                  >
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body2" fontWeight={isUnread ? 'bold' : 'normal'}>
                            {notification.subject || 'Notification'}
                          </Typography>
                          {isUnread && <Chip label="New" size="small" color="primary" />}
                        </Box>
                      }
                      secondary={
                        <Box>
                          <Typography variant="body2" color="text.secondary">
                            {notification.message}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {formatDistanceToNow(new Date(notification.created_at), {
                              addSuffix: true,
                            })}
                          </Typography>
                        </Box>
                      }
                    />
                  </ListItem>
                )
              })}
            </List>
          )}
        </Box>
      </Popover>
    </>
  )
}

export default NotificationBell

