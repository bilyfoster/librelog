import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../utils/api'

interface User {
  id?: string
  username: string
  role: 'admin' | 'producer' | 'dj' | 'sales'
}

interface AuthContextType {
  user: User | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  isLoading: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

interface AuthProviderProps {
  children: ReactNode
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    // Check if user is logged in on app start
    const validateToken = async () => {
      const token = localStorage.getItem('token')
      if (token) {
        try {
          // Validate token and get user info
          // Token is automatically added by api interceptor
          const userResponse = await api.get('/auth/me')
          setUser({
            id: userResponse.data.id,
            username: userResponse.data.username,
            role: userResponse.data.role,
          })
        } catch (error: any) {
          // Token is invalid or expired, clear it
          console.warn('Token validation failed:', error)
          localStorage.removeItem('token')
          setUser(null)
        }
      }
      setIsLoading(false)
    }
    
    validateToken()
  }, [])

  const login = async (username: string, password: string) => {
    try {
      const response = await api.post('/auth/login', {
        username,
        password,
      }, {
        timeout: 10000, // 10 second timeout for login
      })
      
      const { access_token } = response.data
      if (!access_token) {
        throw new Error('No access token received')
      }
      
      localStorage.setItem('token', access_token)
      
      // Get user info from token or API
      try {
        const userResponse = await api.get('/auth/me')
        setUser({
          id: userResponse.data.id,
          username: userResponse.data.username,
          role: userResponse.data.role,
        })
      } catch (err) {
        // Fallback if /auth/me fails
        console.warn('Could not fetch user info, using defaults:', err)
        setUser({ id: 1, username, role: 'admin' })
      }
      
      navigate('/dashboard')
    } catch (error: any) {
      console.error('Login failed:', error)
      const errorMessage = error.response?.data?.detail || error.message || 'Login failed. Please check your credentials.'
      throw new Error(errorMessage)
    }
  }

  const logout = () => {
    localStorage.removeItem('token')
    setUser(null)
    navigate('/login')
  }

  const value = {
    user,
    login,
    logout,
    isLoading,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
