import React from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Alert,
  AlertTitle,
  Box,
  Typography,
  Link,
  List,
  ListItem,
  ListItemText,
  Collapse,
  IconButton,
} from '@mui/material'
import {
  Warning as WarningIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
} from '@mui/icons-material'
import { getExpiringCopy } from '../../utils/api'
import { useNavigate } from 'react-router-dom'

interface ExpiringCopyAlertProps {
  daysAhead?: number
  maxItems?: number
}

const ExpiringCopyAlert: React.FC<ExpiringCopyAlertProps> = ({
  daysAhead = 30,
  maxItems = 5,
}) => {
  const [expanded, setExpanded] = React.useState(false)
  const navigate = useNavigate()

  const { data: expiringCopy, isLoading } = useQuery({
    queryKey: ['expiring-copy', daysAhead],
    queryFn: async () => {
      const data = await getExpiringCopy(daysAhead)
      return data
    },
    refetchInterval: 5 * 60 * 1000, // Refetch every 5 minutes
  })

  if (isLoading || !expiringCopy || expiringCopy.length === 0) {
    return null
  }

  const calculateDaysUntilExpiry = (expiresAt: string) => {
    const expiry = new Date(expiresAt)
    const now = new Date()
    const days = Math.ceil((expiry.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
    return days
  }

  const sortedCopy = [...expiringCopy].sort((a, b) => {
    if (!a.expires_at || !b.expires_at) return 0
    return new Date(a.expires_at).getTime() - new Date(b.expires_at).getTime()
  })

  const displayedItems = sortedCopy.slice(0, maxItems)
  const remainingCount = sortedCopy.length - maxItems

  const handleViewAll = () => {
    navigate('/traffic/copy')
  }

  return (
    <Alert
      severity="warning"
      icon={<WarningIcon />}
      action={
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {sortedCopy.length > maxItems && (
            <IconButton
              size="small"
              onClick={() => setExpanded(!expanded)}
              aria-label="expand"
            >
              {expanded ? <ExpandLessIcon /> : <ExpandMoreIcon />}
            </IconButton>
          )}
        </Box>
      }
    >
      <AlertTitle>
        {sortedCopy.length} Copy Item{sortedCopy.length !== 1 ? 's' : ''} Expiring Soon
      </AlertTitle>
      <Typography variant="body2" sx={{ mb: 1 }}>
        The following copy items will expire within the next {daysAhead} days:
      </Typography>

      <List dense sx={{ mb: 1 }}>
        {displayedItems.map((copy: any) => {
          const daysUntil = copy.expires_at
            ? calculateDaysUntilExpiry(copy.expires_at)
            : null
          return (
            <ListItem key={copy.id} sx={{ py: 0.5 }}>
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="body2" fontWeight="medium">
                      {copy.title}
                    </Typography>
                    {daysUntil !== null && (
                      <Typography
                        variant="caption"
                        color={daysUntil <= 7 ? 'error.main' : 'warning.main'}
                        sx={{ ml: 2 }}
                      >
                        {daysUntil === 0
                          ? 'Expires today'
                          : daysUntil === 1
                          ? 'Expires tomorrow'
                          : `${daysUntil} days`}
                      </Typography>
                    )}
                  </Box>
                }
                secondary={
                  copy.expires_at
                    ? `Expires: ${new Date(copy.expires_at).toLocaleDateString()}`
                    : undefined
                }
              />
            </ListItem>
          )
        })}
      </List>

      <Collapse in={expanded}>
        <List dense>
          {sortedCopy.slice(maxItems).map((copy: any) => {
            const daysUntil = copy.expires_at
              ? calculateDaysUntilExpiry(copy.expires_at)
              : null
            return (
              <ListItem key={copy.id} sx={{ py: 0.5 }}>
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <Typography variant="body2" fontWeight="medium">
                        {copy.title}
                      </Typography>
                      {daysUntil !== null && (
                        <Typography
                          variant="caption"
                          color={daysUntil <= 7 ? 'error.main' : 'warning.main'}
                          sx={{ ml: 2 }}
                        >
                          {daysUntil === 0
                            ? 'Expires today'
                            : daysUntil === 1
                            ? 'Expires tomorrow'
                            : `${daysUntil} days`}
                        </Typography>
                      )}
                    </Box>
                  }
                  secondary={
                    copy.expires_at
                      ? `Expires: ${new Date(copy.expires_at).toLocaleDateString()}`
                      : undefined
                  }
                />
              </ListItem>
            )
          })}
        </List>
      </Collapse>

      {remainingCount > 0 && !expanded && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          ... and {remainingCount} more item{remainingCount !== 1 ? 's' : ''}
        </Typography>
      )}

      <Box sx={{ mt: 2 }}>
        <Link
          component="button"
          variant="body2"
          onClick={handleViewAll}
          sx={{ cursor: 'pointer' }}
        >
          View all in Copy Library →
        </Link>
      </Box>
    </Alert>
  )
}

export default ExpiringCopyAlert

