import React from 'react'
import { Box, Chip, Avatar, Tooltip } from '@mui/material'
import { Person as PersonIcon } from '@mui/icons-material'

interface ActiveUsersProps {
  users: Array<{ user_id: number; username: string }>
  isConnected: boolean
}

const ActiveUsers: React.FC<ActiveUsersProps> = ({ users, isConnected }) => {
  if (!isConnected) {
    return (
      <Chip
        icon={<PersonIcon />}
        label="Connecting..."
        size="small"
        color="default"
      />
    )
  }

  if (users.length === 0) {
    return (
      <Chip
        icon={<PersonIcon />}
        label="Just you"
        size="small"
        color="default"
      />
    )
  }

  return (
    <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
      <Chip
        icon={<PersonIcon />}
        label={`${users.length + 1} editing`}
        size="small"
        color="primary"
      />
      <Box sx={{ display: 'flex', gap: 0.5 }}>
        {users.map((user) => (
          <Tooltip key={user.user_id} title={user.username}>
            <Avatar
              sx={{
                width: 24,
                height: 24,
                fontSize: '0.75rem',
                bgcolor: 'primary.main',
              }}
            >
              {user.username.charAt(0).toUpperCase()}
            </Avatar>
          </Tooltip>
        ))}
      </Box>
    </Box>
  )
}

export default ActiveUsers

