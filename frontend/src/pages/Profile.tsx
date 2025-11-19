import React, { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Alert,
  CircularProgress,
  Stack,
  Divider,
  InputAdornment,
  IconButton,
} from '@mui/material'
import {
  Save as SaveIcon,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material'
import api from '../utils/api'

const Profile: React.FC = () => {
  const [showPassword, setShowPassword] = useState(false)
  const [showCurrentPassword, setShowCurrentPassword] = useState(false)
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    current_password: '',
  })
  const [errors, setErrors] = useState<Record<string, string>>({})

  const queryClient = useQueryClient()

  const { data: profile, isLoading } = useQuery({
    queryKey: ['user-profile'],
    queryFn: async () => {
      const response = await api.get('/auth/profile')
      return response.data
    },
  })

  React.useEffect(() => {
    if (profile) {
      setFormData({
        username: profile.username || '',
        password: '',
        current_password: '',
      })
    }
  }, [profile])

  const updateProfileMutation = useMutation({
    mutationFn: async (data: typeof formData) => {
      const payload: any = {}
      if (data.username !== profile?.username) {
        payload.username = data.username
      }
      if (data.password) {
        payload.password = data.password
        payload.current_password = data.current_password
      }
      const response = await api.put('/auth/profile', payload)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user-profile'] })
      queryClient.invalidateQueries({ queryKey: ['auth-me'] })
      setErrors({})
      setFormData({
        username: formData.username,
        password: '',
        current_password: '',
      })
    },
    onError: (error: any) => {
      if (error.response?.data?.detail) {
        setErrors({ submit: error.response.data.detail })
      } else {
        setErrors({ submit: 'Failed to update profile' })
      }
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setErrors({})

    // Validation
    if (formData.password && !formData.current_password) {
      setErrors({ current_password: 'Current password is required to change password' })
      return
    }

    updateProfileMutation.mutate(formData)
  }

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        My Profile
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Update your profile information and password
      </Typography>

      <Card>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <Stack spacing={3}>
              {errors.submit && (
                <Alert severity="error">{errors.submit}</Alert>
              )}

              <TextField
                label="Username"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                fullWidth
                required
                error={!!errors.username}
                helperText={errors.username}
              />

              <Divider />

              <Typography variant="h6">Change Password</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Leave blank to keep current password
              </Typography>

              <TextField
                label="Current Password"
                type={showCurrentPassword ? 'text' : 'password'}
                value={formData.current_password}
                onChange={(e) => setFormData({ ...formData, current_password: e.target.value })}
                fullWidth
                error={!!errors.current_password}
                helperText={errors.current_password}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                        edge="end"
                      >
                        {showCurrentPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                label="New Password"
                type={showPassword ? 'text' : 'password'}
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                fullWidth
                error={!!errors.password}
                helperText={errors.password}
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        onClick={() => setShowPassword(!showPassword)}
                        edge="end"
                      >
                        {showPassword ? <VisibilityOff /> : <Visibility />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />

              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SaveIcon />}
                  disabled={updateProfileMutation.isPending}
                >
                  {updateProfileMutation.isPending ? 'Saving...' : 'Save Changes'}
                </Button>
              </Box>
            </Stack>
          </form>
        </CardContent>
      </Card>

      {profile && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Account Information
            </Typography>
            <Stack spacing={2}>
              <Box>
                <Typography variant="body2" color="text.secondary">
                  Role
                </Typography>
                <Typography variant="body1">{profile.role}</Typography>
              </Box>
              {profile.created_at && (
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Account Created
                  </Typography>
                  <Typography variant="body1">
                    {new Date(profile.created_at).toLocaleDateString()}
                  </Typography>
                </Box>
              )}
              {profile.last_login && (
                <Box>
                  <Typography variant="body2" color="text.secondary">
                    Last Login
                  </Typography>
                  <Typography variant="body1">
                    {new Date(profile.last_login).toLocaleString()}
                  </Typography>
                </Box>
              )}
            </Stack>
          </CardContent>
        </Card>
      )}
    </Box>
  )
}

export default Profile

