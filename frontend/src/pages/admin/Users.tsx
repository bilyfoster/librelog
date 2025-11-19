import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  CircularProgress,
  Alert,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Chip,
} from '@mui/material'
import { Add, Edit, Delete } from '@mui/icons-material'
import { getUsersProxy, createUser, updateUser, deleteUser } from '../../utils/api'

interface User {
  id: number
  username: string
  role: string
  created_at: string
  last_login?: string | null
  last_activity?: string | null
}

const Users: React.FC = () => {
  const [openDialog, setOpenDialog] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [roleFilter, setRoleFilter] = useState<string>('')
  const queryClient = useQueryClient()

  const { data: users, isLoading, error } = useQuery({
    queryKey: ['users', roleFilter],
    queryFn: async () => {
      const params: any = { limit: 100, skip: 0 }
      if (roleFilter) params.role_filter = roleFilter
      // Use server-side proxy endpoint - all processing happens on backend
      const data = await getUsersProxy(params)
      return Array.isArray(data) ? data : []
    },
    retry: 1,
  })

  const createMutation = useMutation({
    mutationFn: async (data: { username: string; password: string; role: string }) => {
      return await createUser(data)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setOpenDialog(false)
      setEditingUser(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to create user'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Create user error:', error)
    },
  })

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: number; data: { username?: string; password?: string; role?: string } }) => {
      return await updateUser(id, data)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setOpenDialog(false)
      setEditingUser(null)
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to update user'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Update user error:', error)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: async (id: number) => {
      await deleteUser(id)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setErrorMessage(null)
    },
    onError: (error: any) => {
      let message = 'Failed to delete user'
      if (error?.response?.data?.detail) {
        message = error.response.data.detail
      } else if (error?.response?.data?.message) {
        message = error.response.data.message
      } else if (error?.message) {
        message = error.message
      }
      setErrorMessage(message)
      console.error('Delete user error:', error)
    },
  })

  const handleEdit = (user: User) => {
    setEditingUser(user)
    setOpenDialog(true)
    setErrorMessage(null)
  }

  const handleDelete = (id: number) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      deleteMutation.mutate(id)
    }
  }

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const username = formData.get('username') as string
    const password = formData.get('password') as string
    const role = formData.get('role') as string

    if (!username || !role) {
      setErrorMessage('Username and role are required')
      return
    }

    if (editingUser) {
      const updateData: any = { role }
      if (username !== editingUser.username) {
        updateData.username = username
      }
      if (password) {
        updateData.password = password
      }
      updateMutation.mutate({ id: editingUser.id, data: updateData })
    } else {
      if (!password) {
        setErrorMessage('Password is required for new users')
        return
      }
      createMutation.mutate({ username, password, role })
    }
  }

  const handleCloseDialog = () => {
    setOpenDialog(false)
    setEditingUser(null)
    setErrorMessage(null)
  }

  const getRoleColor = (role: string) => {
    const colors: { [key: string]: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' } = {
      admin: 'error',
      producer: 'primary',
      dj: 'info',
      sales: 'success',
    }
    return colors[role] || 'default'
  }

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={3}>
        <CircularProgress />
      </Box>
    )
  }

  if (error) {
    return (
      <Box>
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load users: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
        <Button variant="contained" onClick={() => queryClient.invalidateQueries({ queryKey: ['users'] })}>
          Retry
        </Button>
      </Box>
    )
  }

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">User Management</Typography>
        <Box display="flex" gap={2}>
          <TextField
            select
            label="Filter by Role"
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value)}
            sx={{ minWidth: 150 }}
            size="small"
          >
            <MenuItem value="">All Roles</MenuItem>
            <MenuItem value="admin">Admin</MenuItem>
            <MenuItem value="producer">Producer</MenuItem>
            <MenuItem value="dj">DJ</MenuItem>
            <MenuItem value="sales">Sales</MenuItem>
          </TextField>
          <Button
            variant="contained"
            color="primary"
            startIcon={<Add />}
            onClick={() => {
              setEditingUser(null)
              setOpenDialog(true)
              setErrorMessage(null)
            }}
          >
            Add User
          </Button>
        </Box>
      </Box>

      {errorMessage && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setErrorMessage(null)}>
          {errorMessage}
        </Alert>
      )}

      <Card>
        <CardContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Username</TableCell>
                  <TableCell>Role</TableCell>
                  <TableCell>Created</TableCell>
                  <TableCell>Last Login</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {users && users.length > 0 ? (
                  users.map((user: User) => (
                    <TableRow key={user.id}>
                      <TableCell>{user.username}</TableCell>
                      <TableCell>
                        <Chip label={user.role} color={getRoleColor(user.role)} size="small" />
                      </TableCell>
                      <TableCell>{new Date(user.created_at).toLocaleDateString()}</TableCell>
                      <TableCell>
                        {user.last_login ? new Date(user.last_login).toLocaleDateString() : 'Never'}
                      </TableCell>
                      <TableCell>
                        <IconButton size="small" onClick={() => handleEdit(user)}>
                          <Edit />
                        </IconButton>
                        <IconButton size="small" onClick={() => handleDelete(user.id)} color="error">
                          <Delete />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={5} align="center">
                      No users found
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <form onSubmit={handleSubmit}>
          <DialogTitle>{editingUser ? 'Edit User' : 'Add User'}</DialogTitle>
          <DialogContent>
            {errorMessage && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {errorMessage}
              </Alert>
            )}
            <TextField
              fullWidth
              label="Username"
              name="username"
              defaultValue={editingUser?.username || ''}
              margin="normal"
              required
              disabled={!!editingUser}
            />
            <TextField
              fullWidth
              label="Password"
              name="password"
              type="password"
              margin="normal"
              required={!editingUser}
              helperText={editingUser ? 'Leave blank to keep current password' : 'Required for new users'}
            />
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Role</InputLabel>
              <Select
                name="role"
                defaultValue={editingUser?.role || ''}
                label="Role"
                required
              >
                <MenuItem value="admin">Admin</MenuItem>
                <MenuItem value="producer">Producer</MenuItem>
                <MenuItem value="dj">DJ</MenuItem>
                <MenuItem value="sales">Sales</MenuItem>
              </Select>
            </FormControl>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseDialog}>Cancel</Button>
            <Button
              type="submit"
              variant="contained"
              disabled={createMutation.isPending || updateMutation.isPending}
            >
              {createMutation.isPending || updateMutation.isPending ? <CircularProgress size={24} /> : editingUser ? 'Update' : 'Create'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  )
}

export default Users





