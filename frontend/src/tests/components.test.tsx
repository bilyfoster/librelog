import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import Login from '../pages/auth/Login'
import Dashboard from '../pages/Dashboard'
import LibraryList from '../pages/library/LibraryList'
import api from '../utils/api'

// Mock API
vi.mock('../utils/api', () => ({
  post: vi.fn(),
  get: vi.fn(),
  interceptors: {
    request: { use: vi.fn() },
    response: { use: vi.fn() }
  }
}))

// Mock AuthContext
const mockLogin = vi.fn()
const mockLogout = vi.fn()

vi.mock('../contexts/AuthContext', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'testuser', role: 'admin' },
    login: mockLogin,
    logout: mockLogout,
    isLoading: false
  })
}))

// Test wrapper component
const TestWrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  })
  
  const theme = createTheme()
  
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <BrowserRouter>
          {children}
        </BrowserRouter>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

describe('Login Component', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form', () => {
    render(
      <TestWrapper>
        <Login />
      </TestWrapper>
    )
    
    expect(screen.getByText('LibreLog')).toBeInTheDocument()
    expect(screen.getByText('GayPHX Radio Traffic System')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Sign In' })).toBeInTheDocument()
  })

  it('handles form submission', async () => {
    mockLogin.mockResolvedValue(undefined)
    
    render(
      <TestWrapper>
        <Login />
      </TestWrapper>
    )
    
    const usernameInput = screen.getByLabelText('Username')
    const passwordInput = screen.getByLabelText('Password')
    const submitButton = screen.getByRole('button', { name: 'Sign In' })
    
    fireEvent.change(usernameInput, { target: { value: 'testuser' } })
    fireEvent.change(passwordInput, { target: { value: 'password' } })
    fireEvent.click(submitButton)
    
    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('testuser', 'password')
    })
  })

  it('shows error message on login failure', async () => {
    mockLogin.mockRejectedValue(new Error('Login failed'))
    
    render(
      <TestWrapper>
        <Login />
      </TestWrapper>
    )
    
    const usernameInput = screen.getByLabelText('Username')
    const passwordInput = screen.getByLabelText('Password')
    const submitButton = screen.getByRole('button', { name: 'Sign In' })
    
    fireEvent.change(usernameInput, { target: { value: 'testuser' } })
    fireEvent.change(passwordInput, { target: { value: 'password' } })
    fireEvent.click(submitButton)
    
    await waitFor(() => {
      expect(screen.getByText('Invalid username or password')).toBeInTheDocument()
    })
  })
})

describe('Dashboard Component', () => {
  it('renders dashboard with stats', () => {
    render(
      <TestWrapper>
        <Dashboard />
      </TestWrapper>
    )
    
    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.getByText('Total Tracks')).toBeInTheDocument()
    expect(screen.getByText('Active Campaigns')).toBeInTheDocument()
    expect(screen.getByText('Clock Templates')).toBeInTheDocument()
    expect(screen.getByText('Reports Generated')).toBeInTheDocument()
  })

  it('renders recent activity', () => {
    render(
      <TestWrapper>
        <Dashboard />
      </TestWrapper>
    )
    
    expect(screen.getByText('Recent Activity')).toBeInTheDocument()
    expect(screen.getByText('New track uploaded: "Morning Show Intro"')).toBeInTheDocument()
  })

  it('renders quick actions', () => {
    render(
      <TestWrapper>
        <Dashboard />
      </TestWrapper>
    )
    
    expect(screen.getByText('Quick Actions')).toBeInTheDocument()
    expect(screen.getByText('Upload Track')).toBeInTheDocument()
    expect(screen.getByText('Create Campaign')).toBeInTheDocument()
    expect(screen.getByText('Generate Log')).toBeInTheDocument()
  })
})

describe('LibraryList Component', () => {
  it('renders library list', () => {
    render(
      <TestWrapper>
        <LibraryList />
      </TestWrapper>
    )
    
    expect(screen.getByText('Audio Library')).toBeInTheDocument()
    expect(screen.getByText('Upload Track')).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/Search music\.\.\./i)).toBeInTheDocument()
  })

  it('renders track table', () => {
    render(
      <TestWrapper>
        <LibraryList />
      </TestWrapper>
    )
    
    expect(screen.getByText('Title')).toBeInTheDocument()
    expect(screen.getByText('Artist')).toBeInTheDocument()
    expect(screen.getByText('Type')).toBeInTheDocument()
    expect(screen.getByText('Duration')).toBeInTheDocument()
    expect(screen.getByText('Actions')).toBeInTheDocument()
  })

  it('renders mock tracks', () => {
    render(
      <TestWrapper>
        <LibraryList />
      </TestWrapper>
    )
    
    expect(screen.getByText('Morning Show Intro')).toBeInTheDocument()
    expect(screen.getByText('Local Business Ad')).toBeInTheDocument()
    expect(screen.getByText('Community PSA')).toBeInTheDocument()
  })
})

describe('API Utils', () => {
  it('creates API instance with correct base URL', () => {
    // In browser context, baseURL should always be relative '/api'
    // This works with both Vite dev proxy and Traefik production routing
    expect(api.defaults.baseURL).toBe('/api')
  })

  it('has request interceptor', () => {
    expect(api.interceptors.request.use).toBeDefined()
  })

  it('has response interceptor', () => {
    expect(api.interceptors.response.use).toBeDefined()
  })
})
